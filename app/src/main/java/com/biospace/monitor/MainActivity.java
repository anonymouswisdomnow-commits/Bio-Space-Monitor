package com.biospace.monitor;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends Activity {
    private EnvironmentalScraper spaceScraper = new EnvironmentalScraper();
    private WeatherScraper weatherScraper = new WeatherScraper();
    private BioSpaceBrain brain = new BioSpaceBrain();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView spaceView = findViewById(R.id.space_data);
        TextView envView = findViewById(R.id.env_data);

        // 1. Fetch Space Weather (Bz)
        spaceScraper.fetchSpaceWeather(new EnvironmentalScraper.DataCallback() {
            @Override
            public void onSuccess(double bz, double wind) {
                runOnUiThread(() -> {
                    spaceView.setText("Bz: " + bz + " nT | Instability: " + brain.analyzeSystemInstability(bz, wind));
                });
            }
            @Override public void onError(Exception e) {}
        });

        // 2. Fetch Local Weather (Pressure) - Using Ouachita Parish approx coordinates
        weatherScraper.fetchLocalWeather(32.5, -92.1, new EnvironmentalScraper.DataCallback() {
            @Override
            public void onSuccess(double pressure, double unused) {
                runOnUiThread(() -> envView.setText("Baro: " + pressure + " hPa | HPI: Active"));
            }
            @Override public void onError(Exception e) {}
        });
    }
}
