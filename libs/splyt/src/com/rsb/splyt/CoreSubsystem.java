package com.rsb.splyt;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.content.LocalBroadcastManager;

import com.rsb.gson.Gson;
import com.rsb.gson.JsonObject;
import com.rsb.gson.JsonParser;
import com.rsb.gson.JsonPrimitive;
import com.rsb.splyt.HttpRequest.RequestListener;
import com.rsb.splyt.HttpRequest.RequestResult;

/**
 * <p>This is the core library for Splyt which supports sending data points to and retrieving tuning variables from
 * the system.  In addition to this base functionality, there are also some helper functions for setting whether
 * a user and/or device is new.</p>
 * 
 * <p>Additional Splyt functionality is added using plugins.  Plugins are added by including the appropriate
 * library (jar) files in addition to this core library.</p>
 * @author Copyright 2013 Row Sham Bow, Inc.
 * @version 1.0
 */
class CoreSubsystem
{
    // Private "constants"
    private static final String WS_VERSION        = "4";
    private static final String DEVICEID_KEY_NAME = "deviceId";

    private static String sUserId = null;
    static String getUserId() { return sUserId; }
    private static void setUserId(String value)
    {
        sUserId = value;

        if (null != sAppContext)
        {
            LocalBroadcastManager.getInstance(sAppContext).sendBroadcast(new Intent(SplytConstants.ACTION_CORESUBSYSTEM_SETUSERID));
        }
    }
    private static List<String> sRegisteredUsers = new ArrayList<String>();
    static List<String> getRegisteredUsers() { return sRegisteredUsers; };

    private static String sDeviceId = null;
    static String getDeviceId() { return sDeviceId; }
    private static void setDeviceId(String value) { sDeviceId = value; }

    enum InitializationState {
        Uninitialized,
        Initializing,
        Failed,
        Initialized
    }
    private static InitializationState sInitializationState = InitializationState.Uninitialized;
    static InitializationState getInitializationState() { return sInitializationState; }

    private static String sHost;
    static String getHost() { return sHost; }

    private static int sReqTimeout;
    static int getReqTimeout() { return sReqTimeout; }

    private static Context sAppContext;
    private static String sCustomerId;
    private static String sSDKName;
    private static String sSDKVersion;

    /**
     * Initialize the Splyt system.
     * 
     * @param context
     * 
     * @return A {@link SplytError} value
     */
    static void init(Context context, String customerId, SplytListener listener, TuningUpdater tuningUpdater, String userEntityType, String userId, Map<String, Object> userProperties, String deviceEntityType, String deviceId, Map<String, Object> deviceProperties, int reqTimeout, String host, boolean logEnabled, String sdkName, String sdkVersion)
    {
        // Have we already attempted to initialize Splyt Core?
        if (InitializationState.Uninitialized == sInitializationState)
        {
            // Assume success
            SplytError ret = SplytError.Success;

            // Enable/disable logging
            Util.setLogEnabled(logEnabled);

            // Verify that we've been passed a valid context
            if (null == context)
            {
                Util.logError("Context is null.  Please pass in a valid context");
                ret = SplytError.ErrorInvalidArgs;
            }
            else if (null == listener)
            {
                Util.logError("Please provide a valid SplytListener implementation");
                ret = SplytError.ErrorInvalidArgs;
            }
            else if (SplytConstants.ENTITY_TYPE_USER != userEntityType)
            {
                Util.logError("To provide intitial user settings, be sure to use createUserInfo()");
                ret = SplytError.ErrorInvalidArgs;
            }
            else if (SplytConstants.ENTITY_TYPE_DEVICE != deviceEntityType)
            {
                Util.logError("To provide intitial device settings, be sure to use createDeviceInfo()");
                ret = SplytError.ErrorInvalidArgs;
            }

            if (SplytError.Success == ret)
            {
                sCustomerId = customerId;
                sReqTimeout = reqTimeout;
                sHost = host;
                sSDKName = sdkName;
                sSDKVersion = sdkVersion;
                sAppContext = context.getApplicationContext();
                sInitializationState = InitializationState.Initializing;

                Util.cacheDeviceAndAppInfo(sAppContext);

                // First see if we have a deviceId stored off locally that we can use
                SharedPreferences sharedPrefs = sAppContext.getSharedPreferences(SplytConstants.PREFS_FILENAME, Context.MODE_PRIVATE);

                if (!isValidId(deviceId))
                {
                    String savedDeviceId = sharedPrefs.getString(DEVICEID_KEY_NAME, null);

                    if (isValidId(savedDeviceId))
                    {
                        deviceId = savedDeviceId;
                    }
                    // else use proper device ID (ad id?)
                }

                // set up device id & user id now, in case initial server call doesn't make it back (offline usage, etc)
                if (isValidId(deviceId)) setDeviceId(deviceId);
                if (isValidId(userId)) setUserId(userId);

                // add any auto-scraped device state
                if (null == deviceProperties)
                {
                    deviceProperties = Util.getDeviceAndAppInfo();
                }
                else
                {
                    deviceProperties.putAll(Util.getDeviceAndAppInfo());
                }

                // No device Id, so let's retrieve one and save it off
                String url = sHost + "/isos-personalization/ws/interface/application_init" + getQueryParms();
                List<Object> allArgs = new ArrayList<Object>(6);
                Double curTimeStamp = Double.valueOf(Util.MicroTimestamp.INSTANCE.get());
                allArgs.add(curTimeStamp);
                allArgs.add(curTimeStamp);
                allArgs.add(userId);
                allArgs.add(deviceId);
                allArgs.add(userProperties);
                allArgs.add(deviceProperties);

                try
                {
                    // Create an (async) request to retrieve a device Id. The callback will be triggered when the request is completed
                    new HttpRequest(new URL(url), sReqTimeout, new Gson().toJson(allArgs)).executeAsync(new InitRequestListener(sharedPrefs, tuningUpdater, userProperties, deviceProperties, listener));
                }
                catch (MalformedURLException e)
                {
                    reset();

                    Util.logError("MalformedURLException during the HttpRequest.  Check your host and customerId values");
                    ret = SplytError.ErrorInvalidArgs;
                }
                catch (Exception e)
                {
                    reset();

                    Util.logError("Error during HttpRequest: " + e.getMessage());
                    ret = SplytError.ErrorGeneric;
                }
            }

            // if we have an error at this point, then the listener will not get called through the HttpRequest, so call it now
            if (SplytError.Success != ret && null != listener)
            {
                listener.onComplete(ret);
            }
        }
        else
        {
            // If the subsystem is already successfully initialized, return success, otherwise this call is an error.
            if (null != listener)
            {
                SplytError ret = (InitializationState.Initialized == sInitializationState) ? SplytError.Success : SplytError.ErrorGeneric;
                listener.onComplete(ret);
            }
        }
    }

