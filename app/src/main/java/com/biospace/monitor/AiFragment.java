package com.biospace.monitor;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONObject;

public class AiFragment extends Fragment {
    private EditText etApiKey;
    private TextView tvResponse;
    private MaterialButton btnAnalyze;
    private ReadingDatabase db;
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ai, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        etApiKey = view.findViewById(R.id.et_api_key);
        tvResponse = view.findViewById(R.id.tv_ai_response);
        btnAnalyze = view.findViewById(R.id.btn_analyze);
        db = ReadingDatabase.get(requireContext());

        // Restore saved API key
        String savedKey = BioSpaceApp.get().getGeminiKey();
        if (!savedKey.isEmpty()) etApiKey.setText(savedKey);

        btnAnalyze.setOnClickListener(v -> {
            String key = etApiKey.getText().toString().trim();
            if (key.isEmpty()) {
                tvResponse.setText("Please enter your Gemini API key first.\nGet one free at: aistudio.google.com");
                return;
            }
            BioSpaceApp.get().setGeminiKey(key);
            analyze(key);
        });
    }

    private void analyze(String apiKey) {
        tvResponse.setText("Analyzing your biometric and space weather data...");
        btnAnalyze.setEnabled(false);

        executor.execute(() -> {
            List<BiometricReading> readings = db.getRecent(20);
            if (readings.isEmpty()) {
                requireActivity().runOnUiThread(() -> {
                    tvResponse.setText("No readings found. Take some readings first by pressing the button on your Y007 watch.");
                    btnAnalyze.setEnabled(true);
                });
                return;
            }

            // Build context for Gemini
            StringBuilder sb = new StringBuilder();
            sb.append("You are a clinical health analyst specializing in Autonomic Nervous System (ANS) disorders and dysautonomia. ");
            sb.append("Analyze the following biometric readings from a BP Doctor FIT smartwatch, correlated with real-time space weather data. ");
            sb.append("Focus on ANS patterns, dysautonomia indicators, and space weather correlations.\n\n");
            sb.append("BIOMETRIC READINGS (most recent first):\n");

            for (int i = 0; i < Math.min(readings.size(), 10); i++) {
                BiometricReading r = readings.get(i);
                sb.append(String.format("- BP: %d/%d mmHg | HR: %d bpm | SpO2: %.0f%%",
                    r.systolic, r.diastolic, r.heartRate, r.spo2));
                if (r.kpAtReading >= 0) {
                    sb.append(String.format(" | Kp: %.1f | Bz: %.1fnT | Space Risk: %s",
                        r.kpAtReading, r.bzAtReading, r.spaceRiskAtReading));
                }
                sb.append("\n");
            }

            sb.append("\nSPACE WEATHER CONTEXT:\n");
            sb.append("The following metrics affect the ANS:\n");
            sb.append("- CMEs cause HRV drops and tachycardia (arrive in 1-3 days)\n");
            sb.append("- Solar flares cause BP spikes and brain fog (arrive in 8 minutes)\n");
            sb.append("- Southward Bz opens the magnetic shield - primary dysautonomia trigger\n");
            sb.append("- High solar wind density causes sympathetic dominance (anxiety, insomnia)\n");
            sb.append("- High Kp correlates with arrhythmias in sensitive populations\n\n");
            sb.append("Please provide: 1) Pattern analysis, 2) ANS risk assessment, 3) Space weather correlation, 4) Recommendations.");

            try {
                String result = callGemini(apiKey, sb.toString());
                String finalResult = result;
                requireActivity().runOnUiThread(() -> {
                    tvResponse.setText(finalResult);
                    btnAnalyze.setEnabled(true);
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    tvResponse.setText("Error: " + e.getMessage() + "\n\nCheck your API key and internet connection.");
                    btnAnalyze.setEnabled(true);
                });
            }
        });
    }

    private String callGemini(String apiKey, String prompt) throws Exception {
        String urlStr = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key=" + apiKey;
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(30000);

        JSONObject body = new JSONObject();
        JSONArray contents = new JSONArray();
        JSONObject content = new JSONObject();
        JSONArray parts = new JSONArray();
        JSONObject part = new JSONObject();
        part.put("text", prompt);
        parts.put(part);
        content.put("parts", parts);
        contents.put(content);
        body.put("contents", contents);

        OutputStream os = conn.getOutputStream();
        os.write(body.toString().getBytes());
        os.close();

        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        reader.close();

        JSONObject response = new JSONObject(sb.toString());
        return response.getJSONArray("candidates").getJSONObject(0)
            .getJSONObject("content").getJSONArray("parts")
            .getJSONObject(0).getString("text");
    }
}
