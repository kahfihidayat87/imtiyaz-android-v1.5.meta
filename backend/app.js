require('dotenv').config();
const express = require('express');
const cors = require('cors');
const multer = require('multer');
const axios = require('axios');
const FormData = require('form-data');
const fs = require('fs');
const path = require('path');

const app = express();
const PORT = process.env.PORT || 3000;
const WP_URL = process.env.WP_URL || 'https://pastiumrah.com';
const WP_USER = process.env.WP_USER;
const WP_APP_PASSWORD = process.env.WP_APP_PASSWORD;
const API_KEY = process.env.API_KEY; // lapisan proteksi tambahan untuk endpoint yang menulis data

// FIX: sebelumnya WP_USER/WP_APP_PASSWORD punya fallback string placeholder
// ('admin' / 'xxxx xxxx xxxx xxxx'). Kalau .env lupa di-set, server dulu tetap
// jalan diam-diam memakai kredensial palsu -> semua request ke WordPress gagal
// tanpa pesan error yang jelas. Sekarang server menolak start jika belum di-set.
if (!WP_USER || !WP_APP_PASSWORD) {
  console.error('FATAL: WP_USER dan WP_APP_PASSWORD wajib diisi di .env (lihat .env.example)');
  process.exit(1);
}

const WP_AUTH = { username: WP_USER, password: WP_APP_PASSWORD };

// Pastikan folder upload sementara ada sebelum multer dipakai.
// FIX: sebelumnya tidak dijamin ada -> multer bisa gagal dengan ENOENT di server baru/fresh clone.
const UPLOAD_DIR = path.join(__dirname, 'uploads');
fs.mkdirSync(UPLOAD_DIR, { recursive: true });

app.use(cors());
app.use(express.json());

// FIX: sebelumnya menerima file APA SAJA sampai 5MB (tanpa fileFilter) untuk
// bukti transfer. Sekarang dibatasi ke gambar saja untuk mengurangi risiko upload arbitrer.
const upload = multer({
  dest: UPLOAD_DIR,
  limits: { fileSize: 5 * 1024 * 1024 },
  fileFilter: (req, file, cb) => {
    if (/^image\/(jpeg|jpg|png|webp)$/.test(file.mimetype)) cb(null, true);
    else cb(new Error('Hanya file gambar (jpg/png/webp) yang diperbolehkan'));
  }
});

// FIX: lapisan proteksi tambahan di level Node untuk endpoint yang MENULIS data.
// Ini di luar proteksi utama (Basic Auth ke WordPress di bawah), untuk memperlambat
// bot/spam yang langsung menghantam API publik ini. Set API_KEY di .env, dan kirim
// header 'x-api-key' dari sisi Android app.
function requireApiKey(req, res, next) {
  if (!API_KEY) return next(); // opsional: kalau tidak diset, lewati (mundur ke perilaku lama)
  if (req.header('x-api-key') !== API_KEY) {
    return res.status(401).json({ error: 'API key tidak valid' });
  }
  next();
}

const PAKET_LIST = [
  { id:'slamet', nama:'Paket Slamet', kategori:'Ekonomis', durasi:'9 Hari', harga:'Rp 28,9 jt', fasilitas:'Bus Jogja-Jakarta, Hotel *3 250m/750m, 45 Pax', badge:'' },
  { id:'ayem', nama:'Paket Ayem Tentrem', kategori:'Hemat', durasi:'9 Hari', harga:'Rp 29,9 jt', fasilitas:'Pesawat YIA-CGK, Hotel *3, 35-45 Pax', badge:'' },
  { id:'linuwih', nama:'Paket Linuwih', kategori:'Reguler', durasi:'9 Hari', harga:'Rp 37,4 jt', fasilitas:'Garuda/Saudia Langsung, Hotel B4 250m, Max 25', badge:'Paling Diminati' },
  { id:'kamulyan', nama:'Paket Kamulyan', kategori:'Nyaman Lansia', durasi:'9 Hari', harga:'Rp 41,9 jt', fasilitas:'Garuda/Saudia, Hotel B5 250m, Max 20, Kursi Roda', badge:'Premium' },
  { id:'plus', nama:'Paket Plus', kategori:'Plus Wisata', durasi:'13 Hari', harga:'Rp 40,9 jt', fasilitas:'Plus Mesir/Turki, Hotel *4/*5', badge:'' }
];

