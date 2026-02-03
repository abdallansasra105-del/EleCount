# 📚 **دليل استخدام DALAppWriteConnection**

## 🎯 **نبذة عامة**

`DALAppWriteConnection` هو كلاس Java شامل يوفر طبقة وصول إلى البيانات (Data Access Layer) موحدة وسهلة الاستخدام للتفاعل مع Appwrite Backend-as-a-Service. تم تصميم هذا الكلاس ليكون وسيطاً عاماً بين المبرمجين وقاعدة البيانات، مما يوفر واجهة بسيطة ومرنة لإدارة جميع العمليات الأساسية.

## ✨ **المزايا الرئيسية**

- 🔧 **سهولة الاستخدام**: دوال واضحة ومبسطة مع أمثلة شاملة
- 🌍 **توثيق عربي**: شرح مفصل لكل دالة باللغة العربية
- 📦 **إدارة شاملة**: المستخدمين، قواعد البيانات، والملفات
- 🔒 **آمان عالي**: إدارة جلسات المستخدمين والأذونات
- 🚀 **مرونة عالية**: يعمل مع أي كائن Java
- 📱 **متوافق مع Android**: مصمم خصيصاً للتطبيقات المحمولة

## 📋 **الوظائف المتاحة**

### 🔐 **إدارة المستخدمين**
- إنشاء مستخدمين جدد
- تسجيل الدخول والخروج
- حذف المستخدمين
- فحص حالة تسجيل الدخول
- إدارة الجلسات

### 💾 **إدارة قواعد البيانات**
- حفظ الكائنات (مفرد أو مجمع)
- جلب البيانات من الجداول
- تحديث البيانات الموجودة
- حذف المستندات
- إنشاء جداول جديدة
- فحص وجود الجداول

### 📁 **إدارة التخزين**
- رفع الملفات إلى التخزين
- جلب قائمة الملفات
- حذف الملفات
- تحديث الملفات
- جلب معلومات الملفات

## 🚀 **بدء الاستخدام**

### 1. **تهيئة الكلاس**
```java
// في Activity أو Fragment
DALAppWriteConnection dal = new DALAppWriteConnection(this);

// اختبار الاتصال
if (dal.testConnection()) {
    Log.d("SUCCESS", "الاتصال مع Appwrite يعمل بنجاح!");
} else {
    Log.e("ERROR", "فشل الاتصال مع Appwrite");
}
```

### 2. **إدارة المستخدمين**

#### إنشاء مستخدم جديد
```java
// إنشاء مستخدم افتراضي
OperationResult<UserData> result = dal.createDefaultUser(
    "user@example.com",     // البريد الإلكتروني
    "password123",          // كلمة المرور
    "أحمد",                 // الاسم الأول
    "محمد",                // الاسم الأخير
    "0123456789"           // رقم الهاتف (اختياري)
);

if (result.success) {
    Log.d("SUCCESS", "تم إنشاء المستخدم: " + result.data.userId);
    Log.d("EMAIL", "البريد الإلكتروني: " + result.data.email);
} else {
    Log.e("ERROR", "فشل إنشاء المستخدم: " + result.message);
}
```

#### تسجيل الدخول
```java
OperationResult<UserData> loginResult = dal.loginUser(
    "user@example.com",
    "password123"
);

if (loginResult.success) {
    Log.d("SUCCESS", "تم تسجيل الدخول بنجاح");
    Log.d("USER_ID", "معرف المستخدم: " + loginResult.data.userId);
} else {
    Log.e("ERROR", "فشل تسجيل الدخول: " + loginResult.message);
}
```

#### فحص حالة تسجيل الدخول
```java
if (dal.isUserLoggedIn()) {
    Log.d("STATUS", "المستخدم مسجل الدخول");
    Log.d("USER_ID", dal.getCurrentUserId());
    Log.d("EMAIL", dal.getCurrentUserEmail());
} else {
    Log.d("STATUS", "المستخدم غير مسجل الدخول");
}
```

