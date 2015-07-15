package com.rsb.bubblepop;

import java.util.ArrayList;
import java.util.List;

public class BankAccount
{
    public interface BalanceChangedListener
    {
        public void onBalanceChanged(int newBalance, int delta);
    }

    static List<BalanceChangedListener> sListeners = new ArrayList<BalanceChangedListener>();

    public static void addBalanceChangedListener(BalanceChangedListener listener)
    {
        // Store the listener object
        sListeners.add(listener);
    }

    private static int sBalance;

    public static int Deposit(int value)
    {
        sBalance += value;

        dispatchBalanceChanged(value);

        return (sBalance);
    }

    public static int Withdrawal(int value)
    {
        sBalance -= value;

        dispatchBalanceChanged(-value);

        return (sBalance);
    }

    private static void dispatchBalanceChanged(int delta)
    {
        for (BalanceChangedListener listener : sListeners)
        {
            listener.onBalanceChanged(sBalance, delta);
        }
    }
}