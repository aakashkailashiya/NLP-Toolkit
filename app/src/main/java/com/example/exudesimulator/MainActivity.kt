package com.example.exudesimulator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.exudesimulator.ui.history.HistoryScreen
import com.example.exudesimulator.ui.history.HistoryViewModel
import com.example.exudesimulator.ui.settings.SettingsScreen
import com.example.exudesimulator.ui.theme.ExudeSimulatorTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// --- 1. NLP Simulation Data and Logic (Kotlin Equivalent) ---

// Define the mock word lists
private val STOP_WORDS = setOf(
    "a", "an", "and", "are", "as", "at", "be", "by", "for", "from", "has", "he", "in", "is", "it", "its",
    "of", "on", "that", "the", "to", "was", "were", "will", "with", "you", "your", "this", "they", "their",
    "who", "what", "where", "when", "why", "how", "about", "above", "after", "again", "against", "all", "any",
    "because", "before", "being", "below", "between", "both", "but", "can", "could", "did", "do", "does",
    "doing", "down", "during", "each", "few", "further", "had", "have", "having", "here", "into", "me", "more",
    "most", "my", "nor", "not", "now", "once", "only", "or", "other", "our", "out", "over", "own", "same",
    "she", "should", "so", "some", "such", "than", "then", "there", "those", "through", "thus", "till",
    "time", "under", "until", "up", "upon", "us", "very", "we", "while", "whom", "why", "would", "yet"
)

private val SWEAR_WORDS = setOf(
    "badword", "damn", "hell", "crap", "stupid", "idiot"
)

// Simple mock keywords for simulation
private val COMMON_KEYWORDS = listOf(
    "technology", "development", "innovation", "report", "findings", "authorities", "foxes", "running", "jumping"
)

data class NlpResult(
    val title: String,
    val description: String,
    val useCase: String,
    val content: @Composable () -> Unit,
    val color: Color
)

// Helper to clean and tokenize text
private fun getWords(text: String): List<String> {
    // Regex to remove most punctuation and split by whitespace
    return text.lowercase().replace(Regex("[.,/#!$%^&*;:{}=\\-_`~()\\[\\]]"), "").split(Regex("\\s+")).filter { it.isNotBlank() }
}

private fun getWordsPreservingCase(text: String): List<String> {
    return text.replace(Regex("[.,/#!$%^&*;:{}=\\-_`~()\\[\\]]"), "").split(Regex("\\s+")).filter { it.isNotBlank() }
}

// 1. Stop Word Filtering with highlighting
private fun filterStopWords(text: String): NlpResult {
    val originalWords = getWordsPreservingCase(text)
    val lowerCaseWords = getWords(text)
    var filteredCount = 0

    val annotatedString = buildAnnotatedString {
        for (i in originalWords.indices) {
            val originalWord = originalWords[i]
            val lowerWord = lowerCaseWords[i]
            if (STOP_WORDS.contains(lowerWord)) {
                append(" ")
                withStyle(style = SpanStyle(background = Color(0xFFFBD38D), color = Color.Black, fontWeight = FontWeight.Bold)) {
                    append(originalWord)
                }
                filteredCount++
            } else {
                append(" $originalWord")
            }
        }
    }

    return NlpResult(
        title = "Stop Words Filtering",
        description = "यह सुविधा टेक्स्ट से सामान्य, कम-महत्वपूर्ण शब्दों को हटा देती है।",
        useCase = "सर्च इंजन की परफॉर्मेंस सुधारने, डेटा एनालिसिस के लिए टेक्स्ट को प्री-प्रोसेस करने, और टेक्स्ट की \"Noise\" को कम करने के लिए उपयोगी।\n(${filteredCount} Stop Words हटाए गए/हाइलाइट किए गए)",
        content = { Text(annotatedString, modifier = Modifier.padding(top = 8.dp)) },
        color = Color(0xFF6366F1) // Indigo
    )
}

