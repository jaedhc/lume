package com.example.lume.ui.screens.accounts

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Shader
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lume.ui.theme.ActiveGold
import com.example.lume.ui.theme.SurfaceDark
import com.example.lume.ui.theme.TextGray
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomColorPickerScreen(
    initialColorHex: String,
    onColorSelected: (String) -> Unit,
    onBack: () -> Unit
) {
    // Parse initial color
    val initialColor = try {
        Color(android.graphics.Color.parseColor(initialColorHex))
    } catch (e: Exception) {
        Color(0xFF7B61FF) // Default from image
    }

    val hsv = remember {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(initialColor.toArgb(), hsv)
        hsv
    }

    var hue by remember { mutableStateOf(hsv[0]) }
    var saturation by remember { mutableStateOf(hsv[1]) }
    var value by remember { mutableStateOf(hsv[2]) }

    val currentColor = remember(hue, saturation, value) {
        Color.hsv(hue, saturation, value)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            "COLOR PERSONALIZADO",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    TextButton(onClick = {
                        hue = hsv[0]
                        saturation = hsv[1]
                        value = hsv[2]
                    }) {
                        Text("RESET", color = ActiveGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black),
                windowInsets = WindowInsets(0)
            )
        },
        containerColor = Color.Black
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {

            // Preview Section
            Box(contentAlignment = Alignment.Center) {
                // Glow effect
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .shadow(elevation = 20.dp, shape = CircleShape, ambientColor = currentColor, spotColor = currentColor)
                        .background(currentColor.copy(alpha = 0.3f), CircleShape)
                )
                
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(currentColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Savings, // Piggy bank icon
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.White
                    )
                }
                
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 10.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White
                ) {
                    Text(
                        "VISTA PREVIA",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        color = Color.Black,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Saturation-Value Selector
            SaturationValuePanel(
                hue = hue,
                saturation = saturation,
                value = value,
                onValueChange = { s, v ->
                    saturation = s
                    value = v
                }
            )

            // Hue Slider
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("TONALIDAD", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                HueSlider(hue = hue, onHueChange = { hue = it })
            }

            // HEX & RGB Inputs
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.weight(0.4f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("HEX", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .background(SurfaceDark, RoundedCornerShape(12.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "#${Integer.toHexString(currentColor.toArgb()).substring(2).uppercase()}",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                Column(modifier = Modifier.weight(0.6f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("RGB", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .background(SurfaceDark, RoundedCornerShape(12.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        RGBValue((currentColor.red * 255).roundToInt())
                        Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.White.copy(alpha = 0.2f)))
                        RGBValue((currentColor.green * 255).roundToInt())
                        Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.White.copy(alpha = 0.2f)))
                        RGBValue((currentColor.blue * 255).roundToInt())
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Apply Button
            Button(
                onClick = {
                    val hex = "#${Integer.toHexString(currentColor.toArgb()).substring(2).uppercase()}"
                    onColorSelected(hex)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = currentColor)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Aplicar Color", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
fun RGBValue(value: Int) {
    Text(value.toString(), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
}

@Composable
fun SaturationValuePanel(
    hue: Float,
    saturation: Float,
    value: Float,
    onValueChange: (Float, Float) -> Unit
) {
    val density = LocalDensity.current
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.1f)
            .clip(RoundedCornerShape(24.dp))
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val s = (offset.x / size.width).coerceIn(0f, 1f)
                    val v = 1f - (offset.y / size.height).coerceIn(0f, 1f)
                    onValueChange(s, v)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val s = (change.position.x / size.width).coerceIn(0f, 1f)
                    val v = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                    onValueChange(s, v)
                }
            }
    ) {
        // We use a custom canvas to draw the SV gradient
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            
            // Hue background
            drawRect(color = Color.hsv(hue, 1f, 1f))
            
            // Saturation gradient (white to transparent)
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.White, Color.Transparent)
                )
            )
            
            // Value gradient (transparent to black)
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black)
                )
            )
            
            // Selector Circle
            val selectorX = saturation * width
            val selectorY = (1f - value) * height
            
            drawCircle(
                color = Color.White,
                radius = 12.dp.toPx(),
                center = Offset(selectorX, selectorY),
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}

@Composable
fun HueSlider(hue: Float, onHueChange: (Float) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .clip(RoundedCornerShape(12.dp))
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val h = (offset.x / size.width).coerceIn(0f, 1f) * 360f
                    onHueChange(h)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val h = (change.position.x / size.width).coerceIn(0f, 1f) * 360f
                    onHueChange(h)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val hueColors = (0..360).map { Color.hsv(it.toFloat(), 1f, 1f) }
            drawRect(brush = Brush.horizontalGradient(hueColors))
            
            // Slider thumb
            val thumbX = (hue / 360f) * size.width
            drawCircle(
                color = Color.White,
                radius = 10.dp.toPx(),
                center = Offset(thumbX, size.height / 2f)
            )
            drawCircle(
                color = Color.hsv(hue, 1f, 1f),
                radius = 6.dp.toPx(),
                center = Offset(thumbX, size.height / 2f),
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}
