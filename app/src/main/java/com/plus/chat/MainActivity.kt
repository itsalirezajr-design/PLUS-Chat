package com.plus.chat

import android.content.Context
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val API_BASE = "https://plus-chat.itsalirezajr.workers.dev"
private val Navy = Color(0xFF080D1D)
private val Panel = Color(0xFF121B32)
private val Purple = Color(0xFF8B70FF)
private val Cyan = Color(0xFF55C8F5)
private val Muted = Color(0xFF9AA8C4)

private object PlusApi {
    private fun request(path: String, method: String = "GET", token: String? = null, body: JSONObject? = null): JSONObject {
        val connection = (URL(API_BASE + path).openConnection() as HttpURLConnection)
        try {
            connection.requestMethod = method
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.setRequestProperty("Accept", "application/json")
            if (token != null) connection.setRequestProperty("Authorization", "Bearer $token")
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            val result = if (text.isBlank()) JSONObject() else JSONObject(text)
            if (code !in 200..299) throw IllegalStateException(result.optString("error").ifBlank { "خطای سرور ($code)" })
            return result
        } finally { connection.disconnect() }
    }
    suspend fun auth(path: String, username: String, password: String, displayName: String = "") =
        withContext(Dispatchers.IO) {
            val body = JSONObject().put("username", username).put("password", password)
            if (displayName.isNotBlank()) body.put("displayName", displayName)
            request(path, "POST", body = body)
        }
    suspend fun conversations(token: String) = withContext(Dispatchers.IO) { request("/conversations", token = token).optJSONArray("conversations") ?: JSONArray() }
    suspend fun search(token: String, query: String) = withContext(Dispatchers.IO) { request("/users/search?q=" + java.net.URLEncoder.encode(query, "UTF-8"), token = token).optJSONArray("users") ?: JSONArray() }
    suspend fun createConversation(token: String, username: String) = withContext(Dispatchers.IO) {
        request("/conversations", "POST", token, JSONObject().put("username", username)).getString("conversationId")
    }
    suspend fun messages(token: String, id: String) = withContext(Dispatchers.IO) { request("/conversations/$id/messages", token = token).optJSONArray("messages") ?: JSONArray() }
    suspend fun send(token: String, id: String, body: String) = withContext(Dispatchers.IO) {
        request("/conversations/$id/messages", "POST", token, JSONObject().put("body", body))
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PlusChatApp(getSharedPreferences("plus_chat_session", Context.MODE_PRIVATE)) }
    }
}