// 2. Simple Stemming with highlighting
private fun simpleStem(text: String): NlpResult {
    val originalWords = getWordsPreservingCase(text)
    val lowerCaseWords = getWords(text)

    val annotatedString = buildAnnotatedString {
        for (i in originalWords.indices) {
            val originalWord = originalWords[i]
            val lowerWord = lowerCaseWords[i]
            var stemmedWord = lowerWord

            // Simple stemming rules
            if (lowerWord.length > 5 && lowerWord.endsWith("ies")) {
                stemmedWord = lowerWord.substring(0, lowerWord.length - 3) + "y"
            } else if (lowerWord.length > 3 && lowerWord.endsWith("es")) {
                stemmedWord = lowerWord.substring(0, lowerWord.length - 2)
            } else if (lowerWord.length > 2 && lowerWord.endsWith("s") && !STOP_WORDS.contains(lowerWord)) {
                stemmedWord = lowerWord.substring(0, lowerWord.length - 1)
            }
            if (lowerWord.length > 4 && lowerWord.endsWith("ing")) {
                stemmedWord = lowerWord.substring(0, lowerWord.length - 3)
            }
            if (lowerWord.length > 3 && lowerWord.endsWith("ed")) {
                stemmedWord = lowerWord.substring(0, lowerWord.length - 2)
            }
            if (lowerWord.length > 4 && lowerWord.endsWith("ly")) {
                stemmedWord = lowerWord.substring(0, lowerWord.length - 2)
            }

            append(" ")
            if (stemmedWord != lowerWord) {
                withStyle(style = SpanStyle(background = Color(0xFF90CDF4), color = Color.Black, fontWeight = FontWeight.Bold)) {
                    append(originalWord)
                }
                append(" (stem: $stemmedWord)")
            } else {
                append(originalWord)
            }
        }
    }

    return NlpResult(
        title = "Stemming",
        description = "यह शब्दों से प्रत्यय ('-ing', '-ed', '-s') को हटाकर उनके मूल रूप (stem) को निकालने का प्रयास करता है।",
        useCase = "सर्च इंजन के लिए समानार्थक शब्दों को मैच करने, टेक्स्ट क्लासिफिकेशन, और सूचना पुनर्प्राप्ति के लिए।",
        content = { Text(annotatedString, modifier = Modifier.padding(top = 8.dp)) },
        color = Color(0xFF48BB78) // Green
    )
}

// 3. Swear Word Check with highlighting
private fun checkSwearWords(text: String): NlpResult {
    val originalWords = getWordsPreservingCase(text)
    val lowerCaseWords = getWords(text)
    var foundCount = 0
    val foundWords = mutableSetOf<String>()

    val annotatedString = buildAnnotatedString {
        for (i in originalWords.indices) {
            val originalWord = originalWords[i]
            val lowerWord = lowerCaseWords[i]
            append(" ")
            if (SWEAR_WORDS.contains(lowerWord)) {
                withStyle(style = SpanStyle(background = Color(0xFFFEB2B2), color = Color.Black, fontWeight = FontWeight.Bold)) {
                    append(originalWord)
                }
                foundCount++
                foundWords.add(originalWord.lowercase())
            } else {
                append(originalWord)
            }
        }
    }

    val summary = if (foundCount > 0) {
        "**$foundCount अपशब्द पाए गए!**\n\nपाए गए शब्द: ${foundWords.joinToString(", ")}"
    } else {
        "टेक्स्ट में कोई अपशब्द नहीं पाया गया।"
    }

    return NlpResult(
        title = "अपशब्दों की जाँच",
        description = "यह सुविधा टेक्स्ट में आपत्तिजनक या अपशब्दों की उपस्थिति की जाँच करती है।",
        useCase = "ऑनलाइन कंटेंट मॉडरेशन, कमेंट फ़िल्टरिंग, बच्चों के लिए सुरक्षित ब्राउज़िंग सुनिश्चित करने, और ब्रांड की प्रतिष्ठा बनाए रखने के लिए।\n$summary",
        content = { Text(annotatedString, modifier = Modifier.padding(top = 8.dp)) },
        color = Color(0xFFF56565) // Red
    )
}

