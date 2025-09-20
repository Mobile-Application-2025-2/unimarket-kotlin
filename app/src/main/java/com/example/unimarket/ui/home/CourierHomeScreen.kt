package com.example.unimarket.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.unimarket.R
import com.example.unimarket.ui.scaffold.AppScaffold
import com.example.unimarket.ui.scaffold.BottomDest

private val Accent = Color(0xFFF7B500)
private val CardBg = Color(0xFFFDFCFB)

@Composable
fun CourierHomeScreen() {
    AppScaffold(current = BottomDest.Home, onNavigate = { }) { inner ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(inner)
        ) {
            Image(
                painter = painterResource(id = R.drawable.mapa),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxHeight()
                    .align(Alignment.Center),
                contentScale = ContentScale.FillHeight,
                alignment = Alignment.Center
            )

            Button(
                onClick = { /* no-op */ },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .fillMaxWidth(0.92f)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Accent,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Text("Verify the order", fontWeight = FontWeight.SemiBold)
            }

            ClientCard(
                name = "Cliff Rogers",
                role = "Client",
                eta = "10 mins",
                deliverTo = "W - 403",
                onChat = { /* no-op */ },
                onCall = { /* no-op */ },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )
        }
    }
}

@Composable
private fun ClientCard(
    name: String,
    role: String,
    eta: String,
    deliverTo: String,
    onChat: () -> Unit,
    onCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Avatar placeholder
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFDADADA))
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(role, color = Color.Gray, fontSize = 13.sp)
                }
                YellowIconButton(icon = Icons.Outlined.Chat, onClick = onChat)
                Spacer(Modifier.width(8.dp))
                YellowIconButton(icon = Icons.Outlined.Call, onClick = onCall)
            }

            Divider(Modifier.padding(vertical = 12.dp), color = Color(0xFFEAEAEA))

            InfoRow(title = "Estimated time", value = eta)
            Spacer(Modifier.height(6.dp))
            InfoRow(title = "Deliver to", value = deliverTo)

            Spacer(Modifier.height(12.dp))
            MoreDetailsButton(onClick = { /* no-op */ })
        }
    }
}

@Composable
private fun YellowIconButton(
    icon: ImageVector,
    onClick: () -> Unit
) {
    OutlinedIconButton(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.5.dp, Accent),
        colors = IconButtonDefaults.outlinedIconButtonColors(
            contentColor = Accent
        )
    ) {
        Icon(icon, contentDescription = null)
    }
}

@Composable
private fun MoreDetailsButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Accent.copy(alpha = 0.18f),
            contentColor = Accent
        ),
        border = BorderStroke(1.dp, Accent),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        Text("MORE DETAILS", letterSpacing = 1.2.sp)
    }
}

@Composable
private fun InfoRow(title: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = Color.Gray, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(value, fontWeight = FontWeight.Medium, fontSize = 14.sp)
    }
}

@Preview(showBackground = true, showSystemUi = true, device = "id:pixel_6")
@Composable
private fun PreviewCourierHome() {
    CourierHomeScreen()
}
