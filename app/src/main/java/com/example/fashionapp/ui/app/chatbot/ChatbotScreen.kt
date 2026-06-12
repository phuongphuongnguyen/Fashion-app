package com.example.fashionapp.ui.app.chatbot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.fashionapp.R
import com.example.fashionapp.ui.app.settings.LocalAppSettings
import com.example.fashionapp.ui.theme.AppTheme
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.launch

// ── Data ──
data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatbotScreen(navController: NavController) {
    val settings = LocalAppSettings.current
    val generativeModel = remember {
        GenerativeModel(
            modelName = "models/gemini-3.1-flash-lite",
            apiKey    = ""
        )
    }

    var messages by remember(settings) {
        mutableStateOf(
            listOf(
                ChatMessage(
                    text    = settings.t(
                        "Hello! I'm your fashion assistant. How can I help you today?",
                        "Xin chào! Tôi là trợ lý thời trang của bạn. Tôi có thể giúp gì cho bạn hôm nay?"
                    ),
                    isUser  = false
                )
            )
        )
    }
    var inputText      by remember { mutableStateOf("") }
    var isLoading      by remember { mutableStateOf(false) }

    // Cache context sau lần fetch đầu tiên
    var firestoreContext by remember { mutableStateOf<String?>(null) }
    var isLoadingContext by remember { mutableStateOf(true) }

    val coroutineScope = rememberCoroutineScope()
    val listState      = rememberLazyListState()

    // Fetch Firestore context 1 lần khi mở chatbot
    LaunchedEffect(Unit) {
        try {
            firestoreContext = FirestoreContextBuilder.buildContext()
        } catch (_: Exception) {
            firestoreContext = ""
        } finally {
            isLoadingContext = false
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    fun sendToGemini(userMessage: String) {
        messages   = messages + ChatMessage(text = userMessage, isUser = true)
        inputText  = ""
        isLoading  = true

        coroutineScope.launch {
            try {
                // Tạo prompt với Firestore context
                val context = firestoreContext ?: ""
                val prompt = if (settings.language == com.example.fashionapp.ui.app.settings.AppLanguage.VIETNAMESE) {
                    """
Bạn là trợ lý thời trang thông minh của một app mua sắm thời trang Việt Nam.
Bạn có thể trả lời dựa trên dữ liệu thực tế của app bên dưới.
Hãy trả lời bằng tiếng Việt, thân thiện, ngắn gọn và hữu ích.
Nếu câu hỏi liên quan đến sản phẩm, đơn hàng, voucher hoặc shop — hãy dùng dữ liệu bên dưới.
Nếu không có thông tin liên quan — hãy trả lời dựa trên kiến thức thời trang chung.

--- DỮ LIỆU APP ---
$context
--- HẾT DỮ LIỆU ---

Khách hàng hỏi: $userMessage
                    """.trimIndent()
                } else {
                    """
You are an intelligent fashion assistant of a Vietnamese fashion shopping app.
You can reply based on the app's real data below.
Please reply in English, in a friendly, concise, and helpful manner.
If the question is about products, orders, vouchers, or the shop — use the data below.
If there is no relevant information — reply based on general fashion knowledge.

--- APP DATA ---
$context
--- END DATA ---

Customer asks: $userMessage
                    """.trimIndent()
                }

                val response = generativeModel.generateContent(prompt)
                val botReply = response.text
                    ?: settings.t("Sorry, I could not process your request. Please try again.", "Xin lỗi, tôi không thể xử lý yêu cầu của bạn. Vui lòng thử lại.")
                messages = messages + ChatMessage(text = botReply, isUser = false)
            } catch (e: Exception) {
                android.util.Log.e("CHATBOT_ERROR", "Class: ${e.javaClass.name}", e)
                android.util.Log.e("CHATBOT_ERROR", "Message: ${e.message}")
                android.util.Log.e("CHATBOT_ERROR", "Cause: ${e.cause}")
                messages = messages + ChatMessage(
                    text = settings.t("Sorry, connection error. Please check your internet connection and try again.", "Xin lỗi, có lỗi kết nối. Vui lòng kiểm tra internet và thử lại."),
                    isUser = false
                )
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ── Header ──
            ChatHeader(
                onBack        = { navController.popBackStack() },
                isLoadingData = isLoadingContext
            )

            // ── Messages ──
            LazyColumn(
                state          = listState,
                modifier       = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(messages) { message ->
                    ChatBubble(message = message)
                }
                if (isLoading) {
                    item { TypingIndicator() }
                }
            }

            // ── Input Bar ──
            ChatInputBar(
                value         = inputText,
                onValueChange = { inputText = it },
                onSend        = {
                    if (inputText.isNotBlank() && !isLoadingContext) {
                        sendToGemini(inputText.trim())
                    }
                },
                isLoading     = isLoading || isLoadingContext,
                placeholder   = if (isLoadingContext) settings.t("Loading data...", "Đang tải dữ liệu...") else settings.t("Type a message...", "Nhập tin nhắn...")
            )
        }
    }
}

// ── Chat Header ──
@Composable
private fun ChatHeader(onBack: () -> Unit, isLoadingData: Boolean = false) {
    val settings = LocalAppSettings.current
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = settings.t("Back", "Quay lại"),
                    tint               = MaterialTheme.colorScheme.onSurface
                )
            }

            Icon(
                painter           = painterResource(R.drawable.ic_chatbot),
                contentDescription = null,
                tint              = Color.Unspecified,
                modifier          = Modifier.size(38.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = "Fashion Assistant",
                    fontSize   = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text     = if (isLoadingData) settings.t("Loading app data...", "Đang tải dữ liệu app...") else settings.t("Your fashion assistant", "Trợ lý thời trang của bạn"),
                    fontSize = 12.sp,
                    color    = if (isLoadingData) MaterialTheme.colorScheme.secondary else AppTheme.colors.textSecondary
                )
            }

            // Dot indicator khi đang load data
            if (isLoadingData) {
                CircularProgressIndicator(
                    modifier    = Modifier.size(16.dp).padding(end = 8.dp),
                    strokeWidth = 2.dp,
                    color       = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

// ── Chat Bubble ──
@Composable
private fun ChatBubble(message: ChatMessage) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!message.isUser) {
            Icon(
                painter           = painterResource(R.drawable.ic_chatbot),
                contentDescription = null,
                tint              = Color.Unspecified,
                modifier          = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart    = 16.dp,
                        topEnd      = 16.dp,
                        bottomStart = if (message.isUser) 16.dp else 4.dp,
                        bottomEnd   = if (message.isUser) 4.dp else 16.dp
                    )
                )
                .background(if (message.isUser) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text      = message.text,
                color     = if (message.isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                fontSize  = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}

// ── Typing Indicator ──
@Composable
private fun TypingIndicator() {
    val settings = LocalAppSettings.current
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Icon(
            painter           = painterResource(R.drawable.ic_chatbot),
            contentDescription = null,
            tint              = Color.Unspecified,
            modifier          = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text(settings.t("Typing...", "Đang nhập..."), color = AppTheme.colors.textSecondary, fontSize = 14.sp)
        }
    }
}

// ── Chat Input Bar ──
@Composable
private fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    isLoading: Boolean,
    placeholder: String = "Nhập tin nhắn..."
) {
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 6.dp) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value         = value,
                onValueChange = onValueChange,
                modifier      = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp),
                placeholder   = {
                    Text(placeholder, color = AppTheme.colors.textSecondary, fontSize = 14.sp)
                },
                shape  = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = MaterialTheme.colorScheme.secondary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor   = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                singleLine = false,
                maxLines   = 4
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick  = onSend,
                enabled  = value.isNotBlank() && !isLoading,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (value.isNotBlank() && !isLoading) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
            ) {
                Icon(
                    imageVector        = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint               = Color.White,
                    modifier           = Modifier.size(20.dp)
                )
            }
        }
    }
}