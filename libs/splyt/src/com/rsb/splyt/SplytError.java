package com.rsb.splyt;

import android.util.SparseArray;

/**
 * Error codes that may be returned from the SPLYT framework APIs.
 */
public enum SplytError
{
    /**
     * Success (no error).
     */
    Success(0),

    /**
     * An unspecified error occurred.
     */
    ErrorGeneric(-1),

    /**
     * SPLYT has not been initialized.
     */
    ErrorNotInitialized(-2),

    /**
     * Invalid arguments were passed to a method.
     */
    ErrorInvalidArgs(-3),

    /**
     * The device or user ID is missing or invalid.
     */
    ErrorMissingId(-4),

    /**
     * A web request timed out.
     */
    ErrorRequestTimedout(-5),
    
    /**
     * Not a received error, but this can be used when an error fails to deserialize.
     */
    ErrorUnknown(-6);

    private final int error;
    SplytError(int error) { this.error = error; }
    /**
     * Gets the integer (ordinal) value associated with the error
     * 
     * @return The integer value
     */
    public int getValue() { return error; }
    
    public static SplytError fromInt(int err) { 
        return rlookup.get(err, ErrorUnknown);
    }
    
    // cache to a static to mitigate performance issues
    // Reverse-lookup map for getting a day from an abbreviation
    private static final SparseArray<SplytError> rlookup = new SparseArray<SplytError>();
    static {
        for (SplytError error : SplytError.values())
            rlookup.put(error.getValue(), error);
    }
}
