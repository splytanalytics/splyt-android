package com.rsb.splyt;

import java.util.HashMap;
import java.util.Map;

import android.app.Activity;

/**
 * <p>This is the main library for SPLYT, which is composed of four subsystems:
 * <ul>
 * <li>Core: performs the core functionality of initialization, pausing, and resuming, as well as managing the user and device IDs.
 * <li>Instrumentation: Use this subsystem to instrument your app, which will send data to SPLYT.
 * <li>Tuning: Use this subsystem to retrieve tuning variables from SPLYT. These can be used in A-Z testing.
 * <li>Notification: Use this subsystem if you want support for mobile push notifications.
 * </ul>
 * </p>
 *
 * <p>Simplified instrumentation for common scenarios can be achieved by using {@link SplytPlugins SplytPlugins}.</p>
 *
 * @author Copyright 2015 Knetik, Inc.
 * @version 1.0
 */
public class Splyt
{
    /**
     * Provides access to the most central pieces of the SPLYT framework.
     */
    public static class Core
    {
        private static final String DEFAULT_DATACOLLECTOR_HOSTNAME = "https://data.splyt.com";
        private static final String DEFAULT_NOTIFICATION_HOSTNAME = "https://notification.splyt.com";

        /**
         * Gets the registered ID of the active user.
         */
        public static String getUserId() { return CoreSubsystem.getUserId(); }

        /**
         * Gets the registered ID of the device.
         */
        public static String getDeviceId() { return CoreSubsystem.getDeviceId(); }

        /**
         * A helper class that provides information used to initialize SPLYT. Use the factory method
         * {@link Splyt.Core#createInitParams createInitParams} to create an instance of this class.
         */
        public static class InitParams
        {
            // Required parameters
            private final Activity mActivity;
            private final String mCustomerId;

            // Optional parameters
            private EntityInfo mDeviceInfo = null;
            private EntityInfo mUserInfo = null;
            private int mReqTimeout = SplytConstants.DEFAULT_REQUEST_TIMEOUT;
            private String mHost = DEFAULT_DATACOLLECTOR_HOSTNAME;
            private boolean mLogEnabled = false;
            private String mSDKName = "android";
            private String mSDKVersion = "5.0.0";

            /**
             * Creates an instance of {@link InitParams InitParams}.
             *
             * @param activity    An Android activity (`android.app.Activity`), preferably the "main" activity of
             *                    the application that initialized SPLYT. This cannot be `null`.
             * @param customerId  The customer ID. This is a unique string that associates the data you send with a
             *                    specific product's dashboards in SPLYT. Note that the customer ID must be set up in
             *                    SPLYT's web application beforehand, or else the data will not be saved. If you do
             *                    not know your customer ID, contact SPLYT support: <a href="mailto:support@splyt.com">support@splyt.com</a>.
             */
            private InitParams(Activity activity, String customerId)
            {
                mActivity = activity;
                mCustomerId = customerId;
                Notification = new NotificationParams();
            }

            /**
             * Sets the {@link EntityInfo EntityInfo} that describes the device that the current app instance is
             * running on. If left `null`, SPLYT will use a randomly generated identifier for the device
             * and automatically determine whether this is a new device that is running the app for the first time.
             *
             * @param value The device info.
             * @return This {@link InitParams InitParams} instance.
             */
            public InitParams setDeviceInfo(EntityInfo value)
            {
                mDeviceInfo = value;
                return this;
            }

            /**
             * Sets the {@link EntityInfo EntityInfo} that describes the user of the current app instance.
             *
             * @param value The user info.
             * @return This {@link InitParams InitParams} instance.
             */
            public InitParams setUserInfo(EntityInfo value)
            {
                mUserInfo = value;
                return this;
            }

