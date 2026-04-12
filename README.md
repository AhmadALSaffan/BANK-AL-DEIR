<div align="center">

<img src="https://ik.imagekit.io/qeitebnxx/image%20(5).png" alt="Bank Al-Deir Icon" width="10%" />

#  BANK-AL-DEIR
### A secure, modern Android banking application

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Android](https://img.shields.io/badge/Android-API%2021+-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com/)
[![Firebase](https://img.shields.io/badge/Firebase-Auth%20%26%20DB-FFCA28?style=flat-square&logo=firebase&logoColor=black)](https://firebase.google.com/)
[![Material Design](https://img.shields.io/badge/UI-Material%20Design-757575?style=flat-square&logo=material-design&logoColor=white)](https://material.io/)
[![Status](https://img.shields.io/badge/Status-In%20Development-orange?style=flat-square)](#)

</div>

---

## Overview

**BANK-AL-DEIR** is a native Android banking application built with Kotlin and Firebase, developed by [AhmadALSaffan](https://github.com/AhmadALSaffan). It provides secure and modern banking features including account management, transaction tracking, multi-card support, and QR-based payments — all wrapped in an intuitive Material Design UI.

> ⚠️ **Disclaimer:** BANK-AL-DEIR is **not** a real banking application and does not provide actual banking services. It is a demo project created for learning, portfolio-building, and demonstrating Android development skills on GitHub.

---

## Screenshots

## Screenshots

| Mockup 1 | Mockup 2 | Mockup 3 | Mockup 4 | Mockup 5 |
|---|---|---|---|---|
| <img src="https://raw.githubusercontent.com/AhmadALSaffan/BANK-AL-DEIR/d1bcbfac181b24e594c43cac275487c658e43f12/mockup1.png" width="100%"> | <img src="https://raw.githubusercontent.com/AhmadALSaffan/BANK-AL-DEIR/c430f73c620a194c75e4820a704e5bd51fe20ef1/mockup2.png" width="100%"> | <img src="https://raw.githubusercontent.com/AhmadALSaffan/BANK-AL-DEIR/9f41fd90e652fafcb68e5fa9cac986694a835c5d/mockup3.png" width="100%"> | <img src="https://raw.githubusercontent.com/AhmadALSaffan/BANK-AL-DEIR/9f41fd90e652fafcb68e5fa9cac986694a835c5d/mockup4.png" width="100%"> | <img src="https://raw.githubusercontent.com/AhmadALSaffan/BANK-AL-DEIR/9f41fd90e652fafcb68e5fa9cac986694a835c5d/mockup5.png" width="100%"> |

---

## Features

- 🔐 **Authentication** — Secure user registration and login via Firebase Auth
- 💳 **Account Management** — Balance overview and multi-card support
- 📊 **Transaction History** — Detailed transaction tracking and history
- 📷 **QR Payments** — QR code scanning for payments and transfers
- ☁️ **Firebase Integration** — Real-time database, authentication, and cloud functions
- 🎨 **Material Design UI** — Clean, intuitive, and modern interface

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Material Design Components |
| Auth | Firebase Authentication |
| Database | Firebase Realtime Database |
| QR Scanning | QR Code Scanner Library |
| Cloud | Firebase Cloud Functions |

---

## Getting Started

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17+
- A Firebase project with **Email/Password** authentication enabled

### 1 — Download the APK

To try BANK-AL-DEIR directly on your device:

[⬇️ Download APK](https://www.mediafire.com/file/5w54d712x6u7hxd/Bank_AL_Deir.apk/file)

Open the APK file on your Android device and follow the on-screen installation instructions.

### 2 — Clone the Repository (Developers)

```bash
git clone https://github.com/AhmadALSaffan/BANK-AL-DEIR.git
cd BANK-AL-DEIR
```

### 3 — Add Firebase Config

1. Go to your [Firebase Console](https://console.firebase.google.com/) → Project settings → Download **`google-services.json`**
2. Place it in `app/google-services.json`
3. Enable **Email/Password** sign-in under Authentication → Sign-in method

### 4 — Build & Run

```bash
./gradlew assembleDebug
```

Or press **▶ Run** in Android Studio.

---

## Contributing

Pull requests are welcome! For major changes, please open an issue first to discuss what you'd like to change.

1. Fork the repo
2. Create your feature branch: `git checkout -b feature/your-feature`
3. Commit your changes: `git commit -m "feat: add your feature"`
4. Push to the branch: `git push origin feature/your-feature`
5. Open a Pull Request

---

## License

```text
This is my project — any help, feedback, or contributions are more than welcome!
```

---

<div align="center">
  Built with ❤️ by <a href="https://github.com/AhmadALSaffan">Ahmad AlSaffan</a>
</div>
