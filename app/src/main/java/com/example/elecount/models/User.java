package com.example.elecount.models;

/**
 * نموذج بيانات المستخدم
 * يحتوي على جميع معلومات المستخدم الأساسية
 * 
 * الشرح: هذا الكلاس يمثل المستخدم في النظام
 * كل مستخدم له معرف فريد، اسم، بريد إلكتروني، كلمة سر، وصورة شخصية
 */
public class User {
    // المعرف الفريد للمستخدم - يتم إنشاؤه تلقائياً
    private String id;
    
    // اسم المستخدم الكامل
    private String name;
    
    // البريد الإلكتروني - يستخدم لتسجيل الدخول
    private String email;
    
    // كلمة المرور - يجب تشفيرها في الإنتاج الحقيقي
    private String password;
    
    // رابط صورة المستخدم في التخزين السحابي
    private String imageUrl;

    // سعر الكيلو واط الافتراضي للمستخدم (شيكل)
    private double pricePerKw;
    
    // تاريخ إنشاء الحساب
    private long createdAt;
    
    /**
     * المنشئ الفارغ - مطلوب لتحويل البيانات من JSON
     */
    public User() {
        // يتم استخدامه من قبل Gson لإنشاء الكائن من JSON
        this.createdAt = System.currentTimeMillis();
        this.pricePerKw = 0.60;
    }
    
    /**
     * المنشئ مع البيانات الأساسية
     * @param name اسم المستخدم
     * @param email البريد الإلكتروني
     * @param password كلمة المرور
     */
    public User(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.createdAt = System.currentTimeMillis();
        this.pricePerKw = 0.60;
    }
    
    // === Getters & Setters ===
    // هذه الدوال تسمح بقراءة وتعديل بيانات المستخدم
    
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
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public double getPricePerKw() {
        return pricePerKw > 0 ? pricePerKw : 0.60;
    }

    public void setPricePerKw(double pricePerKw) {
        this.pricePerKw = pricePerKw;
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
    
    /**
     * تحويل المستخدم إلى نص للطباعة
     */
    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}
