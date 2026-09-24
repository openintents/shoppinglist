# OI Shopping List

![logo](ShoppingList/src/main/res/drawable-hdpi-v5/ic_launcher_shoppinglist.png)

Free, ad-free, open source Android application in 40 languages since 2009.

OI Shopping List is a powerful application which makes it easy to create and manage checklists for your daily shopping.

OI Shopping List makes it easy to add items to a list, check the item off, and remove it from the list.
The application can track separate prices for each store that you use, and features a high level of customization.
Font size, sort order, list columns, and list cleanup behavior are all settings controlled by the user.
All of these features are brought together in a crisp layout that is customizable through a variety of themes.

## Distribution Channels / App stores

* **[F-Droid](https://f-droid.org/en/packages/org.openintents.shopping/)** [F-Droid meta data](https://gitlab.com/fdroid/fdroiddata/-/blob/master/metadata/org.openintents.shopping.yml)

* **[Google Play](https://play.google.com/store/apps/details?id=org.openintents.shopping)**

* **[Obtainium](https://github.com/ImranR98/Obtainium)**: add the app with the URL
  `https://github.com/openintents/shoppinglist` (APKs from the GitHub releases)

* **[Zapstore](https://zapstore.dev)**: search for "OI Shopping List"

## Releasing

1. Bump `versionName`/`versionCode` in `ShoppingList/build.gradle`, add
   `fastlane/metadata/android/*/changelogs/<versionCode>.txt` (max. 500 characters)
   and point `release_notes` in `zapstore.yaml` at it.
2. Push a tag `v<versionName>`. The *Release* workflow builds and signs the APK,
   creates the GitHub release (Obtainium) and publishes to Zapstore.
   F-Droid picks up the tag by itself (see `.fdroid.yml`).
