package com.imtiyaztour.app

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.random.Random

// ============================================================================
// "Terbang ke Makkah" - game ringan (endless runner vertical).
// Pesawat terbang ke arah Makkah, hindari hujan meteor.
// Style: Alto's Odyssey (gradient ungu-pink-orange + silhouette + bintang).
// ============================================================================

private val SKY_TOP = Color(0xFF1E1B4B)
private val SKY_MID1 = Color(0xFF7E22CE)
private val SKY_MID2 = Color(0xFFEC4899)
private val SKY_BOTTOM = Color(0xFFFED7AA)
private val SILHOUETTE = Color(0xFF0F172A)
private val METEOR_RED = Color(0xFFDC2626)
private val METEOR_ORANGE = Color(0xFFF97316)
private val METEOR_YELLOW = Color(0xFFFCD34D)
private val SHIELD_COLOR = Color(0xFF06B6D4)
private val BRAND_GREEN = Color(0xFF0F7A5A)

private data class Meteor(var x: Float, var y: Float, val radius: Float)
private data class Star(val x: Float, val y: Float, val radius: Float, val alpha: Float)
private data class Mountain(val x: Float, val height: Float)
private data class Badge(val id: String, val emoji: String, val title: String, val minScore: Int)

private val BADGES = listOf(
    Badge("penerbang_muda", "🛫", "Penerbang Muda", 500),
    Badge("penerbang_handal", "✈️", "Penerbang Handal", 2000),
    Badge("penerbang_ulung", "🚀", "Penerbang Ulung", 5000),
    Badge("sampai_makkah", "🕋", "Sampai Makkah", 10000),
)

private enum class GameState { MENU, PLAYING, GAME_OVER }

private object TkmPrefs {
    private const val NAME = "terbang_ke_makkah"
    private fun get(ctx: Context) = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)
    fun getHighScore(ctx: Context): Int = get(ctx).getInt("highscore", 0)
    fun saveHighScore(ctx: Context, score: Int) { get(ctx).edit().putInt("highscore", score).apply() }
    fun getBadges(ctx: Context): Set<String> = get(ctx).getStringSet("badges", emptySet()) ?: emptySet()
    fun unlockBadge(ctx: Context, id: String) {
        get(ctx).edit().putStringSet("badges", getBadges(ctx) + id).apply()
    }
}

