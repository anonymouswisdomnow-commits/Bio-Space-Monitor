package com.biospace.monitor;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class SpaceFragment extends Fragment {
    private TextView tvKp, tvBz, tvBzDir, tvWindSpeed, tvWindDensity, tvSolarFlux, tvSummary, tvUpdated;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_space, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tvKp = view.findViewById(R.id.tv_kp);
        tvBz = view.findViewById(R.id.tv_bz);
        tvBzDir = view.findViewById(R.id.tv_bz_direction);
        tvWindSpeed = view.findViewById(R.id.tv_wind_speed);
        tvWindDensity = view.findViewById(R.id.tv_wind_density);
        tvSolarFlux = view.findViewById(R.id.tv_solar_flux);
        tvSummary = view.findViewById(R.id.tv_space_summary);
        tvUpdated = view.findViewById(R.id.tv_space_updated);

        fetchData();
    }

    private void fetchData() {
        tvSummary.setText("Fetching NOAA space weather data...");
        new SpaceWeatherService().fetch(new SpaceWeatherService.Callback() {
            @Override
            public void onData(SpaceWeatherData data) {
                if (getContext() == null) return;
                tvKp.setText(data.kpIndex >= 0 ? String.format("%.1f", data.kpIndex) : "--");
                tvBz.setText(String.format("%.1f", data.bzComponent));
                tvBzDir.setText(data.bzComponent < 0 ? "nT ▼ SOUTH" : "nT ▲ NORTH");
                tvBzDir.setTextColor(data.bzComponent < -5 ?
                    requireContext().getColor(R.color.colorAlert) :
                    requireContext().getColor(R.color.colorOk));
                tvWindSpeed.setText(String.format("%.0f", data.solarWindSpeed));
                tvWindDensity.setText(String.format("%.1f", data.solarWindDensity));
                tvSolarFlux.setText(String.format("%.0f", data.solarFlux));
                tvSummary.setText(data.ansRiskSummary);

                int color;
                switch (data.ansRiskLevel) {
                    case "severe": color = requireContext().getColor(R.color.colorAlert); break;
                    case "high":   color = requireContext().getColor(R.color.colorAlert); break;
                    case "moderate": color = requireContext().getColor(R.color.colorWarn); break;
                    default: color = requireContext().getColor(R.color.colorOk);
                }
                tvSummary.setTextColor(color);
                tvUpdated.setText("Source: NOAA Space Weather Prediction Center • Live data");
            }

            @Override
            public void onError(String msg) {
                if (getContext() == null) return;
                tvSummary.setText("Could not fetch space weather data. Check network connection.");
            }
        });
    }
}