            /**
             * <p>
             * Sets the timeout interval, in milliseconds (the default is 1500 milliseconds). If during a connection
             * attempt the request remains idle for longer than the timeout interval, the request is considered to
             * have timed out.
             * </p>
             * <p>
             * <b>Note:</b> If this is not called, the timeout defaults to
             * {@link SplytConstants#DEFAULT_REQUEST_TIMEOUT DEFAULT_REQUEST_TIMEOUT}.
             * </p>
             *
             * @param value The timeout, in milliseconds.
             * @return This {@link InitParams InitParams} instance.
             */
            public InitParams setRequestTimeout(int value)
            {
                mReqTimeout = value;
                return this;
            }

            /**
             * A URL that specifies the protocol and hostname to be used when sending data to SPLYT
             * (default: https://data.splyt.com). This value does not normally need to be changed.
             *
             * @param value The hostname to use when sending data to SPLYT.
             * @return This {@link InitParams InitParams} instance.
             */
            public InitParams setHost(String value)
            {
                /* Internal note: a common scenario for using this as part of SDK development comes
                 * up when debugging under the Android emulator. In those cases, you would want to
                 * set the hostname to http://10.0.2.2 to have data sent to localhost.
                 */
                mHost = value;
                return this;
            }

            /**
             * Specifies whether or not SPLYT should log informational and error messages
             * (default: `false`).
             *
             * @param value Set to `true` to enable logging.
             * @return This InitParams instance.
             */
            public InitParams setLogEnabled(boolean value)
            {
                mLogEnabled = value;
                return this;
            }

            /**
             * This package-private function overrides the SDK name.  This is useful if implementing an SDK that
             * builds on top of this Android SDK.  The SDK name appears in the SPLYT web site's "SDK debugger" page
             * for use in testing your instrumentation.
             *
             * @param value The SDK name override.
             * @return This {@link InitParams InitParams} instance.
             */
            InitParams setSDKName(String value)
            {
                mSDKName = value;
                return this;
            }

            /**
             * This package-private function overrides the SDK version.  This is useful if implementing an SDK that
             * builds on top of this Android SDK.  The SDK version appears in the SPLYT web site's "SDK debugger" page
             * for use in testing your instrumentation.
             *
             * @param value The SDK version override.
             * @return This {@link InitParams InitParams} instance.
             */
            InitParams setSDKVersion(String value)
            {
                mSDKVersion = value;
                return this;
            }

            // Notification-specific initialization parameters
            public static class NotificationParams
            {
                private String mHost = DEFAULT_NOTIFICATION_HOSTNAME;
                private int mSmallIcon;
                private boolean mAlwaysPost;
                private boolean mDisableAutoClear;
                private SplytNotificationReceivedListener mReceivedListener;

                /**
                 * A URL that specifies the protocol and hostname to be used when sending data to SPLYT's notification service
                 * (default: https://notification.splyt.com). This value does not normally need to be changed.
                 *
                 * @param value The hostname to use when sending data to SPLYT's notification service.
                 */
                public NotificationParams setHost(String value)
                {
                    /* Internal note: a common scenario for using this as part of SDK development comes
                     * up when debugging under the Android emulator. In those cases, you would want to
                     * set the hostname to http://10.0.2.2 to have data sent to localhost.
                     */
                    mHost = value;
                    return this;
                }

                /**
                 * Set the small icon resource used with the notification
                 * <p>
                 * <b>Note:</b> If using SPLYT's notification service, this is optional.  If no small icon resource is provided, the app (launcher) icon will be used.
                 *
                 * @param value The resource index for the small icon displayed with the notification.
                 */
                public NotificationParams setSmallIcon(int value)
                {
                    mSmallIcon = value;
                    return this;
                }

                /**
                 * Set whether or not we always want to post notifications to the status bar (i.e., even when the app is in the foreground)
                 * <p>
                 * <b>Note:</b> The default is false
                 *
                 * @param value true if we want status notifications always posted, false if we only want them posted when the app is not in the foreground
                 */
                public NotificationParams setAlwaysPost(boolean value)
                {
                    mAlwaysPost = value;
                    return this;
                }

