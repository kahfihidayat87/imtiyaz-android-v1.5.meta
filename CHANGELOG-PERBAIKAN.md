# Ringkasan Perbaikan

## v1.7.1 — Logo Resmi Sebagai Icon Aplikasi
Icon vector placeholder (lingkaran hijau bulan sabit) diganti logo asli Imtiyaz Tour &
Travel yang Anda kirim. Keputusan desain yang saya ambil, supaya "cantik & proporsional":
- **Hanya memakai simbol burung emasnya**, bukan logo lengkap dengan tulisan "IMTIYAZ
  TOUR & TRAVEL" + kaligrafi Arab. Icon aplikasi di launcher HP ditampilkan sangat kecil
  (48-192px) — teks tagline di ukuran itu jadi noda abu-abu tak terbaca, bukan terlihat
  profesional. Simbol burung saja jauh lebih ikonik dan tetap dikenali di ukuran kecil.
  (Logo lengkap dengan wordmark tetap cocok dipakai di tempat lain seperti splash screen
  atau header aplikasi kalau Anda mau — beri tahu saya kalau perlu.)
- Burung diekstrak otomatis dari file PNG (deteksi bentuk terhubung/connected-component,
  bukan crop kotak manual) supaya tulisan & kaligrafi terpotong bersih tanpa sisa piksel.
- Dibuatkan **adaptive icon** (`ic_launcher_foreground.png` + latar putih) untuk Android
  8+ — bentuknya otomatis menyesuaikan (bulat/persegi membulat/dll) tergantung launcher
  HP jamaah, standar icon modern. Untuk Android di bawah 8, disediakan icon PNG per
  kepadatan layar (mdpi–xxxhdpi) dengan latar putih + burung di tengah.

## v1.7.0 — Doa Umrah, Jadwal Shalat, dan Login Jamaah

**1. Doa Umrah** — dari 6 jadi 14 doa, disusun urut sesuai rangkaian ibadah: keluar
rumah → naik kendaraan/safar → talbiyah → niat umrah → masuk masjid → melihat Ka'bah
→ mulai tawaf → rukun Yamani → sa'i → tahallul → minum zamzam → ziarah Madinah. Teks
Arab/Latin/arti sudah saya validasi silang terhadap referensi klasik (Al-Qur'an &
hadis shahih yang jadi rujukan umum buku panduan umrah) sebelum ditulis ke kode.
*Doa "di Arafah" yang lama saya lepas karena itu ritual haji (wukuf), bukan umrah.*

**2. Jadwal Waktu Shalat** — file baru `PrayerTimes.kt`, metode **Ummul Qura** (Fajr
18,5°, Isya = Maghrib + 90 menit di luar Ramadan). Dihitung LOKAL di perangkat dari
GPS + tanggal & zona waktu HP (tidak butuh internet -- penting karena jamaah sering
tanpa data di Masjidil Haram/pesawat). Rumus astronomi sudah saya uji dulu di Python
dan dibandingkan terhadap pola waktu shalat Mekkah/Madinah/Jakarta yang dikenal umum
sebelum ditulis ke Kotlin. Pakai `LocationManager` bawaan Android (bukan Google Play
Services) supaya tidak menambah dependency baru yang berisiko bikin build gagal lagi.
Tab baru "Shalat" ditambahkan di bottom navigation.
*Catatan jujur: ini estimasi astronomis, bukan jadwal resmi -- ada disclaimer di UI
agar jamaah tetap merujuk ke adzan Masjidil Haram/Nabawi setempat.*

**3. Login Jamaah (username/password dibuat Admin)** — ini perbaikan keamanan, bukan
sekadar fitur baru. Sebelumnya field "ID Jamaah" adalah teks bebas TANPA password --
siapa pun yang tahu/menebak angka ID bisa upload bukti transfer, ubah checklist, atau
kirim data skrining kesehatan atas nama jamaah lain. Sekarang:
- Admin membuat username & password lewat metabox "Data Jamaah" di WP Admin (field
  password: kosongkan untuk tidak mengubah, sama seperti form user WordPress asli).
  Jamaah tidak pernah bisa mendaftar sendiri -- form ini cuma ada di dashboard admin.
