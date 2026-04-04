package com.biospace.monitor.data;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(
    entities = {BiometricReading.class, EnvironmentSnapshot.class, Calibration.class},
    version = 1,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    public abstract BiometricDao biometricDao();
    public abstract EnvironmentDao environmentDao();
    public abstract CalibrationDao calibrationDao();
}
