package com.example.elecount.Hellper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import android.widget.ImageView;

import com.example.elecount.models.Category;
import com.example.elecount.models.Device;
import com.example.elecount.models.User;
import com.example.elecount.models.UsageRecord;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * مدير البيانات الرئيسي
 * يدير جميع عمليات الحفظ والاستعلام من قاعدة البيانات Appwrite
 * أو التخزين المحلي في حالة عدم توفر Appwrite
 * 
 * الشرح: هذا الكلاس يوفر دوال بسيطة لإدارة البيانات
 * يعمل مع Appwrite أو تخزين محلي بديل
 * جميع العمليات تعمل بشكل آمن باستخدام Threads لتجنب تجميد التطبيق
 */
public class DataManager {
    private static final String TAG = "DataManager";
    
    // أسماء الجداول في Appwrite - يجب أن تكون موجودة في قاعدة البيانات
    private static final String TABLE_USERS = "users";
    private static final String TABLE_CATEGORIES = "categories";
    private static final String TABLE_DEVICES = "devices";
    private static final String TABLE_USAGE_RECORDS = "usage_records";
    
    // مرجع لطبقة الاتصال مع Appwrite
    private AppWriteConn appWriteConn;
    
    // ExecutorService لتنفيذ العمليات الشبكية في background thread
    private ExecutorService executor;
    
    // المستخدم الحالي المسجل دخوله
    private User currentUser;
    
    // علم لمعرفة ما إذا كان Appwrite متاحاً
    private boolean appwriteAvailable = false;
    
    // علم لتجنب الرسائل المتكررة
    private boolean errorShown = false;
    
    /**
     * المنشئ
     * @param context سياق التطبيق
     */
    public DataManager(Context context) {
        this.appWriteConn = new AppWriteConn(context);
        this.executor = Executors.newFixedThreadPool(3); // 3 threads للعمليات المتوازية
        UserSession.applyToDataManager(this, context);
        
        // محاولة الاتصال بـ Appwrite
        checkAppwriteConnection();
    }
    
    /**
     * فحص اتصال Appwrite
     */
    private void checkAppwriteConnection() {
        executor.execute(() -> {
            try {
                AppWriteConn.OperationResult<ArrayList<Device>> result = 
                    appWriteConn.getData(TABLE_DEVICES, TABLE_DEVICES, Device.class);
                appwriteAvailable = result.success;
                if (appwriteAvailable) {
                    Log.d(TAG, "✓ Appwrite متصل");
                } else {
                    Log.w(TAG, "⚠ Appwrite غير متاح - استخدام التخزين المحلي");
                }
            } catch (Exception e) {
                appwriteAvailable = false;
                Log.w(TAG, "⚠ Appwrite غير متاح - استخدام التخزين المحلي");
            }
        });
    }
    
    /**
     * التحقق من توفر Appwrite
     * @return true إذا كان Appwrite متاحاً
     */
    public boolean isAppwriteAvailable() {
        return appwriteAvailable;
    }
    
    /**
     * واجهة Callback للحصول على نتيجة العملية بشكل غير متزامن
     * @param <T> نوع البيانات المرتجعة
     */
    public interface DataCallback<T> {
        /**
         * يتم استدعاؤها عند نجاح العملية
         * @param result البيانات المرتجعة
         */
        void onSuccess(T result);
        
        /**
         * يُستدعى عند النجاح مع تحذير اختياري (مثلاً حقول لم تُحفظ في Appwrite)
         */
        default void onSuccess(T result, String warning) {
            onSuccess(result);
        }
        
        /**
         * يتم استدعاؤها عند فشل العملية
         * @param error رسالة الخطأ
         */
        void onError(String error);
    }
    
    // ========== إدارة المستخدمين ==========
    
