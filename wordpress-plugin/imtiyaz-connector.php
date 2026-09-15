<?php
/**
 * Plugin Name: Imtiyaz Connector - Simple Pro v1.7.0
 * Description: Backend v1.7.0 - Login jamaah (username/password dibuat Admin, bukan self-register); endpoint upload bukti, checklist, dan skrining sekarang wajib token login jamaah yang valid. Paket, Dokumen, dan Kontak/WA tetap bisa diedit Admin dari WP Admin.
 * Version: 1.7.0
 * Author: Imtiyaz Tour Jogja
 */
if (!defined('ABSPATH')) exit;

class Imtiyaz_Connector_Clear {

    // Nilai default ini HANYA dipakai kalau admin belum pernah menyimpan pengaturan sama sekali
    // (instalasi baru). Setelah admin menyimpan sekali lewat halaman Pengaturan, nilai yang
    // tersimpan di database (wp_options) yang dipakai, bukan default ini.
    const DEFAULT_PAKET = [
        ['id'=>'slamet','nama'=>'Paket Slamet','kategori'=>'Ekonomis','durasi'=>'9 Hari','harga'=>'Rp 28,9 jt','fasilitas'=>'Bus Jogja-Jakarta, Hotel *3 250m/750m, 45 Pax','badge'=>''],
        ['id'=>'ayem','nama'=>'Paket Ayem Tentrem','kategori'=>'Hemat','durasi'=>'9 Hari','harga'=>'Rp 29,9 jt','fasilitas'=>'Pesawat YIA-CGK, Hotel *3, 35-45 Pax','badge'=>''],
        ['id'=>'linuwih','nama'=>'Paket Linuwih','kategori'=>'Reguler','durasi'=>'9 Hari','harga'=>'Rp 37,4 jt','fasilitas'=>'Garuda/Saudia Langsung, Hotel B4 250m, Max 25','badge'=>'Paling Diminati'],
        ['id'=>'kamulyan','nama'=>'Paket Kamulyan','kategori'=>'Nyaman Lansia','durasi'=>'9 Hari','harga'=>'Rp 41,9 jt','fasilitas'=>'Garuda/Saudia, Hotel B5 250m, Max 20, Kursi Roda','badge'=>'Premium'],
        ['id'=>'plus','nama'=>'Paket Plus','kategori'=>'Plus Wisata','durasi'=>'13 Hari','harga'=>'Rp 40,9 jt','fasilitas'=>'Plus Mesir/Turki, Hotel *4/*5','badge'=>''],
    ];
    const DEFAULT_DOKUMEN = ['ktp'=>'KTP','kk'=>'Kartu Keluarga','paspor'=>'Paspor','vaksin'=>'Vaksin Meningitis','foto'=>'Foto 4x6','nikah'=>'Buku Nikah'];
    const DEFAULT_KONTAK = ['nama_travel'=>'Imtiyaz Tour Jogja','alamat'=>'PPIU U383/2021 • Jln. Pertapan, Tegal Cerme RT08, Baturetno, Banguntapan, Bantul','kontak'=>'0811-277-6543 • pastiumrah.com'];

    public function __construct(){
        add_action('init', [$this, 'register_cpt']);
        add_action('add_meta_boxes', [$this, 'add_metaboxes']);
        add_action('save_post', [$this, 'save_meta']);
        add_action('rest_api_init', [$this, 'register_rest']);
        add_action('admin_menu', [$this, 'admin_menu']);
        add_action('admin_enqueue_scripts', [$this, 'admin_assets']);
    }

    // ============================================================
    // SUMBER DATA — sekarang dari wp_options (bisa diedit admin),
    // bukan lagi array hardcode di kode. Ini perbaikan utama v1.6.0.
    // ============================================================
    public function get_paket_list(){
        $data = get_option('imtiyaz_paket_list', null);
        return is_array($data) && !empty($data) ? $data : self::DEFAULT_PAKET;
    }
    public function get_dokumen_list(){
        $data = get_option('imtiyaz_dokumen_list', null);
        return is_array($data) && !empty($data) ? $data : self::DEFAULT_DOKUMEN;
    }
    public function get_kontak(){
        $data = get_option('imtiyaz_kontak', null);
        $defaults = self::DEFAULT_KONTAK;
        return is_array($data) ? array_merge($defaults, $data) : $defaults;
    }

    public function register_cpt(){
        register_post_type('jamaah', ['labels'=>['name'=>'Data Jamaah','singular_name'=>'Jamaah'],'public'=>true,'show_in_rest'=>true,'supports'=>['title','editor'],'menu_icon'=>'dashicons-groups','show_in_menu'=>false]);
        register_post_type('skrining_kesehatan', ['labels'=>['name'=>'Skrining Kesehatan','singular_name'=>'Skrining'],'public'=>true,'show_in_rest'=>true,'supports'=>['title','editor'],'menu_icon'=>'dashicons-heart','show_in_menu'=>false]);
    }