// 4. Simulated Summary & Keywords
private fun generateSummaryKeywords(text: String): NlpResult {
    val words = getWords(text)
    val sentences = text.split(Regex("[.!?]+")).filter { it.isNotBlank() && it.trim().isNotEmpty() }
    val extractedKeywords = words.filter { COMMON_KEYWORDS.contains(it) && !STOP_WORDS.contains(it) }
    val uniqueKeywords = extractedKeywords.distinct().take(5)

    var summaryText = "विश्लेषण किया गया टेक्स्ट विभिन्न विषयों पर केंद्रित है। यह टेक्स्ट के मुख्य विचार को संक्षेप में प्रस्तुत करता है।"
    if (sentences.isNotEmpty()) {
        summaryText = "विश्लेषण किया गया टेक्स्ट ${sentences.first().trim().lowercase()} जैसी जानकारी से संबंधित है। यह तकनीकी विकास और निष्कर्षों की रिपोर्टिंग पर प्रकाश डालता है।"
    }

    val keywordAnnotatedString = buildAnnotatedString {
        uniqueKeywords.forEachIndexed { index, keyword ->
            if (index > 0) append(", ")
            withStyle(style = SpanStyle(background = Color(0xFFA7F3D0), color = Color.Black, fontWeight = FontWeight.Bold)) {
                append(keyword)
            }
        }
        if (uniqueKeywords.isEmpty()) {
            append("कोई विशिष्ट कीवर्ड नहीं मिला (सिम्युलेटेड)।")
        }
    }

    val content: @Composable () -> Unit = {
        Column(modifier = Modifier.padding(top = 8.dp)) {
            Text("सारांश:", fontWeight = FontWeight.Bold, color = Color(0xFF805AD5))
            Text(summaryText, modifier = Modifier.padding(bottom = 8.dp))
            Text("कीवर्ड:", fontWeight = FontWeight.Bold, color = Color(0xFF805AD5))
            Text(keywordAnnotatedString)
        }
    }

    return NlpResult(
        title = "सारांश & कीवर्ड (Simulated)",
        description = "यह सुविधा (सिम्युलेटेड) एक लंबे टेक्स्ट का संक्षिप्त सारांश प्रदान करती है और मुख्य कीवर्ड्स निकालती है।",
        useCase = "दस्तावेज़ों को जल्दी समझने, कंटेंट इंडेक्सिंग, टेक्स्ट क्लासिफिकेशन, और बड़े डेटासेट से मुख्य जानकारी निकालने के लिए।",
        content = content,
        color = Color(0xFF805AD5) // Purple
    )
}

// --- 2. Jetpack Compose UI Implementation ---

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val systemInDarkTheme = isSystemInDarkTheme()
            val isDarkTheme = remember { mutableStateOf(systemInDarkTheme) }
            ExudeSimulatorTheme(darkTheme = isDarkTheme.value) {
                RootNavigator(isDarkTheme = isDarkTheme.value, onThemeChange = { isDarkTheme.value = it })
            }
        }
    }
}

@Composable
fun RootNavigator(isDarkTheme: Boolean, onThemeChange: (Boolean) -> Unit) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
            ExudeSimulatorApp(navController = navController)
        }
        composable("settings") {
            SettingsScreen(
                isDarkTheme = isDarkTheme,
                onThemeChange = onThemeChange,
                onNavigateUp = { navController.popBackStack() }
            )
        }
        composable("history") {
            HistoryScreen(navController = navController)
        }
    }
}


