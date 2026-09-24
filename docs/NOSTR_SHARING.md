# Shared shopping lists over Nostr: plan

Goal: several people keep **one shopping list** in sync, end-to-end encrypted,
over Nostr relays. Default relay `wss://shopping.openintents.org`; users can
bring their own. No account and no sign-up: you share a list by showing a QR code,
and others join by scanning it. A shared list behaves like a small private
group chat whose messages are list updates.

Version 1 limits: a device can share or join **one list at a time**. The data
model allows more lists later.

---

## 1. User experience

**Share (owner)**
1. List menu → *Share list…*
2. The first time only, a short explainer: "Anyone who scans this code can see
   and change this list. Updates go end-to-end encrypted through
   shopping.openintents.org." The *Relay* link opens the relay setting.
3. A QR code appears, with *Copy link* and *Send link* (Android share sheet).
   The list is live from this moment.

**Join (member)**
- Scan the QR code with the phone's camera app. It opens a link that goes
  straight into the app (see section 5). Alternatively, open a shared link, or tap
  *Join shared list* in the drawer.
- A confirmation shows the list name: *Join "Groceries"?* → the list appears
  and fills in within a second or two.

**While shared**
- A small cloud icon in the app bar shows the state: synced, syncing, offline
  (changes are queued) or error.
- *Share list…* shows the same QR again, so more people can join.
- *Stop sharing* keeps the local copy and stops syncing. It cannot un-share
  data others already have.

There are no keys, no nsec and no relay URLs for the default user, and no sign-up.

## 2. Protocol (v1)

### One secret per list, used as a group key
Sharing a list creates a random 32-byte **list secret** `S`. Everything is
derived from it:

| Derived | How | Purpose |
|---|---|---|
| List key pair | `sk = S` (secp256k1, retried if out of range), `pk = xonly(sk·G)` | Signs every event of this list (BIP-340 Schnorr) |
| Conversation key | NIP-44 v2 `get_conversation_key(sk, pk)` (ECDH with itself) | Encrypts all content (NIP-44 v2) |
| Item address | `d = hex(HMAC-SHA256(S, "item:" + itemUuid))` | Item ids the relay cannot link to names |

Every member signs with the **list** key, so the relay sees a single pubkey per
list and no member identities. Standard NIP-44 still applies, because it is
"encrypt to self" with the list key.

Why not per-user keys (NIP-17 gift wraps, or NIP-EE/MLS groups)? Those need
member management, one copy per member for every change, and key rotation. That
is far more code and many more bytes than this feature needs. It is the upgrade
path if revocation becomes a requirement (section 8).

### Events
- **Item** = one addressable (parameterized replaceable) event, NIP-01 kind
  `3xxxx`. Pick an unused number and register it in nostr-protocol/nips.
  - `pubkey` = list pk; `tags`: `["d", d]`.
  - `content` = NIP-44 encryption of JSON such as
    `{"v":1,"id":"<uuid>","name":"Milk","qty":"2","units":"l","price":119,"status":1,"prio":null,"tags":null,"note":null,"hlc":"<hybrid logical clock>","by":"<device nickname>"}`.
  - Because the event is addressable, the relay keeps only the latest version
    of each item, so a list's full state is just `REQ {kinds:[K], authors:[pk]}`.
- **List meta** (name, theme): the same kind, with `d = HMAC(S, "meta")`.
- **Delete**: publish a tombstone `{"v":1,"id":…,"deleted":true,"hlc":…}` with a
  NIP-40 `expiration` tag of about 60 days, so relays eventually drop it.

### Merge rules
- Newest wins per item, by `hlc` (which gives sub-second ordering), with the
  event id as tie-breaker. The relay's `created_at` replacement order is a
  coarse backstop.
- Checking an item off is simply a status change on the item event, which
  gives offline-safe "last change wins" semantics.
- Local edits go into an **outbox** table and are published when online; an
  item is marked synced when the relay replies with `OK`.

### The share link and QR code
```
https://shopping.openintents.org/j#v=1&s=<base64url S>&r=wss%3A%2F%2Fshopping.openintents.org&n=Groceries
```
- The secret sits in the **fragment**, which browsers never send to the
  server, so even the landing page never sees it.
- `r` can be repeated for several relays (bring your own). The owner's relays
  travel in the QR code so everyone uses the same ones.
- As a QR code this is about 120 characters, which scans easily.

## 3. Relays

- **Default: `shopping.openintents.org`**, running strfry or nostr-rs-relay:
  - A write policy that only accepts kind `K`, content under 4 KB, and rate
    limits per IP and per pubkey.
  - A retention policy, for example dropping events untouched for 12 months.
  - A NIP-11 info document.
  - The same host serves `/.well-known/assetlinks.json` (for the Android App
    Link) and a tiny `/j` landing page ("Install OI Shopping List").
