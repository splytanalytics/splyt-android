package com.rsb.splyt;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;

import com.rsb.gson.Gson;
import com.rsb.gson.JsonObject;
import com.rsb.gson.JsonParser;
import com.rsb.splyt.HttpRequest.RequestListener;
import com.rsb.splyt.HttpRequest.RequestResult;

/**
 * <p>Tuning Subsystem</p>
 *
 * @author Copyright 2015 Knetik, Inc.
 * @version 1.0
 */
class TuningSubsystem
{
    private static final String CACHE_FILENAME = "splyt_tuningCache";

    private static Context sContext;

    private static TuningValues sCacheVars = new TuningValues();

    private static boolean sInitialized;

    /**
     * Performs required initialization for the tuning subsystem
     *
     * @param context Application context to use for caching tuning variable information
     */
    static void init(Context context, SplytListener listener)
    {
        if (!sInitialized)
        {
            sContext = context;

            try
            {
                // Pull in the state data if there is any
                FileInputStream fis = sContext.openFileInput(CACHE_FILENAME);
                ObjectInputStream inputStream = new ObjectInputStream(fis);
                sCacheVars = (TuningValues) inputStream.readObject();

                fis.close();
            }
            catch (Exception ex)
            {
                // Some error occurred reading the cache file.  It may be that the file simply doesn't exist.
                // In any case, we can handle this situation, so carry on
            }
            finally
            {
                sInitialized = true;
            }
        }

        listener.onComplete(SplytError.Success);
    }

    static void refresh(SplytListener listener)
    {
        SplytError ret = SplytError.Success;

        if ((CoreSubsystem.InitializationState.Initialized == CoreSubsystem.getInitializationState()) && (null != sContext))
        {
            String url = CoreSubsystem.getHost() + "/isos-personalization/ws/interface/tuner_refresh" + CoreSubsystem.getQueryParms();

            List<Object> allArgs = new ArrayList<Object>(4);
            Double curTimeStamp = Double.valueOf(Util.MicroTimestamp.INSTANCE.get());
            allArgs.add(curTimeStamp);
            allArgs.add(curTimeStamp);
            allArgs.add(CoreSubsystem.getDeviceId());
            allArgs.add(CoreSubsystem.getRegisteredUsers());

            final SplytListener theListener = listener;
            RequestListener requestListener = new RequestListener() {
                @Override
                public void onComplete(RequestResult result) {
                    SplytError err = parseRefreshResponse(result);
                    if(null != theListener)
                    {
                        theListener.onComplete(err);
                    }
                }
            };

            try
            {
                // Create an (async) request to retrieve a device Id.  The callback will be triggered when the request is completed
                new HttpRequest(new URL(url), CoreSubsystem.getReqTimeout(), new Gson().toJson(allArgs)).executeAsync(requestListener);
            }
            catch (MalformedURLException e)
            {
                Util.logError("MalformedURLException during the HttpRequest.  Check your host and customerId values", e);
                ret = SplytError.ErrorInvalidArgs;
            }
            catch (Exception e)
            {
                Util.logError("Exception during HttpRequest", e);
                ret = SplytError.ErrorGeneric;
            }
        }
        else
        {
            Util.logError("Cannot refresh tuning because Splyt is not initialized");
            ret = SplytError.ErrorNotInitialized;
        }

        // if we have an error at this point, then the listener will not get called through the HttpRequest, so call it now
        if(SplytError.Success != ret && null != listener)
        {
            listener.onComplete(ret);
        }
    }

    /**
     * Get the value of a named variable from Splyt.  If {@link Splyt#cacheVariables cacheVariables} has not been called or has not finished, this function will return the default value provided.
     *
     * @param userId        The user id, or null
     * @param deviceId      The device id
     * @param varName       The name of the variable to retrieve.
     * @param defaultValue  Java Object representing the default value to use.
     *
     * @return The value of the variable (or the default value)
     * <p><b>Note:</b> The return value is guaranteed to match the type of the defaultValue passed in.</p>
     */
    public static Object getVar(String userId, String deviceId, String varName, Object defaultValue)
    {
        String entityType = SplytConstants.ENTITY_TYPE_DEVICE;
        String entityId = deviceId;
        if(null != userId)
        {
            entityType = SplytConstants.ENTITY_TYPE_USER;
            entityId = userId;
        }

        // Assume we're going to return the defaultValue
        Object retVal = defaultValue;

        // grab the tuning value from cache & try to convert it to the expected type
        Object tuningVal = sCacheVars.getValue(entityType, entityId, varName, defaultValue);
        tuningVal = Util.converttype(tuningVal, defaultValue);
        if (null != tuningVal)
        {
            // We successfully converted the retrieved tuning variable to the desired type
            retVal = tuningVal;
        }

        return retVal;
    }

    private static void flushCache()
    {
        // We have some state data, so save it off
        try
        {
            FileOutputStream fos = sContext.openFileOutput(CACHE_FILENAME, Context.MODE_PRIVATE);
            ObjectOutputStream outputStream = new ObjectOutputStream(fos);
            outputStream.writeObject(sCacheVars);
            outputStream.flush();
            outputStream.close();
        }
        catch (Exception ex)
        {
            Util.logError("Failed to save tuning vars to cache", ex);
        }
    }