// Define available tabs
enum class NlpTab(val label: String, val action: (String) -> NlpResult) {
    STOP_WORDS("Stop Words", ::filterStopWords),
    STEMMING("Stemming", ::simpleStem),
    SWEAR_WORDS("अपशब्द", ::checkSwearWords),
    SUMMARY_KEYWORDS("सारांश & कीवर्ड", ::generateSummaryKeywords)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExudeSimulatorApp(navController: NavController, historyViewModel: HistoryViewModel = viewModel()) {
    // State management
    var inputText by remember {
        mutableStateOf(
            "The quick brown foxes are running and jumping quickly. They reported their findings to the authorities. This is a very badword. The company is developing new technologies and innovations. Everyone should know about this."
        )
    }
    var activeTab by remember { mutableStateOf(NlpTab.STOP_WORDS) }
    var result by remember { mutableStateOf<NlpResult?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    // Coroutine scope for simulated delay
    val coroutineScope = rememberCoroutineScope()

    // Function to run the processing
    fun runProcessing() {
        if (inputText.isBlank()) {
            result = NlpResult(
                title = "त्रुटि",
                description = "कृपया कुछ टेक्स्ट दर्ज करें।",
                useCase = "",
                content = { Text("इनपुट टेक्स्ट खाली नहीं होना चाहिए।", color = Color.Red) },
                color = Color.Red
            )
            return
        }

        isLoading = true
        result = null // Clear previous result
        coroutineScope.launch {
            delay(700) // Simulate network/API delay
            result = activeTab.action(inputText)
            isLoading = false
        }
    }

    // Run processing automatically on initial load for the default tab
    LaunchedEffect(key1 = Unit) {
        runProcessing()
    }

    // Main Layout
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Exude API NLP Toolkit") },
                actions = {
                    IconButton(onClick = { navController.navigate("history") }) {
                        Icon(Icons.Default.History, contentDescription = "History")
                    }
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Exude NLP Toolkit",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "यह एक उन्नत टूल है जो Natural Language Processing की विभिन्न क्षमताओं को दर्शाता है।",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Input Text Field
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { newText -> inputText = newText },
                            label = { Text("अपना टेक्स्ट यहाँ दर्ज करें") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 150.dp),
                            shape = RoundedCornerShape(8.dp)
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            IconButton(onClick = { clipboardManager.setText(AnnotatedString(inputText)) }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                            }
                            IconButton(onClick = { inputText = clipboardManager.getText()?.text ?: "" }) {
                                Icon(Icons.Default.ContentPaste, contentDescription = "Paste")
                            }
                            IconButton(onClick = { historyViewModel.saveText(inputText) }) {
                                Icon(Icons.Default.Save, contentDescription = "Save")
                            }
                        }
                    }
                }
            }

            item {
                // Tabs
                ScrollableTabRow(
                    selectedTabIndex = NlpTab.entries.indexOf(activeTab),
                    edgePadding = 0.dp,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                ) {
                    NlpTab.entries.forEach { tab ->
                        val isSelected = tab == activeTab
                        Tab(
                            selected = isSelected,
                            onClick = {
                                activeTab = tab
                                runProcessing()
                            },
                            text = { Text(tab.label, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal) },
                            selectedContentColor = MaterialTheme.colorScheme.primary,
                            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                // Processing Button
                Button(
                    onClick = ::runProcessing,
                    enabled = !isLoading,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(56.dp)
                        .padding(vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("प्रोसेसिंग शुरू करें", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            item {
                // Result Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 250.dp)
                        .padding(vertical = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "परिणाम: ${activeTab.label}",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // Animated content for result
                        AnimatedContent(
                            targetState = Pair(result, isLoading),
                            label = "Result Animation",
                            transitionSpec = {
                                (fadeIn() + slideInVertically { height -> height / 2 }).togetherWith(fadeOut() + slideOutVertically { height -> -height / 2 })
                            }
                        ) { (currentResult, currentIsLoading) ->
                            if (currentIsLoading) {
                                Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                        Text("विश्लेषण हो रहा है...", modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            } else if (currentResult != null) {
                                ResultContent(currentResult)
                            } else {
                                Text("परिणाम प्रदर्शित होने के लिए 'प्रोसेसिंग शुरू करें' पर क्लिक करें।", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ResultContent(result: NlpResult) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(result.color.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        // Description
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
            Icon(Icons.Default.Info, contentDescription = "Info", tint = result.color.copy(alpha = 0.8f))
            Spacer(Modifier.width(8.dp))
            Text(
                text = result.description,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = result.color.copy(alpha = 0.8f)
            )
        }

        HorizontalDivider(color = result.color.copy(alpha = 0.3f), thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

        // Use Case
        Text("उपयोग के मामले:", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
        Text(
            text = result.useCase,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )

        HorizontalDivider(color = result.color.copy(alpha = 0.3f), thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

        // Processed Text/Content
        Text("प्रोसेस्ड टेक्स्ट/आउटपुट:", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
        Card(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                result.content()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ExudeSimulatorAppPreview() {
    // A simple preview of the app
    ExudeSimulatorTheme {
        ExudeSimulatorApp(navController = rememberNavController())
    }
}
