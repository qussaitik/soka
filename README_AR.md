# SokaChat Android

تطبيق Android مستقل يفتح:
https://alshaqi.lovable.app/room

## المزايا
- WebView حديث.
- JavaScript وDOM Storage.
- WebRTC / مايك / كاميرا.
- رفع الملفات من داخل الموقع.
- معالجة WebView Renderer crash وإعادة إنشاء WebView.
- حفظ حالة الصفحة عند إعادة إنشاء النشاط.
- GitHub Actions لبناء APK تلقائيًا.

## بناء APK
1. ارفع المشروع إلى GitHub.
2. افتح Actions.
3. اختر Build SokaChat APK.
4. شغّل workflow.
5. بعد الانتهاء، افتح Artifacts ونزّل SokaChat-debug.

## ملاحظة
نسخة debug مناسبة للاختبار. قبل النشر العام على Google Play يفضل إضافة signing وواجهة أيقونة نهائية وسياسة الخصوصية.
