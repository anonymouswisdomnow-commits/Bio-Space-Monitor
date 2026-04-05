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
        watch.startScheduledFetch(getApplicationContext(), "YOUR_WATCH_MAC_HERE");

        // 2. Determine next interval based on Space Weather
        // (In a full build, we'd pull the last known Bz from a database here)
        int nextInterval = 15; 
        
        // 3. Schedule the NEXT "Adaptive" check
        OneTimeWorkRequest nextWork = new OneTimeWorkRequest.Builder(SyncWorker.class)
                .setInitialDelay(nextInterval, TimeUnit.MINUTES)
                .build();
        
        WorkManager.getInstance(getApplicationContext()).enqueue(nextWork);

        return Result.success();
    }
}
