package com.example.ivandimitrov.rxjavatest;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements MyDataFragment.JokeReceivedListener {
    private TextView       mTextView;
    private MyDataFragment mMyDataFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ArrayList<CountdownIndicator> indicators = new ArrayList<>();

        CountdownIndicator indicator1 = (CountdownIndicator) findViewById(R.id.countdown_icon_1);
        CountdownIndicator indicator2 = (CountdownIndicator) findViewById(R.id.countdown_icon_2);
        CountdownIndicator indicator3 = (CountdownIndicator) findViewById(R.id.countdown_icon_3);

        indicators.add(indicator1);
        indicators.add(indicator2);
        indicators.add(indicator3);

        Switch button = (Switch) findViewById(R.id.button_toggle);
        mTextView = (TextView) findViewById(R.id.random_joke);

        if (savedInstanceState == null) {
            FragmentManager fragmentManager = getFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            mMyDataFragment = new MyDataFragment();
            mMyDataFragment.setRetainInstance(true);
            fragmentTransaction.add(mMyDataFragment, "fragment");
            fragmentTransaction.commit();
            fragmentManager.executePendingTransactions();
            mMyDataFragment.startSession(indicators);
        } else {
            FragmentManager fragmentManager = getFragmentManager();
            mMyDataFragment = (MyDataFragment) fragmentManager.findFragmentByTag("fragment");
            mMyDataFragment.setIndicator(indicators);
        }

        button.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                mMyDataFragment.stopReader();
            } else {
                mMyDataFragment.resumeReader();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMyDataFragment.setListener(this);
        String response = mMyDataFragment.pullResponse();
        if (response != null) {
            onJokeReceived(response);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMyDataFragment.clearListener();
    }

    @Override
    public void onJokeReceived(String joke) {
        mTextView.setText(joke);
    }
}
