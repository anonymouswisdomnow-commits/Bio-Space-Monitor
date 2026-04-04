package com.biospace.monitor.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "environment_snapshots")
public class EnvironmentSnapshot {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long timestamp;

    public float baroPressure;   // mmHg
    public float temperature;    // °F
    public float humidity;       // %
    public float kpIndex;        // 0–9
    public float solarFlux;      // sfu
    public float windSpeed;      // mph

    public String city;
    public String weatherDesc;

    public String notes;

    public long linkedReadingId; // FK to biometric_readings
}
