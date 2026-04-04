package com.biospace.monitor.ui.log;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.biospace.monitor.R;
import com.biospace.monitor.data.BiometricReading;
import com.biospace.monitor.data.BiometricRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LogFragment extends Fragment {

    private BiometricRepository repository;
    private RecyclerView recyclerView;
    private LogAdapter adapter;
    private TextView tvCount;
    private Spinner spinnerFilter;
    private String currentFilter = "all";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_log, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = BiometricRepository.getInstance(requireContext());

        recyclerView = view.findViewById(R.id.recycler_log);
        tvCount = view.findViewById(R.id.tv_log_count);
        spinnerFilter = view.findViewById(R.id.spinner_filter);
        MaterialButton btnExport = view.findViewById(R.id.btn_export);
        MaterialButton btnClear  = view.findViewById(R.id.btn_clear);

        adapter = new LogAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        ArrayAdapter<String> filterAdapter = new ArrayAdapter<>(requireContext(),
            android.R.layout.simple_spinner_item,
            new String[]{"All Entries", "Alerts Only", "Warnings Only", "Normal Only"});
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilter.setAdapter(filterAdapter);
        spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                currentFilter = pos == 1 ? "alert" : pos == 2 ? "warn" : pos == 3 ? "ok" : "all";
                loadData();
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        btnExport.setOnClickListener(v -> exportCsv());
        btnClear.setOnClickListener(v -> confirmClear());

        repository.getAllReadingsLive().observe(getViewLifecycleOwner(), readings -> {
            if (readings != null) filterAndShow(readings);
        });
    }

    private void loadData() {
        repository.getAllReadingsLive().observe(getViewLifecycleOwner(), readings -> {
            if (readings != null) filterAndShow(readings);
        });
    }

    private void filterAndShow(List<BiometricReading> all) {
        List<BiometricReading> filtered = new ArrayList<>();
        for (BiometricReading r : all) {
            if ("all".equals(currentFilter) || currentFilter.equals(r.status))
                filtered.add(r);
        }
        adapter.setData(filtered);
        tvCount.setText(filtered.size() + " entries");
    }

    private void confirmClear() {
        new AlertDialog.Builder(requireContext())
            .setTitle("Clear Log")
            .setMessage("Delete all biometric log entries? This cannot be undone.")
            .setPositiveButton("Delete", (d, w) ->
                repository.clearAllReadings(() ->
                    requireActivity().runOnUiThread(() -> adapter.setData(new ArrayList<>()))))
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void exportCsv() {
        repository.getRecentReadings(500, readings -> {
            try {
                File dir = requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
                if (dir == null) dir = requireContext().getFilesDir();
                String name = "biospace_" + new SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(new Date()) + ".csv";
                File file = new File(dir, name);
                FileWriter fw = new FileWriter(file);
                fw.write("Timestamp,Systolic,Diastolic,HR,HRV,SpO2,Temp,RR,Stress,Steps,Status,Issues\n");
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
                for (BiometricReading r : readings) {
                    fw.write(String.format("%s,%d,%d,%d,%d,%.1f,%.1f,%d,%d,%d,%s,\"%s\"\n",
                        sdf.format(new Date(r.timestamp)),
                        r.systolic, r.diastolic, r.heartRate, r.hrv,
                        r.spo2, r.bodyTemp, r.respiratoryRate, r.stressScore,
                        r.steps, r.status, r.issues != null ? r.issues : ""));
                }
                fw.close();
                Uri uri = FileProvider.getUriForFile(requireContext(),
                    requireContext().getPackageName() + ".provider", file);
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/csv");
                intent.putExtra(Intent.EXTRA_STREAM, uri);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(intent, "Export CSV"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    // ---- Adapter ----

    static class LogAdapter extends RecyclerView.Adapter<LogAdapter.VH> {
        private List<BiometricReading> data = new ArrayList<>();
        private final SimpleDateFormat sdf = new SimpleDateFormat("MM/dd HH:mm", Locale.US);

        void setData(List<BiometricReading> list) {
            data = list;
            notifyDataSetChanged();
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_log_entry, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            BiometricReading r = data.get(pos);
            h.tvTime.setText(sdf.format(new Date(r.timestamp)) + (r.isManual ? " · Manual" : ""));
            h.tvBp.setText(r.systolic + "/" + r.diastolic);
            h.tvHr.setText(r.heartRate + " bpm");
            h.tvSpo2.setText(String.format("%.0f%%", r.spo2));
            h.tvHrv.setText(r.hrv + " ms HRV");
            h.tvStatus.setText(r.status != null ? r.status.toUpperCase() : "OK");
            h.tvIssues.setText(r.issues != null && !r.issues.isEmpty() ? "⚠ " + r.issues : "");
            h.tvIssues.setVisibility(r.issues != null && !r.issues.isEmpty() ? View.VISIBLE : View.GONE);

            int strokeColor;
            if ("alert".equals(r.status)) strokeColor = 0xFFEF4444;
            else if ("warn".equals(r.status)) strokeColor = 0xFFF59E0B;
            else strokeColor = 0xFF10B981;
            h.card.setStrokeColor(strokeColor);
        }

        @Override public int getItemCount() { return data.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvTime, tvBp, tvHr, tvSpo2, tvHrv, tvStatus, tvIssues;
            MaterialCardView card;
            VH(View v) {
                super(v);
                card    = v.findViewById(R.id.card_log_item);
                tvTime  = v.findViewById(R.id.tv_log_time);
                tvBp    = v.findViewById(R.id.tv_log_bp);
                tvHr    = v.findViewById(R.id.tv_log_hr);
                tvSpo2  = v.findViewById(R.id.tv_log_spo2);
                tvHrv   = v.findViewById(R.id.tv_log_hrv);
                tvStatus = v.findViewById(R.id.tv_log_status);
                tvIssues = v.findViewById(R.id.tv_log_issues);
            }
        }
    }
}
