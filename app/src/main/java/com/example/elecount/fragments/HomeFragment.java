package com.example.elecount.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.elecount.R;
import com.example.elecount.Hellper.DataManager;
import com.example.elecount.Hellper.DeviceHistoryDialog;
import com.example.elecount.Hellper.UsageStatsHelper;
import com.example.elecount.adapters.DeviceAdapter;
import com.example.elecount.models.Device;
import com.example.elecount.models.UsageRecord;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Fragment الرئيسية
 * يعرض ملخص الاستهلاك والأجهزة العاملة حالياً
 * 
 * الشرح: هذا الفراجمنت يعرض لوحة التحكم الرئيسية
 * يحتوي على إحصائيات التكلفة والأجهزة العاملة
 * يتم تحديث البيانات تلقائياً كل ثانية
 */
public class HomeFragment extends Fragment {
    
    // مراجع للعناصر في الواجهة
    private TextView welcomeText;
    private TextView totalCostText;
    private TextView runningDevicesText;
    private TextView currentConsumptionText;
    private TextView recordsSessionsText;
    private TextView recordsEnergyText;
    private TextView recordsCostText;
    private TextView mostExpensiveDeviceText;
    private TextView mostExpensiveCostText;
    private TextView cheapestDeviceText;
    private TextView cheapestCostText;
    private TextView emptyText;
    private RecyclerView runningDevicesRecycler;
    
    // مدير البيانات
    private DataManager dataManager;
    
    // محول لعرض قائمة الأجهزة
    private DeviceAdapter deviceAdapter;
    
    // قائمة الأجهزة العاملة
    private ArrayList<Device> runningDevices = new ArrayList<>();

    // سجلات الاستخدام
    private ArrayList<UsageRecord> usageRecords = new ArrayList<>();
    
    // Handler لتحديث البيانات بشكل دوري
    private Handler updateHandler = new Handler(Looper.getMainLooper());
    
    // Runnable للتحديث التلقائي
    private Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            // تحديث البيانات
            updateStats();
            
