package com.rsb.splyt;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.IntentCompat;
import android.support.v4.content.LocalBroadcastManager;

import com.amazon.device.messaging.ADM;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.rsb.gson.Gson;
import com.rsb.gson.JsonObject;
import com.rsb.gson.JsonParser;
import com.rsb.gson.JsonPrimitive;
import com.rsb.gson.reflect.TypeToken;
import com.rsb.splyt.HttpRequest.RequestListener;
import com.rsb.splyt.HttpRequest.RequestResult;

// init Use Cases:
// 1) First time initializing & product is registered with Splyt's notification service
// - Register with 1st-party service (GCM/ADM) to retrieve a registration Id
// - Register device and current entities with Splyt's notification service
// - Cache off the device registration Id (if necessary), entities, and current app version
// 2) First time initializing & product is NOT registered with Splyt's notification service
// - Cache off current app version
// 3) NOT first time initializing & app version has NOT changed & product is registered with Splyt's notification service & what we have cached off is the latest registration Id and entities
// - Do nothing, we're good to go
// 4) NOT first time initializing & app version has NOT changed & product is registered with Splyt's notification service & what we have cached off is NOT the latest registration Id and/or entities
// - Reregister the device and current entities with Splyt's notification service
// - Cache off the device registration Id (if necessary) and entities
// 5) NOT first time initializing & app version has NOT changed & product is NOT registered with Splyt's notification service
// - Do nothing, we're good to go
// 6) NOT first time initializing & app version has changed & product is registered with Splyt's notification service & what we have cached off is the latest registration Id and entities
// - Cache off current app version
// 7) NOT first time initializing & app version has changed & product is registered with Splyt's notification service & what we have cached off is NOT the latest registration Id and/or entities
// - Reregister the device and current entities with Splyt's notification service
// - Cache off the device registration Id (if necessary), entities, and current app version
// 8) NOT first time initializing & app version has changed & product is NOT registered with Splyt's notification service
// - Cache off current app version

// final prevents extension of the class
final class NotificationSubsystem
{
    private static final String NOTIFICATION_SMALLICON_KEY_NAME = "notificationSmallIcon";
    private static final String NOTIFICATION_COMPONENTPACKAGE_KEY_NAME = "notificationCompPackage";
    private static final String NOTIFICATION_COMPONENTCLASS_KEY_NAME = "notificationCompClass";
    private static final String NOTIFICATION_REGISTRATIONID_KEY_NAME = "notificationRegistrationId";
    private static final String NOTIFICATION_HOST_KEY_NAME = "notificationHost";
    private static final String NOTIFICATION_QUERYPARAMS_KEY_NAME = "notificationQueryParams";
    private static final String NOTIFICATION_ENTITYIDS_KEY_NAME = "notificationEntityIds";
    private static final String NOTIFICATION_APPVERSION_KEY_NAME = "notificationAppVersion";
    private static final String NOTIFICATION_EXTRAS_KEY_NAME = SplytConstants.PACKAGE_NAME + ".Notification";
    private static final String WS_VERSION = "0";

    // true if initialization of the subsystem has been attempted, false otherwise
    private static boolean sInitialized;

    // The host address of Splyt's notification service server
    private static String sHost;

    // The query parameters we'll use to make the calls to Splyt's notification service server
    private static String sQueryParams;

    // The active service client
    private static ServiceClient sServiceClient;

    // Listener to invoke when a notification is received while the app is running or if we detect that the app was launched by a notification
    private static SplytNotificationReceivedListener sNotificationReceivedListener;

    // A boolean indicating whether or not we allow notifications to be posted to the status bar
    // Note that by default we assume that we want notifications posted unless the app is in the foreground (i.e., the app is resumed)
    private static boolean sAllowPost = true;

    // Receivers used to observe when pause and resume of the core subsystem (and hence the main activity) happen
    private static BroadcastReceiver sPauseReceiver;
    private static BroadcastReceiver sResumeReceiver;

    // private constructor defined to prevent instantiation
    private NotificationSubsystem()
    {
    }

