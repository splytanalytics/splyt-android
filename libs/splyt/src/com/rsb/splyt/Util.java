package com.rsb.splyt;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Currency;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.util.Log;

/**
 * <p>This is an internal utility class used in the core library for Splyt</p>
 */
class Util
{
    private static final String LOG_TAG = "com.rsb.splyt";
    private static boolean sLogEnabled = false;
    private static Map<String, Object> sDeviceAndAppInfo = new HashMap<String, Object>();
    static Map<String, Object> getDeviceAndAppInfo() { return sDeviceAndAppInfo; }
    private static Set<String> sValidCurrencyCodes = new HashSet<String>();
    private static Map<String, Set<String>> sCurrencyCodesBySymbol = new HashMap<String, Set<String>>();

    static void setLogEnabled(boolean value)
    {
        sLogEnabled = value;
    }

    // Utility function to handle type juggling
    // Only handles src and target objects that are Booleans, Strings, Numbers (most subclasses)
    static Object converttype(Object src, Object target)
    {
        Object retObj = null;
        if ((null != src) && (null != target))
        {
            try
            {
                if (src instanceof Boolean)
                {
                    boolean boolVal = ((Boolean)src).booleanValue();
                    if (target instanceof Boolean)
                    {
                        retObj = Boolean.valueOf(boolVal);
                    }
                    else if (target instanceof String)
                    {
                        retObj = String.valueOf(boolVal);
                    }
                    else if (target instanceof Byte)
                    {
                        retObj = Byte.valueOf((byte)(boolVal ? 1 : 0));
                    }
                    else if (target instanceof Double)
                    {
                        retObj = Double.valueOf(boolVal ? 1.0d : 0.0d);
                    }
                    else if (target instanceof Float)
                    {
                        retObj = Float.valueOf(boolVal ? 1.0f : 0.0f);
                    }
                    else if (target instanceof Integer)
                    {
                        retObj = Integer.valueOf(boolVal ? 1 : 0);
                    }
                    else if (target instanceof Short)
                    {
                        retObj = Short.valueOf((short)(boolVal ? 1 : 0));
                    }
                    else if (target instanceof Long)
                    {
                        retObj = Long.valueOf(boolVal ? 1L : 0L);
                    }
                }
                else if (src instanceof String)
                {
                    String stringVal = (String)src;
                    if (target instanceof Boolean)
                    {
                        retObj = Boolean.valueOf(stringVal);
                    }
                    else if (target instanceof String)
                    {
                        retObj = String.valueOf(stringVal);
                    }
                    else if (target instanceof Byte)
                    {
                        retObj = Byte.valueOf(stringVal);
                    }
                    else if (target instanceof Double)
                    {
                        retObj = Double.valueOf(stringVal);
                    }
                    else if (target instanceof Float)
                    {
                        retObj = Float.valueOf(stringVal);
                    }
                    else if (target instanceof Integer)
                    {
                        retObj = Integer.valueOf(stringVal);
                    }
                    else if (target instanceof Short)
                    {
                        retObj = Short.valueOf(stringVal);
                    }
                    else if (target instanceof Long)
                    {
                        retObj = Long.valueOf(stringVal);
                    }
                }
                else if (src instanceof Number)
                {
                    // Refer to http://docs.oracle.com/javase/tutorial/java/data/numberclasses.html
                    if (target instanceof Boolean)
                    {
                        retObj = Boolean.valueOf((0 == ((Number)src).intValue()) ? false : true);
                    }
                    else if (target instanceof String)
                    {
                        retObj = String.valueOf(src);
                    }
                    else if (target instanceof Byte)
                    {
                        retObj = Byte.valueOf(((Number)src).byteValue());
                    }
                    else if (target instanceof Double)
                    {
                        retObj = Double.valueOf(((Number)src).doubleValue());
                    }
                    else if (target instanceof Float)
                    {
                        retObj = Float.valueOf(((Number)src).floatValue());
                    }
                    else if (target instanceof Integer)
                    {
                        retObj = Integer.valueOf(((Number)src).intValue());
                    }
                    else if (target instanceof Short)
                    {
                        retObj = Short.valueOf(((Number)src).shortValue());
                    }
                    else if (target instanceof Long)
                    {
                        retObj = Long.valueOf(((Number)src).longValue());
                    }
                }
            }
            catch (Exception e) { } // Trap any error and just return null
        }

        return retObj;
    }

