package com.rsb.bubblepop;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ToggleButton;

public class LoginButton extends ToggleButton implements OnClickListener
{
    public LoginButton(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    BroadcastReceiver mReceiver;

    @Override
    protected void onAttachedToWindow()
    {
        super.onAttachedToWindow();

        setChecked(getLoginHelper().isLoggedIn());
        setOnClickListener(this);

        // Create a receiver that we'll use to detect when login the actual login state has changed
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                setChecked(getLoginHelper().isLoggedIn());
                setEnabled(true);
            }
        };

        getContext().registerReceiver(mReceiver, new IntentFilter("LOGIN_CHANGED"));
    }

    @Override
    protected void onDetachedFromWindow()
    {
        super.onDetachedFromWindow();

        getContext().unregisterReceiver(mReceiver);
    }

    @SuppressLint("InflateParams")
    @Override
    public void onClick(View v)
    {
        // no button-mashing, tyvm
        setEnabled(false);

        // Clicking on the button toggles it
        // Since we want to manage the checked state manually we toggle it back here
        toggle();

        if (getLoginHelper().isLoggedIn())
        {
            getLoginHelper().Logout();
        }
        else
        {
            LayoutInflater factory = LayoutInflater.from(getContext());
            final View login = factory.inflate(R.layout.login_body, null);

            new AlertDialog.Builder(getContext())
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setTitle("input")
            .setView(login)
            .setCancelable(false)
            .setPositiveButton("Login", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    final EditText nameInput = (EditText) login.findViewById(R.id.name);
                    final String name = nameInput.getText().toString();

                    final RadioGroup genderInput = (RadioGroup) login.findViewById(R.id.gender);
                    final RadioButton genderButton = (RadioButton) login.findViewById(genderInput.getCheckedRadioButtonId());
                    final String gender = genderButton.getText().toString();

                    if(null != name && "" != name)
                    {
                        getLoginHelper().Login(name, gender);
                    }
                }
            })
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    setEnabled(true);
                }
            })
            .show();
        }
    }

    private LoginHelper getLoginHelper()
    {
        // The view context (MainActivity) implements the LoginHelper
        return (LoginHelper)getContext();
    }
}