@Composable
fun TerbangKeMakkahScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var state by remember { mutableStateOf(GameState.MENU) }
    var score by remember { mutableIntStateOf(0) }
    var lives by remember { mutableIntStateOf(3) }
    var highScore by remember { mutableIntStateOf(TkmPrefs.getHighScore(context)) }
    var badges by remember { mutableStateOf(TkmPrefs.getBadges(context)) }
    var newBadge by remember { mutableStateOf<Badge?>(null) }

    // Game objects
    var planeX by remember { mutableFloatStateOf(0.5f) }
    var targetX by remember { mutableFloatStateOf(0.5f) }
    var shieldUntil by remember { mutableLongStateOf(0L) }
    var nextShieldSpawn by remember { mutableIntStateOf(500) }
    val meteors = remember { mutableStateListOf<Meteor>() }

    val stars = remember {
        List(45) {
            Star(
                x = Random.nextFloat(),
                y = Random.nextFloat() * 0.7f,
                radius = 0.8f + Random.nextFloat() * 1.8f,
                alpha = 0.3f + Random.nextFloat() * 0.6f
            )
        }
    }
    val mountains = remember {
        List(14) {
            Mountain(
                x = it / 13f,
                height = 0.3f + Random.nextFloat() * 0.7f
            )
        }
    }

    fun resetGame() {
        score = 0
        lives = 3
        planeX = 0.5f
        targetX = 0.5f
        shieldUntil = 0L
        nextShieldSpawn = 500
        meteors.clear()
    }

    fun endGame() {
        state = GameState.GAME_OVER
        if (score > highScore) {
            highScore = score
            TkmPrefs.saveHighScore(context, score)
        }
        // Cek badge baru
        BADGES.firstOrNull { score >= it.minScore && !badges.contains(it.id) }?.let { b ->
            TkmPrefs.unlockBadge(context, b.id)
            badges = TkmPrefs.getBadges(context)
            newBadge = b
        }
    }

    // Game loop
    LaunchedEffect(state) {
        if (state != GameState.PLAYING) return@LaunchedEffect
        var last = 0L
        while (state == GameState.PLAYING) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0f else (now - last) / 1_000_000_000f
                last = now
                if (dt <= 0f) return@withFrameNanos

                // Plane movement
                val maxSpeed = 1.4f
                val diff = targetX - planeX
                planeX += diff.coerceIn(-maxSpeed * dt, maxSpeed * dt)
                planeX = planeX.coerceIn(0.08f, 0.92f)

                // Meteors
                val meteorSpeed = 0.42f + (score / 25000f).coerceAtMost(0.6f)
                meteors.forEach { m -> m.y += meteorSpeed * dt }
                meteors.removeAll { it.y > 1.15f }

                // Spawn meteor
                val spawnRate = 0.018f + (score / 500000f).coerceAtMost(0.03f)
                if (Random.nextFloat() < spawnRate) {
                    meteors.add(Meteor(
                        x = 0.05f + Random.nextFloat() * 0.9f,
                        y = -0.08f,
                        radius = 0.035f + Random.nextFloat() * 0.025f
                    ))
                }

                // Shield spawn & pickup
                if (score >= nextShieldSpawn && shieldUntil < now / 1_000_000) {
                    // Auto-activate shield (simplified)
                    shieldUntil = now / 1_000_000 + 5000
                    nextShieldSpawn += 800 + Random.nextInt(400)
                }

                // Collision check
                val planeY = 0.75f
                val planeR = 0.035f
                if (shieldUntil < now / 1_000_000) {
                    val hit = meteors.any { m ->
                        abs(m.x - planeX) < (m.radius + planeR) &&
                        abs(m.y - planeY) < (m.radius + planeR)
                    }
                    if (hit) {
                        lives--
                        meteors.removeAll { m ->
                            abs(m.x - planeX) < 0.18f && abs(m.y - planeY) < 0.18f
                        }
                        if (lives <= 0) {
                            endGame()
                        }
                    }
                }

                // Score
                score += (dt * 110).toInt()
            }
        }
    }

    // UI
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        // GAME CANVAS — always visible
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(state) {
                    if (state != GameState.PLAYING) return@pointerInput
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.type == PointerEventType.Press ||
                                event.type == PointerEventType.Move) {
                                val x = event.changes.firstOrNull()?.position?.x
                                if (x != null) {
                                    targetX = (x / size.width).coerceIn(0.08f, 0.92f)
                                }
                            }
                        }
                    }
                }
        ) {
            val W = size.width
            val H = size.height

            // Sky gradient
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(SKY_TOP, SKY_MID1, SKY_MID2, SKY_BOTTOM),
                    startY = 0f, endY = H
                )
            )

            // Stars
            stars.forEach { s ->
                drawCircle(
                    color = Color.White.copy(alpha = s.alpha),
                    radius = s.radius,
                    center = Offset(s.x * W, s.y * H)
                )
            }

            // Moon + glow
            val moonCenter = Offset(W * 0.82f, H * 0.14f)
            drawCircle(Color.White.copy(alpha = 0.12f), 42.dp.toPx(), moonCenter)
            drawCircle(Color.White, 26.dp.toPx(), moonCenter)
            drawCircle(
                color = SKY_MID1.copy(alpha = 0.6f),
                8.dp.toPx(),
                Offset(moonCenter.x - 6.dp.toPx(), moonCenter.y - 4.dp.toPx())
            )

            // Silhouette mountains
            val horizonY = H * 0.85f
            val path = Path().apply {
                moveTo(0f, H)
                lineTo(0f, horizonY)
                mountains.forEach { m ->
                    lineTo(m.x * W, horizonY - m.height * H * 0.08f)
                }
                lineTo(W, horizonY)
                lineTo(W, H)
                close()
            }
            drawPath(path, SILHOUETTE)

            // Ka'bah silhouette
            val kw = 42.dp.toPx()
            val kh = 58.dp.toPx()
            val kx = W / 2f
            val ky = horizonY - kh + 8.dp.toPx()
            drawRect(SILHOUETTE, Offset(kx - kw / 2, ky), Size(kw, kh))
            drawRect(
                color = Color(0xFFD4AF37),
                topLeft = Offset(kx - kw / 2, ky + kh * 0.32f),
                size = Size(kw, 4.dp.toPx())
            )

            // Meteors with trails
            meteors.forEach { m ->
                val cx = m.x * W
                val cy = m.y * H
                val r = m.radius * W
                for (i in 1..5) {
                    drawCircle(
                        color = METEOR_ORANGE.copy(alpha = (0.5f - i * 0.09f).coerceAtLeast(0f)),
                        radius = r * (1f - i * 0.12f),
                        center = Offset(cx, cy - i * 7.dp.toPx())
                    )
                }
                drawCircle(METEOR_RED, r, Offset(cx, cy))
                drawCircle(METEOR_ORANGE, r * 0.72f, Offset(cx, cy))
                drawCircle(METEOR_YELLOW, r * 0.32f, Offset(cx, cy))
            }

            // Plane + shield
            val pcx = planeX * W
            val pcy = 0.75f * H
            val psize = 24.dp.toPx()

            val shieldActive = shieldUntil > System.currentTimeMillis()
            if (shieldActive) {
                drawCircle(
                    color = SHIELD_COLOR.copy(alpha = 0.35f),
                    radius = psize * 1.7f,
                    center = Offset(pcx, pcy)
                )
                drawCircle(
                    color = SHIELD_COLOR.copy(alpha = 0.85f),
                    radius = psize * 1.7f,
                    center = Offset(pcx, pcy),
                    style = Stroke(width = 2.5.dp.toPx())
                )
            }

            // Plane trail
            drawCircle(Color.White.copy(alpha = 0.18f), 5.dp.toPx(), Offset(pcx, pcy + psize * 0.9f))
            drawCircle(Color.White.copy(alpha = 0.3f), 3.5.dp.toPx(), Offset(pcx, pcy + psize * 0.6f))

            // Plane body
            val planePath = Path().apply {
                moveTo(pcx, pcy - psize)
                lineTo(pcx - psize * 0.75f, pcy + psize * 0.55f)
                lineTo(pcx, pcy + psize * 0.25f)
                lineTo(pcx + psize * 0.75f, pcy + psize * 0.55f)
                close()
            }
            drawPath(planePath, Color.White)

            // Cockpit
            drawCircle(BRAND_GREEN, psize * 0.22f, Offset(pcx, pcy - psize * 0.15f))
        }

        // ============ OVERLAYS ============
        when (state) {
            GameState.MENU -> MenuOverlay(
                highScore = highScore,
                badges = badges,
                onStart = { resetGame(); state = GameState.PLAYING },
                onBack = onBack
            )
            GameState.PLAYING -> {
                // HUD
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "❤".repeat(lives.coerceAtLeast(0)),
                        fontSize = 20.sp,
                        color = Color.White
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$score m", color = Color.White,
                            fontSize = 26.sp, fontWeight = FontWeight.Bold)
                        Text("BEST: $highScore", color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp)
                    }
                    if (shieldUntil > System.currentTimeMillis()) {
                        Text("🛡️", fontSize = 20.sp)
                    } else {
                        Spacer(Modifier.width(30.dp))
                    }
                }
                // Bottom hint
                Text(
                    "Tap kiri / kanan layar untuk mengarahkan pesawat",
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 10.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 20.dp)
                )
            }
            GameState.GAME_OVER -> GameOverOverlay(
                score = score,
                highScore = highScore,
                newBadge = newBadge,
                onRetry = { resetGame(); newBadge = null; state = GameState.PLAYING },
                onMenu = { newBadge = null; state = GameState.MENU },
                onBack = onBack
            )
        }
    }
}

