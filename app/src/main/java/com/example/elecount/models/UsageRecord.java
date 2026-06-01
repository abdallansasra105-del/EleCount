package com.example.elecount.models;

/**
 * نموذج بيانات سجل الاستخدام
 * يسجل كل مرة تشغيل وإيقاف للجهاز مع التكلفة
 * 
 * الشرح: هذا الكلاس يحفظ تاريخ استخدام الأجهزة
 * كل سجل يحتوي على معرف الجهاز، وقت التشغيل، مدة الاستخدام، والتكلفة
 */
public class UsageRecord {
    // المعرف الفريد للسجل
    private String id;
    
    // معرف الجهاز الذي تم استخدامه
    private String deviceId;
    
    // اسم الجهاز (للعرض)
    private String deviceName;
    
    // معرف المستخدم
    private String userId;
    
    // وقت بدء التشغيل (timestamp)
    private long startTime;
    
    // وقت إيقاف التشغيل (timestamp)
    private long endTime;
    
    // مدة الاستخدام بالساعات
    private double durationHours;
    
    // استهلاك الطاقة المستخدم (kWh)
    private double energyConsumed;
    
    // التكلفة الإجمالية لهذا الاستخدام
    private double cost;
    
    // هل هذا سجل محاكاة أم حقيقي
    @com.google.gson.annotations.SerializedName("isSimulation")
    private boolean isSimulation;
    
    // تاريخ إنشاء السجل
    private long createdAt;
    
    /**
     * المنشئ الفارغ
     */
    public UsageRecord() {
        this.isSimulation = false;
        this.createdAt = System.currentTimeMillis();
    }
    
    /**
     * المنشئ من جهاز
     * يستخدم لإنشاء سجل جديد عند إيقاف الجهاز
     * @param device الجهاز الذي تم استخدامه
     * @param startTime وقت البدء
     * @param endTime وقت الإيقاف
     */
    public UsageRecord(Device device, long startTime, long endTime) {
        this.deviceId = device.getId();
        this.deviceName = device.getName();
        this.userId = device.getUserId();
        this.startTime = startTime;
        this.endTime = endTime;
        
        // حساب المدة بالساعات
        long durationMillis = endTime - startTime;
        this.durationHours = durationMillis / (1000.0 * 60 * 60);
        
        // حساب الطاقة المستهلكة
        this.energyConsumed = device.getPowerConsumptionKw() * this.durationHours;
        
        // حساب التكلفة
        this.cost = this.energyConsumed * device.getPricePerKw();
        
        this.isSimulation = false;
        this.createdAt = System.currentTimeMillis();
    }
    
    /**
     * إنشاء سجل محاكاة
     * يستخدم في وضع المحاكي لحساب التكلفة المتوقعة
     * @param device الجهاز
     * @param durationHours مدة الاستخدام المطلوب محاكاتها
     * @return سجل المحاكاة
     */
    public static UsageRecord createSimulation(Device device, double durationHours) {
        UsageRecord record = new UsageRecord();
        record.deviceId = device.getId();
        record.deviceName = device.getName();
        record.durationHours = durationHours;
        record.energyConsumed = device.getPowerConsumptionKw() * durationHours;
        record.cost = record.energyConsumed * device.getPricePerKw();
        record.isSimulation = true;
        record.createdAt = System.currentTimeMillis();
        return record;
    }
    
    /**
     * الحصول على وصف نصي للمدة
     * @return نص مثل "5 ساعات و30 دقيقة"
     */
    public String getDurationText() {
        int hours = (int) durationHours;
        int minutes = (int) ((durationHours - hours) * 60);
        
        if (hours > 0 && minutes > 0) {
            return hours + " ساعة و" + minutes + " دقيقة";
        } else if (hours > 0) {
            return hours + " ساعة";
        } else if (minutes > 0) {
            return minutes + " دقيقة";
        } else {
            return "أقل من دقيقة";
        }
    }
    
    /**
     * الحصول على نص التكلفة مع العملة
     * @return نص مثل "5.25 شيكل"
     */
    public String getCostText() {
        return String.format(java.util.Locale.getDefault(), "%.2f شيكل", cost);
    }
    
    /**
     * نص التاريخ والوقت للعرض في السجل
     */
    public String getDateTimeText() {
        java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat(
                "dd/MM/yyyy HH:mm", java.util.Locale.getDefault());
        if (startTime > 0 && endTime > 0) {
            return fmt.format(new java.util.Date(startTime)) + " — " + fmt.format(new java.util.Date(endTime));
        }
        if (createdAt > 0) {
            return fmt.format(new java.util.Date(createdAt));
        }
        return "—";
    }
    
    // === Getters & Setters ===
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getDeviceId() {
        return deviceId;
    }
    
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
    
    public String getDeviceName() {
        return deviceName;
    }
    
    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public long getStartTime() {
        return startTime;
    }
    
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }
    
    public long getEndTime() {
        return endTime;
    }
    
    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }
    
    public double getDurationHours() {
        return durationHours;
    }
    
    public void setDurationHours(double durationHours) {
        this.durationHours = durationHours;
    }
    
    public double getEnergyConsumed() {
        return energyConsumed;
    }
    
    public void setEnergyConsumed(double energyConsumed) {
        this.energyConsumed = energyConsumed;
    }
    
    public double getCost() {
        return cost;
    }
    
    public void setCost(double cost) {
        this.cost = cost;
    }
    
    public boolean isSimulation() {
        return isSimulation;
    }
    
    public void setSimulation(boolean simulation) {
        isSimulation = simulation;
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
    
    @Override
    public String toString() {
        return "UsageRecord{" +
                "id='" + id + '\'' +
                ", deviceName='" + deviceName + '\'' +
                ", durationHours=" + durationHours +
                ", cost=" + cost +
                ", isSimulation=" + isSimulation +
                '}';
    }
}
