package org.openintents.intents;

/**
 * Intents for automation.
 *
 * @author Peli
 * @version 1.0.0
 */
public class AutomationIntents {

    /**
     * Activity Action: This activity is called to create or edit automation
     * settings.
     * <p/>
     * There can be several activities in an apk package that implement this
     * intent.
     * <p/>
     * <p>
     * Constant Value: "org.openintents.action.EDIT_AUTOMATION_SETTINGS"
     * </p>
     */
    public static final String ACTION_EDIT_AUTOMATION = "org.openintents.action.EDIT_AUTOMATION";

    /**
     * Broadcast Action: This broadcast is sent to the same package in order to
     * activate an automation.
     * <p/>
     * There can only be one broadcast receiver per package that implements this
     * intent. Any differentiation should be done through intent extras.
     * <p/>
     * <p>
     * Constant Value: "org.openintents.action.EDIT_AUTOMATION_SETTINGS"
     * </p>
     */
    public static final String ACTION_RUN_AUTOMATION = "org.openintents.action.RUN_AUTOMATION";

    /**
     * String extra containing a human readable description of the action to be
     * performed.
     * <p/>
     * <p>
     * Constant Value: "org.openintents.extra.DESCRIPTION"
     * </p>
     */
    public static final String EXTRA_DESCRIPTION = "org.openintents.extra.DESCRIPTION";

}
