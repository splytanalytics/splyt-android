package com.rsb.splyt;

import android.content.Intent;

import com.amazon.device.messaging.ADMMessageHandlerBase;
import com.amazon.device.messaging.ADMMessageReceiver;

public class ADMMessageHandler extends ADMMessageHandlerBase
{
    interface RegisteredListener
    {
        public void onComplete(String regId);
    }

    private static RegisteredListener sRegisteredListener = null;
    static void setRegisteredListener(RegisteredListener value) { sRegisteredListener = value; }

    public ADMMessageHandler()
    {
        super("ADMMessageHandler");
    }

    public static class Receiver extends ADMMessageReceiver
    {
        public Receiver()
        {
            super(ADMMessageHandler.class);
        }
        // Nothing else is required here; your broadcast receiver automatically
        // forwards intents to your service for processing.
    }

    @Override
    protected void onRegistered(final String regId)
    {
        if (null != sRegisteredListener)
        {
            sRegisteredListener.onComplete(regId);
        }
    }

    @Override
    protected void onUnregistered(final String registrationId)
    {
        // Just do nothing, we'll handle this on the server side the next time we try to send a notification to this device.
        // The server should disable the corresponding token/endpoint as which point, we'll unregister it
    }

    @Override
    protected void onRegistrationError(final String errorId)
    {
        Util.logError("[Notification] Error registering with ADM: " + errorId);
        if (null != sRegisteredListener)
        {
            sRegisteredListener.onComplete(null);
        }
    }

    @Override
    protected void onMessage(final Intent intent)
    {
        // Post notification of received message.
        NotificationSubsystem.postNotification(this, intent.getExtras());
    }
}