                /**
                 * Set whether or not we always want to disable the feature that automatically clears notifications from the status bar when the app is brought into the foreground
                 * <p>
                 * <b>Note:</b> The default is false
                 *
                 * @param value true if we want to disable auto-clearing of notifications from the status bar, false otherwise
                 */
                public NotificationParams setDisableAutoClear(boolean value)
                {
                    mDisableAutoClear = value;
                    return this;
                }

                /**
                 * Sets a listener (callback) that will be invoked when a notification is received either while the app is running
                 * or if we detect that the app was launched by a notification
                 *
                 * @param value An implementation of a SplytNotificationReceivedListener
                 */
                public NotificationParams setReceivedListener(SplytNotificationReceivedListener value)
                {
                    mReceivedListener = value;
                    return this;
                }
            }

            public NotificationParams Notification;
        }

        /**
         * A helper class used during initialization of SPLYT. Describes the active user or device. Use the factory
         * methods {@link Splyt.Core#createUserInfo createUserInfo} or
         * {@link Splyt.Core#createDeviceInfo createDeviceInfo} to create an instance of this class.
         *
         * <b>Note:</b> When used to build EntityInfo for the {@link Splyt.Core#registerUser registerUser} method, an
         * ID (i.e., the user ID) must be provided.
         */
        public static class EntityInfo
        {
            // Optional parameters
            private String mType = null;
            private String mId = null;
            private Map<String,Object> mProperties = null;

            /**
             * EntityInfo constructor
             */
            private EntityInfo(String type)
            {
                mType = type;
            }

            private EntityInfo(String type, String id)
            {
                mType = type;
                mId = id;
            }

            /**
             * Overrides the user or device ID. Normally, this is only used in the case where an application wants
             * specific control over device IDs, which SPLYT auto-generates by default.
             *
             * @param value The overriding entity ID.
             * @return This {@link EntityInfo EntityInfo} instance.
             */
            public EntityInfo overrideId(String value)
            {
                mId = value;
                return this;
            }

            /**
             * Report that this entity is a new user or device. Normally, SPLYT auto-detects new users or devices
             * based on whether it has previously seen their IDs. This method allows you to override the
             * auto-detection and explicitly control whether the entity will be counted as a new user or device.
             *
             * @param value `true` if the user or device is to be treated as new; otherwise, `false`.
             * @return This {@link EntityInfo EntityInfo} instance.
             */
            public EntityInfo setIsNew(boolean value)
            {
                if(null == mProperties)
                {
                    mProperties = new HashMap<String,Object>();
                }

                mProperties.put(SplytConstants.PROPERTY_ISNEW, value);
                return this;
            }

            /**
             * Set a single property of user or device state.
             *
             * @param key   Key for user or device property.
             * @param value Value for user or device property.
             * @return This {@link EntityInfo EntityInfo} instance.
             */
            public EntityInfo setProperty(String key, Object value)
            {
                if(null == mProperties)
                {
                    mProperties = new HashMap<String,Object>();
                }

                mProperties.put(key, value);
                return this;
            }

            /**
             * Set multiple properties of user or device state.
             *
             * @param value A collection of user or device properties.
             * @return This {@link EntityInfo EntityInfo} instance.
             */
            public EntityInfo setProperties(Map<String,Object> value)
            {
                if(null == mProperties)
                {
                    mProperties = value;
                }
                else
                {
                    mProperties.putAll(value);
                }
                return this;
            }
        }

        /**
         * Creates an instance of {@link InitParams InitParams} with the specified customer ID.
         *
         * @param  activity    An Android activity (`android.app.Activity`), preferably the "main" activity of
         *                     the application that initialized SPLYT. This cannot be `null`.
         * @param  customerId  The customer ID. This is a unique string that associates the data you send with a
         *                     specific product's dashboards in SPLYT. Note that the customer ID must be set up in
         *                     SPLYT's web application beforehand, or else the data will not be saved. If you do
         *                     not know your customer ID, contact SPLYT support: <a href="mailto:support@splyt.com">support@splyt.com</a>.
         * @return The created {@link InitParams InitParams} instance.
         *
         * @see InitParams
         */
        public static InitParams createInitParams(Activity activity, String customerId)
        {
            return new InitParams(activity, customerId);
        }