    // MENU DIRAPIKAN: dari 6 submenu tersebar (termasuk 1 yang cuma tampilan read-only dan
    // 1 lagi khusus WA) jadi 4 submenu yang jelas fungsinya. Kelola Paket, Kelola Dokumen,
    // dan Kontak/WA digabung jadi SATU halaman "Pengaturan Aplikasi" bertab supaya admin
    // tidak perlu loncat-loncat menu untuk kerja yang saling terkait.
    public function admin_menu(){
        add_menu_page('Imtiyaz Dashboard', 'Imtiyaz App', 'manage_options', 'imtiyaz-app', [$this,'dashboard_page'], 'dashicons-smartphone', 25);
        add_submenu_page('imtiyaz-app', 'Data Jamaah & Pembayaran', 'Data Jamaah & Bayar', 'manage_options', 'edit.php?post_type=jamaah');
        add_submenu_page('imtiyaz-app', 'Checklist Dokumen', 'Checklist Dokumen', 'manage_options', 'imtiyaz-checklist', [$this,'checklist_page']);
        add_submenu_page('imtiyaz-app', 'Skrining Kesehatan', 'Skrining Lansia', 'manage_options', 'edit.php?post_type=skrining_kesehatan');
        add_submenu_page('imtiyaz-app', 'Pengaturan Aplikasi', 'Pengaturan Aplikasi', 'manage_options', 'imtiyaz-settings', [$this,'settings_page']);
    }