    /**
     * تسجيل مستخدم جديد
     * @param user بيانات المستخدم
     * @param callback الرد على النتيجة
     * 
     * الشرح: يقوم بإنشاء مستخدم جديد في النظام وحفظه في السحابة
     */
    public void registerUser(User user, DataCallback<User> callback) {
        executor.execute(() -> {
            try {
                // إنشاء ID فريد للمستخدم
                if (user.getId() == null || user.getId().isEmpty()) {
                    user.setId(UUID.randomUUID().toString());
                }
                
                // حفظ المستخدم في Appwrite
                AppWriteConn.OperationResult<ArrayList<User>> result = 
                    appWriteConn.saveData(user, TABLE_USERS, TABLE_USERS);
                
                if (result.success && result.data != null && !result.data.isEmpty()) {
                    User savedUser = result.data.get(0);
                    if (savedUser.getId() == null || savedUser.getId().isEmpty()) {
                        savedUser.setId(user.getId());
                    }
                    currentUser = savedUser;
                    Log.d(TAG, "تم تسجيل المستخدم: " + savedUser.getName());
                    callback.onSuccess(savedUser);
                } else {
                    Log.e(TAG, "فشل تسجيل المستخدم: " + result.message);
                    callback.onError(result.message);
                }
            } catch (Exception e) {
                Log.e(TAG, "خطأ في تسجيل المستخدم", e);
                callback.onError("خطأ: " + e.getMessage());
            }
        });
    }
    
    /**
     * تسجيل دخول المستخدم (بحث بسيط - في الإنتاج استخدم authentication حقيقي)
     * @param email البريد الإلكتروني
     * @param password كلمة المرور
     * @param callback الرد على النتيجة
     */
    public void loginUser(String email, String password, DataCallback<User> callback) {
        executor.execute(() -> {
            try {
                // جلب جميع المستخدمين والبحث عن المطابق
                AppWriteConn.OperationResult<ArrayList<User>> result = 
                    appWriteConn.getData(TABLE_USERS, TABLE_USERS, User.class);
                
                if (result.success && result.data != null) {
                    for (User user : result.data) {
                        if (email.equals(user.getEmail()) && password.equals(user.getPassword())) {
                            currentUser = user;
                            Log.d(TAG, "تم تسجيل الدخول: " + user.getName());
                            callback.onSuccess(user);
                            return;
                        }
                    }
                    callback.onError("البريد الإلكتروني أو كلمة المرور خاطئة");
                } else {
                    callback.onError("فشل الاتصال بالخادم");
                }
            } catch (Exception e) {
                Log.e(TAG, "خطأ في تسجيل الدخول", e);
                callback.onError("خطأ: " + e.getMessage());
            }
        });
    }
    
    /**
     * الحصول على المستخدم الحالي
     */
    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * تعيين المستخدم الحالي (بعد تسجيل الدخول أو استعادة الجلسة)
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    /**
     * البحث عن مستخدم بالبريد الإلكتروني في Appwrite
     */
    private User findUserByEmail(String email) {
        if (email == null || email.isEmpty()) {
            return null;
        }
        AppWriteConn.OperationResult<ArrayList<User>> result =
                appWriteConn.getData(TABLE_USERS, TABLE_USERS, User.class);
        if (result.success && result.data != null) {
            for (User user : result.data) {
                if (email.equalsIgnoreCase(user.getEmail())) {
                    return user;
                }
            }
        }
        return null;
    }

    /**
     * استكمال معرف المستخدم للجلسات القديمة التي لا تحتوي userId
     */
    public void resolveSessionUser(android.content.Context context, DataCallback<User> callback) {
        executor.execute(() -> {
            try {
                User session = UserSession.load(context);
                if (session == null) {
                    callback.onError("غير مسجل");
                    return;
                }

                User resolved = session;
                if (session.getId() == null || session.getId().isEmpty()) {
                    User fromCloud = findUserByEmail(session.getEmail());
                    if (fromCloud == null) {
                        callback.onError("لم يُعثر على الحساب — سجّل خروجاً ثم ادخل مجدداً");
                        return;
                    }
                    if (session.getName() != null && !session.getName().isEmpty()) {
                        fromCloud.setName(session.getName());
                    }
                    if (session.getImageUrl() != null && !session.getImageUrl().isEmpty()) {
                        fromCloud.setImageUrl(session.getImageUrl());
                    }
                    resolved = fromCloud;
                    UserSession.save(context, resolved);
                    Log.d(TAG, "تم استكمال معرف المستخدم: " + resolved.getId());
                }

                currentUser = resolved;
                callback.onSuccess(resolved);
            } catch (Exception e) {
                Log.e(TAG, "خطأ في استكمال جلسة المستخدم", e);
                callback.onError("خطأ: " + e.getMessage());
            }
        });
    }