@Composable
fun PlusChatApp(prefs: android.content.SharedPreferences) {
    var token by remember { mutableStateOf(prefs.getString("token", null)) }
    var username by remember { mutableStateOf(prefs.getString("username", "") ?: "") }
    var displayName by remember { mutableStateOf(prefs.getString("displayName", "") ?: "") }\n    var userId by remember { mutableStateOf(prefs.getString("userId", "") ?: "") }
    var page by remember { mutableStateOf(if (token == null) "welcome" else "chats") }
    var loginName by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var chats by remember { mutableStateOf(listOf<JSONObject>()) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var selectedTitle by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf(listOf<JSONObject>()) }
    var messageInput by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf(listOf<JSONObject>()) }
    val scope = rememberCoroutineScope()

    fun logout() {
        prefs.edit().clear().apply()
        token = null; username = ""; displayName = ""; userId = ""; page = "welcome"; selectedId = null
    }
    fun loadChats() {
        val t = token ?: return
        scope.launch {
            try {
                val arr = PlusApi.conversations(t)
                chats = (0 until arr.length()).map { arr.getJSONObject(it) }
                error = ""
            } catch (e: Exception) { error = e.message ?: "دریافت گفتگوها ناموفق بود" }
        }
    }
    LaunchedEffect(token, page) { if (token != null && page == "chats") loadChats() }
    LaunchedEffect(token, selectedId) {
        val t = token
        val id = selectedId
        if (t != null && id != null) {
            while (true) {
                try {
                    val arr = PlusApi.messages(t, id)
                    messages = (0 until arr.length()).map { arr.getJSONObject(it) }
                } catch (e: Exception) { error = e.message ?: "دریافت پیام‌ها ناموفق بود" }
                delay(4000)
            }
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(primary = Purple, secondary = Cyan, background = Navy, surface = Panel, onBackground = Color.White, onSurface = Color.White)) {
        Surface(Modifier.fillMaxSize(), color = Navy) {
            when {
                token == null -> AuthScreen(
                    mode = page, username = loginName, onUsername = { loginName = it },
                    password = password, onPassword = { password = it },
                    displayName = nameInput, onDisplayName = { nameInput = it }, error = error, busy = busy,
                    onMode = { page = it; error = "" },
                    onSubmit = {
                        val clean = loginName.trim().lowercase()
                        error = when {
                            clean.length !in 3..24 || !clean.matches(Regex("[a-z0-9_.]+")) -> "نام کاربری باید ۳ تا ۲۴ حرف و فقط شامل حروف انگلیسی، عدد، نقطه یا زیرخط باشد"
                            password.length < 10 -> "رمز عبور باید حداقل ۱۰ کاراکتر باشد"
                            page == "signup" && nameInput.isBlank() -> "نام نمایشی را وارد کن"
                            else -> ""
                        }
                        if (error.isEmpty()) scope.launch {
                            busy = true
                            try {
                                val result = PlusApi.auth(if (page == "signup") "/auth/register" else "/auth/login", clean, password, if (page == "signup") nameInput.trim() else "")
                                token = result.getString("token")
                                val user = result.getJSONObject("user")
                                username = user.getString("username")
                                displayName = user.getString("displayName")
                                prefs.edit().putString("token", token).putString("username", username).putString("displayName", displayName).apply()
                                page = "chats"; password = ""; error = ""
                            } catch (e: Exception) { error = e.message ?: "ارتباط با سرور برقرار نشد" }
                            finally { busy = false }
                        }
                    }
                )
                selectedId != null -> ConversationScreen(
                    title = selectedTitle, messages = messages, myUsername = userId,
                    value = messageInput, onValue = { messageInput = it }, error = error,
                    onBack = { selectedId = null; page = "chats" },
                    onSend = {
                        val text = messageInput.trim()
                        val t = token
                        val id = selectedId
                        if (text.isNotEmpty() && t != null && id != null) scope.launch {
                            try { PlusApi.send(t, id, text); messageInput = ""; val arr = PlusApi.messages(t, id); messages = (0 until arr.length()).map { arr.getJSONObject(it) }; error = "" }
                            catch (e: Exception) { error = e.message ?: "ارسال پیام ناموفق بود" }
                        }
                    }
                )
                else -> ChatsScreen(
                    name = displayName.ifBlank { username }, chats = chats, searchQuery = searchQuery,
                    searchResults = searchResults, error = error, onSearch = { searchQuery = it },
                    onFind = {
                        val t = token
                        if (t != null) scope.launch { try { val a = PlusApi.search(t, searchQuery.trim()); searchResults = (0 until a.length()).map { a.getJSONObject(it) }; error = "" } catch (e: Exception) { error = e.message ?: "جستجو ناموفق بود" } }
                    },
                    onOpen = { id, title -> selectedId = id; selectedTitle = title; searchResults = emptyList(); searchQuery = "" },
                    onNewChat = { peer ->
                        val t = token
                        if (t != null) scope.launch { try { val id = PlusApi.createConversation(t, peer); selectedId = id; selectedTitle = peer; searchResults = emptyList(); searchQuery = ""; loadChats() } catch (e: Exception) { error = e.message ?: "ساخت گفتگو ناموفق بود" } }
                    },
                    onRefresh = { loadChats() }, onLogout = { logout() }
                )
            }
        }
    }
}

@Composable
private fun AuthScreen(
    mode: String, username: String, onUsername: (String) -> Unit,
    password: String, onPassword: (String) -> Unit, displayName: String,
    onDisplayName: (String) -> Unit, error: String, busy: Boolean,
    onMode: (String) -> Unit, onSubmit: () -> Unit
) {
    val signup = mode == "signup"
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF111A35), Navy, Color(0xFF080D1D)))).padding(horizontal = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
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
        if (signup) { AuthField(displayName, onDisplayName, "نام نمایشی", Icons.Default.Person, false); Spacer(Modifier.height(12.dp)) }
        AuthField(username, onUsername, "نام کاربری", Icons.Default.Person, false)
        Spacer(Modifier.height(12.dp))
        AuthField(password, onPassword, "رمز عبور (حداقل ۱۰ کاراکتر)", Icons.Default.Lock, true)
        if (error.isNotEmpty()) { Spacer(Modifier.height(10.dp)); Text(error, color = Color(0xFFFF8585), fontSize = 12.sp) }
        Spacer(Modifier.height(22.dp))
        Button(onClick = onSubmit, enabled = !busy, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(17.dp), colors = ButtonDefaults.buttonColors(containerColor = Purple)) {
            if (busy) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp) else Text(if (signup) "ساخت حساب" else "ورود به حساب", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (signup) "قبلاً حساب ساخته‌ای؟" else "حساب کاربری نداری؟", color = Muted, fontSize = 13.sp)
            Text(if (signup) " ورود" else " ثبت‌نام", color = Cyan, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onMode(if (signup) "login" else "signup") }.padding(6.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text("بدون شماره تلفن • بدون ایمیل • بدون گوگل", color = Muted, fontSize = 11.sp)
    }
}

@Composable
private fun AuthField(value: String, onValue: (String) -> Unit, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, secret: Boolean) {
    OutlinedTextField(value = value, onValueChange = onValue, modifier = Modifier.fillMaxWidth(), label = { Text(label) },
        leadingIcon = { Icon(icon, null, tint = Muted) }, singleLine = true, shape = RoundedCornerShape(15.dp),
        visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (secret) KeyboardType.Password else KeyboardType.Text),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Purple, unfocusedBorderColor = Color(0xFF34415F), focusedLabelColor = Purple, unfocusedLabelColor = Muted, focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = Cyan))
}

