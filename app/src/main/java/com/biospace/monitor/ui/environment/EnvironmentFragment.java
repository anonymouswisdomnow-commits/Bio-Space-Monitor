package com.biospace.monitor.ui.environment;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.biospace.monitor.R;
import com.biospace.monitor.data.BiometricRepository;
import com.biospace.monitor.data.EnvironmentSnapshot;
import com.biospace.monitor.sensors.WeatherService;
import com.google.android.material.button.MaterialButton;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EnvironmentFragment extends Fragment {

    private BiometricRepository repository;
    private WeatherService weatherService;

    private TextView tvWeatherMain, tvSpaceWeather, tvEnvLog;
    private EditText etBaro, etTemp, etHumidity, etKp, etFlux, etNotes;
    private MaterialButton btnFetch;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_environment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository    = BiometricRepository.getInstance(requireContext());
        weatherService = new WeatherService();

        tvWeatherMain  = view.findViewById(R.id.tv_weather_main);
        tvSpaceWeather = view.findViewById(R.id.tv_space_weather);
        tvEnvLog       = view.findViewById(R.id.tv_env_log);
        etBaro     = view.findViewById(R.id.et_baro);
        etTemp     = view.findViewById(R.id.et_env_temp);
        etHumidity = view.findViewById(R.id.et_humidity);
        etKp       = view.findViewById(R.id.et_kp);
        etFlux     = view.findViewById(R.id.et_flux);
        etNotes    = view.findViewById(R.id.et_env_notes);
        btnFetch   = view.findViewById(R.id.btn_fetch_env);

        btnFetch.setOnClickListener(v -> fetchWeather());
        view.findViewById(R.id.btn_save_snapshot).setOnClickListener(v -> saveSnapshot());

        loadEnvLog();
    }

    private void fetchWeather() {
        btnFetch.setEnabled(false);
        tvWeatherMain.setText("Fetching weather...");
        tvSpaceWeather.setText("Fetching space weather...");

        weatherService.fetchLocationByIp(new WeatherService.LocationCallback() {
            @Override
            public void onLocation(double lat, double lon, String city) {
                weatherService.fetchAll(lat, lon, new WeatherService.WeatherCallback() {
                    @Override
                    public void onSuccess(EnvironmentSnapshot snap) {
                        if (getActivity() == null) return;
                        getActivity().runOnUiThread(() -> {
                            btnFetch.setEnabled(true);
                            snap.city = city;

                            tvWeatherMain.setText(
                                "📍 " + city + "\n" +
                                "🌡 " + String.format("%.0f°F", snap.temperature) + "\n" +
                                "💧 Humidity: " + String.format("%.0f%%", snap.humidity) + "\n" +
                                "📊 Pressure: " + String.format("%.0f mmHg", snap.baroPressure) + "\n" +
                                "💨 Wind: " + String.format("%.0f mph", snap.windSpeed) + "\n" +
                                "🌤 " + snap.weatherDesc);

                            String kpLevel = snap.kpIndex >= 5 ? "🔴 Storm" : snap.kpIndex >= 3 ? "🟡 Active" : "🟢 Quiet";
                            tvSpaceWeather.setText(
                                "🛸 Kp Index: " + String.format("%.1f", snap.kpIndex) + " — " + kpLevel + "\n" +
                                (snap.solarFlux > 0 ? "☀ Solar Flux: " + String.format("%.0f sfu", snap.solarFlux) + "\n" : "") +
                                "Source: NOAA SWPC");

                            // Auto-fill manual fields
                            etBaro.setText(String.format("%.0f", snap.baroPressure));
                            etTemp.setText(String.format("%.0f", snap.temperature));
                            etHumidity.setText(String.format("%.0f", snap.humidity));
                            etKp.setText(String.format("%.1f", snap.kpIndex));
                            if (snap.solarFlux > 0)
                                etFlux.setText(String.format("%.0f", snap.solarFlux));
                        });
                    }

                    @Override
                    public void onError(String message) {
                        if (getActivity() == null) return;
                        getActivity().runOnUiThread(() -> {
                            btnFetch.setEnabled(true);
                            tvWeatherMain.setText("Unable to fetch weather.\nUse manual entry below.");
                            tvSpaceWeather.setText("Unable to fetch space weather.");
                            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            }

            @Override
            public void onError(String message) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    btnFetch.setEnabled(true);
                    tvWeatherMain.setText("Location unavailable.\nUse manual entry below.");
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void saveSnapshot() {
        EnvironmentSnapshot snap = new EnvironmentSnapshot();
        snap.timestamp = System.currentTimeMillis();
        try { snap.baroPressure = Float.parseFloat(etBaro.getText().toString()); } catch (Exception e) {}
        try { snap.temperature  = Float.parseFloat(etTemp.getText().toString()); } catch (Exception e) {}
        try { snap.humidity     = Float.parseFloat(etHumidity.getText().toString()); } catch (Exception e) {}
        try { snap.kpIndex      = Float.parseFloat(etKp.getText().toString()); } catch (Exception e) {}
        try { snap.solarFlux    = Float.parseFloat(etFlux.getText().toString()); } catch (Exception e) {}
        snap.notes = etNotes.getText().toString().trim();

        repository.insertSnapshot(snap, () -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Snapshot saved", Toast.LENGTH_SHORT).show();
                    etNotes.setText("");
                    loadEnvLog();
                });
            }
        });
    }

    private void loadEnvLog() {
        repository.getAllSnapshotsLive().observe(getViewLifecycleOwner(), snaps -> {
            if (snaps == null || snaps.isEmpty()) {
                tvEnvLog.setText("No snapshots saved yet.");
                return;
            }
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd HH:mm", Locale.US);
            StringBuilder sb = new StringBuilder();
            for (EnvironmentSnapshot s : snaps.subList(0, Math.min(snaps.size(), 20))) {
                sb.append(sdf.format(new Date(s.timestamp))).append("\n");
                if (s.temperature > 0)  sb.append("  🌡 ").append(String.format("%.0f°F", s.temperature)).append("  ");
                if (s.humidity > 0)     sb.append("💧").append(String.format("%.0f%%", s.humidity)).append("  ");
                if (s.baroPressure > 0) sb.append("📊").append(String.format("%.0fmmHg", s.baroPressure)).append("  ");
                if (s.kpIndex > 0)      sb.append("🛸Kp").append(String.format("%.1f", s.kpIndex)).append("  ");
                if (s.solarFlux > 0)    sb.append("☀").append(String.format("%.0fsfu", s.solarFlux));
                sb.append("\n");
                if (s.notes != null && !s.notes.isEmpty()) sb.append("  ").append(s.notes).append("\n");
                sb.append("\n");
            }
            tvEnvLog.setText(sb.toString().trim());
        });
    }
}
