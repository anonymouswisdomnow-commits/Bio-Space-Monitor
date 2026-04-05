package com.biospace.monitor;

import java.util.ArrayList;
import java.util.List;

public class BioSpaceBrain {
    private List<Double> bzHistory = new ArrayList<>();

    public int getRequiredInterval(double bz, double windSpeed) {
        // High Activity Thresholds: Bz < -5.0 or Wind > 500 km/s
        if (bz < -5.0 || windSpeed > 500) {
            return 5; // 5-minute high-resolution mode
        }
        return 15; // 15-minute standard mode
    }

    // ... existing analysis logic ...
}
