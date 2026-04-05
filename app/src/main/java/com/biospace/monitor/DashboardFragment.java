package com.biospace.monitor;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DashboardFragment extends Fragment implements Y007Manager.Listener {

    private TextView tvConnectionStatus, tvBp, tvBpCategory, tvHR, tvSpO2;
    private TextView tvSteps, tvCalories, tvSleep, tvTemp, tvLastUpdate;
    private TextView tvAnsAlert;
    private View cardAnsAlert;
    private MaterialButton btnConnect;

    private Y007Manager y007;
    private SpaceWeatherData currentSpace;
    private ReadingDatabase db;
    private Handler handler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvConnectionStatus = view.findViewById(R.id.tv_connection_status);
        tvBp = view.findViewById(R.id.tv_bp);
        tvBpCategory = view.findViewById(R.id.tv_bp_category);
        tvHR = view.findViewById(R.id.tv_hr);
        tvSpO2 = view.findViewById(R.id.tv_spo2);
        tvSteps = view.findViewById(R.id.tv_steps);
        tvCalories = view.findViewById(R.id.tv_calories);
        tvSleep = view.findViewById(R.id.tv_sleep);
        tvTemp = view.findViewById(R.id.tv_temp);
        tvLastUpdate = view.findViewById(R.id.tv_last_update);
        tvAnsAlert = view.findViewById(R.id.tv_ans_alert);
        cardAnsAlert = view.findViewById(R.id.card_ans_alert);
        btnConnect = view.findViewById(R.id.btn_connect);

        db = ReadingDatabase.get(requireContext());

        y007 = new Y007Manager(requireContext());
        y007.setListener(this);

        btnConnect.setOnClickListener(v -> {
            if (y007.isConnected()) {
                y007.disconnect();
                btnConnect.setText("Connect");
            } else {
                requestPermissionsAndScan();
            }
        });

        // Fetch space weather
        fetchSpaceWeather();

        // Auto-refresh space weather every 15 minutes
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                fetchSpaceWeather();
                handler.postDelayed(this, 15 * 60 * 1000);
            }
        }, 15 * 60 * 1000);
    }

    private void fetchSpaceWeather() {
        new SpaceWeatherService().fetch(new SpaceWeatherService.Callback() {
            @Override
            public void onData(SpaceWeatherData data) {
                currentSpace = data;
                updateAnsAlert(data);
            }
            @Override
            public void onError(String msg) {
                // Silent fail - not critical
            }
        });
    }

    private void updateAnsAlert(SpaceWeatherData data) {
        if (data.ansRiskLevel.equals("low")) {
            cardAnsAlert.setVisibility(View.GONE);
        } else {
            cardAnsAlert.setVisibility(View.VISIBLE);
            tvAnsAlert.setText(data.ansRiskSummary);
        }
    }

    private void requestPermissionsAndScan() {
        String[] perms = {
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.ACCESS_FINE_LOCATION
        };
        boolean needRequest = false;
        for (String p : perms) {
            if (ContextCompat.checkSelfPermission(requireContext(), p) != PackageManager.PERMISSION_GRANTED) {
                needRequest = true;
                break;
            }
        }
        if (needRequest) {
            ActivityCompat.requestPermissions(requireActivity(), perms, 100);
        } else {
            startScan();
        }
    }

    private void startScan() {
        tvConnectionStatus.setText("🔵 Scanning for Y007...");
        btnConnect.setText("Scanning...");
        y007.startScan();
    }

    @Override
    public void onConnected(String deviceName) {
        tvConnectionStatus.setText("🟢 Connected: " + deviceName);
        btnConnect.setText("Disconnect");
    }

    @Override
    public void onDisconnected() {
        tvConnectionStatus.setText("⚫ Disconnected");
        btnConnect.setText("Connect");
    }

    @Override
    public void onScanStarted() {
        tvConnectionStatus.setText("🔵 Scanning...");
    }

    @Override
    public void onReadingReceived(BiometricReading reading) {
        // New BP reading from watch button press - save it
        if (currentSpace != null) {
            reading.kpAtReading = currentSpace.kpIndex;
            reading.bzAtReading = currentSpace.bzComponent;
            reading.spaceRiskAtReading = currentSpace.ansRiskLevel;
        }
        new Thread(() -> db.save(reading)).start();
        updateUI(reading);
        tvLastUpdate.setText("Reading taken at " + new SimpleDateFormat("h:mm a", Locale.getDefault())
            .format(new Date(reading.timestamp)));
    }

    @Override
    public void onDataUpdated() {
        // Continuous data update - just refresh display
        BiometricReading r = y007.getCurrentReading(currentSpace);
        updateUI(r);
    }

    private void updateUI(BiometricReading r) {
        if (r.systolic > 0 && r.diastolic > 0) {
            tvBp.setText(r.systolic + "/" + r.diastolic);
        }
        tvBpCategory.setText(r.bpCategory());
        if (r.heartRate > 0) tvHR.setText(String.valueOf(r.heartRate));
        if (r.spo2 > 0) tvSpO2.setText(String.format("%.0f", r.spo2));
        if (r.steps > 0) tvSteps.setText(String.format("%,d", r.steps));
        if (r.calories > 0) tvCalories.setText(String.valueOf(r.calories));
        if (r.sleepHours > 0) tvSleep.setText(String.format("%.1f", r.sleepHours));
        if (r.bodyTemp > 0) tvTemp.setText(String.format("%.1f", r.bodyTemp));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
        if (y007 != null) y007.disconnect();
    }
}
