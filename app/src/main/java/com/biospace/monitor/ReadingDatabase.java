package com.biospace.monitor;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class ReadingDatabase extends SQLiteOpenHelper {
    private static ReadingDatabase instance;

    public static synchronized ReadingDatabase get(Context ctx) {
        if (instance == null) instance = new ReadingDatabase(ctx.getApplicationContext());
        return instance;
    }

    private ReadingDatabase(Context ctx) {
        super(ctx, "biospace.db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE readings (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "timestamp INTEGER," +
            "systolic INTEGER," +
            "diastolic INTEGER," +
            "heart_rate INTEGER," +
            "spo2 REAL," +
            "steps INTEGER," +
            "calories INTEGER," +
            "sleep_hours REAL," +
            "body_temp REAL," +
            "kp_index REAL," +
            "bz_component REAL," +
            "space_risk TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS readings");
        onCreate(db);
    }

    public void save(BiometricReading r) {
        ContentValues cv = new ContentValues();
        cv.put("timestamp", r.timestamp);
        cv.put("systolic", r.systolic);
        cv.put("diastolic", r.diastolic);
        cv.put("heart_rate", r.heartRate);
        cv.put("spo2", r.spo2);
        cv.put("steps", r.steps);
        cv.put("calories", r.calories);
        cv.put("sleep_hours", r.sleepHours);
        cv.put("body_temp", r.bodyTemp);
        cv.put("kp_index", r.kpAtReading);
        cv.put("bz_component", r.bzAtReading);
        cv.put("space_risk", r.spaceRiskAtReading);
        getWritableDatabase().insert("readings", null, cv);
    }

    public List<BiometricReading> getRecent(int limit) {
        List<BiometricReading> list = new ArrayList<>();
        Cursor c = getReadableDatabase().query("readings", null, null, null, null, null,
            "timestamp DESC", String.valueOf(limit));
        while (c.moveToNext()) {
            BiometricReading r = new BiometricReading();
            r.timestamp = c.getLong(c.getColumnIndexOrThrow("timestamp"));
            r.systolic = c.getInt(c.getColumnIndexOrThrow("systolic"));
            r.diastolic = c.getInt(c.getColumnIndexOrThrow("diastolic"));
            r.heartRate = c.getInt(c.getColumnIndexOrThrow("heart_rate"));
            r.spo2 = c.getFloat(c.getColumnIndexOrThrow("spo2"));
            r.steps = c.getInt(c.getColumnIndexOrThrow("steps"));
            r.calories = c.getInt(c.getColumnIndexOrThrow("calories"));
            r.sleepHours = c.getFloat(c.getColumnIndexOrThrow("sleep_hours"));
            r.bodyTemp = c.getFloat(c.getColumnIndexOrThrow("body_temp"));
            r.kpAtReading = c.getFloat(c.getColumnIndexOrThrow("kp_index"));
            r.bzAtReading = c.getFloat(c.getColumnIndexOrThrow("bz_component"));
            r.spaceRiskAtReading = c.getString(c.getColumnIndexOrThrow("space_risk"));
            list.add(r);
        }
        c.close();
        return list;
    }

    public void clearAll() {
        getWritableDatabase().delete("readings", null, null);
    }
}
