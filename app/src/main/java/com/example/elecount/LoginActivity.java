package com.example.elecount;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.elecount.Hellper.DataManager;
import com.example.elecount.Hellper.UserSession;
import com.example.elecount.models.User;
import com.google.android.material.textfield.TextInputLayout;

/**
 * شاشة تسجيل الدخول وإنشاء حساب جديد
 * 
 * الشرح: هذه الشاشة تسمح للمستخدم بـ:
 * 1. تسجيل الدخول إذا كان لديه حساب
 * 2. إنشاء حساب جديد إذا لم يكن لديه حساب
 * 3. الدخول كضيف لاستخدام المحاكي فقط
 */
public class LoginActivity extends AppCompatActivity {
    
    // اسم ملف الإعدادات المحفوظة
    private static final String PREFS_NAME = "EleCountPrefs";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_EMAIL = "userEmail";
    private static final String KEY_USER_NAME = "userName";
    
    // العناصر في الواجهة
    private TextView formTitle;
    private TextInputLayout nameLayout;
    private EditText nameInput;
    private EditText emailInput;
    private EditText passwordInput;
    private TextInputLayout confirmPasswordLayout;
    private EditText confirmPasswordInput;
    private Button submitButton;
    private ProgressBar progressBar;
    private TextView switchText;
    private TextView switchButton;
    private Button guestButton;
    
    // مدير البيانات
    private DataManager dataManager;
    
    // حالة النموذج: true = تسجيل جديد، false = تسجيل دخول
    private boolean isRegisterMode = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // تغيير لون شريط الحالة
        setStatusBarColor();
        
        // التحقق إذا كان المستخدم مسجل دخوله مسبقاً
        if (isUserLoggedIn()) {
            goToMainActivity();
            return;
        }
        
        setContentView(R.layout.activity_login);
        
        // دعم RTL للعربية
        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        
        // تهيئة مدير البيانات
        dataManager = new DataManager(this);
        
        // ربط العناصر
        initViews();
        
