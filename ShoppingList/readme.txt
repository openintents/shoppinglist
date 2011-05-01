 ****************************************************************************
 * Copyright (C) 2007-2011 OpenIntents.org                                  *
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

---------------------------------------------------------
release 1.3.0-rc1
date: 2011-05-01
- new application icon for Android 2.0 or higher.
- allow app installation on external storage (requires Android 2.2 or higher)
- unit can be added to quantity.
- new field for priority, sort by priority.
- possibility to add notes (requires OI Notepad).
- case insensitive sort order.
- new menu command "mark all items" (patch by Tomek Scieplek)
- long press to launch barcode scanner
- translations into various languages, including Brazilian Portugese, Greek, and Hungarian.

---------------------------------------------------------
release 1.2.6
date: 2011-01-06
- move items between lists
- backup preferences and items
- support for xlarge screens
- launcher icons for different resolutions

---------------------------------------------------------
release 1.2.5
date: 2010-08-05
- translations: Danish, minor changes

---------------------------------------------------------
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

---------------------------------------------------------
release: 1.2.3
date: 2010-03-24
- translations: Russian
- fix #262, localized prices
- fix "Done" button for editing items

---------------------------------------------------------
release: 1.2.2
date: 2010-03-06
- new font size "tiny" (for Droid users).

---------------------------------------------------------
release: 1.2.1
date: 2010-02-21
- update MyBackupPro support v2.2.1
- support external themes
- set theme optionally for all lists

---------------------------------------------------------
release: 1.2.0
date: 2010-01-01
- add quantity field
- optimize styles
- virtual keyboard should not automatically open
  on some devices
- translations: Korean, Romanian

---------------------------------------------------------
release: 1.1.2
date: 2009-10-14
- translations: French, German, Polish, Spanish
- QVGA support
- support for MyBackup Pro
- new intent INSERT_FROM_EXTRAS, to support multiple
  items to be added to a list. Used by Ben Caldwell's
  application Bites.

---------------------------------------------------------
release: 1.1.1
date: 2009-05-28

- fix bug when entering apostrophe
- new sort orders for tags and price
- in default list theme, clicking anywhere outside
  checkbox means to edit the item
- switch back to multiline display for items and tags

---------------------------------------------------------
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

---------------------------------------------------------
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

---------------------------------------------------------
release: 1.0.2
date: 2008-11-21

- allow for extensions with menu icon
- support OI Update and drop internet permission (TODO)
- broadcast changes to database so that extensions
  like VoiceNotes can listen.

---------------------------------------------------------
release: 1.0.1
date: 2008-10-27

- fix bug with resources when uploading to Android Market

---------------------------------------------------------
release: 1.0.0
date: 2008-10-25

- First public release on Android SDK 1.0.

Features: 
- Add items, mark them, clean up list.
- New list, rename list, delete list.
- Choose one of three themes (default, classic, Android).

