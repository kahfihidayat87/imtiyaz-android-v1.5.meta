# Imtiyaz Tour — Project Handoff Document

**Terakhir update:** 27 Sep 2026 (v2.13.0-dev)
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
| Total file Kotlin | **26 file** (22 lama + 4 FCM) |
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

## File Kotlin di App Jamaah (26 file, per 27 Sep 2026)

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

### TAHAP 14 - Uji End-to-End FCM (SEDANG DIUJI)
Kandidat lanjutan:
- A. Firebase Custom Auth (production-ready)
- B. Bump versi ke 2.13.0 + changelog
- C. Supervisor auto-respawn Node.js
- D. Deprecate LocateService.kt (ntfy legacy)
- E. Quick action layar Find Jamaah
- F. RBAC & keamanan App Admin

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
