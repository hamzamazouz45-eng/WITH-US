# المساهمة | Contributing

شكراً لاهتمامك بـ **وصلة | WITH US**. المشروع يهدف إلى مساعدة الصم وضعاف السمع على متابعة مقاطع التواصل الاجتماعي.
Thank you for your interest in **WITH US**. The project aims to help deaf and hard-of-hearing people follow social media videos.

## القاعدة الأهم | The most important rule

**الإشارات الصحيحة لا يكتبها المبرمجون.** أي إشارة تدخل التطبيق يجب أن يراجعها شخص أصمّ أو مترجم إشارة معتمد، وتُسجَّل المراجعة. إشارة خاطئة تضلّل المستخدم، وهي أسوأ من غياب الإشارة.
**Correct signs are not written by programmers.** Every sign added to the app must be reviewed by a deaf person or a qualified sign-language interpreter, and the review must be recorded. A wrong sign misleads the user and is worse than no sign.

- الأوضاع الموجودة الآن مؤقتة وتقريبية (`verified: false`)، وكتبها المطوّر الأول بلا مراجعة.
- لإضافة أو تصحيح إشارات استعمل `tools/sign-editor.html`، واستعمل خانة المراجعة فيه، ثم ضع الملف الناتج في `app/src/main/assets/overlay/signs.js`.
- اذكر في طلب الدمج (Pull Request) من راجع الإشارات وأي لغة إشارة تتبعها (الجزائرية، الموحّدة، ASL، BSL…). لغات الإشارة تختلف من بلد إلى آخر.
- The current poses are temporary approximations (`verified: false`), written without review.
- To add or fix signs use `tools/sign-editor.html` and its review checkbox, then put the exported file in `app/src/main/assets/overlay/signs.js`.
- In your Pull Request, state who reviewed the signs and which sign language they belong to (Algerian, Arabic-unified, ASL, BSL…). Sign languages differ from country to country.

## أين تساعد | Where help is needed

- **قراءة الترجمة:** بعض التطبيقات ترسم الترجمة كصورة فلا يقرؤها `CaptionReader.kt`. شاشة التشخيص في التطبيق تعطي ما رآه. أرفق نتيجتها في أي بلاغ.
- **الاستماع للصوت** كخيار للمقاطع بلا ترجمة (نموذج تعرّف على الكلام يعمل على الجهاز).
- **أفاتار أفضل:** أصابع بمفاصل أدق، وجه وجسم أوضح، أو مقاطع فيديو مسجّلة لمترجمين حقيقيين.
- **لغات إضافية** وقواميس كلمات، بمراجعة أهلها.
- **آيفون:** غير مدعوم، فالنظام لا يسمح بنافذة فوق التطبيقات ولا بقراءة الشاشة.
- **اختبارات** وتحسين الأداء واستهلاك البطارية.
- **Caption reading:** some apps draw captions as an image, so `CaptionReader.kt` cannot read them. The in-app diagnostics screen shows what was seen; attach it to bug reports.
- **Audio listening** as an option for videos without captions (on-device speech recognition).
- **A better avatar:** finer finger joints, clearer face and body, or recorded video clips of real interpreters.
- **More languages** and word dictionaries, reviewed by native signers.
- **iOS:** not supported; the system allows neither overlays over other apps nor screen reading.
- **Tests**, performance and battery use.

## الخصوصية | Privacy

التطبيق **بلا إذن إنترنت** عن قصد، ويقرأ نص الشاشة عبر خدمة إمكانية الوصول ليبحث عن الترجمة فقط، ولا يحفظه. لا تُضف إذن إنترنت ولا مكتبات تتبّع أو إحصاءات. أي تغيير يمس هذا يحتاج نقاشاً مفتوحاً أولاً.
The app deliberately has **no internet permission**. It reads on-screen text through an accessibility service only to find captions, and stores nothing. Do not add the internet permission, trackers or analytics. Any change touching this needs an open discussion first.

## البناء | Building

يبني GitHub Actions ملف APK عند كل push إلى `main` (راجع `.github/workflows/build.yml`)، أو محلياً:
GitHub Actions builds the APK on every push to `main` (see `.github/workflows/build.yml`), or locally:

```
gradle assembleDebug      # JDK 17 + Android SDK 34, Gradle 8.9
```

التوقيع بمفتاح تصحيح ثابت (`app/debug.keystore`) ليتحدّث التطبيق دون حذفه. هذا المفتاح معروف ولا يصلح للنشر في المتجر.
It is signed with a fixed debug key (`app/debug.keystore`) so updates install over each other. The key is public and not suitable for store release.
