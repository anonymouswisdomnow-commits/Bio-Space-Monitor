package com.biospace.monitor.sensors;

import android.os.Handler;
import android.os.Looper;
import com.biospace.monitor.data.BiometricReading;
import com.biospace.monitor.data.Calibration;
import com.biospace.monitor.data.EnvironmentSnapshot;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GeminiService {

    private static final String BASE_URL =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final SimpleDateFormat sdf = new SimpleDateFormat("MM/dd HH:mm", Locale.US);

    public interface AiCallback {
        void onResponse(String text);
        void onError(String message);
    }

    public void analyze(
            String apiKey,
            String userPrompt,
            List<BiometricReading> readings,
            List<EnvironmentSnapshot> envSnapshots,
            Calibration calibration,
            AiCallback callback) {

        if (apiKey == null || apiKey.isEmpty()) {
            mainHandler.post(() -> callback.onError("No Gemini API key configured. Go to Settings to add it."));
            return;
        }

        executor.execute(() -> {
            try {
                String context = buildContext(userPrompt, readings, envSnapshots, calibration);

                JSONObject requestBody = new JSONObject();
                JSONArray contents = new JSONArray();
                JSONObject content = new JSONObject();
                JSONArray parts = new JSONArray();
                JSONObject part = new JSONObject();
                part.put("text", context);
                parts.put(part);
                content.put("parts", parts);
                contents.put(content);
                requestBody.put("contents", contents);

                JSONObject genConfig = new JSONObject();
                genConfig.put("maxOutputTokens", 1000);
                genConfig.put("temperature", 0.7);
                requestBody.put("generationConfig", genConfig);

                URL url = new URL(BASE_URL + apiKey);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(requestBody.toString().getBytes("UTF-8"));
                }

                int code = conn.getResponseCode();
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                    code == 200 ? conn.getInputStream() : conn.getErrorStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();
                conn.disconnect();

                if (code != 200) {
                    JSONObject err = new JSONObject(sb.toString());
                    String msg = err.optJSONObject("error") != null
                        ? err.getJSONObject("error").optString("message", "Unknown error")
                        : "HTTP " + code;
                    mainHandler.post(() -> callback.onError("Gemini error: " + msg));
                    return;
                }

                JSONObject response = new JSONObject(sb.toString());
                String text = response
                    .getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text");

                mainHandler.post(() -> callback.onResponse(text));

            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Network error: " + e.getMessage()));
            }
        });
    }

    private String buildContext(
            String userPrompt,
            List<BiometricReading> readings,
            List<EnvironmentSnapshot> envSnapshots,
            Calibration cal) {

        StringBuilder sb = new StringBuilder();
        sb.append("You are a medical data analyst AI for the BioSpace Monitor app. ");
        sb.append("Analyze the user's biometric and environmental data. ");
        sb.append("Provide specific pattern analysis and correlations. ");
        sb.append("Be direct and data-focused. Note you are not giving medical advice, just data analysis.\n\n");

        sb.append("=== RECENT BIOMETRIC READINGS (").append(readings.size()).append(") ===\n");
        for (BiometricReading r : readings) {
            sb.append(sdf.format(new Date(r.timestamp)));
            sb.append(String.format(Locale.US, ": BP=%d/%d HR=%d HRV=%dms SpO2=%.0f%% Temp=%.1f°F Stress=%d Status=%s",
                r.systolic, r.diastolic, r.heartRate, r.hrv, r.spo2, r.bodyTemp, r.stressScore, r.status));
            if (r.issues != null && !r.issues.isEmpty()) sb.append(" Issues=[").append(r.issues).append("]");
            sb.append("\n");
        }

        if (!envSnapshots.isEmpty()) {
            sb.append("\n=== ENVIRONMENTAL DATA (").append(envSnapshots.size()).append(") ===\n");
            for (EnvironmentSnapshot e : envSnapshots) {
                sb.append(sdf.format(new Date(e.timestamp)));
                sb.append(String.format(Locale.US, ": Baro=%.0fmmHg Temp=%.0f°F Humidity=%.0f%% Kp=%.1f Solar=%.0fsfu",
                    e.baroPressure, e.temperature, e.humidity, e.kpIndex, e.solarFlux));
                sb.append("\n");
            }
        }

        if (cal != null) {
            sb.append("\n=== CALIBRATION ===\n");
            sb.append(String.format(Locale.US, "Active calibration: SysOffset=%+d DiaOffset=%+d (applied to all readings above)\n",
                cal.offsetSystolic, cal.offsetDiastolic));
        }

        sb.append("\n=== USER QUESTION ===\n").append(userPrompt);
        return sb.toString();
    }
}
