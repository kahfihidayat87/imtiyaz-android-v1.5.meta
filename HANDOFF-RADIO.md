# Imtiyaz Tour — Handoff Fitur Radio LiveKit

**Terakhir update:** 28 Sep 2026
**Status:** LiveKit SDK terintegrasi, UI tab switcher selesai, siap uji device
**Fitur:** Siaran radio TL real-time (latency <500 ms) via WebRTC

---

## Ringkasan

Fitur radio TL punya **2 mode** yang bisa dipilih via tab di `RadioScreen`:

| Mode | Teknologi | Latency | Kuota |
|---|---|---|---|
| Voice Note | HTTP multipart + polling 2,5 detik | ~2,5 detik | Gratis (server sendiri) |
| Siaran Live | LiveKit WebRTC SFU | <500 ms | 5.000 participant min/bulan (free) |

Kedua mode **tidak saling mengganggu** — jamaah pilih tab yang diinginkan.

---

## Arsitektur LiveKit
## Kredensial & Konfigurasi

### LiveKit Cloud

| Item | Nilai |
|---|---|
| Project | imtiyaz-tour-radio |
| Region | Singapore (Asia Pacific) |
| URL | wss://imtiyaz-tour-radio-XXXX.livekit.cloud |
| API Key | (di .env server) |
| API Secret | (di .env server) |
| Dashboard | https://cloud.livekit.io |
| Tier | Build (Free) — 5.000 participant min/bulan |

### .env Server

Path: ~/domains/api.pastiumrah.com/hbuilds/current/nodejs/.env
## Server-Side Code

### Endpoint POST /api/radio/token

Fungsi: Generate JWT LiveKit setelah verifikasi jamaah via WordPress.

Request: { "jamaah_id": "123", "token": "xxxxxxxxxxxx" }

Response sukses: { "token": "eyJ...", "url": "wss://...", "is_tl": true }

Response error:
- 400 — Data tidak lengkap
- 401 — Token tidak valid
- 500 — Gagal generate token (error internal)

Penting:
- resolveRadioSession throw error "Request failed with status code 401" kalau token invalid
- Catch block harus cek e.response?.status untuk map HTTP status (401 vs 500)
- WAJIB await at.toJwt() — method return Promise (di SDK v2+)

---

## Android Code

### File RadioLiveKit.kt (baru)

Object singleton dengan method:
- mulai(context, jamaahId, token, onStatus) — connect, TL auto-publish mic
- berhenti() — disconnect + stop mic
- sedangAktif(): Boolean

Imports penting:
- io.livekit.android.LiveKit
- io.livekit.android.RoomOptions (BUKAN .room.RoomOptions!)
- io.livekit.android.room.Room
- io.livekit.android.events.RoomEvent
- io.livekit.android.events.collect

### File Radio.kt (dimodifikasi)

Perubahan:
1. State baru: mode ("voice" | "live"), liveStatus, liveActive
2. TabRow — 2 tab: Voice Note dan Siaran Live
3. if (mode == "voice") UI lama else LiveKitSection
4. DisposableEffect(mode) — auto-connect saat Live, auto-disconnect saat keluar
5. Composable baru LiveKitSection

### Izin

- RECORD_AUDIO sudah ada di AndroidManifest — tidak perlu tambah

---## Kuota & Biaya

### Free Tier (Build)

| Resource | Kuota / Bulan |
|---|---|
| Participant Minutes WebRTC | 5.000 menit |
| Bandwidth downstream | 50 GB |
| AI Agent Sessions | 1.000 menit |
| Koneksi bersamaan | 100 peserta |

### Formula

participant_minutes = total_participants x durasi_menit

### Contoh Perhitungan

1 TL + 45 jamaah (46 partisipan), 2,5 jam:
46 x 150 = 6.900 participant minutes

Kuota gratis 5.000 min -> siaran terputus di menit ~109 (1 jam 48 menit).

### Rekomendasi

| Skenario | Bisa? |
|---|---|
| Live 2,5 jam, 45 jamaah | Over kuota |
| Live 1 jam, 45 jamaah | OK (2.760 min) |
| Live 5 menit, 45 jamaah | OK (230 min) -> 21 sesi/bulan |

