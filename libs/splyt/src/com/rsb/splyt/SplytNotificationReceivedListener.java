package com.rsb.splyt;

import android.os.Bundle;

/**
 * A callback that is invoked when Splyt detects that a notification is received
 */
public interface SplytNotificationReceivedListener
{
    public void onReceived(Bundle info, boolean wasLaunchedBy);
}