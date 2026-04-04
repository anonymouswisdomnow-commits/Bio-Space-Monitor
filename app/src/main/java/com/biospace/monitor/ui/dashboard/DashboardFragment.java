package com.biospace.monitor.ui.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import com.biospace.monitor.MainActivity;
import com.biospace.monitor.R;
import com.biospace.monitor.data.BiometricReading;
import com.biospace.monitor.data.BiometricRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import java.util.List;

public class DashboardFragment extends Fragment {

    private BiometricRepository repository;

    // BP card
    private TextView tvBpDisplay, tvBpStatus, tvBpCategory;
    private MaterialCardView cardBp;

    // Metric tiles
    private TextView tvHR, tvHRV, tvSpO2, tvTemp, tvRR, tvStress;
    private TextView tvSteps, tvCalories, tvSleep;
    private MaterialCardView tileHR, tileHRV, tileSpO2, tileTemp, tileRR, tileStress;

    // Detail rows
    private TextView tvRestingHR, tvPeakHR, tvRmssd, tvSkinDelta, tvActivity, tvSpO2b;

    // Status
    private TextView tvCalBadge, tvLastUpdate;
    private MaterialButton btnRefresh;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = BiometricRepository.getInstance(requireContext());
        bindViews(view);

        btnRefresh.setOnClickListener(v -> {
            BiometricReading r = ((MainActivity) requireActivity()).takeReadingNow();
            if (r != null) updateUI(r);
        });

        // Observe latest reading from DB
        repository.getAllReadingsLive().observe(getViewLifecycleOwner(), readings -> {
            if (readings != null && !readings.isEmpty()) {
                updateUI(readings.get(0));
            }
        });
    }

    private void bindViews(View v) {
        tvBpDisplay    = v.findViewById(R.id.tv_bp_display);
        tvBpStatus     = v.findViewById(R.id.tv_bp_status);
        tvBpCategory   = v.findViewById(R.id.tv_bp_category);
        cardBp         = v.findViewById(R.id.card_bp);
        tvCalBadge     = v.findViewById(R.id.tv_cal_badge);
        tvLastUpdate   = v.findViewById(R.id.tv_last_update);
        btnRefresh     = v.findViewById(R.id.btn_refresh);

        tvHR     = v.findViewById(R.id.tv_hr);
        tvHRV    = v.findViewById(R.id.tv_hrv);
        tvSpO2   = v.findViewById(R.id.tv_spo2);
        tvTemp   = v.findViewById(R.id.tv_temp);
        tvRR     = v.findViewById(R.id.tv_rr);
        tvStress = v.findViewById(R.id.tv_stress);
        tvSteps    = v.findViewById(R.id.tv_steps);
        tvCalories = v.findViewById(R.id.tv_calories);
        tvSleep    = v.findViewById(R.id.tv_sleep);

        tileHR     = v.findViewById(R.id.tile_hr);
        tileHRV    = v.findViewById(R.id.tile_hrv);
        tileSpO2   = v.findViewById(R.id.tile_spo2);
        tileTemp   = v.findViewById(R.id.tile_temp);
        tileRR     = v.findViewById(R.id.tile_rr);
        tileStress = v.findViewById(R.id.tile_stress);

        tvRestingHR  = v.findViewById(R.id.tv_resting_hr);
        tvPeakHR     = v.findViewById(R.id.tv_peak_hr);
        tvRmssd      = v.findViewById(R.id.tv_rmssd);
        tvSkinDelta  = v.findViewById(R.id.tv_skin_delta);
        tvActivity   = v.findViewById(R.id.tv_activity);
        tvSpO2b      = v.findViewById(R.id.tv_spo2b);
    }

    private void updateUI(BiometricReading r) {
        if (getContext() == null) return;

        // BP card
        tvBpDisplay.setText(r.systolic + "/" + r.diastolic);
        tvBpStatus.setText(r.systolic + " / " + r.diastolic + " mmHg");
        tvBpCategory.setText(bpCategory(r.systolic, r.diastolic));

        int bpColor = statusColor(r.status);
        cardBp.setStrokeColor(bpColor);

        // Calibration badge
        tvCalBadge.setVisibility(r.calibrationApplied ? View.VISIBLE : View.GONE);

        // Tiles
        setTile(tileHR, tvHR, r.heartRate + "", r.heartRate > 100 || r.heartRate < 50 ? "alert"
            : r.heartRate > 90 || r.heartRate < 55 ? "warn" : "ok");
        setTile(tileHRV, tvHRV, r.hrv + " ms", r.hrv < 20 ? "alert" : r.hrv < 30 ? "warn" : "ok");
        setTile(tileSpO2, tvSpO2, String.format("%.0f%%", r.spo2),
            r.spo2 < 92 ? "alert" : r.spo2 < 95 ? "warn" : "ok");
        setTile(tileTemp, tvTemp, String.format("%.1f°F", r.bodyTemp),
            r.bodyTemp > 100.4 ? "alert" : r.bodyTemp > 99.5 ? "warn" : "ok");
        setTile(tileRR, tvRR, r.respiratoryRate + "", r.respiratoryRate > 25 || r.respiratoryRate < 8 ? "alert"
            : r.respiratoryRate > 20 || r.respiratoryRate < 10 ? "warn" : "ok");
        setTile(tileStress, tvStress, r.stressScore + "",
            r.stressScore > 80 ? "alert" : r.stressScore > 60 ? "warn" : "ok");

        tvSteps.setText(String.format("%,d", r.steps));
        tvCalories.setText(r.calories + "");
        tvSleep.setText(String.format("%.1f h", r.sleepHours));

        // Detail rows
        tvRestingHR.setText(r.restingHeartRate + " bpm");
        tvPeakHR.setText(r.peakHeartRate + " bpm");
        tvRmssd.setText(r.hrvRmssd + " ms");
        tvSkinDelta.setText(String.format("%+.1f°F", r.skinTempDelta));
        tvActivity.setText(r.activityLevel != null ? r.activityLevel : "--");
        tvSpO2b.setText(String.format("%.0f%%", r.spo2));

        tvLastUpdate.setText("Updated " + android.text.format.DateFormat.format("h:mm a", r.timestamp));
    }

    private void setTile(MaterialCardView tile, TextView tv, String value, String status) {
        if (tile == null || tv == null) return;
        tv.setText(value);
        tile.setStrokeColor(statusColor(status));
        int bg = statusBg(status);
        tile.setCardBackgroundColor(bg);
    }

    private int statusColor(String status) {
        if ("alert".equals(status)) return requireContext().getColor(R.color.colorAlert);
        if ("warn".equals(status))  return requireContext().getColor(R.color.colorWarn);
        return requireContext().getColor(R.color.colorOk);
    }

    private int statusBg(String status) {
        if ("alert".equals(status)) return 0x14EF4444;
        if ("warn".equals(status))  return 0x14F59E0B;
        return 0x0D10B981;
    }

    private String bpCategory(int sys, int dia) {
        if (sys >= 180 || dia >= 120) return "Hypertensive Crisis";
        if (sys >= 140 || dia >= 90)  return "Stage 2 Hypertension";
        if (sys >= 130 || dia >= 80)  return "Stage 1 Hypertension";
        if (sys >= 120)               return "Elevated";
        if (sys < 90 || dia < 60)     return "Hypotension";
        return "Normal";
    }
}