    /**
     * تحديث الملف الشخصي (الاسم والصورة) دون المساس بكلمة المرور
     */
    public void updateUserProfile(User user, DataCallback<User> callback) {
        executor.execute(() -> {
            try {
                if (user == null) {
                    callback.onError("بيانات المستخدم غير موجودة");
                    return;
                }

                User workingUser = user;
                if (workingUser.getId() == null || workingUser.getId().isEmpty()) {
                    User fromCloud = findUserByEmail(workingUser.getEmail());
                    if (fromCloud == null) {
                        callback.onError("معرف المستخدم غير موجود — سجّل خروجاً ثم ادخل مجدداً");
                        return;
                    }
                    fromCloud.setName(workingUser.getName());
                    if (workingUser.getImageUrl() != null) {
                        fromCloud.setImageUrl(workingUser.getImageUrl());
                    }
                    workingUser = fromCloud;
                }

                AppWriteConn.OperationResult<User> fetchResult =
                        appWriteConn.getDataById(TABLE_USERS, workingUser.getId(), TABLE_USERS, User.class);

                User toUpdate;
                if (fetchResult.success && fetchResult.data != null
                        && fetchResult.data.getId() != null && !fetchResult.data.getId().isEmpty()) {
                    toUpdate = fetchResult.data;
                } else if (fetchResult.success && fetchResult.data != null) {
                    toUpdate = fetchResult.data;
                    toUpdate.setId(workingUser.getId());
                } else if (currentUser != null && workingUser.getId().equals(currentUser.getId())) {
                    toUpdate = currentUser;
                } else {
                    toUpdate = workingUser;
                }

                if (workingUser.getName() != null && !workingUser.getName().trim().isEmpty()) {
                    toUpdate.setName(workingUser.getName().trim());
                }
                if (workingUser.getImageUrl() != null) {
                    toUpdate.setImageUrl(workingUser.getImageUrl());
                }

                AppWriteConn.OperationResult<User> result =
                        appWriteConn.updateData(toUpdate, TABLE_USERS, workingUser.getId(), TABLE_USERS);

                if (result.success && result.data != null) {
                    User saved = result.data;
                    saved.setId(workingUser.getId());
                    currentUser = saved;
                    Log.d(TAG, "تم تحديث الملف الشخصي: " + saved.getName());
                    callback.onSuccess(saved);
                } else {
                    callback.onError(result.message != null ? result.message : "فشل تحديث الملف الشخصي");
                }
            } catch (Exception e) {
                Log.e(TAG, "خطأ في تحديث الملف الشخصي", e);
                callback.onError("خطأ: " + e.getMessage());
            }
        });
    }
    
    /**
     * تسجيل الخروج
     */
    public void logout() {
        currentUser = null;
        Log.d(TAG, "تم تسجيل الخروج");
    }
    
    // ========== إدارة التصنيفات ==========
    
    /**
     * إضافة تصنيف جديد
     * @param category التصنيف
     * @param callback الرد على النتيجة
     */
    public void addCategory(Category category, DataCallback<Category> callback) {
        executor.execute(() -> {
            try {
                if (category.getId() == null || category.getId().isEmpty()) {
                    category.setId(UUID.randomUUID().toString());
                }
                
                // ربط التصنيف بالمستخدم الحالي
                if (currentUser != null) {
                    category.setUserId(currentUser.getId());
                }
                
                AppWriteConn.OperationResult<ArrayList<Category>> result = 
                    appWriteConn.saveData(category, TABLE_CATEGORIES, TABLE_CATEGORIES);
                
                if (result.success && result.data != null && !result.data.isEmpty()) {
                    Category savedCategory = result.data.get(0);
                    Log.d(TAG, "تم إضافة التصنيف: " + savedCategory.getName());
                    callback.onSuccess(savedCategory);
                } else {
                    callback.onError(result.message);
                }
            } catch (Exception e) {
                Log.e(TAG, "خطأ في إضافة التصنيف", e);
                callback.onError("خطأ: " + e.getMessage());
            }
        });
    }
    