        /**
         * Factory method for creating an instance of {@link EntityInfo EntityInfo} for a user.
         *
         * @param  userId  The user ID.
         * @return The created {@link EntityInfo EntityInfo} instance.
         *
         * @see EntityInfo
         */
        public static EntityInfo createUserInfo(String userId)
        {
            return new EntityInfo(SplytConstants.ENTITY_TYPE_USER, userId);
        }

        /**
         * Factory (helper) method used to create an instance of EntityInfo for a device
         *
         * @return The created {@link EntityInfo EntityInfo} instance.
         *
         * @see EntityInfo
         */
        public static EntityInfo createDeviceInfo()
        {
            return new EntityInfo(SplytConstants.ENTITY_TYPE_DEVICE);
        }

        /**
         * Initializes the SPLYT framework for use, including instrumentation and tuning.
         *
         * @param  initParams  Initialization parameters.
         * @param  listener    An application-defined listener which will be called when initialization is complete.
         */
        public static void init(InitParams initParams, SplytListener listener)
        {
            final InitParams params = initParams;
            final EntityInfo user = (null != params.mUserInfo) ? params.mUserInfo : createUserInfo(null);
            final EntityInfo device = (null != params.mDeviceInfo) ? params.mDeviceInfo : createDeviceInfo();
            final SplytListener theListener = listener;

            // Preinit the notification subsystem.
            // This needs to be called before any subsystems are initialized to avoid potential race conditions at startup
            NotificationSubsystem.preinit(params.mActivity, params.Notification.mAlwaysPost, params.Notification.mDisableAutoClear, params.Notification.mReceivedListener);

            InstrumentationSubsystem.init();

            final SplytListener coreListener = new SplytListener() {
                @Override
                public void onComplete(SplytError err)
                {
                    if (SplytError.Success == err)
                    {
                        // The notification subsystem requires the core subsystem to be up since it needs a valid entity Id (guaranteed by the core subsystem) to send to the backend.
                        NotificationSubsystem.init(params.mActivity, params.Notification.mHost, params.Notification.mSmallIcon);
                    }

                    // It's not critical that we wait for the notification subsystem to finish initializing, so we're done
                    theListener.onComplete(err);
                }
            };

            SplytListener tuningListener = new SplytListener() {
                @Override
                public void onComplete(SplytError err)
                {
                    if (SplytError.Success == err)
                    {
                        CoreSubsystem.init(params.mActivity, params.mCustomerId, coreListener, new TuningSubsystem.Updater(),
                            user.mType, user.mId, user.mProperties,
                            device.mType, device.mId, device.mProperties,
                            params.mReqTimeout,
                            params.mHost,
                            params.mLogEnabled, params.mSDKName, params.mSDKVersion);
                    }
                    else
                    {
                        theListener.onComplete(err);
                    }
                }
            };


            TuningSubsystem.init(params.mActivity, tuningListener);
        }

        /**
         * Register a user with SPLYT and make them the currently active user. This can be done at any point when a
         * new user is interacted with by the application. Note that if the active user is known at startup, it is
         * generally ideal to provide their info as part of the `initParams` passed to
         * {@link Splyt.Core#init}.
         *
         * @param  userInfo  An {@link EntityInfo EntityInfo} that describes the user, created with {@link Splyt.Core#createUserInfo createUserInfo}.
         * @param  listener  An application-defined listener which will be called when registration is complete.
         */
        public static void registerUser(EntityInfo userInfo, SplytListener listener)
        {
            CoreSubsystem.registerUser(userInfo.mType, userInfo.mId, userInfo.mProperties, new TuningSubsystem.Updater(), listener);
        }

