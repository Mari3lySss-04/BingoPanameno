package com.example.bingopanameno

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.res.fontResource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp

import java.util.*

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)

        val generatedUid = generateUID()

        setContent {
            var uid by remember { mutableStateOf(generatedUid) }
            BingoApp(uid)
        }
    }

    override fun onDestroy() {
        tts?.shutdown()
        super.onDestroy()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale("es", "ES")
        }
    }

    @Composable
    fun BingoApp(initialUid: String) {
        var screen by remember { mutableStateOf("initial") }
        var dimension by remember { mutableStateOf(5) }
        var uid by remember { mutableStateOf(initialUid) }
        var bingoNumbers by remember { mutableStateOf(generateNumbers(dimension)) }
        var resetTrigger by remember { mutableStateOf(0) }

        if (screen == "initial") {
            InitialScreen(
                dimension = dimension,
                onDimensionChange = {
                    dimension = it.coerceIn(1, 9)
                },
                uid = uid,
                onGenerateBingo = {
                    bingoNumbers = generateNumbers(dimension)
                    resetTrigger++
                    screen = "bingo"
                }
            )
        } else {
            BingoScreen(
                numbers = bingoNumbers,
                dimension = dimension,
                onRegenerate = {
                    bingoNumbers = generateNumbers(dimension)
                    resetTrigger++
                },
                onBingo = {
                    HandlerBingo(this, tts)
                },
                resetTrigger = resetTrigger
            )
        }
    }

    fun generateNumbers(n: Int): MutableList<MutableList<String>> {
        val numbers = (1..81).shuffled().take(n * n).toMutableList()
        return MutableList(n) {
            MutableList(n) {
                numbers.removeAt(0).toString()
            }
        }
    }
}

fun HandlerBingo(context: android.content.Context, tts: TextToSpeech?) {
    Toast.makeText(context, "¡BINGO!", Toast.LENGTH_LONG).show()
    tts?.speak("¡Bingo!", TextToSpeech.QUEUE_FLUSH, null, null)
}

@Composable
fun InitialScreen(
    dimension: Int,
    onDimensionChange: (Int) -> Unit,
    uid: String,
    onGenerateBingo: () -> Unit
) {

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White


    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.icon),
                contentDescription = "Bot Avatar",
                modifier = Modifier
                    .size(180.dp)
                    .padding(top = 2.dp, bottom = 2.dp)
                    .clip(RoundedCornerShape(50.dp))
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text("Tu ID de jugador es:", fontSize = 16.sp)
            Text(uid, fontSize = 20.sp, color = Color(0xFF673AB7))
            Spacer(modifier = Modifier.height(24.dp))

            var inputText by remember { mutableStateOf("") }

            OutlinedTextField(
                value = inputText,
                onValueChange = {
                    inputText = it.filter { char -> char.isDigit() }
                    val dim = inputText.toIntOrNull()
                    if (dim != null && dim in 1..9) {
                        onDimensionChange(dim)
                    }
                },
                label = { Text("Ingrese la dimensión (1 a 9)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onGenerateBingo) {
                Text("Generar Bingo")
            }
        }
    }
}

@Composable
fun BingoScreen(
    numbers: MutableList<MutableList<String>>,
    dimension: Int,
    onRegenerate: () -> Unit,
    onBingo: () -> Unit,
    resetTrigger: Int
) {
    val marked = remember(resetTrigger) { mutableStateMapOf<Pair<Int, Int>, Boolean>() }


    Box(modifier = Modifier.fillMaxSize()) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical=46.dp, horizontal = 8.dp)
        ) {
            Text(
                text = "Juego de Bingo!",
                fontSize = 40.sp,
                color = Color.Black,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .padding(horizontal = 30.dp, vertical = 6.dp)
                    .border(width = 2.dp,
                        color = Color.Black,
                        shape = RectangleShape)
                    .padding(horizontal = 12.dp, vertical = 8.dp) // Espacio interno dentro del borde
            )

            Spacer(modifier = Modifier.height(8.dp))

            val circleSize = (320f / dimension).dp
            val fontSize = (circleSize.value * 0.20f).sp

            for ((rowIndex, row) in numbers.withIndex()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    for ((colIndex, num) in row.withIndex()) {
                        val isMarked = marked[Pair(rowIndex, colIndex)] == true

                        Box(
                            modifier = Modifier
                                .size(circleSize)
                                .border(
                                    width = 2.dp,
                                    color = if (isMarked) Color(0xFF800080) else Color.Red,
                                    shape = CircleShape
                                )
                                .background(
                                    color = if (isMarked) Color.Blue else Color(0xFF673AB7),
                                    shape = CircleShape
                                )
                                .clickable {
                                    if (!isMarked) {
                                        marked[Pair(rowIndex, colIndex)] = true
                                        onBingoCheckFromMarked(dimension, marked, onBingo)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = num,
                                color = Color.White,
                                fontSize = fontSize,
                                textAlign = TextAlign.Center
                            )
                        }

                    }
                    }
                }


            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRegenerate) {
                Text("Regenerar Carta")
            }
        }
    }
}

// verifica el estado de los botones marcados
fun onBingoCheckFromMarked(
    size: Int,
    marked: Map<Pair<Int, Int>, Boolean>,
    onBingo: () -> Unit
) {
    // Verificar filas
    for (row in 0 until size) {
        if ((0 until size).all { col -> marked[Pair(row, col)] == true }) {
            onBingo()
            return
        }
    }

    // Verificar columnas
    for (col in 0 until size) {
        if ((0 until size).all { row -> marked[Pair(row, col)] == true }) {
            onBingo()
            return
        }
    }

    // Verificar diagonal principal
    if ((0 until size).all { i -> marked[Pair(i, i)] == true }) {
        onBingo()
        return
    }

    // Verificar diagonal inversa
    if ((0 until size).all { i -> marked[Pair(i, size - i - 1)] == true }) {
        onBingo()
        return
    }

    // Verificar cada cuadrado 2x2 en las 4 esquinas
    val cornerSquares = listOf(
        listOf(Pair(0, 0), Pair(0, 1), Pair(1, 0), Pair(1, 1)), // Superior izquierda
        listOf(Pair(0, size - 2), Pair(0, size - 1), Pair(1, size - 2), Pair(1, size - 1)), // Superior derecha
        listOf(Pair(size - 2, 0), Pair(size - 2, 1), Pair(size - 1, 0), Pair(size - 1, 1)), // Inferior izquierda
        listOf(Pair(size - 2, size - 2), Pair(size - 2, size - 1), Pair(size - 1, size - 2), Pair(size - 1, size - 1)) // Inferior derecha
    )

    for (square in cornerSquares) {
        if (square.all { marked[it] == true }) {
            onBingo()
            return
        }
    }


}

fun generateUID(): String {
    val letters = listOf("AE", "BC", "DE", "FG", "HI")
    val suffix = (0..99).random().toString().padStart(2, '0')
    val last = listOf("P", "X", "Y", "Z").random()
    return "${letters.random()}x$suffix$last"
}
