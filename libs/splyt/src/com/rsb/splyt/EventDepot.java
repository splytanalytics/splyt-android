package com.rsb.splyt;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import android.content.Context;
import android.os.Handler;

import com.rsb.gson.Gson;
import com.rsb.gson.JsonObject;
import com.rsb.gson.JsonParser;
import com.rsb.gson.JsonPrimitive;

/**
 * <p>This is an internal class used in the core library of Splyt to manage bins (groups) of events and send them to the data collector in order.
 * Features include:
 * - Events are stored off into bins and sent at intervals so as to reduce network traffic (i.e., less overhead)
 * - Bins of events are backed by persistent storage (i.e., if the user pauses an application component, the events not yet sent are saved off to storage)
 * - It is capable of handling the case where network connection is lost by archiving off the bins to persistent storage
 * - The total number of events held in both memory and in storage are limited so as to cap resource usage
 * - In cases where the network connection is either lost or "spotty", the frequency of the attempts to send the events is throttled, again to minimize resource usage
 * </p>
 *
 * @author Copyright 2013 Row Sham Bow, Inc.
 */
class EventDepot
{
    private static class State
    {
        // The depot state has the following properties:
        // ResendBin:     A bin of events that we would like to re-send (previous sends failed to reach the data collector).
        //                Under normal operating conditions this bin is empty.
        // HoldingBin:    A bin of events that are being held and waiting to either be sent immediately to the data collector
        //                or archived to "disk" and sent at some later point in time
        // ArchiveStart:  An index representing the start of the "circular buffer" of bins of events that have been archived to "disk"
        // ArchiveEnd:    An index representing the end of the "circular buffer" of bins of events that have been archived to "disk"
        static Map<String, Object> sState = null;

        @SuppressWarnings("unchecked")
        private static <T> T getValue(String name)
        {
            if ((null != sState) && sState.containsKey(name))
            {
                return (T) sState.get(name);
            }
            else
            {
                return null;
            }
        }
        static List<Object> ResendBin() { return getValue("ResendBin"); }
        static List<Object> HoldingBin() { return getValue("HoldingBin"); }

        static Integer ArchiveStart() { return getValue("ArchiveStart"); }
        static void setArchiveStart(int newValue) { sState.put("ArchiveStart", Integer.valueOf(newValue)); }
        static Integer ArchiveEnd() { return getValue("ArchiveEnd"); }
        static void setArchiveEnd(int newValue) { sState.put("ArchiveEnd", Integer.valueOf(newValue)); }

        static URL ResendBinURL() { return getValue("ResendBinURL"); }
        static void setResendBinURL(URL newValue) { sState.put("ResendBinURL", newValue); }
        static URL HoldingBinURL() { return getValue("HoldingBinURL"); }
        static void setHoldingBinURL(URL newValue) { sState.put("HoldingBinURL", newValue); }

        private static void reset()
        {
            sState = new HashMap<String, Object>();
            sState.put("ResendBin", new ArrayList<Object>());
            sState.put("ResendBinURL", sUrl);
            sState.put("HoldingBin", new ArrayList<Object>());
            sState.put("HoldingBinURL", sUrl);
            sState.put("ArchiveStart", Integer.valueOf(0));
            sState.put("ArchiveEnd", Integer.valueOf(0));
        }