    /**
     * جلب جميع التصنيفات
     * @param callback الرد على النتيجة
     */
    public void getAllCategories(DataCallback<ArrayList<Category>> callback) {
        executor.execute(() -> {
            try {
                AppWriteConn.OperationResult<ArrayList<Category>> result = 
                    appWriteConn.getData(TABLE_CATEGORIES, TABLE_CATEGORIES, Category.class);
                
                if (result.success && result.data != null) {
                    // تصفية التصنيفات حسب المستخدم الحالي
                    ArrayList<Category> userCategories = new ArrayList<>();
                    if (currentUser != null) {
                        for (Category category : result.data) {
                            if (currentUser.getId().equals(category.getUserId())) {
                                userCategories.add(category);
                            }
                        }
                        callback.onSuccess(userCategories);
                    } else {
                        // في وضع المحاكي، إرجاع جميع التصنيفات
                        callback.onSuccess(result.data);
                    }
                } else {
                    callback.onError(result.message);
                }
            } catch (Exception e) {
                Log.e(TAG, "خطأ في جلب التصنيفات", e);
                callback.onError("خطأ: " + e.getMessage());
            }
        });
    }
    
    /**
     * تحديث تصنيف موجود
     * @param category التصنيف المحدث
     * @param callback الرد على النتيجة
     */
    public void updateCategory(Category category, DataCallback<Category> callback) {
        executor.execute(() -> {
            try {
                AppWriteConn.OperationResult<Category> result = 
                    appWriteConn.updateData(category, TABLE_CATEGORIES, category.getId(), TABLE_CATEGORIES);
                
                if (result.success && result.data != null) {
                    Log.d(TAG, "تم تحديث التصنيف: " + category.getName());
                    callback.onSuccess(result.data);
                } else {
                    callback.onError(result.message);
                }
            } catch (Exception e) {
                Log.e(TAG, "خطأ في تحديث التصنيف", e);
                callback.onError("خطأ: " + e.getMessage());
            }
        });
    }
    
    /**
     * حذف تصنيف
     * @param categoryId معرف التصنيف
     * @param callback الرد على النتيجة
     */
    public void deleteCategory(String categoryId, DataCallback<Void> callback) {
        executor.execute(() -> {
            try {
                AppWriteConn.OperationResult<Void> result = 
                    appWriteConn.deleteData(TABLE_CATEGORIES, categoryId, TABLE_CATEGORIES);
                
                if (result.success) {
                    Log.d(TAG, "تم حذف التصنيف");
                    callback.onSuccess(null);
                } else {
                    callback.onError(result.message);
                }
            } catch (Exception e) {
                Log.e(TAG, "خطأ في حذف التصنيف", e);
                callback.onError("خطأ: " + e.getMessage());
            }
        });
    }
    
    // ========== إدارة الأجهزة ==========
    
    /**
     * إضافة جهاز جديد
     * @param device الجهاز
     * @param callback الرد على النتيجة
     */
    public void addDevice(Device device, DataCallback<Device> callback) {
        executor.execute(() -> {
            try {
                if (device.getId() == null || device.getId().isEmpty()) {
                    device.setId(UUID.randomUUID().toString());
                }
                
                // ربط الجهاز بالمستخدم الحالي
                if (currentUser != null) {
                    device.setUserId(currentUser.getId());
                }
                
                AppWriteConn.OperationResult<ArrayList<Device>> result = 
                    appWriteConn.saveData(device, TABLE_DEVICES, TABLE_DEVICES);
                
                if (result.success && result.data != null && !result.data.isEmpty()) {
                    Device savedDevice = result.data.get(0);
                    Log.d(TAG, "تم إضافة الجهاز: " + savedDevice.getName());
                    
                    // تحديث عدد الأجهزة في التصنيف
                    updateCategoryDeviceCount(device.getCategoryId(), 1);
                    
                    if (result.warning != null && !result.warning.isEmpty()) {
                        callback.onSuccess(savedDevice, result.warning);
                    } else {
                        callback.onSuccess(savedDevice);
                    }
                } else {
                    callback.onError(result.message);
                }
            } catch (Exception e) {
                Log.e(TAG, "خطأ في إضافة الجهاز", e);
                callback.onError("خطأ: " + e.getMessage());
            }
        });
    }
    
