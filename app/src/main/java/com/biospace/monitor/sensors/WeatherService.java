package com.biospace.monitor.sensors;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.biospace.monitor.data.EnvironmentSnapshot;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WeatherService {

    private static final String TAG = "WeatherService";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface WeatherCallback {
        void onSuccess(EnvironmentSnapshot snapshot);
        void onError(String message);
    }

    public void fetchAll(double lat, double lon, WeatherCallback callback) {
        executor.execute(() -> {
            EnvironmentSnapshot snap = new EnvironmentSnapshot();
            snap.timestamp = System.currentTimeMillis();
            boolean anySuccess = false;

            // 1. Open-Meteo — free, no API key
            try {
                String url = String.format(
                    "https://api.open-meteo.com/v1/forecast"
                    + "?latitude=%.4f&longitude=%.4f"
                    + "&current=temperature_2m,relative_humidity_2m,surface_pressure,wind_speed_10m,weather_code"
                    + "&temperature_unit=fahrenheit&wind_speed_unit=mph",
                    lat, lon);
                JSONObject root = getJson(url);
                JSONObject current = root.getJSONObject("current");
                snap.temperature  = (float) current.getDouble("temperature_2m");
                snap.humidity     = (float) current.getDouble("relative_humidity_2m");
                // surface_pressure is hPa; convert to mmHg
                snap.baroPressure = (float) (current.getDouble("surface_pressure") * 0.75006);
                snap.windSpeed    = (float) current.getDouble("wind_speed_10m");
                snap.weatherDesc  = weatherCodeToDesc(current.getInt("weather_code"));
                anySuccess = true;
            } catch (Exception e) {
                Log.w(TAG, "Open-Meteo failed: " + e.getMessage());
            }

            // 2. NOAA Kp Index (space weather) — free, no key
            try {
                JSONArray arr = getJsonArray(
                    "https://services.swpc.noaa.gov/products/noaa-planetary-k-index-forecast.json");
                // Last entry: [time_tag, kp, observed, noaa_scale]
                JSONArray latest = arr.getJSONArray(arr.length() - 1);
                snap.kpIndex = (float) latest.getDouble(1);
                anySuccess = true;
            } catch (Exception e) {
                Log.w(TAG, "NOAA Kp failed: " + e.getMessage());
            }

            // 3. NOAA Solar Flux
            try {
                JSONObject root = getJson(
                    "https://services.swpc.noaa.gov/json/solar_flux.json");
                // Look for "f10.7_index" or similar
                if (root.has("Flux")) {
                    snap.solarFlux = (float) root.getDouble("Flux");
                }
            } catch (Exception e) {
                Log.w(TAG, "NOAA solar flux failed: " + e.getMessage());
            }

            final boolean success = anySuccess;
            final EnvironmentSnapshot result = snap;
            mainHandler.post(() -> {
                if (success) callback.onSuccess(result);
                else callback.onError("Unable to fetch weather data. Check your network connection.");
            });
        });
    }

    public void fetchLocationByIp(LocationCallback callback) {
        executor.execute(() -> {
            try {
                JSONObject geo = getJson("https://ipapi.co/json/");
                double lat  = geo.getDouble("latitude");
                double lon  = geo.getDouble("longitude");
                String city = geo.optString("city", "Unknown");
                mainHandler.post(() -> callback.onLocation(lat, lon, city));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Location lookup failed: " + e.getMessage()));
            }
        });
    }

    private String weatherCodeToDesc(int code) {
        if (code == 0) return "Clear sky";
        if (code <= 3) return "Partly cloudy";
        if (code <= 48) return "Foggy";
        if (code <= 67) return "Rainy";
        if (code <= 77) return "Snowy";
        if (code <= 82) return "Rain showers";
        if (code <= 99) return "Thunderstorm";
        return "Unknown";
    }

    private JSONObject getJson(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        conn.setRequestProperty("Accept", "application/json");
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        reader.close();
        conn.disconnect();
        return new JSONObject(sb.toString());
    }

    private JSONArray getJsonArray(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        reader.close();
        conn.disconnect();
        return new JSONArray(sb.toString());
    }

    public interface LocationCallback {
        void onLocation(double lat, double lon, String city);
        void onError(String message);
    }
}
