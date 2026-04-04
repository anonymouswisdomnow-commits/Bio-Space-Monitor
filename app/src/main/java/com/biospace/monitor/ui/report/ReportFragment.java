package com.biospace.monitor.ui.report;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.biospace.monitor.R;
import com.biospace.monitor.data.BiometricReading;
import com.biospace.monitor.data.BiometricRepository;
import com.biospace.monitor.data.EnvironmentSnapshot;
import com.google.android.material.button.MaterialButton;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReportFragment extends Fragment {

    private BiometricRepository repository;
    private EditText etName, etDob, etPhysician;
    private CheckBox cbEnv, cbAi, cbHrv;
    private TextView tvReportOutput;
    private MaterialButton btnGenerate, btnShare;
    private String reportText = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_report, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = BiometricRepository.getInstance(requireContext());

        etName      = view.findViewById(R.id.et_rpt_name);
        etDob       = view.findViewById(R.id.et_rpt_dob);
        etPhysician = view.findViewById(R.id.et_rpt_physician);
        cbEnv       = view.findViewById(R.id.cb_include_env);
        cbAi        = view.findViewById(R.id.cb_include_ai);
        cbHrv       = view.findViewById(R.id.cb_include_hrv);
        tvReportOutput = view.findViewById(R.id.tv_report_output);
        btnGenerate = view.findViewById(R.id.btn_generate_report);
        btnShare    = view.findViewById(R.id.btn_share_report);

        btnGenerate.setOnClickListener(v -> generateReport());
        btnShare.setOnClickListener(v -> shareReport());
        btnShare.setEnabled(false);
    }

    private void generateReport() {
        btnGenerate.setEnabled(false);
        tvReportOutput.setText("Generating...");

        long thirtyDaysAgo = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000;
        repository.getReadingsSince(thirtyDaysAgo, readings -> {
            repository.getActiveCalibration(cal -> {
                if (getActivity() == null) return;

                SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.US);
                String name      = etName.getText().toString().trim();
                String dob       = etDob.getText().toString().trim();
                String physician = etPhysician.getText().toString().trim();

                // Stats
                float totalSys = 0, totalDia = 0, totalHR = 0, totalHRV = 0, totalSpo2 = 0;
                int bpCount = 0, hrCount = 0, alerts = 0, warns = 0;
                int maxSys = 0, minSys = 999, maxHR = 0, minHR = 999;
                float minSpo2 = 100;

                for (BiometricReading r : readings) {
                    if (r.systolic > 0) {
                        totalSys += r.systolic; totalDia += r.diastolic; bpCount++;
                        if (r.systolic > maxSys) maxSys = r.systolic;
                        if (r.systolic < minSys) minSys = r.systolic;
                    }
                    if (r.heartRate > 0) {
                        totalHR += r.heartRate; hrCount++;
                        if (r.heartRate > maxHR) maxHR = r.heartRate;
                        if (r.heartRate < minHR) minHR = r.heartRate;
                    }
                    if (r.hrv > 0) totalHRV += r.hrv;
                    if (r.spo2 > 0) { totalSpo2 += r.spo2; if (r.spo2 < minSpo2) minSpo2 = r.spo2; }
                    if ("alert".equals(r.status)) alerts++;
                    else if ("warn".equals(r.status)) warns++;
                }

                float avgSys  = bpCount > 0 ? totalSys / bpCount : 0;
                float avgDia  = bpCount > 0 ? totalDia / bpCount : 0;
                float avgHR   = hrCount > 0 ? totalHR  / hrCount : 0;
                float avgHRV  = hrCount > 0 ? totalHRV / hrCount : 0;
                float avgSpo2 = hrCount > 0 ? totalSpo2 / hrCount : 0;

                String bpCat = avgSys >= 140 || avgDia >= 90 ? "Stage 2 Hypertension"
                    : avgSys >= 130 || avgDia >= 80 ? "Stage 1 Hypertension"
                    : avgSys >= 120 ? "Elevated" : avgSys > 0 ? "Normal" : "Insufficient Data";

                String calNote = cal != null
                    ? String.format("Calibration applied: Sys %+d, Dia %+d mmHg (ref cuff, %s)",
                        cal.offsetSystolic, cal.offsetDiastolic, sdf.format(new Date(cal.timestamp)))
                    : "No calibration applied — raw watch readings";

                StringBuilder sb = new StringBuilder();
                sb.append("═══════════════════════════════\n");
                sb.append("   BIOSPACE MONITOR REPORT\n");
                sb.append("═══════════════════════════════\n\n");
                sb.append("Patient: ").append(name.isEmpty() ? "Not provided" : name).append("\n");
                sb.append("DOB: ").append(dob.isEmpty() ? "Not provided" : dob).append("\n");
                sb.append("Physician: ").append(physician.isEmpty() ? "Not provided" : physician).append("\n");
                sb.append("Generated: ").append(sdf.format(new Date())).append("\n");
                sb.append("Period: Last 30 days (").append(readings.size()).append(" readings)\n\n");

                sb.append("─── BLOOD PRESSURE ───\n");
                if (bpCount > 0) {
                    sb.append(String.format("Average:  %d/%d mmHg\n", (int)avgSys, (int)avgDia));
                    sb.append(String.format("Range:    %d–%d mmHg (systolic)\n", minSys, maxSys));
                    sb.append("Category: ").append(bpCat).append("\n");
                } else {
                    sb.append("Insufficient data\n");
                }
                sb.append("Note: ").append(calNote).append("\n\n");

                sb.append("─── CARDIAC ───\n");
                if (hrCount > 0) {
                    sb.append(String.format("Avg HR:   %d bpm (range %d–%d)\n", (int)avgHR, minHR, maxHR));
                    sb.append(String.format("Avg SpO2: %.0f%% (min %.0f%%)\n", avgSpo2, minSpo2));
                    if (cbHrv.isChecked())
                        sb.append(String.format("Avg HRV:  %d ms SDNN\n", (int)avgHRV));
                } else {
                    sb.append("Insufficient data\n");
                }
                sb.append("\n");

                sb.append("─── ALERT SUMMARY ───\n");
                int total = readings.size();
                sb.append(String.format("Total readings: %d\n", total));
                sb.append(String.format("Critical alerts: %d (%.0f%%)\n", alerts, total > 0 ? alerts * 100f / total : 0));
                sb.append(String.format("Warnings: %d (%.0f%%)\n", warns, total > 0 ? warns * 100f / total : 0));
                sb.append(String.format("Normal: %d (%.0f%%)\n\n", total - alerts - warns,
                    total > 0 ? (total - alerts - warns) * 100f / total : 0));

                sb.append("─── RECENT READINGS (last 10) ───\n");
                for (BiometricReading r : readings.subList(0, Math.min(10, readings.size()))) {
                    sb.append(String.format("%s  BP:%d/%d  HR:%d  SpO2:%.0f%%  [%s]\n",
                        new SimpleDateFormat("MM/dd HH:mm", Locale.US).format(new Date(r.timestamp)),
                        r.systolic, r.diastolic, r.heartRate, r.spo2,
                        r.status != null ? r.status.toUpperCase() : "OK"));
                }

                sb.append("\n─── DISCLAIMER ───\n");
                sb.append("Generated from consumer wearable device data.\n");
                sb.append("Not a clinical diagnosis. All decisions should\n");
                sb.append("be made by a qualified healthcare professional.\n");

                reportText = sb.toString();

                getActivity().runOnUiThread(() -> {
                    tvReportOutput.setText(reportText);
                    btnGenerate.setEnabled(true);
                    btnShare.setEnabled(true);
                });
            });
        });
    }

    private void shareReport() {
        if (reportText.isEmpty()) return;
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, "BioSpace Monitor Health Report");
        intent.putExtra(Intent.EXTRA_TEXT, reportText);
        startActivity(Intent.createChooser(intent, "Share Report"));
    }
}
