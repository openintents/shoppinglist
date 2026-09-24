# Shared shopping lists over Nostr: plan

Goal: several people keep **one shopping list** in sync, end-to-end encrypted,
over Nostr relays. Default relay `wss://shopping.openintents.org`; users can
bring their own. Sharing needs no sign-up: every user gets a Nostr account
automatically, and can switch to their own existing account. A list is shared by
showing a QR code, and others join by scanning it. A shared list behaves like a
small private group chat whose messages are list updates.

Version 1 limits: a device can share or join **one list at a time**. The data
model allows more lists later.

**Decisions made**
- The app declares the `INTERNET` permission. Sharing is opt-in: nothing
  connects until the user shares or joins a list.
- Every user has a **personal Nostr account**, created automatically. Users can
  switch to their **own account** (nsec or an external signer app) at any time,
  including right before sharing a list.

---

## 1. User experience

**Account (automatic)**
- The first time the user shares or joins a list, the app silently creates a
  Nostr key pair: the *automatic account*. There's no dialog and nothing to write
  down.
- Settings → *Nostr account* shows the account (npub, a display name used for
  "changed by …") and offers:
  - *Use my own account…*: paste an `nsec`, or connect a signer app (NIP-55,
    for example Amber), which keeps the key outside this app.
  - *Back up key*: shows or copies the `nsec` of the automatic account.
  - *Switch back to the automatic account*.
- Sharing a list offers the same choice inline: *Share as: Automatic account ▾ /
  Use my own account…*

**Share (owner)**
1. List menu → *Share list…*
2. The first time only, a short explainer: "Anyone who scans this code can see
   and change this list. Updates go end-to-end encrypted through
   shopping.openintents.org." The *Relay* link opens the relay setting, and
   *Share as* chooses the account.
3. A QR code appears, with *Copy link* and *Send link* (Android share sheet).
   The list is live from this moment.

**Join (member)**
- Scan the QR code with the phone's camera app. It opens a link that goes
  straight into the app (see section 5). Alternatively, open a shared link, or tap
  *Join shared list* in the drawer.
- A confirmation shows the list name and who shared it, for example *Join
  "Groceries" from Anna?* → the list appears and fills in within a second or
  two. The joiner's automatic account is created here if needed.

**While shared**
- A small cloud icon in the app bar shows the state: synced, syncing, offline
  (changes are queued) or error.
- Items show who changed them last, when that isn't you ("checked by Anna").
- *Share list…* shows the same QR again, so more people can join.
- *Stop sharing* keeps the local copy and stops syncing. It cannot un-share
  data others already have.

For the default user there are still no keys, no relay URLs and no sign-up.

## 2. Protocol (v1)

Two kinds of keys:

- **User key (identity):** the automatic or own account. It **signs** the events
  a user publishes, so every change is attributed to an author.
- **List secret (group key):** a random 32 bytes `S` per shared list. It
  **encrypts** list content and addresses events. Everyone who scanned the QR
  code has it.

### Derived from the list secret

| Derived | How | Purpose |
|---|---|---|
| List key pair | `lsk = S` (secp256k1, retried if out of range), `lpk = xonly(lsk·G)` | Only used to derive the conversation key |
| Conversation key | NIP-44 v2 `get_conversation_key(lsk, lpk)` | Encrypts all content (NIP-44 v2). The MAC makes content from anyone without `S` fail to decrypt, so it gets ignored. |
| List tag | `h = hex(HMAC-SHA256(S, "list"))` | Relay filter for this list, opaque to the relay |
| Item address | `d = hex(HMAC-SHA256(S, "item:" + itemUuid))` | Item ids the relay cannot link to names |

### Events
- **Item** = one addressable (parameterized replaceable) event, kind `3xxxx`.
  Pick an unused number and register it in nostr-protocol/nips.
  - Signed by the **user key**; `tags`: `["d", d]`, `["h", h]`.
  - `content` = NIP-44 encryption, with the list conversation key, of JSON such as
    `{"v":1,"id":"<uuid>","name":"Milk","qty":"2","units":"l","price":119,"status":1,"prio":null,"tags":null,"note":null,"hlc":"<hybrid logical clock>"}`.
  - The relay keeps the latest version per (author, item). Reading the whole
    list is `REQ {"kinds":[K], "#h":[h]}`. The client keeps, per item, the
    version with the newest `hlc` across all authors.
