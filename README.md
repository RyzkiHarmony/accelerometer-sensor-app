# Road Damage Detector - Accelerometer Sensor App 🚗📉

**Road Damage Detector** adalah aplikasi Android pencatat dan penganalisis sensor (**Sensor Logger & Analyzer**) yang berfokus pada penggunaan **Accelerometer** (akselerometer) untuk mendeteksi anomali permukaan jalan. Aplikasi ini mengubah smartphone Anda menjadi alat monitoring getaran presisi tinggi yang dikombinasikan dengan data geospasial (GPS).

Fungsi utama aplikasi ini adalah menangkap data mentah dari sensor akselerometer 3-sumbu (X, Y, Z) secara real-time untuk mengidentifikasi guncangan yang mengindikasikan kerusakan jalan (seperti lubang atau polisi tidur), sambil merekam jejak lokasi perjalanan.

## 📱 Fitur Unggulan (Sensor & Data)

- **High-Frequency Accelerometer Logging**:
  - Merekam data akselerasi mentah pada sumbu X, Y, dan Z.
  - Menghitung **Magnitudo Total** ($\sqrt{x^2 + y^2 + z^2}$) untuk analisis intensitas guncangan.
  - Frekuensi sampling yang dapat disesuaikan (default: tinggi) untuk menangkap getaran cepat.
- **GPS Geospatial Mapping**: Sinkronisasi setiap titik data getaran dengan koordinat Latitude, Longitude, Kecepatan, dan Akurasi lokasi.
- **Real-time Sensor Visualization**:
  - **Live Graph**: Menampilkan gelombang getaran akselerometer secara langsung saat berkendara.
  - **Magnitude Monitoring**: Indikator visual kekuatan guncangan.
- **Data Export & Analysis**:
  - Menyimpan data mentah dalam format **CSV** yang siap diolah (kompatibel dengan Excel, MATLAB, Python/Pandas).
  - Kolom data lengkap: `timestamp`, `accel_x`, `accel_y`, `accel_z`, `magnitude`, `latitude`, `longitude`, `speed`, `bearing`.
- **Background Service**: Proses perekaman berjalan di _foreground service_ untuk memastikan data tetap terekam meskipun layar mati atau aplikasi diminimalkan.
- **Visualisasi Perjalanan**:
  - Grafik Magnitudo Guncangan (Accelerometer Magnitude).
  - Peta rute perjalanan (dilengkapi dengan marker lokasi).
- **Manajemen Riwayat**: Simpan, lihat kembali, dan hapus riwayat perjalanan (_Trip History_).
- **Sinkronisasi Data**:
  - Penyimpanan lokal menggunakan CSV (untuk data sensor mentah) dan Room Database (untuk metadata).
  - Mekanisme _Auto-upload_ menggunakan **WorkManager** untuk mengirim data ke server saat kondisi jaringan memungkinkan.
- **Pengaturan Pengguna**: Kustomisasi profil pengguna (Nama, Email, Jenis Kendaraan) dan parameter sensor.

## 🛠️ Tech Stack & Library

Project ini dibangun menggunakan teknologi Android modern:

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI Toolkit**: [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material Design 3)
- **Architecture**: MVVM (Model-View-ViewModel)
- **Dependency Injection**: [Hilt](https://dagger.dev/hilt/)
- **Asynchronous**: Coroutines & Flow
- **Local Storage**:
  - [Room Database](https://developer.android.com/training/data-storage/room) (SQLite wrapper)
  - File System (CSV Storage)
  - DataStore (User Preferences)
- **Background Processing**:
  - [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager) (Reliable background jobs)
  - Foreground Services
- **Networking**: [Retrofit](https://square.github.io/retrofit/) + OkHttp
- **Sensors**: Android Sensor Framework (Accelerometer) & Google Location Services (FusedLocationProvider)

## 📂 Struktur Project

```
com.pemalang.roaddamage
├── data
│   ├── local      # Room DAO & Database
│   ├── prefs      # DataStore Preferences
│   └── remote     # Retrofit Service
├── di             # Hilt Dependency Injection Modules
├── model          # Data Classes & Entities
├── recording      # Service & Repository untuk sensor logic
├── sensors        # Handler untuk Accelerometer & GPS
├── ui
│   ├── navigation # Konfigurasi Navigasi Compose
│   └── screens    # Layar UI (Home, TripList, TripDetail, Settings)
├── util           # Utility classes (e.g., Distance calculation)
└── work           # WorkManager Workers (Upload logic)
```

## 🚀 Cara Menjalankan

1.  **Clone Repository**
    ```bash
    git clone https://github.com/username/RoadDamageDetector.git
    ```
2.  **Buka di Android Studio**
    - Pastikan menggunakan Android Studio versi terbaru (Hedgehog/Iguana atau lebih baru).
    - Tunggu proses _Gradle Sync_ selesai.
3.  **Build & Run**
    - Sambungkan perangkat Android fisik (disarankan karena emulator sulit mensimulasikan sensor akselerometer dengan akurat).
    - Pastikan izin Lokasi dan Notifikasi diberikan saat aplikasi pertama kali dijalankan.

## 📝 Requirements

- **Minimum SDK**: Android 8.0 (API Level 26)
- **Target SDK**: Android 14 (API Level 34)
- **Hardware**: Smartphone dengan sensor Akselerometer dan GPS.

## 📄 Lisensi

Project ini dibuat untuk tujuan penelitian dan pengembangan sistem deteksi kerusakan jalan.
