package com.biospace.monitor;

import android.app.Activity;
import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.RadioGroup;

public class MainActivity extends Activity {
    private MaturityManager maturity = new MaturityManager();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RadioButton r30 = findViewById(R.id.radio_flare);
        RadioButton r60 = findViewById(R.id.radio_predict); // Future expansion
        RadioButton r90 = findViewById(R.id.radio_predict);

        // Check age and unlock features
        int age = maturity.getAppAgeInDays(this);
        maturity.updateUI(age, r30, r60, r90);
    }
}