- **List meta** (name, theme, the sharer's display name): the same kind, with
  `d = HMAC(S, "meta")`.
- **Delete**: publish a tombstone `{"v":1,"id":…,"deleted":true,"hlc":…}` with a
  NIP-40 `expiration` tag of about 60 days, so relays eventually drop it.
- **Display names:** the npub of the event author, resolved to a name via the
  author's kind-0 profile if it has one. Otherwise use the nickname they set in the
  app, which is sent encrypted inside the list meta and member events.

### Merge rules
- Newest wins per item, by `hlc` (sub-second ordering), with the author pubkey and
  then the event id as tie-breakers.
- Checking an item off is simply a status change on the item event, which
  gives offline-safe "last change wins" semantics.
- Local edits go into an **outbox** table and are published when online; an
  item is marked synced when the relay replies with `OK`.
- Only events that decrypt with the list conversation key count. Signatures are
  verified as usual.

### The share link and QR code
```
https://shopping.openintents.org/j#v=1&s=<base64url S>&r=wss%3A%2F%2Fshopping.openintents.org&n=Groceries&o=<npub of sharer>
```
- The secret sits in the **fragment**, which browsers never send to the
  server, so even the landing page never sees it.
- `r` can be repeated for several relays (bring your own relay). The owner's
  relays travel in the QR code so everyone uses the same ones.
- `o` (optional) shows the sharer's account in the join dialog.
- As a QR code this is about 190 characters, which scans easily.

## 3. Relays

- **Default: `shopping.openintents.org`**, running strfry or nostr-rs-relay:
  - A write policy that only accepts kind `K` (plus kind 0 profiles), content
    under 4 KB, and rate limits per IP and per pubkey.
  - A retention policy, for example dropping events untouched for 12 months.
  - A NIP-11 info document.
  - The same host serves `/.well-known/assetlinks.json` (for the Android App
    Link) and a tiny `/j` landing page ("Install OI Shopping List").
- **Bring your own:** Settings → *Relays* lets users edit the list, which
  defaults to `wss://shopping.openintents.org`. The setting is used when a new
  share is created. Joining always uses the relays in the QR code. Users with
  their own account can import that account's relay list (NIP-65).
- The client connects to all relays of the list, publishes to all of them, and
  merges what it reads (deduplicated by event id).

## 4. App architecture

| Piece | Where | Notes |
|---|---|---|
| Crypto: secp256k1 BIP-340 sign/verify, ECDH, NIP-44 v2 (HKDF, ChaCha20, HMAC-SHA256), bech32 (npub/nsec, NIP-19) | new `nostr/crypto` package, pure Kotlin | Small and no JNI, so no per-ABI native libraries. Verified against the official BIP-340, NIP-44 and NIP-19 test vectors. Android's own ChaCha20 needs API 28, and minSdk is 21, so it is bundled. |
| Account | `nostr/Account` | `Signer` interface with two implementations: `LocalKeySigner` (the automatic account or a pasted nsec, stored encrypted with an Android Keystore key and excluded from backups) and `Nip55Signer` (external signer app such as Amber, via intents/content resolver). |
| Nostr client (NIP-01 `REQ` / `EVENT` / `EOSE` / `OK` / `CLOSE`) | `nostr/RelayClient` | WebSocket via OkHttp, about 300–400 KB after R8. Reconnects with backoff and only runs while the app is visible. |
| Sync engine | `sync/ListSync` | Maps provider rows to item events and back, owns the outbox and HLC, and applies the merge rules. Signs through `Signer`, so both account kinds work. Pure logic with a fake relay, so it is JVM-testable. |
| Data | DB migration | Adds `contains.remote_id` (UUID), `modified_hlc` and `modified_by`, plus a `shared_lists(list_id, secret, relays, joined_at)` table and an `outbox` table. The secret is stored encrypted with the Keystore key. |
| QR code | show: `zxing-core` encoder (R8 keeps only the encoder); scan: the system camera plus an App Link | No camera code or ML Kit, so the app stays FOSS/F-Droid friendly. In-app *Scan* hands off to an installed scanner (for example Binary Eye), with *Paste link* as a fallback. |
| UI | Compose: share dialog with QR, join confirmation, sync badge, Nostr account and relay settings | |

**Sync triggers**
- Connect and subscribe when the app comes to the foreground; disconnect about
  30 seconds after it goes to the background.
- Changes from the widget or other apps are picked up on the next foreground,
  or immediately through the provider's change notification while connected.
- Background sync (JobScheduler, with no extra library) is optional for v1.1.

## 5. Permissions, links and size

- **`INTERNET`** is added to the manifest. The store descriptions change from
  "does not require the internet permission" to "only goes online when you
  share a list".
- App Links need `autoVerify` on the `/j` path plus `assetlinks.json` with the
  Play and F-Droid signing certificates, which differ, so list both.
- NIP-55 signers need a `<queries>` entry for the `nostrsigner:` scheme
  (Android 11+ package visibility).
- Estimated APK growth: OkHttp about 350 KB, zxing encoder about 60 KB, crypto
  about 50 KB. That is around 0.45 MB on top of 3.6 MB.

## 6. Security and privacy properties (to state in the UI and the docs)

- Relays see: which pubkeys write to a list tag, how many items it has, when they
  change, and IP addresses. They never see item names, prices or list names.
  - **Automatic account:** the pubkey is a fresh, unlinked identity.
  - **Own account:** the relay (and anyone reading it) can see that this public
    identity takes part in *some* shared list. The UI mentions this when the user
    picks their own account.
- **Whoever has the QR code or link can read and write the list until it is
  re-keyed.** Treat it like a password.
- No forward secrecy, and removed members can't be locked out in v1. The
  workaround is *Stop sharing* followed by *Share list…* again, which creates a
  new secret and QR code.
- Events are authenticated twice: by a valid signature of the author, and by
  NIP-44 decryption with the list key. Strangers cannot inject changes.
- The automatic account's key never leaves the device unless the user backs it
  up. Losing it only loses the name on past changes: rejoining a list just needs
  the QR code again.

## 7. Delivery phases

1. **Spec and crypto core.** Write the protocol doc with the kind number. Add
   BIP-340, NIP-44 and NIP-19 in pure Kotlin with the official test vectors as
   JVM tests.
2. **Accounts.** The automatic account, nsec import and export, the NIP-55
   signer, Keystore storage, and the settings screen.
3. **Sync engine and data.** DB migration, outbox, HLC, merge. Test two or three
   simulated users against an in-memory relay: offline edits, conflicts,
   deletions, joining late, one user with their own account.
4. **Relay client.** OkHttp WebSocket plus reconnect. Add an integration test in
   CI against a throwaway strfry container.
5. **UI.** Share dialog with QR code and *Share as*, join by link or App Link,
   sync badge, "changed by", relay settings, stop sharing. Localizable strings.
6. **Infrastructure.** Deploy `shopping.openintents.org` (relay with write
   policy, `assetlinks.json`, `/j` landing page) and add monitoring.
7. **Beta.** Test on two real phones: list on one, check off on the other,
   offline then back online, automatic account plus own account, then release.

## 8. Later (explicitly out of scope for v1)

- Several shared lists per device (the model already supports it; the UI limits it to one).
- Inviting by npub instead of a QR code: send the list link as a NIP-17
  private message. This becomes possible now that every user has an account.
- Member management and revocation: re-key and deliver the new secret only to
  the remaining members' npubs via NIP-17, or use NIP-EE/MLS groups.
- NIP-46 remote signers (bunkers).
- Background push via a relay-to-UnifiedPush bridge.
- A web client using the same protocol.

## 9. Open questions for the maintainer

1. Who runs `shopping.openintents.org`, and what retention period should it use?
2. Are the Play and F-Droid signing certificates available for `assetlinks.json`?
3. Should the automatic account publish a kind-0 profile with the nickname? That's
   more convenient but more public. The default proposed here is no: the nickname
   stays inside the encrypted list.
4. Should there be interoperability with an existing Nostr list or tasks app,
   which would mean reusing its event format instead of a new kind?
