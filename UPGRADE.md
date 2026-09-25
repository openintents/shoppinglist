# OI Shopping List — Google Play Upgrade Notes

Working doc for the upgrade effort. Goal: bring the app up to the **minimum
requirements of Google Play**, while keeping the APK/AAB **as small as possible
with as few dependencies as possible**.

Written 2026-06-09 against commit `81bc3c2` (versionName 2.2.1, versionCode 100221).

---

## 0a. RELEASE 2.3.0 PREP (2026-09-24)

versionName 2.3.0 / versionCode 100230. Release notes: `res/raw/recent_changes.txt`,
`src/play/play/en-US/whatsnew`, `fastlane/.../changelogs/100230.txt` (F-Droid).

Done in this pass (source audit, all components):
- Compose UI: stale-refresh race, last-used list persisted/validated, catalogue item
  reuse on add (no duplicates), refresh on resume, CSV import policy KEEP, price
  validation, rotation-safe dialogs/input, settings honored (hidechecked, showprice,
  capitalization), all strings translatable (`values/strings_compose.xml`), a11y labels,
  list themes shared with the legacy UI (stored as "1"/"2"/"3").
- CSV: persistable SAF grants, crash fixes (HandyShopper import/export, non-list URIs),
  export truncates ("wt").
- Provider/DB: SQL fixes for `containsfull/#` and `itemstores/#`, no selection-arg SQL
  concatenation, cursor leaks, WAL off so the backup agent captures all data.
- Legacy UI: move/delete by id (rotation-safe), cursor-reload crashes, first-run layout
  chooser, edge-to-edge opt-out for all full-screen legacy activities (values-v35).
- Widget receiver no longer exported; per-widget settings PendingIntents.
- Removed dead ShoppingListWear/, src/playInternet/, Ant build.xml, Travis CI,
  aTrackDog meta-data. CI is now `.github/workflows/build.yml` (unit tests, tstore APK,
  play AAB, lint).

