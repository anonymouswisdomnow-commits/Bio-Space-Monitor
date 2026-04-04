package com.biospace.monitor;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import androidx.room.Room;
import com.biospace.monitor.data.AppDatabase;

public class BioSpaceApp extends Application {

    public static final String CHANNEL_ALERTS = "biospace_alerts";
    public static final String CHANNEL_MONITOR = "biospace_monitor";

    private static AppDatabase database;

    @Override
    public void onCreate() {
        super.onCreate();
        database = Room.databaseBuilder(getApplicationContext(),
                AppDatabase.class, "biospace.db")
                .fallbackToDestructiveMigration()
                .build();
        createNotificationChannels();
    }

    public static AppDatabase getDatabase() {
        return database;
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = getSystemService(NotificationManager.class);

            NotificationChannel alerts = new NotificationChannel(
                    CHANNEL_ALERTS, "Biometric Alerts",
                    NotificationManager.IMPORTANCE_HIGH);
            alerts.setDescription("Alerts for abnormal biometric readings");
            nm.createNotificationChannel(alerts);

            NotificationChannel monitor = new NotificationChannel(
                    CHANNEL_MONITOR, "Live Monitoring",
                    NotificationManager.IMPORTANCE_LOW);
            monitor.setDescription("Background monitoring status");
            nm.createNotificationChannel(monitor);
        }
    }
}