        @SuppressWarnings("unchecked")
        static void restore()
        {
            if (null != sContext)
            {
                try
                {
                    // Pull in the state data if there is any
                    FileInputStream fis = sContext.openFileInput(STATE_FILENAME);
                    ObjectInputStream inputStream = new ObjectInputStream(fis);
                    sState = (Map<String, Object>) inputStream.readObject();

                    // insure backward compatibility by setting valid values for URL
                    if(null == State.HoldingBinURL())
                        State.setHoldingBinURL(sUrl);
                    if(null == State.ResendBinURL())
                        State.setResendBinURL(sUrl);

                    fis.close();
                }
                catch (Exception ex)
                {
                    // Some error occurred reading the state data file.  It may be that the file simply doesn't exist.
                    // In any case, we can handle this situation, so carry on
                }

                // Delete the state file since it should now be in memory
                sContext.deleteFile(STATE_FILENAME);
            }

            if (null == sState)
            {
                // No state data available, so create some
                reset();
            }

            // If there is supposed to be no data archived to disk, make sure there is none (i.e., clean up).
            // We do this in case the state somehow got out of sync with what had been written to internal storage
            // If this happens, then all of the events in these archived files are lost, but it would be sent out of order now anyhow and just
            if ((null != sContext) && (ArchiveEnd().equals(ArchiveStart())))
            {
                File listing = sContext.getFilesDir();
                String[] staleArchives = listing.list(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String filename)
                    {
                        return filename.startsWith(BIN_ARCHIVE_FILE_PREFIX);
                    }
                });

