package com.biospace.monitor.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface BiometricDao {

    @Insert
    long insert(BiometricReading reading);

    @Query("SELECT * FROM biometric_readings ORDER BY timestamp DESC")
    LiveData<List<BiometricReading>> getAllLive();

    @Query("SELECT * FROM biometric_readings ORDER BY timestamp DESC LIMIT :limit")
    List<BiometricReading> getRecent(int limit);

    @Query("SELECT * FROM biometric_readings WHERE timestamp >= :since ORDER BY timestamp DESC")
    List<BiometricReading> getSince(long since);

    @Query("SELECT * FROM biometric_readings WHERE status = :status ORDER BY timestamp DESC")
    List<BiometricReading> getByStatus(String status);

    @Query("SELECT COUNT(*) FROM biometric_readings")
    int getCount();

    @Query("SELECT AVG(systolic) FROM biometric_readings WHERE timestamp >= :since AND systolic > 0")
    float getAvgSystolic(long since);

    @Query("SELECT AVG(diastolic) FROM biometric_readings WHERE timestamp >= :since AND diastolic > 0")
    float getAvgDiastolic(long since);

    @Query("SELECT AVG(heartRate) FROM biometric_readings WHERE timestamp >= :since AND heartRate > 0")
    float getAvgHR(long since);

    @Query("SELECT AVG(hrv) FROM biometric_readings WHERE timestamp >= :since AND hrv > 0")
    float getAvgHRV(long since);

    @Query("SELECT AVG(spo2) FROM biometric_readings WHERE timestamp >= :since AND spo2 > 0")
    float getAvgSpO2(long since);

    @Query("SELECT COUNT(*) FROM biometric_readings WHERE status = 'alert' AND timestamp >= :since")
    int getAlertCount(long since);

    @Query("SELECT COUNT(*) FROM biometric_readings WHERE status = 'warn' AND timestamp >= :since")
    int getWarnCount(long since);

    @Query("DELETE FROM biometric_readings")
    void deleteAll();
}

