package org.openintents.shopping

import org.openintents.shopping.ui.compose.ComposeShoppingActivity

/**
 * The app's main screen (the launcher entry).
 *
 * The main activity prior to version 1.4 was ".ShoppingActivity". Home screens,
 * list shortcuts and the widget link to this component, therefore this class
 * must never be renamed or moved.
 *
 * It hosts the Compose UI (see [ComposeShoppingActivity]). The legacy View UI
 * (.ui.ShoppingActivity) only serves the item pick/edit intents of other apps.
 */
open class ShoppingActivity : ComposeShoppingActivity()