app.get('/', (req,res)=>{
  res.json({
    status:'OK',
    service:'Imtiyaz Node API v1.5.1',
    endpoints:['/api/paket','/api/jamaah/:id','/api/jamaah','/api/upload-bukti','/api/update-checklist','/api/skrining','/api/wa-admin']
  });
});

let paketCache=null, cacheTime=0;
app.get('/api/paket', async (req,res)=>{
  try{
    if(paketCache && Date.now()-cacheTime < 300000) return res.json(paketCache);
    const r = await axios.get(`${WP_URL}/wp-json/imtiyaz/v1/paket`, {timeout:4000});
    paketCache=r.data; cacheTime=Date.now(); res.json(r.data);
  }catch(e){ res.json(PAKET_LIST); }
});

// FIX: sebelumnya endpoint ini (dan WP REST-nya) terbuka tanpa autentikasi sama
// sekali -> siapa pun bisa membaca data pribadi & status pembayaran seluruh jamaah
// (kebocoran data / IDOR). Sekarang Node memakai Basic Auth (Application Password)
// ke WordPress, DAN WP-nya (lihat imtiyaz-connector.php) mewajibkan user login.
app.get('/api/jamaah/:id', async (req,res)=>{
  try{ const r = await axios.get(`${WP_URL}/wp-json/imtiyaz/v1/jamaah/${req.params.id}`, { auth: WP_AUTH }); res.json(r.data); }
  catch(e){ res.status(404).json({error:'Jamaah tidak ditemukan'}); }
});

app.get('/api/jamaah', async (req,res)=>{
  try{ const r = await axios.get(`${WP_URL}/wp-json/imtiyaz/v1/jamaah`, { auth: WP_AUTH }); res.json(r.data); }
  catch(e){ res.status(500).json({error:'Gagal load jamaah'}); }
});

app.get('/api/wa-admin', async (req,res)=>{
  try{ const r = await axios.get(`${WP_URL}/wp-json/imtiyaz/v1/wa-admin`); res.json(r.data); }
  catch(e){ res.json({wa_admin:'628112776543', wa_link:'https://wa.me/628112776543'}); }
});

app.post('/api/upload-bukti', requireApiKey, upload.single('bukti'), async (req,res)=>{
  let tempPath = req.file ? req.file.path : null;
  try{
    const { jamaah_id } = req.body;
    if(!jamaah_id || !req.file) return res.status(400).json({error:'jamaah_id dan file bukti wajib'});
    const form = new FormData();
    form.append('file', fs.createReadStream(req.file.path), {filename:req.file.originalname});
    const wpMedia = await axios.post(`${WP_URL}/wp-json/wp/v2/media`, form, { headers:{...form.getHeaders()}, auth: WP_AUTH });
    await axios.post(`${WP_URL}/wp-json/imtiyaz/v1/upload-bukti`, { jamaah_id, bukti_url: wpMedia.data.source_url }, { auth: WP_AUTH });
    res.json({ success:true, bukti_url: wpMedia.data.source_url, status:'Menunggu Verifikasi', message:'Bukti diterima' });
  }catch(e){ res.status(500).json({error:'Gagal upload bukti', detail:e.message}); }
  finally { if (tempPath) fs.unlink(tempPath, () => {}); } // FIX: dulu file temp tidak dihapus kalau upload gagal
});

app.post('/api/update-checklist', requireApiKey, async (req,res)=>{
  try{ const r = await axios.post(`${WP_URL}/wp-json/imtiyaz/v1/update-checklist`, req.body, { auth: WP_AUTH }); res.json({ success:true, message:'Checklist tersimpan - Fitur 3', data:r.data }); }
  catch(e){ res.status(500).json({error:'Gagal update checklist'}); }
});

app.post('/api/skrining', requireApiKey, async (req,res)=>{
  try{ const r = await axios.post(`${WP_URL}/wp-json/imtiyaz/v1/skrining`, req.body, { auth: WP_AUTH }); res.json({ success:true, message:'Skrining terkirim - Fitur 8', id:r.data.id }); }
  catch(e){ res.status(500).json({error:'Gagal kirim skrining'}); }
});

app.listen(PORT, ()=>{ console.log(`Imtiyaz API v1.5.1 jalan di port ${PORT}`); });
