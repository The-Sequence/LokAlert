package com.mobprog.lokalert

import android.media.MediaPlayer
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun AboutUsScreen(onDismiss: () -> Unit = {}, isEmbedded: Boolean = false) {
    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp > 600
    
    // Easter egg state
    var tapCount by remember { mutableIntStateOf(0) }
    var showEasterEgg by remember { mutableStateOf(false) }
    var lastTapTime by remember { mutableLongStateOf(0L) }
    
    // Handle tap on icon for easter egg
    val onIconTap = {
        val currentTime = System.currentTimeMillis()
        // Reset tap count if more than 1 second between taps
        if (currentTime - lastTapTime > 1000) {
            tapCount = 1
        } else {
            tapCount++
        }
        lastTapTime = currentTime
        
        if (tapCount >= 10) {
            showEasterEgg = true
            tapCount = 0
        }
    }
    
    // Easter Egg Dialog
    if (showEasterEgg) {
        FlappyAlertGame(onDismiss = { showEasterEgg = false })
    }
    
    val content = @Composable {
        if (isWideScreen || isEmbedded) {
            // Wide screen layout (side-by-side)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left side: App icon and title
                Column(
                    modifier = Modifier.weight(0.35f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = "LokAlert Icon",
                        modifier = Modifier
                            .size(if (isEmbedded) 120.dp else 100.dp)
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { onIconTap() }
                    )
                    Text(
                        "LokAlert",
                        fontSize = if (isEmbedded) 22.sp else 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                )
                
                // Right side: Message and names
                Column(
                    modifier = Modifier.weight(0.65f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "LokAlert was crafted with care and dedication by the following people. We hope that it serves you well.",
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("Developed by:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val names = listOf(
                        "Adamos, Eurika",
                        "Alemaña, Onyx Herod",
                        "Billones, Gerald",
                        "Crisologo, Terence Joefrey",
                        "Mabahin, Ryan",
                        "Royo, Aenard Ollyer"
                    )
                    
                    names.forEach { name ->
                        Text(name, fontSize = 12.sp, modifier = Modifier.padding(vertical = 2.dp))
                    }
                }
            }
        } else {
            // Narrow screen layout (stacked)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "LokAlert Icon",
                    modifier = Modifier
                        .size(80.dp)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onIconTap() }
                )
                Text(
                    "LokAlert",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                
                Text(
                    "LokAlert was crafted with care and dedication by the following people. We hope that it serves you well.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text("Developed by:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                
                val names = listOf(
                    "Adamos, Eurika",
                    "Alemaña, Onyx Herod",
                    "Billones, Gerald",
                    "Crisologo, Terence Joefrey",
                    "Mabahin, Ryan",
                    "Royo, Aenard Ollyer"
                )
                
                names.forEach { name ->
                    Text(name, fontSize = 11.sp, modifier = Modifier.padding(vertical = 2.dp))
                }
            }
        }
    }
    
    if (isEmbedded) {
        // Embedded in two-pane layout - no dialog
        Column {
            Text("About LokAlert", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))
            content()
        }
    } else {
        // Dialog mode
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("About LokAlert")
                }
            },
            text = { content() },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        )
    }
}

// ============================================================================
// FLAPPY ALERT - Easter Egg Mini Game
// ============================================================================

data class Pipe(
    var x: Float,
    val gapY: Float,
    val gapSize: Float = 350f,  // Larger gap - easier to fly through
    var passed: Boolean = false
)

