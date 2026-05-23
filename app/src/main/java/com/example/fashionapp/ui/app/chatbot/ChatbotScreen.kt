package com.example.fashionapp.ui.app.chatbot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.fashionapp.ui.app.settings.LocalAppSettings
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.launch

// ── Colors ──
private val PrimaryBlue = Color(0xFF3669C9)
private val ChatBubbleUser = Color(0xFF3669C9)

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
    val isDark = settings.isDarkMode
    
    // Dynamic theme styling
    val bgColor = if (isDark) Color(0xFF121212) else Color(0xFFF5F7FB)
    val cardColor = if (isDark) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF1A1A2E)
    val botBubbleColor = if (isDark) Color(0xFF2C2C2C) else Color(0xFFEEF2FF)
    val botBubbleTextColor = if (isDark) Color.White else Color(0xFF1A1A2E)

    val generativeModel = remember {
        GenerativeModel(
            modelName = "models/gemini-2.0-flash",
            apiKey = ""
        )
    }

    var messages by remember {
        mutableStateOf<List<ChatMessage>>(emptyList())
    }
    
    // Auto-update first greeting message based on dynamic language changes
    val greeting = settings.t(
        en = "Hello! I am your personal fashion assistant. How can I help you today?",
        vi = "Xin chào! Tôi là trợ lý thời trang của bạn. Tôi có thể giúp gì cho bạn hôm nay?",
        fr = "Bonjour! Je suis votre assistant de mode personnel. Comment puis-je vous aider aujourd'hui?",
        ja = "こんにちは！私はあなたのパーソナルファッションアシスタントです。本日はどのようなご用件でしょうか？",
        ko = "안녕하세요! 저는 당신의 개인 패션 어시스턴트입니다. 오늘 무엇을 도와드릴까요?",
        zh = "你好！我是您的个人时尚助理。今天有什么我可以帮您的吗？"
    )

    LaunchedEffect(settings.language) {
        if (messages.isEmpty() || (messages.size == 1 && !messages[0].isUser)) {
            messages = listOf(
                ChatMessage(
                    text = greeting,
                    isUser = false
                )
            )
        }
    }
    
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    fun sendToGemini(userMessage: String) {
        messages = messages + ChatMessage(text = userMessage, isUser = true)
        inputText = ""
        isLoading = true

        coroutineScope.launch {
            try {
                val prompt = "You are a friendly, expert fashion assistant for a fashion e-commerce app. " +
                        "Be helpful, concise, and professional. You must respond in the following language: ${settings.language.displayName}. " +
                        "\n\nCustomer: $userMessage"
                val response = generativeModel.generateContent(prompt)
                val botReply = response.text ?: settings.t(
                    en = "Sorry, I cannot process your request right now. Please try again.",
                    vi = "Xin lỗi, tôi không thể xử lý yêu cầu của bạn. Vui lòng thử lại.",
                    fr = "Désolé, je ne peux pas traiter votre demande pour le moment. Veuillez réessayer.",
                    ja = "申し訳ありません。現在リクエストを処理できません。もう一度お試しください。",
                    ko = "죄송합니다. 현재 요청을 처리할 수 없습니다. 다시 시도해 주세요.",
                    zh = "抱歉，我现在无法处理您的请求。请稍后再试。"
                )
                messages = messages + ChatMessage(text = botReply, isUser = false)
            } catch (e: Exception) {
                messages = messages + ChatMessage(
                    text = settings.t(
                        en = "Sorry, there was a connection error. Please check your internet and try again.",
                        vi = "Xin lỗi, có lỗi kết nối. Vui lòng kiểm tra internet và thử lại.",
                        fr = "Désolé, il y a eu une erreur de connexion. Veuillez vérifier votre connexion internet et réessayer.",
                        ja = "接続エラーが発生しました。インターネット接続を確認して再試行してください。",
                        ko = "죄송합니다. 연결 오류가 발생했습니다. 인터넷 연결을 확인하고 다시 시도해 주세요.",
                        zh = "抱歉，连接出错。请检查您的网络并重试。"
                    ),
                    isUser = false
                )
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        containerColor = bgColor
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ── Header ──
            ChatHeader(
                onClose = { navController.popBackStack() },
                title = settings.t(
                    en = "Fashion Assistant",
                    vi = "Trợ lý thời trang",
                    fr = "Assistant de Mode",
                    ja = "ファッションアシスタント",
                    ko = "패션 어시스턴트",
                    zh = "时尚助理"
                ),
                subtitle = settings.t(
                    en = "Your personal fashion assistant",
                    vi = "Trợ lý thời trang của bạn",
                    fr = "Votre assistant de mode",
                    ja = "あなたのパーソナルアシスタント",
                    ko = "개인 패션 어시스턴트",
                    zh = "您的个人时尚助理"
                ),
                isDark = isDark,
                cardColor = cardColor,
                textColor = textColor
            )

            // ── Messages ──
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(messages) { message ->
                    ChatBubble(
                        message = message,
                        botBubbleColor = botBubbleColor,
                        botBubbleTextColor = botBubbleTextColor
                    )
                }
                if (isLoading) {
                    item {
                        TypingIndicator(
                            botBubbleColor = botBubbleColor,
                            typingText = settings.t(
                                en = "Typing...",
                                vi = "Đang nhập...",
                                fr = "Écrit...",
                                ja = "入力中...",
                                ko = "입력 중...",
                                zh = "正在输入..."
                            )
                        )
                    }
                }
            }

            // ── Input Bar ──
            ChatInputBar(
                value = inputText,
                onValueChange = { inputText = it },
                onSend = {
                    if (inputText.isNotBlank()) {
                        sendToGemini(inputText.trim())
                    }
                },
                isLoading = isLoading,
                placeholder = settings.t(
                    en = "Type a message...",
                    vi = "Nhập tin nhắn...",
                    fr = "Écrire un message...",
                    ja = "メッセージを入力...",
                    ko = "메시지 입력...",
                    zh = "输入消息..."
                ),
                isDark = isDark,
                cardColor = cardColor,
                textColor = textColor
            )
        }
    }
}

