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

    @Query("SELECT * FROM calibration WHERE active = 1 LIMIT 1")
    Calibration getActive();

    @Query("SELECT * FROM calibration ORDER BY timestamp DESC")
    List<Calibration> getAll();

    @Query("UPDATE calibration SET active = 0")
    void deactivateAll();
}
