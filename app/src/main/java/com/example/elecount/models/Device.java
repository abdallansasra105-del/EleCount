package com.example.elecount.models;

import com.google.gson.annotations.SerializedName;

/**
 * نموذج بيانات الجهاز الكهربائي
 * يحتوي على جميع معلومات الجهاز الكهربائي
 * 
 * الشرح: هذا الكلاس يمثل جهاز كهربائي (مثل: ثلاجة، تلفاز، مكيف)
 * كل جهاز له اسم، تصنيف، صورة، واستهلاك للطاقة بالكيلو واط
 */
public class Device {
    // المعرف الفريد للجهاز
    private String id;
    
    // اسم الجهاز (مثل: ثلاجة سامسونج، تلفاز LG)
    private String name;
    
    // معرف التصنيف التابع له الجهاز
    private String categoryId;
    
    // اسم التصنيف (للعرض فقط)
    private String categoryName;
    
    // رابط صورة الجهاز في التخزين السحابي
    private String imageUrl;
    
    // استهلاك الطاقة بالكيلو واط في الساعة (kWh)
    // مثال: ثلاجة = 0.15 kWh، مكيف = 2.5 kWh
    private double powerConsumptionKw;
    
    // سعر الكيلو واط الواحد (يمكن تخصيصه حسب الدولة)
    // مثال: في فلسطين = 0.60 شيكل للكيلو واط تقريباً
    private double pricePerKw;
    
    // معرف المستخدم الذي يملك هذا الجهاز
    private String userId;
    
    // حالة الجهاز: هل يعمل حالياً أم لا
    @SerializedName("isRunning")
    private boolean isRunning;
    
    // وقت بدء التشغيل (timestamp) - للحساب الحالي
    private long startTime;
    
    // إجمالي ساعات التشغيل
    private double totalHoursUsed;
    
    // تاريخ إنشاء الجهاز
    private long createdAt;
    
    /**
     * المنشئ الفارغ
     */
    public Device() {
        this.isRunning = false;
        this.totalHoursUsed = 0;
        this.pricePerKw = 0.60; // السعر الافتراضي (شيكل/كيلو واط)
        this.createdAt = System.currentTimeMillis();
    }
    
    /**
     * المنشئ مع البيانات الأساسية
     * @param name اسم الجهاز
     * @param categoryId معرف التصنيف
     * @param powerConsumptionKw استهلاك الطاقة بالكيلو واط
     */
    public Device(String name, String categoryId, double powerConsumptionKw) {
        this.name = name;
        this.categoryId = categoryId;
        this.powerConsumptionKw = powerConsumptionKw;
        this.isRunning = false;
        this.totalHoursUsed = 0;
        this.pricePerKw = 0.60; // السعر الافتراضي (شيكل)
        this.createdAt = System.currentTimeMillis();
    }
    
    /**
     * بدء تشغيل الجهاز
     * يسجل وقت البدء لحساب التكلفة لاحقاً
     */
    public void startDevice() {
        if (!isRunning) {
            this.isRunning = true;
            this.startTime = System.currentTimeMillis();
        }
    }
    
    /**
     * إيقاف تشغيل الجهاز وتصفير التكلفة على البطاقة
     * (التفاصيل تُحفظ في سجل UsageRecord منفصل)
     */
    public void stopDevice() {
        if (isRunning) {
            this.isRunning = false;
            this.startTime = 0;
            this.totalHoursUsed = 0;
        }
    }
    
    /** التكلفة المعروضة على البطاقة (صفر عند التوقف، الجلسة الحية عند التشغيل) */
    public double getDisplayCost() {
        if (isRunning) {
            return getCurrentCost();
        }
        return 0;
    }
    
    /**
     * مدة الجلسة الحالية بالميلي ثانية
     */
    public long getCurrentSessionMillis() {
        if (isRunning && startTime > 0) {
            return System.currentTimeMillis() - startTime;
        }
        return 0;
    }
    
    /**
     * تنسيق المدة: ساعات، دقائق، ثواني
     */
    public static String formatDuration(long millis) {
        long totalSeconds = Math.max(0, millis / 1000);
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        
        if (hours > 0) {
            return String.format(java.util.Locale.getDefault(),
                    "%d س %d د %d ث", hours, minutes, seconds);
        }
        if (minutes > 0) {
            return String.format(java.util.Locale.getDefault(),
                    "%d د %d ث", minutes, seconds);
        }
        return String.format(java.util.Locale.getDefault(), "%d ث", seconds);
    }
    
    /**
     * تنسيق رقمي للمؤقت (HH:MM:SS)
     */
    public static String formatDurationClock(long millis) {
        long totalSeconds = Math.max(0, millis / 1000);
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format(java.util.Locale.getDefault(),
                "%02d:%02d:%02d", hours, minutes, seconds);
    }
    
    /**
     * حساب التكلفة الحالية للتشغيل
     * @return التكلفة بالعملة المحلية
     * 
     * الحساب: (استهلاك الطاقة kW) × (عدد الساعات) × (سعر الكيلو واط)
     */
    public double getCurrentCost() {
        if (isRunning && startTime > 0) {
            long currentTime = System.currentTimeMillis();
            long durationMillis = currentTime - startTime;
            double hoursUsed = durationMillis / (1000.0 * 60 * 60);
            return powerConsumptionKw * hoursUsed * pricePerKw;
        }
        return 0;
    }
    
    /**
     * حساب إجمالي التكلفة (الإجمالي + الحالي إن كان يعمل)
     * @return إجمالي التكلفة
     */
    public double getTotalCost() {
        double totalCost = totalHoursUsed * powerConsumptionKw * pricePerKw;
        if (isRunning) {
            totalCost += getCurrentCost();
        }
        return totalCost;
    }
    
    /**
     * حساب التكلفة المتوقعة لفترة زمنية محددة (للمحاكي)
     * @param hours عدد الساعات
     * @return التكلفة المتوقعة
     */
    public double simulateCost(double hours) {
        return powerConsumptionKw * hours * pricePerKw;
    }
    
    /**
     * الحصول على عدد الساعات الحالية إذا كان الجهاز يعمل
     * @return عدد الساعات
     */
    public double getCurrentHours() {
        if (isRunning && startTime > 0) {
            long currentTime = System.currentTimeMillis();
            long durationMillis = currentTime - startTime;
            return durationMillis / (1000.0 * 60 * 60);
        }
        return 0;
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
    
    public String getCategoryId() {
        return categoryId;
    }
    
    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }
    
    public String getCategoryName() {
        return categoryName;
    }
    
    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    public double getPowerConsumptionKw() {
        return powerConsumptionKw;
    }
    
    public void setPowerConsumptionKw(double powerConsumptionKw) {
        this.powerConsumptionKw = powerConsumptionKw;
    }
    
    public double getPricePerKw() {
        return pricePerKw;
    }
    
    public void setPricePerKw(double pricePerKw) {
        this.pricePerKw = pricePerKw;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public boolean isRunning() {
        return isRunning;
    }
    
    public void setRunning(boolean running) {
        isRunning = running;
    }
    
    public long getStartTime() {
        return startTime;
    }
    
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }
    
    public double getTotalHoursUsed() {
        return totalHoursUsed;
    }
    
    public void setTotalHoursUsed(double totalHoursUsed) {
        this.totalHoursUsed = totalHoursUsed;
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
    
    @Override
    public String toString() {
        return "Device{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", categoryName='" + categoryName + '\'' +
                ", powerConsumptionKw=" + powerConsumptionKw +
                ", isRunning=" + isRunning +
                '}';
    }
}