    static void registerUser(String userEntityType, String userId, Map<String, Object> userProperties, TuningUpdater tuningUpdater, SplytListener listener)
    {
        SplytError ret = SplytError.Success;

        if (InitializationState.Initialized != sInitializationState)
        {
            Util.logError("Cannot registerUser before successful initialization");
            ret = SplytError.ErrorNotInitialized;
        }
        else if (!isValidId(getDeviceId()))
        {
            Util.logError("No device Id set.  Check for prior errors");
            ret = SplytError.ErrorMissingId;
        }
        else if (SplytConstants.ENTITY_TYPE_USER != userEntityType)
        {
            Util.logError("To provide intitial user settings, be sure to use createUserInfo()");
            ret = SplytError.ErrorInvalidArgs;
        }

        if (SplytError.Success == ret)
        {
            String deviceId = getDeviceId();

            String url = sHost + "/isos-personalization/ws/interface/application_updateuser" + getQueryParms();
            List<Object> allArgs = new ArrayList<Object>(2);
            Double curTimeStamp = Double.valueOf(Util.MicroTimestamp.INSTANCE.get());
            allArgs.add(curTimeStamp);
            allArgs.add(curTimeStamp);
            allArgs.add(userId);
            allArgs.add(deviceId);

            // TODO: It's not a good idea to go around the event depot (out of order issue)
            allArgs.add(userProperties);

            try
            {
                // Create an (async) request to retrieve a device Id. The callback will be triggered when the request is completed
                new HttpRequest(new URL(url), sReqTimeout, new Gson().toJson(allArgs)).executeAsync(new InitRequestListener(null, tuningUpdater, userProperties, null, listener));
            }
            catch (MalformedURLException e)
            {
                Util.logError("MalformedURLException during the HttpRequest.  Check your host and customerId values");
                ret = SplytError.ErrorInvalidArgs;
            }
            catch (Exception e)
            {
                Util.logError("Error during HttpRequest: " + e.getMessage());
                ret = SplytError.ErrorGeneric;
            }
        }

        // if we have an error at this point, then the listener will not get called through the HttpRequest, so call it now
        if (SplytError.Success != ret && null != listener)
        {
            listener.onComplete(ret);
        }
    }

