package com.biospace.monitor;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Toast;

public class MainActivity extends Activity {
    private ReportEngine reportEngine = new ReportEngine();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnReport = findViewById(R.id.btn_report);
        RadioGroup group = findViewById(R.id.report_type_group);

        btnReport.setOnClickListener(v -> {
            int selectedId = group.getCheckedRadioButtonId();
            int type = (selectedId == R.id.radio_general) ? 1 : (selectedId == R.id.radio_flare) ? 2 : 3;
            
            // This triggers the Gemini AI Request
            String finalPrompt = reportEngine.buildAIPrompt(type, "Sample Health Data", "Sample Space Data");
            Toast.makeText(this, "Generating Report Type: " + type, Toast.LENGTH_LONG).show();
            
            // Code to send 'finalPrompt' to Gemini API using the User's Key goes here
        });
    }
}
