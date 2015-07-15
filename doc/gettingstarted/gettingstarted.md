Getting Started
=========

Last Updated: March 4, 2104

## Adding the Library to Your Project

The BubblePop sample project included with this SDK was preconfigured to use SPLYT's Android library.

When adding SPLYT to your own app, you will need to add this library to your project. The easiest way to do this is to create a directory named `libs` in the root directory of your project, if it does not already exist.  Then copy the contents of this SDK's `.\libs` subfolder of this SDK to the `libs` directory of your project.

The contents of the SDK's `libs` subfolder include:

* `splyt-android.jar` - the SDK's main `.jar` file
* `splyt-android.jar.properties` - points to the SDK's Javadoc-based documentation for code completion support when working in Eclipse.
* `docs` - a subfolder containing the SDK's Javadoc-based documentation.

## Adding Telemetry to Your App

Once SPLYT's Android library has been added to your project, you'll want to add code to your app to send information to SPLYT.

The following sections provide a quick overview of the APIs available from the SPLYT library.  For a complete reference, refer to the library's [full documentation](annotated.html).

### Singleton Instances

Static classes provide access to the functionality that you'll use the most often:

|          Class          |                                                    Description                                                     |
| ----------------------- | ------------------------------------------------------------------------------------------------------------------ |
| `Splyt.Core`            | Provides access to the most central pieces of the SPLYT framework.                                                 |
| `Splyt.Instrumentation` | Provides access to factory methods for custom transactions, as well as methods for updating device and user state. |
| `Splyt.Tuning`          | Provides access to SPLYT's dynamic tuning variables, used in A/B testing and targeting.                            |
| `SplytPlugins.Purchase` | Provides factory methods for creating transactions that describe in-app purchases.                                 |
| `SplytPlugins.Session`  | Provides factory methods for creating transactions that describe app sessions.                                     |

See the BubblePop sample in the `samples` subfolder of the SDK for examples of how these classes get used in an app.  To get started with that sample, refer to [our walkthrough](md_sample-walkthrough_sample-walkthrough.html).

### Initialization

SPLYT should be initialized as early as possible in your app's startup sequence. SPLYT must be initialized before you start sending telemetry or referencing tuning variables defined in SPLYT.

For Android apps, the logical spot to initialize SPLYT is often in the `onCreate` method of your app's default/starting activity.

Note that the initialization calls a listener upon completion, after which point you can reliably use any of the other calls in the SPLYT SDK. Here's an example, passing `Splyt.Core.InitParams` to `Splyt.Core.init`:

    // contact SPLYT if you do not have a customer ID
    private static final String SPLYT_CUSTOMER_ID = "mycompany-myproduct-test";

    Splyt.Core.InitParams initParams = Splyt.Core.createInitParams(this, SPLYT_CUSTOMER_ID);

    Splyt.Core.init(initParams, new SplytListener() {
        @Override
        public void onComplete(SplytError error) {
            // This function is called when SPLYT finishes its initialization
            if(error != SplytError.Success)
                logError("SPLYT initialization failed!");
            
            // Wait until Splyt has finished initializing so that instrumentation calls in the app
            // work properly. In most cases you will want to allow the application to run even
            // if init fails; however, in those situations the tuning and instrumentation will not work.
	        SplytPlugins.Session.Transaction().begin();
            _startApp();
        }
    });

### Devices

SPLYT will automatically track some hardware information about your device, but if you have additional (perhaps application-specific) properties to report, you can do so at initialization time.  To do this, use `Splyt.Core.createDeviceInfo` to create an info object that describes the device, and then set that object as part of your `InitParams`:

    Splyt.Core.EntityInfo deviceInfo = Splyt.Core.createDeviceInfo()
    	.setProperty("screen_orientation",   "landscape")
    	.setProperty("internal_lib_version", lib.version);

    Splyt.Core.InitParams initParams = Splyt.Core.createInitParams(this, SPLYT_CUSTOMER_ID).setDeviceInfo(deviceInfo);

	Splyt.Core.init(initParams, listener);

To report any changes to the state of the device at any later point, see `Splyt.Instrumentation.updateDeviceState`.

### Users

Many applications track individual users with some form of user ID. For such applications, if you know the user ID at startup, it is recommended that you use `Splyt.Core.createUserInfo` to create an info object that describes the user, and then set that object as part of your `InitParams`.

You can also use the `setProperty` and `setProperties` methods of the info object to include additional properties that describe the user:

    Splyt.Core.EntityInfo userInfo = Splyt.Core.createUserInfo(user.id)
    	.setProperty("lifetime_spend",   user.lifetimeSpend)
    	.setProperty("friend_count",     user.facebookProfile.friendCount);

    Splyt.Core.InitParams initParams = Splyt.Core.createInitParams(this, SPLYT_CUSTOMER_ID).setUserInfo(userInfo);

	Splyt.Core.init(initParams, listener);

