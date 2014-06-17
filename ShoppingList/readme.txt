 ****************************************************************************
 * Copyright (C) 2007-2012 OpenIntents.org                                  *
 *                                                                          *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 *                                                                          *
 *      http://www.apache.org/licenses/LICENSE-2.0                          *
 *                                                                          *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 ****************************************************************************

The OpenIntents Shopping list lets you keep track of your shopping items. 
You can also use it for other kinds of check lists, for example for ToDo 
lists or party guest lists.

To obtain the current release, visit
  http://www.openintents.org

----------------
release 2.0-rc2
date: ??

- Search/add filters the list in-place instead of using the auto-complete popup.

----------------
release 2.0-rc1
date: 2014-06-14

- Holo UI using action bar and navigation drawer.
- Undo support for marking items and Clean List.
- Search / add items via action bar.
- Optionally remember a different sort order for each list.

----------------
release 1.8
date: 2014-02-11

- Add OI Shopping List to the "share" menu of other apps to import text lists. (RuthAlk)
- Unmark All context menu item
- Avoid overlapping text of price and description
- Fix issues with Add button sometimes not adding.
- Fix text color on filter buttons.

----------------
release 1.7
date: 2012-10-16

- Sort list of lists (patch by Stanley F. for Google Summer of Code 2012)
- Set cursor to end in "rename list" dialog (patch by Alex Yaremenko)
- Fix list shortcut when filters are active.
- Fix Stores button in Edit Items dialog (issue 510)
- Filter and QuickEdit menus make better use of screen space on pre-ICS devices.

----------------
release 1.6
date: 2012-01-29

- PLEASE READ THIS: The font size has been adjusted for high-density devices. Please use menu > settings > font size to set the desired size (see issue 427)

- Filter by store or tags. (Enable in advanced preferences.)
- Fast scrolling support. (Enable in advanced preferences.)
- Copy Item added to long-press menu.

Thanks to Google Code-in for many of the following patches:
(see http://www.google-melange.com/gci/homepage/google/gci2011 )

- Widget. (Google Code-In task by Andrey Zaytsev.)
- new setting for long press "Add" button (Google Code-in task by Gautam, issue 358)
- show "Stores" button when tracking per-store prices (Google Code-in task by Michal Zielinski, issue 359)
- reset all option in settings (Google Code-in task by Andrey Zaytsev, issue 360)
- fixed bug with disable screen lock (Google Code-in task by Darriel, issue 431)
- fix font color in list title and text entry field (issue 452)
- Update totals after changing quantity with Quick Edit.

- new icons and translations by Google Code-in students

----------------
release 1.5
date: 2011-12-08

- support for Android 4.0
- new setting for quick edit mode for quantity and priority (Android 2.2 and higher)
- new setting to keep screen orientation (issue 402, Google code-in task by Gautam)
- new setting to disable screen lock (Google Code-in task by Kido)
- new setting to reset quantity when readding items (Google Code-in task by Shuhao)
- many new translations by Google Code-in students

----------------
release 1.4.1
date: 2011-08-27

- choose sort order for Pick Items mode (in advanced settings)
- fix "clean up list" menu command (issue 386)
- fix crash when scanning barcodes (issue 388)
- fix calculation of total price

----------------
release 1.4
date: 2011-08-20
- support for Tablets with new layout for Android 3.0 (patch by Temp)
- support for the ActionBar (Android 3.0)
- optionally switch main list to edit mode (activate in advanced settings through "Pick items directly in list")
- basic support for tracking per-store prices (activate in advanced settings)
- add, rename, delete stores (long-press an item, select "Stores...", then long-press a store name)
- calculate subtotal by priority (activate in advanced settings)
- remove white space around item names (issue 364)
- fix various issues (issues 254, 351, 379)
- tweak performance

----------------
release 1.3.1
date: 2011-06-11
- fix crash when shaking phone (issue 313).
- remember list position (issue 349).
- update translations.

----------------
release 1.3.0
date: 2011-05-28
- new application icon for Android 2.0 or higher.
- allow app installation on external storage (requires Android 2.2 or higher)
- unit can be added to quantity.
- new field for priority, sort by priority.
- possibility to add notes (requires OI Notepad).
- case insensitive sort order.
- new menu command "mark all items" (patch by Tomek Scieplek)
- long press to launch barcode scanner
- translations into various languages (by Alcatel OneTouch), including Brazilian Portugese, Croatian, Greek, Hungarian, Persian (Farsi).

----------------
release 1.2.6
date: 2011-01-06
- move items between lists
- backup preferences and items
- support for xlarge screens
- launcher icons for different resolutions

----------------
release 1.2.5
date: 2010-08-05
- translations: Danish, minor changes

----------------
release 1.2.4
date: 2010-06-26
- translations: Argentinien, Czech, Chinese (Simplified),
  Chinese (Traditional), Dutch, Italian, Japanese, 
  Occitan (post 1500), Portugues
- fix #237: Sometimes crash on shake.
- new sort order "unchecked first, tags alphabetical"
- support for barcode scanner: if scanned item is already
  in list then its status is toggled.
- include quantity, price, and tags when sending a list.
- checkbox clickable with Dpad.
- fix bugs #282, #286 and enter key behaviour

----------------
release: 1.2.3
date: 2010-03-24
- translations: Russian
- fix #262, localized prices
- fix "Done" button for editing items

----------------
release: 1.2.2
date: 2010-03-06
- new font size "tiny" (for Droid users).

----------------
release: 1.2.1
date: 2010-02-21
- update MyBackupPro support v2.2.1
- support external themes
- set theme optionally for all lists

----------------
release: 1.2.0
date: 2010-01-01
- add quantity field
- optimize styles
- virtual keyboard should not automatically open
  on some devices
- translations: Korean, Romanian

----------------
release: 1.1.2
date: 2009-10-14
- translations: French, German, Polish, Spanish
- QVGA support
- support for MyBackup Pro
- new intent INSERT_FROM_EXTRAS, to support multiple
  items to be added to a list. Used by Ben Caldwell's
  application Bites.

----------------
release: 1.1.1
date: 2009-05-28

- fix bug when entering apostrophe
- new sort orders for tags and price
- in default list theme, clicking anywhere outside
  checkbox means to edit the item
- switch back to multiline display for items and tags

----------------
release: 1.1.0
date: 2009-05-16

- add price and tag fields
- automatic display of total price / total checked price
- hide/unhide checked items
- archive items for later use
- new menu > pick items to pick previously used items
  (can also be accessed by pressing "Add" button)
- settings include link to extensions from Market and
  developer homepage
- setting to show last list used (true by default)
- shake the phone to clean up the list
  (turned off by default)

----------------
release: 1.0.3
date: 2009-02-04

- preference for sorting and font size added
- prepare for permissions to access shopping lists
  (but don't activate them yet).
- fix bug when opened through AnyCut from homepage.
- create shortcut from home screen to arbitrary list.
- support for OI About.
- send list.
- fix issue with dialogs on screen orientation change.

----------------
release: 1.0.2
date: 2008-11-21

- allow for extensions with menu icon
- support OI Update and drop internet permission (TODO)
- broadcast changes to database so that extensions
  like VoiceNotes can listen.

----------------
release: 1.0.1
date: 2008-10-27

- fix bug with resources when uploading to Android Market

----------------
release: 1.0.0
date: 2008-10-25

- First public release on Android SDK 1.0.

Features: 
- Add items, mark them, clean up list.
- New list, rename list, delete list.
- Choose one of three themes (default, classic, Android).

