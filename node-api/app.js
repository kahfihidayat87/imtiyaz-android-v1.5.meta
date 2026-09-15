require('dotenv').config();
const express = require('express');
const cors = require('cors');
const multer = require('multer');
const axios = require('axios');
const FormData = require('form-data');
const fs = require('fs');

const app = express();
const PORT = process.env.PORT || 3000;
const WP_URL = process.env.WP_URL || 'https://pastiumrah.com';
const WP_USER = process.env.WP_USER || 'admin';
const WP_APP_PASSWORD = process.env.WP_APP_PASSWORD || 'xxxx xxxx xxxx xxxx';

app.use(cors());
app.use(express.json());
const upload = multer({ dest: 'uploads/', limits: { fileSize: 5*1024*1024 } });

const PAKET_LIST = [
  { id:'slamet', nama:'Paket Slamet', kategori:'Ekonomis', durasi:'9 Hari', harga:'Rp 28,9 jt', fasilitas:'Bus Jogja-Jakarta, Hotel *3 250m/750m, 45 Pax', badge:'' },
  { id:'ayem', nama:'Paket Ayem Tentrem', kategori:'Hemat', durasi:'9 Hari', harga:'Rp 29,9 jt', fasilitas:'Pesawat YIA-CGK, Hotel *3, 35-45 Pax', badge:'' },
  { id:'linuwih', nama:'Paket Linuwih', kategori:'Reguler', durasi:'9 Hari', harga:'Rp 37,4 jt', fasilitas:'Garuda/Saudia Langsung, Hotel B4 250m, Max 25', badge:'Paling Diminati' },
  { id:'kamulyan', nama:'Paket Kamulyan', kategori:'Nyaman Lansia', durasi:'9 Hari', harga:'Rp 41,9 jt', fasilitas:'Garuda/Saudia, Hotel B5 250m, Max 20, Kursi Roda', badge:'Premium' },
  { id:'plus', nama:'Paket Plus', kategori:'Plus Wisata', durasi:'13 Hari', harga:'Rp 40,9 jt', fasilitas:'Plus Mesir/Turki, Hotel *4/*5', badge:'' }
];

app.get('/', (req,res)=>{
  res.json({
    status:'OK CLEAR',
    service:'Imtiyaz Node API v1.5.0 CLEAR - 5 Fitur (1,2,3,4,8) - Ready Tanpa Error',
    fitur_aktif:['1 WA Langsung + Form','2 Status Bayar + Upload Bukti','3 Checklist Dokumen','4 Doa Offline','8 Skrining 29 Pertanyaan'],
    fitur_hilang:['5 Jadwal HILANG','6 Galeri HILANG','7 Absensi HILANG','9 Sertifikat HILANG','GPS HILANG','WebView HILANG'],
    paket: PAKET_LIST,
    endpoints:['/api/paket','/api/jamaah/:id','/api/jamaah','/api/upload-bukti','/api/update-checklist','/api/skrining','/api/wa-admin'],
    clear:true
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

app.get('/api/jamaah/:id', async (req,res)=>{
  try{ const r = await axios.get(`${WP_URL}/wp-json/imtiyaz/v1/jamaah/${req.params.id}`); res.json(r.data); }
  catch(e){ res.status(404).json({error:'Jamaah tidak ditemukan - CLEAR'}); }
});

app.get('/api/jamaah', async (req,res)=>{
  try{ const r = await axios.get(`${WP_URL}/wp-json/imtiyaz/v1/jamaah`); res.json(r.data); }
  catch(e){ res.status(500).json({error:'Gagal load jamaah - CLEAR'}); }
});

app.get('/api/wa-admin', async (req,res)=>{
  try{ const r = await axios.get(`${WP_URL}/wp-json/imtiyaz/v1/wa-admin`); res.json(r.data); }
  catch(e){ res.json({wa_admin:'628112776543', wa_link:'https://wa.me/628112776543', clear:true}); }
});

app.post('/api/upload-bukti', upload.single('bukti'), async (req,res)=>{
  try{
    const { jamaah_id } = req.body;
    if(!jamaah_id || !req.file) return res.status(400).json({error:'jamaah_id dan file bukti wajib - CLEAR'});
    const form = new FormData();
    form.append('file', fs.createReadStream(req.file.path), {filename:req.file.originalname});
    const wpMedia = await axios.post(`${WP_URL}/wp-json/wp/v2/media`, form, { headers:{...form.getHeaders()}, auth:{username:WP_USER,password:WP_APP_PASSWORD} });
    await axios.post(`${WP_URL}/wp-json/imtiyaz/v1/upload-bukti`, { jamaah_id, bukti_url: wpMedia.data.source_url }, { auth:{username:WP_USER,password:WP_APP_PASSWORD} }).catch(()=>{});
    fs.unlinkSync(req.file.path);
    res.json({ success:true, bukti_url: wpMedia.data.source_url, status:'Menunggu Verifikasi', message:'Bukti diterima - Fitur 2 - CLEAR' });
  }catch(e){ res.status(500).json({error:'Gagal upload bukti - CLEAR', detail:e.message}); }
});

app.post('/api/update-checklist', async (req,res)=>{
  try{ const r = await axios.post(`${WP_URL}/wp-json/imtiyaz/v1/update-checklist`, req.body, { headers:{'Content-Type':'application/json'} }); res.json({ success:true, message:'Checklist CLEAR - Fitur 3', data:r.data }); }
  catch(e){ res.status(500).json({error:'Gagal update checklist - CLEAR'}); }
});

app.post('/api/skrining', async (req,res)=>{
  try{ const r = await axios.post(`${WP_URL}/wp-json/imtiyaz/v1/skrining`, req.body); res.json({ success:true, message:'Skrining CLEAR - Fitur 8', id:r.data.id }); }
  catch(e){ res.status(500).json({error:'Gagal kirim skrining - CLEAR'}); }
});

app.listen(PORT, ()=>{ console.log(`Imtiyaz API v1.5.0 CLEAR - 5 Fitur (1,2,3,4,8) jalan di ${PORT} - Ready Tanpa Error`); });