@Composable
private fun MenuOverlay(
    highScore: Int,
    badges: Set<String>,
    onStart: () -> Unit,
    onBack: () -> Unit
) {
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("✈️", fontSize = 64.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                "TERBANG KE MAKKAH",
                color = Color.White, fontWeight = FontWeight.Bold,
                fontSize = 26.sp, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Kemudikan pesawat agar terhindar dari hujan meteor!",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 13.sp, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🏆 BEST SCORE", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                    Text("$highScore m", color = Color.White,
                        fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (badges.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text("Badge", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BADGES.filter { badges.contains(it.id) }.forEach { b ->
                        Text(b.emoji, fontSize = 24.sp)
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BRAND_GREEN),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(6.dp))
                Text("MULAI TERBANG", fontWeight = FontWeight.Bold,
                    color = Color.White, fontSize = 15.sp)
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(6.dp))
                Text("Kembali", color = Color.White)
            }
        }
    }
}

@Composable
private fun GameOverOverlay(
    score: Int,
    highScore: Int,
    newBadge: Badge?,
    onRetry: () -> Unit,
    onMenu: () -> Unit,
    onBack: () -> Unit
) {
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("💥", fontSize = 56.sp)
            Spacer(Modifier.height(12.dp))
            Text("PESAWAT JATUH", color = Color.White,
                fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Spacer(Modifier.height(16.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("SKOR ANDA", color = Color.White.copy(alpha = 0.75f), fontSize = 11.sp)
                    Text("$score m", color = Color.White,
                        fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text("Best: $highScore m", color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp)
                }
            }

            if (newBadge != null) {
                Spacer(Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFD700).copy(alpha = 0.9f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(newBadge.emoji, fontSize = 32.sp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("BADGE BARU!", color = Color.Black.copy(alpha = 0.6f), fontSize = 10.sp)
                            Text(newBadge.title, color = Color.Black,
                                fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BRAND_GREEN),
                shape = RoundedCornerShape(12.dp)
            ) { Text("COBA LAGI", fontWeight = FontWeight.Bold, color = Color.White) }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onMenu,
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) { Text("Menu", color = Color.White) }
            Spacer(Modifier.height(6.dp))
            TextButton(onClick = onBack) {
                Text("Kembali ke Tab", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            }
        }
    }
}