@Composable
private fun ChatsScreen(
    name: String, chats: List<JSONObject>, searchQuery: String, searchResults: List<JSONObject>,
    error: String, onSearch: (String) -> Unit, onFind: () -> Unit,
    onOpen: (String, String) -> Unit, onNewChat: (String) -> Unit, onRefresh: () -> Unit, onLogout: () -> Unit
) {
    Column(Modifier.fillMaxSize().background(Navy)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 22.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text("PLUS CHAT", color = Cyan, fontSize = 12.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold); Text("سلام، $name", fontSize = 24.sp, fontWeight = FontWeight.Bold) }
            Text("تازه‌سازی", color = Cyan, modifier = Modifier.clickable { onRefresh() }.padding(6.dp))
            Text("خروج", color = Muted, modifier = Modifier.clickable { onLogout() }.padding(6.dp))
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = searchQuery, onValueChange = onSearch, modifier = Modifier.weight(1f), placeholder = { Text("جستجوی نام کاربری", color = Muted) }, leadingIcon = { Icon(Icons.Default.Search, null, tint = Muted) }, singleLine = true, shape = RoundedCornerShape(15.dp))
            Spacer(Modifier.width(8.dp)); Button(onClick = onFind, colors = ButtonDefaults.buttonColors(containerColor = Purple)) { Text("جستجو") }
        }
        if (error.isNotEmpty()) Text(error, color = Color(0xFFFF8585), modifier = Modifier.padding(12.dp), fontSize = 12.sp)
        if (searchResults.isNotEmpty()) {
            Text("نتایج جستجو — برای شروع گفتگو انتخاب کن", color = Cyan, modifier = Modifier.padding(16.dp), fontSize = 13.sp)
            LazyColumn(Modifier.weight(1f)) {
                items(searchResults) { user ->
                    val handle = user.optString("username")
                    Row(Modifier.fillMaxWidth().clickable { onNewChat(handle) }.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Avatar(handle); Column(Modifier.padding(start = 12.dp)) { Text(user.optString("displayName"), fontWeight = FontWeight.Bold); Text("@$handle", color = Muted, fontSize = 12.sp) }
                    }
                }
            }
        } else {
            Text("گفتگوهای تو", modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            if (chats.isEmpty()) Text("هنوز گفتگویی نداری؛ نام کاربری دوستت را جستجو کن.", color = Muted, modifier = Modifier.padding(20.dp), fontSize = 13.sp)
            LazyColumn(Modifier.weight(1f)) {
                items(chats) { chat ->
                    val title = chat.optString("displayName", chat.optString("username"))
                    Row(Modifier.fillMaxWidth().clickable { onOpen(chat.optString("id"), title) }.padding(horizontal = 18.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Avatar(title)
                        Column(Modifier.weight(1f).padding(start = 13.dp)) { Text(title, fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(4.dp)); Text(chat.optString("lastMessage", "گفتگو را باز کن"), color = Muted, fontSize = 12.sp) }
                    }
                    HorizontalDivider(color = Color(0xFF202B45), modifier = Modifier.padding(start = 82.dp))
                }
            }
        }
    }
}

@Composable
private fun Avatar(text: String) {
    Box(Modifier.size(48.dp).background(Color(0xFF8B70FF).copy(alpha = .2f), CircleShape), contentAlignment = Alignment.Center) {
        Text(text.take(1).uppercase(), color = Cyan, fontWeight = FontWeight.Bold, fontSize = 20.sp)
    }
}

@Composable
private fun ConversationScreen(
    title: String, messages: List<JSONObject>, myUsername: String, value: String, onValue: (String) -> Unit,
    error: String, onBack: () -> Unit, onSend: () -> Unit
) {
    Column(Modifier.fillMaxSize().background(Navy)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "بازگشت", tint = Color.White) }
            Column { Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp); Text("گفتگوی خصوصی", color = Muted, fontSize = 11.sp) }
        }
        if (error.isNotEmpty()) Text(error, color = Color(0xFFFF8585), modifier = Modifier.padding(horizontal = 16.dp), fontSize = 12.sp)
        LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp), reverseLayout = false) {
            items(messages) { msg ->
                val mine = msg.optString("senderId") == myUsername || msg.optString("senderId") == ""
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
                    Column(Modifier.padding(vertical = 4.dp).background(if (mine) Color(0xFF493C83) else Panel, RoundedCornerShape(15.dp)).padding(12.dp).widthIn(max = 280.dp)) {
                        Text(msg.optString("body"), color = Color.White, fontSize = 14.sp)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = value, onValueChange = onValue, modifier = Modifier.weight(1f), placeholder = { Text("پیامت را بنویس…") }, shape = RoundedCornerShape(16.dp), maxLines = 4)
            Spacer(Modifier.width(8.dp)); Button(onClick = onSend, enabled = value.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = Purple)) { Text("ارسال") }
        }
    }
}
