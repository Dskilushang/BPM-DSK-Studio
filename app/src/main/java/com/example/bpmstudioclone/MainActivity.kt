package com.example.bpmstudioclone

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object AudioBridge {
    init { System.loadLibrary("bpmengine") }
    external fun startEngine()
    external fun setCrossfader(value: Float)
    external fun loadTrackA(data: FloatArray)
    external fun toggleDeckA(play: Boolean)
    external fun setPitchA(pitch: Float)
    external fun cueDeckA()
    external fun loadTrackB(data: FloatArray)
    external fun toggleDeckB(play: Boolean)
    external fun setPitchB(pitch: Float)
    external fun cueDeckB()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AudioBridge.startEngine()
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF121212)) {
                    BPMStudioScreen()
                }
            }
        }
    }
}

@Composable
fun BPMStudioScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var crossfaderValue by remember { mutableFloatStateOf(0.5f) }
    var isPlayingA by remember { mutableStateOf(false) }
    var isPlayingB by remember { mutableStateOf(false) }
    var pitchA by remember { mutableFloatStateOf(1.0f) }
    var pitchB by remember { mutableFloatStateOf(1.0f) }
    var trackNameA by remember { mutableStateOf("Aucune piste") }
    var trackNameB by remember { mutableStateOf("Aucune piste") }

    val pickerA = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            trackNameA = "Chargement..."
            isPlayingA = false
            scope.launch(Dispatchers.IO) {
                val pcm = AudioDecoder.decodeToPcmFloat(context, it)
                AudioBridge.loadTrackA(pcm)
                withContext(Dispatchers.Main) {
                    trackNameA = it.lastPathSegment ?: "Piste A chargée"
                }
            }
        }
    }

    val pickerB = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            trackNameB = "Chargement..."
            isPlayingB = false
            scope.launch(Dispatchers.IO) {
                val pcm = AudioDecoder.decodeToPcmFloat(context, it)
                AudioBridge.loadTrackB(pcm)
                withContext(Dispatchers.Main) {
                    trackNameB = it.lastPathSegment ?: "Piste B chargée"
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text("BPM-DSK-STUDIO", color = Color(0xFFFF9800), fontWeight = FontWeight.Bold, fontSize = 20.sp)

        Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DeckView(
                title = "DECK A",
                trackName = trackNameA,
                isPlaying = isPlayingA,
                pitch = pitchA,
                onLoadClick = { pickerA.launch("audio/*") },
                onPlayToggle = {
                    isPlayingA = !isPlayingA
                    AudioBridge.toggleDeckA(isPlayingA)
                },
                onCueClick = {
                    isPlayingA = false
                    AudioBridge.cueDeckA()
                },
                onPitchChange = {
                    pitchA = it
                    AudioBridge.setPitchA(it)
                },
                modifier = Modifier.weight(1f)
            )

            DeckView(
                title = "DECK B",
                trackName = trackNameB,
                isPlaying = isPlayingB,
                pitch = pitchB,
                onLoadClick = { pickerB.launch("audio/*") },
                onPlayToggle = {
                    isPlayingB = !isPlayingB
                    AudioBridge.toggleDeckB(isPlayingB)
                },
                onCueClick = {
                    isPlayingB = false
                    AudioBridge.cueDeckB()
                },
                onPitchChange = {
                    pitchB = it
                    AudioBridge.setPitchB(it)
                },
                modifier = Modifier.weight(1f)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
            Text("CROSSFADER", color = Color.Gray, fontSize = 12.sp)
            Slider(
                value = crossfaderValue,
                onValueChange = {
                    crossfaderValue = it
                    AudioBridge.setCrossfader(it)
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun DeckView(
    title: String,
    trackName: String,
    isPlaying: Boolean,
    pitch: Float,
    onLoadClick: () -> Unit,
    onPlayToggle: () -> Unit,
    onCueClick: () -> Unit,
    onPitchChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        modifier = modifier.fillMaxHeight()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(title, color = Color.White, fontWeight = FontWeight.Bold)
                    OutlinedButton(onClick = onLoadClick, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                        Text("LOAD", fontSize = 11.sp, color = Color(0xFFFF9800))
                    }
                }
                Text(trackName, color = Color(0xFF00E676), fontSize = 12.sp, maxLines = 1)
            }

            Column {
                Text("Pitch: ${String.format("%.1f", (pitch - 1f) * 100)}%", color = Color.LightGray, fontSize = 12.sp)
                Slider(value = pitch, onValueChange = onPitchChange, valueRange = 0.92f..1.08f)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onPlayToggle,
                    colors = ButtonDefaults.buttonColors(containerColor = if (isPlaying) Color(0xFF00E676) else Color(0xFF333333)),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isPlaying) "PAUSE" else "PLAY", color = Color.Black, fontSize = 12.sp)
                }
                Button(
                    onClick = onCueClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("CUE", color = Color.Black, fontSize = 12.sp)
                }
            }
        }
    }
}
