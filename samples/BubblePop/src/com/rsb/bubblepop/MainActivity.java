package com.rsb.bubblepop;

import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.rsb.splyt.Splyt;
import com.rsb.splyt.SplytConstants;
import com.rsb.splyt.SplytError;
import com.rsb.splyt.SplytListener;
import com.rsb.splyt.SplytNotificationReceivedListener;
import com.rsb.splyt.SplytPlugins;

public class MainActivity extends Activity implements GameBoard.GameOverListener, LoginHelper, PurchaseHelper
{
    private static final String SPLYT_CUSTOMER_ID = "splyt-bubblepop-test";

    private final int INITIAL_DEPOSIT = 10;
    private final int GAME_COST = 5;

    private GameBoard mGameBoard;
    private String mUser = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Initialize Splyt with logging enabled.
        // Note that one should only enable logging when debugging an issue
        Splyt.Core.InitParams initParams = Splyt.Core.createInitParams(this, SPLYT_CUSTOMER_ID)
            .setLogEnabled(true);

        // Set up notification-specific initialization parameters
        initParams.Notification
            .setSmallIcon(R.drawable.ic_notification)
            // If the app uses Splyt's notification service, you may want to know when notifications are received and/or when the app is launched via a notification...
            .setReceivedListener(new SplytNotificationReceivedListener() {
                @Override
                public void onReceived(Bundle info, boolean wasLaunchedBy) {
                    String infoString = "[";
                    for (String key : info.keySet()) {
                        infoString += " " + key + " => " + info.get(key) + ";";
                    }
                    infoString += "]";
                    if (wasLaunchedBy) {
                        logDebug("Bubble Pop! was launch by notification: " + infoString);
                    }
                    else {
                        logDebug("Bubble Pop! received a notification: " + infoString);
                    }
                }
            });

        // To send data somewhere other than the default location(s), uncomment the following line(s) and set the URL(s) accordingly.
        // NOTE:  This is typically used for development purposes
        //initParams.setHost("http://10.0.2.2");
        //initParams.Notification.setHost("http://10.0.2.2");

        SharedPreferences sp = this.getPreferences(Context.MODE_PRIVATE);
        mUser = sp.getString("user", null);
        if(null != mUser)
        {
            initParams.setUserInfo(
                Splyt.Core.createUserInfo(mUser)
                    .setProperty("bubblepop_property", "interesting_value")
            );
        }

