package com.rsb.bubblepop;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;

public class PurchaseButton extends Button implements View.OnClickListener
{
    public static final int VIRTUAL_CURRENCY_PURCHASEAMOUNT = 25;
    public static final String VIRTUAL_CURRENCY_NAME = "Coins";

    // Item that's being purchased with this button
    private static final String sItemName= VIRTUAL_CURRENCY_PURCHASEAMOUNT + " " + VIRTUAL_CURRENCY_NAME;

    public PurchaseButton(Context context)
    {
        super(context);
        init();
    }

    public PurchaseButton(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init();
    }

    public PurchaseButton(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        init();
    }

    private void init()
    {
        setText("BUY (" + sItemName + ")");
        setOnClickListener(this);
    }

    @Override
    public void onClick(View v)
    {
        getPurchaseHelper().BeginPurchase(sItemName, 1.99);

        new AlertDialog.Builder(this.getContext())
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setTitle("Purchase confirmation")
            .setMessage("Are you sure you want to buy " + VIRTUAL_CURRENCY_PURCHASEAMOUNT + " coins?")
            .setCancelable(false)
            .setPositiveButton("Yes", new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    getPurchaseHelper().CompletePurchase(VIRTUAL_CURRENCY_PURCHASEAMOUNT);
                }
    
            })
            .setNegativeButton("No", new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    getPurchaseHelper().CancelPurchase();
                }
            })
            .show();
    }

    private PurchaseHelper getPurchaseHelper()
    {
        // The view context (MainActivity) implements the PurchaseHelper
        return (PurchaseHelper)getContext();
    }
}