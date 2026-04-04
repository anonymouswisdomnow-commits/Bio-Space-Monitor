package com.biospace.monitor.ui.ai;

import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.biospace.monitor.R;
import com.biospace.monitor.data.BiometricReading;
import com.biospace.monitor.data.BiometricRepository;
import com.biospace.monitor.data.Calibration;
import com.biospace.monitor.data.EnvironmentSnapshot;
import com.biospace.monitor.sensors.GeminiService;
import com.google.android.material.button.MaterialButton;
import java.util.List;

public class AiFragment extends Fragment {

    private BiometricRepository repository;
    private GeminiService geminiService;

    private EditText etApiKey, etPrompt;
    private TextView tvResponse;
    private ScrollView scrollResponse;
    private MaterialButton btnAnalyze;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ai, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository    = BiometricRepository.getInstance(requireContext());
        geminiService = new GeminiService();

        etApiKey      = view.findViewById(R.id.et_api_key);
        etPrompt      = view.findViewById(R.id.et_ai_prompt);
        tvResponse    = view.findViewById(R.id.tv_ai_response);
        scrollResponse = view.findViewById(R.id.scroll_ai_response);
        btnAnalyze    = view.findViewById(R.id.btn_analyze);

        // Pre-fill saved key
        String savedKey = repository.getGeminiApiKey();
        if (!savedKey.isEmpty()) etApiKey.setText(savedKey);

        btnAnalyze.setOnClickListener(v -> analyze());

        view.findViewById(R.id.btn_quick_triggers).setOnClickListener(v ->
            runQuick("Analyze my biometric and environmental data to identify what factors (weather pressure, temperature, Kp index, solar flux) correlate with my abnormal readings. Rank the most likely triggers by correlation strength."));
        view.findViewById(R.id.btn_quick_trends).setOnClickListener(v ->
            runQuick("Analyze trends in my biometric data. Are readings improving, worsening, or stable? What time-of-day patterns exist? What does my HRV trend suggest about autonomic health?"));
        view.findViewById(R.id.btn_quick_space).setOnClickListener(v ->
            runQuick("Focus on space weather effects. Correlate elevated Kp index readings with my abnormal biometric events. Is there evidence geomagnetic activity affects my cardiovascular measurements?"));
        view.findViewById(R.id.btn_quick_summary).setOnClickListener(v ->
            runQuick("Give me a comprehensive health summary: average BP and category, HRV health assessment, SpO2 status, stress patterns, and any concerning trends to discuss with my doctor."));
    }

    private void runQuick(String prompt) {
        etPrompt.setText(prompt);
        analyze();
    }

    private void analyze() {
        String key = etApiKey.getText().toString().trim();
        String prompt = etPrompt.getText().toString().trim();

        if (key.isEmpty()) {
            Toast.makeText(requireContext(), "Enter your Gemini API key first", Toast.LENGTH_SHORT).show();
            return;
        }
        if (prompt.isEmpty()) {
            Toast.makeText(requireContext(), "Enter a question or tap a quick analysis button", Toast.LENGTH_SHORT).show();
            return;
        }

        // Save key
        repository.setGeminiApiKey(key);
        btnAnalyze.setEnabled(false);
        tvResponse.setText("⏳ Analyzing your data with Gemini...");

        repository.getRecentReadings(30, readings -> {
            repository.getAllSnapshotsLive().observe(getViewLifecycleOwner(), snaps -> {
                repository.getActiveCalibration(cal -> {
                    List<EnvironmentSnapshot> envList = snaps != null ? snaps.subList(0, Math.min(10, snaps.size())) : null;
                    geminiService.analyze(key, prompt, readings, envList, cal, new GeminiService.AiCallback() {
                        @Override
                        public void onResponse(String text) {
                            if (getActivity() == null) return;
                            getActivity().runOnUiThread(() -> {
                                btnAnalyze.setEnabled(true);
                                // Convert markdown bold to HTML bold
                                String html = text
                                    .replace("&", "&amp;")
                                    .replace("<", "&lt;")
                                    .replaceAll("\\*\\*(.*?)\\*\\*", "<b>$1</b>")
                                    .replace("\n", "<br>");
                                tvResponse.setText(Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT));
                                scrollResponse.post(() -> scrollResponse.fullScroll(View.FOCUS_UP));
                            });
                        }
                        @Override
                        public void onError(String message) {
                            if (getActivity() == null) return;
                            getActivity().runOnUiThread(() -> {
                                btnAnalyze.setEnabled(true);
                                tvResponse.setText("Error: " + message);
                            });
                        }
                    });
                });
            });
        });
    }
}