    /**
     * جلب جميع الأجهزة
     * @param callback الرد على النتيجة
     */
    public void getAllDevices(DataCallback<ArrayList<Device>> callback) {
        executor.execute(() -> {
            try {
                AppWriteConn.OperationResult<ArrayList<Device>> result = 
                    appWriteConn.getData(TABLE_DEVICES, TABLE_DEVICES, Device.class);
                
                if (result.success && result.data != null) {
                    // تصفية الأجهزة حسب المستخدم الحالي
                    ArrayList<Device> userDevices = new ArrayList<>();
                    if (currentUser != null) {
                        for (Device device : result.data) {
                            if (currentUser.getId().equals(device.getUserId())) {
                                userDevices.add(device);
                            }
                        }
                        callback.onSuccess(userDevices);
                    } else {
                        // في وضع المحاكي، إرجاع جميع الأجهزة
                        callback.onSuccess(result.data);
                    }
                } else {
                    callback.onError(result.message);
                }
            } catch (Exception e) {
                Log.e(TAG, "خطأ في جلب الأجهزة", e);
                callback.onError("خطأ: " + e.getMessage());
            }
        });
    }
    
    /**
     * جلب أجهزة تصنيف محدد
     * @param categoryId معرف التصنيف
     * @param callback الرد على النتيجة
     */
    public void getDevicesByCategory(String categoryId, DataCallback<ArrayList<Device>> callback) {
        executor.execute(() -> {
            try {
                AppWriteConn.OperationResult<ArrayList<Device>> result = 
                    appWriteConn.getData(TABLE_DEVICES, TABLE_DEVICES, Device.class);
                
                if (result.success && result.data != null) {
                    ArrayList<Device> categoryDevices = new ArrayList<>();
                    for (Device device : result.data) {
                        if (categoryId.equals(device.getCategoryId())) {
                            categoryDevices.add(device);
                        }
                    }
                    callback.onSuccess(categoryDevices);
                } else {
                    callback.onError(result.message);
                }
            } catch (Exception e) {
                Log.e(TAG, "خطأ في جلب أجهزة التصنيف", e);
                callback.onError("خطأ: " + e.getMessage());
            }
        });
    }
    
    /**
     * تحديث جهاز موجود
     * @param device الجهاز المحدث
     * @param callback الرد على النتيجة
     */
    public void updateDevice(Device device, DataCallback<Device> callback) {
        executor.execute(() -> {
            try {
                if (device.getId() == null || device.getId().isEmpty()) {
                    Log.e(TAG, "updateDevice: معرف الجهاز فارغ");
                    callback.onError("معرف الجهاز غير موجود");
                    return;
                }
                
                Log.d(TAG, "updateDevice: id=" + device.getId() + " name=" + device.getName());
                
                AppWriteConn.OperationResult<Device> result = 
                    appWriteConn.updateData(device, TABLE_DEVICES, device.getId(), TABLE_DEVICES);
                
                if (result.success && result.data != null) {
                    Log.d(TAG, "تم تحديث الجهاز: " + device.getName());
                    if (result.warning != null && !result.warning.isEmpty()) {
                        callback.onSuccess(result.data, result.warning);
                    } else {
                        callback.onSuccess(result.data);
                    }
                } else {
                    Log.e(TAG, "فشل تحديث الجهاز: " + result.message);
                    callback.onError(result.message != null ? result.message : "فشل تحديث الجهاز");
                }
            } catch (Exception e) {
                Log.e(TAG, "خطأ في تحديث الجهاز", e);
                callback.onError("خطأ: " + e.getMessage());
            }
        });
    }
    