            // جدولة التحديث التالي بعد ثانية واحدة
            updateHandler.postDelayed(this, 1000);
        }
    };
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                             @Nullable Bundle savedInstanceState) {
        // تضخيم الـ layout الخاص بالـ Fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        
        // تهيئة DataManager
        dataManager = new DataManager(requireContext());
        
        // ربط العناصر من الـ layout
        initViews(view);
        
        // إعداد RecyclerView
        setupRecyclerView();
        
        // تحميل البيانات
        loadData();
        
        return view;
    }
    
    /**
     * ربط العناصر من الـ layout
     */
    private void initViews(View view) {
        welcomeText = view.findViewById(R.id.welcomeText);
        totalCostText = view.findViewById(R.id.totalCostText);
        runningDevicesText = view.findViewById(R.id.runningDevicesText);
        currentConsumptionText = view.findViewById(R.id.currentConsumptionText);
        recordsSessionsText = view.findViewById(R.id.recordsSessionsText);
        recordsEnergyText = view.findViewById(R.id.recordsEnergyText);
        recordsCostText = view.findViewById(R.id.recordsCostText);
        mostExpensiveDeviceText = view.findViewById(R.id.mostExpensiveDeviceText);
        mostExpensiveCostText = view.findViewById(R.id.mostExpensiveCostText);
        cheapestDeviceText = view.findViewById(R.id.cheapestDeviceText);
        cheapestCostText = view.findViewById(R.id.cheapestCostText);
        emptyText = view.findViewById(R.id.emptyText);
        runningDevicesRecycler = view.findViewById(R.id.runningDevicesRecycler);
        
        // تحديث رسالة الترحيب
        if (dataManager.getCurrentUser() != null) {
            welcomeText.setText(getString(R.string.home_welcome) + " " + 
                              dataManager.getCurrentUser().getName());
        } else {
            // وضع الضيف
            welcomeText.setText("مرحباً بك في مدير الكهرباء");
        }
    }
    
    /**
     * إعداد RecyclerView لعرض الأجهزة العاملة
     */
    private void setupRecyclerView() {
        runningDevicesRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        deviceAdapter = new DeviceAdapter(requireContext(), runningDevices, dataManager);
        runningDevicesRecycler.setAdapter(deviceAdapter);
        
        // إعداد listener لتحديث البيانات عند تغيير حالة الجهاز
        deviceAdapter.setOnDeviceChangedListener(() -> {
            refreshRunningDevicesQuietly();
            loadUsageRecordsQuietly();
        });

        deviceAdapter.setOnDeviceHistoryListener(device ->
                DeviceHistoryDialog.show(this, device, dataManager));
    }
    
    /**
     * تحميل البيانات من قاعدة البيانات
     */
    private void loadData() {
        // عرض رسالة توضيحية أثناء التحميل
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                emptyText.setVisibility(View.VISIBLE);
                emptyText.setText("مرحباً! أضف أجهزة كهربائية من تبويب \"الأجهزة\" لتبدأ");
            });
        }
        
        // جلب جميع الأجهزة وسجلات الاستخدام
        loadUsageRecordsQuietly();
        dataManager.getAllDevices(new DataManager.DataCallback<ArrayList<Device>>() {
            @Override
            public void onSuccess(ArrayList<Device> result) {
                // تحديث قائمة الأجهزة العاملة
                updateRunningDevices(result);
                
                // تحديث الإحصائيات
                updateStats();
                
                // إخفاء رسالة الفراغ إذا كانت هناك أجهزة
                if (getActivity() != null && !result.isEmpty()) {
                    getActivity().runOnUiThread(() -> {
                        emptyText.setVisibility(View.GONE);
                    });
                }
            }
            
            @Override
            public void onError(String error) {
                // عرض رسالة فقط في الواجهة بدون Toast
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        emptyText.setVisibility(View.VISIBLE);
                        emptyText.setText("أضف أجهزة كهربائية من تبويب \"الأجهزة\" لتبدأ");
                    });
                }
            }
        });
    }
    
    /**
     * إعادة جلب الأجهزة من السحابة (بدون رسالة تحميل)
     */
    private void refreshRunningDevicesQuietly() {
        loadUsageRecordsQuietly();
        dataManager.getAllDevices(new DataManager.DataCallback<ArrayList<Device>>() {
            @Override
            public void onSuccess(ArrayList<Device> result) {
                if (isAdded()) {
                    updateRunningDevices(result);
                    updateStats();
                }
            }

            @Override
            public void onError(String error) {
                if (isAdded()) {
                    updateStats();
                }
            }
        });
    }

    /**
     * جلب سجلات الاستخدام وتحديث الإحصائيات
     */
    private void loadUsageRecordsQuietly() {
        dataManager.getAllUsageRecords(new DataManager.DataCallback<ArrayList<UsageRecord>>() {
            @Override
            public void onSuccess(ArrayList<UsageRecord> result) {
                if (!isAdded()) {
                    return;
                }
                usageRecords.clear();
                if (result != null) {
                    usageRecords.addAll(result);
                }
                if (deviceAdapter != null) {
                    deviceAdapter.setHistoricalCosts(
                            UsageStatsHelper.aggregateCostByDevice(usageRecords));
                }
                updateRecordStats();
                updateStats();
            }

            @Override
            public void onError(String error) {
                if (isAdded()) {
                    updateRecordStats();
                    updateStats();
                }
            }
        });
    }

    /**
     * تحديث بطاقات إحصائيات السجلات وأغلى/أرخص جهاز
     */
    private void updateRecordStats() {
        if (getActivity() == null || !isAdded()) {
            return;
        }

        final int sessions = UsageStatsHelper.sessionCount(usageRecords);
        final double energy = UsageStatsHelper.totalEnergy(usageRecords);
        final double recordsCost = UsageStatsHelper.totalCost(usageRecords);
        final UsageStatsHelper.DeviceRank highest = UsageStatsHelper.findHighest(usageRecords);
        final UsageStatsHelper.DeviceRank lowest = UsageStatsHelper.findLowest(usageRecords);

        getActivity().runOnUiThread(() -> {
            if (!isAdded()) {
                return;
            }
            recordsSessionsText.setText(String.valueOf(sessions));
            recordsEnergyText.setText(String.format(Locale.getDefault(), "%.2f kWh", energy));
            recordsCostText.setText(String.format(Locale.getDefault(), "%.2f", recordsCost));

            if (highest != null) {
                String name = highest.deviceName != null ? highest.deviceName : "—";
                mostExpensiveDeviceText.setText(name);
                mostExpensiveCostText.setText(String.format(Locale.getDefault(),
                        "%.2f شيكل", highest.totalCost));
            } else {
                mostExpensiveDeviceText.setText(R.string.home_no_records);
                mostExpensiveCostText.setText("0.00 شيكل");
            }

            if (lowest != null) {
                String name = lowest.deviceName != null ? lowest.deviceName : "—";
                cheapestDeviceText.setText(name);
                cheapestCostText.setText(String.format(Locale.getDefault(),
                        "%.2f شيكل", lowest.totalCost));
            } else {
                cheapestDeviceText.setText(R.string.home_no_records);
                cheapestCostText.setText("0.00 شيكل");
            }
        });
    }
    
    /**
     * تحديث قائمة الأجهزة العاملة
     */
    private void updateRunningDevices(ArrayList<Device> allDevices) {
        runningDevices.clear();
        
        // تصفية الأجهزة العاملة فقط
        for (Device device : allDevices) {
            if (device.isRunning()) {
                runningDevices.add(device);
            }
        }
        
        // تحديث RecyclerView
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                deviceAdapter.notifyDataSetChanged();
                
                // إظهار/إخفاء رسالة "لا توجد أجهزة"
                if (runningDevices.isEmpty()) {
                    emptyText.setVisibility(View.VISIBLE);
                    emptyText.setText(R.string.home_no_devices);
                    runningDevicesRecycler.setVisibility(View.GONE);
                } else {
                    emptyText.setVisibility(View.GONE);
                    runningDevicesRecycler.setVisibility(View.VISIBLE);
                }
                updateStats();
            });
        }
    }
    
    /**
     * تحديث الإحصائيات
     * يتم استدعاؤها بشكل دوري لتحديث التكلفة الحالية
     */
    private void updateStats() {
        double recordsCost = UsageStatsHelper.totalCost(usageRecords);
        double liveCost = 0;
        double currentConsumption = 0;
        int runningCount = 0;

        for (Device device : runningDevices) {
            if (device.isRunning()) {
                liveCost += device.getCurrentCost();
                currentConsumption += device.getPowerConsumptionKw();
                runningCount++;
            }
        }

        final double grandTotal = recordsCost + liveCost;
        final double finalCurrentConsumption = currentConsumption;
        final int finalRunningCount = runningCount;

        if (getActivity() != null && isAdded()) {
            getActivity().runOnUiThread(() -> {
                if (!isAdded()) {
                    return;
                }
                totalCostText.setText(String.format(Locale.getDefault(), "%.2f", grandTotal));
                runningDevicesText.setText(String.valueOf(finalRunningCount));
                currentConsumptionText.setText(
                        String.format(Locale.getDefault(), "%.2f كيلو واط/ساعة", finalCurrentConsumption)
                );
            });
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        refreshRunningDevicesQuietly();
        updateHandler.removeCallbacks(updateRunnable);
        updateHandler.post(updateRunnable);
        if (deviceAdapter != null) {
            deviceAdapter.startLiveUpdates();
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();
        updateHandler.removeCallbacks(updateRunnable);
        if (deviceAdapter != null) {
            deviceAdapter.stopLiveUpdates();
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        // تنظيف الموارد
        if (dataManager != null) {
            dataManager.shutdown();
        }
    }
}