    /*
     * Explicitly sets the active user id.  Only required when multiple concurrent users
     * are required/supported
     */
    static SplytError setActiveUser(String userId)
    {
        if (!isValidId(userId))
        {
            setUserId(null);
            return SplytError.Success;
        }

        if (sRegisteredUsers.contains(userId))
        {
            setUserId(userId);
            return SplytError.Success;
        }

        Util.logError("User ID " + userId + " has not been registered.  Be sure to call registerUser to prep an id for usage.");
        return SplytError.ErrorInvalidArgs;
    }

    /**
     * Pause the Splyt system.  This causes Splyt to save off its state to Internal Storage and stop checking for events to send.
     * One would typically call this in the onPause() of any activity that makes calls to Splyt.
     * 
     * <p><b>Note:</b> One can still make calls to Splyt functions even when it's paused,
     * but doing so will trigger reads and writes to Internal Storage, so it is discouraged.</p>
     */
    static void pause()
    {
        // Pause the event depot
        EventDepot.pause();

        // Broadcast that the core subsystem has been paused
        if (null != sAppContext)
        {
            LocalBroadcastManager.getInstance(sAppContext).sendBroadcast(new Intent(SplytConstants.ACTION_CORESUBSYSTEM_PAUSED));
        }
    }

    /**
     * Resume the Splyt system.  This causes Splyt read its last known state from Internal Storage and restart polling for events to send.
     * One would typically call this in the onResume() of any activity that makes calls to Splyt.
     */
    static void resume()
    {
        // Resume normal operations in the event depot
        EventDepot.resume();

        // Broadcast that the core subsystem has been resumed
        if (null != sAppContext)
        {
            LocalBroadcastManager.getInstance(sAppContext).sendBroadcast(new Intent(SplytConstants.ACTION_CORESUBSYSTEM_RESUMED));
        }
    }

    /**
     * Helper class
     * 
     * @internal
     * 
     * NOTE: Using this builder pattern as opposed to a static method allows us to support "default parameters"
     */
    static class DataPointBuilder
    {
        private final String       _call;
        private final List<Object> _args = new ArrayList<Object>();

        DataPointBuilder(String call)
        {
            _call = call;
        }

        SplytError send()
        {
            // Assume success
            SplytError ret = SplytError.Success;

            if (InitializationState.Initialized == sInitializationState)
            {
                // Build up the data object
                List<Object> allArgs = new ArrayList<Object>();

                Double curTimeStamp = Double.valueOf(Util.MicroTimestamp.INSTANCE.get());
                // The interface calls require two time stamps (to support batching), so we'll send the same one for both
                allArgs.add(curTimeStamp);
                allArgs.add(curTimeStamp);
                allArgs.add(getUserId());
                allArgs.add(getDeviceId());
                allArgs.addAll(_args);

                // Build the event and store it in the depot
                Map<String, Object> event = new HashMap<String, Object>(2);
                event.put("method", _call);
                event.put("args", allArgs);

                ret = EventDepot.store(event);
            }
            else
            {
                ret = SplytError.ErrorNotInitialized;
            }

            return ret;
        }

        DataPointBuilder setArg(Object obj)
        {
            _args.add(obj);
            return this;
        }
    }

    static boolean isValidId(String id)
    {
        return ((null != id) && ("" != id));
    }

    static String getQueryParms()
    {
        return getQueryParms(WS_VERSION);
    }

    static String getQueryParms(String wsVersion)
    {
        return "?ssf_ws_version=" + wsVersion + "&ssf_cust_id=" + sCustomerId + "&ssf_output=json&ssf_sdk=" + sSDKName + "&ssf_sdk_version=" + sSDKVersion;
    }

    // //////////////////////////
    // Private helper methods //
    // //////////////////////////

    private static void reset()
    {
        // Reset all of the static vars to their default values
        sCustomerId = null;
        sSDKName = null;
        sSDKVersion = null;
        sReqTimeout = 0;
        sHost = null;
        setUserId(null);
        setDeviceId(null);
        sInitializationState = InitializationState.Uninitialized;
    }

    // //////////////////////////
    // Private helper classes //
    // //////////////////////////

    /**
     * Request listener used for both the {@link init} and {@link registerUser} calls.  The important distinction
     * between the server response from these calls is that only the init() call contains a deviceId.  This is also
     * why the shared preferences is not provided during the registerUser() call, as it is unneeded.
     * 
     */
    private static class InitRequestListener implements RequestListener
    {
        private final SharedPreferences   mSharedPrefs;
        private final TuningUpdater       mTuningUpdater;
        private final SplytListener       mListener;
        private final Map<String, Object> mUserProperties;
        private final Map<String, Object> mDeviceProperties;

