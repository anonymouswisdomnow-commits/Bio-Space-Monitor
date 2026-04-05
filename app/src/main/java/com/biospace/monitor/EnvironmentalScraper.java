package com.biospace.monitor;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;

public class EnvironmentalScraper {
    private final OkHttpClient client = new OkHttpClient();

    public interface DataCallback {
        void onSuccess(double value1, double value2);
        void onError(Exception e);
    }

    public void fetchHemisphericPower(DataCallback callback) {
        new Thread(() -> {
            try {
                // NOAA HPI JSON feed
                Request request = new Request.Builder()
                    .url("https://services.swpc.noaa.gov/json/ovation_aurora_latest.json")
                    .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        // This feed is a bit larger, but we'll extract the global power estimate
                        callback.onSuccess(45.5, 0.0); // Logic to parse HPI goes here
                    }
                }
            } catch (Exception e) {
                callback.onError(e);
            }
        }).start();
    }

    // Keep your existing fetchSpaceWeather logic below...
}
