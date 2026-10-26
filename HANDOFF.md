# Imtiyaz Tour — Project Handoff Document

**Terakhir update:** 14 Okt 2026 (v2.15.0, TAHAP 25 SELESAI)
**Repo utama:** https://github.com/kahfihidayat87/imtiyaz-android-v1.5.meta
**Local path:** C:\Users\DELL\Documents\GitHub\imtiyaz-android-v1.5.meta

---

## Status Saat Ini

| Item | Nilai |
|---|---|
| Versi aktif (App Jamaah) | **2.13.0** (versionCode 15) — rilis terbaru: v2.13.0-build115 |
| Commit terakhir | Lihat `git log --oneline -5` |
| Branch | `main` |
| CI | GitHub Actions hijau |
| Total file Kotlin | **34 file** (+SetupWizard, +LocationCache, +KirimLokasiWorker, +OtaDetector, +AlertIzinHelper) |
| Warna tema | Hijau `#0F7A5A` |
| Distribusi | Play Store (AAB) + APK Release signed (langsung) |
| **Tracking lokasi** | **FCM + Firestore** (menggantikan ntfy.sh) |
| **Firebase project** | `imtiyaztourapp` |

---

## Struktur Project (4 Komponen)

### 1. App Jamaah — `imtiyaz-android-v1.5.meta`
- Package: `com.imtiyaztour.app`
- Distribusi: Play Store (AAB) + APK signed via GitHub Release
- Warna: Hijau `#0F7A5A`

### 2. App Admin — `imtiyaz-admin-android`
- Package: `com.imtiyaztour.admin`
- Distribusi: Private APK
- Warna: Biru `#1E3A8A`
- Status: Belum dikirim ke konteks ini

### 3. WordPress Plugin — `pastiumrah.com`
- Path: `public_html/wp-content/plugins/connector-app/`
- File utama: `imtiyaz-connector.php`, `imtiyaz-admin-endpoints.php`, `imtiyaz-admin-ui-trait.php`
- Custom Tables: `wpfq_imtiyaz_locate_req`, `wpfq_imtiyaz_locate_res`, `wpfq_imtiyaz_admin`
- Endpoint REST: ~30 endpoint

### 4. Node.js Proxy — `api.pastiumrah.com`
- File: `app.js` v2.12.0 (~700 baris)
- Host: Hostinger (Node.js LiteSpeed + CloudLinux)
- Port: 3000
- Node binary: `/opt/alt/alt-nodejs24/root/usr/bin/node`
- Python binary: `/opt/alt/python311/bin/python3.11`
- Fungsi: Proxy + cache + upload handler + **FCM sender**
- Endpoint kunci: `/api/find`, `/api/track-ping`, `/api/kanal-jamaah`, `/api/radio/send`, `/api/radio/poll`

---

## File Kotlin di App Jamaah (25 file, per 27 Sep 2026)

| File | Fungsi |
|---|---|
| MainActivity.kt | Entry point, navigasi 6 tab, splash, FCM subscribe |
| Adzan.kt | Notifikasi adzan otomatis |
| AlMatsurat.kt | Al-Ma'tsurat Sughra |
| Announcement.kt | Info dari admin + notif |
| AudioPlayer.kt | MediaPlayer shared helper |
| FindJamaah.kt | UI TL cari jamaah + Firestore listener |
| HafalanQuran.kt | Quiz Tebak Kata + Susun Ayat (Juz 30) |
| Invoice.kt | Daftar invoice PDF jamaah |
| Itinerary.kt | Rencana perjalanan 9 hari |
| JadwalKeberangkatan.kt | Trip real-time WP Travel Engine |
| Journal.kt | Catatan perjalanan lokal |
| LocateService.kt | Foreground service tracking (legacy ntfy) |
| LocationHelper.kt | GPS multi-sample + adaptive battery |
| LokasiMapView.kt | Peta inline OSM (osmdroid) |
| Manasik.kt | 14 langkah interaktif + progress |
| Pembimbing.kt | Profil ustadz + audio ceramah |
| PrayerTimes.kt | Jadwal shalat Ummul Qura |
| Quran.kt | 114 surat via equran.id v2 |
| Radio.kt | Voice note TL (polling 2.5s) |
| Reminder.kt | Pengingat ibadah & kesehatan |
| SusunAyat.kt | Susun potongan ayat per ronde (drag & drop) |
| FcmService.kt | Penerima FCM: onNewToken + onMessageReceived |
| FcmTokenStore.kt | Simpan token & kirim ke Firestore |
| KirimLokasiHelper.kt | Kirim GPS ke Firestore saat dapat FCM |
| NotifikasiHelper.kt | Tampilkan notifikasi lokal |
| SetupWizardScreen.kt | Wizard setup 3-langkah (PIN lokasi, notif, baterai) |
| LocationCache.kt | Cache GPS terakhir untuk kirim cepat saat FCM masuk |
| KirimLokasiWorker.kt | WorkManager fallback kirim lokasi (FGS ditolak OS) |
| OtaDetector.kt | Deteksi OTA + cek izin + kirim alert ke TL |
| AlertIzinHelper.kt | Kirim alert izin ke Firestore alert_izin |
| ModeAmanService.kt | Foreground service Mode Aman (tracking lokasi saat app background) |
| RadioLiveKit.kt | Wrapper LiveKit SDK untuk siaran radio real-time |
| SkriningFormV2.kt | Form skrining kesehatan multi-step 7 langkah |
| SkriningModels.kt | Model data + validasi skrining kesehatan |

---

## Fitur di App Jamaah

1. Jadwal Shalat (Ummul Qura + notifikasi adzan)
2. Al-Quran (114 surat + audio 6 qari)
3. Doa (14 doa + audio opsional dari admin)
4. Manasik (14 langkah + progress)
5. Journal (catatan perjalanan lokal)
6. Reminder (ibadah + kesehatan)
7. Pengumuman (dari admin)
8. Radio Tour Leader (push-to-talk via polling)
9. Tracking Lansia via FCM + Firestore (menggantikan ntfy)
10. Pembimbing (profil + audio ceramah)
11. Itinerary & Jadwal Keberangkatan
12. Checklist Dokumen (diisi admin)
13. Upload Bukti Transfer
14. Skrining Kesehatan 29 pertanyaan
15. Susun Ayat
16. Quiz Hafalan Juz 30
17. Al-Ma'tsurat Sughra
18. Tracking Istiqamah
19. Dokumen ke Saya
20. Peta Multi-Jamaah

---

## Riwayat TAHAP

### TAHAP 1-2.5 - Fix CI & Polish Peta (SELESAI)
- 9a2b0fe, aca2c24

### TAHAP 3-7 - Fitur Tracking, Invoice, Peta, Quiz (SELESAI)
- fdee216, 1d0a56e, d6c8c4d, e7cc984, bd0a1d7, 97fcaf0, cbf6012, 9b0acf2

### TAHAP 8 - Hotfix ColumnScope di SusunAyat.kt (SELESAI)
- Commit: 944a669

### TAHAP 9 - Update HANDOFF.md v1 (SELESAI)
- Commit: 6b82eef

### TAHAP 10 - Quiz Polish (SELESAI)
- Commit: 8860c05, c074a4e, 1c47343

### TAHAP 11 - Build APK Release Signed (SELESAI)
- File: .github/workflows/build-apk.yml
- Link: https://github.com/kahfihidayat87/imtiyaz-android-v1.5.meta/releases/latest

### TAHAP 12 - Fix Tracking Lokasi via API (SELESAI)
- Server: fix EADDRINUSE, rate limit 30s->10s, pesan 429 ramah
- Android: polling 2s->5s, durasi 90s->60s, disable tombol saat loading
- Commit: d52b6e7

### TAHAP 13 - Migrasi ke FCM + Firestore (SELESAI)
- Alasan: ntfy.sh limit 250/hari + 502 error + single point of failure
- FASE 1: Firebase project imtiyaztourapp, Firestore asia-southeast2
- FASE 2: Gradle plugin google-services 4.4.2, Firebase BOM 33.7.0
- FASE 3: 4 file Kotlin baru (FcmService, FcmTokenStore, KirimLokasiHelper, NotifikasiHelper)
- FASE 4: FindJamaah.kt ganti polling dengan addSnapshotListener
- FASE 6: Node.js install firebase-admin@14.5.0, modular import, spawn manual
- FASE 7: MainActivity subscribe topic imtiyaz-loc-j{jamaahId}
- Firestore Rules: allow read/write if true (DEV ONLY)

### TAHAP 14 - Uji End-to-End FCM + Fix Peta (SELESAI)
**Uji end-to-end FCM sukses:**
- HP TL tap Cari -> server kirim FCM ke HP jamaah -> jamaah kirim GPS ke Firestore -> TL terima real-time
- Tracking lokasi end-to-end berjalan dengan baik

**Fix 1 - Peta inline baca Firestore real-time:**
- Sebelumnya: peta inline pakai API /api/kanal-lokasi (WordPress DB, lambat, cache)
- Tombol Google Maps pakai Firestore (real-time)
- Efek: koordinat peta beda dengan tombol Google Maps
- Solusi: LokasiMapView.kt ganti ke addSnapshotListener Firestore
- Filter: hanya tampilkan jamaah dari kanal TL (daftarJamaahKanal map)
- Label UI: Auto-refresh 30 detik -> Real-time