#### تسجيل الخروج
```java
OperationResult<Void> logoutResult = dal.logoutUser();

if (logoutResult.success) {
    Log.d("SUCCESS", "تم تسجيل الخروج بنجاح");
} else {
    Log.e("ERROR", "فشل تسجيل الخروج: " + logoutResult.message);
}
```

### 3. **إدارة قواعد البيانات**

#### حفظ البيانات (كائن واحد)
```java
// إنشاء كائن Product
class Product {
    public String name;
    public double price;
    public String description;
}

Product product = new Product();
product.name = "هاتف ذكي";
product.price = 999.99;
product.description = "هاتف ذكي متطور";

// حفظ المنتج
OperationResult<ArrayList<Product>> saveResult = dal.saveData(
    product,              // البيانات
    "products",           // اسم الجدول
    null                  // معرف المجموعة (اختياري)
);

if (saveResult.success) {
    Log.d("SUCCESS", "تم حفظ " + saveResult.data.size() + " منتج");
} else {
    Log.e("ERROR", "فشل حفظ البيانات: " + saveResult.message);
}
```

#### حفظ البيانات (مجموعة كائنات)
```java
ArrayList<Product> products = new ArrayList<>();
products.add(product1);
products.add(product2);
products.add(product3);

OperationResult<ArrayList<Product>> bulkResult = dal.saveData(
    products,
    "products",
    null
);

if (bulkResult.success) {
    Log.d("BULK", "تم حفظ " + bulkResult.data.size() + " منتج");
}
```

#### جلب البيانات
```java
OperationResult<ArrayList<Product>> getResult = dal.getData(
    "products",           // اسم الجدول
    null,                 // معرف المجموعة
    Product.class         // نوع الكلاس
);

if (getResult.success) {
    ArrayList<Product> products = getResult.data;
    Log.d("PRODUCTS", "تم جلب " + products.size() + " منتج");
    
    for (Product product : products) {
        Log.d("PRODUCT", "الاسم: " + product.name + ", السعر: " + product.price);
    }
} else {
    Log.e("ERROR", "فشل جلب البيانات: " + getResult.message);
}
```

#### جلب عنصر واحد
```java
OperationResult<Product> singleResult = dal.getDataById(
    "products",           // اسم الجدول
    "document-id-here",   // معرف المستند
    null,                 // معرف المجموعة
    Product.class         // نوع الكلاس
);

if (singleResult.success) {
    Product product = singleResult.data;
    Log.d("PRODUCT", "الاسم: " + product.name + ", السعر: " + product.price);
} else {
    Log.e("ERROR", "فشل جلب المنتج: " + singleResult.message);
}
```

#### تحديث البيانات
```java
// الحصول على منتج موجود وتحديثه
Product existingProduct = getProductFromSomewhere();
existingProduct.price = 799.99; // تحديث السعر

OperationResult<Product> updateResult = dal.updateData(
    existingProduct,      // البيانات المحدثة
    "products",           // اسم الجدول
    "document-id-here",   // معرف المستند
    null                  // معرف المجموعة
);

if (updateResult.success) {
    Log.d("SUCCESS", "تم تحديث المنتج بنجاح");
    Log.d("NEW_PRICE", "السعر الجديد: " + result.data.price);
} else {
    Log.e("ERROR", "فشل تحديث المنتج: " + updateResult.message);
}
```

#### حذف البيانات
```java
OperationResult<Void> deleteResult = dal.deleteData(
    "products",           // اسم الجدول
    "document-id-here",   // معرف المستند
    null                  // معرف المجموعة
);

if (deleteResult.success) {
    Log.d("SUCCESS", "تم حذف المنتج بنجاح");
} else {
    Log.e("ERROR", "فشل حذف المنتج: " + deleteResult.message);
}
```

### 4. **إدارة التخزين**

