package com.rsb.splyt;

import java.util.Map;

/*
 * The TuningUpdater class provides a simple interface that's used for processing tuning value updates
 * @exclude
 */
interface TuningUpdater
{
    public void onUpdate(String type, String id, Map<String, Object> values);
    public void onClear(String type, String id);
    public void commit();
}