**Fix 2 - Format geo: intent Google Maps:**
- Sebelumnya: geo:lat,lon?q=lat,lon(label) - tidak reliable
- Efek: Google Maps kadang abaikan koordinat dan pakai lokasi user
- Solusi: geo:0,0?q=lat,lon(label) - format standar yang reliable
- Tambah safeLabel (max 50 char) untuk cegah URL rusak

**Fix 3 - Marker overlap di peta:**
- Dua jamaah di lokasi berdekatan -> tooltip marker salah (nampil nama jamaah lain)
- Solusi: sortedBy jamaah_id == highlightJamaahId - marker target di-add paling akhir
- Marker target muncul di atas saat marker bertumpuk

**Commit:** lihat git log --oneline -5

### TAHAP 15 - Belum Ditentukan
Kandidat:
- A. Firebase Custom Auth (production-ready, gantikan rules terbuka)
- B. Bump versi ke 2.13.0 + changelog rilis
- C. Supervisor auto-respawn Node.js (LiteSpeed tidak auto-respawn)
- D. Deprecate LocateService.kt (ntfy legacy, sudah tidak dipakai)
- E. Quick action layar Find Jamaah (copy koordinat, share WA)
- F. RBAC dan keamanan App Admin
- G. Update HANDOFF.md otomatis via CI

---

## File Kritis

- MainActivity.kt (Jamaah) — navigasi, FCM subscribe
- FindJamaah.kt — UI TL + Firestore listener
- FcmService.kt — penerima FCM
- PrayerTimes.kt — hitung jadwal astronomis
- LocateService.kt — service ntfy (legacy)
- app.js (Node) — proxy + FCM sender
- imtiyaz-connector.php — plugin WP utama
- .github/workflows/build-apk.yml — CI
- android-app/app/google-services.json — Firebase config
- service-account.json (server) — JANGAN COMMIT
- imtiyaz-release-key.jks (LOKAL) — signing keystore
- .env (server) — credentials WP + FIREBASE

---

## Distribution & Release

### Play Store (AAB)
- Upload AAB dari artifact Imtiyaz-AAB-PlayStore

### APK Release (Langsung)
- Otomatis dibuat setiap push ke main
- Link: https://github.com/kahfihidayat87/imtiyaz-android-v1.5.meta/releases/latest

### Secrets GitHub
- RELEASE_KEYSTORE_BASE64
- RELEASE_KEYSTORE_PASSWORD
- RELEASE_KEY_ALIAS
- RELEASE_KEY_PASSWORD

---

## Environment

### Lokal
- Windows 11, Git Bash (MINGW64)
- Repo: C:/Users/DELL/Documents/GitHub/imtiyaz-android-v1.5.meta
- Tools: Git Bash, Python 3.14, gh CLI, scp, ssh

### Server Hostinger
- SSH: ssh -p 65002 u120369480@153.92.10.222
- Node: /opt/alt/alt-nodejs24/root/usr/bin/node (v24.6.0)
- NPM: /opt/alt/alt-nodejs24/root/usr/bin/npm
- Python: /opt/alt/python311/bin/python3.11
- App dir: ~/domains/api.pastiumrah.com/hbuilds/current/nodejs/
- Logs: console.log, stderr.log
- LiteSpeed TIDAK auto-respawn - kill = server mati total

### Firebase
- Project ID: imtiyaztourapp
- Firestore region: asia-southeast2
- Console: https://console.firebase.google.com/project/imtiyaztourapp

---

## Konvensi

1. Script patch disimpan lokal (patch-*.py), TIDAK di-commit
2. File backup *.bak-*, TIDAK di-commit
3. Line endings: LF
4. Signing release: env variable (GitHub Secrets)
5. Version bump setiap rilis
6. Commit kecil per tahap, tunggu CI hijau
7. JANGAN paste token/password/service-account.json ke chat
8. Update HANDOFF.md setiap batch fitur baru
9. JANGAN paste Kotlin/XML langsung ke Git Bash
10. Verifikasi brace balance sebelum commit Kotlin

---

## Catatan untuk Percakapan Berikutnya

1. Konteks hilang setiap chat baru - kirim file ini + repo link
2. Format: Bahasa Indonesia, tabel, step-by-step Git Bash
3. Commit kecil, tunggu CI hijau
4. Hindari emoji berlebihan
5. Checklist verifikasi saat mulai chat:
   - git log --oneline -10
   - gh run list --limit 5
   - gh secret list
   - sed -n '1,40p' HANDOFF.md
   - ssh -p 65002 u120369480@153.92.10.222 "ps aux | grep api.pastiumrah | grep -v grep"

---

## Link Penting

- GitHub Actions: https://github.com/kahfihidayat87/imtiyaz-android-v1.5.meta/actions
- GitHub Releases: https://github.com/kahfihidayat87/imtiyaz-android-v1.5.meta/releases
- GitHub Secrets: https://github.com/kahfihidayat87/imtiyaz-android-v1.5.meta/settings/secrets/actions
- Firebase Console: https://console.firebase.google.com/project/imtiyaztourapp
- Firestore: https://console.firebase.google.com/project/imtiyaztourapp/firestore

---

## Deprecation Log

- **v2.13.0 (TAHAP 15)**: `LocateService.kt` (ntfy.sh) di-deprecate.
  - Call `LocateService.start()` di `MainActivity.kt` dihapus.
  - `<service android:name=".LocateService" />` dihapus dari `AndroidManifest.xml`.
  - Class ditandai `@Deprecated` — file tetap ada untuk 1 rilis.
  - Tracking lokasi sepenuhnya via **FcmService** (`KirimLokasiHelper` → Firestore `lokasi_jamaah/{jamaahId}`).
  - **Akan dihapus total di v2.14.0.**

## Roadmap TAHAP 15 Lanjutan

| Batch | Isi | Status |
|---|---|---|
| 2 | Deprecate LocateService | ✅ |
| 3 | FCM high-priority + data-only (server `app.js`) | ⏳ |
| 4 | LocationCache + KirimLokasiHelper v2 (kirim 2×) | ⏳ |
| 5 | Battery whitelist + wake-lock | ⏳ |

Target: lokasi TL muncul < 5 detik, HP tetap bangun di merek agresif (Xiaomi/Oppo/Vivo/Samsung).

---

## Prosedur Restart Node.js (Battle-Tested)

**Script:** `bash scripts/restart-node.sh`
**SSH:** `ssh -p 65002 u120369480@153.92.10.222`
**App dir:** `~/domains/api.pastiumrah.com/hbuilds/current/nodejs`
**Node binary:** `/opt/alt/alt-nodejs24/root/usr/bin/node`

**Karakteristik:** Node di-spawn manual via `nohup` — **tidak auto-respawn**. Setelah edit `app.js`, wajib jalankan script restart.

**Startup sukses ditandai dengan:**
- `[FCM] Firebase Admin SDK initialized`
- `Imtiyaz API v2.12.0 jalan di port 3000`
- `stderr.log` kosong

**Catatan:** Restart manual menggantikan proses `lsnode` (LiteSpeed wrapper). Setelah restart, pastikan hanya **1 PID** `node app.js` aktif — hindari duplikasi instance yang rebutan port 3000.

### TAHAP 15 - Batch 3 (SELESAI): FCM High-Priority + CollapseKey

**Server:** `~/domains/api.pastiumrah.com/hbuilds/current/nodejs/app.js`

**Patch `fcmMintaLokasi()` (line 408-430):**
- ✅ Hapus fallback `ntfyPublish()` — error langsung `throw` (biar terlihat di log & response `/api/find`)
- ✅ Tambah `collapseKey: 'minta-lokasi-j${jamaahId}'` — cegah FCM duplikat kalau TL tap Find berkali-kali dalam <60s
- ✅ Sudah ada sebelumnya: `priority: 'high'`, `ttl: 60000`, data-only payload

**Target:** lokasi TL muncul < 5 s. HP jamaah bangun meski Doze mode (FCM high-priority menembus Doze).

**Backup:** `/tmp/app.js.20260927-150538.bak` (di server)

**Catatan server:**
- Runtime: `node app.js` spawn manual via `nohup`, TIDAK auto-respawn
- **Ditemukan PID eksternal** (`381103`) saat restart — kemungkinan spawn dari LiteSpeed wrapper. Perlu investigasi kandidat C (Supervisor auto-respawn).
- Restart via `bash scripts/restart-node.sh`

**Verifikasi:**
- `node --check app.js` → Syntax OK
- Startup log: `[FCM] Firebase Admin SDK initialized` + `Imtiyaz API v2.12.0 jalan di port 3000`
- Stderr kosong
- PID tunggal (`607949`), health endpoint OK

---

## TAHAP 20 — Batch 1b Minify + Fix Fragment (9 Okt 2026)

**Status:** ✅ Selesai & teruji

### Perubahan
- Fix fragment deprecated (1.0.0 → 1.8.5) — Play Console warning #2
- Enable R8 minify + shrinkResources — Play Console warning #1
- proguard-rules.pro 163 baris (Retrofit, Gson, OkHttp, Firestore, LiveKit, osmdroid)
- Nuclear fix: keep seluruh package `com.imtiyaztour.app.**`
- Bump versi 2.13.0 (code 15)

### Hasil
- APK release: **36 MB → 28 MB** (-22%)
- Warning Play Console DEX ✅ teratasi
- Warning Play Console Fragment ✅ teratasi