#### رفع ملف
```java
// الحصول على بيانات الملف (مثال من الكاميرا)
byte[] imageData = getImageBytesFromCamera();

// رفع الصورة
OperationResult<FileInfo> uploadResult = dal.uploadFile(
    imageData,            // بيانات الملف
    "photo.jpg",          // اسم الملف
    "image/jpeg",         // نوع الملف
    null                  // معرف التخزين (اختياري)
);

if (uploadResult.success) {
    FileInfo fileInfo = uploadResult.data;
    Log.d("SUCCESS", "تم رفع الملف بنجاح");
    Log.d("FILE_URL", "رابط الملف: " + fileInfo.fileUrl);
    Log.d("FILE_ID", "معرف الملف: " + fileInfo.fileId);
} else {
    Log.e("ERROR", "فشل رفع الملف: " + uploadResult.message);
}
```

#### جلب قائمة الملفات
```java
OperationResult<ArrayList<FileInfo>> filesResult = dal.getStorageFiles(
    null                  // معرف التخزين (اختياري)
);

if (filesResult.success) {
    ArrayList<FileInfo> files = filesResult.data;
    Log.d("FILES", "عدد الملفات: " + files.size());
    
    for (FileInfo file : files) {
        Log.d("FILE", "الاسم: " + file.fileName);
        Log.d("FILE", "النوع: " + file.mimeType);
        Log.d("FILE", "الحجم: " + (file.fileSize / 1024) + " KB");
    }
} else {
    Log.e("ERROR", "فشل جلب الملفات: " + filesResult.message);
}
```

#### جلب معلومات ملف واحد
```java
OperationResult<FileInfo> fileInfoResult = dal.getFileInfo(
    "file-id-here",       // معرف الملف
    null                  // معرف التخزين
);

if (fileInfoResult.success) {
    FileInfo fileInfo = fileInfoResult.data;
    Log.d("FILE", "الاسم: " + fileInfo.fileName);
    Log.d("FILE", "الرابط: " + fileInfo.fileUrl);
    Log.d("FILE", "الحجم: " + fileInfo.fileSize + " بايت");
} else {
    Log.e("ERROR", "فشل جلب معلومات الملف: " + fileInfoResult.message);
}
```

#### حذف ملف
```java
OperationResult<Void> deleteFileResult = dal.deleteFile(
    "file-id-here",       // معرف الملف
    null                  // معرف التخزين
);

if (deleteFileResult.success) {
    Log.d("SUCCESS", "تم حذف الملف بنجاح");
} else {
    Log.e("ERROR", "فشل حذف الملف: " + deleteFileResult.message);
}
```

#### تحديث ملف
```java
// الحصول على بيانات الملف الجديدة
byte[] newImageData = getNewImageBytes();

OperationResult<FileInfo> updateFileResult = dal.updateFile(
    "existing-file-id",   // معرف الملف القديم
    newImageData,         // البيانات الجديدة
    "updated_photo.jpg",  // الاسم الجديد (اختياري)
    "image/jpeg",         // النوع الجديد (اختياري)
    null                  // معرف التخزين
);

if (updateFileResult.success) {
    Log.d("SUCCESS", "تم تحديث الملف بنجاح");
    Log.d("NEW_NAME", "الاسم الجديد: " + updateFileResult.data.fileName);
} else {
    Log.e("ERROR", "فشل تحديث الملف: " + updateFileResult.message);
}
```

## 📖 **النماذج (Models)**

### UserData
```java
class UserData {
    public String userId;        // معرف المستخدم
    public String email;         // البريد الإلكتروني
    public String firstName;     // الاسم الأول
    public String lastName;      // الاسم الأخير
    public String phone;         // رقم الهاتف
    public String profileImageUrl; // رابط صورة البروفايل
    public Date createdAt;       // تاريخ الإنشاء
    public Date lastLoginAt;     // آخر تسجيل دخول
    public boolean isActive;     // حالة النشاط
}
```

### OperationResult<T>
```java
class OperationResult<T> {
    public boolean success;      // نجح أم فشل
    public String message;       // رسالة النتيجة
    public T data;              // البيانات المرجعة
    public String errorCode;     // كود الخطأ (اختياري)
}
```