                if (null != staleArchives)
                {
                    // Delete any archive files found
                    for (String archive : staleArchives)
                    {
                        sContext.deleteFile(archive);
                    }
                }
            }
        }

        static void save()
        {
            if ((null != sContext) && (null != sState))
            {
                // We have some state data, so save it off
                try
                {
                    FileOutputStream fos = sContext.openFileOutput(STATE_FILENAME, Context.MODE_PRIVATE);
                    ObjectOutputStream outputStream = new ObjectOutputStream(fos);
                    outputStream.writeObject(sState);
                    outputStream.flush();
                    outputStream.close();
                }
                catch (IOException ioex) { }
            }

            // State data is saved, so clear the in-memory data
            reset();
        }
    }

    // A constant representing the maximum number of events we allow in a single bin
    // This constraint is here to limit our memory footprint
    // Since we have 3 bins of events in memory at any given time, the maximum number of events resident in memory is 3x this number
    private static final int MAX_EVENTS_PER_BIN = 50;

    private static final String STATE_FILENAME = "splyt_depotState";

    private static final String BIN_ARCHIVE_FILE_PREFIX = "splyt_binArchive";
    private static final int BIN_ARCHIVES_SIZE = 201; // 200 archived bins -> a maximum of 10k events

    // Used to calculate the period at which we process the bins
    private static final int PROCESSBIN_MIN_PERIOD = 5000;    // In ms
    private static final int PROCESSBIN_MAX_PERIOD = 30000;   // In ms
    private static int sCurProcessBinPeriod = PROCESSBIN_MIN_PERIOD;

    private static Context sContext;
    private static URL sUrl;
    private static int sReqTimeout;
    private static boolean sPaused;
    private static boolean sInitialized;
    private static BlockingQueue<Runnable> sJobQueue;
    private static Handler sHandler;

    /**
     * Initialize the event depot.
     *
     * @param host          The host name of the data collector
     * @param queryParams   Query parameters to send along with the request
     * @param reqTimeout    A timeout, in milliseconds, representing the maxmimum amount of time one should wait for Splyt network requests to complete.
     */
    static void init(Context context, String host, String queryParams, int reqTimeout)
    {
        if (!sInitialized)
        {
            // Save off the parameters needed to submit requests to send the events to the data collector
            sContext = context;
            sReqTimeout = reqTimeout;

            try
            {
                sUrl = new URL(host + "/isos-personalization/ws/interface/datacollector_batch" + queryParams);
            }
            catch (MalformedURLException e)
            {
                Util.logError("MalformedURLException.  Check your host and customerId values");
            }

            // Create a new handler that we'll use to schedule the bin processing
            sHandler = new Handler();

            // Create the job queue and start up the job consumer in another thread
            sJobQueue = new LinkedBlockingQueue<Runnable>();
            new Thread(new JobConsumer(sJobQueue), EventDepot.class.getSimpleName()).start();

            // Queue up the DepotInitJob
            sJobQueue.offer(new DepotInitJob());

            sInitialized = true;
        }
    }

    /**
     * Store an event in the depot.
     *
     * @param event The event we wish to store
     *
     * NOTE: This method can be called from multiple threads (i.e., it's thread-safe)
     */
    static SplytError store(Map<String, Object> event)
    {
        SplytError ret = SplytError.Success;

        if (null != sJobQueue)
        {
            // Add the job to the queue
            sJobQueue.offer(new StoreEventJob(event));
        }
        else
        {
            ret = SplytError.ErrorNotInitialized;
        }

        return ret;
    }

    static void pause()
    {
        if (null != sJobQueue)
        {
            // Add the job to the queue
            sJobQueue.offer(new PauseDepotJob());
        }
    }

    static void resume()
    {
        if (null != sJobQueue)
        {
            // Add the job to the queue
            sJobQueue.offer(new ResumeDepotJob());
        }
    }

    //////////////////////////////
    // Private helper functions //
    //////////////////////////////

    private static void logErrorResponse(String response)
    {
        if (null != response)
        {
            try
            {
                JsonParser parser = new JsonParser();
                JsonObject obj = parser.parse(response).getAsJsonObject();
                JsonPrimitive error = obj.getAsJsonPrimitive("error");
                if (null != error)
                {
                    if (SplytError.Success.getValue() != error.getAsInt())
                    {
                        Util.logError("Top-level error [" + error.getAsString() + "] returned from data collector");
                    }
                    else
                    {
                        // Got a successful top-level response, now check the datacollector_batch context
                        JsonObject data = obj.getAsJsonObject("data");
                        if (null != data)
                        {
                            JsonObject context = data.getAsJsonObject("datacollector_batch");
                            if (null != context)
                            {
                                JsonPrimitive contextError = context.getAsJsonPrimitive("error");
                                if (SplytError.Success.getValue() != contextError.getAsInt())
                                {
                                    Util.logError("datacollector_batch error [" + error.getAsString() + "] returned from data collector");
                                }
                            }
                            else
                            {
                                Util.logError("Unexpected response returned from data collector, context missing");
                            }
                        }
                        else
                        {
                            Util.logError("Unexpected response returned from data collector, data missing");
                        }
                    }
                }
                else
                {
                    Util.logError("Unexpected response returned from data collector, error missing");
                }
            }
            catch (Exception e)
            {
                Util.logError("Exception parsing server response: " + response);
            }
        }
    }

    private static boolean sendBin(URL url, List<Object> data)
    {
        // Build up the data object
        List<Object> allArgs = new ArrayList<Object>(2);
        allArgs.add(Double.valueOf(Util.MicroTimestamp.INSTANCE.get()));
        allArgs.add(data);

        // Create a new request to send the data synchronously
        HttpRequest.RequestResult result = new HttpRequest(url, sReqTimeout, new Gson().toJson(allArgs)).executeSync();
        if (SplytError.Success == result.error)
        {
            // We got "some" response from the server
            // Note that we won't attempt to re-send this bin of events regardless of the response

            // If we were throttling the sends because of a connectivity issue, let's throttle back up now that we're getting responses
            if (PROCESSBIN_MIN_PERIOD != sCurProcessBinPeriod)
            {
                int decrementBy = Math.max((sCurProcessBinPeriod - PROCESSBIN_MIN_PERIOD) / 5, 500);
                sCurProcessBinPeriod = Math.max(sCurProcessBinPeriod - decrementBy, PROCESSBIN_MIN_PERIOD);
            }

            // Now, let's check for errors in the data returned from the server so that we can at least log them
            logErrorResponse(result.response);
        }
        else
        {
            // Some IO or timeout error occurred, so we might have some connectivity issue
            // Let's throttle the timer in order to attempt sending bins of events less often in case the user is not connected
            if (PROCESSBIN_MAX_PERIOD != sCurProcessBinPeriod)
            {
                sCurProcessBinPeriod = Math.min(sCurProcessBinPeriod + 500, PROCESSBIN_MAX_PERIOD);
            }
        }

        return (SplytError.Success == result.error);
    }

    private static void processBins(boolean flushHoldingBin)
    {
        // First, let's try and send a bin of events to the data collector
        List<Object> rb = State.ResendBin();
        List<Object> hb = State.HoldingBin();
        Util.logDebug("Resend Bin Count [" + rb.size() + "]");
        Util.logDebug("Holding Bin Count [" + hb.size() + "]");
        Util.logDebug("Archive Infos [" + State.ArchiveStart() + ", " + State.ArchiveEnd() + "]");
        if (rb.size() > 0)
        {
            // We have events in the re-send bin.  These are our first priority, so let's try and send them
            if (sendBin(State.ResendBinURL(), rb))
            {
                // Successful send, clear the bin
                rb.clear();
            }
        }
        else if (!State.ArchiveEnd().equals(State.ArchiveStart()))
        {
            // Nothing in the re-send bin, but we have some data archived to disk.  These are our second priority as we must send events in timestamp order
            String archiveFileName = BIN_ARCHIVE_FILE_PREFIX + State.ArchiveStart().toString();

            try
            {
                FileInputStream fis = sContext.openFileInput(archiveFileName);
                ObjectInputStream inputStream = new ObjectInputStream(fis);
                URL url = (URL) inputStream.readObject();
                @SuppressWarnings("unchecked")
                ArrayList<Object> diskData = (ArrayList<Object>) inputStream.readObject();
                fis.close();

                if (!sendBin(url, diskData))
                {
                    // Failed to send the bin of events.  Dump them into the re-send bin so we can try again next time
                    rb.addAll(diskData);
                    State.setResendBinURL(url);
                }
            }
            catch (IOException ioex)
            {
                // Some error occurred reading the archive data file.  This is unexpected, so we log it
                // But it's safe to carry on
                Util.logError("IOException loading file [" + archiveFileName + "].  Skipping...");
            }
            catch (ClassNotFoundException clnex)
            {
                // Contents of the archive file are corrupted.   This is unexpected, so we log it
                // But it's safe to carry on
                Util.logError("ClassNotFoundException loading file [" + archiveFileName + "].  Skipping...");
            }

            // Remove the archive file and update the start index
            sContext.deleteFile(archiveFileName);
            State.setArchiveStart((State.ArchiveStart() + 1) % BIN_ARCHIVES_SIZE);
        }
        else if (hb.size() > 0)
        {
            // Noting in the re-send bin and we have no data archived to disk, so let's attempt to send what's in the holding bin
            if (!sendBin(State.HoldingBinURL(), hb))
            {
                // Failed to send the bin of events.  Dump them into the re-send bin so we can try again next time
                rb.addAll(hb);
                State.setResendBinURL(State.HoldingBinURL());
            }

            // Clear the holding bin
            hb.clear();
        }

        // Handle bin overflow
        while ( (hb.size() >= MAX_EVENTS_PER_BIN) || (flushHoldingBin && hb.size() > 0) )
        {
            // Our holding bin is full, so rotate out a chunk of events to disk
            List<Object> dataToArchive = hb.subList(0, (hb.size() > MAX_EVENTS_PER_BIN) ? MAX_EVENTS_PER_BIN : hb.size());
            String archiveFileName = BIN_ARCHIVE_FILE_PREFIX + State.ArchiveEnd().toString();

            State.setArchiveEnd((State.ArchiveEnd() + 1) % BIN_ARCHIVES_SIZE);
            if (State.ArchiveEnd().equals(State.ArchiveStart()))
            {
                // We've reached the max archives we wish to store, so purge the oldest one
                sContext.deleteFile(BIN_ARCHIVE_FILE_PREFIX + State.ArchiveStart().toString());

                State.setArchiveStart((State.ArchiveStart() + 1) % BIN_ARCHIVES_SIZE);
            }

            try
            {
                FileOutputStream fos = sContext.openFileOutput(archiveFileName, Context.MODE_PRIVATE);
                ObjectOutputStream outputStream = new ObjectOutputStream(fos);
                outputStream.writeObject(State.HoldingBinURL());
                outputStream.writeObject(new ArrayList<Object>(dataToArchive));
                fos.flush();
                fos.close();

                // Now that we've archived the data, clear it from the holding bin
                dataToArchive.clear();
            }
            catch (IOException ioex)
            {
                Util.logError("EventDepot: Failed to write Archive data");
            }
        }
    }

    // Submits the job to processes the bins and then resubmits itself
    private static Runnable sBinProcessor = new Runnable()
    {
        @Override
        public void run()
        {
            // Add the job to the queue
            sJobQueue.offer(new ProcessBinsJob());

            // Now register this runnable to run again at the current period
            sHandler.postDelayed(this, sCurProcessBinPeriod);
        }
    };

    /////////////////////////
    // Job Implementations //
    /////////////////////////
    private static class DepotInitJob implements Runnable
    {
        @Override
        public void run()
        {
            // Restore any state data that may have been saved off
            State.restore();

            // initially, if the URL has changed, we need to flush the old stuff out of the holding bin
            boolean flushHoldingBin = sUrl != State.HoldingBinURL();
            processBins(flushHoldingBin);
            State.setHoldingBinURL(sUrl);

            // Start the periodic bin processing
            sHandler.postDelayed(sBinProcessor, sCurProcessBinPeriod);
        }
    }

    private static class StoreEventJob implements Runnable
    {
        Map<String, Object> mEvent; // The event to store

        StoreEventJob(Map<String, Object> event)
        {
            mEvent = event;
        }

        @Override
        public void run()
        {
            // We have an event to store
            if (sPaused)
            {
                // The system has been paused, so process this event on demand

                // Restore any state data that may have been saved off
                State.restore();

                // Add the event to the holding bin
                List<Object> hb = State.HoldingBin();
                hb.add(mEvent);

                // Process bins immediately
                processBins(false);

                // Save off the state
                State.save();
            }
            else
            {
                List<Object> hb = State.HoldingBin();
                hb.add(mEvent);
                if (hb.size() >= MAX_EVENTS_PER_BIN)
                {
                    // We've reached the maximum desired batch size, so process the bins immediately
                    processBins(false);
                }
            }
        }
    }

    private static class ProcessBinsJob implements Runnable
    {
        @Override
        public void run()
        {
            // Process the bins
            processBins(false);
        }
    }

    private static class PauseDepotJob implements Runnable
    {

        @Override
        public void run()
        {
            if (!sPaused)
            {
                sPaused = true;

                // Stop the periodic bin processing
                sHandler.removeCallbacks(sBinProcessor);

                // Save off the state
                State.save();
            }
        }
    }

    private static class ResumeDepotJob implements Runnable
    {
        @Override
        public void run()
        {
            if (sPaused)
            {
                // Restore the state
                State.restore();

                // Reset the period and start the bin processing
                sCurProcessBinPeriod = PROCESSBIN_MIN_PERIOD;
                sHandler.postDelayed(sBinProcessor, sCurProcessBinPeriod);

                sPaused = false;
            }
        }
    }

    // This class implements the main processing loop for the job queue.  It is intended to be run in a worker thread
    private static class JobConsumer implements Runnable
    {
        private final BlockingQueue<Runnable> mQueue;
        JobConsumer(BlockingQueue<Runnable> queue)
        {
            mQueue = queue;
        }

        @Override
        public void run()
        {
            try
            {
                while (true)
                {
                    Runnable nextJob = mQueue.take();
                    nextJob.run();

                    int numItems = 0;
                    for (Runnable r : mQueue)
                    {
                        Util.logDebug("Job Queue [" + Integer.toString(numItems++) + "]: " + r.getClass().getSimpleName());
                    }
                }
            }
            catch (InterruptedException ex)
            {
                String errorMsg = "Unexpected InterruptedException in the JobConsumer";
                if (null != ex.getMessage())
                {
                    errorMsg += ": " + ex.getMessage();
                }
                Util.logError(errorMsg);
            }
        }
    }

}
