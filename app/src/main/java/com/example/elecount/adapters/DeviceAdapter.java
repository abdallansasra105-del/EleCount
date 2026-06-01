package com.example.elecount.adapters;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.elecount.R;
import com.example.elecount.Hellper.DataManager;
import com.example.elecount.models.Device;
import com.example.elecount.models.UsageRecord;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * محول لعرض قائمة الأجهزة في RecyclerView
 */
public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder> {

    private static final long TICK_INTERVAL_MS = 1000L;

    private Context context;
    private ArrayList<Device> devices;
    private DataManager dataManager;
    private OnDeviceChangedListener onDeviceChangedListener;
    private OnDeviceEditListener onDeviceEditListener;
    private OnDeviceHistoryListener onDeviceHistoryListener;

    private final Handler tickHandler = new Handler(Looper.getMainLooper());
    private final Set<DeviceViewHolder> runningHolders = new HashSet<>();
    private final Map<String, Double> historicalCosts = new HashMap<>();
    private boolean tickerActive = false;

    private final Runnable tickRunnable = new Runnable() {
        @Override
        public void run() {
            for (DeviceViewHolder holder : new HashSet<>(runningHolders)) {
                holder.updateRunningDisplay();
            }
            if (tickerActive) {
                tickHandler.postDelayed(this, TICK_INTERVAL_MS);
            }
        }
    };

    public interface OnDeviceChangedListener {
        void onDeviceChanged();
    }

    public interface OnDeviceEditListener {
        void onEditDevice(Device device);
    }

    public interface OnDeviceHistoryListener {
        void onViewHistory(Device device);
    }

    public DeviceAdapter(Context context, ArrayList<Device> devices, DataManager dataManager) {
        this.context = context;
        this.devices = devices;
        this.dataManager = dataManager;
    }

    public void setOnDeviceChangedListener(OnDeviceChangedListener listener) {
        this.onDeviceChangedListener = listener;
    }

    public void setOnDeviceEditListener(OnDeviceEditListener listener) {
        this.onDeviceEditListener = listener;
    }

    public void setOnDeviceHistoryListener(OnDeviceHistoryListener listener) {
        this.onDeviceHistoryListener = listener;
    }

    /** تحديث التكلفة التراكمية من سجلات الاستخدام */
    public void setHistoricalCosts(Map<String, Double> costs) {
        historicalCosts.clear();
        if (costs != null) {
            historicalCosts.putAll(costs);
        }
        tickHandler.post(this::notifyDataSetChanged);
    }

    private double getHistoricalCost(Device device) {
        if (device == null || device.getId() == null) {
            return 0;
        }
        Double cost = historicalCosts.get(device.getId());
        return cost != null ? cost : 0;
    }

    private void updateTotalCostDisplay(Device device, TextView costView) {
        double total = getHistoricalCost(device);
        if (device.isRunning()) {
            total += device.getCurrentCost();
        }
        costView.setText(String.format(Locale.getDefault(), "%.2f شيكل", total));
    }

    /** بدء تحديث المؤقت والتكلفة كل ثانية */
    public void startLiveUpdates() {
        tickerActive = true;
        tickHandler.removeCallbacks(tickRunnable);
        tickHandler.post(tickRunnable);
    }

    /** إيقاف التحديث المباشر */
    public void stopLiveUpdates() {
        tickerActive = false;
        tickHandler.removeCallbacks(tickRunnable);
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_device, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        holder.bind(devices.get(position));
    }

    @Override
    public void onViewRecycled(@NonNull DeviceViewHolder holder) {
        runningHolders.remove(holder);
        holder.clearBinding();
        super.onViewRecycled(holder);
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    class DeviceViewHolder extends RecyclerView.ViewHolder {

        ImageView deviceImage;
        TextView deviceName;
        TextView deviceCategory;
        TextView devicePower;
        ImageView deviceStatus;
        LinearLayout runningInfoContainer;
        TextView deviceRunningTime;
        TextView deviceRunningTimeText;
        TextView deviceLiveCost;
        TextView deviceCostLabel;
        TextView deviceCost;
        Button toggleButton;
        ImageButton editButton;
        ImageButton deleteButton;
        Button historyButton;

        Device boundDevice;

        DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            deviceImage = itemView.findViewById(R.id.deviceImage);
            deviceName = itemView.findViewById(R.id.deviceName);
            deviceCategory = itemView.findViewById(R.id.deviceCategory);
            devicePower = itemView.findViewById(R.id.devicePower);
            deviceStatus = itemView.findViewById(R.id.deviceStatus);
            runningInfoContainer = itemView.findViewById(R.id.runningInfoContainer);
            deviceRunningTime = itemView.findViewById(R.id.deviceRunningTime);
            deviceRunningTimeText = itemView.findViewById(R.id.deviceRunningTimeText);
            deviceLiveCost = itemView.findViewById(R.id.deviceLiveCost);
            deviceCostLabel = itemView.findViewById(R.id.deviceCostLabel);
            deviceCost = itemView.findViewById(R.id.deviceCost);
            toggleButton = itemView.findViewById(R.id.toggleButton);
            editButton = itemView.findViewById(R.id.editButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
            historyButton = itemView.findViewById(R.id.historyButton);
        }

        void clearBinding() {
            boundDevice = null;
        }

        void bind(Device device) {
            boundDevice = device;
            runningHolders.remove(this);

            deviceName.setText(device.getName());
            deviceCategory.setText(getCategoryDisplayName(device));
            devicePower.setText(String.format(Locale.getDefault(),
                    "%.2f كيلو واط", device.getPowerConsumptionKw()));

            if (device.isRunning()) {
                deviceStatus.setImageResource(android.R.drawable.presence_online);
                deviceStatus.setColorFilter(context.getColor(R.color.green));
                toggleButton.setText(R.string.device_stop);
                toggleButton.setBackgroundColor(context.getColor(R.color.red));

                runningInfoContainer.setVisibility(View.VISIBLE);
                deviceCostLabel.setText(R.string.device_total_cost);
                runningHolders.add(this);
                updateRunningDisplay();
            } else {
                deviceStatus.setImageResource(android.R.drawable.presence_offline);
                deviceStatus.setColorFilter(context.getColor(R.color.gray));
                toggleButton.setText(R.string.device_start);
                toggleButton.setBackgroundColor(context.getColor(R.color.green));

                runningInfoContainer.setVisibility(View.GONE);
                deviceCostLabel.setText(R.string.device_total_cost);
                updateTotalCostDisplay(device, deviceCost);
            }

            if (device.getImageUrl() != null && !device.getImageUrl().isEmpty()) {
                dataManager.loadImageIntoView(device.getImageUrl(), deviceImage,
                        android.R.drawable.ic_menu_gallery);
            } else {
                deviceImage.setTag(null);
                deviceImage.setImageResource(android.R.drawable.ic_menu_gallery);
            }

            toggleButton.setOnClickListener(v -> {
                if (device.isRunning()) {
                    stopDevice(device);
                } else {
                    startDevice(device);
                }
            });

            editButton.setOnClickListener(v -> {
                if (onDeviceEditListener != null) {
                    onDeviceEditListener.onEditDevice(device);
                }
            });

            deleteButton.setOnClickListener(v -> deleteDevice(device, getAdapterPosition()));

            historyButton.setOnClickListener(v -> {
                if (onDeviceHistoryListener != null) {
                    onDeviceHistoryListener.onViewHistory(device);
                }
            });
        }

        /** تحديث المؤقت والتكلفة الحية (يُستدعى كل ثانية) */
        void updateRunningDisplay() {
            Device device = boundDevice;
            if (device == null || !device.isRunning() || device.getStartTime() <= 0) {
                return;
            }

            long millis = device.getCurrentSessionMillis();
            deviceRunningTime.setText(Device.formatDurationClock(millis));
            deviceRunningTimeText.setText(Device.formatDuration(millis));
            double sessionCost = device.getCurrentCost();
            deviceLiveCost.setText(String.format(Locale.getDefault(), "%.4f شيكل", sessionCost));
            updateTotalCostDisplay(device, deviceCost);
        }

        private void startDevice(Device device) {
            device.startDevice();
            int pos = getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                notifyItemChanged(pos);
            }
            if (onDeviceChangedListener != null) {
                onDeviceChangedListener.onDeviceChanged();
            }
            dataManager.updateDevice(device, new DataManager.DataCallback<Device>() {
                @Override
                public void onSuccess(Device result) {
                }

                @Override
                public void onError(String error) {
                }
            });
        }

        private void stopDevice(Device device) {
            long startTime = device.getStartTime();
            long endTime = System.currentTimeMillis();
            UsageRecord record = new UsageRecord(device, startTime, endTime);

            device.stopDevice();

            String deviceId = device.getId();
            if (deviceId != null) {
                historicalCosts.put(deviceId,
                        historicalCosts.getOrDefault(deviceId, 0.0) + record.getCost());
            }

            int pos = getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                notifyItemChanged(pos);
            }
            if (onDeviceChangedListener != null) {
                onDeviceChangedListener.onDeviceChanged();
            }

            dataManager.saveUsageRecord(record, new DataManager.DataCallback<UsageRecord>() {
                @Override
                public void onSuccess(UsageRecord result) {
                }

                @Override
                public void onError(String error) {
                }
            });

            dataManager.updateDevice(device, new DataManager.DataCallback<Device>() {
                @Override
                public void onSuccess(Device result) {
                }

                @Override
                public void onError(String error) {
                }
            });
        }

        private void deleteDevice(Device device, int position) {
            dataManager.deleteDevice(device, new DataManager.DataCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    if (position >= 0 && position < devices.size()) {
                        devices.remove(position);
                        notifyItemRemoved(position);
                        notifyItemRangeChanged(position, devices.size());
                    }
                    if (onDeviceChangedListener != null) {
                        onDeviceChangedListener.onDeviceChanged();
                    }
                }

                @Override
                public void onError(String error) {
                }
            });
        }
    }

    private static String getCategoryDisplayName(Device device) {
        if (device.getCategoryName() != null && !device.getCategoryName().isEmpty()) {
            return device.getCategoryName();
        }
        if (device.getCategoryId() != null && !device.getCategoryId().isEmpty()) {
            return device.getCategoryId();
        }
        return "غير محدد";
    }
}
