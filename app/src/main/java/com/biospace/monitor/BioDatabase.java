package com.biospace.monitor;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class BioDatabase extends SQLiteOpenHelper {
    public BioDatabase(Context context) {
        super(context, "BioSpace.db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Table for combined Space and Health logs
        db.execSQL("CREATE TABLE logs (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "hr INTEGER, bp_sys INTEGER, bp_dia INTEGER, " +
                "bz REAL, wind_speed REAL, kp_index REAL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {}
}
