package com.rsb.splyt;

import java.util.Map;

/**
 * <p>Instrumentation Subsystem</p>
 *
 * @author Copyright 2015 Knetik, Inc.
 * @version 1.0
 */
class InstrumentationSubsystem
{
    private static boolean sInitialized;

    static void init()
    {
        if (!sInitialized)
        {
            Util.cacheCurrencyInfo();

            sInitialized = true;
        }
    }

    static void beginTransaction(String category, String timeoutMode, Double timeout, String transactionId, Map<String,Object> properties)
    {
        new CoreSubsystem.DataPointBuilder("datacollector_beginTransaction")
        .setArg(category)
        .setArg(timeoutMode)
        .setArg(timeout)
        .setArg(transactionId)
        .setArg(properties)
        .send();
    }

    static void updateTransaction(String category, Integer progress, String transactionId, Map<String,Object> properties)
    {
        new CoreSubsystem.DataPointBuilder("datacollector_updateTransaction")
        .setArg(category)
        .setArg(progress)
        .setArg(transactionId)
        .setArg(properties)
        .send();
    }

    static void endTransaction(String category, String result, String transactionId, Map<String,Object> properties)
    {
        new CoreSubsystem.DataPointBuilder("datacollector_endTransaction")
        .setArg(category)
        .setArg(result)
        .setArg(transactionId)
        .setArg(properties)
        .send();
    }

    /**
     * Updates state information about the user.
     *
     * @param properties A key-value object representing the user state we want to update. This can be a nested object structure.
     */
    static void updateUserState(Map<String,Object> properties)
    {
        new CoreSubsystem.DataPointBuilder("datacollector_updateUserState").setArg(properties).send();
    }

    /**
     * Updates state information about a device.
     *
     * @param properties A key-value object representing the device state we want to update. This can be a nested object structure.
     */
    static void updateDeviceState(Map<String,Object> properties)
    {
        new CoreSubsystem.DataPointBuilder("datacollector_updateDeviceState").setArg(properties).send();
    }

    /**
     * Update a collection balance for a user.
     *
     * @param name                  The name of the collection.
     * @param balance               The new balance of the collection.
     * @param balanceModification   The change in balance being recorded.  To reduce the balance, specify a negative number.
     * @param isCurrency            Whether or not this collection represents a currency in the application.
     */
    static void updateCollection(String name, Double balance, Double balanceModification, Boolean isCurrency)
    {
        new CoreSubsystem.DataPointBuilder("datacollector_updateCollection")
        .setArg(name)
        .setArg(balance)
        .setArg(balanceModification)
        .setArg(isCurrency)
        .send();
    }
}