package com.biospace.monitor;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SpaceWeatherService {
    private static final String TAG = "SpaceWeather";

    private static final String URL_KP =
        "https://services.swpc.noaa.gov/products/noaa-planetary-k-index-forecast.json";
    private static final String URL_SOLAR_WIND_MAG =
        "https://services.swpc.noaa.gov/products/solar-wind/mag-7-day.json";
    private static final String URL_SOLAR_WIND_PLASMA =
        "https://services.swpc.noaa.gov/products/solar-wind/plasma-7-day.json";
    private static final String URL_SOLAR_FLUX =
        "https://services.swpc.noaa.gov/json/solar_flux.json";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface Callback {
        void onData(SpaceWeatherData data);
        void onError(String msg);
    }

    public void fetch(Callback callback) {
        executor.execute(() -> {
            SpaceWeatherData data = new SpaceWeatherData();
            try {
                // Fetch Kp index
                String kpJson = get(URL_KP);
                if (kpJson != null) {
                    JSONArray arr = new JSONArray(kpJson);
                    // Last entry is most recent forecast
                    for (int i = arr.length() - 1; i >= 0; i--) {
                        JSONArray row = arr.getJSONArray(i);
                        if (row.length() >= 2 && !row.getString(0).equals("time_tag")) {
                            try {
                                data.kpIndex = (float) row.getDouble(1);
                                break;
                            } catch (Exception ignored) {}
                        }
                    }
                }

                // Fetch solar wind magnetic field (Bz)
                String magJson = get(URL_SOLAR_WIND_MAG);
                if (magJson != null) {
                    JSONArray arr = new JSONArray(magJson);
                    // Last entry = most recent
                    for (int i = arr.length() - 1; i >= 0; i--) {
                        JSONArray row = arr.getJSONArray(i);
                        if (row.length() >= 7 && !row.getString(0).equals("time_tag")) {
                            try {
                                // Bz is column 6 in NOAA mag data
                                data.bzComponent = (float) row.getDouble(6);
                                break;
                            } catch (Exception ignored) {}
                        }
                    }
                }

                // Fetch solar wind plasma (speed, density)
                String plasmaJson = get(URL_SOLAR_WIND_PLASMA);
                if (plasmaJson != null) {
                    JSONArray arr = new JSONArray(plasmaJson);
                    for (int i = arr.length() - 1; i >= 0; i--) {
                        JSONArray row = arr.getJSONArray(i);
                        if (row.length() >= 3 && !row.getString(0).equals("time_tag")) {
                            try {
                                data.solarWindSpeed = (float) row.getDouble(2);
                                data.solarWindDensity = (float) row.getDouble(1);
                                break;
                            } catch (Exception ignored) {}
                        }
                    }
                }

                // Fetch solar flux
                String fluxJson = get(URL_SOLAR_FLUX);
                if (fluxJson != null) {
                    JSONArray arr = new JSONArray(fluxJson);
                    if (arr.length() > 0) {
                        JSONObject latest = arr.getJSONObject(arr.length() - 1);
                        if (latest.has("flux")) {
                            data.solarFlux = (float) latest.getDouble("flux");
                        }
                    }
                }

                data.evaluate();
                mainHandler.post(() -> callback.onData(data));

            } catch (Exception e) {
                Log.e(TAG, "Error fetching space weather: " + e.getMessage());
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    private String get(String urlStr) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestMethod("GET");
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();
            return sb.toString();
        } catch (Exception e) {
            Log.e(TAG, "GET failed for " + urlStr + ": " + e.getMessage());
            return null;
        }
    }
}
