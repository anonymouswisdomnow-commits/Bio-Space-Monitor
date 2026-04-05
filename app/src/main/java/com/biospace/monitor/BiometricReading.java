package com.biospace.monitor;

public class BiometricReading {
    public long timestamp;
    public int systolic;
    public int diastolic;
    public int heartRate;
    public float spo2;
    public int steps;
    public int calories;
    public float sleepHours;
    public float bodyTemp;
    // Space weather at time of reading
    public float kpAtReading = -1;
    public float bzAtReading = 0;
    public String spaceRiskAtReading = "unknown";

    public String bpCategory() {
        if (systolic >= 180 || diastolic >= 120) return "Hypertensive Crisis";
        if (systolic >= 140 || diastolic >= 90) return "Stage 2 Hypertension";
        if (systolic >= 130 || diastolic >= 80) return "Stage 1 Hypertension";
        if (systolic >= 120) return "Elevated";
        if (systolic < 90 || diastolic < 60) return "Hypotension";
        if (systolic == 0) return "Awaiting reading";
        return "Normal";
    }

    public String statusColor() {
        if (systolic >= 140 || diastolic >= 90) return "alert";
        if (systolic >= 130 || diastolic >= 80) return "warn";
        return "ok";
    }
}
