<div align="center">

# 🧮 Kalkulator Kasir

**Aplikasi kasir Android yang ringan, cepat, dan modern.**
Hitung belanjaan, uang pembeli, dan kembalian dalam hitungan detik.

[![Build](https://github.com/BagasExpoit/kalkulator-kasir/actions/workflows/android.yml/badge.svg)](https://github.com/BagasExpoit/kalkulator-kasir/actions/workflows/android.yml)
[![Release](https://img.shields.io/github/v/release/BagasExpoit/kalkulator-kasir?color=00695C&label=release)](https://github.com/BagasExpoit/kalkulator-kasir/releases/latest)
[![Downloads](https://img.shields.io/github/downloads/BagasExpoit/kalkulator-kasir/total?color=80CBC4)](https://github.com/BagasExpoit/kalkulator-kasir/releases)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)
![Android](https://img.shields.io/badge/Android-7.0%2B-3DDC84?logo=android&logoColor=white)

[⬇️ Unduh APK](https://github.com/BagasExpoit/kalkulator-kasir/releases/latest) ·
[🐞 Laporkan Bug](https://github.com/BagasExpoit/kalkulator-kasir/issues) ·
[👤 Developer](https://facebook.com/willydevops1)

</div>

---

## ✨ Fitur

| | Fitur | Keterangan |
|---|---|---|
| 💰 | **Input harga barang** | Masukkan harga dan jumlah, lalu tambahkan ke daftar belanja |
| 🛒 | **Total barang & total belanja** | Dihitung otomatis setiap ada perubahan |
| 💵 | **Uang pembeli** | Input nominal uang yang dibayarkan |
| 🔁 | **Kembalian otomatis** | Jika uang kurang, aplikasi menampilkan selisih kekurangannya |
| 🗑️ | **Hapus per barang** | Salah input? Hapus satu barang tanpa mengulang dari awal |
| 🌗 | **Tema gelap & terang** | Ganti tema lewat saklar, default mengikuti pengaturan HP |
| 🇮🇩 | **Format Rupiah** | Pemisah ribuan sesuai standar Indonesia (Rp 1.250.000) |
| 📐 | **Tampilan ringkas** | Seluruh komponen muat dalam satu layar |

<!--
## 📸 Tampilan
Taruh screenshot di folder docs/ lalu aktifkan baris di bawah:

| Terang | Gelap |
|:---:|:---:|
| <img src="docs/terang.png" width="260"> | <img src="docs/gelap.png" width="260"> |
-->

## 📲 Instalasi

1. Buka halaman **[Releases](https://github.com/BagasExpoit/kalkulator-kasir/releases/latest)**.
2. Unduh file `KalkulatorKasir.apk`.
3. Buka file tersebut di HP, lalu izinkan **Install dari sumber tidak dikenal** jika diminta.

> **Catatan:** Android 7.0 (API 24) atau lebih baru.

## 🧰 Teknologi

- **Kotlin** – bahasa utama
- **Jetpack Compose** + **Material 3** – antarmuka deklaratif
- **Gradle Kotlin DSL** – konfigurasi build
- **GitHub Actions** – build dan rilis APK otomatis

## 🗂️ Struktur Proyek

```text
kalkulator-kasir/
├── .github/workflows/android.yml   # CI: build, tanda tangan, dan rilis APK
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/bagas/kasir/MainActivity.kt
│       └── res/
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

## 🛠️ Build dari Source

**Lewat Android Studio**
1. Clone repo ini: `git clone https://github.com/BagasExpoit/kalkulator-kasir.git`
2. Buka folder proyek di Android Studio.
3. Tekan **Run ▶**.

**Lewat terminal** (JDK 17 dan Gradle 8.9)
```bash
gradle assembleDebug
```
APK debug ada di `app/build/outputs/apk/debug/`.

## 🚀 Build Otomatis dengan GitHub Actions

Setiap push ke `main` akan membangun APK yang sudah ditandatangani. Push tag berawalan `v` akan menerbitkannya ke halaman **Releases**.

```bash
git tag v1.0
git push origin v1.0
```

### 🔐 Secrets yang dibutuhkan

Buka **Settings → Secrets and variables → Actions**, lalu tambahkan:

| Secret | Isi |
|---|---|
| `KEYSTORE_BASE64` | Isi file keystore dalam format base64 |
| `KEYSTORE_PASSWORD` | Password keystore |
| `KEY_ALIAS` | Alias kunci (misalnya `kasir`) |
| `KEY_PASSWORD` | Password kunci |

Membuat keystore:
```bash
keytool -genkeypair -v -keystore kasir-release.jks -alias kasir \
  -keyalg RSA -keysize 2048 -validity 10000
base64 -w 0 kasir-release.jks   # salin hasilnya ke KEYSTORE_BASE64
```

> ⚠️ Jangan pernah meng-commit file `.jks` ke repository dan simpan cadangannya di tempat aman.

## 👨‍💻 Developer

<div align="center">

**Bagas Orcarius**

[![Facebook](https://img.shields.io/badge/Facebook-willydevops1-1877F2?logo=facebook&logoColor=white)](https://facebook.com/willydevops1)

⭐ Kalau aplikasi ini bermanfaat, beri bintang di repo ini!

<sub>Dibuat dengan Kotlin by Bagas Orcarius</sub>

</div>
