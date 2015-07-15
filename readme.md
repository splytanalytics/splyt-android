# Splyt SDK for Android - Integration Guide

This SDK is intended for use in an Android-based applications and is built using the [Android SDK](http://developer.android.com/sdk/).  

The sample application in this SDK was tested using the Android Developer Tools (ADT) Bundle, which is a version of Eclipse preconfigured with Android Developer Tools.  The version used in testing was ADT 22.3 together with an emulator running Android 4.3.

***

## Sample Application and Setup

The SDK includes a sample application called BubblePop.  BubblePop is a very simple [Shell Game](http://en.wikipedia.org/wiki/Shell_game) that demonstrates basic Splyt integration.  It also provides a simple examples of incorporating Splyt's plugins.

To view and run this sample:

1. Start Eclipse with Android Developer Tools
2. Open a workspace
3. Import the sample project
    * Click **File | Import...**
    * Choose **General | Existing Projects into Workspace**
    * Click **Next >**
    * Make sure **Select root directory** is checked, then click **Browse...**
    * Browse to the folder where you extracted the Splyt SDK for Android, and then select the `samples` subfolder.  Click **Open**
    * In the **Projects** list, **BubblePop** should now be listed.  Make sure it is checked, and then click **Finish**
4. In the **Package Explorer** window, right-click on the *BubblePop* node at the top of the tree view.  Then select **Debug As | Android Application** to start a debug session of BubblePop running under the Android emulator.

## Integration

Integrating Splyt involves adding code to your application that:

1. Prepares Splyt to start receiving data.  In these *initialization* steps, you will inform Splyt of the device and/or user running the app, and begin a session.
2. Add *instrumentation* code that sends data to Splyt describing how the user is engaging with your app.
3. Optionally, use Splyt's *tuning and testing* system to make dynamic virtual economy decisions and add A/B tests to your app.

Each of these integration steps are covered in more detail, below.

### Initialization

To prepare Splyt for use:

1. First, **Initialize Splyt** as early as you can by setting the customer ID, as well as the device and/or user IDs:

        Splyt.Core.InitParams initParams = Splyt.Core.createInitParams(this, SPLYT_CUSTOMER_ID, mSplytInitListener).setLogEnabled(true);

    ...where:

    * `SPLYT_CUSTOMER_ID` is a Java string constant.  The value of this string is a customer ID provided to you by the Splyt team.
    * `mSplytInitListener` is a `SplytListener` interface reference.  This interface exposes a single method, `onComplete`, which is called after Splyt initialization is complete.

2. After initialization is complete, **begin a session**.  As early as possible in your application's boot flow, use the Session plugin and call:

        SplytPlugins.Session.begin();

    ...to mark the beginning of a period of activity.  The BubblePop sample shows how this method can be called from a `SplytListener.onComplete()` callback, which is invoked after the Splyt core is initialized.

In order for sent data to be valid, you will need to have at least one of the user OR device IDs set.  If you do not specify a device ID, one will be requested for you from the Splyt servers and stored on the device (in local storage) for reuse in subsequent sessions.  Please see the API documentation and the BubblePop sample for more information.

Note that at any point after initialization, the device and/or user IDs can be set using `Splyt.Core.setDeviceId()` and `Splyt.Core.setUserId()`, respectively.  This is useful, for example, if you have a login flow and do not have a user ID at initialization but at some point after.  In this case, data before login will be linked only to a device ID and after the user ID is known, to both a device ID and a user ID.

3. In each Activity in which you intend to instrument with Splyt.  Be sure and override the onPause() and onResume() methods and add calls to Splyt.Core.pause() and Splyt.Core.resume(), respectively.  This will reduce the risk of data loss.

### Instrumentation

Instrument your application using the additional plugins provided in the SDK, where possible.  Refer to the the sample application for examples.

Here are few best practices to keep in mind:

* You must specify the user ID and/or device ID that the telemetry data is associated with.  You can do this by:

    1. Call `Splyt.Core.createInitParams` (as demonstrated in `MainActivity.java` in the Bubblepop sample)
    2. Call `Splyt.Core.setUserId()` and/or `Splyt.Core.setDeviceId()` on the `Splyt.Core.InitParams` instance that was returned by the previous step.
    3. Pass the `Splyt.Core.InitParams` instance to `Splyt.Core.init`

    ...if you do not specify a user ID or a device ID, the effective user ID will be `null`, and a device ID will be auto-generated.

*   If you do have both a user ID and a device ID, and you find yourself calling `Splyt.Instrumentation.updateUserState()`, you should also call `Splyt.Instrumentation.updateDeviceState()` with the same arguments.

*   You should update your user/device's state at the earliest possible moment that you can.  Further, to the extent that you can, you should always update user/device state before opening a new transaction.

    Taking purchasing as an example, the first block of code is preferable to the second.  The reason being that we want the purchase to be associated with the known user state at the time of the purchase.

        // First  : Preferred
        Splyt.Instrumentation.updateUserState(userProperties);
        SplytPlugins.Purchase.recordPurchase(purchaseProperties, Splyt.TXN_SUCCESS);

        // Second : Avoid
        SplytPlugins.Purchase.recordPurchase(purchaseProperties, Splyt.TXN_SUCCESS);
        Splyt.Instrumentation.updateUserState(userProperties);

*   You should apply all transaction properties to a transaction at the earliest moment that you can.
Do not defer updates to the property collection passed to `Splyt.Instrumentation.Transaction.setProperties()` to a later state of progress than is necessary.

### Tuning & Testing

If you are utilizing the tuning system, you will want to cache the tuning variables for a given user and device.  It is recommended you do this as early as possible in the flow and block on the call so you can be sure that everything is ready to go.  The following example shows one way to do this.

        _initGame();

        // Retrieve the tuning values.
        //
        // Since you do not want the game to proceed without having the actual tuning values,
        // you can provide a callback for when the call finishes.
        Splyt.Tuning.cacheVariables(new SplytListener()
        {
            @Override
            // This function is called when Splyt finishes its initialization
            public void onComplete(SplytErrors error)
            {
                // Even if "myVar" cannot be retrieved, 1.0 is used as a default value.
                mTuningVar = Splyt.Tuning.getVar("myVar", SplytConstants.ENTITY_TYPE_DEVICE, Float.valueOf(1.0f)).toString();

                // Now that we have a value for mTuningVar, we can start the game
                _startGame();
            }
        });

## Adding Splyt to Your Project

* Add the `sample/BubblePop/libs` folder to the project.
* In the project's properties (accessible from the **Project | Properties** menu item):

    1. Click on **Java Build Path** on the left-hand side of the Properties window.
    2. In the right-hand side, click on the **Libraries** tab and click **Add External JARS...**.
    3. Browse to the `libs` folder you added to the project, and add:

        * `splyt-android.jar`

    4. Click on the **Order and Export** tab, and make sure each of the libraries you added in the previous step is checked.
    5. Click **OK** to close the project Properties window.

### Javadoc Location

Javadocs for each Splyt plugin may be found under `samples/BubblePop/libs/docs/**/html`.  When Splyt jars are added to your Eclipse project using the steps above, tooltip documentation and autocomplete for Splyt is automatically enabled. 