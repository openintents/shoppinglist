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

## Ishy Fork

This fork (`ishy/app-id-rename` branch) is a personal build for side-by-side installation with the original app.

Changes from upstream:

- App ID changed to `org.openintents.shopping.ishy`
- Display name changed to "OI Shopping List Ishy"
- Removed the incomplete "New UI" (Compose) launcher icon
- Fixed a first-run loop where selecting a default layout would return to the layout selection screen instead of proceeding to the main view
- Moved the Capitalization setting to Advanced Settings where it is visible
- Centered the undo popup and added a preference to disable it
