package com.biospace.monitor;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;

public class WeatherScraper {
    private final OkHttpClient client = new OkHttpClient();
    // Using a public test key for now - you can replace with your own later
    private final String API_KEY = "b6907d289e10d714a6e88b30761fae22"; 

    public void fetchLocalWeather(double lat, double lon, EnvironmentalScraper.DataCallback callback) {
        new Thread(() -> {
            try {
                // Fetching pressure, humidity, and temp
                String url = "https://api.openweathermap.org/data/2.5/weather?lat=" + lat + "&lon=" + lon + "&appid=" + API_KEY + "&units=metric";
                Request request = new Request.Builder().url(url).build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        JSONObject json = new JSONObject(response.body().string());
                        double pressure = json.getJSONObject("main").getDouble("pressure");
                        // We'll pass the pressure back through the callback
                        callback.onSuccess(pressure, 0.0);
                    }
                }
            } catch (Exception e) {
                callback.onError(e);
            }
        }).start();
    }
}