    // Utility method that performs a deep copy of a java Object (typically a Map)
    // We do this by serializing the Object to a byte array and then deserializing the byte array back into the Object
    // This works as long as all of the objects (values) are serializable.
    static <T extends Object> T deepCopy(T source)
    {
        try
        {
            // Serialize

            // Create an output stream for writing content to an (internal) byte array.
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

            // Create a specialized (wrapper) OutputStream that writes to the ByteArrayOutputStream and is able to write (serialize) Java objects as well as primitive data types (int, byte, char etc.).
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);

            // Use the specialized OutputStream to write the data to the ByteArrayOutputStream
            objectOutputStream.writeObject(source);

            // Deserialize

            // Create a specialized InputStream for reading the contents of our byte array (serialized data)
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());

            // Create a specialized (wrapper) InputStream that reads from the ByteArrayInputStream and is able to read (deserialize) Java objects as well as primitive data types (int, byte, char etc.)
            ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);

            // Use the specialized InputStream to read the data from the ByteArrayInputStream
            @SuppressWarnings("unchecked")
            T copy = (T)objectInputStream.readObject();

            // Close all of the streams to make sure any resources associated with them are freed
            objectInputStream.close();
            byteArrayInputStream.close();
            objectOutputStream.close();
            byteArrayOutputStream.close();

            return copy;
        }
        catch (IOException e)
        {
            logError("IOException in deepCopy", e);
        }
        catch (ClassNotFoundException e)
        {
            logError("ClassNotFoundException in deepCopy", e);
        }

        return null;
    }

    // Internal logging.  These can be enabled by calling Util.setLogEnabled(true)
    static void logDebug(String msg)
    {
        if (sLogEnabled)
        {
            Log.d(LOG_TAG, msg);
        }
    }

    static void logError(String msg, Exception e)
    {
        if (sLogEnabled)
        {
            Log.e(LOG_TAG, msg, e);
        }
    }

    static void logError(String msg)
    {
        if (sLogEnabled)
        {
            Log.e(LOG_TAG, msg);
        }
    }

    @SuppressWarnings("deprecation")
    static void cacheDeviceAndAppInfo(Context context)
    {
        if (null != context)
        {
            // Clear out any previously set data
            sDeviceAndAppInfo.clear();

            // Get the "platform".  All kindle devices have Amazon listed as the manufacturer.  All others are "normal" android devices.
            String manufacturer = Build.MANUFACTURER;
            if (manufacturer.equals("Amazon"))
            {
                sDeviceAndAppInfo.put("splyt.platform", "kindle");
            }
            else
            {
                sDeviceAndAppInfo.put("splyt.platform", "android");
            }

            // Get the rest of the information about the device
            sDeviceAndAppInfo.put("splyt.deviceinfo.manufacturer", manufacturer);
            sDeviceAndAppInfo.put("splyt.deviceinfo.model", Build.MODEL);
            sDeviceAndAppInfo.put("splyt.deviceinfo.product", Build.PRODUCT);
            sDeviceAndAppInfo.put("splyt.deviceinfo.brand", Build.BRAND);
            sDeviceAndAppInfo.put("splyt.deviceinfo.device", Build.DEVICE);
            sDeviceAndAppInfo.put("splyt.deviceinfo.cpu_abi", Build.CPU_ABI);
            sDeviceAndAppInfo.put("splyt.deviceinfo.cpu_abi2", Build.CPU_ABI2);
            sDeviceAndAppInfo.put("splyt.deviceinfo.osversion", "Android " + Build.VERSION.RELEASE);

            // Get some interesting information about the app
            String packageName = context.getPackageName();
            PackageManager pm = context.getPackageManager();
            try
            {
                PackageInfo info = pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS);
                sDeviceAndAppInfo.put("splyt.appinfo.versionCode", info.versionCode);
                sDeviceAndAppInfo.put("splyt.appinfo.versionName", info.versionName);
                sDeviceAndAppInfo.put("splyt.appinfo.firstInstallTime", info.firstInstallTime);
                sDeviceAndAppInfo.put("splyt.appinfo.lastUpdateTime", info.lastUpdateTime);
                sDeviceAndAppInfo.put("splyt.appinfo.requestedPermissions", Arrays.toString(info.requestedPermissions));
            }
            catch (NameNotFoundException e)
            {
                Util.logError("Unexpected NameNotFoundException during cacheDeviceAndAppInfo.");
            }
        }
        else
        {
            logError("Invalid context passed to cacheDeviceAndAppInfo");
        }
    }

    static void cacheCurrencyInfo()
    {
        // Clear out any previously set data
        sValidCurrencyCodes.clear();
        sCurrencyCodesBySymbol.clear();

        // Cache a set of valid ISO 4217 currency codes and a map of valid currency symbols to ISO 4217 currency codes
        Locale[] locs = Locale.getAvailableLocales();
        for (Locale loc : locs)
        {
            try
            {
                Currency cur = Currency.getInstance(loc);
                sValidCurrencyCodes.add(cur.getCurrencyCode());

                String curSymbol = cur.getSymbol(loc);
                if (sCurrencyCodesBySymbol.containsKey(curSymbol))
                {
                    sCurrencyCodesBySymbol.get(curSymbol).add(cur.getCurrencyCode());
                }
                else
                {
                    Set<String> codes = new HashSet<String>();
                    codes.add(cur.getCurrencyCode());
                    sCurrencyCodesBySymbol.put(curSymbol, codes);
                }
            }
            catch (IllegalArgumentException ex)
            {
                // Locale's country is not a supported ISO 3166 country.
            }
        }
    }

    // Given an input currency string, return a string that is valid currency string.
    // This can be either a valid ISO 4217 currency code or a currency symbol (e.g., for real currencies),  or simply any other ASCII string (e.g., for virtual currencies)
    // If one cannot be determined, this method returns "unknown"
    static String getValidCurrencyString(String currency)
    {
        String validCurrencyStr;

        // First check if the string is already a valid ISO 4217 currency code (i.e., it's in the list of known codes)
        if (sValidCurrencyCodes.contains(currency.toUpperCase()))
        {
            // It is, just return it
            validCurrencyStr = currency.toUpperCase();
        }
        else
        {
            // Not a valid currency code, is it a currency symbol?
            Set<String> possibleCodes = sCurrencyCodesBySymbol.get(currency.toUpperCase());
            if (null != possibleCodes)
            {
                // It's a valid symbol

                // If there is only one associated currency code, use it
                if (1 == possibleCodes.size())
                {
                    validCurrencyStr = possibleCodes.iterator().next();
                }
                else
                {
                    // Ok, more than one code associated with this symbol
                    // We make a best guess as to the actual currency code based on the user's locale.
                    try
                    {
                          Currency localeCurrency = Currency.getInstance(Locale.getDefault());
                          if (possibleCodes.contains(localeCurrency.getCurrencyCode()))
                          {
                              // The locale currency is in the list of possible codes
                              // It's pretty likely that this currency symbol refers to the locale currency, so let's assume that
                              // This is not a perfect solution, but it's the best we can do until Google and Amazon start giving us more than just currency symbols
                              validCurrencyStr = localeCurrency.getCurrencyCode();
                          }
                          else
                          {
                              // We have no idea which currency this symbol refers to, so just set it to "unknown"
                              validCurrencyStr = "unknown";
                          }
                    }
                    catch (IllegalArgumentException ex)
                    {
                        // Problem retrieving the locale currency, just set it to "unknown"
                        validCurrencyStr = "unknown";
                    }
                }
            }
            else
            {
                // This is not a known currency symbol, so it must be a virtual currency
                // Strip out any non-ASCII characters
                validCurrencyStr = currency.replaceAll("[^\\p{ASCII}]", "");
            }
        }

        return validCurrencyStr;
    }

    // Idea boosted from http://stackoverflow.com/questions/1712205/current-time-in-microseconds-in-java
    /**
     * <p>package-private class that generates timestamps with microsecond precision</p>
     */
    static enum MicroTimestamp
    {
        INSTANCE;

        private final double startTime;
        private final long startNanoseconds;

        private MicroTimestamp()
        {
            this.startTime = System.currentTimeMillis() / 1000.0;
            this.startNanoseconds = System.nanoTime();
        }

        /**
         * Get a timestamp in seconds with microsecond precision
         */
        public double get()
        {
            long microSecondsSinceStart = (System.nanoTime() - this.startNanoseconds) / 1000;
            return this.startTime + (microSecondsSinceStart / 1000000.0);
        }
    }
}