    /**
     * Pre-initialize the notification subsystem.  This gets called before any subsystems are initialized
     *
     * @param activity         The "main" activity of the application that initialized SPLYT. This cannot be `null`.
     *                         This is the activity we launch when a notification is pressed/tapped
     * @param alwaysPost       A boolean specifying whether or not we always want to allow status notifications to be posted
     * @param disableAutoClear A boolean specifying whether or not we always want to disable the feature that automatically clears notifications from the status bar when the app is brought into the foreground
     * @param listener         Listener to invoke when a notification is received while the app is running or if we detect that the app was launched by a notification
     */
    static void preinit(final Activity activity, final boolean alwaysPost, final boolean disableAutoClear, SplytNotificationReceivedListener listener)
    {
        // Verify that we have a valid activity and host
        if (null == activity)
        {
            Util.logError("[Notification] Activity is null.  Please pass in a valid activity");
            return;
        }

        // Save off references to any static variables that may be required by this subsystem before initialization occurs
        // This can happen if the main activity of an app calls its resume method before our core subsystem finishes initializing
        // And since this subsystem doesn't get initialized until the core subsystem finished initializing...
        sNotificationReceivedListener = listener;

        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(activity);

        if (null != sPauseReceiver)
        {
            // Unregister any previous receiver because we may have a new instance of the main activity
            lbm.unregisterReceiver(sPauseReceiver);
        }

        // Register a receiver that calls this subsystem's pause method any time the core subsystem is paused
        sPauseReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                pause(activity);
            }
        };
        lbm.registerReceiver(sPauseReceiver, new IntentFilter(SplytConstants.ACTION_CORESUBSYSTEM_PAUSED));

        if (null != sResumeReceiver)
        {
            // Unregister any previous receiver because we may have a new instance of the main activity
            lbm.unregisterReceiver(sResumeReceiver);
        }

        sAllowPost = alwaysPost;

        // Register a receiver that calls this subsystem's resume method any time the core subsystem is resumed
        sResumeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                resume(activity, alwaysPost, disableAutoClear);
            }
        };
        lbm.registerReceiver(sResumeReceiver, new IntentFilter(SplytConstants.ACTION_CORESUBSYSTEM_RESUMED));
    }

    /**
     * Performs required initialization for the notification subsystem
     *
     * @param activity  The "main" activity of the application that initialized SPLYT. This cannot be `null`.
     *                  This is the activity we launch when a notification is pressed/tapped
     * @param host      The host address of Splyt's notification service server
     * @param smallIcon Resource index of the small icon to be displayed with the notification
     *                  Not that 0 is not a valid resource ID. See http://developer.android.com/reference/android/content/res/Resources.html#getIdentifier%28java.lang.String,%20java.lang.String,%20java.lang.String%29
     */
    static void init(Activity activity, String host, int smallIcon)
    {
        if (!sInitialized)
        {
            // Only attempt initialization of this subsystem once per launch of the app
            // It's not a "critical" subsystem, so if something goes wrong (e.g., Splyt's server is unreachable), we'll just try again the next time the app is launched
            sInitialized = true;

            // Verify that we have a valid activity and host
            if (null == activity)
            {
                Util.logError("[Notification] Activity is null.  Please pass in a valid activity");
                return;
            }
            else if ((null == host) || host.isEmpty())
            {
                Util.logError("[Notification] Please provide a valid host");
                return;
            }
            sHost = host;

            sQueryParams = CoreSubsystem.getQueryParms(WS_VERSION);

            Context appContext = activity.getApplicationContext();

            // Now check if ADM or Google Play Services is available.
            if (checkADMAvailable())
            {
                sServiceClient = new ADMClient(appContext);
            }
            else if (checkPlayServices(appContext))
            {
                sServiceClient = new GCMClient(appContext);
            }
            else
            {
                // No client service available.  This is ok.
                Util.logDebug("[Notification] No native client service available, notification service not enabled");
            }

            if (null != sServiceClient)
            {
                // If no icon resource is set, try and get the app's icon resource (default)
                if (0 == smallIcon)
                {
                    try
                    {
                        smallIcon = appContext.getPackageManager().getApplicationInfo(appContext.getPackageName(), PackageManager.GET_META_DATA).icon;
                    }
                    catch (NameNotFoundException ex)
                    {
                        // should never happen
                    }
                }

                // We have a client, so store off some preferences we'll need to post notifications
                // Get the parts of the component name of our activity.  We'll store this off so that we can launch the activity from a notification
                ComponentName compName = activity.getComponentName();
                String compPackage = compName.getPackageName();
                String compClass = compName.getClassName();

                SharedPreferences.Editor editor = activity.getSharedPreferences(SplytConstants.PREFS_FILENAME, Context.MODE_PRIVATE).edit();
                editor.putInt(NOTIFICATION_SMALLICON_KEY_NAME, smallIcon)
                      .putString(NOTIFICATION_COMPONENTPACKAGE_KEY_NAME, compPackage)
                      .putString(NOTIFICATION_COMPONENTCLASS_KEY_NAME, compClass)
                      .commit();

                // Set up a (permanent) receiver needed for when user entity Ids get set/updated (e.g., on login/logout)
                // We need to inform our backend any time this happens
                LocalBroadcastManager.getInstance(appContext).registerReceiver(new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        if (null != sServiceClient)
                        {
                            // Re-register the device, if necessary
                            sServiceClient.registerDevice(sServiceClient.getCachedRegistrationId());
                        }
                    }
                }, new IntentFilter(SplytConstants.ACTION_CORESUBSYSTEM_SETUSERID));

                // Initialize the client
                sServiceClient.init();
            }
        }
    }

    /**
     * Forcibly reregister the device
     * This method is typically invoked when the app or system is updated
     *
     * @param context The current context
     */
    static void reregisterDevice(Context context)
    {
        // Only valid for GCM and only if we have successfully registered before (i.e., we have a host and some query params)
        SharedPreferences sharedPrefs = context.getSharedPreferences(SplytConstants.PREFS_FILENAME, Context.MODE_PRIVATE);
        sHost = sharedPrefs.getString(NOTIFICATION_HOST_KEY_NAME, null);
        sQueryParams = sharedPrefs.getString(NOTIFICATION_QUERYPARAMS_KEY_NAME, null);
        if ((null != sHost) && (null != sQueryParams) && checkPlayServices(context))
        {
            // Clear the app version so that we are guaranteed to register the device
            sharedPrefs.edit().remove(NOTIFICATION_APPVERSION_KEY_NAME).commit();

            // Initialize the GCM client which starts the process of device registration
            sServiceClient = new GCMClient(context);

            sServiceClient.init();
        }
    }

    /**
     * Put the message into a notification and post it.
     * Note that this function must be callable even when Splyt's NotificationSubsystem has not been initialized
     * Hence the use of SharedPreferences for retrieving things like the notification icon resource and the main activity name
     *
     * @param context The current context (usually an IntentService)
     * @param info    A Bundle containing the notification message and optionally a title
     */
    static void postNotification(Context context, Bundle info)
    {
        Util.logDebug("[Notification] Notification received");

        // Only post notifications to the status bar if the app is not active (i.e., in the background)
        if (sAllowPost)
        {
            if (info.containsKey("message"))
            {
                // Grab the the data we need from the sharedPrefs to fire off our status notification
                SharedPreferences sharedPrefs = context.getSharedPreferences(SplytConstants.PREFS_FILENAME, Context.MODE_PRIVATE);
                int icon = sharedPrefs.getInt(NOTIFICATION_SMALLICON_KEY_NAME, 0);
                String compPackage = sharedPrefs.getString(NOTIFICATION_COMPONENTPACKAGE_KEY_NAME, "");
                String compClass = sharedPrefs.getString(NOTIFICATION_COMPONENTCLASS_KEY_NAME, "");
                if ((0 != icon) && !compPackage.isEmpty() && !compClass.isEmpty())
                {
                    ComponentName compName = new ComponentName(compPackage, compClass);

                    // Create an intent to launch the main (root) activity of a task
                    // We use the IntentCompat class provided in the Android Support Library to allow us to use the function makeMainActivity on older versions of Android (pre API level 11)
                    // See http://developer.android.com/guide/topics/ui/notifiers/notifications.html and
                    // http://developer.android.com/reference/android/content/Intent.html#FLAG_ACTIVITY_CLEAR_TOP
                    // for explanation as to why these specific flags are used
                    Intent notificationIntent = IntentCompat.makeMainActivity(compName)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent. FLAG_ACTIVITY_CLEAR_TOP)
                            .putExtra(NOTIFICATION_EXTRAS_KEY_NAME, info);

                    // Use a request code to make sure the intent is unique
                    // This way, any unique extras we pack into the intent can be properly retrieved
                    // See http://stackoverflow.com/questions/3009059/android-pending-intent-notification-problem
                    PendingIntent intent = PendingIntent.getActivity(context, UUID.randomUUID().hashCode(), notificationIntent, 0);

                    // Create our notification using the Builder class.
                    // We use the NotificationCompat class provided in the Android Support Library to allow its use on older versions of Android
                    int defaultFlags = Notification.DEFAULT_ALL;
                    NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                    .setContentText(info.getString("message"))
                    .setSmallIcon(icon)
                    .setContentIntent(intent)
                    .setAutoCancel(true)
                    .setDefaults(defaultFlags);

                    // Add some optional stuff
                    if (info.containsKey("title"))
                    {
                        builder.setContentTitle(info.getString("title"));
                    }

                    // Now build the notification to send
                    Notification notification = builder.build();

                    // Use the NotificationManager system service to notify the user
                    // Note that for the second parameter passed to the notify method, we use the icon as it will always be a unique number within the application
                    NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
                    notificationManager.notify(null, icon, notification);
                }
            }
        }
        else
        {
            // The app is active, let's not post a notification in the status bar.  Instead, we'll invoke the listener if it's been set
            if (null != sNotificationReceivedListener)
            {
                sNotificationReceivedListener.onReceived(info, false);
            }
        }
    }

    /**
     * Inform the notification subsystem that the main activity (or app) is being paused (i.e. put into the background)
     * This call would typically be triggered by the onPause() of the main activity that makes calls to Splyt.
     *
     * @param activity The "main" activity of the application that initialized SPLYT. This cannot be `null`.
     *                 This is the activity we launch when a notification is pressed/tapped
     */
    private static void pause(Activity activity)
    {
        if (null != activity)
        {
            // Clear any notification data that might be set since we don't want to get a false positive if the app is resumed without being restarted
            activity.getIntent().removeExtra(NOTIFICATION_EXTRAS_KEY_NAME);
        }
        else
        {
            Util.logError("[Notification] null Activity passed to pause().  Please pass in a valid activity");
        }

        // We always allow notifications to be posted when the main activity is paused
        sAllowPost = true;
    }

    /**
     * Inform the notification subsystem that the main activity (or app) is resuming (i.e. put into the foreground)
     * This call would typically be triggered by the onResume() of the main activity that makes calls to Splyt.
     *
     * @param activity         The "main" activity of the application that initialized SPLYT. This cannot be `null`.
     *                         This is the activity we launch when a notification is pressed/tapped
     * @param alwaysPost       A boolean specifying whether or not we always want to allow status notifications to be posted
     * @param disableAutoClear A boolean specifying whether or not we always want to disable the feature that automatically clears notifications from the status bar when the app is brought into the foreground
     */
    private static void resume(Activity activity, boolean alwaysPost, boolean disableAutoClear)
    {
        if (null != activity)
        {
            // Clear any status notifications from the Status Bar
            if (!disableAutoClear)
            {
                try
                {
                    // Use the NotificationManager system service to clear all notifications
                    NotificationManager notificationManager = (NotificationManager)activity.getSystemService(Context.NOTIFICATION_SERVICE);
                    notificationManager.cancelAll();
                }
                catch (Exception e)
                {
                    Util.logError("[Notification] Error clearing notifications: " + e.getMessage());
                }
            }

            // Determine if we were launched by a notification and if so we automatically log a transaction with the id and type of the notification that launched the app
            Bundle info = activity.getIntent().getBundleExtra(NOTIFICATION_EXTRAS_KEY_NAME);
            if (null != info)
            {
                // We were launched by a notification and if the core subsystem can receive data, automatically send an event to the data collector
                CoreSubsystem.InitializationState initState = CoreSubsystem.getInitializationState();
                if (CoreSubsystem.InitializationState.Initialized == initState || CoreSubsystem.InitializationState.Initializing == initState)
                {
                    final Splyt.Instrumentation.Transaction txn = Splyt.Instrumentation.Transaction("splyt.launchedbynotification");
                    String splytData = info.getString("splyt");
                    if (null != splytData)
                    {
                        JsonParser parser = new JsonParser();
                        JsonObject obj = parser.parse(splytData).getAsJsonObject();
                        JsonPrimitive idPrim = obj.getAsJsonPrimitive("id");
                        if (null != idPrim)
                        {
                            String id = idPrim.getAsString();
                            if (null != id)
                            {
                                txn.setProperty("id", id);
                            }
                        }

                        JsonPrimitive typePrim = obj.getAsJsonPrimitive("type");
                        if (null != typePrim)
                        {
                            String type = typePrim.getAsString();
                            if (null != type)
                            {
                                txn.setProperty("type", type);
                            }
                        }
                    }

                    // If the core subsystem has successfully initialized, go ahead and send the transaction now, otherwise we'll wait for initialization to finish
                    if (CoreSubsystem.InitializationState.Initialized == initState)
                    {
                        // Send immediately
                        txn.beginAndEnd();
                    }
                    else
                    {
                        final LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(activity);
                        BroadcastReceiver receiver = new BroadcastReceiver() {
                            @Override
                            public void onReceive(Context context, Intent intent) {
                                // Core subsystem is now initialized, it's safe to send the transaction
                                txn.beginAndEnd();

                                // Unregister ourselves as a receiver
                                lbm.unregisterReceiver(this);
                            }
                        };

                        lbm.registerReceiver(receiver, new IntentFilter(SplytConstants.ACTION_CORESUBSYSTEM_INITIALIZATIONCOMPLETE));
                    }
                }

                // If a listener is registered...
                if (null != sNotificationReceivedListener)
                {
                    // Invoke the listener now
                    sNotificationReceivedListener.onReceived(info, true);
                }
            }
        }
        else
        {
            Util.logError("[Notification] null Activity passed to resume().  Please pass in a valid activity");
        }

        if (!alwaysPost)
        {
            // By default, when the main activity is resumed (i.e., the app is brought into the foreground), we don't want to post notifications to the status bar.
            // This is similar behavior to what's done on iOS
            // However, we do allow the integrator to override this behavior at initialization
            sAllowPost = false;
        }
    }

    /**
     * Checks the device to see if it has the Google Play Services APK.
     *
     * @param context The current context (usually the main activity)
     *
     * @return true if the Google Play Services APK is installed, false otherwise
     */
    private static boolean checkPlayServices(Context context)
    {
        if (ConnectionResult.SUCCESS != GooglePlayServicesUtil.isGooglePlayServicesAvailable(context))
        {
            Util.logDebug("Google Play Services not available:  Splyt Notification service on GCM not supported.");
            return false;
        }

        return true;
    }

    /**
     * Checks the device to see if Amazon Device Messaging (ADM) is supported
     *
     * @return true if the ADM is supported, false otherwise
     */
    private static boolean checkADMAvailable()
    {
        boolean ADMAvailable = false;
        try
        {
            Class.forName("com.amazon.device.messaging.ADM");
            ADMAvailable = true;
        }
        catch (ClassNotFoundException ex)
        {
            Util.logDebug("ADM not available:  Splyt Notification service on ADM not supported.");
        }

        return ADMAvailable;
    }

    // Base class for client (GCM/ADM) implementations
    private static abstract class ServiceClient
    {
        protected final Context mContext;

        protected String mName;

        public ServiceClient(Context context)
        {
            mContext = context;
        }

        /**
         * Gets the name of the client service.
         *
         * @return The client service name
         */
        public String getName() { return mName; }

        private Map<String, String> getCurEntityIds()
        {
            // Gather the entity info (we need to have some entity to associate this with)
            Map<String, String> entityIds = new HashMap<String, String>();

            String userId = CoreSubsystem.getUserId();
            if (CoreSubsystem.isValidId(userId))
            {
                entityIds.put(SplytConstants.ENTITY_TYPE_USER, userId);
            }

            String deviceId = CoreSubsystem.getDeviceId();
            if (CoreSubsystem.isValidId(deviceId))
            {
                entityIds.put(SplytConstants.ENTITY_TYPE_DEVICE, deviceId);
            }

            return entityIds;
        }

        /**
         * Checks if our current entity Ids are in sync with what we have cached in prefs
         * Note that we only care about the last seen user Id, so if it gets cleared (e.g., during a logout), we really don't care and still consider the Ids to be in sync
         *
         * @param curEntityIds The current entity Ids as set in the CoreSubsystem
         *
         * @return true if the entities are in sync, false otherwise
         */
        private boolean entitiesInSync(Map<String, String> curEntityIds)
        {
            // Assume the entities are in sync
            boolean inSync = true;

            // The entities are considered NOT in sync if:
            // a) The device Id has changed (we're guaranteed to always have a device Id)
            // OR
            // b) The user Id has changed and is NOT nil
            // That is, even if the user Id is nil, the entities can still be considered to be in sync
            SharedPreferences sharedPrefs = mContext.getSharedPreferences(SplytConstants.PREFS_FILENAME, Context.MODE_PRIVATE);

            String cachedEntityIdsJson = sharedPrefs.getString(NOTIFICATION_ENTITYIDS_KEY_NAME, "");

            Map<String, String> cachedEntityIds = new Gson().fromJson(cachedEntityIdsJson, new TypeToken<Map<String, String>>(){}.getType());
            String curDeviceId = curEntityIds.get(SplytConstants.ENTITY_TYPE_DEVICE);
            String cachedDeviceId = cachedEntityIds.get(SplytConstants.ENTITY_TYPE_DEVICE);
            if ((null != curDeviceId) && !curDeviceId.equals(cachedDeviceId))
            {
                inSync = false;
            }
            else
            {
                // device Ids are in sync, what about the user Ids?
                String curUserId = curEntityIds.get(SplytConstants.ENTITY_TYPE_USER);
                String cachedUserId = cachedEntityIds.get(SplytConstants.ENTITY_TYPE_USER);
                if ((null != curUserId) && !curUserId.equals(cachedUserId))
                {
                    inSync = false;
                }
            }

            return inSync;
        }

        /**
         * Initialize the service client
         */
        public void init()
        {
            // First check if this is our first time initializing (i.e., if we have an app version cached in the shared prefs)
            SharedPreferences sharedPrefs = mContext.getSharedPreferences(SplytConstants.PREFS_FILENAME, Context.MODE_PRIVATE);

            int cachedAppVersion = sharedPrefs.getInt(NOTIFICATION_APPVERSION_KEY_NAME, Integer.MIN_VALUE);
            if (getCurAppVersion() != cachedAppVersion)
            {
                // Either the app version has changed
                // OR this is the first time we're initializing
                // OR we've never successfully completed initialization before
                // So, let's run the check to see if the product is registered for Splyt's notification service
                // This path is use case 1, 2, 6, 7, or 8

                checkProductRegistered();
            }
            else
            {
                // The app version has not changed
                // Let's see if we have a registration Id stored locally
                String regId = getCachedRegistrationId();
                if (null != regId)
                {
                    // We do, so we assume that this product is registered for Splyt's for notification service (no need to hit the server to check)
                    // We just need to verify that the device is properly registered
                    // This path is use case 3 or 4
                    registerDevice(regId);
                }
                else
                {
                    // No device token stored locally, this product is NOT registered for Splyt's notification service, we're done
                    // This path is use case 5
                    Util.logDebug("[Notification] Product is not registered with Splyt's notification service");
                }
            }
        }

        /**
         * Register the device with Splyt's notification service (if necessary)
         *
         * @param curRegId The current (GCM or ADM) registration Id associated with the device
         */
        public void registerDevice(final String curRegId)
        {
            if (null != curRegId)
            {
                // If the current registration Id matches what we already have cached locally and the current entities are in sync, then we're finished
                // Otherwise, we need to (re)register the device with Splyt's notification service
                String cachedRegId = getCachedRegistrationId();

                final Map<String, String> curEntityIds = getCurEntityIds();
                if ((null == cachedRegId) || !curRegId.equals(cachedRegId) || !entitiesInSync(curEntityIds))
                {
                    // We need to (re)register the device with Splyt's notification service
                    List<Object> args = new ArrayList<Object>(3);
                    args.add(getName());
                    args.add(curRegId);
                    args.add(curEntityIds);

                    try
                    {
                        // Create an (async) request to send the registration Id to Splyt. The callback will be triggered when the request is completed
                        String url = sHost + "/splyt-notification/ws/interface/device_register" + sQueryParams;

                        new HttpRequest(new URL(url), CoreSubsystem.getReqTimeout(), new Gson().toJson(args)).executeAsync(new RequestListener() {
                            @Override
                            public void onComplete(RequestResult result) {
                                if (SplytError.Success == result.error)
                                {
                                    try
                                    {
                                        JsonParser parser = new JsonParser();
                                        JsonObject obj = parser.parse(result.response).getAsJsonObject();
                                        JsonPrimitive error = obj.getAsJsonPrimitive("error");
                                        if ((null != error) && (SplytError.Success.getValue() == error.getAsInt()))
                                        {
                                            JsonObject resultObj = obj.getAsJsonObject("data").getAsJsonObject("device_register");
                                            error = resultObj.getAsJsonPrimitive("error");
                                            if ((null != error) && (SplytError.Success.getValue() == error.getAsInt()))
                                            {
                                                if (cachePrefs(curRegId, curEntityIds))
                                                {
                                                    // Success!
                                                    Util.logDebug("[Notification] Device and associated entities successfully registered with Splyt's notification service");
                                                }
                                                else
                                                {
                                                    Util.logError("[Notification] Problem caching prefs");
                                                }
                                            }
                                            else
                                            {
                                                JsonPrimitive desc = resultObj.getAsJsonPrimitive("description");
                                                Util.logError("[Notification] Problem on device registration, error description: " + desc.getAsString());
                                            }
                                        }
                                        else
                                        {
                                            // Top-level error
                                            JsonPrimitive desc = obj.getAsJsonPrimitive("description");
                                            Util.logError("[Notification] Problem on device registration, error description: " + desc.getAsString());
                                        }
                                    }
                                    catch (Exception e)
                                    {
                                        Util.logError("[Notification] Exception during device registration: " + result.response);
                                    }
                                }
                                else
                                {
                                    // Request failure (likely a timeout), pass it through
                                    Util.logError("[Notification] device_register call failed: code " + result.error);
                                }
                            }
                        });
                    }
                    catch (MalformedURLException e)
                    {
                        Util.logError("[Notification] MalformedURLException during the HttpRequest.  Check your host and customerId values");
                    }
                    catch (Exception e)
                    {
                        Util.logError("[Notification] Error during HttpRequest: " + e.getMessage());
                    }
                }
                else
                {
                    // The device is already registered with Splyt's notification service and the entities are in sync
                    // Cache the prefs to make sure the app version is up-to-date
                    cachePrefs(null, null);
                }
            }
        }

        /**
         * Gets the cached registration Id for the client service.
         * If result is null, the app needs to register for one.
         *
         * @return the cached registration Id, or null if none exists.
         */
        public String getCachedRegistrationId()
        {
            return mContext.getSharedPreferences(SplytConstants.PREFS_FILENAME, Context.MODE_PRIVATE).getString(NOTIFICATION_REGISTRATIONID_KEY_NAME, null);
        }

        /**
         * Caches preferences needed by the notification subsystem
         * {@code SharedPreferences}.
         *
         * @param regId The registration ID of the device
         * @param entityIds Map of the entity Ids (DEVICE and/or USER)
         *
         * @return true if the prefs were cached, false otherwise
         */
        private boolean cachePrefs(String regId, Map<String, String> entityIds)
        {
            SharedPreferences.Editor editor = mContext.getSharedPreferences(SplytConstants.PREFS_FILENAME, Context.MODE_PRIVATE).edit();

            editor.putInt(NOTIFICATION_APPVERSION_KEY_NAME, getCurAppVersion());
            if (null != regId)
            {
                editor.putString(NOTIFICATION_REGISTRATIONID_KEY_NAME, regId);

                // Store off the host and query params used when registering this device
                editor.putString(NOTIFICATION_HOST_KEY_NAME, sHost);
                editor.putString(NOTIFICATION_QUERYPARAMS_KEY_NAME, sQueryParams);
            }
            if (null != entityIds) editor.putString(NOTIFICATION_ENTITYIDS_KEY_NAME, new Gson().toJson(entityIds));

            return editor.commit();
        }

        /**
         * Checks if the product is registered with Splyt's notification service and if it is, proceeds accordingly
         */
        private void checkProductRegistered()
        {
            List<Object> args = new ArrayList<Object>(1);
            args.add(getName());

            // Create an (async) request to determine if the product is registered with Splyt. The listener callback will be triggered when the request is completed
            try
            {
                String url = sHost + "/splyt-notification/ws/interface/product_isregistered" + sQueryParams;

                new HttpRequest(new URL(url), CoreSubsystem.getReqTimeout(), new Gson().toJson(args)).executeAsync(new ProductIsRegisteredRequestListener());
            }
            catch (MalformedURLException e)
            {
                Util.logError("[Notification] MalformedURLException during the HttpRequest.  Check your host and customerId values");
            }
            catch (Exception e)
            {
                Util.logError("[Notification] Error during HttpRequest: " + e.getMessage());
            }
        }

        /**
         * Gets the application's current version code
         *
         * @return The application's version code from the {@code PackageManager}.
         */
        protected int getCurAppVersion()
        {
            int appVersion = -1;

            try
            {
                PackageInfo packageInfo = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
                appVersion = packageInfo.versionCode;
            }
            catch (NameNotFoundException ex)
            {
                // should never happen
            }

            return appVersion;
        }

        ////////////////////////////
        // Private helper classes //
        ////////////////////////////

        /**
         * Request listener used when verifying whether or not the product is registered with Splyt's notification service.
         */
        private class ProductIsRegisteredRequestListener implements RequestListener
        {
            class isRegisteredRet
            {
                boolean registered;
                String  projectNumber;
            }

            @Override
            public void onComplete(RequestResult result)
            {

                if (SplytError.Success == result.error)
                {
                    try
                    {
                        JsonParser parser = new JsonParser();
                        JsonObject obj = parser.parse(result.response).getAsJsonObject();
                        JsonPrimitive error = obj.getAsJsonPrimitive("error");
                        if ((null != error) && (SplytError.Success.getValue() == error.getAsInt()))
                        {
                            JsonObject resultObj = obj.getAsJsonObject("data").getAsJsonObject("product_isregistered");
                            error = resultObj.getAsJsonPrimitive("error");
                            if ((null != error) && (SplytError.Success.getValue() == error.getAsInt()))
                            {
                                JsonObject data = resultObj.getAsJsonObject("data");
                                isRegisteredRet ret = new Gson().fromJson(data, isRegisteredRet.class);
                                if (ret.registered)
                                {
                                    // The product is registered with Splyt's notification service.
                                    // Now attempt to register with the client and obtain a registration Id
                                    Util.logDebug("[Notification] Product is set up for notifications, registering with provider");
                                    sServiceClient.register(ret.projectNumber);
                                }
                                else
                                {
                                    // Product is not registered with splyt's notification service. This is ok.
                                    // Just pass null for the registration Id and entities when caching the prefs
                                    // Use case 2 or 8
                                    if (cachePrefs(null, null))
                                    {
                                        Util.logDebug("[Notification] Product is not registered with Splyt's notification service");
                                    }
                                    else
                                    {
                                        Util.logError("[Notification] Problem caching defaults");
                                    }
                                }
                            }
                            else
                            {
                                JsonPrimitive desc = resultObj.getAsJsonPrimitive("description");
                                Util.logError("[Notification] Problem determining product registration, error description: " + desc.getAsString());
                            }
                        }
                        else
                        {
                            // Top-level error
                            JsonPrimitive desc = obj.getAsJsonPrimitive("description");
                            Util.logError("[Notification] Problem determining product registration, error description: " + desc.getAsString());
                        }
                    }
                    catch (Exception e)
                    {
                        Util.logError("[Notification] Exception determining product registration: " + result.response);
                    }
                }
                else
                {
                    // Request failure (likely a timeout), pass it through
                    Util.logError("[Notification] product_isregistered call failed: code " + result.error);
                }
            }
        }

        /**
         * Registers the device with the service and retrieves a (new) registration Id
         *
         * @param projectNumber The project number required by some platforms to register for a new Id
         */
        abstract void register(String projectNumber);
    }

    ////////////////////////
    // GCM implementation //
    ////////////////////////

    private static class GCMClient extends ServiceClient
    {
        public GCMClient(Context context)
        {
            super(context);

            mName = "GCM";
        }

        @Override
        public void register(final String projectNumber)
        {
            if ((null != projectNumber) && !projectNumber.isEmpty())
            {
                Util.logDebug("[Notification] Registering with GCN...");

                // We have a project Number (Sender ID)
                // Now, let's get a registration Id
                new AsyncTask<Void, Void, String>()
                {
                    @Override
                    protected String doInBackground(Void... params)
                    {
                        String regId = null;

                        Random randomGenerator = new Random();
                        for (int tryNum = 0; tryNum < 5; ++tryNum)
                        {
                            try
                            {
                                regId = GoogleCloudMessaging.getInstance(mContext).register(projectNumber);
                                Util.logDebug("[Notification] GCM registration Id: " + regId);

                                break;
                            }
                            catch (IOException ex)
                            {
                                Util.logError("[Notification] Exception retrieving the GCM registration Id: " + ex.getMessage());

                                // Apply exponential backoff and retry
                                try
                                {
                                    Thread.sleep((1 << tryNum) * 1000 + randomGenerator.nextInt(1001));
                                }
                                catch (InterruptedException e) { }
                            }
                            catch (Exception ex)
                            {
                                Util.logError("[Notification] Unexpected Exception retrieving the GCM registration Id: " + ex.getMessage());
                                break;
                            }
                        }

                        return regId;
                    }

                    @Override
                    protected void onPostExecute(String regId)
                    {
                        // Register the device with Splyt's notification service
                        registerDevice(regId);
                    }
                }.execute();
            }
            else
            {
                // This should never happen
                Util.logDebug("[Notification] Missing project Number");
            }
        }
    }

    ////////////////////////
    // ADM implementation //
    ////////////////////////

    private static class ADMClient extends ServiceClient
    {
        public ADMClient(Context context)
        {
            super(context);

            mName = "ADM";

            // Add a listener to the ADMMessageHandler in case we retrieve a new device registration Id
            ADMMessageHandler.setRegisteredListener(new com.rsb.splyt.ADMMessageHandler.RegisteredListener() {
                @Override
                public void onComplete(String regId)
                {
                    // Register the device with Splyt's notification service
                    Util.logDebug("[Notification] ADM registration Id: " + regId);
                    registerDevice(regId);
                }
            });
        }

        @Override
        public void register(String projectNumber)
        {
            // Now, let's get a registration Id
            // Note that adm.startRegister() is asynchronous and the app is notified via the ADMMessageHandler's onRegistered() callback when the registration ID is available.
            Util.logDebug("[Notification] Registering with ADM...");
            new ADM(mContext).startRegister();
        }
    }
}