    /**
     * حذف جهاز
     * @param device الجهاز المراد حذفه
     * @param callback الرد على النتيجة
     */
    public void deleteDevice(Device device, DataCallback<Void> callback) {
        executor.execute(() -> {
            try {
                AppWriteConn.OperationResult<Void> result = 
                    appWriteConn.deleteData(TABLE_DEVICES, device.getId(), TABLE_DEVICES);
                
                if (result.success) {
                    Log.d(TAG, "تم حذف الجهاز");
                    
                    // تحديث عدد الأجهزة في التصنيف
                    updateCategoryDeviceCount(device.getCategoryId(), -1);
                    
                    callback.onSuccess(null);
                } else {
                    callback.onError(result.message);
                }
            } catch (Exception e) {
                Log.e(TAG, "خطأ في حذف الجهاز", e);
                callback.onError("خطأ: " + e.getMessage());
            }
        });
    }
    
    // ========== إدارة سجلات الاستخدام ==========
    
    /**
     * حفظ سجل استخدام جديد
     * @param record السجل
     * @param callback الرد على النتيجة
     */
    public void saveUsageRecord(UsageRecord record, DataCallback<UsageRecord> callback) {
        executor.execute(() -> {
            try {
                if (record.getId() == null || record.getId().isEmpty()) {
                    record.setId(UUID.randomUUID().toString());
                }
                
                AppWriteConn.OperationResult<ArrayList<UsageRecord>> result = 
                    appWriteConn.saveData(record, TABLE_USAGE_RECORDS, TABLE_USAGE_RECORDS);
                
                if (result.success && result.data != null && !result.data.isEmpty()) {
                    UsageRecord savedRecord = result.data.get(0);
                    Log.d(TAG, "تم حفظ سجل الاستخدام: deviceId=" + record.getDeviceId()
                            + " cost=" + record.getCost());
                    callback.onSuccess(savedRecord);
                } else {
                    Log.e(TAG, "فشل حفظ سجل الاستخدام: " + result.message);
                    callback.onError(result.message != null ? result.message : "فشل حفظ السجل");
                }
            } catch (Exception e) {
                Log.e(TAG, "خطأ في حفظ سجل الاستخدام", e);
                callback.onError("خطأ: " + e.getMessage());
            }
        });
    }
    
    /**
     * جلب سجلات استخدام جهاز محدد
     * @param deviceId معرف الجهاز
     * @param callback الرد على النتيجة
     */
    public void getUsageRecordsByDevice(String deviceId, DataCallback<ArrayList<UsageRecord>> callback) {
        executor.execute(() -> {
            try {
                AppWriteConn.OperationResult<ArrayList<UsageRecord>> result = 
                    appWriteConn.getData(TABLE_USAGE_RECORDS, TABLE_USAGE_RECORDS, UsageRecord.class);
                
                if (result.success && result.data != null) {
                    ArrayList<UsageRecord> deviceRecords = new ArrayList<>();
                    for (UsageRecord record : result.data) {
                        if (deviceId.equals(record.getDeviceId())) {
                            deviceRecords.add(record);
                        }
                    }
                    callback.onSuccess(deviceRecords);
                } else {
                    callback.onError(result.message);
                }
            } catch (Exception e) {
                Log.e(TAG, "خطأ في جلب سجلات الاستخدام", e);
                callback.onError("خطأ: " + e.getMessage());
            }
        });
    }
    
    /**
     * جلب جميع سجلات الاستخدام للمستخدم الحالي
     * @param callback الرد على النتيجة
     */
    public void getAllUsageRecords(DataCallback<ArrayList<UsageRecord>> callback) {
        executor.execute(() -> {
            try {
                AppWriteConn.OperationResult<ArrayList<UsageRecord>> result = 
                    appWriteConn.getData(TABLE_USAGE_RECORDS, TABLE_USAGE_RECORDS, UsageRecord.class);
                
                if (result.success && result.data != null) {
                    if (currentUser != null) {
                        ArrayList<UsageRecord> userRecords = new ArrayList<>();
                        for (UsageRecord record : result.data) {
                            if (currentUser.getId().equals(record.getUserId())) {
                                userRecords.add(record);
                            }
                        }
                        callback.onSuccess(userRecords);
                    } else {
                        callback.onSuccess(result.data);
                    }
                } else {
                    callback.onError(result.message);
                }
            } catch (Exception e) {
                Log.e(TAG, "خطأ في جلب سجلات الاستخدام", e);
                callback.onError("خطأ: " + e.getMessage());
            }
        });
    }
    
