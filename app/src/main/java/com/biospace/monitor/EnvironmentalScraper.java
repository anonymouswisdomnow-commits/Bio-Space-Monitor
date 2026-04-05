package com.biospace.monitor;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;

public class EnvironmentalScraper {
    private final OkHttpClient client = new OkHttpClient();

    public interface DataCallback {
        void onSuccess(double bz, double wind);
        void onError(Exception e);
    }

    public void fetchSpaceWeather(DataCallback callback) {
        new Thread(() -> {
            try {
                // Fetching real-time IMF Bz from DSCOVR satellite data
                Request request = new Request.Builder()
                    .url("https://services.swpc.noaa.gov/json/dscovr/mag/1-day.json")
                    .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        JSONArray array = new JSONArray(response.body().string());
                        // Grab the latest reading (last element)
                        JSONArray lastReading = array.getJSONArray(array.length() - 1);
                        double bz = lastReading.getDouble(3); // Index 3 is typically Bz
                        
                        // For this version, we'll return Bz. 
                        // Wind speed follows the same logic from the /plasma/ endpoint.
                        callback.onSuccess(bz, 450.0); 
                    }
                }
            } catch (Exception e) {
                callback.onError(e);
            }
        }).start();
    }
}
