# Ringkasan Perbaikan

## v2.3 — Sinkronisasi Dokumen, Rapikan Layout, Radio Siaran, Jadwal Shalat Auto-Update

**1. Status dokumen tidak sinkron -- diperbaiki dengan menghapus penulis ganda.**
Root cause: checklist bisa diedit jamaah DI HP (tersimpan lokal) SEKALIGUS oleh
Admin di WP -- dua sumber saling menimpa. Sesuai saran Anda, sekarang:
- Status dokumen HANYA diisi Admin. Aplikasi jadi read-only, menampilkan
  "Lengkap / Tidak Lengkap / Belum Diperiksa" per dokumen.
- Layar ini sekarang digerbang login (data pribadi jamaah), sumbernya endpoint
  `/api/me` yang sama dipakai kartu Status Pembayaran.
- Sekalian menutup bug tersembunyi: kalau Admin belum pernah membuka halaman
  edit jamaah tertentu, WordPress mengembalikan checklist sebagai teks kosong
  bukan objek -- ini bisa membuat aplikasi gagal memuat data. Sudah diperbaiki
  di plugin (`get_checklist_for()`), berlaku juga untuk data lain yang serupa.

**2. Jarak kartu tumpang tindih di tab Saya -- diperbaiki.** Sebab: 4 kartu
(Status Pembayaran, Skrining, Radio, Profil) semua dijejalkan dalam SATU blok
`item{}` LazyColumn tanpa jarak sama sekali di antaranya -- `spacedBy` pada
LazyColumn cuma berlaku ANTAR `item{}`, bukan di dalam satu item yang isinya
banyak. Sekarang setiap kartu jadi `item{}` sendiri, jarak antar-kartu normal.

**3. Radio diubah jadi model siaran + Kode Radio per rombongan.** Sesuai
gambaran Anda (1 TL bicara, jamaah dengar): sekarang jamaah biasa otomatis
HANYA bisa mendengarkan. Admin mengisi "Kode Radio TL" per paket (tab Paket
Umrah di Pengaturan Aplikasi) dan memberikannya HANYA ke Tour Leader rombongan
itu. Tour Leader memasukkan kode itu di layar Radio untuk mengaktifkan tombol
bicara. **Dicek di server** (bukan cuma disembunyikan di tampilan), jadi tidak
bisa dilewati dengan mengedit tampilan aplikasi. Kalau Admin tidak mengisi kode
untuk suatu paket, kanal itu tetap terbuka (siapa saja boleh bicara) supaya
rombongan lama yang belum diatur kodenya tidak mendadak tidak bisa dipakai.

**4. Jadwal Shalat tidak update otomatis -- dua bug ditemukan & diperbaiki:**
- `lastKnownLocation` dipakai tanpa cek umur -- kalau HP sudah lama tidak dapat
  fix baru (GPS mati/di dalam gedung), lokasi yang dipakai bisa jadi cache lama
  dari kota sebelumnya. Sekarang hanya dipercaya kalau umurnya < 10 menit.
- Kartu jadwal shalat cuma menghitung SEKALI saat pertama muncul, tidak pernah
  diperbarui lagi walau jamaah pindah kota (Indonesia → Madinah → Makkah).
  Sekarang otomatis dihitung ulang setiap 5 menit. Ditambah timeout 15 detik
  supaya kalau tidak dapat fix GPS sama sekali, tidak menggantung selamanya.

## v2.2 — Nama Jamaah di Beranda, Radio Walkie-Talkie, Itinerary

1. **Label "Fitur 1" di form pendaftaran** — sudah bersih di update sebelumnya
   (v2.1); dicek ulang, tidak ada lagi teks "(Fitur N)" di layar mana pun yang
   dilihat jamaah. Sisa referensi "Fitur" cuma di komentar kode dan panel Admin WP.
2. **Nama jamaah di Beranda** — setelah "Assalamualaikum," sekarang tampil nama
   jamaah yang sedang login (mis. "Yasir Ismail"), bukan lagi nama travel. Kalau
   belum login, tetap fallback ke nama travel.
3. **Radio Tour Leader (Walkie-Talkie)** — file baru `Radio.kt`, diakses dari tab
   Saya (jadi otomatis hanya bisa dipakai setelah login). **Catatan jujur soal
   batasan teknis**: ini model *push-to-talk* ala Zello (tekan-tahan untuk
   merekam, lepas untuk kirim, penerima mendengar lewat polling ~2,5 detik) --
   BUKAN audio streaming langsung sungguhan seperti HT/radio fisik. Radio
   streaming real-time butuh infrastruktur WebRTC + server TURN/SFU yang jauh
   lebih besar dari stack WordPress+Node yang ada; kalau ke depan dibutuhkan itu,
   sebaiknya jadi proyek infrastruktur terpisah (bisa pakai layanan seperti Agora
   /Twilio). Kanal ditentukan otomatis dari paket_id jamaah (bukan input manual),
   jadi rombongan satu paket otomatis satu kanal dan tidak bisa dengar kanal lain.
   Pesan tersimpan di memori server maksimal 10 menit lalu otomatis hilang.
