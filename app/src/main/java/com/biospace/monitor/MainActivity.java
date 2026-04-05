package com.biospace.monitor;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends Activity {
    private EnvironmentalScraper scraper = new EnvironmentalScraper();
    private BioSpaceBrain brain = new BioSpaceBrain();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView spaceView = findViewById(R.id.space_data);
        
        scraper.fetchSpaceWeather(new EnvironmentalScraper.DataCallback() {
            @Override
            public void onSuccess(double bz, double wind) {
                runOnUiThread(() -> {
                    spaceView.setText("Bz: " + bz + " nT | Wind: " + wind + " km/s");
                    // The Brain immediately analyzes the satellite data
                    EnvironmentalScraper.EnvData currentEnv = new EnvironmentalScraper.EnvData();
                    currentEnv.bz = bz;
                    currentEnv.windSpeed = wind;
                    brain.analyzeANSInteraction(70, 120, currentEnv);
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> spaceView.setText("Space Data: Connection Error"));
            }
        });
    }
}
