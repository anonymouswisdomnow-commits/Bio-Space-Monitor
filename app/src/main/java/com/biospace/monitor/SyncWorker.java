package com.biospace.monitor;

import android.content.Context;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import java.util.concurrent.TimeUnit;

public class SyncWorker extends Worker {
    private BioSpaceBrain brain = new BioSpaceBrain();
    private WatchAutomation watch = new WatchAutomation();

    public SyncWorker(Context context, WorkerParameters params) {
        super(context, params);
    }

    @Override
    public Result doWork() {
        // 1. Trigger the Watch Sync
        watch.startScheduledFetch(getApplicationContext(), "C0:29:AB:60:4D:10");

        // 2. Adaptive Logic: Check the last known Space Weather
        // (If solar wind > 500 or Bz < -5, interval drops to 5 mins)
        int nextInterval = brain.getRequiredInterval(-6.0, 550.0); 
        
        // 3. Schedule the NEXT adaptive check
        OneTimeWorkRequest nextWork = new OneTimeWorkRequest.Builder(SyncWorker.class)
                .setInitialDelay(nextInterval, TimeUnit.MINUTES)
                .build();
        
        WorkManager.getInstance(getApplicationContext()).enqueue(nextWork);

        return Result.success();
    }
}
