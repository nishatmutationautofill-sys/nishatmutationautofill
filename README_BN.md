# Firebase কোড অক্ষত রেখে Android Chat Admin APK

এই প্রজেক্টে আপনার সর্বশেষ `index(2).html`-এর অ্যাডমিন প্যানেল ব্যবহার করা হয়েছে। HTML-এর বিদ্যমান Firebase/Firestore config URL fallback বাদ দেওয়া হয়নি। Android অ্যাপে সার্ভারের HTTPS URL কনফিগার করলে native bridge সেটি ব্যবহার করে; ওয়েব সংস্করণে Firestore থেকে URL আনার আগের ব্যবস্থা বহাল আছে।

## কী আছে
- Android WebView-তে আপনার চ্যাট অ্যাডমিন প্যানেল।
- অ্যাডমিন টোকেন ফোনে সংরক্ষণ: অ্যাপ বন্ধ করে আবার খুললে লগইন সেশন ধরে রাখার চেষ্টা করে। লগ আউট করলে টোকেন মুছে যায়।
- Foreground service নিজস্ব VPS-এর `/admin-ws?token=...` WebSocket-এ সংযুক্ত থাকে এবং `msg` / `group_msg` ইভেন্টে Android notification দেখায়। সংযোগ বিচ্ছিন্ন হলে আবার চেষ্টা করে।
- GitHub Actions দিয়ে ওয়েব ব্রাউজার/GitHub থেকে APK build; Android Studio লাগবে না।

## সীমাবদ্ধতা—গুরুত্বপূর্ণ
- APK build সফল হয়েছে বলে দাবি করা হচ্ছে না; এই পরিবেশে Android SDK/Gradle দিয়ে build ও ফোনে পরীক্ষা করা হয়নি। GitHub Actions-এ build চালিয়ে ফলাফল যাচাই করুন।
- নিজস্ব VPS-এ valid HTTPS/WSS certificate এবং `/admin-ws` WebSocket reverse-proxy সমর্থন থাকতে হবে।
- সার্ভারের WebSocket-এ `msg` ও `group_msg` ইভেন্ট অ্যাডমিন ক্লায়েন্টকে পাঠানো থাকতে হবে। পৃথক upload event থাকলে সেটি server-side-এ যোগ করতে হবে; বর্তমানে মেসেজের সঙ্গে আসা attachment-এ notification হবে।
- Android 13+ এ notification permission Allow করুন। ব্যাটারি অপ্টিমাইজেশন, ফোনের নির্মাতার ব্যাকগ্রাউন্ড সীমা, Force stop বা টোকেন revoke হলে notification বন্ধ হতে পারে। Firebase Cloud Messaging (FCM) ছাড়া সব ফোনে force-stop/বন্ধ অবস্থায় ১০০% নির্ভরযোগ্য push নিশ্চিত করা যায় না।
- Admin token SharedPreferences-এ রাখা হয়। এটি ডিভাইস-লোকাল স্টোরেজ, কিন্তু rooted/compromised ফোনে সম্পূর্ণ নিরাপদ বলে ধরা যাবে না। ফোন হারালে VPS-এ token পরিবর্তন করুন।

## GitHub Actions দিয়ে APK তৈরি
1. GitHub-এ একটি **private repository** তৈরি করুন।
2. ZIP-এর সব ফাইল repository root-এ আপলোড/commit করুন। `settings.gradle`, `build.gradle`, `app/`, `.github/` root-এ থাকতে হবে।
3. **Actions** ট্যাবে গিয়ে `Build Android APK` নির্বাচন করুন।
4. **Run workflow** চাপুন (অথবা `main`/`master`-এ push করুন)।
5. সফল run-এর নিচের **Artifacts** থেকে `server-notify-admin-debug-apk` ডাউনলোড করুন। এর মধ্যে `app-debug.apk` থাকবে।
6. APK ফোনে ইনস্টল করুন। প্রয়োজন হলে Android-এর **Install unknown apps** অনুমতি দিন।

## প্রথমবার চালু
1. অ্যাপ খুলে নিজের VPS-এর URL দিন, যেমন `https://chat.example.com`—এটি উদাহরণ, নিজের ডোমেইন ব্যবহার করবেন।
2. অ্যাডমিন টোকেন দিয়ে লগইন করুন এবং notification permission Allow করুন।
3. অ্যাপ থেকে বের হলেও foreground service সংযোগ ধরে রাখার চেষ্টা করবে। Notification tray-তে চলমান সার্ভিসের notification দেখা স্বাভাবিক।

## Firebase
Firebase/Firestore URL fallback-সহ আপনার HTML-এর বিদ্যমান কোড রাখা হয়েছে। এই APK নিজস্ব VPS-এর WebSocket ব্যবহার করলেও Firebase-এর অন্য বিদ্যমান ব্যবহার স্বয়ংক্রিয়ভাবে মুছে বা প্রতিস্থাপন করে না।
