package com.biospace.monitor;

import java.util.Random;

public class EnvironmentalScraper {
    // In a full build, these use OkHttp to pull from NOAA and OpenWeather
    // For now, we establish the data structure the Brain needs
    public static class EnvData {
        public double kp = 0;
        public double bz = 0;
        public double windSpeed = 0;
        public double baroPressure = 1013.25; // Standard hPa
        public double schumann = 7.83; // Hz
        public double hemiPower = 10.0; // GW
    }

    public EnvData getCurrentMetrics() {
        EnvData data = new EnvData();
        // This is where the real API calls hook in
        return data;
    }
}
