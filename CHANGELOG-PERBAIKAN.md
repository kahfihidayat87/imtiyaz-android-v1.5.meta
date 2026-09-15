# Ringkasan Perbaikan

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
