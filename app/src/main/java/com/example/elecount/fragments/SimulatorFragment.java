package com.example.elecount.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.example.elecount.R;
import com.example.elecount.Hellper.DataManager;
import com.example.elecount.models.Device;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Fragment المحاكي
 * يسمح بحساب التكلفة المتوقعة لتشغيل جهاز لفترة محددة
 * 
 * الشرح: هذا الفراجمنت يعمل بدون تسجيل دخول (وضع المحاكي)
 * يستخدمه العملاء في المحلات التجارية لمحاكاة تكلفة الجهاز قبل الشراء
 */
public class SimulatorFragment extends Fragment {
    
    // مراجع للعناصر في الواجهة
    private Spinner deviceSpinner;
    private EditText durationInput;
    private Spinner durationUnitSpinner;
    private Button calculateButton;
    private CardView resultCard;
    private TextView estimatedCostText;
    private TextView energyConsumedText;
    
    // مدير البيانات
    private DataManager dataManager;
    
    // قائمة الأجهزة المتاحة
    private ArrayList<Device> devices = new ArrayList<>();
    private ArrayList<String> deviceNames = new ArrayList<>();
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                             @Nullable Bundle savedInstanceState) {
        // تضخيم الـ layout الخاص بالـ Fragment
        View view = inflater.inflate(R.layout.fragment_simulator, container, false);
        
        // تهيئة DataManager
        dataManager = new DataManager(requireContext());
        
        // ربط العناصر من الـ layout
        initViews(view);
        
        // تحميل الأجهزة
        loadDevices();
        
        // إعداد زر الحساب
        setupCalculateButton();
        
        return view;
    }
    
    /**
     * ربط العناصر من الـ layout
     */
    private void initViews(View view) {
        deviceSpinner = view.findViewById(R.id.deviceSpinner);
        durationInput = view.findViewById(R.id.durationInput);
        durationUnitSpinner = view.findViewById(R.id.durationUnitSpinner);
        calculateButton = view.findViewById(R.id.calculateButton);
        resultCard = view.findViewById(R.id.resultCard);
        estimatedCostText = view.findViewById(R.id.estimatedCostText);
        energyConsumedText = view.findViewById(R.id.energyConsumedText);
    }
    
    /**
     * تحميل الأجهزة من قاعدة البيانات
     */
    private void loadDevices() {
        // في وضع المحاكي، نجلب جميع الأجهزة (بدون تسجيل دخول)
        dataManager.getAllDevices(new DataManager.DataCallback<ArrayList<Device>>() {
            @Override
            public void onSuccess(ArrayList<Device> result) {
                devices.clear();
                devices.addAll(result);
                
                deviceNames.clear();
                for (Device device : devices) {
                    deviceNames.add(device.getName());
                }
                
                // تحديث Spinner
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (devices.isEmpty()) {
                            // إضافة رسالة توضيحية
                            deviceNames.add("لا توجد أجهزة. أضف جهاز من تبويب الأجهزة");
                        }
                        
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            requireContext(),
                            android.R.layout.simple_spinner_item,
                            deviceNames
                        );
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        deviceSpinner.setAdapter(adapter);
                    });
                }
            }
            
            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        deviceNames.clear();
                        deviceNames.add("أضف أجهزة من تبويب الأجهزة أولاً");
                        
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            requireContext(),
                            android.R.layout.simple_spinner_item,
                            deviceNames
                        );
                        deviceSpinner.setAdapter(adapter);
                    });
                }
            }
        });
    }
    
    /**
     * إعداد زر الحساب
     */
    private void setupCalculateButton() {
        calculateButton.setOnClickListener(v -> {
            // التحقق من اختيار جهاز
            if (devices.isEmpty()) {
                Toast.makeText(requireContext(), getString(R.string.simulator_no_device), 
                             Toast.LENGTH_SHORT).show();
                return;
            }
            
            // الحصول على الجهاز المختار
            int selectedPosition = deviceSpinner.getSelectedItemPosition();
            Device selectedDevice = devices.get(selectedPosition);
            
            // الحصول على المدة
            String durationStr = durationInput.getText().toString().trim();
            if (durationStr.isEmpty()) {
                durationInput.setError(getString(R.string.error_empty_field));
                return;
            }
            
            double duration;
            try {
                duration = Double.parseDouble(durationStr);
            } catch (NumberFormatException e) {
                durationInput.setError("قيمة غير صحيحة");
                return;
            }
            
            // تحويل المدة إلى ساعات حسب الوحدة المختارة
            String unit = durationUnitSpinner.getSelectedItem().toString();
            double hoursMultiplier;
            if (unit.equals("ساعات")) {
                hoursMultiplier = 1;
            } else if (unit.equals("أيام")) {
                hoursMultiplier = 24;
            } else if (unit.equals("أشهر")) {
                hoursMultiplier = 24 * 30; // 30 يوم في الشهر
            } else {
                hoursMultiplier = 1;
            }
            
            double totalHours = duration * hoursMultiplier;
            
            // حساب التكلفة والطاقة المستهلكة
            double estimatedCost = selectedDevice.simulateCost(totalHours);
            double energyConsumed = selectedDevice.getPowerConsumptionKw() * totalHours;
            
            // عرض النتيجة
            showResult(estimatedCost, energyConsumed);
        });
    }
    
    /**
     * عرض نتيجة الحساب
     */
    private void showResult(double cost, double energy) {
        // عرض بطاقة النتيجة
        resultCard.setVisibility(View.VISIBLE);
        
        // تحديث النصوص
        estimatedCostText.setText(String.format(Locale.getDefault(), "%.2f شيكل", cost));
        energyConsumedText.setText(String.format(Locale.getDefault(), "%.2f كيلو واط/ساعة", energy));
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
