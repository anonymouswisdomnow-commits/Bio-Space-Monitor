package com.biospace.monitor;

public class ReportEngine {
    public String buildAIPrompt(int type, String healthData, String spaceData) {
        String base = "Act as a specialist in Heliobiology and Dysautonomia. ";
        
        switch(type) {
            case 1: // General
                return base + "Analyze these vitals for standard cardiovascular trends: " + healthData;
            case 2: // Symptom Flare
                return base + "Correlate this patient's HR spikes with these magnetic field (Bz) shifts: " + spaceData + " | Vitals: " + healthData;
            case 3: // Prediction
                return base + "Based on the last 30 days of sensitivity to Kp-Index peaks, predict flare risk for the current solar forecast: " + spaceData;
            default:
                return base + "Provide a summary of current Bio-Space status.";
        }
    }
}