- Endpoint baru `/imtiyaz/v1/login` (WP) dan `/api/login` (Node) mengeluarkan token
  setelah username+password cocok (`wp_check_password`, token disimpan sebagai post
  meta, dicek pakai `hash_equals` biar aman dari timing attack).
- Ketiga endpoint yang menulis data (`upload-bukti`, `update-checklist`, `skrining`)
  sekarang WAJIB menyertakan token yang valid -- ini pengecekan tambahan di dalam
  fungsi itu sendiri, terpisah dari Basic Auth Node→WP yang sudah ada sebelumnya
  (Basic Auth cuma membuktikan "ini server app.js kami", bukan "ini jamaah yang benar").
- Android: field ID Jamaah polos diganti `LoginScreen` sungguhan; token & ID disimpan
  di SharedPreferences setelah login sukses; kalau server bilang token tidak valid
  (kedaluwarsa/logout dari perangkat lain), app otomatis memaksa login ulang.

## v1.6.0 — Admin akses penuh atas Paket, Dokumen Wajib, dan Kontak/WA
Gap yang ditemukan: harga/fasilitas paket, daftar dokumen wajib, dan nomor WA/kontak
semuanya **hardcode di kode** (PHP maupun Kotlin) — admin tidak bisa mengubah apa pun
tanpa minta developer edit & build ulang. Diperbaiki di 3 layer sekaligus (kalau cuma
satu layer yang dibenahi, perubahan admin tidak akan pernah sampai ke aplikasi):

1. **Plugin WordPress**: halaman baru "Pengaturan Aplikasi" (menu Imtiyaz App) dengan
   3 tab — Paket Umrah (tambah/edit/hapus paket beserta harga & fasilitas), Dokumen
   Wajib (tambah/edit/hapus item checklist), dan Kontak & WA (nomor WA, nama travel,
   alamat, kontak yang tampil di app). Semua disimpan di `wp_options`, bukan lagi array
   di kode. Menu lama yang read-only ("Pilih Paket Umrah") dan terpisah ("Pengaturan WA")
   digabung jadi satu halaman ini biar admin tidak loncat-loncat menu.
2. **`app.js`**: endpoint baru `/api/dokumen` dan `/api/kontak` yang meneruskan data dari
   WordPress ke Android (sebelumnya cuma `/api/paket` yang ada, dua lainnya belum dibuat
   sama sekali).
3. **Android**: `listPaket`/`listDokumen` yang tadinya konstanta hardcode sekarang jadi
   `defaultPaket`/`defaultDokumen` — cuma dipakai sebagai fallback offline. Data aktif
   diambil dari server sekali saat app dibuka (`AppData` di `MainActivity.kt`), termasuk
   nomor WA yang sebelumnya juga hardcode di 2 tempat. Kalau fetch gagal (offline/server
   mati), app tetap jalan pakai nilai default, tidak crash.

**Catatan batasan**: konten Doa (tab "Doa") sengaja tetap hardcode/offline karena
memang dirancang bisa dibaca tanpa internet (di pesawat, di Masjidil Haram). Kalau
Anda ingin itu juga bisa diedit admin, itu perubahan arsitektur terpisah — beri tahu
saya kalau mau saya kerjakan juga.


## 1. File yang hilang — `android-app/app/build.gradle`
Dua file bernama sama ter-upload (root & app-level), satu menimpa yang lain sehingga app-level `build.gradle` hilang. **Dibuat ulang dari nol** berdasarkan import yang dipakai di `MainActivity.kt` (Compose, Material3, activity-compose) + `namespace` sesuai package `com.imtiyaztour.app`. **Tolong bandingkan dengan versi asli Anda** (jika ada) untuk memastikan tidak ada dependency lain yang saya lewatkan.

