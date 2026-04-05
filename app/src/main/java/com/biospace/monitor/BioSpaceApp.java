package com.biospace.monitor;

import android.app.Application;
import android.content.SharedPreferences;

public class BioSpaceApp extends Application {
    private static BioSpaceApp instance;
    private SharedPreferences prefs;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        prefs = getSharedPreferences("biospace", MODE_PRIVATE);
    }

    public static BioSpaceApp get() { return instance; }

    public String getGeminiKey() { return prefs.getString("gemini_key", ""); }
    public void setGeminiKey(String k) { prefs.edit().putString("gemini_key", k).apply(); }
}