    public function admin_assets($hook){
        if (strpos($hook, 'imtiyaz') === false) return;
        // Vanilla JS kecil untuk tombol tambah/hapus baris di tabel Paket & Dokumen — tidak
        // butuh build tool/npm, cukup di-enqueue inline supaya file plugin tetap satu file.
        wp_add_inline_script('jquery-core', "
            function imtiyazAddRow(tableBodyId, templateRowId){
                var tbody = document.getElementById(tableBodyId);
                var tpl = document.getElementById(templateRowId);
                var clone = tpl.cloneNode(true);
                clone.removeAttribute('id');
                clone.style.display = '';
                tbody.appendChild(clone);
            }
            function imtiyazRemoveRow(btn){
                var row = btn.closest('tr');
                row.parentNode.removeChild(row);
            }
        ");
    }

    private function count_by_meta($key,$value){
        $q = new WP_Query(['post_type'=>'jamaah','meta_key'=>$key,'meta_value'=>$value,'posts_per_page'=>-1,'fields'=>'ids']);
        return $q->found_posts;
    }

    // FIX KEAMANAN: route yang menulis/membaca data pribadi jamaah wajib login (Application
    // Password), tidak lagi '__return_true' terbuka untuk publik seperti versi sebelumnya.
    private function require_auth() {
        return current_user_can('edit_posts');
    }

    // ============================================================
    // LOGIN JAMAAH — username & password DIBUAT OLEH ADMIN lewat
    // metabox di halaman Data Jamaah (lihat render_jamaah_box), bukan
    // oleh jamaah sendiri. Setelah login sukses, token yang didapat
    // dipakai untuk membuktikan permintaan berikutnya (upload bukti,
    // checklist, skrining) benar-benar datang dari jamaah pemilik ID
    // itu -- sebelumnya jamaah_id polos bisa ditebak/dipakai siapa saja.
    // ============================================================
    private function find_jamaah_by_username($username){
        $q = new WP_Query(['post_type'=>'jamaah','meta_key'=>'_jamaah_username','meta_value'=>$username,'posts_per_page'=>1,'fields'=>'ids']);
        return $q->posts ? $q->posts[0] : null;
    }
    private function verify_jamaah_token($jamaah_id, $token){
        if(empty($jamaah_id) || empty($token)) return false;
        $stored = get_post_meta($jamaah_id, '_jamaah_token', true);
        return !empty($stored) && hash_equals($stored, (string)$token);
    }
    public function api_login($req){
        $params = $req->get_json_params(); if(empty($params)) $params = $req->get_params();
        $username = sanitize_user($params['username'] ?? '', true);
        $password = (string)($params['password'] ?? '');
        if($username === '' || $password === '') return new WP_Error('invalid','Username dan password wajib diisi',['status'=>400]);

        $jamaah_id = $this->find_jamaah_by_username($username);
        if(!$jamaah_id) return new WP_Error('invalid_login','Username atau password salah',['status'=>401]);

        $hash = get_post_meta($jamaah_id, '_jamaah_password_hash', true);
        if(empty($hash) || !wp_check_password($password, $hash)){
            return new WP_Error('invalid_login','Username atau password salah',['status'=>401]);
        }

        $token = wp_generate_password(48, false, false);
        update_post_meta($jamaah_id, '_jamaah_token', $token);
        $post = get_post($jamaah_id);
        return rest_ensure_response(['success'=>true, 'jamaah_id'=>$jamaah_id, 'nama'=>$post->post_title, 'token'=>$token]);
    }
    public function api_logout($req){
        $params = $req->get_json_params(); if(empty($params)) $params = $req->get_params();
        $jamaah_id = intval($params['jamaah_id'] ?? 0);
        $token = $params['token'] ?? '';
        if($this->verify_jamaah_token($jamaah_id, $token)) delete_post_meta($jamaah_id, '_jamaah_token');
        return rest_ensure_response(['success'=>true]);
    }

    public function dashboard_page(){
        $total = wp_count_posts('jamaah')->publish;
        $belum = $this->count_by_meta('_status_pembayaran','Belum Lunas');
        $menunggu = $this->count_by_meta('_status_pembayaran','Menunggu Verifikasi');
        $skrining = wp_count_posts('skrining_kesehatan')->publish;
        $jumlah_paket = count($this->get_paket_list());
        $jumlah_dokumen = count($this->get_dokumen_list());
        echo '<div class="wrap"><h1>Imtiyaz Dashboard v1.6.0</h1>';
        echo '<div style="display:grid;grid-template-columns:1fr 1fr 1fr 1fr;gap:12px;margin:20px 0">';
        echo "<div style='background:#d1fae5;padding:20px;border-radius:12px'><h2>$total</h2><p>Total Jamaah</p></div>";
        echo "<div style='background:#fef3c7;padding:20px;border-radius:12px'><h2>$belum</h2><p>Belum Lunas</p></div>";
        echo "<div style='background:#dbeafe;padding:20px;border-radius:12px'><h2>$menunggu</h2><p>Menunggu Verifikasi</p></div>";
        echo "<div style='background:#fce7f3;padding:20px;border-radius:12px'><h2>$skrining</h2><p>Skrining Masuk</p></div>";
        echo '</div>';
        echo '<div style="background:#fff;padding:20px;border-radius:12px;border:1px solid #e5e7eb;margin-bottom:16px"><h3>Kelola Konten Aplikasi</h3>';
        echo "<p>Saat ini ada <b>$jumlah_paket paket umrah</b> dan <b>$jumlah_dokumen dokumen wajib</b> yang tampil di aplikasi Android. Semuanya bisa diubah langsung tanpa perlu edit kode.</p>";
        echo '<p><a href="'.admin_url('admin.php?page=imtiyaz-settings&tab=paket').'" class="button button-primary">Kelola Paket Umrah</a> ';
        echo '<a href="'.admin_url('admin.php?page=imtiyaz-settings&tab=dokumen').'" class="button">Kelola Dokumen Wajib</a> ';
        echo '<a href="'.admin_url('admin.php?page=imtiyaz-settings&tab=kontak').'" class="button">Kontak &amp; WA</a></p>';
        echo '</div>';
        echo '<div style="background:#fff;padding:20px;border-radius:12px;border:1px solid #e5e7eb"><h3>Status Teknis</h3>';
        echo '<p>Endpoint /jamaah, /upload-bukti, /update-checklist, dan /skrining WAJIB autentikasi (Application Password). Pastikan app.js sudah diisi WP_USER &amp; WP_APP_PASSWORD yang valid, atau permintaan akan ditolak 401/403.</p>';
        echo '<h4>Test API publik (tanpa login):</h4><code>pastiumrah.com/wp-json/imtiyaz/v1/paket<br>pastiumrah.com/wp-json/imtiyaz/v1/wa-admin</code>';
        echo '</div></div>';
    }

    public function checklist_page(){
        $dokumen_list = $this->get_dokumen_list();
        $jumlah = count($dokumen_list);
        echo '<div class="wrap"><h1>Checklist Dokumen ('.$jumlah.' item) <a href="'.admin_url('admin.php?page=imtiyaz-settings&tab=dokumen').'" class="page-title-action">Kelola Daftar Dokumen</a></h1>';
        echo '<table class="widefat"><thead><tr><th>Nama</th><th>Paket</th>';
        foreach($dokumen_list as $label){ echo "<th>".esc_html($label)."</th>"; }
        echo '<th>Progress</th></tr></thead><tbody>';
        $posts = get_posts(['post_type'=>'jamaah','numberposts'=>100]);
        foreach($posts as $p){
            $checklist = get_post_meta($p->ID,'_checklist_dokumen',true); if(!is_array($checklist)) $checklist=[];
            $done = count(array_filter($checklist));
            $paket_id = get_post_meta($p->ID,'_paket_id',true);
            echo "<tr><td><b>".esc_html($p->post_title)."</b></td><td>".esc_html($paket_id)."</td>";
            foreach($dokumen_list as $key=>$label){ $c = !empty($checklist[$key]) ? '✅' : '❌'; echo "<td>$c</td>"; }
            echo "<td><b>$done / $jumlah</b></td></tr>";
        }
        if(empty($posts)) echo '<tr><td colspan="'.($jumlah+3).'">Belum ada data jamaah.</td></tr>';
        echo '</tbody></table></div>';
    }

    // ============================================================
    // HALAMAN PENGATURAN (BARU) — di sinilah admin sekarang punya
    // kendali penuh atas Paket, Dokumen Wajib, dan Kontak/WA.
    // ============================================================
    public function settings_page(){
        $tab = isset($_GET['tab']) ? sanitize_key($_GET['tab']) : 'paket';
        echo '<div class="wrap"><h1>Pengaturan Aplikasi</h1>';
        echo '<h2 class="nav-tab-wrapper">';
        $tabs = ['paket'=>'Paket Umrah', 'dokumen'=>'Dokumen Wajib', 'kontak'=>'Kontak & WA'];
        foreach($tabs as $key=>$label){
            $active = $tab===$key ? 'nav-tab-active' : '';
            echo '<a href="'.admin_url('admin.php?page=imtiyaz-settings&tab='.$key).'" class="nav-tab '.$active.'">'.$label.'</a>';
        }
        echo '</h2><div style="margin-top:20px">';
        if($tab==='dokumen') $this->render_dokumen_tab();
        elseif($tab==='kontak') $this->render_kontak_tab();
        else $this->render_paket_tab();
        echo '</div></div>';
    }

    private function render_paket_tab(){
        if(isset($_POST['imtiyaz_paket_nonce']) && wp_verify_nonce($_POST['imtiyaz_paket_nonce'],'imtiyaz_save_paket')){
            $nama = $_POST['paket_nama'] ?? [];
            $kategori = $_POST['paket_kategori'] ?? [];
            $durasi = $_POST['paket_durasi'] ?? [];
            $harga = $_POST['paket_harga'] ?? [];
            $fasilitas = $_POST['paket_fasilitas'] ?? [];
            $badge = $_POST['paket_badge'] ?? [];
            $rows = []; $used_ids = [];
            for($i=0;$i<count($nama);$i++){
                $n = sanitize_text_field($nama[$i] ?? '');
                if($n==='') continue; // baris kosong (misal baru ditambah lalu tidak diisi) dilewati
                $base_id = sanitize_title($n); $id = $base_id; $suffix=2;
                while(in_array($id,$used_ids,true)){ $id = $base_id.'-'.$suffix; $suffix++; }
                $used_ids[] = $id;
                $rows[] = [
                    'id' => $id,
                    'nama' => $n,
                    'kategori' => sanitize_text_field($kategori[$i] ?? ''),
                    'durasi' => sanitize_text_field($durasi[$i] ?? ''),
                    'harga' => sanitize_text_field($harga[$i] ?? ''),
                    'fasilitas' => sanitize_textarea_field($fasilitas[$i] ?? ''),
                    'badge' => sanitize_text_field($badge[$i] ?? ''),
                ];
            }
            update_option('imtiyaz_paket_list', $rows);
            echo '<div class="notice notice-success is-dismissible"><p>Paket umrah tersimpan ('.count($rows).' paket). Perubahan langsung berlaku di endpoint <code>/wp-json/imtiyaz/v1/paket</code> yang dipakai aplikasi Android.</p></div>';
        }
        $paket_list = $this->get_paket_list();
        echo '<form method="post">';
        wp_nonce_field('imtiyaz_save_paket','imtiyaz_paket_nonce');
        echo '<p>Atur paket umrah yang tampil di aplikasi Android — harga, fasilitas, dan label badge. Klik "Tambah Paket" untuk baris baru, ikon 🗑 untuk hapus.</p>';
        echo '<table class="widefat striped"><thead><tr><th style="width:16%">Nama Paket</th><th style="width:12%">Kategori</th><th style="width:8%">Durasi</th><th style="width:10%">Harga</th><th>Fasilitas</th><th style="width:10%">Badge</th><th style="width:5%"></th></tr></thead>';
        echo '<tbody id="imtiyaz-paket-body">';
        foreach($paket_list as $p){ echo $this->paket_row_html($p); }
        echo '</tbody></table>';
        // Baris template tersembunyi, dipakai JS untuk membuat baris baru saat "Tambah Paket" diklik
        echo '<table style="display:none"><tbody>'.$this->paket_row_html(['id'=>'','nama'=>'','kategori'=>'','durasi'=>'','harga'=>'','fasilitas'=>'','badge'=>''], true).'</tbody></table>';
        echo '<p style="margin-top:10px"><button type="button" class="button" onclick="imtiyazAddRow(\'imtiyaz-paket-body\',\'imtiyaz-paket-template\')">+ Tambah Paket</button></p>';
        echo '<p class="submit"><button class="button button-primary">Simpan Paket Umrah</button></p>';
        echo '</form>';
    }
    private function paket_row_html($p, $is_template=false){
        ob_start(); ?>
        <tr<?php echo $is_template ? ' id="imtiyaz-paket-template"' : ''; ?>>
            <td><input type="text" name="paket_nama[]" value="<?php echo esc_attr($p['nama']); ?>" style="width:100%" placeholder="Contoh: Paket Slamet"></td>
            <td><input type="text" name="paket_kategori[]" value="<?php echo esc_attr($p['kategori']); ?>" style="width:100%" placeholder="Ekonomis"></td>
            <td><input type="text" name="paket_durasi[]" value="<?php echo esc_attr($p['durasi']); ?>" style="width:100%" placeholder="9 Hari"></td>
            <td><input type="text" name="paket_harga[]" value="<?php echo esc_attr($p['harga']); ?>" style="width:100%" placeholder="Rp 28,9 jt"></td>
            <td><textarea name="paket_fasilitas[]" rows="2" style="width:100%" placeholder="Hotel, penerbangan, dll"><?php echo esc_textarea($p['fasilitas']); ?></textarea></td>
            <td><input type="text" name="paket_badge[]" value="<?php echo esc_attr($p['badge']); ?>" style="width:100%" placeholder="(kosongkan jika tidak ada)"></td>
            <td><button type="button" class="button" onclick="imtiyazRemoveRow(this)" title="Hapus paket ini">🗑</button></td>
        </tr>
        <?php return ob_get_clean();
    }

    private function render_dokumen_tab(){
        if(isset($_POST['imtiyaz_dokumen_nonce']) && wp_verify_nonce($_POST['imtiyaz_dokumen_nonce'],'imtiyaz_save_dokumen')){
            $keys = $_POST['dokumen_key'] ?? [];
            $labels = $_POST['dokumen_label'] ?? [];
            $rows = []; $used_keys = [];
            for($i=0;$i<count($labels);$i++){
                $label = sanitize_text_field($labels[$i] ?? '');
                if($label==='') continue;
                $raw_key = sanitize_key($keys[$i] ?? '');
                $base_key = $raw_key !== '' ? $raw_key : sanitize_key($label);
                $key = $base_key; $suffix=2;
                while(isset($rows[$key]) || in_array($key,$used_keys,true)){ $key = $base_key.'-'.$suffix; $suffix++; }
                $used_keys[] = $key;
                $rows[$key] = $label;
            }
            update_option('imtiyaz_dokumen_list', $rows);
            echo '<div class="notice notice-success is-dismissible"><p>Daftar dokumen wajib tersimpan ('.count($rows).' item). Checklist di aplikasi Android otomatis mengikuti daftar ini.</p></div>';
        }
        $dokumen_list = $this->get_dokumen_list();
        echo '<form method="post">';
        wp_nonce_field('imtiyaz_save_dokumen','imtiyaz_dokumen_nonce');
        echo '<p>Atur dokumen apa saja yang wajib dicentang jamaah di aplikasi (tab "Dokumen"). Kode unik (key) dipakai sistem di belakang layar — biarkan kosong agar dibuat otomatis dari nama.</p>';
        echo '<table class="widefat striped"><thead><tr><th style="width:20%">Kode (opsional)</th><th>Nama Dokumen</th><th style="width:5%"></th></tr></thead>';
        echo '<tbody id="imtiyaz-dokumen-body">';
        foreach($dokumen_list as $key=>$label){ echo $this->dokumen_row_html($key,$label); }
        echo '</tbody></table>';
        echo '<table style="display:none"><tbody>'.$this->dokumen_row_html('','', true).'</tbody></table>';
        echo '<p style="margin-top:10px"><button type="button" class="button" onclick="imtiyazAddRow(\'imtiyaz-dokumen-body\',\'imtiyaz-dokumen-template\')">+ Tambah Dokumen</button></p>';
        echo '<p class="submit"><button class="button button-primary">Simpan Dokumen Wajib</button></p>';
        echo '</form>';
    }
    private function dokumen_row_html($key,$label,$is_template=false){
        ob_start(); ?>
        <tr<?php echo $is_template ? ' id="imtiyaz-dokumen-template"' : ''; ?>>
            <td><input type="text" name="dokumen_key[]" value="<?php echo esc_attr($key); ?>" style="width:100%" placeholder="mis: ktp"></td>
            <td><input type="text" name="dokumen_label[]" value="<?php echo esc_attr($label); ?>" style="width:100%" placeholder="mis: KTP"></td>
            <td><button type="button" class="button" onclick="imtiyazRemoveRow(this)" title="Hapus dokumen ini">🗑</button></td>
        </tr>
        <?php return ob_get_clean();
    }

    private function render_kontak_tab(){
        if(isset($_POST['imtiyaz_kontak_nonce']) && wp_verify_nonce($_POST['imtiyaz_kontak_nonce'],'imtiyaz_save_kontak')){
            update_option('imtiyaz_wa_admin', sanitize_text_field($_POST['wa_admin'] ?? '628112776543'));
            update_option('imtiyaz_kontak', [
                'nama_travel' => sanitize_text_field($_POST['nama_travel'] ?? ''),
                'alamat' => sanitize_text_field($_POST['alamat'] ?? ''),
                'kontak' => sanitize_text_field($_POST['kontak'] ?? ''),
            ]);
            echo '<div class="notice notice-success is-dismissible"><p>Kontak &amp; WA Admin tersimpan.</p></div>';
        }
        $wa = get_option('imtiyaz_wa_admin','628112776543');
        $k = $this->get_kontak();
        echo '<form method="post">';
        wp_nonce_field('imtiyaz_save_kontak','imtiyaz_kontak_nonce');
        echo '<table class="form-table">';
        echo '<tr><th>Nomor WA Admin</th><td><input type="text" name="wa_admin" value="'.esc_attr($wa).'" style="width:300px"><p class="description">Format: 628112776543 tanpa + — dipakai tombol "Daftar via WhatsApp" (Fitur 1)</p></td></tr>';
        echo '<tr><th>Nama Travel</th><td><input type="text" name="nama_travel" value="'.esc_attr($k['nama_travel']).'" style="width:400px"></td></tr>';
        echo '<tr><th>Alamat / Izin PPIU</th><td><input type="text" name="alamat" value="'.esc_attr($k['alamat']).'" style="width:400px"></td></tr>';
        echo '<tr><th>Kontak Ditampilkan</th><td><input type="text" name="kontak" value="'.esc_attr($k['kontak']).'" style="width:400px"></td></tr>';
        echo '</table>';
        echo '<p class="submit"><button class="button button-primary">Simpan Kontak &amp; WA</button></p>';
        echo '</form>';
        echo '<div style="background:#fff;padding:15px;border-radius:8px;margin-top:20px;border:1px solid #e5e7eb"><h3>Format Pesan WA Otomatis</h3><code>Assalamualaikum, saya mau daftar Paket [Nama] [Harga] - Nama: [Input] - HP: [Input]</code></div>';
    }

    public function add_metaboxes(){
        add_meta_box('imtiyaz_jamaah_detail','Status Pembayaran + Checklist + Bukti (Fitur 2,3)',[$this,'render_jamaah_box'],'jamaah','normal','high');
    }
    public function render_jamaah_box($post){
        $paket_list = $this->get_paket_list();
        $dokumen_list = $this->get_dokumen_list();
        $total = get_post_meta($post->ID,'_total_tagihan',true); $sudah = get_post_meta($post->ID,'_sudah_dibayar',true); $sisa = get_post_meta($post->ID,'_sisa_tagihan',true);
        $status = get_post_meta($post->ID,'_status_pembayaran',true); if(empty($status)) $status='Belum Lunas';
        $bukti = get_post_meta($post->ID,'_bukti_transfer',true); $paket_id = get_post_meta($post->ID,'_paket_id',true);
        $checklist = get_post_meta($post->ID,'_checklist_dokumen',true); if(!is_array($checklist)) $checklist=[];
        wp_nonce_field('imtiyaz_save','imtiyaz_nonce');
        echo '<div style="display:grid;grid-template-columns:1fr 1fr;gap:15px">';
        echo '<p><label>Paket ID</label><br><select name="paket_id" style="width:100%"><option value="">Pilih Paket</option>';
        foreach($paket_list as $p){ $sel = selected($paket_id,$p['id'],false); echo "<option value='".esc_attr($p['id'])."' $sel>".esc_html($p['nama'].' - '.$p['harga'])."</option>"; }
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
        echo '<hr><h3>Login Aplikasi (khusus jamaah ini)</h3>';
        $username = get_post_meta($post->ID,'_jamaah_username',true);
        $has_password = get_post_meta($post->ID,'_jamaah_password_hash',true) ? true : false;
        echo '<div style="display:grid;grid-template-columns:1fr 1fr;gap:15px">';
        echo '<p><label>Username Login</label><br><input type="text" name="jamaah_username" value="'.esc_attr($username).'" style="width:100%" placeholder="mis: nama.jamaah atau nomor HP"></p>';
        echo '<p><label>Password Login'.($has_password ? ' <span style="color:#16a34a">(sudah diset)</span>' : ' <span style="color:#dc2626">(belum diset)</span>').'</label><br><input type="text" name="jamaah_password" value="" style="width:100%" placeholder="Kosongkan jika tidak ingin mengubah"><p class="description">Diisi & diberikan langsung oleh Admin ke jamaah -- jamaah TIDAK bisa mendaftar/mengubah sendiri.</p></p>';
        echo '</div>';
        echo '<hr><h3>Checklist Dokumen ('.count($dokumen_list).' item — <a href="'.admin_url('admin.php?page=imtiyaz-settings&tab=dokumen').'">kelola daftar</a>)</h3><div style="display:grid;grid-template-columns:1fr 1fr 1fr;gap:10px">';
        foreach($dokumen_list as $key=>$label){ $checked = !empty($checklist[$key]) ? 'checked' : ''; echo "<label style='background:#f9fafb;padding:10px;border-radius:8px'><input type='checkbox' name='checklist[".esc_attr($key)."]' value='1' $checked> ".esc_html($label)."</label>"; }
        echo '</div>';
    }
    public function save_meta($post_id){
        if(!isset($_POST['imtiyaz_nonce']) || !wp_verify_nonce($_POST['imtiyaz_nonce'],'imtiyaz_save')) return;
        if(defined('DOING_AUTOSAVE') && DOING_AUTOSAVE) return;
        foreach(['total_tagihan','sudah_dibayar','sisa_tagihan','status_pembayaran','paket_id'] as $f){ if(isset($_POST[$f])) update_post_meta($post_id,'_'.$f,sanitize_text_field($_POST[$f])); }
        $total = intval($_POST['total_tagihan'] ?? 0); $sudah = intval($_POST['sudah_dibayar'] ?? 0); if($total>0) update_post_meta($post_id,'_sisa_tagihan',$total-$sudah);
        $dokumen_list = $this->get_dokumen_list();
        $checklist=[]; foreach($dokumen_list as $key=>$label){ $checklist[$key] = isset($_POST['checklist'][$key]); } update_post_meta($post_id,'_checklist_dokumen',$checklist);

        // FITUR LOGIN JAMAAH: hanya Admin (yang sudah punya akses edit_posts di sini) yang bisa
        // set/ubah username & password ini -- jamaah tidak pernah bisa mendaftar sendiri karena
        // form ini cuma ada di dashboard WP Admin, bukan di aplikasi Android.
        if(isset($_POST['jamaah_username'])){
            update_post_meta($post_id, '_jamaah_username', sanitize_user($_POST['jamaah_username'], true));
        }
        if(!empty($_POST['jamaah_password'])){
            // Password baru diisi -> hash & simpan, sekaligus reset token supaya sesi lama (kalau ada) tidak berlaku lagi.
            update_post_meta($post_id, '_jamaah_password_hash', wp_hash_password(sanitize_text_field($_POST['jamaah_password'])));
            delete_post_meta($post_id, '_jamaah_token');
        }
        // Kalau field password dikosongkan, password lama TIDAK diubah (pola sama seperti form user WP inti).
    }
    public function register_rest(){
        // Publik & aman untuk dibaca siapa saja (tidak mengandung data pribadi):
        register_rest_route('imtiyaz/v1','/paket',['methods'=>'GET','callback'=>[$this,'api_paket'],'permission_callback'=>'__return_true']);
        register_rest_route('imtiyaz/v1','/wa-admin',['methods'=>'GET','callback'=>[$this,'api_wa_admin'],'permission_callback'=>'__return_true']);
        register_rest_route('imtiyaz/v1','/kontak',['methods'=>'GET','callback'=>[$this,'api_kontak'],'permission_callback'=>'__return_true']);
        register_rest_route('imtiyaz/v1','/dokumen',['methods'=>'GET','callback'=>[$this,'api_dokumen'],'permission_callback'=>'__return_true']);
        register_rest_route('imtiyaz/v1','/login',['methods'=>'POST','callback'=>[$this,'api_login'],'permission_callback'=>'__return_true']);
        register_rest_route('imtiyaz/v1','/logout',['methods'=>'POST','callback'=>[$this,'api_logout'],'permission_callback'=>'__return_true']);

        // Wajib login (dipanggil oleh app.js via Basic Auth) karena mengandung/mengubah data pribadi jamaah:
        register_rest_route('imtiyaz/v1','/jamaah/(?P<id>\d+)',['methods'=>'GET','callback'=>[$this,'api_jamaah_detail'],'permission_callback'=>[$this,'require_auth']]);
        register_rest_route('imtiyaz/v1','/jamaah',['methods'=>'GET','callback'=>[$this,'api_jamaah_list'],'permission_callback'=>[$this,'require_auth']]);
        register_rest_route('imtiyaz/v1','/upload-bukti',['methods'=>'POST','callback'=>[$this,'api_upload_bukti'],'permission_callback'=>[$this,'require_auth']]);
        register_rest_route('imtiyaz/v1','/update-checklist',['methods'=>'POST','callback'=>[$this,'api_update_checklist'],'permission_callback'=>[$this,'require_auth']]);
        register_rest_route('imtiyaz/v1','/skrining',['methods'=>'POST','callback'=>[$this,'api_skrining'],'permission_callback'=>[$this,'require_auth']]);
    }
    public function api_paket(){ return rest_ensure_response($this->get_paket_list()); }
    public function api_dokumen(){ return rest_ensure_response($this->get_dokumen_list()); }
    public function api_kontak(){ return rest_ensure_response($this->get_kontak()); }
    public function api_wa_admin(){ return rest_ensure_response(['wa_admin'=>get_option('imtiyaz_wa_admin','628112776543'),'wa_link'=>'https://wa.me/'.get_option('imtiyaz_wa_admin','628112776543')]); }
    public function api_jamaah_detail($req){ $id=$req['id']; $post=get_post($id); if(!$post || $post->post_type!=='jamaah') return new WP_Error('not_found','Jamaah tidak ditemukan',['status'=>404]); return rest_ensure_response(['id'=>$post->ID,'nama'=>$post->post_title,'paket_id'=>get_post_meta($id,'_paket_id',true),'total_tagihan'=>get_post_meta($id,'_total_tagihan',true),'sudah_dibayar'=>get_post_meta($id,'_sudah_dibayar',true),'sisa_tagihan'=>get_post_meta($id,'_sisa_tagihan',true),'status_pembayaran'=>get_post_meta($id,'_status_pembayaran',true),'bukti_transfer'=>get_post_meta($id,'_bukti_transfer',true),'checklist_dokumen'=>get_post_meta($id,'_checklist_dokumen',true)]); }
    public function api_jamaah_list(){ $posts=get_posts(['post_type'=>'jamaah','numberposts'=>100]); $data=[]; foreach($posts as $p){ $data[]=['id'=>$p->ID,'nama'=>$p->post_title,'paket_id'=>get_post_meta($p->ID,'_paket_id',true),'status_pembayaran'=>get_post_meta($p->ID,'_status_pembayaran',true),'checklist'=>get_post_meta($p->ID,'_checklist_dokumen',true)]; } return rest_ensure_response($data); }
    public function api_upload_bukti($req){ $params=$req->get_json_params(); if(empty($params)) $params=$req->get_params(); $jamaah_id=intval($params['jamaah_id']??0); $bukti_url=esc_url_raw($params['bukti_url']??''); if(!$this->verify_jamaah_token($jamaah_id, $params['token']??'')) return new WP_Error('unauthorized','Token login jamaah tidak valid -- silakan login ulang',['status'=>401]); if($jamaah_id && $bukti_url){ update_post_meta($jamaah_id,'_bukti_transfer',$bukti_url); update_post_meta($jamaah_id,'_status_pembayaran','Menunggu Verifikasi'); return rest_ensure_response(['success'=>true,'message'=>'Bukti diterima - Menunggu Verifikasi']); } return new WP_Error('invalid','jamaah_id dan bukti_url wajib',['status'=>400]); }
    public function api_update_checklist($req){ $params=$req->get_json_params(); if(empty($params)) $params=$req->get_params(); $jamaah_id=intval($params['jamaah_id']??0); $checklist=$params['checklist']??[]; if(!$this->verify_jamaah_token($jamaah_id, $params['token']??'')) return new WP_Error('unauthorized','Token login jamaah tidak valid -- silakan login ulang',['status'=>401]); if($jamaah_id && is_array($checklist)){ update_post_meta($jamaah_id,'_checklist_dokumen',$checklist); return rest_ensure_response(['success'=>true,'checklist'=>$checklist,'message'=>'Checklist tersimpan']); } return new WP_Error('invalid','Data tidak lengkap',['status'=>400]); }
    public function api_skrining($req){ $params=$req->get_json_params(); if(empty($params)) $params=$req->get_params(); $jamaah_id=intval($params['jamaah_id']??0); if(!$this->verify_jamaah_token($jamaah_id, $params['token']??'')) return new WP_Error('unauthorized','Token login jamaah tidak valid -- silakan login ulang',['status'=>401]); $post_id=wp_insert_post(['post_type'=>'skrining_kesehatan','post_title'=>'Skrining - '.($params['nama_lengkap']??'Anonim').' - '.date('d-m-Y H:i'),'post_status'=>'publish']); update_post_meta($post_id,'_skrining_data',wp_json_encode($params,JSON_UNESCAPED_UNICODE)); update_post_meta($post_id,'_jamaah_id',$jamaah_id); return rest_ensure_response(['success'=>true,'id'=>$post_id,'message'=>'Skrining terkirim']); }
}
new Imtiyaz_Connector_Clear();