### Pelajaran Kritis
- R8 default terlalu agresif untuk Retrofit/Gson — login gagal tanpa pesan error
- Debug APK adalah alat diagnosa: kalau debug OK tapi release gagal → **proguard issue**
- Nuclear fix (keep seluruh package app) = trade-off size vs fungsional
- Selalu test 9 fitur setelah minify: login, skrining, radio, peta, mode aman, quran, doa, jadwal, logout

### File Backup
- `app/build.gradle.bak-*`
- `app/proguard-rules.pro.bak-*`
- `.github/workflows/build-apk.yml.bak-*`


---

## Hotfix — Block MEDIA_PROJECTION (9 Okt 2026)

**Status:** Selesai & teruji

### Masalah
Google Play Console mendeteksi 2 FGS di AAB:
- FOREGROUND_SERVICE_LOCATION (dipakai — Mode Aman)
- FOREGROUND_SERVICE_MEDIA_PROJECTION (TIDAK dipakai)

Root cause: LiveKit SDK manifest merger otomatis menambahkan MEDIA_PROJECTION.

### Fix
Tambah di AndroidManifest.xml:

    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION" tools:node="remove" />

### Verifikasi
- Merged manifest debug: MEDIA_PROJECTION = 0
- Play Console: FGS error hilang

### Pelajaran
LiveKit SDK menambahkan FGS MEDIA_PROJECTION (untuk screen share). Kita tidak pakai screen share, jadi wajib di-block dari manifest merger.


---

## Cek Kesehatan Jamaah (9 Okt 2026)

**Status:** ✅ Live

### Fitur Baru
Card "Cek Kesehatan" di Saya Screen (antara Skrining & Mode Aman).

### Konten
Menampilkan hasil cek kesehatan yang diisi admin:
- Tensi, Gula Darah, Asam Urat, Kolesterol
- Petugas, Catatan, Update terakhir

### File Terkait
- `MainActivity.kt` — SayaScreen (baris ~1008)
- API: `POST /wp-json/imtiyaz/v1/kesehatan-me`
- Data class: `KesehatanMeResponse`, `KesehatanItemConfig`, `KesehatanData`

### Empty State
Kalau belum ada data: "Belum ada data kesehatan. Hubungi Tour Leader atau Admin."

---

## TAHAP 16 — Mode Aman (Foreground Service)

**Tanggal:** 29 Sep 2026
**Status:** ✅ Selesai, teruji

### Tujuan
HP jamaah tetap respon terhadap permintaan lokasi dari TL meski:
- App background
- Di-swipe dari Recents
- Low memory (OS kill)

### Arsitektur "Mode Aman" — 3 Layer

| Layer | Mekanisme | Skenario |
|---|---|---|
| 1 | Foreground Service (notifikasi permanen) | App di-background |
| 2 | `onTaskRemoved()` → restart via AlarmManager | App di-swipe dari Recents |
| 3 | FCM High-Priority (sudah ada) | App mati / Doze mode |

### File Terkait

| File | Fungsi |
|---|---|
| `ModeAmanService.kt` (~188 baris) | FGS `location` type + refresh Firestore tiap 60 detik + `START_STICKY` |
| `AndroidManifest.xml` | +`<service android:name=".ModeAmanService" android:foregroundServiceType="location" />` |
| `MainActivity.kt` | Card UI di `SayaScreen` + permission launcher |
| `Radio.kt` | Rapikan UI (tombol "Mulai Mendengarkan") |

### Batasan (Disadari)
- Force Stop oleh user → **tidak bisa wake** (batasan OS)
- Android 15+ → FGS lebih agresif di-kill

---

## TAHAP 17 — Skrining Kesehatan V2 (Full Native Form)

**Tanggal:** 29 Sep 2026
**Status:** ✅ Selesai, teruji end-to-end

### File Terkait

| File | Baris | Fungsi |
|---|---|---|
| `SkriningFormV2.kt` | 505 | UI multi-step 7 langkah (8 section A-H, ~32 pertanyaan) |
| `SkriningModels.kt` | 281 | Model data + `validateStep` + `toMap` + `SkriningOptions` |
| `MainActivity.kt` | — | Ganti `SkriningForm` → `SkriningFormV2`, signature `submitSkrining` jadi `Map<String, @JvmSuppressWildcards Any>` |

### Struktur Form
8 section: A) Data diri, B) Riwayat umrah, C) Riwayat kesehatan, D) Mobilitas, E) Aktivitas harian, F) Kognitif, G) Kesiapan ibadah, H) Diet & asuransi.

### Server
- Node.js `/api/skrining` = murni proxy (tidak perlu ubah)
- WP `api_skrining()` = fleksibel, simpan JSON di `_skrining_data`
- WP Metabox `imtiyaz_render_skrining_metabox` ditambah di `imtiyaz-connector.php`

### Pelajaran Kritis
- ❌ Jangan taruh `Column(verticalScroll())` di dalam `LazyColumn` → crash
- ✅ Pakai `rememberSaveable` (bukan `remember`) untuk state di `LazyColumn`
- ✅ Retrofit + `Map<String, Any>` butuh `@JvmSuppressWildcards`
- ✅ Auto-save pakai `LaunchedEffect(step)` (bukan `LaunchedEffect(data)`)

---

## Koreksi Catatan Server Node.js (10 Okt 2026)

**Temuan dari diagnostik SSH:**

PID 3347168 lsnode:/home/u120369480/domains/api.pastiumrah.com/...
exe → /opt/alt/alt-nodejs24/root/usr/bin/node

- `lsnode` **BUKAN** wrapper + child — `readlink /proc/PID/exe` = binary Node.js.
- argv[0] di-spoof oleh LiteSpeed jadi `lsnode:...`, tapi itu **proses node yang menjalankan `app.js`**.
- Hanya **1 proses** aktif untuk `api.pastiumrah.com`.
- Uptime sejak **08 Okt 2026** (>2 hari), stderr kosong, FCM terkirim rutin.

**Revisi catatan lama:**
> ~~"Node di-spawn manual via `nohup` — tidak auto-respawn."~~

Menjadi:
> **LiteSpeed menjalankan & menjaga proses Node.js (`lsnode`). Setelah edit `app.js`, jalankan `bash scripts/restart-node.sh` untuk memuat perubahan. Proses otomatis di-respawn oleh LiteSpeed wrapper jika crash. Hindari duplikasi PID (port 3000 hanya boleh dipegang 1 proses).**

---

## TAHAP 22 — Zero-Touch Tracking (10 Okt 2026)

**Status:** ✅ Selesai, rilis build 115

### Tujuan
Jamaah lansia tidak perlu klik apapun setelah setup awal. Setup wizard muncul
sekali setelah login pertama, lalu Mode Aman auto-start dan selamanya jalan
di background.

### Sub-batch

| Batch | Isi | Commit |
|---|---|---|
| **4a** | Setup wizard 3-langkah + auto-start Mode Aman | `70a8842` + `49bc44c` |
| **4a-text** | Perbaikan teks (PIN Lokasi, batrai ON) | `3928d17` |
| **4d** | LocationCache + kirim 2x + WakeLock + warna marker | `2d1a103` |
| **4b** | BootReceiver auto-start + WorkManager fallback | `80ee4fd` |
| **4c-1** | Server: endpoint heartbeat + alert-izin | (server-only) |
| **4c-2** | Workflow GitHub Actions cron 6 jam | `753724d` |
| **4c-3** | OTA detection + heartbeat handler + banner TL | `57123e9` |

### Fitur Baru

**Setup Wizard (SetupWizardScreen.kt):**
- Muncul otomatis setelah login pertama (kalau `setup_complete=false`)
- Step 1: PIN lokasi (FINE + BACKGROUND)
- Step 2: Izin notifikasi
- Step 3: Battery unrestricted
- Tombol "Lewati" + "Jalankan Ulang Setup" di Card Mode Aman
- Tombol lama "Aktifkan Mode Aman" tetap berfungsi (antisipasi skip/tolak izin)

**LocationCache + Kirim 2x:**
- `KirimLokasiHelper.kirimLokasi()` jadi **suspend fun** (bug fix, lihat di bawah)
- Alur: cache dulu (instant) → GPS akurat → kirim ulang → update cache
- Efek: TL lihat marker <1 detik dari cache, lalu refresh saat GPS akurat

**WakeLock (FcmService):**
- PARTIAL_WAKE_LOCK 90s selama proses kirim lokasi
- Cegah Doze kill coroutine sebelum Firestore write selesai

**BootReceiver (extend di Adzan.kt):**
- Auto-start Mode Aman setelah HP restart
- Handle QUICKBOOT_POWERON Xiaomi/Oppo/Vivo

**WorkManager Fallback (KirimLokasiWorker.kt):**
- Kalau FGS start ditolak OS (Android 12+) → WorkManager retry 3x
- Exponential backoff 15s

**Heartbeat FCM (cron 6 jam):**
- Workflow `.github/workflows/heartbeat.yml`
- Server `/api/heartbeat-broadcast` (guard API key)
- Bangunkan HP dari App Standby Bucket Android

**OTA Detection (OtaDetector.kt):**
- Cek `Build.FINGERPRINT` berubah → verifikasi izin
- Kalau izin hilang → `AlertIzinHelper` kirim ke Firestore `alert_izin/{jamaahId}`
- Throttle 1 jam per jamaah

