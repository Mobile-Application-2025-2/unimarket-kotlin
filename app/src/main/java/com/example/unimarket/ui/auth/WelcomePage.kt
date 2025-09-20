package com.example.unimarket.ui.welcome
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.unimarket.R

private val Accent = Color(0xFFFFC436)

@Composable
fun WelcomeScreen(
    onSignUp: () -> Unit = {},
    onLogin: () -> Unit = {}
) {
    Scaffold(containerColor = Color.White) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 2.dp)
        ) {
            Spacer(Modifier.height(24.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Image(
                            painter = painterResource(R.drawable.personajesingup),
                    contentDescription = null,
                    modifier = Modifier
                        .height(88.dp)
                        .wrapContentWidth(),
                    contentScale = ContentScale.Fit
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "UNIMARKET",
                    color = Accent,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(Modifier.weight(1f))
            Button(
                onClick = onSignUp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Accent,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Text("SIGN UP", fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            }

            Spacer(Modifier.height(12.dp))
            val annotated = buildAnnotatedString {
                withStyle(SpanStyle(color = Color(0xFFB0B4BF))) {
                    append("ALREADY HAVE AN ACCOUNT? ")
                }
                pushStringAnnotation(tag = "login", annotation = "login")
                withStyle(SpanStyle(color = Accent, fontWeight = FontWeight.SemiBold)) {
                    append("LOG IN")
                }
                pop()
            }
            ClickableText(
                text = annotated,
                style = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
            ) { offset ->
                annotated.getStringAnnotations("login", offset, offset).firstOrNull()
                    ?.let { onLogin() }
            }
        }
    }
}

@Preview(showSystemUi = true, showBackground = true, device = "id:pixel_6")
@Composable
private fun PreviewWelcome() {
    WelcomeScreen()
}