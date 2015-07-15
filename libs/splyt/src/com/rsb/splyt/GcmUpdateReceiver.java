package com.rsb.splyt;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class GcmUpdateReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent)
    {
        // Either the app has been updated or the user has performed a system update
        // So, let's forcibly reregister the device
        // This way we can continue to send push notifications even if the user has not opened the app since the update occurred
        // See https://snowdog.co/blog/dealing-with-service_not_available-google-cloud-messaging/
        // and https://blog.pushbullet.com/2014/02/12/keeping-google-cloud-messaging-for-android-working-reliably-techincal-post/
        NotificationSubsystem.reregisterDevice(context);
    }
}
