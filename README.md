# Ruang Hari — aplikasi agenda, tabungan, dan target (Android)

## Fitur versi 1.0
- Agenda per tanggal: tambah, ubah, hapus, tandai selesai.
- Pengingat berdasarkan tanggal dan jam dengan notifikasi prioritas tinggi dan suara alarm.
- Catatan tabungan harian dan total saldo tabungan yang dicatat.
- Target dengan progres numerik.
- Penyimpanan lokal di perangkat; tidak memerlukan akun atau server.
- Palet biru lembut, putih, abu-abu, dan hijau.

## Membuka proyek
1. Instal Android Studio di komputer.
2. Pilih **Open** lalu pilih folder `RuangHariAndroid`.
3. Tunggu Gradle Sync selesai.
4. Hubungkan HP Android dengan USB debugging atau jalankan emulator.
5. Tekan **Run** untuk memasang aplikasi.

## Membuat APK
Di Android Studio: **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
APK debug biasanya berada di `app/build/outputs/apk/debug/app-debug.apk`.

## Penting tentang notifikasi dan alarm
- Izinkan notifikasi saat diminta.
- Pada Android 12+, izinkan **Alarms & reminders / Alarm dan pengingat** bila diminta sistem.
- Penghemat baterai pada beberapa merek HP dapat menunda alarm; izinkan aktivitas latar belakang/autostart jika diperlukan.
- Android tidak mengizinkan aplikasi biasa membuat notifikasi yang benar-benar mustahil dihapus oleh pengguna. Notifikasi dapat dihapus/dimatikan melalui kontrol sistem. Ruang Hari memakai notifikasi prioritas tinggi, tetapi tetap mengikuti kendali Android.
- Jika aplikasi dipaksa berhenti oleh pengguna atau izin notifikasi dicabut, pengingat mungkin tidak tampil sampai aplikasi dibuka lagi.

## Catatan teknis
Proyek ini adalah kode sumber Android native (Java), `minSdk 26` (Android 8.0+), tanpa pustaka UI pihak ketiga. Lingkungan pembuatan ini tidak menyertakan Android SDK/Gradle sehingga APK tidak dapat dikompilasi langsung di sini; proyek disiapkan agar bisa dibuka dan dibangun melalui Android Studio.
