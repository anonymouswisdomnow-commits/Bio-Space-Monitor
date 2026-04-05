package com.biospace.monitor;

import java.util.ArrayList;
import java.util.List;

public class BioSpaceBrain {
    private List<Double> bzHistory = new ArrayList<>();

    public String analyzeSevereEvents(double xRayFlux, double protonDensity, double windSpeed, double bz) {
        // 1. SOLAR FLARE LOGIC (X-Ray Flux)
        if (xRayFlux >= 0.0001) {
            return "💥 FLARE ALERT: M-Class or higher detected. Immediate EM interference risk.";
        }

        // 2. CME SHOCK LOGIC (Density + Speed)
        if (protonDensity > 20.0 && windSpeed > 600) {
            return "🌊 CME IMPACT: High-density plasma cloud detected. Sustained ANS pressure.";
        }

        // 3. COMBINED INSTABILITY
        if (Math.abs(bz) > 15.0 && windSpeed > 700) {
            return "🚨 SEVERE STORM: Magnetosphere under extreme compression.";
        }

        return "Solar Activity: Nominal.";
    }
}