4. **Itinerary Umrah 9 Hari** — file baru `Itinerary.kt`, diakses dari kartu di
   Beranda, dengan toggle Ramadhan / Reguler (Non-Ramadhan). Versi Reguler saya
   sesuaikan sendiri dari contoh Ramadhan yang Anda berikan: sahur→bangun pagi+
   sarapan, buka puasa→makan malam, dan bagian tarawih dihapus -- rute kota &
   titik ziarah tetap sama. Tanggal/hari spesifik ("Sabtu 21 Feb") saya lepas
   jadi "Hari 1, 2, 3..." karena sifatnya template umum untuk semua grup
   keberangkatan, bukan tanggal satu keberangkatan tertentu.

## v2.1 — Bersihkan Data Palsu di Tab "Saya" + Tutup Celah Keamanan Terkait

Laporan Anda benar dan itu bug nyata, bukan cuma tampilan: kartu "Status Pembayaran"
masih memakai **angka contoh yang di-hardcode** dari kode awal ("Rp 37.400.000",
"Paket Linuwih") — tidak pernah disambungkan ke data jamaah yang sebenarnya login.
Selagi membenarkan ini saya juga menemukan (dan menutup) celah keamanan yang terkait:

1. **Endpoint baru `/api/me`** (WP: `/imtiyaz/v1/me`) — mengembalikan data pembayaran
   HANYA milik jamaah yang tokennya valid, bukan sekadar percaya ID yang dikirim.
   Endpoint lama `/api/jamaah/:id` yang dipakai sebelumnya ternyata **tidak mengecek
   token sama sekali** — siapa pun bisa lihat data pembayaran jamaah lain hanya
   dengan mengganti angka ID di request. Endpoint lama itu saya biarkan ada (dipakai
   untuk keperluan admin/internal lewat Basic Auth), tapi Android sekarang pakai
   `/api/me` yang aman untuk tampilan milik sendiri.
2. **Tab "Saya" sekarang menampilkan data asli**: nama paket (dicocokkan dari daftar
   paket admin), total tagihan, sudah dibayar, sisa, dan status — semua dari data
   yang benar-benar diisi Admin untuk jamaah yang login. Kalau Admin belum mengisi
   data pendaftaran jamaah tersebut, tampil pesan yang jujur ("belum diisi Admin")
   alih-alih angka palsu.
3. Setelah upload bukti transfer berhasil, data pembayaran otomatis dimuat ulang
   (status berubah jadi "Menunggu Verifikasi" tanpa perlu keluar-masuk app).
4. **Label internal dibersihkan**: teks seperti "(Fitur 2)", "(Fitur 3)", "(Fitur 8)"
   yang tadinya bocor ke tampilan jamaah (sisa istilah development) sudah dihapus
   dari semua layar.

## v2.0 — Splash Screen, Jadwal Shalat di Beranda, E-Quran, Audio Doa

**1. Splash Screen dengan logo lengkap.** Dua lapis: (a) tema Android
(`windowBackground`) menampilkan logo saat cold-start, menutup jeda kosong sebelum
Compose sempat menggambar; (b) layar splash Compose tampil 1,4 detik dengan logo
lengkap (burung + wordmark + kaligrafi) di atas putih, baru masuk ke halaman utama.
Tidak perlu library tambahan (pakai teknik `windowBackground` standar Android).

**2. Jadwal Shalat dipindah ke Beranda** sebagai kartu ringkas (6 waktu dalam satu
baris horizontal, waktu shalat yang sedang berlangsung disorot warna hijau). Tab
"Shalat" yang berdiri sendiri dihapus.

**Soal izin lokasi "diberikan saat instal"** — ini perlu saya perjelas: Android TIDAK
mengizinkan aplikasi manapun meminta izin dangerous (termasuk lokasi) saat proses
instal APK. Ini bukan keterbatasan aplikasi Anda, tapi aturan sistem Android sejak
versi 6.0 (2015) untuk semua aplikasi. Yang paling mendekati dan sudah saya terapkan:
dialog izin lokasi sekarang muncul **otomatis di awal, sebelum layar utama tampil**
(saat app pertama dibuka) — bukan lagi ditunda sampai user membuka fitur tertentu.

**3. E-Quran** (tab baru "Quran") — 114 surat lengkap: teks Arab, Latin, terjemahan
Indonesia, dan audio murottal per-ayat maupun per-surat. Pakai **equran.id API v2**:
gratis, tanpa API key, berbahasa Indonesia (cocok untuk jamaah), sudah termasuk audio
dari 6 qari (default: Misyari Rasyid Al-Afasi). Referensi: https://equran.id/apidev/v2

**4. Audio Doa** — admin sekarang bisa mengisi link mp3 untuk masing-masing 14 doa
lewat tab baru "Audio Doa" di Pengaturan Aplikasi (WP Admin), tanpa perlu update APK.
Teks doa tetap tersimpan offline di aplikasi (bisa dibaca tanpa internet seperti
sebelumnya) — audio saja yang butuh koneksi saat diputar. **Catatan jujur**: saya
tidak punya rekaman audio asli untuk 14 doa ini, jadi semua kolom URL di tab "Audio
Doa" sengaja saya biarkan kosong sampai Anda isi sendiri (rekaman sendiri, atau file
yang di-hosting di server/CDN Anda). Infrastrukturnya sudah lengkap — begitu Anda isi
URL mp3-nya, tombol putar otomatis muncul di aplikasi tanpa perlu saya sentuh lagi.

Pemutaran audio (doa & Quran) pakai `MediaPlayer` bawaan Android -- tidak menambah
dependency baru, supaya risiko build gagal tetap minimal.

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