        Splyt.Core.init(initParams, new SplytListener() {
            @Override
            public void onComplete(SplytError error) {
                // This function is called when Splyt finishes its initialization
                if(SplytError.Success == error)
                    logDebug("Splyt initialization SUCCEEDED!");
                else
                    logError("Splyt initialization FAILED!");

                // Wait until Splyt has finished initializing, so that instrumentation calls in the game
                // work properly. In most cases you will want to allow the application to run even
                // if .init fails; however, in those situations the tuning and instrumentation will not work.
                _startGame();
            }
        });
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        // This activity is being paused; inform Splyt so that it can save off any events queued up to reduce the risk of losing data
        Splyt.Core.pause();
    }

    @Override
    protected void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);

        // If for some reason the activity is not finished and re-created when launched,
        // make sure the intent gets updated so that we can detect if the activity was launched due to a notification
        // See http://developer.android.com/reference/android/content/Intent.html#FLAG_ACTIVITY_CLEAR_TOP
        this.setIntent(intent);
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        // This activity is resuming; tell Splyt to resume normal operations
        Splyt.Core.resume();
    }

    private void _startGame()
    {
        // Start the session
        SplytPlugins.Session.Transaction().begin();

        // Set the content from the layout (xml) file
        setContentView(R.layout.activity_main);
        
        BankAccount.addBalanceChangedListener(new BankAccount.BalanceChangedListener() {
            @Override
            public void onBalanceChanged(int newBalance, int delta) {
                Splyt.Instrumentation.updateCollection("Coins", (double)newBalance, (double)delta, true);
            }
        });

        // Deposit INITIAL_DEPOSIT currency into the BankAccount
        Integer initDeposit = Splyt.Tuning.getVar("Initial Deposit", INITIAL_DEPOSIT);
        BankAccount.Deposit(initDeposit.intValue());

        // Create the GameBoard and add the main activity as a listener for when the game is declared as over
        mGameBoard = (GameBoard)findViewById(R.id.gameboardsection);
        mGameBoard.create();
        mGameBoard.addGameOverListener(this);

        // Set the label for the start button
        Button startButton = (Button)findViewById(R.id.startbutton);
        startButton.setText("START (" + GAME_COST + " " + PurchaseButton.VIRTUAL_CURRENCY_NAME + ")");

        // Start the service where we'll test sending events both from another thread and even if this activity goes away
        startService(new Intent(this, HeartbeatService.class));
    }

    public void onStartButtonClick(View v)
    {
        // Withdrawal the amount that a game costs
        BankAccount.Withdrawal(GAME_COST);

        // Hide the start button
        v.setVisibility(View.INVISIBLE);

        // Reset the GameBoard
        mGameBoard.reset();

        // Example:  retrieving a tuning variable
        float testVar1 = Splyt.Tuning.getVar("TestVar1", 1.0f);
        logDebug("TestVar1 = " + testVar1);

        Splyt.Instrumentation.Transaction("gameStarted").beginAndEnd();
    }

    // Gets a game score.
    //
    // Score is on a 0 to 1 scale, based on the number of pops.
    //
    // Score starts at 1 and decreases with each pop. If all bubbles are popped
    // before the player wins, final score is 0.
    private float _getScore()
    {
        int numBubbles = mGameBoard.getTotalNumCells();
        return (numBubbles - mGameBoard.getNumMoves()) / (float)(numBubbles - 1);
    }

    @Override
    public void onGameOver()
    {
        Map<String,Object> properties = new HashMap<String, Object>();
        properties.put("numberOfMisses", mGameBoard.getNumMoves() - 1);
        properties.put("didWin", true); // We always win this awesome game
        properties.put("winQuality", _getScore());
        Splyt.Instrumentation.Transaction("gameFinished").setProperties(properties).beginAndEnd();

        Splyt.Tuning.refresh(new SplytListener() {
            @Override
            public void onComplete(SplytError error) {
                // This function is called when Splyt finishes refreshing tuning variables
                if(SplytError.Success == error)
                    logDebug("Splyt refresh SUCCEEDED!");
                else
                    logError("Splyt refresh FAILED!");
                
                // once the refresh is complete, turn the start button back on
                Button startButton = (Button)findViewById(R.id.startbutton);
                startButton.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public boolean isLoggedIn()
    {
        return null != mUser;
    }

    @Override
    public void Login(String name, String gender)
    {
        final MainActivity that = this;

        updateUser(name);

        Splyt.Core.EntityInfo userInfo = Splyt.Core.createUserInfo(name).setProperty("gender", gender);
        Splyt.Core.registerUser(userInfo, new SplytListener(){
            @Override
            public void onComplete(SplytError error) { 
                float testVar1 = Splyt.Tuning.getVar("TestVar1", 1.0f);
                Log.d(getClass().getName(), "TestVar1 personalized for " + that.mUser + ": " + testVar1);
                
                that.sendBroadcast(new Intent("LOGIN_CHANGED"));
            }
        });
    }
    
    @Override
    public void Logout()
    {
        updateUser(null);
        
        Splyt.Core.clearActiveUser();
        this.sendBroadcast(new Intent("LOGIN_CHANGED"));
    }

    @Override
    public void BeginPurchase(String itemName, Double price)
    {
        // We begin a purchase
        SplytPlugins.Purchase.Transaction()
            .setProperty("promotion", "none")
            .setItemName(itemName)
            .setPrice("USD", price)
            .begin();
    }
    
    @Override
    public void CompletePurchase(int quantity)
    {
        int balance = BankAccount.Deposit(quantity);
        
        SplytPlugins.Purchase.Transaction()
            .setProperty("new_balance", balance)
            .end();
    }
    
    @Override
    public void CancelPurchase()
    {
        SplytPlugins.Purchase.Transaction()
            .setProperty("reason", "cancelled")
            .end(SplytConstants.TXN_ERROR);
    }
    
    private void updateUser(String userName)
    {
        mUser = userName;
        
        // application code to remember that the user is logged in
        SharedPreferences sp = this.getPreferences(Context.MODE_PRIVATE);
        Editor spEditor = sp.edit();
        spEditor.putString("user", userName);
        spEditor.commit();
        
    }

    void logDebug(String msg)
    {
        Log.d(getClass().getName(), msg);
    }
    
    void logError(String msg)
    {
        Log.e(getClass().getName(), msg);
    }
}
