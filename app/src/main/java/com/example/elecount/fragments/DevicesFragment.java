package com.example.elecount.fragments;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.elecount.R;
import com.example.elecount.Hellper.DataManager;
import com.example.elecount.Hellper.UserSession;
import com.example.elecount.Hellper.DeviceHistoryDialog;
import com.example.elecount.Hellper.UsageStatsHelper;
import com.example.elecount.adapters.DeviceAdapter;
import com.example.elecount.models.Device;
import com.example.elecount.models.UsageRecord;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;

/**
 * Fragment الأجهزة
 * يعرض قائمة جميع الأجهزة مع إمكانية إضافة وحذف وتشغيل/إيقاف
 * 
 * الشرح: هذا الفراجمنت يدير جميع الأجهزة الكهربائية
 * يمكن للمستخدم إضافة أجهزة جديدة، حذف أجهزة، وتشغيلها/إيقافها
 */
public class DevicesFragment extends Fragment {
    
    // مراجع للعناصر في الواجهة
    private RecyclerView devicesRecycler;
    private LinearLayout emptyLayout;
    private FloatingActionButton addDeviceFab;
    
    // مدير البيانات
    private DataManager dataManager;
    
    // محول لعرض قائمة الأجهزة
    private DeviceAdapter deviceAdapter;
    
    // قائمة الأجهزة
    private ArrayList<Device> devices = new ArrayList<>();
    
    // الصورة المختارة في نافذة الإضافة/التعديل
    private Uri selectedImageUri;
    private Uri cameraImageUri;
    private ImageView dialogImagePreview;
    