        /**
         * Explicitly sets the active user ID. Generally only required when multiple concurrent users are
         * required/supported, since {@link Splyt.Core#init} and {@link Splyt.Core#registerUser}
         * both activate the provided user by default.
         *
         * @param  userId  The user ID.
         * @return A {@link SplytError SplytError} enum that describes whether an error occurred.
         */
        public static SplytError setActiveUser(String userId)
        {
            if(!CoreSubsystem.isValidId(userId))
            {
                Util.logError("Trying to activate an invalid id.  If you are trying to clear the active user, use clearActiveUser()");

                return SplytError.ErrorInvalidArgs;
            }
            return CoreSubsystem.setActiveUser(userId);
        }

        /**
         * Clears the active user ID. Useful when the logged in user logs out of the application. Clearing the active
         * user allows SPLYT to provide non user-specific tuning values and report telemetry which is not linked to a user.
         *
         * @return A {@link SplytError SplytError} enum that describes whether an error occurred.
         */
        public static SplytError clearActiveUser()
        {
            return CoreSubsystem.setActiveUser(null);
        }

        /**
         * Pauses SPLYT. This causes SPLYT to save off its state to internal storage and stop checking for events to
         * send. One would typically call this from the `onPause()` method of any Android Activity that
         * makes calls to SPLYT.
         * <p>
         * <b>Note:</b> One can still make calls to SPLYT functions even when it is paused, but doing so will trigger
         * reads and writes to internal storage, so it should be done judiciously.
         */
        public static void pause()
        {
            // Pause the event depot
            CoreSubsystem.pause();
        }

        /**
         * Resumes SPLYT. This causes SPLYT to read its last known state from internal storage and restart polling for
         * events to send. One would typically call this from the `onResume()` method of any Android
         * Activity that makes calls to SPLYT.
         */
        public static void resume()
        {
            // Resume normal operations in the event depot
            CoreSubsystem.resume();
        }
    }

    ///////////////////////////////
    // Instrumentation subsystem //
    ///////////////////////////////

    /**
     * Provides factory methods for creating custom transactions, reporting updated state information about the device
     * that the app is running on, and reporting updated state information about the active user.
     */
    public static class Instrumentation
    {
        /**
         * Reports activity that is taking place in your app to SPLYT.
         * <p>
         * You can think of a {@link Transaction} as a way of reporting some activity in your app that
         * spans time. Here is the common pattern for `Transactions`:
         *
         * <ul>
         * <li>When the activity begins, call {@link Transaction#begin} or one of its variants.
         * <li>As the activity proceeds:
         *     <ol>
         *     <li>Call {@link Transaction#setProperty} or {@link Transaction#setProperties} to update properties of
         *         the transaction that reflect the activity's changing state.
         *     <li>Call {@link Transaction#update} to describe how "far along" the activity is.
         *     </ol>
         * <li>When the activity ends, call {@link Transaction#end}.
         * </ul>
         * <p>
         * <b>Note:</b> Use the factory method {@link Splyt.Instrumentation#Transaction Transaction} to create an
         * instance of this class.
         */
        public static class Transaction
        {
            private final String mCategory;
            private final String mTransactionId;
            protected String mTimeoutMode = SplytConstants.TIMEOUT_MODE_TRANSACTION;
            protected Double mTimeout = -1.0;
            protected Map<String,Object> mProperties;
            private String mResult = SplytConstants.TXN_SUCCESS;

            /**
             * Creates an instance of {@link Transaction}
             *
             * @param  category       The category of the created transaction. Should be a descriptive name for the app
             *                        activity that is modeled by the transaction.
             * @param  transactionId  A unique identifier for the created transaction. This is only required in
             *                        situations where multiple transactions in the same category may exist for the
             *                        same user or device at the same time. Set to `null` if no
             *                        transaction ID is required.
             */
            public Transaction(String category, String transactionId)
            {
                mCategory = category;
                mTransactionId = transactionId;
                mProperties = new HashMap<String,Object>();
            }

