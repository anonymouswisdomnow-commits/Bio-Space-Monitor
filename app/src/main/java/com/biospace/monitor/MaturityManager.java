package com.biospace.monitor;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class MaturityManager {
    public int getAppAgeInDays(Context context) {
        BioDatabase helper = new BioDatabase(context);
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT install_date FROM metadata", null);
        if (cursor.moveToFirst()) {
            long installTime = cursor.getLong(0);
            long diff = System.currentTimeMillis() - installTime;
            return (int) (diff / (1000 * 60 * 60 * 24));
        }
        return 0;
    }

    public void updateUI(int days, android.widget.RadioButton r30, android.widget.RadioButton r60, android.widget.RadioButton r90) {
        r30.setVisibility(days >= 30 ? android.view.View.VISIBLE : android.view.View.GONE);
        r60.setVisibility(days >= 60 ? android.view.View.VISIBLE : android.view.View.GONE);
        r90.setVisibility(days >= 90 ? android.view.View.VISIBLE : android.view.View.GONE);
    }
}
