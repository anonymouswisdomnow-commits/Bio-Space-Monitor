package com.biospace.monitor;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LogFragment extends Fragment {
    private RecyclerView recycler;
    private ReadingDatabase db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_log, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recycler = view.findViewById(R.id.recycler_log);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        db = ReadingDatabase.get(requireContext());

        MaterialButton btnClear = view.findViewById(R.id.btn_clear_log);
        btnClear.setOnClickListener(v -> {
            new Thread(() -> {
                db.clearAll();
                requireActivity().runOnUiThread(this::loadData);
            }).start();
        });

        loadData();
    }

    private void loadData() {
        new Thread(() -> {
            List<BiometricReading> readings = db.getRecent(100);
            requireActivity().runOnUiThread(() -> {
                recycler.setAdapter(new LogAdapter(readings));
            });
        }).start();
    }

    static class LogAdapter extends RecyclerView.Adapter<LogAdapter.VH> {
        private final List<BiometricReading> data;
        private final SimpleDateFormat sdf = new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault());

        LogAdapter(List<BiometricReading> data) { this.data = data; }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_log, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            BiometricReading r = data.get(pos);
            h.tvBp.setText(r.systolic + "/" + r.diastolic + " mmHg");
            h.tvHr.setText("HR: " + r.heartRate + " | SpO2: " + String.format("%.0f", r.spo2) + "%");
            h.tvTime.setText(sdf.format(new Date(r.timestamp)));
            if (r.kpAtReading >= 0) {
                String spaceInfo = "Kp=" + String.format("%.1f", r.kpAtReading) +
                    " Bz=" + String.format("%.1f", r.bzAtReading) + "nT";
                h.tvSpace.setText(spaceInfo);
                h.tvSpace.setVisibility(View.VISIBLE);
            } else {
                h.tvSpace.setVisibility(View.GONE);
            }
            int statusColor;
            switch (r.statusColor()) {
                case "alert": statusColor = h.tvStatus.getContext().getColor(R.color.colorAlert); break;
                case "warn": statusColor = h.tvStatus.getContext().getColor(R.color.colorWarn); break;
                default: statusColor = h.tvStatus.getContext().getColor(R.color.colorOk);
            }
            h.tvStatus.setTextColor(statusColor);
        }

        @Override public int getItemCount() { return data.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvBp, tvHr, tvTime, tvStatus, tvSpace;
            VH(View v) {
                super(v);
                tvBp = v.findViewById(R.id.tv_log_bp);
                tvHr = v.findViewById(R.id.tv_log_hr);
                tvTime = v.findViewById(R.id.tv_log_time);
                tvStatus = v.findViewById(R.id.tv_log_status);
                tvSpace = v.findViewById(R.id.tv_log_space);
            }
        }
    }
}
