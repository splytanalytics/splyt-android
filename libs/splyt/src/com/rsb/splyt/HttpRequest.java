package com.rsb.splyt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;

import android.os.AsyncTask;

class HttpRequest
{
    public static class RequestResult
    {
        SplytError error;      // Result error code
        String     response;   // Server response (null if no response)
    }

    public interface RequestListener
    {
        public void onComplete(RequestResult result);
    }

    // The URL request
    private final URL mUrl;

    // Timeout, in milliseconds, for the request
    private final int mTimeout;

    // Data to send (optional)
    private final String mSendData;

    public HttpRequest(URL url, int requestTimeout, String sendData)
    {
        // Set all of the member variables
        mUrl = url;
        mTimeout = requestTimeout;
        mSendData = sendData;
    }

    public RequestResult executeSync()
    {
        return executeRequest();
    }

    // listener - Callback function to be called when the request is complete
    public void executeAsync(final RequestListener listener)
    {
        new AsyncTask<Void, Void, HttpRequest.RequestResult>()
        {
            @Override
            protected RequestResult doInBackground(Void... params)
            {
                return executeRequest();
            }

            @Override
            protected void onPostExecute(RequestResult result)
            {
                if (null != listener)
                {
                    listener.onComplete(result);
                }
            }
        }.execute();
    }

    private RequestResult executeRequest()
    {
        // Assume a generic error
        RequestResult result = new RequestResult();
        result.error = SplytError.ErrorGeneric;

        // Use HttpURLConnection
        // See http://stackoverflow.com/questions/3505930/make-an-http-request-with-android
        // and http://android-developers.blogspot.com/2011/09/androids-http-clients.html
        HttpURLConnection urlConnection = null;
        try
        {
            urlConnection = (HttpURLConnection)mUrl.openConnection();

            // Connecting to a server will fail with a SocketTimeoutException if the timeout elapses before a connection is established.
            urlConnection.setConnectTimeout(mTimeout);

            urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");

            // If the connection is set to keep-alive and the server keep-alive timeout is encountered,
            // we could end up seeing an IOException when we try to send/read the response.
            urlConnection.setRequestProperty("Connection", "close");

            if (null != mSendData)
            {
                urlConnection.addRequestProperty("ssf-use-positional-post-params", "true");
                urlConnection.addRequestProperty("ssf-contents-not-url-encoded", "true");

                byte[] dataBytes = mSendData.getBytes("UTF-8");

                // We have data to send, so specify that this connection allows it (i.e., a "POST");
                urlConnection.setDoOutput(true);
                urlConnection.setFixedLengthStreamingMode(dataBytes.length);

                // No need for buffering as we do only "bulk" writes
                OutputStream out = urlConnection.getOutputStream();
                out.write(dataBytes);
                out.close();
            }

            int httpStatus = urlConnection.getResponseCode();
            if (HttpURLConnection.HTTP_OK == httpStatus)
            {
                BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while (null != (line = reader.readLine()))
                {
                    response.append(line);
                }

                // Set the result
                result.error = SplytError.Success;
                result.response = response.toString();
            }
            else
            {
                Util.logError("http response [" + httpStatus + "]: " +  urlConnection.getResponseMessage());
            }
        }
        catch (SocketTimeoutException e)
        {
            result.error = SplytError.ErrorRequestTimedout;
            Util.logError("Request timed out.  Try increasing the timeout value you send to Splyt.init()");
        }
        catch (IOException e)
        {
            Util.logError("Request IO Exception.  Please verify that android.permission.INTERNET is set in your app's manifest file!");
        }
        finally
        {
            if (null != urlConnection)
            {
                urlConnection.disconnect();
            }
        }

        return result;
    }
}