**Banner TL (FindJamaah.kt):**
- Banner merah real-time kalau ada jamaah izin hilang
- Filter: hanya jamaah di kanal TL
- Format: `⚠ N Jamaah Butuh Perhatian` + nama + reason + umur

**Marker Warna (LokasiMapView.kt + FindJamaah.kt):**

| Kondisi | Warna | Label |
|---|---|---|
| GPS akurat, <30 menit | 🟢 Hijau `#0F7A5A` | "GPS Akurat • baru" |
| Cache, <10 menit | 🟡 Kuning `#F59E0B` | "GPS Terakhir • Xs lalu" |
| Heartbeat Mode Aman | ⚫ Abu `#6B7280` | "Heartbeat • Xm lalu" |
| Stale >30 menit | 🔴 Merah `#DC2626` | "GPS • Xj lalu" |

### File Baru
- `SetupWizardScreen.kt` (~221 baris)
- `LocationCache.kt` (~57 baris)
- `KirimLokasiWorker.kt` (~43 baris)
- `OtaDetector.kt` (~93 baris)
- `AlertIzinHelper.kt` (~76 baris)

### Server-Side (TIDAK DI GIT)

**`app.js` (Node.js) — endpoint baru:**
- `POST /api/heartbeat-broadcast` (guard `requireApiKey`) — kirim FCM heartbeat
  ke topic global `imtiyaz-heartbeat`
- `POST /api/alert-izin` — terima alert dari HP jamaah, tulis Firestore
- `POST /api/kesehatan-me` — proxy ke WP `/imtiyaz/v1/kesehatan-me`
- Cache in-memory: `/api/me` 60s, `/api/kesehatan-me` 120s (per jamaah_id)

**`imtiyaz-connector.php` (WP plugin):**
- Token safety — cek hash sebelum hapus `_jamaah_token` (bulk update + save_meta)
- Logging `TOKEN-SAFE-BULK` / `TOKEN-SAFE-META` / `TOKEN-SAFE-LOGOUT`

**Backup di server:** `/tmp/app.js.bak-*`, `/tmp/imtiyaz-connector.php.bak-*`

### Secrets GitHub (5)
- `RELEASE_KEYSTORE_BASE64`, `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`
- `API_KEY_PASTIUMRAH` (untuk workflow heartbeat)

---

## Bug Fix Sesi 10 Okt 2026

### Bug #1 — Tracking "HP tidak merespons"

**Gejala:** TL tap "Cari" pada jamaah Mode Aman aktif → timeout 60s →
"HP tidak merespons, jamaah mungkin tidak membawa HP".

**Root cause:** `KirimLokasiHelper.kirimLokasi()` spawn `CoroutineScope`
sendiri (tidak suspend). `withWakeLock` di FcmService release instan
sebelum Firestore write selesai. Coroutine di-kill OS → `request_id` tidak
tertulis ke Firestore → listener TL tidak match → timeout.

**Bukti:** Firestore `lokasi_jamaah/1410` punya `source=mode_aman_service`
+ `updated_at` fresh, TAPI tidak ada field `request_id`.

**Fix (commit `b888bc1`):**
- `KirimLokasiHelper.kirimLokasi()` jadi **suspend fun**
- FcmService hold `PARTIAL_WAKE_LOCK` di dalam coroutine sampai await selesai
- `KirimLokasiWorker` await suspend
- `FindJamaah`: fallback ke lokasi terakhir Firestore kalau timeout
  (jangan langsung "HP tidak merespons")

### Bug #2 — Card "Cek Kesehatan" selalu empty

**Gejala:** Data kesehatan ada di WP Admin (`_kesehatan_data` postmeta
lengkap) tapi Card di app jamaah tampil "Belum ada data kesehatan".

**Root cause:** Retrofit path `wp-json/imtiyaz/v1/kesehatan-me` di-resolve
terhadap `BASE_URL=https://api.pastiumrah.com/` →
`https://api.pastiumrah.com/wp-json/imtiyaz/v1/kesehatan-me` = **404**.
Endpoint Node.js yang benar ada di `/api/kesehatan-me`.

**Fix (commit `19d8367`):**
- Android: ganti path Retrofit jadi `api/kesehatan-me`
- Server: tambah proxy `/api/kesehatan-me` di `app.js`

### Performa — Tab Saya lambat

**Gejala:** Buka tab Saya tunggu 1-2 detik.

**Root cause:** `/api/me` + `/api/kesehatan-me` tidak di-cache, setiap
buka tab panggil WP fresh (~0.4s + network).

**Fix (server-side, tidak di git):**
- Cache `/api/me` 60s per `jamaah_id`
- Cache `/api/kesehatan-me` 120s per `jamaah_id`
- Efek: warm request dari 0.4s → **0.014s** (30x lebih cepat)

### Token jamaah terhapus otomatis

**Gejala:** `_jamaah_token` di postmeta KOSONG padahal admin tidak klik
apa-apa.

**Root cause:** Form login di WP Admin punya input `jamaah_password` yang
di-autofill browser. Setiap admin edit jamaah (isi kesehatan, ubah paket)
→ hook `save_meta` trigger → hapus token (meski password tidak diubah).

**Fix (WP `imtiyaz-connector.php`):**
- Sebelum hapus token, cek apakah password benar-benar berubah:
  `if (empty($old_hash) || !wp_check_password($new_pass, $old_hash))`
- Kalau password sama → token TIDAK dihapus
- Kalau password beda → token dihapus (perilaku lama)

### Catatan Server

- Node.js TIDAK auto-respawn — restart manual via `pkill + nohup` (lihat
  bagian Prosedur Restart di bawah)
- Endpoint `/api/heartbeat-broadcast` perlu header `x-api-key` (guard)
- Firestore rules masih `allow read/write if true` (DEV ONLY — kandidat
  Firebase Custom Auth)

---

## Catatan Sesi untuk Chat Berikutnya

1. Server code (`app.js`, `imtiyaz-connector.php`) TIDAK di git — edit
   langsung via SSH, backup di `/tmp/`
2. Setiap edit `app.js` wajib restart: `pkill + nohup spawn`
3. Secrets GitHub: 5 (4 keystore + `API_KEY_PASTIUMRAH`)
4. Firestore rules masih terbuka (kandidat hardening)
5. Kandidat TAHAP 23:
   - Firebase Custom Auth (production-ready)
   - Quick action Find Jamaah (copy koord, share WA)
   - Rekam radio LiveKit (Egress)
   - Hapus `LocateService.kt` (ntfy legacy) → v2.14.0

---

## TAHAP 23 — Admin Login Fix + Jadwal UI + Trip Fasilitas (12 Okt 2026)

**Status:** ✅ Selesai

### Bug #1 — Admin app tidak bisa login (DNS mismatch)

**Gejala:** App admin error:
failed to connect to pastiumrah.com/88.223.91.143 (port 443)
from 172.16.11.249 (port 47550) after 60000ms

**Root cause:**
- IP `88.223.91.143` tidak ada di DNS record `pastiumrah.com`
- DNS publik: `212.1.212.67` / `191.101.104.69`
- Server DNS intermittent resolve IPv6-only

**Fix (Android + Server):**
- Android: `ApiConfig.BASE_URL` dari `pastiumrah.com` → `api.pastiumrah.com`
- Android: `AdminApi.kt` 28 endpoint dari `wp-json/imtiyaz/v1/admin-X` → `api/admin/admin-X`
- Server: `app.js` proxy generik `/api/admin/:endpoint` (27 endpoint POST + 1 GET)
- Server: override DNS lookup di HTTPS agent (paksa `pastiumrah.com` → `127.0.0.1`)

**Commit admin repo:**
- `370c859` fix(admin): alihkan endpoint admin ke Node.js proxy
- `2ff2e50` chore(gitignore): blokir console.log + stderr.log
- `dfe3567` style(icon): ganti ikon app — logo hitam + background putih

**File baru admin:** — (tidak ada, hanya modifikasi)
**Server-side (tidak di git):** `app.js` — proxy admin + DNS override

### Fitur #1 — Ikon Admin baru

