package org.openintents.intents

/**
 * Intents for automation.
 *
 * @author Peli
 * @version 1.0.0
 */
object AutomationIntents {

    /**
     * Activity Action: This activity is called to create or edit automation
     * settings.
     *
     * There can be several activities in an apk package that implement this
     * intent.
     *
     * Constant Value: "org.openintents.action.EDIT_AUTOMATION_SETTINGS"
     */
    const val ACTION_EDIT_AUTOMATION = "org.openintents.action.EDIT_AUTOMATION"

    /**
     * Broadcast Action: This broadcast is sent to the same package in order to
     * activate an automation.
     *
     * There can only be one broadcast receiver per package that implements this
     * intent. Any differentiation should be done through intent extras.
     *
     * Constant Value: "org.openintents.action.EDIT_AUTOMATION_SETTINGS"
     */
    const val ACTION_RUN_AUTOMATION = "org.openintents.action.RUN_AUTOMATION"

    /**
     * String extra containing a human readable description of the action to be
     * performed.
     *
     * Constant Value: "org.openintents.extra.DESCRIPTION"
     */
    const val EXTRA_DESCRIPTION = "org.openintents.extra.DESCRIPTION"
}