Strategi: Live mode untuk pengumuman penting/darurat (singkat). Komunikasi rutin pakai Voice Note.

### Upgrade Opsi

| Opsi | Biaya | Cocok |
|---|---|---|
| Self-host LiveKit di VPS | ~$6-12/bulan (unlimited) | Volume tinggi |
| LiveKit Ship tier | ~$0,005/min | Volume sedang |

---

## Cara Test di Device

### Persiapan
1. 2 HP: 1 TL, 1 jamaah
2. TL sudah di-set di WordPress (Data Jamaah -> centang Tour Leader)
3. Install APK terbaru

### Test Flow

Step 1 — Mode Voice Note (lama):
- HP TL: Radio -> tab Voice Note -> tekan-tahan mic -> lepas
- HP jamaah: dengar dalam ~2,5 detik

Step 2 — Mode Siaran Live:
- HP TL: tap tab Siaran Live -> status "Siaran aktif", minta izin mic
- HP jamaah: tap tab Siaran Live -> status "Mendengarkan TL..."
- HP TL bicara -> jamaah dengar real-time, latency <500 ms

Step 3 — Auto-disconnect:
- HP TL: tap tab Voice Note -> LiveKit berhenti otomatis

Step 4 — Multi-jamaah:
- Tambah 2-3 jamaah di tab Live -> semua dengar suara TL

---

## Troubleshooting

### Server-side

| Error | Cek | Fix |
|---|---|---|
| 401 dari /api/radio/token | Token jamaah valid? | Login ulang |
| 500 dari /api/radio/token | Log: tail console.log | Cek credentials .env |
| LIVEKIT_URL: 0 char | .env belum diisi | Edit .env via nano |
| EADDRINUSE | Proses ganda | Kill -9 semua, spawn ulang |

### Android-side

| Error | Cek | Fix |
|---|---|---|
| Unresolved reference: RoomOptions | Import salah | Pakai io.livekit.android.RoomOptions |
| CI gagal "Could not find audioswitch" | settings.gradle belum JitPack | Tambah maven jitpack |
| Status "Gagal dapat token" | Endpoint server error | Cek via curl |
| Audio TL tidak dengar | Mic permission | Izinkan RECORD_AUDIO |

### Monitor Log Server

ssh -p 65002 u120369480@153.92.10.222 "tail -f ~/domains/api.pastiumrah.com/hbuilds/current/nodejs/console.log"

Cari: [LiveKit] Auth error (401) atau [LiveKit] Token error (500).

---## Restart Server

Gunakan script battle-tested:

ssh -p 65002 u120369480@153.92.10.222 "cd ~/domains/api.pastiumrah.com/hbuilds/current/nodejs && bash scripts/restart-node.sh"

---

## File Terkait

| File | Peran |
|---|---|
| RadioLiveKit.kt | Wrapper LiveKit (baru) |
| Radio.kt | UI radio + tab switcher |
| app/build.gradle | Dependency LiveKit |
| settings.gradle | Repo JitPack |
| app.js (server) | Endpoint /api/radio/token |
| .env (server) | Credentials LiveKit |

---

## Next Steps

- [ ] Uji device end-to-end (TL publish + jamaah subscribe)
- [ ] Ukur latency — apakah benar <500 ms?
- [ ] Handle edge case: TL keluar app, jamaah join saat TL siaran, multiple TL
- [ ] UI indicator: kapan TL siaran, kapan diam
- [ ] Audio ducking: turunkan volume musik saat TL bicara
- [ ] Self-host LiveKit kalau volume tinggi
- [ ] Rekam siaran untuk replay (LiveKit Egress — berbayar)
- [ ] Notifikasi jamaah saat TL mulai siaran (via FCM)

---

## Referensi

- LiveKit Cloud Dashboard: https://cloud.livekit.io
- LiveKit Android SDK: https://github.com/livekit/client-sdk-android
- LiveKit Server SDK (Node): https://github.com/livekit/server-sdk-js
- LiveKit Docs: https://docs.livekit.io
- LiveKit Pricing: https://livekit.io/pricing
