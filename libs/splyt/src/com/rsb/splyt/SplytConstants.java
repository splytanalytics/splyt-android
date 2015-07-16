package com.rsb.splyt;

/**
 * Public SPLYT Constants
 *
 * @author Copyright 2015 Knetik, Inc.
 * @version 1.0
 */
public class SplytConstants
{
    /**
     * The default timeout for web requests sent from the SPLYT SDK, in milliseconds.
     */
    public static final int DEFAULT_REQUEST_TIMEOUT = 3000; // in ms

    /**
     * The string `success`, which represents successful completion of a
     * {@link Splyt.Instrumentation.Transaction}.  If no result is specified when ending a
     * {@link Splyt.Instrumentation.Transaction}, this is used as the default.
     */
    public static final String TXN_SUCCESS = "success";

    /**
     * The string `error`, which can be used to represent a {@link Splyt.Instrumentation.Transaction}
     * that did not complete successfully.
     */
    public static final String TXN_ERROR = "error";

    /**
     * A string that indicates than an entity represents a user. Clients should not normally need to make direct use
     * of this constant when using a SPLYT SDK.
     *
     * @see Splyt.Core.EntityInfo
     */
    public static final String ENTITY_TYPE_USER = "USER";

    /**
     * A string that indicates than an entity represents a device. Clients should not normally need to make direct use
     * of this constant when using a SPLYT SDK.
     *
     * @see Splyt.Core.EntityInfo
     */
    public static final String ENTITY_TYPE_DEVICE = "DEVICE";

    /**
     * Indicates that a {@link Splyt.Instrumentation.Transaction} will be kept "open" only by direct updates to the
     * transaction itself.  If no timeout mode is specified, this is the default.
     *
     * @see Splyt.Core.EntityInfo
     */
    public static final String TIMEOUT_MODE_TRANSACTION = "TXN";

    /**
     * The transaction will be kept "open" by updates to any transaction for the current device or user.
     * <b>Note</b>: this is reserved for future use, and is not yet supported.
     *
     * @see Splyt.Core.EntityInfo
     */
    public static final String TIMEOUT_MODE_ANY = "ANY";

    static final String PROPERTY_ISNEW = "_SPLYT_isNew";
    static final Double TIME_RECORDAGAIN = 8.0*60.0*60.0;  // only record each 8 hours
    static final String PREFS_FILENAME = "com.rsb.splyt";

    static final String PACKAGE_NAME = SplytConstants.class.getPackage().getName();
    static final String ACTION_CORESUBSYSTEM_INITIALIZATIONCOMPLETE = PACKAGE_NAME + ".CoreSubsystem.InitializationComplete";
    static final String ACTION_CORESUBSYSTEM_PAUSED = PACKAGE_NAME + ".CoreSubsystem.Paused";
    static final String ACTION_CORESUBSYSTEM_RESUMED = PACKAGE_NAME + ".CoreSubsystem.Resumed";
    static final String ACTION_CORESUBSYSTEM_SETUSERID = PACKAGE_NAME + ".CoreSubsystem.SetUserId";
}