- **Bring your own:** Settings → *Relays* lets users edit the list, which
  defaults to `wss://shopping.openintents.org`. The setting is used when a new
  share is created. Joining always uses the relays in the QR code.
- The client connects to all relays of the list, publishes to all of them, and
  merges what it reads (deduplicated by event id).

## 4. App architecture

| Piece | Where | Notes |
|---|---|---|
| Crypto: secp256k1 BIP-340 sign/verify, ECDH, NIP-44 v2 (HKDF, ChaCha20, HMAC-SHA256) | new `nostr/crypto` package, pure Kotlin | Small and no JNI, so no per-ABI native libraries. Verified against the official BIP-340 and NIP-44 test vectors. Android's own ChaCha20 needs API 28, and minSdk is 21, so it is bundled. |
| Nostr client (NIP-01 `REQ` / `EVENT` / `EOSE` / `OK` / `CLOSE`) | `nostr/RelayClient` | WebSocket via OkHttp, which adds about 300–400 KB after R8. Reconnects with backoff and only runs while the app is visible. |
| Sync engine | `sync/ListSync` | Maps provider rows to item events and back, owns the outbox and HLC, and applies the merge rules. Pure logic with a fake relay, so it is JVM-testable. |
| Data | DB migration | Adds `contains.remote_id` (UUID) and `modified_hlc`, plus a `shared_lists(list_id, secret, relays, joined_at)` table and an `outbox` table. The secret is stored encrypted with an Android Keystore key. |
| QR code | show: `zxing-core` encoder (R8 keeps only the encoder); scan: the system camera plus an App Link | No camera code or ML Kit, so the app stays FOSS/F-Droid friendly. In-app *Scan* hands off to an installed scanner (for example Binary Eye), with *Paste link* as a fallback. |
| UI | Compose: share dialog with QR, join confirmation, sync badge, relay settings | |

**Sync triggers**
- Connect and subscribe when the app comes to the foreground; disconnect about
  30 seconds after it goes to the background.
- Changes from the widget or other apps are picked up on the next foreground,
  or immediately through the provider's change notification while connected.
- Background sync (JobScheduler, with no extra library) is optional for v1.1.

## 5. Permissions and size

- **`INTERNET` is new.** Until now "does not require the internet permission"
  has been a selling point. Options:
  1. **Recommended:** add it and keep the feature opt-in. Nothing connects
     until the user shares or joins a list, and the store description says so.
  2. Ship two flavors, with and without sync. This doubles release work and
     F-Droid builds.
- App Links need `autoVerify` on the `/j` path plus `assetlinks.json` with the
  Play and F-Droid signing certificates, which differ, so list both.
- Estimated APK growth: OkHttp about 350 KB, zxing encoder about 60 KB, crypto
  about 40 KB. That is around 0.45 MB on top of 3.6 MB.

## 6. Security properties (to state in the UI and the docs)

- Relays see: one pubkey per list, how many items it has, when they change, and
  IP addresses. They never see item names, prices or who is who.
- **Whoever has the QR code or link can read and write the list until it is
  re-keyed.** Treat it like a password.
- No forward secrecy, and removed members can't be locked out in v1. The
  workaround is *Stop sharing* followed by *Share list…* again, which creates a
  new secret and QR code.
- Events are authenticated (a valid list-key signature is required), so strangers
  cannot inject changes without the secret.

## 7. Delivery phases

1. **Spec and crypto core.** Write the protocol doc with the kind number. Add
   BIP-340 and NIP-44 in pure Kotlin with the official test vectors as JVM
   tests.
2. **Sync engine and data.** DB migration, outbox, HLC, merge. Test two
   simulated devices against an in-memory relay: offline edits, conflicts,
   deletions, joining late.
3. **Relay client.** OkHttp WebSocket plus reconnect. Add an integration test in
   CI against a throwaway strfry container.
4. **UI.** Share dialog with QR code, join by link or App Link, sync badge,
   relay settings, stop sharing. Localizable strings.
5. **Infrastructure.** Deploy `shopping.openintents.org` (relay with write
   policy, `assetlinks.json`, `/j` landing page) and add monitoring.
6. **Beta.** Test on two real phones (and emulators), for example list on one
   and check off on the other, offline then back online, then release.

## 8. Later (explicitly out of scope for v1)

- Several shared lists per device (the model already supports it; the UI limits it to one).
- Member management and revocation via per-user keys (NIP-17 / NIP-EE MLS).
- Background push via a relay-to-UnifiedPush bridge.
- Sharing with OI Shopping List on other platforms, since the protocol is
  plain Nostr and a web client is easy to build.

## 9. Open questions for the maintainer

1. Is it OK to add `INTERNET` (option 1 above), or should there be two flavors?
2. Who runs `shopping.openintents.org`, and what retention period should it use?
3. Are the Play and F-Droid signing certificates available for `assetlinks.json`?
4. Should there be interoperability with an existing Nostr list or tasks app,
   which would mean reusing its event format instead of a new kind?
