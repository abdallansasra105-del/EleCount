package com.example.elecount;

import android.os.Bundle;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.elecount.Hellper.SystemBarsHelper;

import com.example.elecount.fragments.DevicesFragment;
import com.example.elecount.fragments.HomeFragment;
import com.example.elecount.fragments.SettingsFragment;
import com.example.elecount.fragments.SimulatorFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * النشاط الرئيسي للتطبيق
 * يدير التنقل بين الـ Fragments باستخدام Bottom Navigation
 * 
 * الشرح: هذا الكلاس هو نقطة دخول التطبيق
 * يحتوي على Toolbar علوي و Bottom Navigation سفلي
 * يعرض الـ Fragments المختلفة حسب اختيار المستخدم
 */
public class MainActivity extends AppCompatActivity {
    
    // Toolbar العلوي
    private Toolbar toolbar;
    
    // Bottom Navigation
    private BottomNavigationView bottomNavigation;
    
    // الـ Fragment الحالي المعروض
    private Fragment currentFragment;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // فرض اتجاه RTL للعربية
        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        
        setContentView(R.layout.activity_main);
        
        // توحيد لون شريط الحالة مع الهيدر
        SystemBarsHelper.applyColoredHeader(this, findViewById(R.id.headerContainer));
        
        // إعداد Toolbar
        setupToolbar();
        
        // إعداد Bottom Navigation
        setupBottomNavigation();
        
        // إعداد معالج زر الرجوع (Back Button Handler)
        setupBackPressedHandler();
        
        // عرض الـ Fragment الرئيسي عند بداية التطبيق
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }
    }
    
    /**
     * إعداد Toolbar العلوي
     */
    private void setupToolbar() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        // تعيين عنوان Toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.app_name);
        }
    }
    
    /**
     * إعداد Bottom Navigation وربط الأحداث
     */
    private void setupBottomNavigation() {
        bottomNavigation = findViewById(R.id.bottomNavigation);
        bottomNavigation.setLabelVisibilityMode(
                BottomNavigationView.LABEL_VISIBILITY_LABELED);
        
        // معالج تغيير الاختيار في Bottom Navigation
        bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            String title = getString(R.string.app_name);
            
            // تحديد الـ Fragment حسب العنصر المختار
            int itemId = item.getItemId();
            
            if (itemId == R.id.navigation_home) {
                // الرئيسية
                selectedFragment = new HomeFragment();
                title = getString(R.string.menu_home);
                
            } else if (itemId == R.id.navigation_devices) {
                // الأجهزة
                selectedFragment = new DevicesFragment();
                title = getString(R.string.menu_devices);
                
            } else if (itemId == R.id.navigation_simulator) {
                // المحاكي
                selectedFragment = new SimulatorFragment();
                title = getString(R.string.menu_simulator);
                
            } else if (itemId == R.id.navigation_settings) {
                // الإعدادات
                selectedFragment = new SettingsFragment();
                title = getString(R.string.menu_settings);
            }
            
            // تحميل الـ Fragment المختار
            if (selectedFragment != null) {
                loadFragment(selectedFragment);
                
                // تحديث عنوان Toolbar
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(title);
                }
                
                return true;
            }
            
            return false;
        });
    }
    
    /**
     * إعداد معالج زر الرجوع باستخدام OnBackPressedDispatcher الحديث
     * 
     * الشرح: هذه الطريقة الحديثة لمعالجة زر الرجوع في Android
     * تستخدم OnBackPressedCallback بدلاً من onBackPressed() القديمة
     * تدعم back gestures (الإيماءات) في Android 10 وما فوق
     */
    private void setupBackPressedHandler() {
        // إنشاء callback جديد لمعالجة زر الرجوع
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // إذا كان هناك أكثر من fragment في back stack
                if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    // الرجوع للـ fragment السابق
                    getSupportFragmentManager().popBackStack();
                } else {
                    // إذا كنا في الصفحة الرئيسية، تعطيل الـ callback
                    // والسماح للنظام بالخروج من التطبيق
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        };
        
        // تسجيل الـ callback مع OnBackPressedDispatcher
        getOnBackPressedDispatcher().addCallback(this, callback);
    }
    
    /**
     * تحميل Fragment في الـ Container
     * @param fragment الـ Fragment المراد عرضه
     */
    private void loadFragment(Fragment fragment) {
        // بدء transaction لتغيير الـ Fragment
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        
        // استبدال الـ Fragment الحالي بالجديد
        transaction.replace(R.id.fragmentContainer, fragment);
        
        // إضافة إلى back stack (للسماح بالرجوع)
        if (currentFragment != null) {
            transaction.addToBackStack(null);
        }
        
        // تنفيذ التغيير
        transaction.commit();
        
        // حفظ الـ Fragment الحالي
        currentFragment = fragment;
    }
}
