# Imtiyaz Tour — Project Handoff Document

**Terakhir update:** 27 Sep 2026 (v2.13.0-dev, TAHAP 14 SELESAI)
**Repo utama:** https://github.com/kahfihidayat87/imtiyaz-android-v1.5.meta
**Local path:** C:\Users\DELL\Documents\GitHub\imtiyaz-android-v1.5.meta

---

## Status Saat Ini

| Item | Nilai |
|---|---|
| Versi aktif (App Jamaah) | **2.12.0** (versionCode 14) — perlu bump ke 2.13.0 setelah uji FCM |
| Commit terakhir | Lihat `git log --oneline -5` |
| Branch | `main` |
| CI | GitHub Actions hijau |
| Total file Kotlin | **25 file** (21 lama + 4 FCM/notifikasi) |
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
