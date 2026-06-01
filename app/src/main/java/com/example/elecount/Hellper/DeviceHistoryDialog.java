package com.example.elecount.Hellper;

import android.app.Dialog;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.elecount.R;
import com.example.elecount.adapters.UsageRecordAdapter;
import com.example.elecount.models.Device;
import com.example.elecount.models.UsageRecord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

public final class DeviceHistoryDialog {

    private DeviceHistoryDialog() {
    }

    public static void show(Fragment fragment, Device device, DataManager dataManager) {
        Dialog dialog = new Dialog(fragment.requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_device_history);

        TextView historyTitle = dialog.findViewById(R.id.historyTitle);
        TextView historyTotalText = dialog.findViewById(R.id.historyTotalText);
        TextView historyEmptyText = dialog.findViewById(R.id.historyEmptyText);
        RecyclerView historyRecycler = dialog.findViewById(R.id.historyRecycler);
        Button historyCloseButton = dialog.findViewById(R.id.historyCloseButton);

        historyTitle.setText(fragment.getString(R.string.device_history_title, device.getName()));
        historyRecycler.setLayoutManager(new LinearLayoutManager(fragment.requireContext()));
        historyRecycler.setAdapter(new UsageRecordAdapter(fragment.requireContext(), new ArrayList<>()));
        historyRecycler.setVisibility(android.view.View.GONE);

        historyCloseButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();

        dataManager.getUsageRecordsByDevice(device.getId(), new DataManager.DataCallback<ArrayList<UsageRecord>>() {
            @Override
            public void onSuccess(ArrayList<UsageRecord> result) {
                if (!fragment.isAdded()) {
                    return;
                }
                fragment.requireActivity().runOnUiThread(() -> {
                    ArrayList<UsageRecord> sorted = new ArrayList<>(result);
                    Collections.sort(sorted, new Comparator<UsageRecord>() {
                        @Override
                        public int compare(UsageRecord a, UsageRecord b) {
                            return Long.compare(b.getEndTime(), a.getEndTime());
                        }
                    });

                    double totalCost = 0;
                    for (UsageRecord record : sorted) {
                        totalCost += record.getCost();
                    }
                    historyTotalText.setText(fragment.getString(
                            R.string.device_history_total,
                            String.format(Locale.getDefault(), "%.2f شيكل", totalCost)));

                    if (sorted.isEmpty()) {
                        historyEmptyText.setVisibility(android.view.View.VISIBLE);
                        historyRecycler.setVisibility(android.view.View.GONE);
                    } else {
                        historyEmptyText.setVisibility(android.view.View.GONE);
                        historyRecycler.setVisibility(android.view.View.VISIBLE);
                        historyRecycler.setAdapter(new UsageRecordAdapter(fragment.requireContext(), sorted));
                    }
                });
            }

            @Override
            public void onError(String error) {
                if (!fragment.isAdded()) {
                    return;
                }
                fragment.requireActivity().runOnUiThread(() -> {
                    historyEmptyText.setVisibility(android.view.View.VISIBLE);
                    historyEmptyText.setText(error);
                    historyRecycler.setVisibility(android.view.View.GONE);
                });
            }
        });

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    (int) (fragment.getResources().getDisplayMetrics().widthPixels * 0.92),
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}