If the user is *not* known at startup, they can be registered at a later point by creating a SplytEntityInfo instance in the same fashion as above and then passing it to SplytCore::registerUser:andThen:

    Splyt.Core.registerUser(userInfo,  new SplytListener() {
        @Override
        public void onComplete(SplytError error) {
            // The app may now safely log telemetry and use tuned variables for the user
        }
    });

Additional notes:

* For applications which allow multiple concurrent users, see `Splyt.Core.setActiveUser`
* For applications which need to support users "logging out", see `Splyt.Core.clearActiveUser`
* To report any changes to the state of the user at any later point, see `Splyt.Instrumentation.updateUserState`:

### Telemetry

#### Transactions

Transactions are the primary unit of telemetry in SPLYT. Reporting events with a `Transaction` object is simple, but powerful. Consider:

    Splyt.Instrumentation.Transaction("UserAction").begin();

    // Time passes...

    Splyt.Instrumentation.Transaction("play_game").setProperty("something interesting", "about the transaction");
    Splyt.Instrumentation.Transaction("play_game").end();

Note that properties of the transaction may be set at the beginning or end of the transaction, or at any point in between as part of an `update`, but as a best practice, transaction properties should be reported as early as their value is known or known to have changed.

To handle the somewhat common case where a transaction occurs instantaneously, use the `Splyt.Instrumentation.Transaction.beginAndEnd` method.

Also note that the setting of transaction properties is only persisted after a call to one of the `begin`, `update`, `end`, or `beginAndEnd` methods of `Transaction`.

#### Collections

Collections in SPLYT are an abstraction for anything the user of the application might accumulate or have a varying quantity of. Common examples of this might be virtual currency, number of contacts, or achievements. `Splyt.Instrumentation.updateCollection` can be used at any point where the quantity
of a collection is known to have changed:

	// The user's friend count has decreased by 2 to 27.  friend count is NOT a currency.
    Splyt.Instrumentation.updateCollection(friendCount, 27, -2, NO);

It is recommended to instrument all of the important collections in the application, as they will add surprising power to your data analysis through contextualization.

#### Entity State

As previously mentioned, users and devices (commonly referred to as "entities" in SPLYT) may have their state recorded during initialization, or at a later point in the app by calling `Splyt.Instrumentation.updateUserState` or `Splyt.Instrumentation.updateDeviceState`, respectively. Reporting
changes in entity state is another great way to unlock the power of contextualization.

### Tuning

SPLYT's tuning system provides a means for dynamically altering the behavior of the application, conducting an A/Z test, and creating customized behavior for segments of your user base (targeting).  The instrumentation is very simple, and the hardest part might be deciding what you want to be able to tune. When initialized, SPLYT will retrieve any dynamic tuning for the device or user. At any point thereafter, the application may request a value using `Splyt.Tuning.getVar`:

    // before SPLYT
    String welcomeString = "Hi there!";
    float welcomeDuration = 3.0f;

    // with SPLYT tuning variables
    String welcomeString = Splyt.Tuning.getVar("welcomeString", "Hi there!");
    float welcomeDuration = Splyt.Tuning.getVar("welcomeTime", 3.0f);

Note that we specify a default value as part of the call to `getVar`. It is important to provide a "safe" default value to provide reliable behavior in the event that a dynamic value is not available. This also allows for the application to be safely instrumented in advance of any dynamic tuning, targeting, or A/Z test.

In addition to instrumenting key points in your code with `getVar`, applications which may remain running for long periods of time are encouraged to use `Splyt.Tuning.refresh` in order to make sure that the application has access to the latest tuned values at any point in time. A typical integration point for `refresh` on a mobile app might be whenever the application is brought to the foreground, and the code for handling it is quite simple:

    Splyt.Tuning.refresh(new SplytListener() {
        @Override
        public void onComplete(SplytError error) {
            // At this point, tuning for the device and any and all registered users should be refreshed.
        }
    });

It is not necessary to block for the completion of this call, as is typically recommended for `Splyt.Core.init`
and `Splyt.Core.registerUser`, since the application should already have access to viable tuned variables prior to the call to
`refresh`.  However, the callback is provided, leaving it to the discretion of the integrator.

### Mobile Apps

For mobile apps, it is likely that the app may become active and inactive many times during it's lifetime. In addition, it is best practice to ensure that applications can function properly under poor network conditions, or even when the device has no network connection. In order to support these characteristics, the SPLYT SDK is designed to protect your telemetry in these situations. However, there's a small bit that needs to be done by the implementor.

When the application is becoming inactive, call `Splyt.Core.pause`.  For example, the BubblePop sample does the following from the `onPause` method of `MainActivity`:

	@Override
	protected void onPause()
	{
	    super.onPause();

        // This activity is being paused; pause Splyt so that it can save off any events queued up to reduce the risk of losing data
	    Splyt.Core.pause();
	}

And then when it becomes active again, call `Splyt.Core.resume`:

    @Override
    protected void onResume()
    {
        super.onResume();

        // This activity is resuming; tell Splyt to resume normal operations
        Splyt.Core.resume();
    }

If necessary, you can still report telemetry while SPLYT is in a paused state, but the telemetry calls may execute more slowly, due to data being read and written from the device's local storage.