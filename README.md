# Kalkulator Kasir

Aplikasi Android (Kotlin + Jetpack Compose): input harga barang, total barang,
uang pembeli, dan kembalian. Mendukung tema gelap & terang.

Developer: **Bagas Orcarius**
Facebook: https://facebook.com/willydevops1

Dibuat dengan Kotlin by Bagas Orcarius

## Build otomatis dengan GitHub Actions
1. Upload isi folder ini ke repository GitHub (branch `main`).
2. Buka tab **Actions** -> workflow **Build APK** berjalan otomatis.
3. Unduh APK di bagian **Artifacts** pada run yang selesai.
4. Opsional: buat tag `v1.0` (`git tag v1.0 && git push --tags`) agar APK
   otomatis terbit di halaman **Releases**.

## Build lokal
Buka folder ini di Android Studio, lalu Run. Atau jalankan `gradle assembleDebug`
(Gradle 8.9, JDK 17).
