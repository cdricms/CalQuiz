package dev.cems.calquiz

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kotlin.random.Random
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "main_menu") {
        composable("main_menu") {
            MainMenuScreen(
                context = LocalContext.current,
                onStartGame = { navController.navigate("game") },
                onShowAbout = { navController.navigate("about") }
            )
        }
        composable("game") {
            MathGameScreen(
                context = LocalContext.current,
                onBack = { navController.popBackStack() }
            )
        }
        composable("about") {
            AboutScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@Serializable
data class HighScore(
    val name: String,
    val score: Int,
    val date: String,
    val totalTime: Long
)

@Composable
fun MainMenuScreen(
    context: Context,
    onStartGame: () -> Unit,
    onShowAbout: () -> Unit
) {
    val highScores = remember { mutableStateOf(listOf<HighScore>()) }
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("CalQuizPrefs", Context.MODE_PRIVATE)
        val highScoresJson = prefs.getString("high_scores", null)
        if (highScoresJson != null) {
            highScores.value = Json.decodeFromString<List<HighScore>>(highScoresJson)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onStartGame,
            modifier = Modifier.fillMaxWidth(0.6f)
        ) {
            Text(text = stringResource(R.string.start_game))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onShowAbout,
            modifier = Modifier.fillMaxWidth(0.6f)
        ) {
            Text(text = stringResource(R.string.about))
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = stringResource(R.string.scores),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(highScores.value) { highScore ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.score_name, highScore.name),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = stringResource(R.string.score_value, highScore.score),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = stringResource(R.string.score_date, highScore.date),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = stringResource(R.string.score_time, highScore.totalTime),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MathGameScreen(context: Context, onBack: () -> Unit) {
    val MAX_LIVES = 3
    var equation by remember { mutableStateOf(generateEquation()) }
    var userInput by remember { mutableStateOf(TextFieldValue()) }
    var feedback by remember { mutableStateOf("") }
    var score by remember { mutableStateOf(0) }
    var lives by remember { mutableStateOf(MAX_LIVES) }
    var showDialog by remember { mutableStateOf(false) }
    var playerName by remember { mutableStateOf(TextFieldValue()) }
    var questionStartTime by remember { mutableStateOf(0L) }
    var totalTime by remember { mutableStateOf(0L) }
    var lastHighScore by remember { mutableStateOf<HighScore?>(null) }

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(equation) {
        questionStartTime = System.currentTimeMillis()
    }

    fun saveHighScore(name: String, score: Int, totalTime: Long) {
        if (name.isBlank()) return

        val prefs = context.getSharedPreferences("CalQuizPrefs", Context.MODE_PRIVATE)
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val date = LocalDateTime.now().format(formatter)
        val newHighScore = HighScore(name, score, date, totalTime)

        val highScoresJson = prefs.getString("high_scores", null)
        val highScores = if (highScoresJson != null) {
            Json.decodeFromString<List<HighScore>>(highScoresJson).toMutableList()
        } else {
            mutableListOf()
        }

        highScores.add(newHighScore)
        highScores.sortByDescending { it.score }

        val limitedHighScores = highScores.take(10)

        val editor = prefs.edit()
        editor.putString("high_scores", Json.encodeToString(limitedHighScores))
        editor.apply()

        lastHighScore = newHighScore
    }

    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("CalQuizPrefs", Context.MODE_PRIVATE)
        val highScoresJson = prefs.getString("high_scores", null)
        if (highScoresJson != null) {
            val highScores = Json.decodeFromString<List<HighScore>>(highScoresJson)
            lastHighScore = highScores.maxByOrNull { it.score }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.score_value, score),
                style = MaterialTheme.typography.titleMedium
            )
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(MAX_LIVES - lives) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Lost Life",
                        tint = Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }
                repeat(lives) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Life",
                        tint = Color.Red,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = equation.question,
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = userInput,
                onValueChange = { userInput = it },
                label = { Text(text = stringResource(R.string.answer_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(0.6f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val timeTaken = (System.currentTimeMillis() - questionStartTime) / 1000L
                    val effectiveTime = minOf(timeTaken, 20L)
                    totalTime += effectiveTime

                    val userAnswer = userInput.text.toIntOrNull()
                    if (userAnswer != null && userAnswer == equation.answer) {
                        val multiplier = (20f - effectiveTime) / 20f
                        score += (10 * (1 + multiplier)).toInt()
                        feedback = "Correct!"
                        equation = generateEquation()
                        userInput = TextFieldValue()
                    } else {
                        lives--
                        feedback = "Wrong answer!"
                        if (lives > 0) {
                            equation = generateEquation()
                            userInput = TextFieldValue()
                        } else {
                            if (playerName.text.isNotBlank()) {
                                saveHighScore(playerName.text, score, totalTime)
                            }
                            showDialog = true
                        }
                    }
                }
            ) {
                Text(text = stringResource(R.string.answer_submit))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = feedback,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(0.6f)
            ) {
                Text(text = stringResource(R.string.back_menu))
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Game Over!") },
            text = {
                Column {
                    Text(text = stringResource(R.string.final_score, score))
                    Text(text = stringResource(R.string.final_time, totalTime))
                    lastHighScore?.let { highScore ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = stringResource(R.string.highest_score, highScore.score, highScore.name, highScore.date, highScore.totalTime))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = playerName,
                        onValueChange = { playerName = it },
                        label = { Text(text = stringResource(R.string.name_input)) },
                        singleLine = true,
                        isError = playerName.text.isBlank() && showDialog
                    )
                    if (playerName.text.isBlank()) {
                        Text(
                            text =stringResource(R.string.name_required),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (playerName.text.isNotBlank()) {
                            saveHighScore(playerName.text, score, totalTime)
                            score = 0
                            lives = MAX_LIVES
                            equation = generateEquation()
                            userInput = TextFieldValue()
                            feedback = ""
                            totalTime = 0L
                            playerName = TextFieldValue()
                            showDialog = false
                        }
                    },
                    enabled = playerName.text.isNotBlank()
                ) {
                    Text(text = stringResource(R.string.restart))
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        if (playerName.text.isNotBlank()) {
                            saveHighScore(playerName.text, score, totalTime)
                            score = 0
                            lives = MAX_LIVES
                            equation = generateEquation()
                            userInput = TextFieldValue()
                            feedback = ""
                            totalTime = 0L
                            playerName = TextFieldValue()
                            showDialog = false
                            onBack()
                        }
                    },
                    enabled = playerName.text.isNotBlank()
                ) {
                    Text(text = stringResource(R.string.main_menu))
                }
            }
        )
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.about_header),
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.about_description),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(0.6f)
        ) {
            Text(text = stringResource(R.string.back))
        }
    }
}

data class Equation(val question: String, val answer: Int)

fun generateEquation(): Equation {
    val num1 = Random.nextInt(1, 20)
    val num2 = Random.nextInt(1, 20)
    val operator = listOf("+", "-", "*", "/").random()

    return when (operator) {
        "+" -> Equation("$num1 + $num2 = ?", num1 + num2)
        "-" -> Equation("$num1 - $num2 = ?", num1 - num2)
        "*" -> Equation("$num1 * $num2 = ?", num1 * num2)
        "/" -> Equation("$num1 / $num2 = ?", num1 / num2)
        else -> Equation("$num1 + $num2 = ?", num1 + num2)
    }
}