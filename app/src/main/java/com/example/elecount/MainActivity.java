package com.example.elecount;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.elecount.Hellper.DALAppWriteConnection;
import com.example.elecount.R;
import com.example.elecount.adapter.OwnerAdapter;
import com.example.elecount.model.Owner;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    
    Button btadd, btnPickDate;
    RecyclerView recyclerView;
    OwnerAdapter adapter;
    ArrayList<Owner> ownerList = new ArrayList<>();
    private DALAppWriteConnection dal;
    
    // الحقول الجديدة
    EditText etName;
    TextView tvSelectedDate, tvImageIcon;
    ImageView ivPreview;
    LinearLayout layoutPickImage;
    Date selectedDate = null;
    String selectedImageUrl = "";
    
    // Activity Result Launchers للكاميرا والمعرض
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> galleryLauncher;
    
    // متغيرات مؤقتة لحفظ الصورة المختارة
    private ImageView currentTargetImageView;
    private String[] currentUrlVariable;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // تهيئة DAL
        dal = new DALAppWriteConnection(this);
        
        // تهيئة Activity Result Launchers
        initializeImagePickers();
        
        // تهيئة الحقول
        etName = findViewById(R.id.etName);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        tvImageIcon = findViewById(R.id.tvImageIcon);
        ivPreview = findViewById(R.id.ivPreview);
        layoutPickImage = findViewById(R.id.layoutPickImage);
        btnPickDate = findViewById(R.id.btnPickDate);
        
        // تهيئة RecyclerView
        recyclerView = findViewById(R.id.rc);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OwnerAdapter(ownerList, dal);
        recyclerView.setAdapter(adapter);

        // زر الإضافة
        btadd = findViewById(R.id.btadd);
        btadd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // حفظ البيانات من الحقول
                saveOwnerFromFields();
            }
        });
        
        // زر اختيار التاريخ
        btnPickDate.setOnClickListener(v -> showDatePicker());
        
        // اختيار الصورة - فتح حوار لإدخال الرابط
        layoutPickImage.setOnClickListener(v -> {
            // استخدام الدالة العامة - سطر واحد فقط!
            pickAndUploadImage(ivPreview, url -> selectedImageUrl = url);
        });
        
        // جلب البيانات عند فتح التطبيق
        testGetData();
    }
    
    // ========== دوال رفع الصور (استخدام بسطر واحد!) ==========
    
    /**
     * تهيئة Activity Result Launchers للكاميرا والمعرض
     */
    private void initializeImagePickers() {
        // Launcher للكاميرا
        cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bundle extras = result.getData().getExtras();
                    Bitmap imageBitmap = (Bitmap) extras.get("data");
                    
                    if (imageBitmap != null) {
                        // تحويل Bitmap إلى byte array ورفعه
                        uploadImageToServer(imageBitmap, currentTargetImageView, currentUrlVariable);
                    }
                }
            }
        );
        
        // Launcher للمعرض
        galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    
                    if (imageUri != null) {
                        try {
                            // تحويل Uri إلى byte array ورفعه
                            InputStream inputStream = getContentResolver().openInputStream(imageUri);
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                            uploadImageToServer(bitmap, currentTargetImageView, currentUrlVariable);
                        } catch (Exception e) {
                            Toast.makeText(this, "⚠️ خطأ في قراءة الصورة: " + e.getMessage(), 
                                Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        );
    }
    
    /**
     * 🚀 الدالة العامة لرفع الصورة - استخدام بسطر واحد فقط!
     * 
     * @param targetImageView معرف الصورة التي سيتم عرض الصورة المرفوعة فيها
     * @param urlCallback دالة Callback لتحديث متغير الرابط عند نجاح الرفع
     * 
     * مثال الاستخدام:
     * pickAndUploadImage(ivPreview, url -> selectedImageUrl = url);
     */
    public void pickAndUploadImage(ImageView targetImageView, ImageUploadCallback urlCallback) {
        currentTargetImageView = targetImageView;
        
        // عرض قائمة الاختيار
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("اختر طريقة الإضافة");
        
        String[] options = {"📷 التقاط صورة", "🖼️ اختيار من المعرض"};
        
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                // فتح الكاميرا
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                cameraLauncher.launch(cameraIntent);
            } else {
                // فتح المعرض
                Intent galleryIntent = new Intent(Intent.ACTION_PICK);
                galleryIntent.setType("image/*");
                galleryLauncher.launch(galleryIntent);
            }
        });
        
        builder.setNegativeButton("إلغاء", null);
        builder.show();
        
        // حفظ الـ callback لاستخدامه بعد رفع الصورة
        this.imageUploadCallback = urlCallback;
    }
    
    private ImageUploadCallback imageUploadCallback;
    
    /**
     * رفع الصورة إلى السيرفر
     */
    private void uploadImageToServer(Bitmap bitmap, ImageView targetImageView, String[] urlVariable) {
        // عرض رسالة تحميل
        Toast.makeText(this, "📤 جاري رفع الصورة...", Toast.LENGTH_SHORT).show();
        
        new Thread(() -> {
            try {
                // تحويل Bitmap إلى byte array
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream);
                byte[] imageBytes = stream.toByteArray();
                
                // تسمية الملف بالوقت الحالي
                String fileName = "img_" + System.currentTimeMillis() + ".jpg";
                
                // رفع الملف باستخدام DALAppWriteConnection
                DALAppWriteConnection.OperationResult<DALAppWriteConnection.FileInfo> result = 
                    dal.uploadFile(imageBytes, fileName, "image/jpeg", null);
                
                runOnUiThread(() -> {
                    if (result.success) {
                        String uploadedUrl = result.data.fileUrl;
                        
                        // طباعة الرابط للتأكد
                        Log.d("IMAGE_UPLOAD", "✅ تم رفع الصورة بنجاح!");
                        Log.d("IMAGE_UPLOAD", "📌 الرابط: " + uploadedUrl);
                        Log.d("IMAGE_UPLOAD", "📁 معرف الملف: " + result.data.fileId);
                        Log.d("IMAGE_UPLOAD", "💾 حجم الملف: " + (result.data.fileSize / 1024) + " KB");
                        
                        // عرض الصورة في ImageView مع إضافة headers
                        Glide.with(this)
                            .load(new com.bumptech.glide.load.model.GlideUrl(uploadedUrl, 
                                new com.bumptech.glide.load.model.LazyHeaders.Builder()
                                    .addHeader("X-Appwrite-Project", "69033828003328299847")
                                    .addHeader("X-Appwrite-Key", "standard_2b5b7365808986dc2e7724df693d7e68b81f3ec6511ae1c7980a4be803a7b7d1a4de9e89805f53bbf1eceee468d61fc760d2eb3dcfe50647375d8b05ed16d7c911cf7f11a0ea48dfe678291aa169a29116e5adc85ff3dc7ebb9bb33c87ac975368c36a79dbd2ebe045811f459c851b59025a22c136a513c012bd3fff339386dd")
                                    .build()))
                            .centerCrop()
                            .into(targetImageView);
                        
                        // استدعاء الـ callback لتحديث المتغير
                        if (imageUploadCallback != null) {
                            imageUploadCallback.onImageUploaded(uploadedUrl);
                        }
                        
                        // إخفاء الأيقونة إذا كانت موجودة
                        if (tvImageIcon != null) {
                            tvImageIcon.setVisibility(View.GONE);
                        }
                        targetImageView.setVisibility(View.VISIBLE);
                        
                        Toast.makeText(this, "✅ تم رفع الصورة بنجاح!", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.e("IMAGE_UPLOAD", "❌ فشل رفع الصورة: " + result.message);
                        Toast.makeText(this, "❌ فشل رفع الصورة: " + result.message, 
                            Toast.LENGTH_LONG).show();
                    }
                });
                
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "⚠️ خطأ: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
    
    /**
     * Interface للـ Callback
     */
    public interface ImageUploadCallback {
        void onImageUploaded(String imageUrl);
    }
    
    // ========== باقي الدوال ==========
    
    /**
     * اختيار التاريخ
     */
    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            this,
            (view, selectedYear, selectedMonth, selectedDay) -> {
                calendar.set(selectedYear, selectedMonth, selectedDay);
                selectedDate = calendar.getTime();
                
                // عرض التاريخ المختار
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                tvSelectedDate.setText(sdf.format(selectedDate));
                tvSelectedDate.setTextColor(getResources().getColor(android.R.color.black));
            },
            year, month, day
        );
        
        datePickerDialog.show();
    }
    
    /**
     * حفظ المالك من الحقول
     */
    private void saveOwnerFromFields() {
        // جلب البيانات من الحقول
        String name = etName.getText().toString().trim();
        
        // التحقق من البيانات
        if (name.isEmpty()) {
            Toast.makeText(this, "⚠️ أدخل الاسم", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (selectedImageUrl.isEmpty()) {
            Toast.makeText(this, "⚠️ اختر صورة", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (selectedDate == null) {
            Toast.makeText(this, "⚠️ اختر التاريخ", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // إنشاء كائن Owner جديد
        Owner newOwner = new Owner(name, selectedDate, selectedImageUrl);
        
        // حفظ في قاعدة البيانات
        saveOwner(newOwner);
    }
    
    /**
     * حفظ المالك في قاعدة البيانات
     */
    private void saveOwner(Owner owner) {
        String tableName = "owner";
        String collectionId = null;
        
        new Thread(() -> {
            try {
                DALAppWriteConnection.OperationResult<ArrayList<Owner>> result = dal.saveData(
                    owner,
                    tableName,
                    collectionId
                );
                
                runOnUiThread(() -> {
                    if (result.success) {
                        Toast.makeText(this, "✅ تم الحفظ بنجاح", Toast.LENGTH_SHORT).show();
                        
                        // مسح الحقول
                        clearFields();
                        
                        // إعادة جلب البيانات
                        testGetData();
                    } else {
                        Toast.makeText(this, "❌ فشل الحفظ: " + result.message, Toast.LENGTH_LONG).show();
                    }
                });
                
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "⚠️ خطأ: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
    
    /**
     * مسح الحقول بعد الحفظ
     */
    private void clearFields() {
        etName.setText("");
        tvSelectedDate.setText("اختر التاريخ");
        tvSelectedDate.setTextColor(getResources().getColor(android.R.color.darker_gray));
        selectedDate = null;
        selectedImageUrl = "";
        
        ivPreview.setVisibility(View.GONE);
        tvImageIcon.setVisibility(View.VISIBLE);
    }
    private void testSaveData() {
        Log.d("SAVE_DATA_TEST", "=== بدء اختبار saveData ===");
        /*
        // تعريف Clothes object
        Clothes shirt = new Clothes(
            "قميص أبيض أساسي", 
            150.0, 
            "https://share7a.com/wp-content/uploads/2023/12/1000071380.jpg"
        );

*/
        Owner owner = new Owner(
                "sleman",
                new Date(),
                "https://share7a.com/wp-content/uploads/2023/12/1000071380.jpg"
        );

        
        // تعريف المتغيرات
        String tableName = "owner"; // اسم الجدول الجديد
        String collectionId = null; // null لأننا نستخدم tableName كـ collection ID
        

        
        // تشغيل saveData في Thread منفصل
        new Thread(() -> {
            try {
                // استدعاء saveData
                DALAppWriteConnection.OperationResult<ArrayList<Owner>> result = dal.saveData(
                    owner,           // البيانات
                    tableName,       // اسم الجدول
                    collectionId     // Collection ID (null)
                );
                
                Log.d("SAVE_DATA_TEST", "انتهى استدعاء saveData");
                Log.d("SAVE_DATA_TEST", "النتيجة: success=" + result.success + ", message=" + result.message);
                
                runOnUiThread(() -> {
                    if (result.success) {
                        // نجح الحفظ
                        ArrayList<Owner> savedItems = result.data;
                        testGetData();
                            
                    } else {
                        // فشل الحفظ
                        Log.e("ERROR", "فشل الحفظ: " + result.message);
                        

                    }
                });
                
            } catch (Exception e) {
                Log.e("SAVE_DATA_TEST", "خطأ في saveData: " + e.getMessage(), e);
                
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, 
                        "⚠️ خطأ تقني:\n" + e.getMessage(),
                        Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
    
    /**
     * مثال لجلب البيانات من قاعدة البيانات باستخدام getData
     */
    private void testGetData() {
        Log.d("GET_DATA_TEST", "=== بدء اختبار getData ===");
        
        String tableName = "owner";
        String collectionId = null;
        
        Log.d("GET_DATA_TEST", "جلب البيانات من الجدول: " + tableName);
        
        // تشغيل getData في Thread منفصل
        new Thread(() -> {
            try {
                DALAppWriteConnection.OperationResult<ArrayList<Owner>> result = dal.getData(
                    tableName,       // اسم الجدول
                    collectionId,    // Collection ID (null)
                    Owner.class       // نوع الكلاس المطلوب
                );
                
                Log.d("GET_DATA_TEST", "انتهى استدعاء getData");
                Log.d("GET_DATA_TEST", "النتيجة: success=" + result.success);
                
                runOnUiThread(() -> {
                    if (result.success) {
                        ArrayList<Owner> fetchedList = result.data;
                        
                        // تحديث القائمة وإشعار الـ Adapter
                        ownerList.clear();
                        ownerList.addAll(fetchedList);
                        adapter.notifyDataSetChanged();
                        
                        Toast.makeText(MainActivity.this, 
                            "✅ تم جلب " + ownerList.size() + " مالك",
                            Toast.LENGTH_SHORT).show();
                            
                        // طباعة العناصر في Logcat
                        for (Owner owner : ownerList) {
                            Log.d("OWNER_ITEM", "المالك: " + owner.toString() + ", ID: " + owner.getId());
                        }
                    } else {
                        Toast.makeText(MainActivity.this, 
                            "❌ فشل جلب البيانات: " + result.message,
                            Toast.LENGTH_LONG).show();
                    }
                });
                
            } catch (Exception e) {
                Log.e("GET_DATA_TEST", "خطأ في getData: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, 
                        "⚠️ خطأ: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
}