package com.rsb.bubblepop;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

public class BalanceBox extends TextView implements BankAccount.BalanceChangedListener
{
    public BalanceBox(Context context)
    {
        super(context);
        init();
    }
    
    public BalanceBox(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init();
    }

    public BalanceBox(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        init();
    }

    private void init()
    {
        BankAccount.addBalanceChangedListener(this);
    }

    @Override
    public void onBalanceChanged(int newBalance, int delta)
    {
        // The balance in the BankAccount has changed, so update the text
        setText("Balance: " + newBalance);
    }
}