// ── Chat Header ──

@Composable
private fun ChatHeader(
    onClose: () -> Unit,
    title: String,
    subtitle: String,
    isDark: Boolean,
    cardColor: Color,
    textColor: Color
) {
    Surface(
        color = cardColor,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(PrimaryBlue),
                contentAlignment = Alignment.Center
            ) {
                Text("🤖", fontSize = 20.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = if (isDark) Color(0xFFB0B0B0) else Color.Gray
                )
            }

            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = if (isDark) Color.White else Color.Gray
                )
            }
        }
    }
}

// ── Chat Bubble ──

@Composable
private fun ChatBubble(
    message: ChatMessage,
    botBubbleColor: Color,
    botBubbleTextColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!message.isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(PrimaryBlue),
                contentAlignment = Alignment.Center
            ) {
                Text("🤖", fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (message.isUser) 16.dp else 4.dp,
                        bottomEnd = if (message.isUser) 4.dp else 16.dp
                    )
                )
                .background(if (message.isUser) ChatBubbleUser else botBubbleColor)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = message.text,
                color = if (message.isUser) Color.White else botBubbleTextColor,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}

// ── Typing Indicator ──

@Composable
private fun TypingIndicator(
    botBubbleColor: Color,
    typingText: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(PrimaryBlue),
            contentAlignment = Alignment.Center
        ) {
            Text("🤖", fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp))
                .background(botBubbleColor)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text(typingText, color = Color.Gray, fontSize = 14.sp)
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
    placeholder: String,
    isDark: Boolean,
    cardColor: Color,
    textColor: Color
) {
    Surface(
        color = cardColor,
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp),
                placeholder = {
                    Text(placeholder, color = if (isDark) Color(0xFF888888) else Color.Gray, fontSize = 14.sp)
                },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = if (isDark) Color(0xFF333333) else Color(0xFFE0E0E0),
                    focusedContainerColor = if (isDark) Color(0xFF121212) else Color.White,
                    unfocusedContainerColor = if (isDark) Color(0xFF1C1C1C) else Color(0xFFF5F5F5),
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor
                ),
                singleLine = false,
                maxLines = 4
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onSend,
                enabled = value.isNotBlank() && !isLoading,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (value.isNotBlank() && !isLoading) PrimaryBlue
                        else Color(0xFFB0BEC5)
                    )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
