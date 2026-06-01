package com.example.elecount;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.elecount.Hellper.SystemBarsHelper;
import com.example.elecount.Hellper.UserSession;

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

    public static final String EXTRA_GUEST_MODE = "guest_mode";
    
    // Toolbar العلوي
    private Toolbar toolbar;
    
    // Bottom Navigation
    private BottomNavigationView bottomNavigation;
    
    // الـ Fragment الحالي المعروض
    private Fragment currentFragment;

    private boolean guestMode;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        guestMode = getIntent().getBooleanExtra(EXTRA_GUEST_MODE, false)
                || UserSession.isGuest(this);
        
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
        
        // عرض الشاشة المناسبة عند بداية التطبيق
        if (savedInstanceState == null) {
            if (guestMode) {
                loadFragment(new SimulatorFragment(), false);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(R.string.menu_simulator);
                }
            } else {
                loadFragment(new HomeFragment(), false);
            }
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

        if (guestMode) {
            bottomNavigation.setVisibility(View.GONE);
            return;
        }

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
                loadFragment(selectedFragment, true);
                
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
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (guestMode) {
                    openLoginScreen();
                    return;
                }
                if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    getSupportFragmentManager().popBackStack();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        };

        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    /** العودة لشاشة تسجيل الدخول (من وضع الضيف) */
    public void openLoginScreen() {
        UserSession.clear(this);
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (guestMode) {
            getMenuInflater().inflate(R.menu.menu_guest, menu);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (guestMode && item.getItemId() == R.id.action_guest_login) {
            openLoginScreen();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void loadFragment(Fragment fragment) {
        loadFragment(fragment, true);
    }

    /**
     * تحميل Fragment في الـ Container
     * @param fragment الـ Fragment المراد عرضه
     * @param addToBackStack إضافة إلى back stack
     */
    private void loadFragment(Fragment fragment, boolean addToBackStack) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, fragment);

        if (addToBackStack && currentFragment != null) {
            transaction.addToBackStack(null);
        }

        transaction.commit();
        currentFragment = fragment;
    }
}