## 2. Android app sama sekali tidak terhubung ke backend
- Ditambahkan lapisan networking (Retrofit + OkHttp) yang sebelumnya tidak ada.
- **Fitur 3 (Checklist)**: sekarang tersimpan permanen di perangkat (SharedPreferences) dan disinkron ke `/api/update-checklist`.
- **Fitur 2 (Upload Bukti)**: tombol "Galeri" & "Kamera" sebelumnya `onClick = {}` (kosong) — sekarang benar-benar membuka galeri/kamera dan meng-upload ke `/api/upload-bukti`.
- **Fitur 8 (Skrining)**: tombol "Kirim ke Admin" sebelumnya cuma menutup form — sekarang benar-benar POST ke `/api/skrining`.
- Ditambahkan field **"ID Jamaah"** di tab Saya karena fitur-fitur di atas butuh cara mengaitkan data ke satu jamaah tertentu di WordPress — sebelumnya tidak ada konsep ini sama sekali di app. **Ini keputusan desain, bukan sekadar bug-fix — silakan diskusikan dengan tim apakah ID ini diberikan manual oleh admin, atau perlu sistem login yang lebih formal.**
- Permission `CAMERA` + `FileProvider` ditambahkan di `AndroidManifest.xml` (dibutuhkan untuk fitur kamera yang baru berfungsi).

## 3. Celah keamanan kritis — REST API WordPress tanpa autentikasi
Semua route (`upload-bukti`, `update-checklist`, `skrining`, `jamaah`, `jamaah/:id`) sebelumnya `permission_callback => __return_true` — siapa pun bisa membaca data pribadi jamaah dan mengubah status pembayaran/checklist orang lain (IDOR).
**Fix**: route sensitif sekarang mewajibkan user terautentikasi (`current_user_can('edit_posts')`), dan `app.js` mengirim Basic Auth (WordPress Application Password) di semua panggilan ke route tersebut — bukan cuma untuk upload media seperti sebelumnya.
Route `paket` dan `wa-admin` tetap publik karena tidak mengandung data pribadi.

## 4. `app.js`
- Menghapus fallback kredensial palsu (`admin` / `xxxx xxxx xxxx xxxx`) — server sekarang **gagal start dengan pesan jelas** jika `.env` belum diisi, bukan diam-diam pakai kredensial salah.
- Folder `uploads/` dibuat otomatis (`fs.mkdirSync recursive`) — sebelumnya tidak dijamin ada.
- `multer` sekarang membatasi tipe file ke gambar saja (dulu terima file apa saja).
- File temporary dihapus juga saat upload gagal (`finally` block) — dulu hanya dihapus saat sukses.
- Ditambahkan `requireApiKey` middleware opsional (`x-api-key` header) sebagai lapisan tambahan di endpoint POST.

## 5. `gradlew` & Gradle Wrapper
File `gradlew` asli hanya stub 4 baris tanpa `gradle-wrapper.jar`. **Diganti dengan script wrapper resmi Gradle** + jar yang valid, supaya build bisa jalan tanpa bergantung pada step "Generate Wrapper" di CI (step itu sudah dihapus dari `build-apk.yml`, disederhanakan).

## 6. Yang TIDAK bisa saya verifikasi di sini
Sandbox saya tidak punya Android SDK/emulator maupun akses ke Maven Google, jadi saya **tidak bisa benar-benar mengompilasi APK ini**. Yang saya lakukan adalah audit baris-per-baris + pencocokan versi dependency terhadap dokumentasi resmi (Kotlin 1.9.10 ↔ Compose compiler 1.5.3, dst). **Rekomendasi: push ke branch test dan jalankan `build-apk.yml` untuk konfirmasi akhir**, lalu kabari saya kalau ada error spesifik dari log CI — saya bisa bantu debug dari situ.
