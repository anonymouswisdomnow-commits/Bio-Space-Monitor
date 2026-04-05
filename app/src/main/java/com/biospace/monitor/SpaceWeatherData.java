package com.biospace.monitor;

public class SpaceWeatherData {
    public float kpIndex = -1;
    public float bzComponent = 0;
    public float solarWindSpeed = 0;
    public float solarWindDensity = 0;
    public float solarFlux = 0;
    public String ansRiskLevel = "unknown"; // low, moderate, high, severe
    public String ansRiskSummary = "";

    public void evaluate() {
        int riskScore = 0;
        StringBuilder summary = new StringBuilder();

        if (kpIndex >= 5) {
            riskScore += 2;
            summary.append("• Kp=").append(String.format("%.1f", kpIndex))
                   .append(" — Elevated geomagnetic activity. Arrhythmia risk increased.\n");
        }
        if (bzComponent < -5) {
            riskScore += 3;
            summary.append("• Bz=").append(String.format("%.1f", bzComponent))
                   .append("nT (Southward) — Magnetic shield OPEN. Primary flare-up trigger active.\n");
        }
        if (solarWindSpeed > 500) {
            riskScore += 1;
            summary.append("• Wind Speed=").append(String.format("%.0f", solarWindSpeed))
                   .append("km/s — High-speed stream. Expect fatigue and elevated resting HR.\n");
        }
        if (solarWindDensity > 10) {
            riskScore += 2;
            summary.append("• Wind Density=").append(String.format("%.1f", solarWindDensity))
                   .append("p/cm³ — Increased pressure. Sympathetic dominance likely (anxiety, insomnia).\n");
        }

        if (riskScore == 0) {
            ansRiskLevel = "low";
            ansRiskSummary = "✅ Space weather conditions are calm. Minimal ANS impact expected.";
        } else if (riskScore <= 2) {
            ansRiskLevel = "moderate";
            ansRiskSummary = "⚡ Moderate space weather activity:\n" + summary;
        } else if (riskScore <= 4) {
            ansRiskLevel = "high";
            ansRiskSummary = "⚠ High space weather activity:\n" + summary;
        } else {
            ansRiskLevel = "severe";
            ansRiskSummary = "🔴 SEVERE space weather activity:\n" + summary +
                "\nRecommend rest, hydration, avoid strenuous activity.";
        }
    }
}