        class initRet
        {
            String              userid;
            Map<String, Object> usertuning;
            boolean             usernew;
            String              deviceid;
            Map<String, Object> devicetuning;
            boolean             devicenew;
        }

        InitRequestListener(SharedPreferences sharedPrefs, TuningUpdater tuningUpdater, Map<String, Object> userProperties, Map<String, Object> deviceProperties, SplytListener listener)
        {
            mSharedPrefs = sharedPrefs;
            mTuningUpdater = tuningUpdater;

            // Make deep copies of the properties so that we can guarantee no mutation of the data
            Map<String,Object> temp = Util.deepCopy(userProperties);
            mUserProperties = (null != temp) ? temp : null;
            temp = Util.deepCopy(deviceProperties);
            mDeviceProperties = (null != temp) ? temp : null;

            mListener = listener;
        }

        @Override
        public void onComplete(RequestResult result)
        {
            SplytError retError = SplytError.ErrorGeneric;
            boolean userNew = false;
            boolean deviceNew = false;

            if (SplytError.Success == result.error)
            {
                try
                {
                    Gson gson = new Gson();

                    JsonParser parser = new JsonParser();
                    JsonObject obj = parser.parse(result.response).getAsJsonObject();
                    JsonPrimitive error = obj.getAsJsonPrimitive("error");
                    if ((null != error) && (SplytError.Success.getValue() == error.getAsInt()))
                    {
                        JsonObject data = obj.getAsJsonObject("data");
                        initRet ret = gson.fromJson(data, initRet.class);

                        // NOTE: deviceId should only be set during the callback from init()
                        if (null != mSharedPrefs && isValidId(ret.deviceid))
                        {
                            deviceNew = ret.devicenew;

                            // Save it off
                            SharedPreferences.Editor editor = mSharedPrefs.edit();
                            editor.putString(DEVICEID_KEY_NAME, ret.deviceid);
                            editor.commit();

                            mTuningUpdater.onUpdate(SplytConstants.ENTITY_TYPE_DEVICE, ret.deviceid, ret.devicetuning);
                            setDeviceId(ret.deviceid);
                        }

                        // now handle the user id if there is one
                        if (isValidId(ret.userid))
                        {
                            userNew = ret.usernew;

                            mTuningUpdater.onUpdate(SplytConstants.ENTITY_TYPE_USER, ret.userid, ret.usertuning);
                            if (!sRegisteredUsers.contains(ret.userid))
                            {
                                sRegisteredUsers.add(ret.userid);
                            }
                            setUserId(ret.userid);
                        }
                    }
                    else
                    {
                        JsonPrimitive desc = obj.getAsJsonPrimitive("description");
                        Util.logError("Problem on initialization, error description: " + desc.getAsString());
                        retError = SplytError.ErrorGeneric;
                    }
                }
                catch (Exception e)
                {
                    Util.logError("Exception during intialization: " + result.response);
                    retError = SplytError.ErrorGeneric;
                }

                mTuningUpdater.commit();
            }
            else
            {
                // Request failure (likely a timeout), pass it through
                Util.logError("Initialization call failed: code " + result.error);
                retError = result.error;
            }

            // even if the init call failed, all is well as long as we AT LEAST have a device id
            if (isValidId(getDeviceId()))
            {
                // Initialize the event depot now that the subsystem is initialized and will accept events
                EventDepot.init(sAppContext, sHost, getQueryParms(), sReqTimeout);

                sInitializationState = InitializationState.Initialized;

                // queue up some telemetry for the initial state...
                if(null != mDeviceProperties)
                    new DataPointBuilder("datacollector_updateDeviceState").setArg(mDeviceProperties).send();
                if(null != mUserProperties)
                    new DataPointBuilder("datacollector_updateUserState").setArg(mUserProperties).send();
                if(deviceNew)
                    new DataPointBuilder("datacollector_newDevice").send();
                if(userNew)
                    new DataPointBuilder("datacollector_newUser").send();

                // TODO - decide if we want to send a TuningFailed error at this point, if there was some kind of error?

                retError = SplytError.Success;
            }
            else
            {
                // Failed to properly initialize
                sInitializationState = InitializationState.Failed;
            }

            // Make a local broadcast that initialization has completed (either successfully or not)
            LocalBroadcastManager.getInstance(sAppContext).sendBroadcast(new Intent(SplytConstants.ACTION_CORESUBSYSTEM_INITIALIZATIONCOMPLETE));

            // Call the callback
            if (null != mListener)
            {
                mListener.onComplete(retError);
            }
        }
    }
}