            /**
             * Report a single piece of known state about the transaction.
             * <p>
             * Adding properties to transactions allows you to better understand patterns of activity.
             *
             * For example, suppose you have an app that lets users read news stories. If you created a transaction
             * with a category named `ViewStory` when a user starts reading a story, you might set a
             * property on that transaction with a key of `NewsCategory`, and with a value describing what type
             * of story it is, for example, `World News`, `Science & Technology`,
             * `Health`, `Entertainment`, etc. With this property in place, you could create
             * a chart that lets you compare news categories and see which types of stories users read most.
             *
             * Finally, note that properties can change as the transaction progresses. For example, you might have
             * a transaction with a category named `Tutorial` that keeps track of a user's progress
             * through your app tutorial. You could add a `LastStepCompleted` property that you update
             * with a new value as the user completes each step in your tutorial.
             *
             * @param key     The property name.
             * @param value   The property value.
             * @return This {@link Transaction} instance.
             */
            public Transaction setProperty(String key, Object value)
            {
                // Create a dictionary from the key/value
                Map<String,Object> temp = new HashMap<String,Object>();
                temp.put(key, value);

                // Now append these properties
                setProperties(temp);

                return this;
            }

            /**
             * Report any known state about the transaction as a set of key-value pairs.
             *
             * See {@link Transaction#setProperty} for a brief discussion of why it is
             * important to add meaningful properties to transactions.
             *
             * @param  properties  A collection of user or device properties.
             * @return This {@link Transaction} instance.
             */
            public Transaction setProperties(Map<String,Object> properties)
            {
                // Make our own deep copy of what's passed in so that the data doesn't mutate before it's actually sent
                Map<String,Object> copy = Util.deepCopy(properties);

                if (null != copy)
                {
                    // Add to the current set of properties
                    mProperties.putAll(copy);
                }

                return this;
            }


            /**
             * Send telemetry to report the beginning of a transaction.
             * <p>
             * When beginning a transaction, any properties which have been set (see {@link Transaction#setProperty} and
             * {@link Transaction#setProperties}) are also included with the data sent to SPLYT.
             * <p>
             * When calling this method, the transaction will use a default timeout of one hour. That is, if SPLYT
             * does not receive any updates to this transaction for a period longer than one hour, the transaction is
             * considered to have timed out.
             */
            public void begin()
            {
                InstrumentationSubsystem.beginTransaction(mCategory, mTimeoutMode, mTimeout, mTransactionId, mProperties);

                // Clear the properties so we don't waste bandwidth by sending them again on update/end
                // Note that we create a new HashMap as opposed to clearing the objects as those are still being referenced by the event that has yet to be sent
                mProperties = new HashMap<String,Object>();
            }

            /**
             * Send telemetry to report the beginning of a transaction.
             * <p>
             * When beginning a transaction, any properties which have been set (see {@link Transaction#setProperty} and
             * {@link Transaction#setProperties}) are also included with the data sent to SPLYT.
             *
             * @param  timeoutMode  The type of activity which will keep the transaction open.  <b>Note:</b> For this
             *                      release, {@link SplytConstants#TIMEOUT_MODE_TRANSACTION} is the only supported value.
             * @param  timeout      If SPLYT does not receive any updates to this transaction for a period longer than
             *                      the timeout interval specified, the transaction is considered to have timed out.
             */
            public void begin(String timeoutMode, Double timeout)
            {
                mTimeoutMode = timeoutMode;
                mTimeout = timeout;
                begin();
            }