- Foreground adaptive: 432×432 dari `icon-hitam.png`
- Raster legacy mdpi..xxxhdpi regenerate (background putih #FFFFFF)
- Round icon regenerate
- Commit `dfe3567`

### Fitur #2 — Jadwal Keberangkatan pindah ke tab Jadwal

**Perubahan:**
- Tab bottom nav: `Paket` → `Jadwal` (icon DateRange)
- `JadwalKeberangkatan.kt`: pecah jadi `Section` (inline, Column) + `Screen` (wrapper)
- `PaketListScreen`: 2 section — Jadwal (atas) + Paket Umrah (bawah) + Divider
- `BerandaScreen`: hapus Card Jadwal + param `onJadwalClick` (fix overlap dengan Card Itinerary)
- Bersihkan state `showJadwal` + BackHandler + render overlay

**Commit:** `c5f490c` feat(ui): pindah Jadwal Keberangkatan ke tab Jadwal + fix overlap

### Fitur #3 — Trip Fasilitas (Termasuk/Tidak Termasuk)

**Fitur:** Klik Card jadwal → buka `TripFasilitasScreen` dengan data lengkap:
- Header hijau: nama paket + tanggal + harga (asli + coret)
- Card Fasilitas Termasuk (✅ 13 item)
- Card Tidak Termasuk (❌ 4 item)
- CTA "Tanya / Daftar via WhatsApp"

**Server-side (tidak di git):**

**WP `imtiyaz-connector.php`:**
- Endpoint baru: `GET /imtiyaz/v1/trip-fasilitas/{id}`
- Fasilitas **TIDAK** di `post_content` (hanya 131 char) — di-render runtime oleh WP Travel Engine block
- Solusi: **scrape HTML** via `wp_remote_get()` + regex `wte-block-icon__check` / `wte-block-icon__times`
- Cache 1 jam via `set_transient()` (hindari scrape berulang)
- Dedup hasil

**Node.js `app.js`:**
- Proxy `GET /api/trip-fasilitas/:id` ke WP

**Android:**
- `TripFasilitasScreen.kt` (baru, ~200 baris)
- `JadwalKeberangkatan.kt`: `TripFasilitasResponse` data class + `getTripFasilitas()` API + `JadwalCard` clickable
- `MainActivity.kt`: state `selectedTripId` + navigasi + wire ke `PaketListScreen`

**Commit jamaah repo:**
- `c7d4c3f` feat(jadwal): klik card jadwal -> TripFasilitasScreen
- `b634207` fix(jadwal): import clickable (Unresolved androidx di CI)

**Bukti test (trip 827 - Paket Linuwih 17 Okt 2026):**
- Response JSON: 13 termasuk + 4 tidak termasuk
- Sample: "Visa umrah", "Asuransi perjalanan", "Hotel selama program"
- Tidak termasuk: "Biaya pembuatan paspor", "Vaksin meningitis dan polio"

### Catatan Server

- DNS intermittent `pastiumrah.com` resolve IPv6-only → di-fix dengan custom lookup di HTTPS agent
- **Jangan hapus blok `_imtiyazLookup`** di `app.js` (fix DNS intermittent)
- **Jangan hapus blok `api_trip_fasilitas`** di `imtiyaz-connector.php` (scrape + cache)
- Backup terbaru: `/tmp/app.js.bak-trip-fasilitas-*`, `/tmp/imtiyaz-connector.php.bak-scrape-*`

### Kandidat TAHAP 24

- Bump versi app jamaah ke 2.14.0 (banyak fitur + fix)
- Hapus `LocateService.kt` (ntfy legacy) → v2.14.0
- Firebase Custom Auth (rules Firestore masih terbuka)
- Quick action Find Jamaah (copy koord, share WA)
- Optimasi cold start `/api/paket` (2s → <500ms)
- SEO website (DR 0.1 → 1.0 dalam 3 bulan)

---

## TAHAP 24 — Firebase Custom Auth + D3 Auto-Assign Kamar (13 Okt 2026)

**Status:** ✅ Selesai, teruji end-to-end

### Fokus

1. **Firebase Custom Auth** — ganti Firestore rules `allow read/write if true` (DEV) → ketat berbasis custom claims
2. **D3 Auto-Assign Kamar** — fitur agensi di App Admin untuk kelola trip & pembagian kamar
3. **Fix Mode Aman GPS** — fallback + warm-up untuk HP dengan chip GPS lambat
4. **Optimasi server** — `/health` endpoint + pre-warm cache + auto-sync role TL

### Firebase Custom Auth (A1-A4)

| Batch | Isi | Bukti |
|---|---|---|
| A1 | Endpoint `/api/firebase-token` (jamaah) | ✅ `POST` + verify token WP |
| A2 | Android Firebase Auth di app jamaah | ✅ login/logout + `FirebaseAuthHelper.kt` |
| A3 | Firestore rules berbasis claims (`role`, `jamaahId`) | ✅ `isSelf()`, `isTourLeader()`, `isAdmin()` |
| A4 | Auto-sync role TL via `save_post_jamaah` hook | ✅ race condition di-fix |

**Firestore Rules final:**
- `jamaah_tokens`, `lokasi_jamaah`, `alert_izin` — self-write + TL read
- `trips/**` — admin-only (read all, write admin/super_admin/keuangan)

**Custom claims format:**
- Jamaah: `{ role: "jamaah"|"tour_leader", jamaahId: "1453" }`
- Admin: `{ role: "admin"|"super_admin"|"keuangan", adminId: "1", platform: "admin" }`

### Endpoint Baru di `app.js` (Node.js)

| Endpoint | Fungsi |
|---|---|
| `POST /api/firebase-token` | Custom token untuk jamaah |
| `POST /api/firebase-token-admin` | Custom token untuk admin (auto-create user) |
| `POST /api/sync-role` | Sync claims role TL (guard `x-api-key`) |
| `GET /health` | Keep-alive target |

### Optimasi Server

- **Pre-warm cache** saat startup: `paket, dokumen, jadwal, doa-audio, pembimbing`
- **`/health` endpoint** → GitHub Actions cron 5 menit
- **Keep-alive workflow** `.github/workflows/keep-alive.yml`
- **Script `restart-api.sh`** di-fix: match `lsnode.*api\.pastiumrah` (bukan hanya `node app.js`)

**Hasil:** cold `/api/paket` 2 s → TTFB server ~47 ms (target <500 ms tercapai)

### Fix Mode Aman GPS (v2.14.0)

**Masalah:** `ModeAmanService` pakai `lastLocation` (network provider, akurasi ~100m) atau `getBestLocation` timeout → loop mati.

**Fix:**
1. `LocationHelper.getBestLocation()` dengan `timeoutMs = 30s` (samakan FCM trigger)
2. Fallback `lastLocation` kalau GPS fresh null
3. Re-throw `CancellationException` (loop tidak mati)
4. **Warm-up GPS** 60s di awal `refreshLoop()` — panaskan chip GPS
5. Flag `accuracy_quality: "high"|"medium"|"low"` di Firestore

**Hasil:** GPS lock akurasi < 2m di HP chip bagus (TL 1454), fallback 100m di HP chip lemah (jamaah 1453) — tidak crash.

### App Admin — D3 Auto-Assign Kamar

**Repo:** `imtiyaz-admin-android`

**Fitur:**
- Firebase Custom Auth (setup Firebase SDK + login/logout)
- Trip CRUD (create/edit/delete) + Hotel CRUD
- **2 mode generate kamar:**
  - **Manual:** input pax L/P + jumlah kamar per tipe (quint/quad/triple/double/single)
  - **Auto:** input pax L/P saja → sistem hitung quad→triple→double→single (tanpa quint)
- **Auto-assign engine:** proporsional (gender fair, terbesar dulu)
- **Edit nama penghuni + nomor kamar** per kamar (sync ke manifest)
- **Export PDF** daftar kamar (A4 profesional, multi-page, header/footer)
- **Warna kamar:** biru (L), pink (P), abu (kosong)
- **Sortir kamar:** nomor / tipe / L / P

**Algoritma `hitungKamarOtomatis(pax)`:**
sisa = pax
while sisa > 0:
if sisa >= 4: quad, sisa -= 4
elif sisa == 3: triple, sisa -= 3
elif sisa == 2: double, sisa -= 2
else: single, sisa = 0

Contoh: 15 pax → 3 quad + 1 triple = 4 kamar

### File Terkait

**App Jamaah:**
- `FirebaseAuthHelper.kt` (baru)
- `ModeAmanService.kt` (fix GPS)
- `LocationHelper.kt` (reuse getBestLocation)
- `.github/workflows/keep-alive.yml` (baru)

**App Admin (`imtiyaz-admin-android`):**
- `FirebaseAuthHelper.kt`, `TripModels.kt`, `TripRepository.kt`, `RoomAssignEngine.kt`
- `TripListScreen.kt`, `AddTripScreen.kt`, `TripDetailScreen.kt`, `AddHotelScreen.kt`
- `HotelRoomScreen.kt`, `GenerateRoomsScreen.kt`, `GenerateKamarAutoScreen.kt`
- `RoomListPdfExporter.kt`

**Server (tidak di git):**
- `app.js` — endpoint baru + debug cleanup
- `imtiyaz-connector.php` — hook `sync_role_after_save`
- `restart-api.sh` — pattern fix

### Bug Fix Sesi 13 Okt 2026

| Bug | Root Cause | Fix |
|---|---|---|
| `/api/firebase-token-admin` 401 | Node.js cek `r.data.admin_id`, WP return `id` | Ganti cek ke `r.data.id` |
| `setCustomUserClaims` user not found | User Firebase belum dibuat | `createUser` langsung, abaikan kalau ada |
| `lsnode` + `node app.js` duplikat | 2 proses rebutan port | Kill semua, spawn 1 manual |
| Node.js tidak reload patch | LiteSpeed `lsnode` serve kode lama | Kill `lsnode` lama |
| Mode Aman `Job was cancelled` | `CancellationException` ditelan | Re-throw + fallback lastLocation |
| GPS selalu TIMEOUT | Chip GPS HP lambat | Warm-up 60s di refreshLoop |

### Pelajaran Kritis

- `setCustomUserClaims` **butuh user Firebase existing** — createUser dulu
- `CancellationException` di `suspendCancellableCoroutine` **wajib re-throw**
- Firebase Admin SDK v14: error code `user-not-found` **tanpa prefix `auth/`** — cek pakai `.includes()`
- **2 proses Node.js** (manual + `lsnode`) rebutan port — LiteSpeed proxy pakai `lsnode`, bukan yang manual
- `console.error` di LiteSpeed **tidak muncul** di `stderr.log` — perlu `console.log` untuk debug
- Fallback `lastLocation` penting untuk HP dengan GPS lemah

---

## TAHAP 25 — Radio Gelombang + Game Simulasi Penerbangan + Checklist Barang (14 Okt 2026)

**Status:** ✅ Selesai, teruji end-to-end
**Versi:** 2.15.0 (versionCode 16)

### Fokus

1. **Radio Gelombang** — TL buat kode, jamaah join via kode (Live + Voice Note)
2. **Game "Terbang ke Makkah" v3** — simulasi penerbangan umrah YIA → JED dengan TTS
3. **Checklist Barang Bawaan** — item baru di tab Saya
4. **GPS Auto-Focus** — peta otomatis zoom ke jamaah target
5. **Fix gate TL di Radio** — fetch `is_tour_leader` di awal RadioScreen
6. **Heartbeat workflow** — retry 3× + timeout 60s

---

### 1. Radio Gelombang (v2.15.0)

**Konsep:** Ganti sistem kanal otomatis (admin assign WP) → **kode gelombang**.
TL generate kode → share WA → jamaah input untuk join.

**Format kode (Hybrid):**
- TL input nama: `"Linuwih 17 Okt"` → prefix `LINU`
- Server generate suffix random: `LINU-7K3M` (9 char)
- **Berlaku 24 jam** (auto expire)

**Server (`app.js`) — endpoint baru:**
| Endpoint | Fungsi |
|---|---|
| `POST /api/radio/gelombang/create` | TL buat gelombang |
| `POST /api/radio/gelombang/join` | Jamaah join via kode |
| `POST /api/radio/gelombang/leave` | Jamaah keluar |
| `POST /api/radio/gelombang/revoke` | TL akhiri |
| `GET /api/radio/gelombang/info/:kode` | Preview |

**Modifikasi endpoint existing:**
- `radio/send`, `radio/poll`, `radio/token` — support field `kode_gelombang`
- Fallback ke `kanal_id` (backward compat) kalau tidak ada kode

**Firestore:**
gelombang/{kode}
├── kode, nama, tl_id, tl_nama
├── is_active, created_at, expires_at (24h)
└── peserta: { "1453": { nama, joined_at, is_tl } }


**Rules:** `allow read: if isSignedIn(); allow write: if false;` (write via admin SDK)

**Android:**
- `RadioGelombangApi.kt` (baru) — interface Retrofit + data class
- `RadioLiveKit.kt` — terima `kodeGelombang` (LiveKit room: `imtiyaz-gelombang-{kode}`)
- `Radio.kt` — UI create/join + panel kode + share WA + dialog

**Aturan:**
- 1 jamaah = **1 gelombang aktif** (harus leave dulu sebelum join lain)
- TL buat gelombang baru → gelombang lama **auto-revoke**
- Live mode & Voice Note mode **sama-sama pakai gelombang** (konsisten)

---

### 2. Game "Terbang ke Makkah" v3

**Rewrite total** — dari endless runner hindari meteor → **simulasi penerbangan umrah**.

**Story:** IMTIYAZ AIRLINE flight **IZ-6711** · YIA Yogyakarta → JED King Abdul Aziz · 9 jam 50 menit · 38.000 kaki.

**Fase:**
1. **Bandara (5 layar):** Pintu masuk · Check-in · Screening · Imigrasi · Boarding
2. **Terbang:** Rute 10 waypoint + gameplay (kumpulkan koin pahala)
3. **Mendarat:** Landing + pesan selamat datang di Tanah Suci

**Aturan screening lengkap:**
- Lepas: jaket, ikat pinggang, jam tangan, topi, sepatu
- Keluarkan: laptop, HP, tablet, power bank
- Air minum: max 100 ml/wadah, total 1 liter
- Dilarang: benda tajam, gunting, korek api, petasan
- Obat: sertakan resep dokter

**Rute (10 waypoint):**
| Km | Lokasi |
|---|---|
| 0 | YIA Yogyakarta |
| 400 | Selat Sunda |
| 1200 | Selat Malaka |
| 2400 | Laut Andaman |
| 3500 | Teluk Benggala |
| 4800 | India Selatan |
| 6200 | Laut Arab |
| 7300 | Oman (Maskat) |
| 7700 | Arab Saudi |
| 7950 | Jeddah (mendarat) |

**Fitur teknis:**
- **TTS (TextToSpeech)** — bahasa Indonesia, auto-speak per milestone
- **Meteor warning** tiap 15 detik real → "Mohon kenakan sabuk pengaman. Jangan gunakan kamar mandi."
- **Turbulensi** (bukan kecelakaan) — pesawat goyang + TTS
- **Koin ⭐ Pahala** (+50) + **Near-miss combo** (+10)
- **Durasi 3 menit real** (10800 game detik / 60)
- **Parallax 3 layer** (bintang twinkle, gunung 2 lapis, awan)
- **Pesawat ikon detail** (fuselage + sayap + ekor + jendela)
- **Badge:** Penerbang Muda (1000km) · Handal (3000) · Ulung (6000) · Sampai Makkah (8000)

**File:**
- `TerbangKeMakkah.kt` (~1100 baris) — game engine + UI
- `TerbangKeMakkahTts.kt` (~80 baris) — wrapper TTS robust (pre-check + fallback + debug log)

**Ganti:** `HafalanQuran.kt` + `SusunAyat.kt` (dihapus). Tab Quiz → **Game**.
**Card di `Quran.kt`** ("Latihan Hafalan Juz 30") dihapus.

---

### 3. Checklist Barang Bawaan

**File baru:** `ChecklistBarangScreen.kt` (~290 baris)

**Isi (4 section, 19 item):**
| Section | Item |
|---|---|
| 🎒 Perlengkapan | Al-Quran, HP+charger, dompet+KTP+ATM, stop kontak, hanger, sandal, tali raffia, kacamata (8) |
| 👕 Pakaian | Ikhram/mukena, 4-5 set pakaian, seragam, jaket, kaos dalam (5) |
| 💊 Obat | Obat rutin, obat warung, jamu (3) |
| 🍲 Makanan | Garam, sambel/abon, pop mie (3) |

**Info koper:**
- Koper bagasi (24"): maks 20 kg · ❌ power bank, cairan, benda tajam
- Koper kabin (18"): maks 7 kg · ✅ cairan maks 100 ml/pcs · ✅ power bank maks 10.000 mAh
- Tas dokumen (slempang): paspor, KTP, tiket, visa

**State:** lokal via `Prefs` (`barang_<id>`)
**Fitur:** Progress bar, reset, auto-save
**Entry:** Card di tab Saya, di bawah "Checklist Dokumen"

**Bug fix:** Render pakai `when` block + BackHandler (bukan `if`).

---

### 4. GPS Auto-Focus

**Masalah:** Setelah TL cari jamaah, peta tidak auto-zoom ke titik jamaah — perlu zoom out manual.

**Fix (`LokasiMapView.kt`):**
```kotlin
val target = highlightJamaahId?.let { hid ->
    allJamaah.firstOrNull { it.jamaah_id == hid && it.latitude != null }
}
if (target != null) {
    mapView.controller.animateTo(GeoPoint(target.latitude!!, target.longitude!!), 17.0, 900L)
} else if (points.isNotEmpty()) {
    // fallback avg center
}

Efek: TL cari jamaah → peta langsung zoom 17 ke titik target.
5. Fix Gate TL di Radio
Masalah: Login sebagai TL, tapi di tab Radio muncul form "Masukkan Kode Gelombang" (form jamaah), bukan "Buat Gelombang Baru" (form TL).

Root cause: isTourLeader hanya di-set oleh poll loop. Setelah Batch C, poll loop di-guard if (kodeGelombang.isBlank()) return — jadi sebelum join, isTourLeader tetap false.

Fix: Tambah LaunchedEffect(Unit) di awal RadioScreen — fetch is_tour_leader sekali via /api/radio/poll tanpa kode gelombang:
LaunchedEffect(Unit) {
    try {
        val resp = withContext(Dispatchers.IO) {
            RadioApiClient.service.poll(RadioPollRequest(jamaahId, jamaahToken, 0L))
        }
        isTourLeader = resp.is_tour_leader ?: false
        // ... set channelNama, myId, isFallbackChannel
    } catch (e: Exception) { /* skip */ }
}

6. Heartbeat Workflow Fix
Masalah: Cron 6 jam gagal dengan exit code 28 (curl timeout 30s).

Fix (.github/workflows/heartbeat.yml):

Timeout naik dari 30s → 60s

--connect-timeout 15 + --retry 2 --retry-delay 5

Retry 3× dengan delay 10s antar percobaan

Hasil: Heartbeat cron berikutnya sukses (9s).

Bug Fix Sesi Ini
Bug	Root Cause	Fix
Checklist Barang tidak bisa diklik	Render pakai when block, bukan if	Patch render branch + BackHandler
Radio Voice mode auto-join	Belum migrasi ke gelombang	Batch C: gate + poll guard
TL dilihat sebagai Jamaah	isTourLeader hanya di-set poll loop	Fetch awal via LaunchedEffect
Progress bar error	Int division	toFloat() + .coerceIn(0f, 1f)
TTS tidak keluar suara	Beberapa HP tidak support id-ID	Fallback ke Locale.getDefault()
Heartbeat workflow fail	Curl timeout 30s transient	Retry 3× + timeout 60s
Banner meteor tutup pesawat	Posisi center	Pindah ke top=130dp
Game durasi 5 menit terlalu lama	REAL_TO_GAME_TIME 120f	Jadi 60f (3 menit real)
File Baru
File	Fungsi
RadioGelombangApi.kt	API client gelombang
ChecklistBarangScreen.kt	UI checklist barang
TerbangKeMakkah.kt	Game simulasi penerbangan
TerbangKeMakkahTts.kt	Wrapper TTS
File Dihapus
File	Alasan
HafalanQuran.kt	Diganti game
SusunAyat.kt	Diganti game
File Dimodifikasi
File	Perubahan
MainActivity.kt	Render branch checklist barang
Radio.kt	Gelombang UI + Voice Note gate + fetch TL awal
RadioLiveKit.kt	Terima kodeGelombang
LokasiMapView.kt	Auto-focus target
Quran.kt	Hapus Card Latihan Hafalan
app/build.gradle	Bump version 2.15.0
.github/workflows/heartbeat.yml	Retry 3× + timeout 60s
Server-Side (Tidak di Git)
app.js (Node.js):

5 endpoint gelombang baru

Modifikasi radio/send, radio/poll, radio/token

Debug log cleanup di /api/firebase-token-admin

Firestore:

Collection baru gelombang/{kode}

Rules: read for signed-in, write via admin SDK

Commit Log
6f81b50 feat(game): Terbang ke Makkah — endless runner (v1)

9a99b16 feat(game): v2 — parallax, pesawat ikon, partikel, koin, near-miss, pause

c3de5f5 feat(game-v3): simulasi penerbangan umrah

b9a7824 fix: Checklist Barang render branch

de8426f feat(radio-v2.15.0): gelombang — TL create kode

e0a1b84 feat(radio-v2.15.0): Voice Note mode ikut gelombang

(latest) fix(radio): fetch is_tour_leader di awal RadioScreen

Kandidat TAHAP 26
LiveKit self-host untuk biaya radio

D2 Manifest Real-time (Admin app)

D1 Planning Trip (Gantt)

D4 Visa Tracking

Game leaderboard antar jamaah (Firestore)

Bump ke 2.16.0 kalau ada fitur besar berikutnya

---

## Hotfix Infrastruktur — Migrasi Keep-Alive & Heartbeat ke cron-job.org (16 Okt 2026)

**Status:** ✅ Selesai
**Kategori:** Infrastruktur / WAF Mitigation

---

### Ringkasan

Hostinger mengaktifkan **WAF/CDN JS Challenge** di depan `api.pastiumrah.com` sekitar 16 Okt 2026. WAF ini memblokir semua **IP datacenter** (GitHub Actions runners, AWS, GCP, Azure, dll), sehingga workflow `keep-alive.yml` dan `heartbeat.yml` gagal dengan **HTTP 403**.

**Solusi:** Migrasi kedua cron ke **cron-job.org** — IP mereka sudah di-whitelist Hostinger (dibuktikan oleh `pastiumrah.com/wp-cron.php` yang sudah lama aktif di cron-job.org tanpa masalah).

---

### 1. Masalah Detail

#### Gejala

Workflow GitHub Actions `keep-alive.yml` gagal setiap 5 menit dengan response:
HTTP 403

<!DOCTYPE html><title>Checking your browser before accessing. Just a moment...</title> <script src="/hcdn-cgi/jschallenge"></script> ```
Response body adalah halaman JS Challenge (mirip Cloudflare) — bukan response dari Node.js API.

Bukti Diagnostik
Sumber Request	HTTP Code	Kesimpulan
Lokal (IP rumah)	200	✅ Residential IP lolos WAF
Dengan User-Agent browser	200	✅ UA bukan faktor
Direct IP (--resolve)	200	✅ Server OK
GitHub Actions	403	❌ IP datacenter diblok
SSH server Hostinger	200	✅ IP server lolos
Kesimpulan: WAF Hostinger memblokir IP range datacenter (kebijakan anti-bot). Tidak bergantung User-Agent.

Dampak
Fitur	Efek
Keep-alive (5 menit)	❌ Fail → server cold start berulang
Heartbeat broadcast (6 jam)	❌ Fail → HP jamaah tidak bangun dari App Standby
Pre-warm cache	Hilang saat cold start → /api/paket lambat ~2 detik untuk request pertama
2. Solusi — cron-job.org
Alasan pilih cron-job.org
Alternatif	Status	Catatan
cron-job.org	✅ Dipilih	IP sudah whitelisted (WP-Cron existing lolos)
UptimeRobot	Backup	IP juga whitelisted, tapi fokus monitoring (bukan cron)
hPanel Cron Jobs	Backup	Internal server, bypass WAF, tapi butuh akses hPanel
GitHub Actions	❌ Dimatikan	Kena WAF
Konfigurasi Cron Baru
Cron 1 — Health Check (Keep-Alive):

Field	Nilai
Judul	Imtiyaz API Health Check
URL	https://api.pastiumrah.com/health
Method	GET
Header	(tidak ada)
Body	(tidak ada)
Jadwal	*/5 * * * * (setiap 5 menit)
Zona waktu	Asia/Jakarta
Cron 2 — Heartbeat Broadcast:

Field	Nilai
Judul	Imtiyaz Heartbeat Broadcast
URL	https://api.pastiumrah.com/api/heartbeat-broadcast
Method	POST
Header 1	x-api-key: <API_KEY_PASTIUMRAH>
Header 2	Content-Type: application/json
Body	{}
Jadwal	0 */6 * * * (jam 00:00, 06:00, 12:00, 18:00 UTC = 07, 13, 19, 01 WIB)
Zona waktu	Asia/Jakarta
Verifikasi Berhasil
Cron	Terakhir	Durasi	Status
Imtiyaz API Health Check	9:20 PM	795 ms	✅ Berhasil
Imtiyaz Heartbeat Broadcast	12:00 AM	—	✅ Berhasil (verified)
Pastiumrah WP-Cron	9:20 PM	213 ms	✅ Berhasil (existing)
3. Perubahan Repo
Kedua workflow GitHub Actions di-rename ke .disabled (tidak dihapus, sebagai referensi):
.github/workflows/heartbeat.yml       → .github/workflows/heartbeat.yml.disabled
.github/workflows/keep-alive.yml      → .github/workflows/keep-alive.yml.disabled
Commit:

032a9e2 — ci: disable heartbeat GH Actions — diganti cron-job.org (bypass WAF Hostinger)

c6c607e — ci: disable keep-alive GH Actions — diganti cron-job.org

Alasan rename (bukan delete):

Kalau WAF Hostinger di-disable di masa depan, workflow bisa di-enable kembali dengan mv *.disabled → .yml

Menyimpan konfigurasi sebagai referensi (command curl, retry, dll)

4. Catatan Infrastruktur Penting
Aturan WAF Hostinger
Semua endpoint publik api.pastiumrah.com memblokir IP datacenter.

Yang lolos:

IP residential (HP user, komputer pribadi)

IP server Hostinger sendiri (localhost, SSH commands)

IP cron-job.org (whitelisted partner)

IP UptimeRobot (whitelisted partner)

Yang diblokir:

IP AWS/GCP/Azure (termasuk GitHub Actions runners)

IP VPS pribadi (kemungkinan besar)

IP datacenter umum lainnya

Implikasi untuk Fitur Mendatang
Fitur	Dampak
FCM/Heartbeat	Harus via cron-job.org (bukan GH Actions)
Cron backup database	Harus via hPanel Cron Jobs atau server-side
Webhook dari GitHub	❌ Tidak bisa — GH Actions IP diblok
Deploy automation dari CI	Harus pakai SSH (bukan HTTP endpoint)
Monitoring uptime	Pakai UptimeRobot (bukan GH Actions)
API request dari pihak ketiga	Cek dulu apakah IP mereka diblok
Kalau Butuh Migrasi Lagi
Kalau cron-job.org down atau butuh alternatif:

UptimeRobot — https://uptimerobot.com

Daftar gratis, 50 monitor 5 menit

Sudah whitelisted Hostinger

Fokus monitoring, tapi bisa ping GET

hPanel Cron Jobs — Advanced → Cron Jobs

Jalan dari server Hostinger sendiri

Bypass WAF 100%

Tapi butuh SSH command syntax

Server-side Scheduler

Setup node-cron atau setInterval di app.js

Jalan selama server hidup

Tidak butuh external cron

Yang TIDAK Boleh Dilakukan
❌ Jangan re-enable workflow GH Actions yang ping ke api.pastiumrah.com — akan kena 403

❌ Jangan gunakan GH Actions untuk webhook endpoint (/api/*) — akan kena 403

❌ Jangan hapus file .disabled — simpan sebagai referensi

❌ Jangan lupa cek cron-job.org kalau heartbeat/health check terasa mati

5. File Terkait
File	Status	Fungsi
.github/workflows/heartbeat.yml.disabled	⏸️ Disabled	Referensi heartbeat (dimigrasi)
.github/workflows/keep-alive.yml.disabled	⏸️ Disabled	Referensi keep-alive (dimigrasi)
cron-job.org — Imtiyaz API Health Check	✅ Aktif	Ping /health tiap 5 menit
cron-job.org — Imtiyaz Heartbeat Broadcast	✅ Aktif	POST /api/heartbeat-broadcast tiap 6 jam
cron-job.org — Pastiumrah WP-Cron	✅ Aktif	Existing (tidak berubah)
6. Verifikasi Cepat (Kalau Meragukan)
Cek server masih warm:

bash
ssh -p 65002 u120369480@153.92.10.222
cd ~/domains/api.pastiumrah.com/hbuilds/current/nodejs
tail -30 console.log | grep -E "jalan di port|Pre-warm"
Expected: Kalau cron bekerja, tidak ada log Pre-warm baru setiap 5 menit. Server tetap warm.

Cek cron-job.org history:

Buka https://console.cron-job.org/jobs

Klik RIWAYAT di Imtiyaz API Health Check

Cek status: harusnya semua Berhasil dengan HTTP 200

Cek dari server langsung:

bash
curl -s -o /dev/null -w "HTTP %{http_code} (%{time_total}s)\n" \
  https://api.pastiumrah.com/health
Expected: HTTP 200 (dari IP server, bypass WAF).

7. Pelajaran
Jangan asumsikan cron GH Actions selalu bekerja — layanan pihak ketiga (WAF, CDN) bisa berubah tanpa notice.

Selalu test dari multiple IP saat debug HTTP issue — memisahkan masalah "endpoint down" vs "IP diblok".

Renaming .disabled (bukan delete) adalah pattern baik untuk preserve konfigurasi yang tidak aktif.

cron-job.org adalah fallback andal untuk cron di lingkungan dengan WAF ketat.

Hostinger WAF aktif 16 Okt 2026 — catat tanggal ini kalau ada issue aneh di masa depan.

----
---

## Hotfix — Migrasi Keep-Alive & Heartbeat ke cron-job.org (16 Okt 2026)

**Status:** ✅ Selesai

### Masalah
Hostinger mengaktifkan **WAF/CDN JS Challenge** di depan `api.pastiumrah.com`. WAF ini memblokir IP datacenter (termasuk GitHub Actions), sehingga `keep-alive.yml` dan `heartbeat.yml` gagal (HTTP 403).

### Solusi
1. Workflow GH Actions di-disable (rename ke `.disabled`).
2. Migrasi cron ke **cron-job.org** (IP whitelisted Hostinger):
   - **Imtiyaz API Health Check**: GET `/health` (5 menit).
   - **Imtiyaz Heartbeat Broadcast**: POST `/api/heartbeat-broadcast` (6 jam, butuh header `x-api-key`).
**Commit:** `032a9e2`, `c6c607e`

---

## TAHAP 26 — CS Virtual AI Berbasis Suara (📋 RENCANA)

**Status:** 📋 Belum dieksekusi (siap masuk Batch 1)

### Konsep
Jamaah bertanya seputar umrah (paket, sisa bayar) lewat suara, dijawab AI secara *real-time*.

### Arsitektur
- **STT:** `SpeechRecognizer` Android.
- **LLM:** **Gemini 4 Argon** (halusinasi terendah, termurah).
- **RAG:** Fetch dinamis dari WP REST (`/api/paket`, `/api/me`, dll).
- **TTS:** Reuse `TerbangKeMakkahTts.kt`.
- **Server:** Endpoint `/api/cs/tanya`.

### Guardrails
- **Boleh:** Harga, fasilitas, sisa tagihan, jadwal.
- **DILARANG:** Fiqih/hukum, janji di luar paket. (Dialihkan ke WA Admin).

### Rencana Batch (Total ~20 jam)
1. **Server:** `/api/cs/tanya` + Gemini API + RAG.
2. **Android UI:** `CsVirtualScreen.kt` (Chat Teks).
3. **Android Voice:** Integrasi STT.
4. **Android Audio:** Integrasi TTS + Animasi.
5. **Safety:** Guardrails + Fallback WA.
6. **Testing:** Device.

____
Terakhir update: 26 Okt 2026 (Migrasi LiveKit VPS & Perbaikan Radio)
Repo utama: https://github.com/kahfihidayat87/imtiyaz-android-v1.5.meta
Local path: C:\Users\DELL\Documents\GitHub\imtiyaz-android-v1.5.meta

📊 Status Saat Ini
Item	Nilai
Versi aktif (App Jamaah)	2.13.0 (versionCode 15) — Perlu di-bump ke 2.15.0 sebelum rilis Play Store
Branch	main
CI	GitHub Actions hijau
Server Node.js	Berjalan via PM2 di Shared Hosting (BUKAN lsnode lagi)
Server LiveKit	VPS Hostinger (187.124.137.227)
Domain LiveKit	livekit.pastiumrah.com (SSL via Caddy)
🚀 Pencapaian Besar Sesi Ini (26 Okt 2026)
1. Migrasi LiveKit Cloud → VPS Hostinger
Kita telah sukses memindahkan server siaran radio dari LiveKit Cloud ke VPS Hostinger milik sendiri.

VPS IP: 187.124.137.227

Konfigurasi VPS: Docker Compose (livekit.yaml & Caddyfile di /opt/livekit).

SSL: Otomatis via Caddy (Let's Encrypt).

Port Firewall VPS: 80, 443, 7880, 7881, 7882 (UDP/TCP).

Catatan Penting: Blok turn: di livekit.yaml dihapus agar Caddy bisa menangani SSL dengan lancar.

2. Perbaikan Node.js API (Shared Hosting)
Server Node.js (api.pastiumrah.com) sempat crash karena Hostinger melakukan deploy ulang dan menghapus modul serta file kustom.

PM2: Diinstall secara global (di ~/.npm-global) dan dijalankan sebagai process manager (imtiyaz-api).

Auto-Start: Dibuat script pm2-watchdog.sh yang berjalan di background untuk memastikan API selalu hidup (menggantikan crontab yang diblokir Hostinger).

File Kustom yang Hilang: skrining-routes.js dibuat ulang (mode darurat), service-account.json (kunci Firebase) dipulihkan.

Modul Hilang: firebase-admin, pdfkit, dan livekit-server-sdk di-install ulang secara manual via NPM.

3. Perubahan Logika API (Node.js)
Kode Gelombang Manual: Server TIDAK LAGI menggunakan generateKodeGelombang(). Kode 100% diambil dari input manual kode_manual yang dikirim TL. Jika kosong, server menolak (HTTP 400).

Guest Mode: Endpoint /api/radio/gelombang/join dan /api/radio/token sekarang menerima ID berawalan guest_ tanpa memerlukan token WordPress.

Firebase: service-account.json berhasil dimuat kembali ([FCM] Firebase Admin SDK initialized).

4. Perbaikan Android (App Jamaah)
Branding: "Radio Tour Leader" diganti menjadi "Radio Imtiyaz". Teks "Tour Leader" diganti menjadi "Pembimbing".

UI: Bottom Navigation disembunyikan saat berada di layar Radio. Tombol mic diperbaiki agar bisa di-tap.

Guest Mode: Prefs.getOrCreateGuestId() ditambahkan. Jika user tidak login, ID guest_xxxx dikirim ke server.

Background Service: Dibuat RadioService.kt (Foreground Service + WakeLock). Radio tetap tersambung meski layar HP mati/terkunci.

Battery Optimization: Izin REQUEST_IGNORE_BATTERY_OPTIMIZATIONS ditambahkan di MainActivity saat aplikasi pertama kali dibuka.

Error Handling: Menambahkan parsing JSON agar jika server error, pesan yang muncul di HP adalah pesan aslinya (bukan sekadar "HTTP 400").

5. Firestore Rules
Collection gelombang sekarang allow read: if true; agar Guest (tanpa login) bisa mendengarkan radio.

🔧 Cara Mengelola Server (PENTING!)
Sejak sesi ini, server Node.js TIDAK LAGI menggunakan LiteSpeed (lsnode). Kita beralih ke PM2.

Server API (api.pastiumrah.com)
Login: ssh -p 65002 u120369480@153.92.10.222

Perintah	Fungsi
pm2 status	Cek status server
pm2 restart imtiyaz-api	Restart server (setelah edit kode)
pm2 logs imtiyaz-api	Lihat log realtime
pm2 stop imtiyaz-api	Matikan server
Server LiveKit VPS (livekit.pastiumrah.com)
Login: ssh root@187.124.137.227
Folder: /opt/livekit

Perintah	Fungsi
docker ps	Cek status container (LiveKit & Caddy)
docker compose restart	Restart LiveKit & Caddy
docker compose logs -f livekit	Lihat log LiveKit
docker compose logs -f caddy	Lihat log SSL/Caddy
📝 Kandidat TAHAP Selanjutnya
Bump Versi Play Store: Ubah versionCode 15 → 16, versionName 2.13.0 → 2.15.0. Build AAB untuk Play Store.

Kustomisasi UI Radio: Perbaiki tampilan error HTTP 400 di UI (jika ada) agar lebih rapi.

Dokumentasi Admin: Update HANDOFF untuk App Admin (memastikan fitur KAS & PROSPEK terdokumentasi).

Pembersihan Sisa Kode: Hapus total LocateService.kt (karena sudah tidak dipakai) dan hapus file .bak- di lokal.

⚠️ Catatan Kritis untuk Percakapan Berikutnya
Jangan menyentuh service-account.json atau .env di server tanpa backup.

Jangan pernah menghapus atau menimpa file app.js dengan versi asli Hostinger (karena Hostinger tidak menyertakan patch LiveKit kita).

Jika Hostinger melakukan deploy ulang (folder versions berubah), wajib menjalankan npm install firebase-admin pdfkit livekit-server-sdk dan memastikan file service-account.json ada.

Selalu gunakan pm2 restart imtiyaz-api setelah mengedit app.js.

