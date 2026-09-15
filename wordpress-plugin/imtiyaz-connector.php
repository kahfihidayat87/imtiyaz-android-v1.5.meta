<?php
/**
 * Plugin Name: Imtiyaz Connector - Simple Pro v1.5.1
 * Description: Backend v1.5.1 - 5 Fitur (1 WA Form, 2 Bayar+Upload, 3 Checklist, 4 Doa Offline, 8 Skrining) - REST API sekarang butuh autentikasi untuk endpoint yang menulis atau membaca data pribadi jamaah.
 * Version: 1.5.1
 * Author: Imtiyaz Tour Jogja
 */
if (!defined('ABSPATH')) exit;

class Imtiyaz_Connector_Clear {
    private $paket_list = [
        ['id'=>'slamet','nama'=>'Paket Slamet','kategori'=>'Ekonomis','durasi'=>'9 Hari','harga'=>'Rp 28,9 jt','fasilitas'=>'Bus Jogja-Jakarta, Hotel *3 250m/750m, 45 Pax','badge'=>''],
        ['id'=>'ayem','nama'=>'Paket Ayem Tentrem','kategori'=>'Hemat','durasi'=>'9 Hari','harga'=>'Rp 29,9 jt','fasilitas'=>'Pesawat YIA-CGK, Hotel *3, 35-45 Pax','badge'=>''],
        ['id'=>'linuwih','nama'=>'Paket Linuwih','kategori'=>'Reguler','durasi'=>'9 Hari','harga'=>'Rp 37,4 jt','fasilitas'=>'Garuda/Saudia Langsung, Hotel B4 250m, Max 25','badge'=>'Paling Diminati'],
        ['id'=>'kamulyan','nama'=>'Paket Kamulyan','kategori'=>'Nyaman Lansia','durasi'=>'9 Hari','harga'=>'Rp 41,9 jt','fasilitas'=>'Garuda/Saudia, Hotel B5 250m, Max 20, Kursi Roda','badge'=>'Premium'],
        ['id'=>'plus','nama'=>'Paket Plus','kategori'=>'Plus Wisata','durasi'=>'13 Hari','harga'=>'Rp 40,9 jt','fasilitas'=>'Plus Mesir/Turki, Hotel *4/*5','badge'=>''],
    ];
    private $dokumen_list = ['ktp'=>'KTP','kk'=>'Kartu Keluarga','paspor'=>'Paspor','vaksin'=>'Vaksin Meningitis','foto'=>'Foto 4x6','nikah'=>'Buku Nikah'];

    public function __construct(){
        add_action('init', [$this, 'register_cpt']);
        add_action('add_meta_boxes', [$this, 'add_metaboxes']);
        add_action('save_post', [$this, 'save_meta']);
        add_action('rest_api_init', [$this, 'register_rest']);
        add_action('admin_menu', [$this, 'admin_menu']);
    }
    public function register_cpt(){
        register_post_type('jamaah', ['labels'=>['name'=>'Data Jamaah','singular_name'=>'Jamaah'],'public'=>true,'show_in_rest'=>true,'supports'=>['title','editor'],'menu_icon'=>'dashicons-groups','show_in_menu'=>false]);
        register_post_type('skrining_kesehatan', ['labels'=>['name'=>'Skrining Kesehatan','singular_name'=>'Skrining'],'public'=>true,'show_in_rest'=>true,'supports'=>['title','editor'],'menu_icon'=>'dashicons-heart','show_in_menu'=>false]);
    }
    public function admin_menu(){
        add_menu_page('Imtiyaz Dashboard', 'Imtiyaz App', 'manage_options', 'imtiyaz-app', [$this,'dashboard_page'], 'dashicons-smartphone', 25);
        add_submenu_page('imtiyaz-app', 'Data Jamaah & Pembayaran', 'Data Jamaah & Bayar', 'manage_options', 'edit.php?post_type=jamaah');
        add_submenu_page('imtiyaz-app', 'Checklist Dokumen', 'Checklist Dokumen', 'manage_options', 'imtiyaz-checklist', [$this,'checklist_page']);
        add_submenu_page('imtiyaz-app', 'Skrining Kesehatan', 'Skrining Lansia', 'manage_options', 'edit.php?post_type=skrining_kesehatan');
        add_submenu_page('imtiyaz-app', 'Paket Umrah', 'Pilih Paket Umrah', 'manage_options', 'imtiyaz-paket', [$this,'paket_page']);
        add_submenu_page('imtiyaz-app', 'Pengaturan WA', 'WA Admin', 'manage_options', 'imtiyaz-wa', [$this,'wa_page']);
    }
    private function count_by_meta($key,$value){
        $q = new WP_Query(['post_type'=>'jamaah','meta_key'=>$key,'meta_value'=>$value,'posts_per_page'=>-1,'fields'=>'ids']);
        return $q->found_posts;
    }

