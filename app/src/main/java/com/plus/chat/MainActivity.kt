package com.plus.chat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Navy = Color(0xFF080D1D)
private val Panel = Color(0xFF121B32)
private val Purple = Color(0xFF8B70FF)
private val Cyan = Color(0xFF55C8F5)
private val Muted = Color(0xFF9AA8C4)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PlusChatApp() }
    }
}

@Composable
fun PlusChatApp() {
    var page by remember { mutableStateOf("welcome") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    MaterialTheme(colorScheme = darkColorScheme(
        primary = Purple, secondary = Cyan, background = Navy, surface = Panel,
        onBackground = Color.White, onSurface = Color.White
    )) {
        Surface(Modifier.fillMaxSize(), color = Navy) {
            when (page) {
                "welcome", "login", "signup" -> AuthScreen(
                    mode = page, username = username, onUsername = { username = it },
                    password = password, onPassword = { password = it },
                    displayName = displayName, onDisplayName = { displayName = it },
                    error = error,
                    onMode = { page = it; error = "" },
                    onSubmit = {
                        error = when {
                            username.trim().length < 3 -> "نام کاربری باید حداقل ۳ حرف باشد"
                            !username.matches(Regex("[A-Za-z0-9_.]+")) -> "فقط حروف انگلیسی، عدد، نقطه و زیرخط مجاز است"
                            password.length < 8 -> "رمز عبور باید حداقل ۸ کاراکتر باشد"
                            page == "signup" && displayName.isBlank() -> "نام نمایشی را وارد کن"
                            else -> ""
                        }
                        if (error.isEmpty()) page = "chats"
                    }
                )
                else -> ChatsScreen(name = displayName.ifBlank { username }, onLogout = { page = "welcome"; password = "" })
            }
        }
    }
}

@Composable
private fun AuthScreen(
    mode: String, username: String, onUsername: (String) -> Unit,
    password: String, onPassword: (String) -> Unit,
    displayName: String, onDisplayName: (String) -> Unit,
    error: String, onMode: (String) -> Unit, onSubmit: () -> Unit
) {
    val signup = mode == "signup"
    Column(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF111A35), Navy, Color(0xFF080D1D))))
            .padding(horizontal = 26.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(Modifier.size(86.dp).background(Brush.linearGradient(listOf(Purple, Cyan)), CircleShape), contentAlignment = Alignment.Center) {
            Text("＋", color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(18.dp))
        Text("PLUS CHAT", fontSize = 29.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
        Text("ارتباطی ساده، سریع و اختصاصی", color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(34.dp))
        Text(if (signup) "ساخت حساب جدید" else if (mode == "login") "خوش برگشتی!" else "به PLUS Chat خوش آمدی", fontSize = 23.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("ورود فقط با نام کاربری و رمز عبور", color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(24.dp))
        if (signup) {
            AuthField(displayName, onDisplayName, "نام نمایشی", Icons.Default.Person, false)
            Spacer(Modifier.height(12.dp))
        }
        AuthField(username, onUsername, "نام کاربری", Icons.Default.Person, false)
        Spacer(Modifier.height(12.dp))
        AuthField(password, onPassword, "رمز عبور (حداقل ۸ کاراکتر)", Icons.Default.Lock, true)
        if (error.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text(error, color = Color(0xFFFF8585), fontSize = 12.sp)
        }
        Spacer(Modifier.height(22.dp))
        Button(onClick = onSubmit, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(17.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Purple)) {
            Text(if (signup) "ساخت حساب" else "ورود به حساب", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (signup) "قبلاً حساب ساخته‌ای؟" else "حساب کاربری نداری؟", color = Muted, fontSize = 13.sp)
            Text(if (signup) " ورود" else " ثبت‌نام", color = Cyan, fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onMode(if (signup) "login" else "signup") }.padding(6.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text("بدون شماره تلفن • بدون ایمیل • بدون گوگل", color = Muted, fontSize = 11.sp)
    }
}

@Composable
private fun AuthField(value: String, onValue: (String) -> Unit, label: String,
                      icon: androidx.compose.ui.graphics.vector.ImageVector, secret: Boolean) {
    OutlinedTextField(value = value, onValueChange = onValue, modifier = Modifier.fillMaxWidth(),
        label = { Text(label) }, leadingIcon = { Icon(icon, null, tint = Muted) },
        singleLine = true, shape = RoundedCornerShape(15.dp),
        visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (secret) KeyboardType.Password else KeyboardType.Text),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Purple, unfocusedBorderColor = Color(0xFF34415F),
            focusedLabelColor = Purple, unfocusedLabelColor = Muted,
            focusedTextColor = Color.White, unfocusedTextColor = Color.White,
            cursorColor = Cyan
        )
    )
}

private data class ChatPreview(val title: String, val message: String, val time: String, val initials: String, val color: Color)

@Composable
private fun ChatsScreen(name: String, onLogout: () -> Unit) {
    val chats = listOf(
        ChatPreview("تیم PLUS", "به PLUS Chat خوش آمدید!", "۱۲:۰۵", "P", Purple),
        ChatPreview("گفتگوی ذخیره‌شده", "اینجا پیام‌های شخصی تو قرار می‌گیرد", "دیروز", "★", Color(0xFFDBA84E)),
        ChatPreview("شروع گفتگو", "برای پیدا کردن دوستان جستجو کن", "", "+", Cyan)
    )
    Column(Modifier.fillMaxSize().background(Navy)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 22.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("PLUS CHAT", color = Cyan, fontSize = 12.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                Text("سلام، $name", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Text("خروج", color = Muted, modifier = Modifier.clickable { onLogout() }.padding(8.dp))
        }
        OutlinedTextField(value = "", onValueChange = {}, modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            placeholder = { Text("جستجوی گفتگوها", color = Muted) }, leadingIcon = { Icon(Icons.Default.Search, null, tint = Muted) },
            shape = RoundedCornerShape(16.dp), singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFF34415F), focusedBorderColor = Purple))
        Spacer(Modifier.height(18.dp))
        Text("پیام‌ها", modifier = Modifier.padding(horizontal = 20.dp), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        LazyColumn(Modifier.weight(1f).padding(top = 8.dp)) {
            items(chats) { chat ->
                Row(Modifier.fillMaxWidth().clickable { }.padding(horizontal = 18.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(52.dp).background(chat.color.copy(alpha = .20f), CircleShape), contentAlignment = Alignment.Center) {
                        Text(chat.initials, color = chat.color, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(Modifier.weight(1f).padding(start = 13.dp)) {
                        Text(chat.title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(chat.message, color = Muted, fontSize = 12.sp)
                    }
                    Text(chat.time, color = Muted, fontSize = 10.sp)
                }
                HorizontalDivider(color = Color(0xFF202B45), modifier = Modifier.padding(start = 82.dp))
            }
        }
        Text("نسخه نمایشی رابط کاربری — اتصال سرور هنوز انجام نشده", color = Muted, fontSize = 11.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(14.dp))
    }
}
