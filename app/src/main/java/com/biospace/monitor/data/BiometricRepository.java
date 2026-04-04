package com.biospace.monitor.data;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.lifecycle.LiveData;
import com.biospace.monitor.BioSpaceApp;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BiometricRepository {

    private static BiometricRepository instance;
    private final BiometricDao biometricDao;
    private final EnvironmentDao environmentDao;
    private final CalibrationDao calibrationDao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final SharedPreferences prefs;

    private BiometricRepository(Context context) {
        AppDatabase db = BioSpaceApp.getDatabase();
        biometricDao = db.biometricDao();
        environmentDao = db.environmentDao();
        calibrationDao = db.calibrationDao();
        prefs = context.getSharedPreferences("biospace_prefs", Context.MODE_PRIVATE);
    }

    public static synchronized BiometricRepository getInstance(Context context) {
        if (instance == null) {
            instance = new BiometricRepository(context.getApplicationContext());
        }
        return instance;
    }

    // ---- Biometric ----

    public LiveData<List<BiometricReading>> getAllReadingsLive() {
        return biometricDao.getAllLive();
    }

    public void insertReading(BiometricReading reading, Runnable onComplete) {
        executor.execute(() -> {
            biometricDao.insert(reading);
            if (onComplete != null) onComplete.run();
        });
    }

    public void getRecentReadings(int limit, Callback<List<BiometricReading>> callback) {
        executor.execute(() -> callback.onResult(biometricDao.getRecent(limit)));
    }

    public void getReadingsSince(long since, Callback<List<BiometricReading>> callback) {
        executor.execute(() -> callback.onResult(biometricDao.getSince(since)));
    }

    public void clearAllReadings(Runnable onComplete) {
        executor.execute(() -> {
            biometricDao.deleteAll();
            if (onComplete != null) onComplete.run();
        });
    }

    public void getStats(long since, Callback<ReadingStats> callback) {
        executor.execute(() -> {
            ReadingStats stats = new ReadingStats();
            stats.avgSys = biometricDao.getAvgSystolic(since);
            stats.avgDia = biometricDao.getAvgDiastolic(since);
            stats.avgHR  = biometricDao.getAvgHR(since);
            stats.avgHRV = biometricDao.getAvgHRV(since);
            stats.avgSpO2 = biometricDao.getAvgSpO2(since);
            stats.alertCount = biometricDao.getAlertCount(since);
            stats.warnCount  = biometricDao.getWarnCount(since);
            stats.totalCount = biometricDao.getCount();
            callback.onResult(stats);
        });
    }

    // ---- Environment ----

    public LiveData<List<EnvironmentSnapshot>> getAllSnapshotsLive() {
        return environmentDao.getAllLive();
    }

    public void insertSnapshot(EnvironmentSnapshot snapshot, Runnable onComplete) {
        executor.execute(() -> {
            environmentDao.insert(snapshot);
            if (onComplete != null) onComplete.run();
        });
    }

    // ---- Calibration ----

    public void saveCalibration(Calibration cal, Runnable onComplete) {
        executor.execute(() -> {
            calibrationDao.deactivateAll();
            cal.isActive = true;
            calibrationDao.insert(cal);
            if (onComplete != null) onComplete.run();
        });
    }

    public void getActiveCalibration(Callback<Calibration> callback) {
        executor.execute(() -> callback.onResult(calibrationDao.getActive()));
    }

    public void getAllCalibrations(Callback<List<Calibration>> callback) {
        executor.execute(() -> callback.onResult(calibrationDao.getAll()));
    }

    // ---- Preferences ----

    public String getGeminiApiKey() {
        return prefs.getString("gemini_api_key", "");
    }

    public void setGeminiApiKey(String key) {
        prefs.edit().putString("gemini_api_key", key).apply();
    }

    public long getLastAlertTime() {
        return prefs.getLong("last_alert_time", 0);
    }

    public void setLastAlertTime(long time) {
        prefs.edit().putLong("last_alert_time", time).apply();
    }

    // ---- Inner types ----

    public interface Callback<T> {
        void onResult(T result);
    }

    public static class ReadingStats {
        public float avgSys, avgDia, avgHR, avgHRV, avgSpO2;
        public int alertCount, warnCount, totalCount;
    }
}