            /**
             * Send telemetry to report the progress of a transaction.
             * <p>
             * When updating the progress of a transaction, any properties which have been added or changed (see
             * {@link Transaction#setProperty} or {@link Transaction#setProperties}) are also included with the data
             * sent to SPLYT.
             *
             * @param  progress  A value between `1` and `99` that describes the percentage
             *                   progress of this transaction. You should treat progress as a strictly increasing
             *                   value. That is, you not should use the same value for progress multiple calls to
             *                   {@link Transaction#update}, nor should you use a lower value for progress than you
             *                   used in a previous call for the same transaction.
             */
            public void update(Integer progress)
            {
                InstrumentationSubsystem.updateTransaction(mCategory, progress, mTransactionId, mProperties);

                // Clear the properties so we don't waste bandwidth by sending them again on end
                // Note that we create a new HashMap as opposed to removing the objects as those are still being referenced by the event that has yet to be sent
                mProperties = new HashMap<String,Object>();
            }

            /**
             * Send telemetry to report the ending of a transaction. The transaction's result will be reported as
             * {@link SplytConstants#TXN_SUCCESS}.
             * <p>
             * End a transaction when the activity it describes is complete.
             */
            public void end()
            {
                InstrumentationSubsystem.endTransaction(mCategory, mResult, mTransactionId, mProperties);

                // Clear the properties in case this transaction happens to be reused.  If so, we expect new properties to be set
                // Note that we create a new HashMap as opposed to removing the objects as those are still being referenced by the event that has yet to be sent
                mProperties = new HashMap<String,Object>();
            }

            /**
             * Send telemetry to report the ending of a transaction.
             *
             * @param  result  A string describing the outcome of the activity described by the transaction.
             *                 Common results include {@link SplytConstants#TXN_SUCCESS} and
             *                 {@link SplytConstants#TXN_ERROR}, but you may use a custom string to describe
             *                 the transaction's outcome. For example, if the user cancelled the activity,
             *                 you might report `cancelled`.
             */
            public void end(String result)
            {
                mResult = result;
                end();
            }

            /**
             * Send telemetry to report an instantaneous transaction.
             * <p>
             * Any properties which have been set (see {@link Transaction#setProperty} and {@link Transaction#setProperties})
             * are also included with the data sent to SPLYT.
             * <p>
             * "Instantaneous transactions" are analogous to "custom events" in some other analytics systems; that
             * is, they report an activity as occurring at an instant in time.
             * <p>
             * <b>Note:</b> Only use {@link Transaction#beginAndEnd} when modeling some activity that really does
             * occur at a single instant; e.g., "the user clicked a button". In other cases, you should describe the
             * activity as beginning at one moment in time, and ending at some later point in time. Doing so presents
             * a couple of advantages:
             * <ol>
             * <li>SPLYT will calculate the duration of the activity automatically, which is a common measure of user
             *     engagement.
             * <li>By describing activities as spanning time, SPLYT can contextualize information about the
             *     transaction together with other data you report to SPLYT during the same time period.
             * </ol>
             */
            public void beginAndEnd()
            {
                end();
            }

            /**
             * Send telemetry to report an instantaneous transaction.
             * <p>
             * See {@link Transaction#beginAndEnd()} for a discussion of when you do and don't want to use
             * "instantaneous transactions."
             *
             * @param  result  A string describing the outcome of the activity described by the transaction
             *                 Common results include {@link SplytConstants#TXN_SUCCESS} and
             *                 {@link SplytConstants#TXN_ERROR}, but you may use a custom string to describe
             *                 the transaction's outcome. For example, if the user cancelled the activity,
             *                 you might report `cancelled`.
             */
            public void beginAndEnd(String result)
            {
                end(result);
            }
        }

        /**
         * Factory method used to create an instance of {@link Splyt.Instrumentation.Transaction}.
         *
         * @param  category  The category of the created transaction. Should be a descriptive name for the app
         *                   activity that is modeled by the transaction.
         *
         * @return The created {@link Splyt.Instrumentation.Transaction} instance.
         */
        public static Transaction Transaction(String category)
        {
            return new Transaction(category, null);
        }

