package com.example.elecount.models;

/**
 * نموذج بيانات التصنيف (القسم)
 * يستخدم لتنظيم الأجهزة الكهربائية في مجموعات
 * 
 * الشرح: هذا الكلاس يمثل تصنيف الأجهزة (مثل: غرفة النوم، المطبخ، الصالة)
 * كل تصنيف له اسم وعدد الأجهزة التابعة له
 */
public class Category {
    // المعرف الفريد للتصنيف
    private String id;
    
    // اسم التصنيف (مثل: المطبخ، غرفة النوم، الصالة)
    private String name;
    
    // عدد الأجهزة في هذا التصنيف - يتم حسابه تلقائياً
    private int deviceCount;
    
    // معرف المستخدم الذي يملك هذا التصنيف
    private String userId;
    
    // تاريخ إنشاء التصنيف
    private long createdAt;
    
    /**
     * المنشئ الفارغ - مطلوب لـ Gson
     */
    public Category() {
        this.deviceCount = 0;
        this.createdAt = System.currentTimeMillis();
    }
    
    /**
     * المنشئ مع الاسم
     * @param name اسم التصنيف
     */
    public Category(String name) {
        this.name = name;
        this.deviceCount = 0;
        this.createdAt = System.currentTimeMillis();
    }
    
    /**
     * المنشئ الكامل
     * @param name اسم التصنيف
     * @param userId معرف المستخدم
     */
    public Category(String name, String userId) {
        this.name = name;
        this.userId = userId;
        this.deviceCount = 0;
        this.createdAt = System.currentTimeMillis();
    }
    
    // === Getters & Setters ===
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public int getDeviceCount() {
        return deviceCount;
    }
    
    public void setDeviceCount(int deviceCount) {
        this.deviceCount = deviceCount;
    }
    
    /**
     * زيادة عدد الأجهزة بمقدار 1
     * يستخدم عند إضافة جهاز جديد للتصنيف
     */
    public void incrementDeviceCount() {
        this.deviceCount++;
    }
    
    /**
     * تقليل عدد الأجهزة بمقدار 1
     * يستخدم عند حذف جهاز من التصنيف
     */
    public void decrementDeviceCount() {
        if (this.deviceCount > 0) {
            this.deviceCount--;
        }
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
    
    @Override
    public String toString() {
        return "Category{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", deviceCount=" + deviceCount +
                '}';
    }
}