    // FIX KEAMANAN UTAMA: sebelumnya SEMUA route di bawah pakai 'permission_callback'=>'__return_true',
    // artinya siapa pun di internet tanpa login bisa membaca data pribadi jamaah (nama, status bayar)
    // dan menulis/mengubah status pembayaran, checklist, serta mengirim data kesehatan atas nama
    // jamaah manapun (IDOR). Sekarang route yang sensitif mewajibkan user terautentikasi
    // (via Basic Auth / Application Password yang dikirim dari app.js) dengan kapabilitas 'edit_posts'.
    private function require_auth() {
        return current_user_can('edit_posts');
    }

    public function dashboard_page(){
        $total = wp_count_posts('jamaah')->publish;
        $belum = $this->count_by_meta('_status_pembayaran','Belum Lunas');
        $menunggu = $this->count_by_meta('_status_pembayaran','Menunggu Verifikasi');
        $skrining = wp_count_posts('skrining_kesehatan')->publish;
        echo '<div class="wrap"><h1>Imtiyaz Dashboard v1.5.1</h1>';
        echo '<div style="display:grid;grid-template-columns:1fr 1fr 1fr 1fr;gap:12px;margin:20px 0">';
        echo "<div style='background:#d1fae5;padding:20px;border-radius:12px'><h2>$total</h2><p>Total Jamaah</p></div>";
        echo "<div style='background:#fef3c7;padding:20px;border-radius:12px'><h2>$belum</h2><p>Belum Lunas</p></div>";
        echo "<div style='background:#dbeafe;padding:20px;border-radius:12px'><h2>$menunggu</h2><p>Menunggu Verifikasi</p></div>";
        echo "<div style='background:#fce7f3;padding:20px;border-radius:12px'><h2>$skrining</h2><p>Skrining Masuk</p></div>";
        echo '</div>';
        echo '<div style="background:#fff;padding:20px;border-radius:12px;border:1px solid #e5e7eb"><h3>Status</h3>';
        echo '<p>Endpoint /jamaah, /upload-bukti, /update-checklist, dan /skrining sekarang WAJIB autentikasi (Application Password). Pastikan app.js sudah diisi WP_USER & WP_APP_PASSWORD yang valid di .env, atau permintaan akan ditolak 401/403.</p>';
        echo '<h4>Test API publik (tanpa login):</h4><code>pastiumrah.com/wp-json/imtiyaz/v1/paket<br>pastiumrah.com/wp-json/imtiyaz/v1/wa-admin</code>';
        echo '</div></div>';
    }
    public function checklist_page(){
        echo '<div class="wrap"><h1>Checklist Dokumen - 6 Dokumen (Fitur 3)</h1>';
        echo '<table class="widefat"><thead><tr><th>Nama</th><th>Paket</th><th>KTP</th><th>KK</th><th>Paspor</th><th>Vaksin</th><th>Foto</th><th>Nikah</th><th>Progress</th></tr></thead><tbody>';
        $posts = get_posts(['post_type'=>'jamaah','numberposts'=>100]);
        foreach($posts as $p){
            $checklist = get_post_meta($p->ID,'_checklist_dokumen',true); if(!is_array($checklist)) $checklist=[];
            $done = count(array_filter($checklist));
            $paket_id = get_post_meta($p->ID,'_paket_id',true);
            echo "<tr><td><b>{$p->post_title}</b></td><td>$paket_id</td>";
            foreach($this->dokumen_list as $key=>$label){ $c = !empty($checklist[$key]) ? '✅' : '❌'; echo "<td>$c</td>"; }
            echo "<td><b>$done / 6</b></td></tr>";
        }
        echo '</tbody></table></div>';
    }
    public function paket_page(){
        echo '<div class="wrap"><h1>Pilih Paket Umrah - 5 Paket Native (Tanpa WebView)</h1>';
        echo '<table class="widefat"><thead><tr><th>Paket</th><th>Kategori</th><th>Durasi</th><th>Harga</th><th>Fasilitas</th></tr></thead><tbody>';
        foreach($this->paket_list as $p){ echo "<tr><td><b>{$p['nama']}</b> {$p['badge']}</td><td>{$p['kategori']}</td><td>{$p['durasi']}</td><td><b>{$p['harga']}</b></td><td>{$p['fasilitas']}</td></tr>"; }
        echo '</tbody></table></div>';
    }
    public function wa_page(){
        $wa = get_option('imtiyaz_wa_admin','628112776543');
        if(isset($_POST['wa_admin'])){ update_option('imtiyaz_wa_admin', sanitize_text_field($_POST['wa_admin'])); $wa = $_POST['wa_admin']; echo '<div class="notice notice-success"><p>WA Admin disimpan</p></div>'; }
        echo '<div class="wrap"><h1>WA Admin - Fitur 1</h1><form method="post"><table class="form-table"><tr><th>Nomor WA Admin</th><td><input type="text" name="wa_admin" value="'.esc_attr($wa).'" style="width:300px"><p class="description">Format: 628112776543 tanpa + untuk wa.me</p></td></tr></table><p class="submit"><button class="button button-primary">Simpan</button></p></form>';
        echo '<div style="background:#fff;padding:15px;border-radius:8px;margin-top:20px"><h3>Format Pesan WA Otomatis:</h3><code>Assalamualaikum, saya mau daftar Paket [Nama] [Harga] - Nama: [Input] - HP: [Input]</code></div></div>';
    }
    public function add_metaboxes(){
        add_meta_box('imtiyaz_jamaah_detail','Status Pembayaran + Checklist + Bukti (Fitur 2,3)',[$this,'render_jamaah_box'],'jamaah','normal','high');
    }
    public function render_jamaah_box($post){
        $total = get_post_meta($post->ID,'_total_tagihan',true); $sudah = get_post_meta($post->ID,'_sudah_dibayar',true); $sisa = get_post_meta($post->ID,'_sisa_tagihan',true);
        $status = get_post_meta($post->ID,'_status_pembayaran',true); if(empty($status)) $status='Belum Lunas';
        $bukti = get_post_meta($post->ID,'_bukti_transfer',true); $paket_id = get_post_meta($post->ID,'_paket_id',true);
        $checklist = get_post_meta($post->ID,'_checklist_dokumen',true); if(!is_array($checklist)) $checklist=[];
        wp_nonce_field('imtiyaz_save','imtiyaz_nonce');
        echo '<div style="display:grid;grid-template-columns:1fr 1fr;gap:15px">';
        echo '<p><label>Paket ID</label><br><select name="paket_id" style="width:100%"><option value="">Pilih Paket</option>';
        foreach($this->paket_list as $p){ $sel = selected($paket_id,$p['id'],false); echo "<option value='{$p['id']}' $sel>{$p['nama']} - {$p['harga']}</option>"; }
        echo '</select></p>';
        echo '<p><label>Status Pembayaran (Fitur 2)</label><br><select name="status_pembayaran" style="width:100%"><option '.selected($status,'Belum Lunas',false).'>Belum Lunas</option><option '.selected($status,'Menunggu Verifikasi',false).'>Menunggu Verifikasi</option><option '.selected($status,'Lunas',false).'>Lunas</option></select></p>';
        echo '</div>';
        echo '<div style="display:grid;grid-template-columns:1fr 1fr 1fr;gap:15px">';
        echo '<p><label>Total Tagihan</label><br><input type="number" name="total_tagihan" id="total_tagihan" value="'.esc_attr($total).'" style="width:100%" oninput="calcSisa()"></p>';
        echo '<p><label>Sudah Dibayar</label><br><input type="number" name="sudah_dibayar" id="sudah_dibayar" value="'.esc_attr($sudah).'" style="width:100%" oninput="calcSisa()"></p>';
        echo '<p><label>Sisa Tagihan (auto)</label><br><input type="number" name="sisa_tagihan" id="sisa_tagihan" value="'.esc_attr($sisa).'" style="width:100%;background:#fff3cd" readonly></p>';
        echo '</div>';
        echo '<script>function calcSisa(){var t=parseInt(document.getElementById("total_tagihan").value)||0;var s=parseInt(document.getElementById("sudah_dibayar").value)||0;document.getElementById("sisa_tagihan").value=t-s;}</script>';
        if($bukti){ echo '<p><label>Bukti Transfer (Fitur 2 - dari App)</label><br><a href="'.esc_url($bukti).'" target="_blank"><img src="'.esc_url($bukti).'" style="max-width:300px;border-radius:8px"></a></p>'; }
        echo '<hr><h3>Checklist Dokumen 6 item (Fitur 3)</h3><div style="display:grid;grid-template-columns:1fr 1fr 1fr;gap:10px">';
        foreach($this->dokumen_list as $key=>$label){ $checked = !empty($checklist[$key]) ? 'checked' : ''; echo "<label style='background:#f9fafb;padding:10px;border-radius:8px'><input type='checkbox' name='checklist[$key]' value='1' $checked> $label</label>"; }
        echo '</div>';
    }
    public function save_meta($post_id){
        if(!isset($_POST['imtiyaz_nonce']) || !wp_verify_nonce($_POST['imtiyaz_nonce'],'imtiyaz_save')) return;
        if(defined('DOING_AUTOSAVE') && DOING_AUTOSAVE) return;
        foreach(['total_tagihan','sudah_dibayar','sisa_tagihan','status_pembayaran','paket_id'] as $f){ if(isset($_POST[$f])) update_post_meta($post_id,'_'.$f,sanitize_text_field($_POST[$f])); }
        $total = intval($_POST['total_tagihan'] ?? 0); $sudah = intval($_POST['sudah_dibayar'] ?? 0); if($total>0) update_post_meta($post_id,'_sisa_tagihan',$total-$sudah);
        $checklist=[]; foreach($this->dokumen_list as $key=>$label){ $checklist[$key] = isset($_POST['checklist'][$key]); } update_post_meta($post_id,'_checklist_dokumen',$checklist);
    }
    public function register_rest(){
        // Publik & aman untuk dibaca siapa saja (tidak mengandung data pribadi):
        register_rest_route('imtiyaz/v1','/paket',['methods'=>'GET','callback'=>[$this,'api_paket'],'permission_callback'=>'__return_true']);
        register_rest_route('imtiyaz/v1','/wa-admin',['methods'=>'GET','callback'=>[$this,'api_wa_admin'],'permission_callback'=>'__return_true']);

        // FIX: dulu '__return_true' -> sekarang wajib login (dipanggil oleh app.js via Basic Auth).
        register_rest_route('imtiyaz/v1','/jamaah/(?P<id>\d+)',['methods'=>'GET','callback'=>[$this,'api_jamaah_detail'],'permission_callback'=>[$this,'require_auth']]);
        register_rest_route('imtiyaz/v1','/jamaah',['methods'=>'GET','callback'=>[$this,'api_jamaah_list'],'permission_callback'=>[$this,'require_auth']]);
        register_rest_route('imtiyaz/v1','/upload-bukti',['methods'=>'POST','callback'=>[$this,'api_upload_bukti'],'permission_callback'=>[$this,'require_auth']]);
        register_rest_route('imtiyaz/v1','/update-checklist',['methods'=>'POST','callback'=>[$this,'api_update_checklist'],'permission_callback'=>[$this,'require_auth']]);
        register_rest_route('imtiyaz/v1','/skrining',['methods'=>'POST','callback'=>[$this,'api_skrining'],'permission_callback'=>[$this,'require_auth']]);
    }
    public function api_paket(){ return rest_ensure_response($this->paket_list); }
    public function api_wa_admin(){ return rest_ensure_response(['wa_admin'=>get_option('imtiyaz_wa_admin','628112776543'),'wa_link'=>'https://wa.me/'.get_option('imtiyaz_wa_admin','628112776543')]); }
    public function api_jamaah_detail($req){ $id=$req['id']; $post=get_post($id); if(!$post || $post->post_type!=='jamaah') return new WP_Error('not_found','Jamaah tidak ditemukan',['status'=>404]); return rest_ensure_response(['id'=>$post->ID,'nama'=>$post->post_title,'paket_id'=>get_post_meta($id,'_paket_id',true),'total_tagihan'=>get_post_meta($id,'_total_tagihan',true),'sudah_dibayar'=>get_post_meta($id,'_sudah_dibayar',true),'sisa_tagihan'=>get_post_meta($id,'_sisa_tagihan',true),'status_pembayaran'=>get_post_meta($id,'_status_pembayaran',true),'bukti_transfer'=>get_post_meta($id,'_bukti_transfer',true),'checklist_dokumen'=>get_post_meta($id,'_checklist_dokumen',true)]); }
    public function api_jamaah_list(){ $posts=get_posts(['post_type'=>'jamaah','numberposts'=>100]); $data=[]; foreach($posts as $p){ $data[]=['id'=>$p->ID,'nama'=>$p->post_title,'paket_id'=>get_post_meta($p->ID,'_paket_id',true),'status_pembayaran'=>get_post_meta($p->ID,'_status_pembayaran',true),'checklist'=>get_post_meta($p->ID,'_checklist_dokumen',true)]; } return rest_ensure_response($data); }
    public function api_upload_bukti($req){ $params=$req->get_json_params(); if(empty($params)) $params=$req->get_params(); $jamaah_id=intval($params['jamaah_id']??0); $bukti_url=esc_url_raw($params['bukti_url']??''); if($jamaah_id && $bukti_url){ update_post_meta($jamaah_id,'_bukti_transfer',$bukti_url); update_post_meta($jamaah_id,'_status_pembayaran','Menunggu Verifikasi'); return rest_ensure_response(['success'=>true,'message'=>'Bukti diterima - Menunggu Verifikasi']); } return new WP_Error('invalid','jamaah_id dan bukti_url wajib',['status'=>400]); }
    public function api_update_checklist($req){ $params=$req->get_json_params(); if(empty($params)) $params=$req->get_params(); $jamaah_id=intval($params['jamaah_id']??0); $checklist=$params['checklist']??[]; if($jamaah_id && is_array($checklist)){ update_post_meta($jamaah_id,'_checklist_dokumen',$checklist); return rest_ensure_response(['success'=>true,'checklist'=>$checklist,'message'=>'Checklist tersimpan']); } return new WP_Error('invalid','Data tidak lengkap',['status'=>400]); }
    public function api_skrining($req){ $params=$req->get_json_params(); if(empty($params)) $params=$req->get_params(); $post_id=wp_insert_post(['post_type'=>'skrining_kesehatan','post_title'=>'Skrining - '.($params['nama_lengkap']??'Anonim').' - '.date('d-m-Y H:i'),'post_status'=>'publish']); update_post_meta($post_id,'_skrining_data',wp_json_encode($params,JSON_UNESCAPED_UNICODE)); if(!empty($params['jamaah_id'])) update_post_meta($post_id,'_jamaah_id',intval($params['jamaah_id'])); return rest_ensure_response(['success'=>true,'id'=>$post_id,'message'=>'Skrining terkirim']); }
}
new Imtiyaz_Connector_Clear();