### FileInfo
```java
class FileInfo {
    public String fileId;        // معرف الملف
    public String fileName;      // اسم الملف
    public String fileUrl;       // رابط الملف
    public String mimeType;      // نوع الملف
    public long fileSize;        // حجم الملف
    public String uploadedBy;    // الذي رفع الملف
    public Date uploadDate;      // تاريخ الرفع
    public String bucketId;      // معرف التخزين
}
```

## ⚠️ **نصائح مهمة**

### 1. **معالجة الأخطاء**
```java
OperationResult<UserData> result = dal.createDefaultUser(...);

if (!result.success) {
    // معالجة الخطأ
    Log.e("ERROR", result.message);
    // يمكن عرض رسالة للمستخدم أو اتخاذ إجراء آخر
    return;
}

// العمل مع البيانات الناجحة
UserData user = result.data;
// ...
```

### 2. **إدارة الجلسات**
```java
// تحقق من تسجيل الدخول قبل العمليات الحساسة
if (!dal.isUserLoggedIn()) {
    // توجيه المستخدم لصفحة تسجيل الدخول
    startActivity(new Intent(this, LoginActivity.class));
    return;
}

// الآن يمكن تنفيذ العمليات التي تتطلب تسجيل دخول
```

### 3. **أنواع الملفات المدعومة**
```java
// الصور
"image/jpeg"
"image/png"
"image/gif"
"image/webp"

// المستندات
"application/pdf"
"application/msword"
"application/vnd.openxmlformats-officedocument.wordprocessingml.document"

// ملفات أخرى
"text/plain"
"application/json"
```

### 4. **أفضل الممارسات**
```java
// 1. استخدم أسماء واضحة للجداول
dal.saveData(product, "store_products", null);

// 2. تحقق من النجاح بعد كل عملية
if (result.success) {
    // تحديث واجهة المستخدم
    updateUI();
} else {
    showError(result.message);
}

// 3. استخدم collectionId للجداول المعقدة
dal.getData("user_data", "user_profile_data", UserProfile.class);

// 4. احفظ معرفات الملفات للعمليات المستقبلية
String fileId = uploadedFile.fileId;
// احفظ fileId في قاعدة البيانات للوصول السريع
```

## 🔧 **استكشاف الأخطاء**

### أخطاء شائعة وحلولها

#### 1. خطأ الاتصال
```
Connection Error: null
```
**الحل**: تحقق من اتصال الإنترنت وإعدادات Appwrite

#### 2. خطأ أذونات الكاميرا
```
Permission Denial: starting Intent
```
**الحل**: تأكد من طلب إذن CAMERA في AndroidManifest

#### 3. خطأ في حفظ البيانات
```
Failed to save data: Database not found
```
**الحل**: تأكد من صحة اسم الجدول وإعدادات قاعدة البيانات

#### 4. خطأ في رفع الملفات
```
Failed to upload file: HTTP 400
```
**الحل**: تحقق من صحة بيانات الملف ونوع MIME

## 📝 **ملاحظات التطوير**

- تم تصميم هذا الكلاس للعمل مع Appwrite Cloud
- يدعم جميع العمليات الأساسية CRUD (Create, Read, Update, Delete)
- يتعامل تلقائياً مع إنشاء الجداول المفقودة
- يحفظ معلومات الملفات في جدول خاص للمتابعة
- يدعم رفع الملفات الكبيرة (حتى 30 ثانية timeout)

## 🎉 **خلاصة**

`DALAppWriteConnection` يوفر حلاً شاملاً وسهلاً للتعامل مع Appwrite في تطبيقات Android. مع واجهة بسيطة وتوثيق عربي مفصل، يمكن لأي مطور البدء في استخدامه فوراً لإدارة المستخدمين والبيانات والملفات بكفاءة عالية.

**🚀 ابدأ الآن واستمتع بتطوير تطبيقات قوية مع Appwrite!**
