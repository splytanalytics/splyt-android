package com.rsb.splyt;

import android.app.IntentService;
import android.content.Intent;

import com.google.android.gms.gcm.GoogleCloudMessaging;

public class GcmIntentService extends IntentService
{
    public GcmIntentService()
    {
        super("GcmIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent)
    {
        // See http://docs.aws.amazon.com/sns/latest/dg/mobile-push-send-custommessage.html
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(gcm.getMessageType(intent)))
        {
            // Post notification of received message.
            NotificationSubsystem.postNotification(this, intent.getExtras());
        }

        // Release the wake lock provided by the WakefulBroadcastReceiver.
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }
}