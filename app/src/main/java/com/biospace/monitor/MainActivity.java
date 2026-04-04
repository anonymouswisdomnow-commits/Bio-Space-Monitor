package com.biospace.monitor;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.biospace.monitor.data.BiometricRepository;
import com.biospace.monitor.data.Calibration;
import com.biospace.monitor.sensors.BiometricEngine;
import com.biospace.monitor.ui.ai.AiFragment;
import com.biospace.monitor.ui.calibrate.CalibrateFragment;
import com.biospace.monitor.ui.dashboard.DashboardFragment;
import com.biospace.monitor.ui.environment.EnvironmentFragment;
import com.biospace.monitor.ui.log.LogFragment;
import com.biospace.monitor.ui.report.ReportFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PERM_REQUEST = 100;
    private static final long MONITOR_INTERVAL_MS = 30_000;

    private BiometricEngine biometricEngine;
    private BiometricRepository repository;
    private Handler monitorHandler;
    private boolean monitoring = false;

    // Shared engine accessor for fragments
    private static MainActivity instance;
    public static BiometricEngine getEngine() {
        return instance != null ? instance.biometricEngine : null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;
        setContentView(R.layout.activity_main);

        repository = BiometricRepository.getInstance(this);
        monitorHandler = new Handler(Looper.getMainLooper());

        // Load calibration then start engine
        repository.getActiveCalibration(cal -> {
            biometricEngine = new BiometricEngine(this);
            biometricEngine.setCalibration(cal);
            biometricEngine.setListener(new BiometricEngine.ReadingListener() {
                @Override
                public void onReadingReady(com.biospace.monitor.data.BiometricReading reading) {
                    // Handled in DashboardFragment
                }
                @Override
                public void onBluetoothStateChanged(boolean connected, String deviceName) {
                    runOnUiThread(() -> showBluetoothStatus(connected, deviceName));
                }
            });
            startMonitoring();
        });

        setupBottomNav();
        requestPermissions();

        if (savedInstanceState == null) {
            showFragment(new DashboardFragment());
        }
    }

    private void setupBottomNav() {
        BottomNavigationView nav = findViewById(R.id.bottom_nav);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_dashboard)    showFragment(new DashboardFragment());
            else if (id == R.id.nav_calibrate) showFragment(new CalibrateFragment());
            else if (id == R.id.nav_log)      showFragment(new LogFragment());
            else if (id == R.id.nav_env)      showFragment(new EnvironmentFragment());
            else if (id == R.id.nav_ai)       showFragment(new AiFragment());
            else if (id == R.id.nav_report)   showFragment(new ReportFragment());
            return true;
        });
    }

    private void showFragment(Fragment fragment) {
        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit();
    }

    public void startMonitoring() {
        if (monitoring) return;
        monitoring = true;
        scheduleNextReading();
    }

    private void scheduleNextReading() {
        monitorHandler.postDelayed(() -> {
            if (biometricEngine != null) {
                com.biospace.monitor.data.BiometricReading reading = biometricEngine.buildReading();
                repository.insertReading(reading, null);
                checkAlert(reading);
            }
            if (monitoring) scheduleNextReading();
        }, MONITOR_INTERVAL_MS);
    }

    public com.biospace.monitor.data.BiometricReading takeReadingNow() {
        if (biometricEngine == null) return null;
        com.biospace.monitor.data.BiometricReading reading = biometricEngine.buildReading();
        repository.insertReading(reading, null);
        checkAlert(reading);
        return reading;
    }

    private void checkAlert(com.biospace.monitor.data.BiometricReading r) {
        if (!"ok".equals(r.status)) {
            long now = System.currentTimeMillis();
            long last = repository.getLastAlertTime();
            if (now - last > 300_000) {
                repository.setLastAlertTime(now);
                runOnUiThread(() -> showAlertBanner(r));
            }
        }
    }

    private void showAlertBanner(com.biospace.monitor.data.BiometricReading r) {
        View root = findViewById(android.R.id.content);
        String msg = "⚠️ " + r.issues;
        Snackbar.make(root, msg, Snackbar.LENGTH_LONG)
            .setBackgroundTint(getColor(R.color.colorAlert))
            .setTextColor(0xFFFFFFFF)
            .setAction("View", v -> {
                BottomNavigationView nav = findViewById(R.id.bottom_nav);
                nav.setSelectedItemId(R.id.nav_dashboard);
            })
            .show();
    }

    private void showBluetoothStatus(boolean connected, String name) {
        View root = findViewById(android.R.id.content);
        String msg = connected ? "⌚ Connected: " + name : "⌚ Smartwatch disconnected";
        Snackbar.make(root, msg, Snackbar.LENGTH_SHORT).show();
    }

    public void updateCalibration(Calibration cal) {
        if (biometricEngine != null) biometricEngine.setCalibration(cal);
    }

    // ---- Permissions ----

    private void requestPermissions() {
        List<String> needed = new ArrayList<>();
        String[] perms = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BODY_SENSORS,
            Manifest.permission.ACTIVITY_RECOGNITION,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
        };
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            needed.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        for (String p : perms) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED)
                needed.add(p);
        }
        if (!needed.isEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toArray(new String[0]), PERM_REQUEST);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        // Engine re-evaluates available sensors on next read automatically
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        instance = null;
        monitoring = false;
        monitorHandler.removeCallbacksAndMessages(null);
        if (biometricEngine != null) biometricEngine.release();
    }
}
