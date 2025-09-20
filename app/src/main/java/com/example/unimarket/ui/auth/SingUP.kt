import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.unimarket.R
@Composable
fun SIGNUP(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .requiredWidth(width = 393.dp)
            .requiredHeight(height = 852.dp)
            .background(color = Color.White)
    ) {
        Box(
            modifier = Modifier
                .align(alignment = Alignment.TopStart)
                .offset(x = 49.dp,
                    y = 271.dp)
                .requiredWidth(width = 86.dp)
                .requiredHeight(height = 79.dp)
                .clip(shape = RoundedCornerShape(1.dp))
                .rotate(degrees = -3.68f)
                .background(color = Color(0xffffff72)))
        Image(
            painter = painterResource(id = R.drawable.pasta),
            contentDescription = "image 9",
            modifier = Modifier
                .align(alignment = Alignment.TopStart)
                .offset(x = 27.dp,
                    y = 300.dp)
                .requiredWidth(width = 113.dp)
                .requiredHeight(height = 112.dp))
        Text(
            text = "UNIMARKET",
            color = Color(0xffffc436),
            lineHeight = 3.55.em,
            style = TextStyle(
                fontSize = 38.sp),
            modifier = Modifier
                .fillMaxSize()
                .wrapContentHeight(align = Alignment.CenterVertically))
        Box(
            modifier = Modifier
                .align(alignment = Alignment.TopStart)
                .offset(x = 40.dp,
                    y = 729.dp)
                .requiredWidth(width = 313.dp)
                .requiredHeight(height = 85.dp)
        ) {
            Box(
                modifier = Modifier
                    .requiredWidth(width = 313.dp)
                    .requiredHeight(height = 54.dp)
                    .clip(shape = RoundedCornerShape(38.dp))
                    .background(color = Color(0xffffc436)))
            Text(
                text = "SIGN UP",
                color = Color(0xfff6f1fb),
                textAlign = TextAlign.Center,
                lineHeight = 6.76.em,
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold),
                modifier = Modifier
                    .align(alignment = Alignment.TopStart)
                    .offset(x = 118.dp,
                        y = 18.dp)
                    .requiredWidth(width = 76.dp)
                    .requiredHeight(height = 17.dp)
                    .wrapContentHeight(align = Alignment.CenterVertically))
            Text(
                textAlign = TextAlign.Center,
                lineHeight = 8.sp,
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(
                        color = Color(0xffa1a4b2),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    ) {append("ALREADY HAVE AN ACCOUNT?")}
                    withStyle(style = SpanStyle(
                        color = Color.Black,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium)) {append(" ")}
                    withStyle(style = SpanStyle(
                        color = Color(0xffffc436),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium)) {append("LOG IN")}},
                modifier = Modifier
                    .align(alignment = Alignment.TopStart)
                    .offset(x = 16.dp,
                        y = 72.dp)
                    .requiredWidth(width = 281.dp)
                    .requiredHeight(height = 13.dp)
                    .wrapContentHeight(align = Alignment.CenterVertically))
        }
    }
}

@Preview(widthDp = 393, heightDp = 852)
@Composable
private fun SIGNUPPreview() {
    SIGNUP(Modifier)
}