    // ========== رفع الصور ==========
    
    /**
     * تحميل صورة من Appwrite Storage وعرضها في ImageView
     */
    public void loadImageIntoView(String imageUrl, ImageView imageView, int placeholderResId) {
        if (imageView == null) {
            return;
        }
        if (imageUrl == null || imageUrl.isEmpty()) {
            imageView.setImageResource(placeholderResId);
            return;
        }
        
        imageView.setTag(imageUrl);
        imageView.setImageResource(placeholderResId);
        
        executor.execute(() -> {
            byte[] imageData = appWriteConn.downloadStorageFile(imageUrl);
            if (imageData == null || imageData.length == 0) {
                Log.w(TAG, "loadImageIntoView: empty data for " + imageUrl);
                return;
            }
            
            Bitmap bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
            if (bitmap == null) {
                Log.w(TAG, "loadImageIntoView: decode failed for " + imageUrl);
                return;
            }
            
            imageView.post(() -> {
                if (imageUrl.equals(imageView.getTag())) {
                    imageView.setImageBitmap(bitmap);
                }
            });
        });
    }
    
    /**
     * رفع صورة إلى Appwrite Storage
     * @param context سياق التطبيق
     * @param imageUri رابط الصورة المحلية
     * @param callback يرجع رابط الصورة في السحابة
     */
    public void uploadImage(Context context, Uri imageUri, DataCallback<String> callback) {
        executor.execute(() -> {
            try {
                InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
                if (inputStream == null) {
                    callback.onError("تعذر قراءة الصورة");
                    return;
                }
                
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                byte[] data = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(data)) != -1) {
                    buffer.write(data, 0, bytesRead);
                }
                inputStream.close();
                
                byte[] imageBytes = buffer.toByteArray();
                String fileName = "device_" + System.currentTimeMillis() + ".jpg";
                
                AppWriteConn.OperationResult<AppWriteConn.FileInfo> result =
                        appWriteConn.uploadFile(imageBytes, fileName, "image/jpeg", null);
                
                if (result.success && result.data != null && result.data.fileUrl != null) {
                    callback.onSuccess(result.data.fileUrl);
                } else {
                    callback.onError(result.message != null ? result.message : "فشل رفع الصورة");
                }
            } catch (Exception e) {
                Log.e(TAG, "خطأ في رفع الصورة", e);
                callback.onError("خطأ في رفع الصورة: " + e.getMessage());
            }
        });
    }
    
    // ========== دوال مساعدة ==========
    
    /**
     * تحديث عدد الأجهزة في التصنيف
     * @param categoryId معرف التصنيف
     * @param change التغيير (+1 للإضافة، -1 للحذف)
     */
    private void updateCategoryDeviceCount(String categoryId, int change) {
        if (categoryId == null || categoryId.isEmpty()) {
            return;
        }
        
        executor.execute(() -> {
            try {
                AppWriteConn.OperationResult<Category> result = 
                    appWriteConn.getDataById(TABLE_CATEGORIES, categoryId, TABLE_CATEGORIES, Category.class);
                
                if (result.success && result.data != null) {
                    Category category = result.data;
                    category.setDeviceCount(category.getDeviceCount() + change);
                    
                    appWriteConn.updateData(category, TABLE_CATEGORIES, categoryId, TABLE_CATEGORIES);
                }
            } catch (Exception e) {
                Log.e(TAG, "خطأ في تحديث عدد الأجهزة", e);
            }
        });
    }
    
    /**
     * إغلاق ExecutorService عند انتهاء استخدام DataManager
     */
    public void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
