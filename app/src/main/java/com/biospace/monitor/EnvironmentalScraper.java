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
                        String responseData = response.body().string();
                        JSONArray array = new JSONArray(responseData);
                        // Grab the latest reading (last element in the array)
                        JSONArray lastReading = array.getJSONArray(array.length() - 1);
                        double bz = lastReading.getDouble(3); 
                        
                        callback.onSuccess(bz, 450.0); // Placeholder for wind speed
                    }
                }
            } catch (Exception e) {
                callback.onError(e);
            }
        }).start();
    }
}
