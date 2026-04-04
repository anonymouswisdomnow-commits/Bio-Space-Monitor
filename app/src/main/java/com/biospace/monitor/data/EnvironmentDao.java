package com.biospace.monitor.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface EnvironmentDao {

    @Insert
    long insert(EnvironmentSnapshot snapshot);

    @Query("SELECT * FROM environment_snapshots ORDER BY timestamp DESC")
    LiveData<List<EnvironmentSnapshot>> getAllLive();

    @Query("SELECT * FROM environment_snapshots ORDER BY timestamp DESC LIMIT :limit")
    List<EnvironmentSnapshot> getRecent(int limit);

    @Query("DELETE FROM environment_snapshots")
    void deleteAll();
}

@Dao
interface CalibrationDao {

    @Insert
    long insert(Calibration calibration);

    @Query("SELECT * FROM calibrations WHERE isActive = 1 ORDER BY timestamp DESC LIMIT 1")
    Calibration getActive();

    @Query("SELECT * FROM calibrations ORDER BY timestamp DESC")
    List<Calibration> getAll();

    @Query("UPDATE calibrations SET isActive = 0")
    void deactivateAll();

    @Update
    void update(Calibration calibration);
}