        /**
         * Factory method used to create an instance of {@link Splyt.Instrumentation.Transaction}.
         *
         * @param  category       The category of the created transaction. Should be a descriptive name for the app
         *                        activity that is modeled by the transaction.
         * @param  transactionId  A unique identifier for the created transaction. This is only required in situations
         *                        where multiple transactions in the same category may exist for the same user or
         *                        device at the same time.
         *
         * @return The created {@link Splyt.Instrumentation.Transaction} instance.
         */
        public static Transaction Transaction(String category, String transactionId)
        {
            return new Transaction(category, transactionId);
        }

        /**
         * Updates state information about the active user.
         *
         * @param  properties  A collection of properties that describe the current state of the active user.
         */
        public static void updateUserState(Map<String,Object> properties)
        {
            // Make our own deep copy of what's passed in so that the data doesn't mutate before it's actually sent
            Map<String,Object> copy = Util.deepCopy(properties);

            if (null != copy)
            {
                InstrumentationSubsystem.updateUserState(properties);
            }
        }

        /**
         * Updates state information about the device that the app is running on.
         *
         * @param  properties  A collection of properties that describe the current state of the device.
         */
        public static void updateDeviceState(Map<String,Object> properties)
        {
            // Make our own deep copy of what's passed in so that the data doesn't mutate before it's actually sent
            Map<String,Object> copy = Util.deepCopy(properties);

            if (null != copy)
            {
                InstrumentationSubsystem.updateDeviceState(properties);
            }
        }

        /**
         * Updates a collection balance for the active user.
         *
         * @param  name                 The application-supplied name for the collection.
         * @param  balance              The new balance of the collection.
         * @param  balanceModification  The amount that the balance is changing by (if known).
         * @param  isCurrency           `true` if the collection represents an in-app virtual currency;
         *                              `false` otherwise.
         */
        public static void updateCollection(String name, Double balance, Double balanceModification, Boolean isCurrency)
        {
            InstrumentationSubsystem.updateCollection(name, balance, balanceModification, isCurrency);
        }
    }

    /**
     * Provides access to SPLYT's dynamic tuning variables.
     * <p>
     * Tuning variables are defined in SPLYT's web app and can have customized values on a per-user basis, depending
     * on whether the active user of the app is participating in an A/B test, or belongs to a segment of users that
     * has been assigned customized values for one or more tuning variables.
     */
    public static class Tuning
    {
        /**
         * Retrieves updated values from SPLYT for all tuning variables.
         * <p>
         * If multiple users are registered (see {@link Core#registerUser}), updated values will be retrieved
         * for all of them.
         *
         * @param  listener    An application-defined listener which will be called when initialization is complete.
         */
        public static void refresh(SplytListener listener)
        {
            TuningSubsystem.refresh(listener);
        }

        /**
         * Gets the value of a named tuning variable from SPLYT.  The type parameter `T` indicates the type of
         * the value being retrieved.
         * <p>
         * <b>Note:</b> This is not an asynchronous or blocking operation. Tuning values are proactively cached by the
         * SPLYT framework during {@link Core#init}, {@link Core#registerUser}, and
         * {@link Tuning#refresh}.
         *
         * @param  varName       Application-defined name of a tuning variable to retrieve.
         * @param  defaultValue  A default value for the tuning variable, used when a dynamic value has not been
         *                       specified or is otherwise not available.
         * @return The dynamic value of the variable, or the default value (if the dynamic value could not be
         *         retrieved). <b>Note:</b> The return value is guaranteed to match the type of
         *         `defaultValue`. If a dynamic value is set in SPLYT which cannot be converted into the
         *         proper type, the default will be returned.
         */
        public static <T> T getVar(String varName, T defaultValue)
        {
            @SuppressWarnings("unchecked")
            T val = (T) TuningSubsystem.getVar(CoreSubsystem.getUserId(), CoreSubsystem.getDeviceId(), varName, (Object)defaultValue);

            return val;
        }
    }
}