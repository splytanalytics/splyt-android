package com.rsb.bubblepop;

import java.util.HashMap;
import java.util.Map;

import android.app.Service;
import android.content.Intent;
import android.os.CountDownTimer;
import android.os.IBinder;

import com.rsb.splyt.Splyt;

public class HeartbeatService extends Service
{
    private final long TIMER_MILLIS_TO_RUN = 90000;     // 90 seconds
    private final long TIMER_MILLIS_INTERVAL = 3000;    // 3 seconds

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        // Create a countdown timer to pump some custom events from this service and do it in another thread
        new CountDownTimer(TIMER_MILLIS_TO_RUN, TIMER_MILLIS_INTERVAL)
        {
            @Override
            public void onFinish()
            {
                // The timer is done, have this service stop itself
                stopSelf();
            }

            @Override
            public void onTick(long millisUntilFinished)
            {
                Map<String,Object> properties = new HashMap<String, Object>();
                properties.put("MillisUntilFinished", Long.toString(millisUntilFinished));
                Splyt.Instrumentation.Transaction("HeartBeatEvent").setProperties(properties).beginAndEnd();
            }
        }.start();

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent arg0)
    {
        // This service is not intended to be a bound service
        return null;
    }
}
