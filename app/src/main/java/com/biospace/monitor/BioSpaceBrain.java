package com.biospace.monitor;

import java.util.ArrayList;
import java.util.List;

public class BioSpaceBrain {
    private List<Double> bzHistory = new ArrayList<>();
    private static final int MAX_HISTORY = 10;

    public String analyzeSystemInstability(double currentBz, double windSpeed) {
        bzHistory.add(currentBz);
        if (bzHistory.size() > MAX_HISTORY) bzHistory.remove(0);

        if (bzHistory.size() < 2) return "Stabilizing...";

        // Calculate Rate of Change (Delta)
        double delta = Math.abs(currentBz - bzHistory.get(bzHistory.size() - 2));
        
        // Calculate Variance (How much is it jumping around?)
        double variance = 0;
        for (double val : bzHistory) variance += Math.abs(val - currentBz);
        variance /= bzHistory.size();

        // THE INSTABILITY FLAG
        if (variance > 5.0 && windSpeed > 500) {
            return "⚠️ SYSTEM INSTABILITY: Rapid Bz Oscillations Detected. High ANS stress risk.";
        }
        
        if (delta > 10.0) {
            return "⚡ POLARITY FLIP: Sudden North/South shift. Check for heart rate spikes.";
        }

        return "Magnetic Field: Stable.";
    }
}