        // إعداد الأحداث
        setupListeners();
    }
    
    /**
     * تغيير لون شريط الحالة
     */
    private void setStatusBarColor() {
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimary));
        
        // جعل أيقونات شريط الحالة فاتحة (بيضاء)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            View decorView = window.getDecorView();
            int flags = decorView.getSystemUiVisibility();
            flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            decorView.setSystemUiVisibility(flags);
        }
    }
    
    /**
     * ربط العناصر من الـ layout
     */
    private void initViews() {
        formTitle = findViewById(R.id.formTitle);
        nameLayout = findViewById(R.id.nameLayout);
        nameInput = findViewById(R.id.nameInput);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        confirmPasswordLayout = findViewById(R.id.confirmPasswordLayout);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        submitButton = findViewById(R.id.submitButton);
        progressBar = findViewById(R.id.progressBar);
        switchText = findViewById(R.id.switchText);
        switchButton = findViewById(R.id.switchButton);
        guestButton = findViewById(R.id.guestButton);
    }
    
    /**
     * إعداد مستمعي الأحداث
     */
    private void setupListeners() {
        // زر تسجيل الدخول / إنشاء حساب
        submitButton.setOnClickListener(v -> {
            if (isRegisterMode) {
                handleRegister();
            } else {
                handleLogin();
            }
        });
        
        // رابط التبديل بين تسجيل الدخول والتسجيل
        switchButton.setOnClickListener(v -> toggleMode());
        
        // زر الدخول كضيف
        guestButton.setOnClickListener(v -> goToMainActivity());
    }
    
    /**
     * التبديل بين وضع تسجيل الدخول ووضع إنشاء حساب
     */
    private void toggleMode() {
        isRegisterMode = !isRegisterMode;
        
        if (isRegisterMode) {
            // وضع إنشاء حساب جديد
            formTitle.setText("إنشاء حساب جديد");
            nameLayout.setVisibility(View.VISIBLE);
            confirmPasswordLayout.setVisibility(View.VISIBLE);
            submitButton.setText("إنشاء حساب");
            switchText.setText("لديك حساب؟");
            switchButton.setText("تسجيل الدخول");
        } else {
            // وضع تسجيل الدخول
            formTitle.setText("تسجيل الدخول");
            nameLayout.setVisibility(View.GONE);
            confirmPasswordLayout.setVisibility(View.GONE);
            submitButton.setText("تسجيل الدخول");
            switchText.setText("ليس لديك حساب؟");
            switchButton.setText("إنشاء حساب");
        }
        
        // مسح الحقول
        clearFields();
    }
    
    /**
     * معالجة تسجيل الدخول
     */
    private void handleLogin() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        
        // التحقق من صحة البيانات
        if (email.isEmpty()) {
            emailInput.setError("أدخل البريد الإلكتروني");
            return;
        }
        
        if (password.isEmpty()) {
            passwordInput.setError("أدخل كلمة المرور");
            return;
        }
        
        // عرض شريط التحميل
        showLoading(true);
        
        // محاولة تسجيل الدخول
        dataManager.loginUser(email, password, new DataManager.DataCallback<User>() {
            @Override
            public void onSuccess(User user) {
                runOnUiThread(() -> {
                    showLoading(false);
                    
                    // حفظ حالة تسجيل الدخول
                    saveLoginState(user);
                    
                    Toast.makeText(LoginActivity.this, 
                        "مرحباً " + user.getName(), Toast.LENGTH_SHORT).show();
                    
                    // الانتقال للشاشة الرئيسية
                    goToMainActivity();
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(LoginActivity.this, 
                        "خطأ: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    
    /**
     * معالجة إنشاء حساب جديد
     */
    private void handleRegister() {
        String name = nameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();
        
        // التحقق من صحة البيانات
        if (name.isEmpty()) {
            nameInput.setError("أدخل الاسم");
            return;
        }
        
        if (email.isEmpty()) {
            emailInput.setError("أدخل البريد الإلكتروني");
            return;
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("البريد الإلكتروني غير صحيح");
            return;
        }
        
        if (password.isEmpty()) {
            passwordInput.setError("أدخل كلمة المرور");
            return;
        }
        
        if (password.length() < 6) {
            passwordInput.setError("كلمة المرور يجب أن تكون 6 أحرف على الأقل");
            return;
        }
        
        if (!password.equals(confirmPassword)) {
            confirmPasswordInput.setError("كلمتا المرور غير متطابقتين");
            return;
        }
        
        // عرض شريط التحميل
        showLoading(true);
        
        // إنشاء مستخدم جديد
        User newUser = new User();
        newUser.setName(name);
        newUser.setEmail(email);
        newUser.setPassword(password);
        
        // محاولة التسجيل
        dataManager.registerUser(newUser, new DataManager.DataCallback<User>() {
            @Override
            public void onSuccess(User user) {
                runOnUiThread(() -> {
                    showLoading(false);
                    
                    // حفظ حالة تسجيل الدخول
                    saveLoginState(user);
                    
                    Toast.makeText(LoginActivity.this, 
                        "تم إنشاء الحساب بنجاح! مرحباً " + user.getName(), 
                        Toast.LENGTH_SHORT).show();
                    
                    // الانتقال للشاشة الرئيسية
                    goToMainActivity();
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(LoginActivity.this, 
                        "خطأ: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    
    /**
     * عرض/إخفاء شريط التحميل
     */
    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        submitButton.setEnabled(!show);
        guestButton.setEnabled(!show);
    }
    
    /**
     * مسح الحقول
     */
    private void clearFields() {
        nameInput.setText("");
        emailInput.setText("");
        passwordInput.setText("");
        confirmPasswordInput.setText("");
        nameInput.setError(null);
        emailInput.setError(null);
        passwordInput.setError(null);
        confirmPasswordInput.setError(null);
    }
    
    /**
     * حفظ حالة تسجيل الدخول
     */
    private void saveLoginState(User user) {
        UserSession.save(this, user);
    }
    
    /**
     * التحقق إذا كان المستخدم مسجل دخوله
     */
    private boolean isUserLoggedIn() {
        return UserSession.isLoggedIn(this);
    }
    
    /**
     * الانتقال للشاشة الرئيسية
     */
    private void goToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dataManager != null) {
            dataManager.shutdown();
        }
    }
}
