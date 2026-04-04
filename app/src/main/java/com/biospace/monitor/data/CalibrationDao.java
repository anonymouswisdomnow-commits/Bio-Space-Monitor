package com.biospace.monitor.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface CalibrationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Calibration calibration);

    @Query("SELECT * FROM calibrations WHERE isActive = 1 LIMIT 1")
    Calibration getActive();

    @Query("SELECT * FROM calibrations ORDER BY timestamp DESC")
    List<Calibration> getAll();

    @Query("UPDATE calibrations SET isActive = 0")
    void deactivateAll();
}