    // this can't be declared INSIDE parseRefreshResponse (as per http://stackoverflow.com/questions/10927699/simplest-gson-fromjson-example-fails)
    //  but we'll at least put the code as close as possible
    private class refreshResponse
    {
        class tuningBundle
        {
            @SuppressWarnings("unused")
            String status;
            @SuppressWarnings("unused")
            String type;
            Map<String, Object> value;
        }
        class ssfBundle
        {
            int error;
            String description;
            tuningBundle data;
        }

        ssfBundle deviceTuning;
        ssfBundle userTuning;
    }

    private static SplytError parseRefreshResponse(RequestResult result)
    {
        SplytError error = result.error;

        if(SplytError.Success == error)
        {
            TuningUpdater updater = new Updater();

            try
            {
                JsonParser parser = new JsonParser();
                JsonObject obj = parser.parse(result.response).getAsJsonObject();
                error = SplytError.fromInt(obj.getAsJsonPrimitive("error").getAsInt());
                if (SplytError.Success == error)
                {
                    Gson gson = new Gson();

                    refreshResponse data = gson.fromJson(obj.getAsJsonObject("data"), refreshResponse.class);

                    if (null != data.deviceTuning && null != data.deviceTuning.data)
                    {
                        updater.onUpdate(SplytConstants.ENTITY_TYPE_DEVICE, CoreSubsystem.getDeviceId(), data.deviceTuning.data.value);
                    }
                    else
                    {
                        Util.logError("Error processing device tuning:");
                        if(null != data.deviceTuning)
                        {
                            Util.logError(SplytError.fromInt(data.deviceTuning.error).toString() + "(" + data.deviceTuning.description + ")");
                        }
                    }

                    if (null != data.userTuning && null != data.userTuning.data && null != data.userTuning.data.value)
                    {
                        for (Map.Entry<String, Object> entry : data.userTuning.data.value.entrySet()) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> values = (Map<String, Object>) entry.getValue();
                            if(null != values)
                            {
                                updater.onUpdate(SplytConstants.ENTITY_TYPE_USER, entry.getKey(), values);
                            }
                            else
                            {
                                Util.logError("User tuning data missing for user " + entry.getKey() + " in refresh response");
                            }
                        }
                    }
                    else
                    {
                        Util.logError("Error processing user tuning:");
                        if(null != data.userTuning)
                        {
                            Util.logError(SplytError.fromInt(data.userTuning.error).toString() + "(" + data.userTuning.description + ")");
                        }
                    }
                }
                else
                {
                    Util.logError("Error received in refresh response from server: ");
                    Util.logError(error.toString() + "(" + obj.getAsJsonPrimitive("description").getAsString() + ")");
                }
            }
            catch(Exception e)
            {
                Util.logError("Exception parsing refresh response: ", e);
                error = SplytError.ErrorGeneric;
            }
            finally
            {
                updater.commit();
            }
        }
        else
        {
            Util.logError("refresh() failed on server.  SSF Error: " + error);
        }

        return error;
    }

    public static class Updater implements TuningUpdater
    {
        private boolean mDirty = false;

        @Override
        public void onUpdate(String type, String id, Map<String, Object> values) {

            sCacheVars.updateEntity(type, id, values);

            mDirty = true;
        }

        @Override
        public void onClear(String type, String id) {

            sCacheVars.removeEntity(type, id);

            mDirty = true;
        }

        @Override
        public void commit()
        {
            if(mDirty)
            {
                flushCache();
                mDirty = false;
            }
        }
    }

    @SuppressWarnings("serial")
    private static class TuningValues implements java.io.Serializable
    {
        private Map< String, Map<String, Object > > mStorage = new HashMap<String, Map<String, Object> >();
        private Map< String, Double > mUsed = new HashMap<String, Double>();

        void updateEntity(String type, String id, Map<String, Object> values)
        {
            if(!mStorage.containsKey(type))
            {
                mStorage.put(type, new HashMap<String, Object>());
            }

            Map<String, Object> typeStorage = mStorage.get(type);
            typeStorage.put(id, values);
        }

        void removeEntity(String type, String id)
        {
            if(!mStorage.containsKey(type))
            {
                mStorage.put(type, new HashMap<String, Object>());
            }

            Map<String, Object> typeStorage = mStorage.get(type);
            if(typeStorage.containsKey(id))
            {
                typeStorage.remove(id);
            }
        }

        Object getValue(String type, String id, String var, Object defaultValue)
        {
            // let the splyt backend know that this request took place
            Double curTimeStamp = Double.valueOf(Util.MicroTimestamp.INSTANCE.get());
            if(!mUsed.containsKey(var) || curTimeStamp > mUsed.get(var) + SplytConstants.TIME_RECORDAGAIN)
            {
                mUsed.put(var, curTimeStamp);
                new CoreSubsystem.DataPointBuilder("tuner_recordUsed")
                    .setArg(var)
                    .setArg(defaultValue)
                    .send();
            }

            Object ret = null;
            if(mStorage.containsKey(type))
            {
                Map<String, Object> typeStorage = mStorage.get(type);
                if(typeStorage.containsKey(id))
                {
                    @SuppressWarnings("unchecked")
                    Map<String,Object> entityData = (Map<String, Object>)typeStorage.get(id);
                    if(entityData.containsKey(var))
                    {
                        ret = entityData.get(var);
                    }
                }
            }
            return ret;
        }
    }
}