package com.example.unimarket.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.unimarket.R
private val Accent = Color(0xFFFFC436)
private val LightGray = Color(0xFFF2F3F7)
private val GoogleStroke = Color(0xFFEBEAEC)
private val OutlookBlue = Color(0xFF0082C6)

@Composable
fun CreateAccountScreen() {
    // estados de los campos (solo UI para que se vea bien)
    var name by remember { mutableStateOf("Camilo Martinez") }
    var email by remember { mutableStateOf("camimartinez@gmail.com") }
    var password by remember { mutableStateOf("••••••••") }
    var showPassword by remember { mutableStateOf(false) }
    var accept by remember { mutableStateOf(true) }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Formas de fondo
        Image(
            painter = painterResource(R.drawable.formas),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .height(260.dp),
            alpha = 0.25f,
            contentScale = ContentScale.Crop
        )

        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(20.dp))

            // Botón volver (antes era un TextField blanco)
            Surface(
                shape = CircleShape,
                color = Color.White,
                tonalElevation = 2.dp,
                shadowElevation = 2.dp
            ) {
                IconButton(onClick = { /* no-op */ }) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                }
            }

            Spacer(Modifier.height(20.dp))

            Text(
                "Create your account",
                color = Accent,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 4.8.em
                )
            )

            Spacer(Modifier.height(18.dp))

            // Botón Outlook
            Button(
                onClick = { /* no-op */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(63.dp),
                shape = RoundedCornerShape(38.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OutlookBlue,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(R.drawable.outlook),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("CONTINUE WITH OUTLOOK", fontWeight = FontWeight.Medium)
                }
            }

            Spacer(Modifier.height(12.dp))

            // Botón Google (outline)
            OutlinedButton(
                onClick = { /* no-op */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(63.dp),
                shape = RoundedCornerShape(38.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(GoogleStroke)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF3F414E))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(R.drawable.google),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("CONTINUE WITH GOOGLE", fontWeight = FontWeight.Medium)
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                "OR LOG IN WITH EMAIL",
                color = Color(0xFFA1A4B2),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium.copy(letterSpacing = 0.3.sp)
            )

            Spacer(Modifier.height(12.dp))

            // Nombre
            TextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(63.dp),
                shape = RoundedCornerShape(15.dp),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = LightGray,
                    unfocusedContainerColor = LightGray,
                    disabledContainerColor = LightGray,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = Color.Black
                ),
                trailingIcon = {
                    Icon(
                        Icons.Outlined.Check,
                        contentDescription = null,
                        tint = Accent
                    )
                }
            )

            Spacer(Modifier.height(10.dp))

            // Email
            TextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(63.dp),
                shape = RoundedCornerShape(15.dp),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = LightGray,
                    unfocusedContainerColor = LightGray,
                    disabledContainerColor = LightGray,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = Color.Black
                ),
                trailingIcon = {
                    Icon(
                        Icons.Outlined.Check,
                        contentDescription = null,
                        tint = Accent
                    )
                }
            )

            Spacer(Modifier.height(10.dp))

            // Password
            TextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(63.dp),
                shape = RoundedCornerShape(15.dp),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = LightGray,
                    unfocusedContainerColor = LightGray,
                    disabledContainerColor = LightGray,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = Color.Black
                ),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            if (showPassword) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                            contentDescription = null,
                            tint = Color(0xFFB1B3BD)
                        )
                    }
                },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation()
            )

            Spacer(Modifier.height(12.dp))

            // Términos
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                val text = buildAnnotatedString {
                    withStyle(SpanStyle(color = Color(0xFFA1A4B2))) { append("I have read the") }
                    append(" ")
                    withStyle(SpanStyle(color = Accent, fontWeight = FontWeight.Medium)) { append("Privacy Policy") }
                }
                Text(text, modifier = Modifier.weight(1f))
                Checkbox(
                    checked = accept,
                    onCheckedChange = { accept = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Accent,
                        uncheckedColor = Accent
                    )
                )
            }

            Spacer(Modifier.height(16.dp))

            // Sign in
            Button(
                onClick = { /* no-op */ },
                enabled = accept,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(38.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Accent,
                    contentColor = Color.White,
                    disabledContainerColor = Accent.copy(alpha = 0.5f),
                    disabledContentColor = Color.White
                )
            ) {
                Text("SIGN IN", fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, device = "id:pixel_6")
@Composable
private fun PreviewCreateAccount() {
    CreateAccountScreen()
}