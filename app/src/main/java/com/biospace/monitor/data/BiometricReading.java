package com.biospace.monitor.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "biometric_readings")
public class BiometricReading {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long timestamp;       // System.currentTimeMillis()
    public boolean isManual;

    // Blood pressure
    public int systolic;
    public int diastolic;

    // Cardiac
    public int heartRate;
    public int hrv;              // SDNN ms
    public int hrvRmssd;         // rMSSD ms
    public int restingHeartRate;
    public int peakHeartRate;

    // Vitals
    public float spo2;           // %
    public float bodyTemp;       // °F
    public float skinTempDelta;  // °F delta
    public int respiratoryRate;  // breaths/min

    // Activity
    public int stressScore;      // 0–100
    public int steps;
    public int calories;
    public float sleepHours;
    public String activityLevel; // Resting, Light, Moderate, Active

    // Status
    public String status;        // ok, warn, alert
    public String issues;        // comma-separated list

    // Notes
    public String notes;

    // Calibration flag
    public boolean calibrationApplied;
    public int calOffsetSys;
    public int calOffsetDia;
}