@Composable
fun FlappyAlertGame(onDismiss: () -> Unit) {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    val screenWidth = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeight = with(density) { configuration.screenHeightDp.dp.toPx() }
    
    // Sound effects
    val jumpSound = remember { MediaPlayer.create(context, R.raw.game_jump) }
    val scoreSound = remember { MediaPlayer.create(context, R.raw.game_score) }
    val hitSound = remember { MediaPlayer.create(context, R.raw.game_hit) }
    
    // Clean up sound players when composable is disposed
    DisposableEffect(Unit) {
        onDispose {
            jumpSound?.release()
            scoreSound?.release()
            hitSound?.release()
        }
    }
    
    // Helper function to play sounds
    fun playSound(player: MediaPlayer?) {
        try {
            player?.let {
                if (it.isPlaying) {
                    it.seekTo(0)
                } else {
                    it.start()
                }
            }
        } catch (e: Exception) {
            // Ignore sound errors
        }
    }
    
    // Game state
    var isPlaying by remember { mutableStateOf(false) }
    var isGameOver by remember { mutableStateOf(false) }
    var score by remember { mutableIntStateOf(0) }
    var highScore by remember { mutableIntStateOf(0) }
    var lastScore by remember { mutableIntStateOf(-1) }
    
    // Bird physics - EASY MODE
    var birdY by remember { mutableFloatStateOf(screenHeight / 2) }
    var birdVelocity by remember { mutableFloatStateOf(0f) }
    val birdX = screenWidth * 0.2f
    val birdSize = 30f
    val gravity = 2.0f  // Reduced - bird falls slower
    val jumpVelocity = -20f  // Stronger jump
    
    // Pipes - EASY MODE
    var pipes by remember { mutableStateOf(listOf<Pipe>()) }
    val pipeWidth = 80f
    val pipeSpeed = 6f  // Slower pipes
    val pipeSpacing = 1000f  // More space between pipes
    
    // Animation
    var frameCount by remember { mutableLongStateOf(0L) }
    
    // Bird rotation based on velocity
    val birdRotation = (birdVelocity * 3).coerceIn(-30f, 90f)
    
    // Pulsing animation for start screen
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    
    // Game loop
    LaunchedEffect(isPlaying, isGameOver) {
        if (isPlaying && !isGameOver) {
            while (isPlaying && !isGameOver) {
                delay(16) // ~60 FPS
                frameCount++
                
                // Apply gravity
                birdVelocity += gravity
                birdY += birdVelocity
                
                // Move pipes
                pipes = pipes.map { pipe ->
                    pipe.copy(x = pipe.x - pipeSpeed)
                }.filter { it.x > -pipeWidth }
                
                // Spawn new pipes
                if (pipes.isEmpty() || pipes.last().x < screenWidth - pipeSpacing) {
                    val gapY = Random.nextFloat() * (screenHeight - 400) + 150
                    pipes = pipes + Pipe(x = screenWidth, gapY = gapY)
                }
                
                // Check scoring
                pipes.forEach { pipe ->
                    if (!pipe.passed && pipe.x + pipeWidth < birdX) {
                        pipe.passed = true
                        score++
                        // Play score sound when passing a pipe
                        if (score != lastScore) {
                            lastScore = score
                            playSound(scoreSound)
                        }
                    }
                }
                
                // Check collisions
                val birdTop = birdY - birdSize / 2
                val birdBottom = birdY + birdSize / 2
                val birdLeft = birdX - birdSize / 2
                val birdRight = birdX + birdSize / 2
                
                // Ground and ceiling collision
                if (birdTop < 0 || birdBottom > screenHeight) {
                    isGameOver = true
                    if (score > highScore) highScore = score
                    playSound(hitSound) // Play hit sound on collision
                }
                
                // Pipe collision
                for (pipe in pipes) {
                    val pipeLeft = pipe.x
                    val pipeRight = pipe.x + pipeWidth
                    val gapTop = pipe.gapY - pipe.gapSize / 2
                    val gapBottom = pipe.gapY + pipe.gapSize / 2
                    
                    if (birdRight > pipeLeft && birdLeft < pipeRight) {
                        if (birdTop < gapTop || birdBottom > gapBottom) {
                            isGameOver = true
                            if (score > highScore) highScore = score
                            playSound(hitSound) // Play hit sound on collision
                            break
                        }
                    }
                }
            }
        }
    }
    
    fun jump() {
        if (!isPlaying) {
            isPlaying = true
            isGameOver = false
            score = 0
            birdY = screenHeight / 2
            birdVelocity = 0f
            pipes = emptyList()
        } else if (!isGameOver) {
            birdVelocity = jumpVelocity
            playSound(jumpSound) // Play jump sound when bird flaps
        }
    }
    
    fun restart() {
        isPlaying = false
        isGameOver = false
        score = 0
        birdY = screenHeight / 2
        birdVelocity = 0f
        pipes = emptyList()
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF87CEEB), // Sky blue
                            Color(0xFF98D8C8)  // Mint green
                        )
                    )
                )
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { jump() }
        ) {
            // Draw game canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Draw clouds
                drawCloud(size.width * 0.2f, size.height * 0.15f, 60f)
                drawCloud(size.width * 0.6f, size.height * 0.1f, 80f)
                drawCloud(size.width * 0.85f, size.height * 0.2f, 50f)
                
                // Draw ground
                drawRect(
                    color = Color(0xFF8B4513), // Brown
                    topLeft = Offset(0f, size.height - 50f),
                    size = Size(size.width, 50f)
                )
                drawRect(
                    color = Color(0xFF228B22), // Forest green
                    topLeft = Offset(0f, size.height - 60f),
                    size = Size(size.width, 15f)
                )
                
                // Draw pipes
                pipes.forEach { pipe ->
                    val gapTop = pipe.gapY - pipe.gapSize / 2
                    val gapBottom = pipe.gapY + pipe.gapSize / 2
                    
                    // Top pipe
                    drawPipe(pipe.x, 0f, pipeWidth, gapTop)
                    
                    // Bottom pipe
                    drawPipe(pipe.x, gapBottom, pipeWidth, size.height - gapBottom - 50f)
                }
            }
            
            // Draw bird (LokAlert icon representation)
            Box(
                modifier = Modifier
                    .offset(
                        x = with(density) { (birdX - birdSize / 2).toDp() },
                        y = with(density) { (birdY - birdSize / 2).toDp() }
                    )
                    .size(with(density) { birdSize.toDp() })
                    .rotate(birdRotation)
                    .scale(if (!isPlaying) pulseScale else 1f)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF4FC3F7), // Light blue
                                Color(0xFF0288D1)  // Dark blue
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🔔",
                    fontSize = 24.sp
                )
            }
            
            // Score display
            if (isPlaying) {
                Text(
                    text = score.toString(),
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 60.dp)
                )
            }
            
            // Start screen
            if (!isPlaying) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🎮 FLAPPY ALERT 🔔",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Tap anywhere to fly!",
                        fontSize = 18.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    if (highScore > 0) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White.copy(alpha = 0.2f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "🏆 High Score: $highScore",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
            
            // Game over screen
            if (isGameOver) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.padding(32.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Game Over!",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF333333)
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                text = "Score: $score",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0288D1)
                            )
                            
                            if (score >= highScore && score > 0) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "🎉 New High Score! 🎉",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFFF9800)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = { restart(); jump() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF4CAF50)
                                    )
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Retry")
                                }
                                
                                OutlinedButton(
                                    onClick = onDismiss
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Exit")
                                }
                            }
                        }
                    }
                }
            }
            
            // Close button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

