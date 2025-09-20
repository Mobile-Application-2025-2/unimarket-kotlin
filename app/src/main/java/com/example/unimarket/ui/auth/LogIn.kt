import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
fun LOGIN(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .requiredWidth(width = 393.dp)
            .requiredHeight(height = 852.dp)
            .background(color = Color.White)
    ) {
        Image(
            painter = painterResource(id = R.drawable.pasta),
            contentDescription = "Group 6800",
            alpha = 0.25f,
            modifier = Modifier
                .align(alignment = Alignment.TopStart)
                .offset(x = (-111.38).dp,
                    y = (-99.77).dp)
                .requiredWidth(width = 607.dp)
                .requiredHeight(height = 428.dp))
        Box(
            modifier = Modifier
                .align(alignment = Alignment.TopStart)
                .offset(x = 10.dp,
                    y = 28.dp)
                .requiredSize(size = 55.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.pasta),
                contentDescription = "Vector",
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(degrees = 180f)
                    .border(border = BorderStroke(1.dp, Color(0xff3f414e))))
            Box(
                modifier = Modifier
                    .requiredSize(size = 55.dp)
                    .rotate(degrees = 180f)
            ) {
                OutlinedTextField(
                    value = "",
                    onValueChange = {},
                    modifier = Modifier
                        .requiredSize(size = 55.dp))
            }
        }
        Text(
            text = "Welcome Back!",
            color = Color(0xffffc436),
            textAlign = TextAlign.Center,
            lineHeight = 4.82.em,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier
                .align(alignment = Alignment.TopStart)
                .offset(x = 89.dp,
                    y = 105.dp)
                .wrapContentHeight(align = Alignment.CenterVertically))
        Box(
            modifier = Modifier
                .align(alignment = Alignment.TopStart)
                .offset(x = 41.dp,
                    y = 182.47.dp)
                .requiredWidth(width = 313.dp)
                .requiredHeight(height = 63.dp)
        ) {
            Box(
                modifier = Modifier
                    .requiredWidth(width = 313.dp)
                    .requiredHeight(height = 63.dp)
                    .clip(shape = RoundedCornerShape(38.dp))
                    .background(color = Color(0xff0082c6)))
            Image(
                painter = painterResource(id = R.drawable.pasta),
                contentDescription = "image 3",
                modifier = Modifier
                    .align(alignment = Alignment.TopStart)
                    .offset(x = 24.dp,
                        y = 20.53.dp)
                    .requiredWidth(width = 24.dp)
                    .requiredHeight(height = 22.dp))
            Text(
                text = "CONTINUE WITH OUTLOOK",
                color = Color(0xfff6f1fb),
                lineHeight = 7.72.em,
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium),
                modifier = Modifier
                    .align(alignment = Alignment.TopStart)
                    .offset(x = 70.dp,
                        y = 24.53.dp)
                    .requiredWidth(width = 201.dp)
                    .requiredHeight(height = 14.dp))
        }
        Box(
            modifier = Modifier
                .align(alignment = Alignment.TopStart)
                .offset(x = 41.dp,
                    y = 265.47.dp)
                .requiredWidth(width = 313.dp)
                .requiredHeight(height = 63.dp)
        ) {
            Box(
                modifier = Modifier
                    .requiredWidth(width = 313.dp)
                    .requiredHeight(height = 63.dp)
            ) {
                Box(
                    modifier = Modifier
                        .requiredWidth(width = 313.dp)
                        .requiredHeight(height = 63.dp)
                        .clip(shape = RoundedCornerShape(38.dp))
                        .border(border = BorderStroke(1.dp, Color(0xffebeaec)),
                            shape = RoundedCornerShape(38.dp)))
                Text(
                    text = "CONTINUE WITH GOOGLE",
                    color = Color(0xff3f414e),
                    lineHeight = 7.72.em,
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium),
                    modifier = Modifier
                        .align(alignment = Alignment.TopStart)
                        .offset(x = 70.dp,
                            y = 24.53.dp)
                        .requiredWidth(width = 193.dp)
                        .requiredHeight(height = 14.dp))
            }
            Image(
                painter = painterResource(id = R.drawable.pasta),
                contentDescription = "Group 6795",
                modifier = Modifier
                    .align(alignment = Alignment.TopStart)
                    .offset(x = 24.33.dp,
                        y = 19.47.dp)
                    .requiredWidth(width = 24.dp)
                    .requiredHeight(height = 24.dp))
        }
        Text(
            text = "OR LOG IN WITH EMAIL",
            color = Color(0xffa1a4b2),
            textAlign = TextAlign.Center,
            lineHeight = 7.72.em,
            style = TextStyle(
                fontSize = 14.sp),
            modifier = Modifier
                .align(alignment = Alignment.TopStart)
                .offset(x = 102.dp,
                    y = 368.dp)
                .requiredWidth(width = 189.dp)
                .requiredHeight(height = 14.dp))
        Box(
            modifier = Modifier
                .align(alignment = Alignment.TopStart)
                .offset(x = 41.dp,
                    y = 422.47.dp)
                .requiredWidth(width = 313.dp)
                .requiredHeight(height = 63.dp)
        ) {
            Box(
                modifier = Modifier
                    .requiredWidth(width = 313.dp)
                    .requiredHeight(height = 63.dp)
                    .clip(shape = RoundedCornerShape(15.dp))
                    .background(color = Color(0xfff2f3f7)))
            Text(
                text = "Email address",
                color = Color(0xffa1a4b2),
                lineHeight = 6.76.em,
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Light),
                modifier = Modifier
                    .align(alignment = Alignment.TopStart)
                    .offset(x = 17.dp,
                        y = 22.53.dp)
                    .requiredWidth(width = 133.dp)
                    .requiredHeight(height = 18.dp)
                    .border(border = BorderStroke(0.15000000596046448.dp, Color(0xffa1a4b2))))
        }
        Box(
            modifier = Modifier
                .align(alignment = Alignment.TopStart)
                .offset(x = 41.dp,
                    y = 505.47.dp)
                .requiredWidth(width = 313.dp)
                .requiredHeight(height = 63.dp)
        ) {
            Box(
                modifier = Modifier
                    .requiredWidth(width = 313.dp)
                    .requiredHeight(height = 63.dp)
                    .clip(shape = RoundedCornerShape(15.dp))
                    .background(color = Color(0xfff2f3f7)))
            Text(
                text = "Password",
                color = Color(0xffa1a4b2),
                lineHeight = 6.76.em,
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Light),
                modifier = Modifier
                    .align(alignment = Alignment.TopStart)
                    .offset(x = 16.74.dp,
                        y = 22.36.dp)
                    .requiredWidth(width = 100.dp)
                    .requiredHeight(height = 14.dp)
                    .border(border = BorderStroke(0.15000000596046448.dp, Color(0xffa1a4b2))))
        }
        Box(
            modifier = Modifier
                .align(alignment = Alignment.TopStart)
                .offset(x = 41.dp,
                    y = 610.dp)
                .requiredWidth(width = 313.dp)
                .requiredHeight(height = 54.dp)
        ) {
            Box(
                modifier = Modifier
                    .requiredWidth(width = 313.dp)
                    .requiredHeight(height = 54.dp)
                    .clip(shape = RoundedCornerShape(38.dp))
                    .background(color = Color(0xffffc436)))
            Text(
                text = "SIGN IN",
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
        }
        Text(
            text = "Forgot Password?",
            color = Color(0xffa1a4b2),
            lineHeight = 7.72.em,
            style = TextStyle(
                fontSize = 14.sp),
            modifier = Modifier
                .align(alignment = Alignment.TopStart)
                .offset(x = 129.49.dp,
                    y = 681.47.dp)
                .requiredWidth(width = 135.dp)
                .requiredHeight(height = 14.dp))
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
                    fontWeight = FontWeight.Medium)) {append("SIGN UP")}},
            modifier = Modifier
                .align(alignment = Alignment.TopStart)
                .offset(x = 51.dp,
                    y = 801.dp)
                .requiredWidth(width = 292.dp)
                .requiredHeight(height = 13.dp)
                .wrapContentHeight(align = Alignment.CenterVertically))
    }
}

@Preview(widthDp = 393, heightDp = 852)
@Composable
private fun LOGINPreview() {
    LOGIN(Modifier)
}