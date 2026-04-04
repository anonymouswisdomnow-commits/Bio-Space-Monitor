package com.biospace.monitor.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "calibrations")
public class Calibration {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long timestamp;

    public int refSystolic;
    public int refDiastolic;
    public int rawSystolic;
    public int rawDiastolic;

    public int offsetSystolic;   // ref - raw
    public int offsetDiastolic;

    public boolean isActive;     // only one active at a time
}