    private ActivityResultLauncher<String> pickImageLauncher;
    private ActivityResultLauncher<Uri> takePictureLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedImageUri = uri;
                        updateDialogImagePreview();
                    }
                }
        );
        
        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success && cameraImageUri != null) {
                        selectedImageUri = cameraImageUri;
                        updateDialogImagePreview();
                    }
                }
        );
        
        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) {
                        launchCamera();
                    } else {
                        Toast.makeText(requireContext(), "يلزم إذن الكاميرا للتصوير", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                             @Nullable Bundle savedInstanceState) {
        // تضخيم الـ layout الخاص بالـ Fragment
        View view = inflater.inflate(R.layout.fragment_devices, container, false);
        
        // تهيئة DataManager
        dataManager = new DataManager(requireContext());
        
        // ربط العناصر من الـ layout
        initViews(view);
        
        // إعداد RecyclerView
        setupRecyclerView();
        
        // إعداد زر الإضافة
        setupAddButton();
        
        // تحميل البيانات
        loadDevices();
        
        return view;
    }
    
    /**
     * ربط العناصر من الـ layout
     */
    private void initViews(View view) {
        devicesRecycler = view.findViewById(R.id.devicesRecycler);
        emptyLayout = view.findViewById(R.id.emptyLayout);
        addDeviceFab = view.findViewById(R.id.addDeviceFab);
    }
    
    /**
     * إعداد RecyclerView لعرض الأجهزة
     * يستخدم LinearLayoutManager لعرض الأجهزة في قائمة عمودية
     */
    private void setupRecyclerView() {
        // استخدام LinearLayoutManager لعرض عمود واحد
        devicesRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        deviceAdapter = new DeviceAdapter(requireContext(), devices, dataManager);
        devicesRecycler.setAdapter(deviceAdapter);
        
        // إعداد listener لتحديث البيانات عند تغيير حالة الجهاز
        deviceAdapter.setOnDeviceChangedListener(() -> loadHistoricalCosts());
        
        deviceAdapter.setOnDeviceEditListener(device -> showDeviceDialog(device));

        deviceAdapter.setOnDeviceHistoryListener(device ->
                DeviceHistoryDialog.show(this, device, dataManager));
    }
    
    /**
     * إعداد زر إضافة جهاز جديد
     */
    private void setupAddButton() {
        addDeviceFab.setOnClickListener(v -> showDeviceDialog(null));
    }
    
    /**
     * تحميل الأجهزة من قاعدة البيانات
     */
    private void loadDevices() {
        // عرض رسالة أثناء التحميل
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                emptyLayout.setVisibility(View.VISIBLE);
                devicesRecycler.setVisibility(View.GONE);
            });
        }
        
        dataManager.getAllDevices(new DataManager.DataCallback<ArrayList<Device>>() {
            @Override
            public void onSuccess(ArrayList<Device> result) {
                devices.clear();
                devices.addAll(result);
                
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        deviceAdapter.notifyDataSetChanged();
                        loadHistoricalCosts();
                        
                        // إظهار/إخفاء رسالة "لا توجد أجهزة"
                        if (devices.isEmpty()) {
                            emptyLayout.setVisibility(View.VISIBLE);
                            devicesRecycler.setVisibility(View.GONE);
                        } else {
                            emptyLayout.setVisibility(View.GONE);
                            devicesRecycler.setVisibility(View.VISIBLE);
                        }
                    });
                }
            }
            
            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        emptyLayout.setVisibility(View.VISIBLE);
                        devicesRecycler.setVisibility(View.GONE);
                    });
                }
            }
        });
    }
    
    // قائمة التصنيفات الافتراضية
    private static final String[] DEFAULT_CATEGORIES = {
        "تبريد وتكييف",
        "إضاءة",
        "مطبخ",
        "ترفيه",
        "تنظيف",
        "تدفئة",
        "أخرى"
    };
    
    // قائمة أسماء التصنيفات للـ Spinner
    private ArrayList<String> categoryNames = new ArrayList<>();
    
    /**
     * عرض Dialog لإضافة أو تعديل جهاز
     */
    private void showDeviceDialog(@Nullable Device deviceToEdit) {
        boolean isEdit = deviceToEdit != null;
        
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_add_device);
        
        TextView dialogTitle = dialog.findViewById(R.id.dialogTitle);
        ImageView deviceImagePreview = dialog.findViewById(R.id.deviceImagePreview);
        Button selectImageButton = dialog.findViewById(R.id.selectImageButton);
        EditText deviceNameInput = dialog.findViewById(R.id.deviceNameInput);
        Spinner categorySpinner = dialog.findViewById(R.id.categorySpinner);
        EditText powerInput = dialog.findViewById(R.id.powerInput);
        Button cancelButton = dialog.findViewById(R.id.cancelButton);
        Button saveButton = dialog.findViewById(R.id.saveButton);
        
        dialogImagePreview = deviceImagePreview;
        selectedImageUri = null;
        
        dialogTitle.setText(isEdit ? R.string.device_edit_title : R.string.devices_add);
        
        if (isEdit) {
            deviceNameInput.setText(deviceToEdit.getName());
            powerInput.setText(String.format(java.util.Locale.getDefault(), "%.2f",
                    deviceToEdit.getPowerConsumptionKw()));
            if (deviceToEdit.getImageUrl() != null && !deviceToEdit.getImageUrl().isEmpty()) {
                dataManager.loadImageIntoView(deviceToEdit.getImageUrl(), deviceImagePreview,
                        android.R.drawable.ic_menu_gallery);
            }
        }
        
        selectImageButton.setOnClickListener(v -> showImageSourceDialog());
        
        String selectedCategory = isEdit
                ? (deviceToEdit.getCategoryName() != null ? deviceToEdit.getCategoryName() : deviceToEdit.getCategoryId())
                : null;
        loadCategoriesForSpinner(categorySpinner, selectedCategory);
        
        cancelButton.setOnClickListener(v -> dialog.dismiss());
        
        saveButton.setOnClickListener(v -> {
            String name = deviceNameInput.getText().toString().trim();
            String powerStr = powerInput.getText().toString().trim();
            String categoryName = categorySpinner.getSelectedItem().toString();
            
            if (name.isEmpty()) {
                deviceNameInput.setError(getString(R.string.error_empty_field));
                return;
            }
            
            if (powerStr.isEmpty()) {
                powerInput.setError(getString(R.string.error_empty_field));
                return;
            }
            
            double power;
            try {
                power = Double.parseDouble(powerStr);
            } catch (NumberFormatException e) {
                powerInput.setError("قيمة غير صحيحة");
                return;
            }
            
            saveButton.setEnabled(false);
            
            if (isEdit && (deviceToEdit.getId() == null || deviceToEdit.getId().isEmpty())) {
                saveButton.setEnabled(true);
                Toast.makeText(requireContext(), "معرف الجهاز غير موجود، أعد تحميل القائمة", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (isEdit) {
                deviceToEdit.setName(name);
                deviceToEdit.setCategoryId(categoryName);
                deviceToEdit.setPowerConsumptionKw(power);
                saveDeviceWithOptionalImage(deviceToEdit, true, dialog, saveButton);
            } else {
                Device newDevice = new Device(name, categoryName, power);
                newDevice.setPricePerKw(UserSession.getPricePerKw(requireContext()));
                saveDeviceWithOptionalImage(newDevice, false, dialog, saveButton);
            }
        });
        
        dialog.show();
        
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }
    
    /**
     * عرض نتيجة حفظ/تحديث الجهاز
     */
    private void showSaveResult(boolean isEdit, @Nullable String warning, Dialog dialog, Button saveButton) {
        if (getActivity() == null) {
            return;
        }
        getActivity().runOnUiThread(() -> {
            loadDevices();
            dialog.dismiss();
            String message = isEdit ? "تم تحديث الجهاز" : "تم إضافة الجهاز";
            if (warning != null && !warning.isEmpty()) {
                message = message + "\n" + warning;
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * حفظ الجهاز مع رفع الصورة إن وُجدت
     */
    private void saveDeviceWithOptionalImage(Device device, boolean isEdit, Dialog dialog, Button saveButton) {
        Runnable saveToCloud = () -> {
            DataManager.DataCallback<Device> callback = new DataManager.DataCallback<Device>() {
                @Override
                public void onSuccess(Device result) {
                    showSaveResult(isEdit, null, dialog, saveButton);
                }
                
                @Override
                public void onSuccess(Device result, String warning) {
                    showSaveResult(isEdit, warning, dialog, saveButton);
                }
                
                @Override
                public void onError(String error) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            saveButton.setEnabled(true);
                            Toast.makeText(requireContext(),
                                    isEdit ? "فشل تحديث الجهاز: " + error : "فشل إضافة الجهاز: " + error,
                                    Toast.LENGTH_LONG).show();
                        });
                    }
                }
            };
            
            if (isEdit) {
                dataManager.updateDevice(device, callback);
            } else {
                dataManager.addDevice(device, callback);
            }
        };
        
        if (selectedImageUri != null) {
            dataManager.uploadImage(requireContext(), selectedImageUri, new DataManager.DataCallback<String>() {
                @Override
                public void onSuccess(String imageUrl) {
                    device.setImageUrl(imageUrl);
                    saveToCloud.run();
                }
                
                @Override
                public void onError(String error) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            saveButton.setEnabled(true);
                            Toast.makeText(requireContext(), "فشل رفع الصورة: " + error, Toast.LENGTH_LONG).show();
                        });
                    }
                }
            });
        } else {
            saveToCloud.run();
        }
    }
    
    /**
     * عرض خيارات اختيار الصورة
     */
    private void showImageSourceDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("اختر صورة الجهاز")
                .setItems(new CharSequence[]{"التقاط صورة", "اختيار من المعرض"}, (d, which) -> {
                    if (which == 0) {
                        openCamera();
                    } else {
                        pickImageLauncher.launch("image/*");
                    }
                })
                .show();
    }
    
    /**
     * فتح الكاميرا بعد التحقق من الإذن
     */
    private void openCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
            return;
        }
        launchCamera();
    }
    
    /**
     * تشغيل الكاميرا
     */
    private void launchCamera() {
        try {
            File imageFile = new File(requireContext().getCacheDir(),
                    "device_" + System.currentTimeMillis() + ".jpg");
            cameraImageUri = FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".fileprovider",
                    imageFile
            );
            takePictureLauncher.launch(cameraImageUri);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "تعذر فتح الكاميرا", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * تحديث معاينة الصورة في النافذة
     */
    private void updateDialogImagePreview() {
        if (dialogImagePreview != null && selectedImageUri != null) {
            dialogImagePreview.setImageURI(selectedImageUri);
        }
    }
    
    /**
     * تحميل صورة من رابط Appwrite
     */
    /**
     * جلب التصنيفات لعرضها في الـ Spinner
     */
    private void loadCategoriesForSpinner(Spinner categorySpinner, @Nullable String selectedCategory) {
        dataManager.getAllCategories(new DataManager.DataCallback<ArrayList<com.example.elecount.models.Category>>() {
            @Override
            public void onSuccess(ArrayList<com.example.elecount.models.Category> result) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        categoryNames.clear();
                        
                        for (com.example.elecount.models.Category category : result) {
                            categoryNames.add(category.getName());
                        }
                        
                        if (categoryNames.isEmpty()) {
                            for (String cat : DEFAULT_CATEGORIES) {
                                categoryNames.add(cat);
                            }
                        }
                        
                        setupSpinnerAdapter(categorySpinner, selectedCategory);
                    });
                }
            }
            
            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        categoryNames.clear();
                        for (String cat : DEFAULT_CATEGORIES) {
                            categoryNames.add(cat);
                        }
                        setupSpinnerAdapter(categorySpinner, selectedCategory);
                    });
                }
            }
        });
    }
    
    /**
     * إعداد Spinner واختيار التصنيف الحالي عند التعديل
     */
    private void setupSpinnerAdapter(Spinner categorySpinner, @Nullable String selectedCategory) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            categoryNames
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);
        
        if (selectedCategory != null) {
            int index = categoryNames.indexOf(selectedCategory);
            if (index >= 0) {
                categorySpinner.setSelection(index);
            }
        }
    }

    private void loadHistoricalCosts() {
        if (deviceAdapter == null || dataManager == null) {
            return;
        }
        dataManager.getAllUsageRecords(new DataManager.DataCallback<ArrayList<UsageRecord>>() {
            @Override
            public void onSuccess(ArrayList<UsageRecord> result) {
                if (isAdded() && deviceAdapter != null) {
                    deviceAdapter.setHistoricalCosts(
                            UsageStatsHelper.aggregateCostByDevice(result));
                }
            }

            @Override
            public void onError(String error) {
            }
        });
    }
    
    @Override
    public void onResume() {
        super.onResume();
        loadHistoricalCosts();
        if (deviceAdapter != null) {
            deviceAdapter.startLiveUpdates();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
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
