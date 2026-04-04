package com.biospace.monitor.ui.calibrate;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.biospace.monitor.MainActivity;
import com.biospace.monitor.R;
import com.biospace.monitor.data.Calibration;
import com.biospace.monitor.data.BiometricRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class CalibrateFragment extends Fragment {

    private BiometricRepository repository;
    private int currentStep = 1;

    // Step 1
    private MaterialCardView cardStep1, cardStep2, cardStep3, cardStep4;

    // Step 2 inputs
    private EditText etRefSys, etRefDia, etRefPulse;

    // Step 3
    private TextView tvWatchRaw;
    private int rawSys, rawDia;
    private int refSys, refDia;

    // Step 4
    private TextView tvCalSummary;
    private TextView tvCalHistory;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_calibrate, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = BiometricRepository.getInstance(requireContext());

        cardStep1 = view.findViewById(R.id.card_cal_step1);
        cardStep2 = view.findViewById(R.id.card_cal_step2);
        cardStep3 = view.findViewById(R.id.card_cal_step3);
        cardStep4 = view.findViewById(R.id.card_cal_step4);

        etRefSys  = view.findViewById(R.id.et_ref_sys);
        etRefDia  = view.findViewById(R.id.et_ref_dia);
        etRefPulse = view.findViewById(R.id.et_ref_pulse);
        tvWatchRaw = view.findViewById(R.id.tv_watch_raw);
        tvCalSummary = view.findViewById(R.id.tv_cal_summary);
        tvCalHistory = view.findViewById(R.id.tv_cal_history);

        view.findViewById(R.id.btn_cal_step1_next).setOnClickListener(v -> goToStep(2));
        view.findViewById(R.id.btn_cal_step2_next).setOnClickListener(v -> captureRefValues());
        view.findViewById(R.id.btn_cal_step2_back).setOnClickListener(v -> goToStep(1));
        view.findViewById(R.id.btn_read_watch).setOnClickListener(v -> readWatch());
        view.findViewById(R.id.btn_cal_finish).setOnClickListener(v -> finishCalibration());
        view.findViewById(R.id.btn_cal_redo).setOnClickListener(v -> goToStep(1));

        goToStep(1);
        loadHistory();
    }

    private void goToStep(int step) {
        currentStep = step;
        cardStep1.setVisibility(step == 1 ? View.VISIBLE : View.GONE);
        cardStep2.setVisibility(step == 2 ? View.VISIBLE : View.GONE);
        cardStep3.setVisibility(step == 3 ? View.VISIBLE : View.GONE);
        cardStep4.setVisibility(step == 4 ? View.VISIBLE : View.GONE);
    }

    private void captureRefValues() {
        String sysStr = etRefSys.getText().toString().trim();
        String diaStr = etRefDia.getText().toString().trim();
        if (sysStr.isEmpty() || diaStr.isEmpty()) {
            Toast.makeText(requireContext(), "Enter both systolic and diastolic values", Toast.LENGTH_SHORT).show();
            return;
        }
        refSys = Integer.parseInt(sysStr);
        refDia = Integer.parseInt(diaStr);
        if (refSys < 60 || refSys > 250 || refDia < 40 || refDia > 150) {
            Toast.makeText(requireContext(), "Values out of physiological range", Toast.LENGTH_SHORT).show();
            return;
        }
        goToStep(3);
    }

    private void readWatch() {
        // Simulate watch raw reading (slightly offset from reference)
        Random rnd = new Random();
        rawSys = refSys + Math.round((rnd.nextFloat() - 0.5f) * 20f);
        rawDia = refDia + Math.round((rnd.nextFloat() - 0.5f) * 12f);
        tvWatchRaw.setText("Watch reading: " + rawSys + "/" + rawDia + " mmHg");

        int offSys = refSys - rawSys;
        int offDia = refDia - rawDia;
        tvCalSummary.setText(
            "Reference cuff: " + refSys + "/" + refDia + " mmHg\n" +
            "Watch raw: " + rawSys + "/" + rawDia + " mmHg\n" +
            "Calibration offset: " + (offSys >= 0 ? "+" : "") + offSys +
            " / " + (offDia >= 0 ? "+" : "") + offDia + " mmHg\n\n" +
            "All future readings will be corrected by this offset."
        );
        goToStep(4);
    }

    private void finishCalibration() {
        Calibration cal = new Calibration();
        cal.timestamp = System.currentTimeMillis();
        cal.refSystolic   = refSys;
        cal.refDiastolic  = refDia;
        cal.rawSystolic   = rawSys;
        cal.rawDiastolic  = rawDia;
        cal.offsetSystolic  = refSys - rawSys;
        cal.offsetDiastolic = refDia - rawDia;
        cal.isActive = true;

        repository.saveCalibration(cal, () -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    ((MainActivity) requireActivity()).updateCalibration(cal);
                    Toast.makeText(requireContext(), "Calibration saved!", Toast.LENGTH_SHORT).show();
                    loadHistory();
                    goToStep(1);
                });
            }
        });
    }

    private void loadHistory() {
        repository.getAllCalibrations(cals -> {
            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                if (cals.isEmpty()) {
                    tvCalHistory.setText("No calibrations recorded yet.");
                    return;
                }
                SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.US);
                StringBuilder sb = new StringBuilder();
                for (Calibration c : cals) {
                    sb.append(sdf.format(new Date(c.timestamp))).append("\n");
                    sb.append("  Ref: ").append(c.refSystolic).append("/").append(c.refDiastolic);
                    sb.append("  Raw: ").append(c.rawSystolic).append("/").append(c.rawDiastolic);
                    sb.append("  Offset: ").append(c.offsetSystolic >= 0 ? "+" : "").append(c.offsetSystolic);
                    sb.append("/").append(c.offsetDiastolic >= 0 ? "+" : "").append(c.offsetDiastolic);
                    if (c.isActive) sb.append("  ✓ Active");
                    sb.append("\n\n");
                }
                tvCalHistory.setText(sb.toString().trim());
            });
        });
    }
}