// Helper function to draw clouds
private fun DrawScope.drawCloud(x: Float, y: Float, size: Float) {
    val color = Color.White.copy(alpha = 0.8f)
    drawCircle(color, size * 0.6f, Offset(x, y))
    drawCircle(color, size * 0.5f, Offset(x - size * 0.4f, y + size * 0.1f))
    drawCircle(color, size * 0.5f, Offset(x + size * 0.4f, y + size * 0.1f))
    drawCircle(color, size * 0.4f, Offset(x - size * 0.6f, y + size * 0.2f))
    drawCircle(color, size * 0.4f, Offset(x + size * 0.6f, y + size * 0.2f))
}

// Helper function to draw pipes
private fun DrawScope.drawPipe(x: Float, y: Float, width: Float, height: Float) {
    // Pipe body
    drawRect(
        color = Color(0xFF2E7D32), // Green
        topLeft = Offset(x, y),
        size = Size(width, height)
    )
    
    // Pipe highlight
    drawRect(
        color = Color(0xFF4CAF50), // Lighter green
        topLeft = Offset(x + 5f, y),
        size = Size(10f, height)
    )
    
    // Pipe shadow
    drawRect(
        color = Color(0xFF1B5E20), // Darker green
        topLeft = Offset(x + width - 10f, y),
        size = Size(10f, height)
    )
    
    // Pipe cap
    val capHeight = 30f
    val capOverhang = 10f
    if (y == 0f) {
        // Top pipe cap (at bottom of top pipe)
        drawRect(
            color = Color(0xFF2E7D32),
            topLeft = Offset(x - capOverhang, y + height - capHeight),
            size = Size(width + capOverhang * 2, capHeight)
        )
    } else {
        // Bottom pipe cap (at top of bottom pipe)
        drawRect(
            color = Color(0xFF2E7D32),
            topLeft = Offset(x - capOverhang, y),
            size = Size(width + capOverhang * 2, capHeight)
        )
    }
}