Feature parity with the legacy UI (checked before release 2.3.0). The legacy screens
are no longer in the manifest; their sources (ui/ShoppingActivity.java, ui/widget/
ShoppingItemsView.kt, ui/dialog/*, share/*, theme/*, provider/Alert|Location|Hardware|Tag,
PickItemsActivity, AddLocationAlertActivity, ...) can be deleted:

| Legacy feature | New UI |
|---|---|
| Add / check / edit / remove items, undo | yes (tap = check, long-press = edit) |
| Search / add in the action bar | yes, setting "Search/add items in action bar" |
| 10 sort orders, per-list sort, Pick items sort, list order | yes (same settings) |
| Store filter / tag filter ("use_filters") | yes, chips above the list |
| Mark all / unmark all / clean up with undo, shake to clean up | yes |
| Move / copy / delete item permanently | yes (edit dialog) |
| Per-item stores (stocks, aisle, price) | yes (edit dialog -> Stores..., legacy screen) |
| Priority subtotal, show price/tags/units/quantity/priority | yes |
| Themes incl. "use for all lists", font size | yes |
| Keep screen on, orientation, reset quantity, completion scope | yes |
| Convert CSV (HandyShopper, encoding, policy), About | yes (menu) |
| Items from other apps (SEND text, INSERT_FROM_EXTRAS), list shortcuts, widget | yes |
| Other apps picking items (PICK/GET_CONTENT item) | removed with the legacy screen (2.3.0) |
| Quick edit mode (tap quantity/priority) | replaced by long-press edit |
| Fast scroll | yes ("Fast scrolling" setting) |
| - | new: compact view, barcode scanning (scanner app + Open Food Facts) |
| Location alerts (OI Locations), GTalk sharing, Wear sync, theme packs from other apps, Market add-on links | dropped: the partner apps/services no longer exist |

Release channels: F-Droid (tag + `.fdroid.yml`), GitHub releases for Obtainium and
Zapstore (`.github/workflows/release.yml`, needs the signing secrets and
`ZAPSTORE_SIGN_WITH`), Google Play (`bundlePlayRelease`, uploaded by hand).

## 0b. ARCHITECTURE DIRECTION (updated 2026-06-09)

After the Google-Play upgrade + Kotlin migration, the project pivoted to:
**Jetpack Compose UI + extracted, tested business logic. Target: Android only**
(Compose Multiplatform was considered and declined — the app's core is Android-
specific: ContentProvider as public API, widget, backup agent, Tasker automation).

DONE (green, tested):
- Kotlin migration: 62/63 main+lib Java files + both built flavors are now Kotlin.
  ONLY `ui/ShoppingActivity.java` (3324 lines) remains Java — automated agent
  conversion is blocked by a content-filter false-positive on that one large file.
  It interops fine with the Kotlin code. Convert it later via Android Studio's
  Code > Convert Java File to Kotlin (the right tool at that size), OR replace it
  screen-by-screen with Compose (preferred — see below).
- Compose + test toolchain: Kotlin compose-compiler plugin, Compose BOM 2024.09.03,
  material3, activity-compose, lifecycle-viewmodel/runtime-compose; Robolectric +
  androidx.test + coroutines-test for JVM unit tests.
- Architecture (the pattern, repeated per slice):
  * `data/ShoppingRepository` (interface) + `data/ProviderShoppingRepository`
    (ContentProvider impl) + `data/Models.kt` — business-logic boundary; returns
    ShoppingListInfo/ShoppingItem domain models, no Cursors in the UI.
  * pure logic in `data/` (computeTotals, arrangeItems, PriceConverter) — no Android.
  * `ui/compose/ShoppingListViewModel` — constructor-injected repo + injectable
    dispatcher + ViewModelProvider.Factory; StateFlow state, IO off-main.
  * `ui/compose/ShoppingListScreen` (stateless, hoisted) + `ShoppingListRoute` +
    `ComposeShoppingActivity` — hosted SEPARATELY (exported=false, NOT the launcher)
    so the legacy app keeps working during migration.
- Compose UI DONE (all tested, green): view items · add · check off (strikethrough) ·
  switch/create lists (drawer) · edit item (name/quantity/price/units/priority/tags
  via ItemEdit) · remove · list totals (to-buy/bought) · sort/hide-checked/cleanup ·
  store management (add/remove) · per-store prices · store filter on the main screen
  (FilterChips; selected store's prices flow into list + totals) · **Settings screen**
  (all 17 prefs, writing the same SharedPreferences keys the legacy getters read) ·
  **CSV import/export via the Storage Access Framework** (closes the scoped-storage gap).
- Tests: **48 passing**. Pure-JVM: PriceConverter, ShoppingTotals, ItemArrangement,
  ShoppingListViewModel + SettingsViewModel (via fakes). Robolectric (real provider+
  SQLite + real prefs): ShoppingRepository, SettingsRepository (incl. a faithfulness
  test that legacy getters see Compose writes). Run: `./gradlew :ShoppingList:testPlayDebugUnitTest`
- The Compose UI is now a SECOND launcher entry ("OI Shopping (new UI)", exported)
  for on-device testing. The production swap is a one-line manifest change (move the
  MAIN/LAUNCHER filter off the legacy .ShoppingActivity) — do it AFTER a device pass.

NOT ported (intentionally — auxiliary / Android-glue, reached via their own intents
and still working): home-screen widget + config, Tasker automation, GTalk sharing
(largely dead), and list "themes" (loads fonts/colors from other installed apps —
Android-specific, low value). The legacy ShoppingActivity remains for these + as the
current launcher until the Compose UI is device-verified and promoted.
- Dependencies kept minimal as requested: drawer/menus/dialogs are all material3;
  DI + testability use the lifecycle + coroutines libs already present. NO Hilt,
  Navigation-Compose, or Accompanist. (Compose itself is the one accepted size
  trade vs. the original "smallest app" goal; R8 strips unused parts in release.)

NEXT (remaining migration work — needs product decisions and/or device testing):
- Stores + per-store prices screen (touches stores/itemstores tables) — bigger slice.
- Preferences screen in Compose (font size, per-list sort, completion behavior…).
- Themes, sharing, automation/Tasker, CSV import/export UI, widget config — these
  stay in the legacy/Android layer for now; decide per-feature whether to port.
- Make the Compose UI reachable / eventually the launcher: only after the above
  reach parity, then delete the matching legacy code and retire `ShoppingActivity.java`
  last. Until then `ComposeShoppingActivity` is exported=false (not user-reachable).
- Compose UI tests (androidx.compose.ui:ui-test-junit4 is wired up) once screens settle.
- Device smoke-test the Compose flows on a real emulator/device.
- Still pending from Phase 1: scoped-storage audit in `convertcsv`, remove
  aTrackDog/permissionGroup/dead Wear+playInternet, androidTest manifest cleanup,
  targetSdk 35→36, release signing config.

## 0. PROGRESS (updated 2026-06-09)

Branch: `upgrade/google-play-androidx`. **The app now builds against SDK 35 with
AndroidX on a modern toolchain.** Release APK shrank from 7.0 MB (debug) to
**2.65 MB** (R8 + resource shrinking).

**DONE — builds green** (`./gradlew :ShoppingList:assemblePlayDebug`,
`assemblePlayRelease`, `assembleTstoreRelease` all SUCCESSFUL with JDK 17):
- Toolchain: AGP `4.0.0 → 8.7.3`, Gradle `6.5 → 8.9`, Java `1.8 → 17`.
- Repos: dropped dead `jcenter()`, use `google()` + `mavenCentral()` (+ jitpack for the distribution lib).
- New `gradle.properties` (useAndroidX, enableJetifier, nonTransitiveRClass).
- New `local.properties` (sdk.dir, gitignored).
- SDK levels: compile/target `28 → 35`, minSdk app `16 → 21`, library `4 → 21`.
- AndroidX migration of the 6 main + 1 distribution java files + `activity_shopping.xml`
  (`android.support.* → androidx.*`; Snackbar → `com.google.android.material`).
  Kept `androidx.appcompat:appcompat` + `com.google.android.material:material`.
  Jetifier rewrites the JitPack `distribution:3.0.2` lib cleanly.
- AGP 8 namespaces added to both modules; `package=` removed from all manifests.
- `android:exported` added to all 8 components with intent-filters + a `tools:node="merge"`
  override for the lib's `about.About` activity (required API 31+).
- Non-final R fix: `AddLocationAlertActivity` switch→if/else (AGP 8 non-const R.id).
- Runtime fix: `FLAG_IMMUTABLE` added to all 5 widget `PendingIntent`s (API 31+ crash).
- androidTest deps moved to `androidx.test:*` (imports migrated).

**NOT YET DONE (next session):**
- **Behavioral audits for the API 28→35 jump** (build passes ≠ runs correctly):
  - **Scoped storage** in `convertcsv/*` (CSV import/export likely uses raw file
    paths / `WRITE_EXTERNAL_STORAGE`) — biggest risk. Needs SAF.
  - **Backup**: manifest still has `backupAgent` + Google backup `api_key` meta-data — verify/replace.
  - **POST_NOTIFICATIONS** (API 33+) if location alerts post notifications.
- **androidTest manifest** (`src/androidTest/AndroidManifest.xml`) still has stale
  `package=` + ancient `<uses-sdk minSdk 3 / target 15>` — clean before running instrumentation tests.
- **Dependency minimisation (the "fewest deps" goal):** drop `com.google.android.material`
  by replacing the 6 Snackbar sites; vendor the ~4 distribution classes and drop the
  JitPack dep + `jitpack.io` repo + `enableJetifier`. End state = appcompat only.
- **Cleanup**: remove `aTrackDog` meta-data, the deprecated `permissionGroup` on the
  two custom perms, dead `ShoppingListWear/` + `src/playInternet/`.
- **Bump targetSdk 35 → 36** once the above is stable (next Play ratchet ~Aug 2026).
- **Signing**: release builds are currently unsigned (no `build-private.properties`).
- Run the app on a device/emulator and smoke-test (drawer, add/check items, undo
  snackbar, widget, CSV, themes).

---

## 1. Current state (the starting point)

### Build system (all badly outdated)
| Thing | Current | Notes |
|---|---|---|
| Android Gradle Plugin | `4.0.0` | from 2020; cannot compile against modern SDKs |
| Gradle wrapper | `6.5` | needs to move in lock-step with AGP |
| `compileSdkVersion` | `28` | Android 9 |
| `targetSdkVersion` | `28` | **this is what Google Play rejects** |
| `minSdkVersion` | `16` (app) / `4` (library) | Android 4.1 / 1.6 |
| Java source/target | `1.8` | |
| Repositories | `jcenter()` + `google()` + `jitpack.io` | **jcenter() is dead/read-only**, must go |
| Support libraries | `com.android.support:*` `28.+` | **pre-AndroidX** — the central migration problem |
| Installed locally | SDK platforms 31–36, build-tools 30–36, JDK 8/11/17/20/21 | use JDK 17 or 21 |

There is **no `gradle.properties`** file at all — it will need to be created
(for AndroidX flags, JVM args, etc.).

### Module layout
`settings.gradle` includes only two modules:
```
:ShoppingListLibrary   (com.android.library)  — contract, sync iface, utils (4 java files)
:ShoppingList          (com.android.application) — the app (73 java files)
```
- **`ShoppingListWear` exists on disk but is NOT in `settings.gradle`** — it is
  already dead code. It pulls in `play-services-wearable` + `wearable`. Leave it
  out / delete it. Do **not** re-add it (keeps deps minimal).
- All code is **Java** (no Kotlin). 0 Kotlin files.

### Product flavors (dimension `market`)
- `play` — the FOSS-friendly flavor. `OptionalDependencies` is a no-op subclass
  of `BaseOptionalDependencies` (no Google deps).
- `tstore` — same no-op, just flips a `hideMarketLink` meta-data flag. F-Droid
  builds this flavor.
- `playInternet` — **source dir exists but flavor is NOT declared in
  build.gradle** (only `play` and `tstore` are). It contains the only Google Play
  Services code (`GooglePlaySyncSupport` using `com.google.android.gms.wearable`).
  It is currently dead. **Keep it dead / delete it** — it is the only thing in the
  app pulling Google Play Services, and dropping it keeps the build FOSS and small.
- Conclusion: the app **already has no Google Play Services / gms dependency** in
  the active `play`/`tstore` flavors. Good — nothing to remove there.

### Dependencies actually compiled into the app
```
implementation project(':ShoppingListLibrary')
implementation 'com.github.openintents:distribution:3.0.2'   // JitPack, built against support-lib
implementation 'com.android.support:appcompat-v7:28.+'
implementation 'com.android.support:design:28.+'
// + androidTest: junit, support-test runner/rules, espresso-core/-contrib
```

### Support-library usage scope (the AndroidX migration surface — it's SMALL)
Only **6 main-source java files** + 2 androidTest files import `android.support.*`:
```
ui/widget/QuickSelectMenu.java
ui/LayoutChoiceActivity.java
ui/ShoppingActivity.java
ui/widget/ShoppingItemsView.java
distribution/DistributionLibraryFragmentActivity.java
convertcsv/common/ConvertCsvBaseActivity.java
```
Imports in use:
```
android.support.v7.app.*        (AppCompatActivity, ActionBar, AlertDialog)
android.support.v7.widget.*     (RecyclerView/Toolbar-ish)
android.support.v4.view.*       (ViewPager / MenuItemCompat)
android.support.v4.widget.*
android.support.v4.content.*    (ContextCompat / LocalBroadcastManager)
android.support.design.widget.* (TabLayout / FloatingActionButton etc.)
android.support.annotation.*
```
XML touch-points: `res/values/styles.xml` (parents `Theme.AppCompat`,
`Widget.AppCompat.ActionBar`) and `res/layout/activity_shopping.xml`.

### The JitPack dependency — the main external risk
`com.github.openintents:distribution:3.0.2` (the "OI Distribution" library:
EULA / new-version / "download other OI app" dialogs). It is compiled against the
**old support library**. After migrating the app to AndroidX this will either:
- need **Jetifier** (`android.enableJetifier=true`) to be rewritten on the fly, or
- be **forked/vendored/replaced**. The app only uses a handful of classes:
  `DistributionLibraryFragmentActivity`, `DownloadAppDialog`, `DownloadOIAppDialog`,
  plus `EulaActivity` / `NewVersionActivity` (referenced in the manifest).
  A local copy of `DistributionLibraryFragmentActivity.java` already exists in
  `org/openintents/distribution/` — so partial vendoring is already happening.

**Recommendation for minimal deps:** vendor the few needed distribution classes
into the app (or strip the feature) and **drop the JitPack dependency entirely**.
That also lets `jitpack.io` be removed from repositories. If that's too much work
for the first pass, keep it and rely on Jetifier, then remove later.

---

## 2. What Google Play actually requires (the target)

Google Play enforces a **target API level** floor for new apps and updates,
ratcheted every year (~Aug 31):
- Since Aug 2024: `targetSdk >= 34`
- **Since Aug 31, 2025: `targetSdk >= 35` (Android 15)** ← current floor
- ~Aug 2026 (upcoming): expected `targetSdk >= 36` (Android 16)

**Decision:** target **35** at minimum; prefer **36** (SDK 36 is installed) to stay
ahead of the next ratchet. `compileSdk` must be `>=` target, so `compileSdk = 36`.

Google Play does **not** mandate a `minSdk`. For smallest size + modern AndroidX,
bump `minSdk` up from 16. AndroidX baseline is 21 (Android 5.0) for most artifacts
now (many require 19/21). **Recommend `minSdk = 21`** (drops legacy multidex,
legacy drawable buckets, ~all `-v11`/`-v9`/`-v5` qualified resources become
removable later). 23 is also reasonable. Confirm against analytics if any exist;
otherwise 21 is a safe, dependency-friendly choice.

Other Play requirements to verify late in the process:
- App must ship as **AAB** (Android App Bundle) for Play upload, not APK. F-Droid
  still wants the APK from the `tstore` flavor — keep both working.
- 64-bit requirement: N/A (no native code here).
- Declared permissions / sensitive APIs: this app only declares its own custom
  `READ/WRITE_PERMISSION` (+ optional INTERNET in the dead playInternet flavor).
  The custom permissions use `permissionGroup="android.permission-group.PERSONAL_INFO"`
  which is **deprecated** — drop the `permissionGroup` attribute (groups are ignored
  for custom perms on modern Android).

---

## 3. Migration plan (suggested order)

Do this incrementally and keep the project compiling at each step. Use
`./gradlew :ShoppingList:assemblePlayDebug` as the fast feedback loop.

### Step 0 — toolchain
- Use **JDK 17** (or 21). `export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64`.
  (JDK 20 is installed but is a non-LTS; AGP 8 officially wants 17.)
- Create `local.properties` with `sdk.dir=/home/friedger/Android/sdk` (gitignored).
  (Only `template of local.properties` exists today.)

### Step 1 — Gradle + AGP bump (do FIRST, expect breakage)
- AGP `8.5+` (pick a recent 8.x) → requires Gradle `8.7+`. Update
  `gradle/wrapper/gradle-wrapper.properties` (`distributionUrl` + refresh
  `distributionSha256Sum`).
- Root `build.gradle`: replace `classpath 'com.android.tools.build:gradle:4.0.0'`,
  **remove every `jcenter()`**, replace with `mavenCentral()`. Keep `google()`.
  Keep `jitpack.io` only if still using the distribution lib.
- AGP 7+ no longer allows `compileSdkVersion`-via-ext the old way is fine, but
  note `buildToolsVersion` is auto-selected now — the Wear module references
  `rootProject.ext.buildToolsVersion` which is **undefined** (another reason Wear
  is dead; ignore it).

### Step 2 — AndroidX migration
- Add to a new `gradle.properties`:
  ```
  android.useAndroidX=true
  android.enableJetifier=true     # while the distribution JitPack dep remains
  org.gradle.jvmargs=-Xmx2048m
  ```
- Replace support deps in `ShoppingList/build.gradle`:
  ```
  com.android.support:appcompat-v7  ->  androidx.appcompat:appcompat:1.7.0
  com.android.support:design        ->  com.google.android.material:material:1.12.0
  ```
- Run **Android Studio → Refactor → Migrate to AndroidX**, or hand-edit the 6+2
  files: `android.support.v7.app` → `androidx.appcompat.app`,
  `android.support.v4.*` → `androidx.core.*` / `androidx.fragment.*` /
  `androidx.viewpager.widget` / `androidx.localbroadcastmanager`,
  `android.support.design.widget` → `com.google.android.material.*`,
  `android.support.annotation` → `androidx.annotation`.
- `LocalBroadcastManager` is deprecated in AndroidX — it still works via
  `androidx.localbroadcastmanager:localbroadcastmanager`, but consider replacing
  later for fewer deps.
- Update androidTest deps: `com.android.support.test:*` → `androidx.test:*`,
  espresso → `androidx.test.espresso:*`, runner
  `android.support.test.runner.AndroidJUnitRunner` →
  `androidx.test.runner.AndroidJUnitRunner` (in `defaultConfig` + manifests).

### Step 3 — SDK levels
- Root `ext`: `compileSdkVersion = 36`, `targetSdkVersion = 36`.
- App `minSdkVersion 16 -> 21`; Library `minSdkVersion 4 -> 21`.
- Remove the `supportVersion` / `playServicesVersion` / `supportWearableVersion`
  ext entries once nothing references them.

### Step 4 — manifest / target-API behavior fixes (API 28 → 36 is a big jump)
Things that commonly break going past API 30/31/33:
- **`android:exported`** must be explicit on every `<activity>/<service>/<receiver>`
  that has an `<intent-filter>` (required since API 31). Several activities/receivers
  here have intent-filters without `exported` — add `android:exported="true"`
  (or `false` where not externally launched). The provider and ConvertCsvActivity
  already set it; audit the rest (ShoppingActivity, ShoppingListsActivity,
  share/location/automation activities, `CheckItemsWidget` receiver,
  `AutomationReceiver`, widget config activity).
- **`PendingIntent`** must specify `FLAG_IMMUTABLE`/`FLAG_MUTABLE` (required API 31+).
  Check the widget (`widgets/CheckItemsWidget`) and any notification/alarm code.
- **Scoped storage** (API 29+): the CSV import/export (`convertcsv`) likely uses
  raw file paths / `WRITE_EXTERNAL_STORAGE`. Move to Storage Access Framework
  (`ACTION_OPEN_DOCUMENT` / `ACTION_CREATE_DOCUMENT`). Audit `convertcsv/*`.
- **Backup**: manifest declares `backupAgent="ShoppingBackupAgent"` + a Google
  backup `api_key` meta-data. The Google backup transport API key is legacy;
  consider switching to modern auto-backup rules and dropping the api_key. At
  minimum verify it still builds.
- Remove dead **`aTrackDog` meta-data** and other 2013-era cruft.
- Drop `permissionGroup="android.permission-group.PERSONAL_INFO"` from the two
  custom `<permission>` decls.
- Foreground-service / notification-channel / `POST_NOTIFICATIONS` (API 33+): only
  relevant if notifications are used (location alerts?). Audit
  `ui/AddLocationAlertActivity` + any alarm/notification code.

### Step 5 — distribution dependency decision
- Either keep `enableJetifier=true` and the JitPack dep (fast path), **or** vendor
  the ~4 needed classes and delete the dep + `jitpack.io` repo + jetifier (smaller,
  cleaner — preferred end state).

### Step 6 — shrink (smallest-APK goals)
- Already has `minifyEnabled true` + ProGuard on release. Verify R8 (default in
  AGP 8) keeps rules in `ShoppingList/proguard.cfg`. Enable `shrinkResources true`.
- `minSdk 21` lets you delete legacy resource buckets later (`-ldpi`, `*-v5/v9/v11`).
- 40 translations are fine to keep (cheap). Two bundled TTF fonts
  (`assets/fonts/AnkeHand.ttf` 124KB, `Crysta.ttf` 16KB) — keep unless theme work
  drops them.
- Build an **AAB** for Play (`bundlePlayRelease`) — per-device splitting shrinks
  install size for free.

---

## 4. Open questions / things to verify next session
- Does `com.github.openintents:distribution:3.0.2` resolve at all today (JitPack
  still up)? If not, vendoring becomes mandatory, not optional.
- Confirm exact Google Play target floor at upload time (35 enforced now; 36 may be
  enforced by the next August window — 36 is the safe choice).
- Audit `convertcsv` for storage-permission / file-path usage (biggest behavioral
  risk in the API-level jump).
- Audit all manifest components for missing `android:exported`.
- Decide minSdk: 21 (recommended) vs higher.
- Decide whether to delete `ShoppingListWear/`, `src/playInternet/`, and the
  `tstore` vs `play` split, or keep them. For minimal deps: delete Wear +
  playInternet; keep `play` (Play) and `tstore` (F-Droid) flavors.

## 5. Quick command reference
```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
echo "sdk.dir=/home/friedger/Android/sdk" > local.properties   # gitignored
./gradlew :ShoppingList:assemblePlayDebug      # fast compile loop
./gradlew :ShoppingList:bundlePlayRelease      # AAB for Google Play
./gradlew :ShoppingList:assembleTstoreRelease  # APK for F-Droid
./gradlew lint
```
