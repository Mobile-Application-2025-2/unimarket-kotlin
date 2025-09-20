package com.example.unimarket.ui.onboarding

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.unimarket.R

private val Accent = Color(0xFFF7B500)        // Amarillo marca
private val TextOnAccent = Color(0xFFFFFFFF)  // Blanco sobre amarillo

@Composable
fun VerifyIdScreen(
    userName: String = "Camilo",
    @DrawableRes illustrationRes: Int? = null // p.ej. R.drawable.id_onboarding
) {
    Scaffold(containerColor = Color.White) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
        ) {
            // CABECERA AMARILLA CON ONDA + contenido
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp)
            ) {
                WaveHeaderBackground(modifier = Modifier.fillMaxSize())

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Logo + nombre
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = TextOnAccent.copy(alpha = 0.18f),
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Outlined.WbSunny,
                                contentDescription = null,
                                tint = TextOnAccent,
                                modifier = Modifier.padding(3.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "UNIMARKET",
                            color = TextOnAccent,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = "Hi $userName!",
                        color = TextOnAccent,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = "Before starting, we will need your student ID\nor identity ID for business outside college",
                        color = TextOnAccent.copy(alpha = 0.90f),
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(Modifier.height(18.dp))

                    // Campo de ID (solo UI, sin lógica por ahora)
                    TextField(
                        value = "",
                        onValueChange = {},
                        placeholder = { Text("ID") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        readOnly = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            disabledContainerColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = Accent
                        )
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            IllustrationCard(
                illustrationRes = illustrationRes,
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()
                    .height(260.dp)
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { /* no-op */ },
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Accent,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Text("GET STARTED", fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun WaveHeaderBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.background(Accent)) {
        val w = size.width
        val h = size.height

        val wave = Path().apply {
            moveTo(0f, h * 0.72f)
            cubicTo(
                w * 0.20f, h * 0.78f,
                w * 0.38f, h * 0.60f,
                w * 0.55f, h * 0.76f
            )
            cubicTo(
                w * 0.72f, h * 0.92f,
                w * 0.88f, h * 0.86f,
                w,        h * 0.90f
            )
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(path = wave, color = Color.White, style = Fill)

        val stroke = Path().apply {
            moveTo(0f, h * 0.72f)
            cubicTo(
                w * 0.20f, h * 0.78f,
                w * 0.38f, h * 0.60f,
                w * 0.55f, h * 0.76f
            )
            cubicTo(
                w * 0.72f, h * 0.92f,
                w * 0.88f, h * 0.86f,
                w,        h * 0.90f
            )
        }
        drawPath(path = stroke, color = Accent.copy(alpha = 0.25f), style = Stroke(width = 2f))

        // “Sol” suave
        drawCircle(
            color = Color.White.copy(alpha = 0.06f),
            radius = h * 0.22f,
            center = Offset(x = w * 0.18f, y = h * 0.15f)
        )
    }
}
@Composable
private fun IllustrationCard(
    @DrawableRes illustrationRes: Int?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFFFFBF2),
        border = BorderStroke(2.dp, Accent),
        tonalElevation = 0.dp,
        shadowElevation = 4.dp
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (illustrationRes != null) {
                Image(
                    painter = painterResource(id = illustrationRes),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth(0.82f)
                        .fillMaxHeight(0.9f)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit
                )
            } else {
                Surface(
                    shape = CircleShape,
                    color = Accent.copy(alpha = 0.12f),
                    modifier = Modifier.size(300.dp)
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        val resId = illustrationRes ?: R.drawable.personajeid
                        Image(
                            painter = painterResource(id = resId),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth(1f)
                                .fillMaxHeight(1f)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        }
    }
}

@Preview(showSystemUi = true, showBackground = true, device = "id:pixel_6")
@Composable
private fun PreviewVerifyId() {
    VerifyIdScreen()
}