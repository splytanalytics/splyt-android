Using the BubblePop Sample
=========

Last Updated: March 4, 2014

<a name="introduction"></a>
## Introduction

The SDK includes a very simple Android game that exercises some of the features of SPLYT.  This serves as a good starting point for many new users.  By stepping through the sample code, you can see how the SPLYT APIs are called.  Furthermore, you can use our SDK Debugger tool to verify that telemetry sent by these APIs is received by SPLYT.

BubblePop is an extremely simple game that sends data to SPLYT.  In this game: 

* You have a balance of virtual currency, referred to as "gold". 
* To play a game, you need to spend some of your gold.
* In the game, you click on bubbles to pop them.  One of these bubbles is a "winning" bubble.  The goal is to find that bubble before it is the only one left.
* You can also add more gold to your account by making an in-app purchase (don't worry -- in this sample game, you don't actually have to spend money make an in-app purchase!).

Admittedly, this game is not technically impressive; nor is it even all that much fun!  It simply exists to illustrate the steps involved in implementing SPLYT in an app.

The following steps walk you through the process of running the BubblePop sample.  These steps are based on Eclipse Kepler (4.3) Service Release 1 with Android Development Tools 22.3, running on Mac OS X 10.9 Mavericks.  The steps will vary slightly if you are using a different operating system or a different version of Eclipse.

## Opening the Project

1. To get started, open Eclipse,
2. After Eclipse starts, import a project into the workspace by selecting **File | Import...**.
   ![Import a Project](01-file_import.png)
3. In the window that appears, expand the **General** node and then select **Existing Projects into Workspace**.  Click **Next >**.
   ![Import an Existing Project](02-import_existing.png)
4. Make sure the **Select root directory** option is chosen, and click the **Browse...** button next to it.
   ![Select the Project's Root Directory](03-select_root.png)
5. In the window that appears, navigate to the folder where you unzipped the SPLYT SDK for Android, select the `samples` folder, and click **Open**
   ![Import the Sample Project](04-select_sample_dir_as_root.png)
6. You should be returned to the previous window.  In the **Projects** box, you should see the **BubblePop** project was found by Eclipse and that it is checked.  Then click **Finish** to finish importing the project.
   ![Finish Importing the BubblePop Project](05-finish_import.png)

&nbsp; <!-- stupidity to satisfy Doxygen formatting -->

<a name="productid"></a>
## Optional: Setting Your SPLYT Customer ID

To see your events in SPLYT, the game needs to be initialized with your unique customer ID.

Note that you may skip this step and continue to run the sample. However, if you skip this step, the data for BubblePop will not be visible to you in SPLYT's SDK debugger and dashboards.

If you *do* choose to set your customer ID to send BubblePop data to SPLYT, you will only want to send it to your `test` environment.  And if you wish to remove the BubblePop data from SPLYT at a later date, you will need to send a request to the [SPLYT Support Team](mailto:support@splyt.com) to do this for you.  A request to clear the data in your SPLYT `test` environment will result in *all* data being removed, whether it came from the BubblePop sample or your own app.

1. To use your specific customer ID, we'll need to change the code in BubblePop. To open the BubblePop code, go to Eclipse's Package Explorer. If it is not already open, you can open it by clicking the **Window | Show View | Package Explorer** menu item.
    ![Open the Package Explorer](06-open_package_explorer.png)
2. In the Package Explorer, expand the **BubblePop** project, then expand the `src` folder, and then expand the `com.rsb.bubblepop` folder.  Double-click the file `MainActivity.java` to open it in the editor.
    ![Open MainActivity.java](07_open-mainactivity_java.png)
3. Find the string constant `SPLYT_CUSTOMER_ID` near the top of the file.  Change the value of that constant from `splyt-bubblepopunity-test` to your own customer ID.  Be sure to specify the customer ID for your test environment; it should end with the characters `-test` .
    ![Changing the SPLYT Customer ID for BubblePop](08-change_product_id_eclipse.png)

> <i>Note: If you do not have or do not know your SPLYT customer ID, contact [support@splyt.com](support@splyt.com) to get one.</i>

&nbsp; <!-- stupidity to satisfy Doxygen formatting -->

## Running BubblePop

1. After you've opened the project and made sure it is using the appropriate customer ID, you can debug and run it.
2. Make sure the **BubblePop** project is selected in the Package Explorer.  Then click the drop-down portion of the **Debug** button in the toolbar and then choose the **Debug As | Android Application** menu item.
   ![Debug as an Android Application](09-debug_as_android_app.png)
3. After BubblePop has been built successfully, it launches on the device or emulator that you have configured for debugging.
   ![Launching BubblePop](09-bubblepop_android_emu.png)

&nbsp; <!-- stupidity to satisfy Doxygen formatting -->

## Using the SDK Debugger Page to View your Data

As BubblePop runs on the device, it will send data to SPLYT about what is happening in the game. If you chose to [set up a valid customer ID for BubblePop](#productid), then you can use SPLYT's SDK Debugger to verify that this data is arriving at SPLYT.  To do this, follow these steps:

1. Open a web browser, navigate to [https://dashboard.splyt.com](https://dashboard.splyt.com), and log into SPLYT.
2. Choose the product [whose customer ID you specified](#productid) when you set up the sample:
    ![Choosing BubblePop in SPLYT](splyt_choose_product.png)
3. Choose **Tools** from the nav bar on the top right:
    ![Opening the Tools Section of SPLYT](splyt_choose_tools.png)
4.	Once in the Tools section, choose the **Debugger** tool from the nav bar on the left.
5.	As you continue to play the BubblePop game that you started on iOS during the steps above, the SDK Debugger page will periodically refresh and show data that the game is sending to SPLYT.  You can use this to verify that your app is able to talk to SPLYT, and that your instrumentation is correct.
    ![SPLYT's SDK Debugger Page](debugger_android.png)
    Some tips on how to use this page:
    * All of SPLYT's SDKs send data using an underlying HTTP API.  The event names that appear on this page are based on the HTTP API names, and will differ from the actual SDK method names that you call from your code.
    * The page shows the 25 most recently received events.
    * By clicking on a given event/row, you will see more details.  These details make it easier to match up the data that you see in the debugger with the spot in your code where you called SPLYT to send the data. Click the row again to dismiss these details.
    * If there are any errors in the data that gets sent, they will be highlighted in red.
    * This page shows events received from *all* clients running your app.  It's best to use this page when few clients are running, so that the event stream is more easily interpretable.
    * The controls on the left include a **Play/Pause** toggle and a **Clear** button:
    	* If you toggle the page to **Pause** mode, it will not refresh with new events until you toggle it back to **Play**.  At that point, all events received since the page was paused will be retrieved.
    	* **Clear** permanently removes all events currently shown on the page.
    * This page only works for test environments (i.e., for SPLYT customer IDs ending in `-test`).
6. Data that is received by SPLYT will ultimately be included into all the charts available from the **Visualization** section of SPLYT.  SPLYT processes your data periodically throughout the day.  In general, you should expect your dashboards to update with new data within a couple of hours of the time it was received.

