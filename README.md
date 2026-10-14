# وصلة | WITH US

**العربية**
وصلة تطبيق أندرويد يساعد الصم وضعاف السمع على متابعة مقاطع التواصل الاجتماعي. يقرأ الترجمة المكتوبة (CC) الظاهرة على الشاشة، ويعرضها بلغة الإشارة والهجاء الإصبعي في نافذة عائمة فوق أي تطبيق، بأفاتار من رأس ويدين. يدعم العربية والإنجليزية.

**English**
WITH US is an Android app that helps deaf and hard-of-hearing people follow social media videos. It reads the on-screen captions (CC) and shows them as sign language and fingerspelling in a floating window above any app, using an avatar made of a head and two hands. Arabic and English are supported.

---

- **نموذج أولي | Prototype.** أوضاع اليدين في `app/src/main/assets/overlay/index.html` تقريبية ولم يراجعها مترجم إشارة، فلا تعتمد عليها للتعلّم أو للتواصل الحقيقي. Hand poses are approximate and have not been reviewed by a sign-language interpreter; do not rely on them for learning or real communication.
- **الخصوصية | Privacy.** التطبيق بلا إذن إنترنت (راجع `AndroidManifest.xml`). يقرأ نص الشاشة عبر خدمة إمكانية الوصول ليبحث عن الترجمة فقط، ولا يحفظه. The app has no internet permission. It scans on-screen text through an accessibility service only to find captions, and stores nothing.
- **التثبيت | Install.** حمّل `app-debug.apk` من صفحة Releases ثم افتح التطبيق واتبع الخطوات. Download `app-debug.apk` from Releases, open the app and follow the steps.
- **البناء | Build.** يتم تلقائياً على GitHub Actions عند كل push إلى `main`. Built automatically by GitHub Actions on every push to `main`.
