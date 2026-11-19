import androidx.compose.ui.text.font.FontWeight
import java.util.Locale
@Composable
fun PlayerUnit(
    modifier: Modifier,
    playerLabel: String,
    trackTitle: String,
    isPlaying: Boolean,
    position: Float,
    elapsedSec: Double,
    level: Float,
    onPlayPause: () -> Unit,
    onCue: () -> Unit,
    onCup: () -> Unit,
    onSeek: (Float) -> Unit,
    onPitchChange: (Float) -> Unit,
    onPitchEnabled: (Boolean) -> Unit,
    onPitchBend: (Float) -> Unit,
    onLoopIn: () -> Unit,
    onLoopOut: () -> Unit,
    onReloop: () -> Unit
) {
    var pitchPercent by remember { mutableFloatStateOf(0f) }
    var pitchOn by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(PanelMetal, RoundedCornerShape(4.dp))
            .border(1.dp, Color(0xFF3E4452), RoundedCornerShape(4.dp))
            .padding(5.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(LcdBlueBg, RoundedCornerShape(3.dp))
                .border(1.dp, Color(0xFF1C4E80), RoundedCornerShape(3.dp))
                .padding(horizontal = 6.dp, vertical = 3.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = playerLabel,
                    color = LcdCyan,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatTime(elapsedSec),
                    color = LcdCyan,
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = String.format(Locale.US, "%+.1f%%", pitchPercent),
                    color = LcdRed,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = trackTitle,
                color = Color.White,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .padding(top = 2.dp)
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val norm = (offset.x / size.width).coerceIn(0f, 1f)
                            onSeek(norm)
                        }
                    }
            ) {
                drawRect(Color(0xFF0A2A4A), size = size)
                val barCount = 28
                val barW = size.width / barCount
                for (i in 0 until barCount) {
                    val active = (i.toFloat() / barCount) <= position
                    val amp = if (isPlaying) (0.25f + (level * 0.75f * ((i % 4 + 1) / 4f))) else 0.2f
                    val h = size.height * amp.coerceIn(0.15f, 1f)
                    drawRect(
                        color = if (active) LcdCyan else Color(0xFF1D4E7A),
                        topLeft = Offset(i * barW + 1f, size.height - h),
                        size = Size(barW - 2f, h)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MiniHardwareBtn("IN", Color(0xFF333945), Modifier.weight(1f), onLoopIn)
            MiniHardwareBtn("OUT", Color(0xFF333945), Modifier.weight(1f), onLoopOut)
            MiniHardwareBtn("REL", BtnAmber, Modifier.weight(1f), onReloop)
            MiniHardwareBtn("BEND-", Color(0xFF2B303A), Modifier.weight(1f)) {
                onPitchBend(-0.04f)
            }
            MiniHardwareBtn("BEND+", Color(0xFF2B303A), Modifier.weight(1f)) {
                onPitchBend(0.04f)
            }
            MiniHardwareBtn("0%", Color(0xFF2B303A), Modifier.weight(0.9f)) {
                onPitchBend(0f)
                pitchPercent = 0f
                onPitchChange(1.0f)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(if (pitchOn) BtnAmber else Color(0xFF333945), RoundedCornerShape(3.dp))
                    .clickable {
                        pitchOn = !pitchOn
                        onPitchEnabled(pitchOn)
                    }
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text("PITCH", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = pitchPercent,
                onValueChange = {
                    pitchPercent = it
                    onPitchChange(1.0f + (it / 100f))
                },
                valueRange = -12f..12f,
                modifier = Modifier
                    .weight(1f)
                    .height(20.dp),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = LcdCyan,
                    inactiveTrackColor = Color(0xFF111317)
                )
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            TransportButton(
                label = "CUP",
                bgColor = Color(0xFF353B48),
                modifier = Modifier.weight(0.8f),
                onClick = onCup
            )
            TransportButton(
                label = "CUE",
                bgColor = BtnRed,
                modifier = Modifier.weight(1f),
                onClick = onCue
            )
            TransportButton(
                label = if (isPlaying) "PAUSE" else "PLAY",
                bgColor = BtnGreen,
                modifier = Modifier.weight(1.2f),
                onClick = onPlayPause
            )
        }
    }
}

@Composable
fun TransportButton(
    label: String,
    bgColor: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(bgColor, RoundedCornerShape(4.dp))
            .border(1.dp, Color.Black, RoundedCornerShape(4.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}
@Composable
fun CenterFadeModule(
    modifier: Modifier,
    crossfader: Float,
    fadeTime: Float,
    onCrossfaderChange: (Float) -> Unit,
    onFadeTimeChange: (Float) -> Unit,
    onAutoFade: () -> Unit
) {
    Column(
        modifier = modifier
            .background(PanelMetal, RoundedCornerShape(4.dp))
            .border(1.dp, Color(0xFF3E4452), RoundedCornerShape(4.dp))
            .padding(5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text("CROSS FADE", color = Color.LightGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(LcdBlueBg, RoundedCornerShape(3.dp))
                .border(1.dp, Color(0xFF1C4E80), RoundedCornerShape(3.dp))
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = String.format(Locale.US, "%.1f s", fadeTime),
                color = LcdCyan,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("FADE TIME", color = Color.Gray, fontSize = 8.sp)
            Slider(
                value = fadeTime,
                onValueChange = onFadeTimeChange,
                valueRange = 1f..15f,
                modifier = Modifier.height(20.dp),
                colors = SliderDefaults.colors(thumbColor = LcdCyan, activeTrackColor = LcdCyan)
            )
        }
        Button(
            onClick = onAutoFade,
            colors = ButtonDefaults.buttonColors(containerColor = BtnAmber),
            contentPadding = PaddingValues(0.dp),
            shape = RoundedCornerShape(3.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
        ) {
            Text("AUTO FADE", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("MANUAL FADE", color = Color.Gray, fontSize = 8.sp)
            Slider(
                value = crossfader,
                onValueChange = onCrossfaderChange,
                valueRange = 0f..1f,
                modifier = Modifier.height(22.dp),
                colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = BtnAmber)
            )
        }
    }
}

@Composable
fun PlaylistPanel(
    playlist: List<PlaylistItem>,
    onAddFiles: () -> Unit,
    onClear: () -> Unit,
    onLoadA: (PlaylistItem) -> Unit,
    onLoadB: (PlaylistItem) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("PLAYLIST (${playlist.size})", color = LcdCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SmallDeckLoadBtn("+ ADD AUDIO", BtnGreen, onAddFiles)
                SmallDeckLoadBtn("CLEAR", BtnRed, onClear)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            itemsIndexed(playlist) { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1A1E26), RoundedCornerShape(3.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${index + 1}. ${item.title}",
                        color = Color.White,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    SmallDeckLoadBtn("LOAD A", LcdCyan) { onLoadA(item) }
                    SmallDeckLoadBtn("LOAD B", BtnAmber) { onLoadB(item) }
                }
            }
        }
    }
}

@Composable
fun MixerEqPanel() {
    var volA by remember { mutableFloatStateOf(0.85f) }
    var volB by remember { mutableFloatStateOf(0.85f) }
    var masterVol by remember { mutableFloatStateOf(0.9f) }

    var lowA by remember { mutableFloatStateOf(1f) }
    var midA by remember { mutableFloatStateOf(1f) }
    var highA by remember { mutableFloatStateOf(1f) }

    var lowB by remember { mutableFloatStateOf(1f) }
    var midB by remember { mutableFloatStateOf(1f) }
    var highB by remember { mutableFloatStateOf(1f) }

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("DECK A EQ & VOL", color = LcdCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            EqSlider("LOW", lowA) { lowA = it; AudioBridge.setEqA(lowA, midA, highA) }
            EqSlider("MID", midA) { midA = it; AudioBridge.setEqA(lowA, midA, highA) }
            EqSlider("HIGH", highA) { highA = it; AudioBridge.setEqA(lowA, midA, highA) }
            EqSlider("VOL", volA) { volA = it; AudioBridge.setVolumeA(volA) }
        }

        Column(
            modifier = Modifier.width(110.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("MASTER", color = BtnAmber, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Slider(
                value = masterVol,
                onValueChange = { masterVol = it; AudioBridge.setMasterVolume(it) },
                valueRange = 0f..1.2f,
                colors = SliderDefaults.colors(thumbColor = BtnAmber, activeTrackColor = BtnAmber)
            )
            Text(
                text = String.format(Locale.US, "%.0f%%", masterVol * 100f),
                color = Color.White,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text("DECK B EQ & VOL", color = BtnAmber, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            EqSlider("LOW", lowB) { lowB = it; AudioBridge.setEqB(lowB, midB, highB) }
            EqSlider("MID", midB) { midB = it; AudioBridge.setEqB(lowB, midB, highB) }
            EqSlider("HIGH", highB) { highB = it; AudioBridge.setEqB(lowB, midB, highB) }
            EqSlider("VOL", volB) { volB = it; AudioBridge.setVolumeB(volB) }
        }
    }
}

@Composable
private fun EqSlider(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.LightGray, fontSize = 8.sp, modifier = Modifier.width(28.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..1.5f,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = LcdCyan)
        )
    }
}

@Composable
fun SamplerPanel(onTriggerSample: (String, FloatArray) -> Unit) {
    val pads = remember {
        listOf(
            "KICK" to SampleSynth.kick(),
            "SNARE" to SampleSynth.snare(),
            "HI-HAT" to SampleSynth.hiHat(),
            "SIREN" to SampleSynth.siren(),
            "LASER" to SampleSynth.laser(),
            "AIRHORN" to SampleSynth.airhorn(),
            "SUB DROP" to SampleSynth.subDrop(),
            "BEEP" to SampleSynth.beep(),
            "NOISE" to SampleSynth.noiseBurst()
        )
    }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        for (row in 0 until 3) {
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (col in 0 until 3) {
                    val idx = row * 3 + col
                    val (name, data) = pads[idx]
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(Color(0xFF2B3240), RoundedCornerShape(3.dp))
                            .border(1.dp, LcdCyan.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
                            .clickable { onTriggerSample(name, data) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${idx + 1}. $name",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

object SampleSynth {
    private const val SR = 44100

    fun kick(): FloatArray = FloatArray(SR / 3) { i ->
        val t = i.toDouble() / SR
        val freq = 140.0 * exp(-t * 18.0) + 45.0
        (sin(2.0 * PI * freq * t) * exp(-t * 9.0)).toFloat()
    }

    fun snare(): FloatArray = FloatArray(SR / 4) { i ->
        val t = i.toDouble() / SR
        val tone = sin(2.0 * PI * 185.0 * t) * exp(-t * 25.0)
        val noise = Random.nextDouble(-1.0, 1.0) * exp(-t * 14.0)
        ((tone * 0.4 + noise * 0.6)).toFloat()
    }

    fun hiHat(): FloatArray = FloatArray(SR / 10) { i ->
        val t = i.toDouble() / SR
        (Random.nextDouble(-1.0, 1.0) * exp(-t * 45.0) * 0.5).toFloat()
    }

    fun siren(): FloatArray = FloatArray(SR) { i ->
        val t = i.toDouble() / SR
        val freq = 650.0 + 350.0 * sin(2.0 * PI * 3.0 * t)
        (sin(2.0 * PI * freq * t) * 0.5).toFloat()
    }

    fun laser(): FloatArray = FloatArray(SR / 3) { i ->
        val t = i.toDouble() / SR
        val freq = 1800.0 * exp(-t * 7.0) + 160.0
        (sin(2.0 * PI * freq * t) * exp(-t * 5.0) * 0.5).toFloat()
    }

    fun airhorn(): FloatArray = FloatArray((SR * 0.6).toInt()) { i ->
        val t = i.toDouble() / SR
        val f1 = 440.0
        val f2 = 554.37
        val env = ((1.0 - exp(-t * 40.0)) * if (t > 0.45) exp(-(t - 0.45) * 15.0) else 1.0)
        (((sin(2.0 * PI * f1 * t) + sin(2.0 * PI * f2 * t)) * 0.35 * env)).toFloat()
    }

    fun subDrop(): FloatArray = FloatArray((SR * 0.7).toInt()) { i ->
        val t = i.toDouble() / SR
        val freq = 95.0 * exp(-t * 4.5) + 32.0
        (sin(2.0 * PI * freq * t) * exp(-t * 3.0) * 0.7).toFloat()
    }

    fun beep(): FloatArray = FloatArray(SR / 6) { i ->
        val t = i.toDouble() / SR
        (sin(2.0 * PI * 1000.0 * t) * exp(-t * 12.0) * 0.4).toFloat()
    }

    fun noiseBurst(): FloatArray = FloatArray(SR / 5) { i ->
        val t = i.toDouble() / SR
        (Random.nextDouble(-1.0, 1.0) * exp(-t * 10.0) * 0.4).toFloat()
    }
}

object AudioDecoder {
    fun decodeToPcmFloat(context: android.content.Context, uri: Uri): FloatArray? {
        val extractor = android.media.MediaExtractor()
        return try {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
                extractor.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            } ?: return null

            var trackIndex = -1
            var format: android.media.MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val f = extractor.getTrackFormat(i)
                val mime = f.getString(android.media.MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    trackIndex = i
                    format = f
                    break
                }
            }
            if (trackIndex < 0 || format == null) return null

            extractor.selectTrack(trackIndex)
            val mime = format.getString(android.media.MediaFormat.KEY_MIME) ?: return null
            val codec = android.media.MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            val outSamples = ArrayList<Float>(1024 * 64)
            val bufferInfo = android.media.MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false

            while (!outputDone) {
                if (!inputDone) {
                    val inIdx = codec.dequeueInputBuffer(10_000)
                    if (inIdx >= 0) {
                        val inBuf = codec.getInputBuffer(inIdx)
                        val sampleSize = if (inBuf != null) extractor.readSampleData(inBuf, 0) else -1
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(inIdx, 0, 0, 0L, android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            val pts = extractor.sampleTime
                            codec.queueInputBuffer(inIdx, 0, sampleSize, pts, 0)
                            extractor.advance()
                        }
                    }
                }

                val outIdx = codec.dequeueOutputBuffer(bufferInfo, 10_000)
                if (outIdx >= 0) {
                    val outBuf = codec.getOutputBuffer(outIdx)
                    if (outBuf != null && bufferInfo.size > 0) {
                        outBuf.position(bufferInfo.offset)
                        outBuf.limit(bufferInfo.offset + bufferInfo.size)
                        val shortBuf = outBuf.order(java.nio.ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                        while (shortBuf.hasRemaining()) {
                            outSamples.add((shortBuf.get() / 32768f).coerceIn(-1f, 1f))
                        }
                    }
                    codec.releaseOutputBuffer(outIdx, false)
                    if ((bufferInfo.flags and android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        outputDone = true
                    }
                }
            }

            codec.stop()
            codec.release()
            outSamples.toFloatArray()
        } catch (_: Exception) {
            null
        } finally {
            extractor.release()
        }
    }
}

@Composable
fun MiniHardwareBtn(label: String, bgColor: Color, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(20.dp)
            .background(bgColor, RoundedCornerShape(2.dp))
            .border(1.dp, Color.Black, RoundedCornerShape(2.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SmallDeckLoadBtn(label: String, accentColor: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(Color(0xFF1B2838), RoundedCornerShape(2.dp))
            .border(1.dp, accentColor, RoundedCornerShape(2.dp))
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(label, color = accentColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TabButton(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(if (selected) BtnAmber else Color(0xFF2B303A), RoundedCornerShape(3.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) Color.Black else Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun formatTime(seconds: Double): String {
    val totalSec = seconds.toInt().coerceAtLeast(0)
    val mins = totalSec / 60
    val secs = totalSec % 60
    val hundredths = ((seconds - totalSec) * 100).toInt().coerceIn(0, 99)
    return String.format(Locale.US, "%02d:%02d.%02d", mins, secs, hundredths)
}



@Composable
fun PlaylistPanel(
    playlist: List<PlaylistItem>,
    onAddFiles: () -> Unit,
    onClear: () -> Unit,
    onLoadA: (PlaylistItem) -> Unit,
    onLoadB: (PlaylistItem) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("PLAYLIST (${playlist.size})", color = LcdCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SmallDeckLoadBtn("+ ADD AUDIO", BtnGreen, onAddFiles)
                SmallDeckLoadBtn("CLEAR", BtnRed, onClear)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            itemsIndexed(playlist) { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1A1E26), RoundedCornerShape(3.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${index + 1}. ${item.title}",
                        color = Color.White,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    SmallDeckLoadBtn("LOAD A", LcdCyan) { onLoadA(item) }
                    SmallDeckLoadBtn("LOAD B", BtnAmber) { onLoadB(item) }
                }
            }
        }
    }
}

@Composable
fun MixerEqPanel() {
    var volA by remember { mutableFloatStateOf(0.85f) }
    var volB by remember { mutableFloatStateOf(0.85f) }
    var masterVol by remember { mutableFloatStateOf(0.9f) }

    var lowA by remember { mutableFloatStateOf(1f) }
    var midA by remember { mutableFloatStateOf(1f) }
    var highA by remember { mutableFloatStateOf(1f) }

    var lowB by remember { mutableFloatStateOf(1f) }
    var midB by remember { mutableFloatStateOf(1f) }
    var highB by remember { mutableFloatStateOf(1f) }

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("DECK A EQ & VOL", color = LcdCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            EqSlider("LOW", lowA) { lowA = it; AudioBridge.setEqA(lowA, midA, highA) }
            EqSlider("MID", midA) { midA = it; AudioBridge.setEqA(lowA, midA, highA) }
            EqSlider("HIGH", highA) { highA = it; AudioBridge.setEqA(lowA, midA, highA) }
            EqSlider("VOL", volA) { volA = it; AudioBridge.setVolumeA(volA) }
        }

        Column(
            modifier = Modifier.width(110.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("MASTER", color = BtnAmber, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Slider(
                value = masterVol,
                onValueChange = { masterVol = it; AudioBridge.setMasterVolume(it) },
                valueRange = 0f..1.2f,
                colors = SliderDefaults.colors(thumbColor = BtnAmber, activeTrackColor = BtnAmber)
            )
            Text(
                text = String.format(Locale.US, "%.0f%%", masterVol * 100f),
                color = Color.White,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text("DECK B EQ & VOL", color = BtnAmber, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            EqSlider("LOW", lowB) { lowB = it; AudioBridge.setEqB(lowB, midB, highB) }
            EqSlider("MID", midB) { midB = it; AudioBridge.setEqB(lowB, midB, highB) }
            EqSlider("HIGH", highB) { highB = it; AudioBridge.setEqB(lowB, midB, highB) }
            EqSlider("VOL", volB) { volB = it; AudioBridge.setVolumeB(volB) }
        }
    }
}

@Composable
private fun EqSlider(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.LightGray, fontSize = 8.sp, modifier = Modifier.width(28.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..1.5f,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = LcdCyan)
        )
    }
}

@Composable
fun SamplerPanel(onTriggerSample: (String, FloatArray) -> Unit) {
    val pads = remember {
        listOf(
            "KICK" to SampleSynth.kick(),
            "SNARE" to SampleSynth.snare(),
            "HI-HAT" to SampleSynth.hiHat(),
            "SIREN" to SampleSynth.siren(),
            "LASER" to SampleSynth.laser(),
            "AIRHORN" to SampleSynth.airhorn(),
            "SUB DROP" to SampleSynth.subDrop(),
            "BEEP" to SampleSynth.beep(),
            "NOISE" to SampleSynth.noiseBurst()
        )
    }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        for (row in 0 until 3) {
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (col in 0 until 3) {
                    val idx = row * 3 + col
                    val (name, data) = pads[idx]
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(Color(0xFF2B3240), RoundedCornerShape(3.dp))
                            .border(1.dp, LcdCyan.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
                            .clickable { onTriggerSample(name, data) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${idx + 1}. $name",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

object SampleSynth {
    private const val SR = 44100

    fun kick(): FloatArray = FloatArray(SR / 3) { i ->
        val t = i.toDouble() / SR
        val freq = 140.0 * exp(-t * 18.0) + 45.0
        (sin(2.0 * PI * freq * t) * exp(-t * 9.0)).toFloat()
    }

    fun snare(): FloatArray = FloatArray(SR / 4) { i ->
        val t = i.toDouble() / SR
        val tone = sin(2.0 * PI * 185.0 * t) * exp(-t * 25.0)
        val noise = Random.nextDouble(-1.0, 1.0) * exp(-t * 14.0)
        ((tone * 0.4 + noise * 0.6)).toFloat()
    }

    fun hiHat(): FloatArray = FloatArray(SR / 10) { i ->
        val t = i.toDouble() / SR
        (Random.nextDouble(-1.0, 1.0) * exp(-t * 45.0) * 0.5).toFloat()
    }

    fun siren(): FloatArray = FloatArray(SR) { i ->
        val t = i.toDouble() / SR
        val freq = 650.0 + 350.0 * sin(2.0 * PI * 3.0 * t)
        (sin(2.0 * PI * freq * t) * 0.5).toFloat()
    }

    fun laser(): FloatArray = FloatArray(SR / 3) { i ->
        val t = i.toDouble() / SR
        val freq = 1800.0 * exp(-t * 7.0) + 160.0
        (sin(2.0 * PI * freq * t) * exp(-t * 5.0) * 0.5).toFloat()
    }

    fun airhorn(): FloatArray = FloatArray((SR * 0.6).toInt()) { i ->
        val t = i.toDouble() / SR
        val f1 = 440.0
        val f2 = 554.37
        val env = ((1.0 - exp(-t * 40.0)) * if (t > 0.45) exp(-(t - 0.45) * 15.0) else 1.0)
        (((sin(2.0 * PI * f1 * t) + sin(2.0 * PI * f2 * t)) * 0.35 * env)).toFloat()
    }

    fun subDrop(): FloatArray = FloatArray((SR * 0.7).toInt()) { i ->
        val t = i.toDouble() / SR
        val freq = 95.0 * exp(-t * 4.5) + 32.0
        (sin(2.0 * PI * freq * t) * exp(-t * 3.0) * 0.7).toFloat()
    }

    fun beep(): FloatArray = FloatArray(SR / 6) { i ->
        val t = i.toDouble() / SR
        (sin(2.0 * PI * 1000.0 * t) * exp(-t * 12.0) * 0.4).toFloat()
    }

    fun noiseBurst(): FloatArray = FloatArray(SR / 5) { i ->
        val t = i.toDouble() / SR
        (Random.nextDouble(-1.0, 1.0) * exp(-t * 10.0) * 0.4).toFloat()
    }
}

object AudioDecoder {
    fun decodeToPcmFloat(context: android.content.Context, uri: Uri): FloatArray? {
        val extractor = android.media.MediaExtractor()
        return try {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
                extractor.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            } ?: return null

            var trackIndex = -1
            var format: android.media.MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val f = extractor.getTrackFormat(i)
                val mime = f.getString(android.media.MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    trackIndex = i
                    format = f
                    break
                }
            }
            if (trackIndex < 0 || format == null) return null

            extractor.selectTrack(trackIndex)
            val mime = format.getString(android.media.MediaFormat.KEY_MIME) ?: return null
            val codec = android.media.MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            val outSamples = ArrayList<Float>(1024 * 64)
            val bufferInfo = android.media.MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false

            while (!outputDone) {
                if (!inputDone) {
                    val inIdx = codec.dequeueInputBuffer(10_000)
                    if (inIdx >= 0) {
                        val inBuf = codec.getInputBuffer(inIdx)
                        val sampleSize = if (inBuf != null) extractor.readSampleData(inBuf, 0) else -1
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(inIdx, 0, 0, 0L, android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            val pts = extractor.sampleTime
                            codec.queueInputBuffer(inIdx, 0, sampleSize, pts, 0)
                            extractor.advance()
                        }
                    }
                }

                val outIdx = codec.dequeueOutputBuffer(bufferInfo, 10_000)
                if (outIdx >= 0) {
                    val outBuf = codec.getOutputBuffer(outIdx)
                    if (outBuf != null && bufferInfo.size > 0) {
                        outBuf.position(bufferInfo.offset)
                        outBuf.limit(bufferInfo.offset + bufferInfo.size)
                        val shortBuf = outBuf.order(java.nio.ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                        while (shortBuf.hasRemaining()) {
                            outSamples.add((shortBuf.get() / 32768f).coerceIn(-1f, 1f))
                        }
                    }
                    codec.releaseOutputBuffer(outIdx, false)
                    if ((bufferInfo.flags and android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        outputDone = true
                    }
                }
            }

            codec.stop()
            codec.release()
            outSamples.toFloatArray()
        } catch (_: Exception) {
            null
        } finally {
            extractor.release()
        }
    }
}

@Composable
fun MiniHardwareBtn(label: String, bgColor: Color, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(20.dp)
            .background(bgColor, RoundedCornerShape(2.dp))
            .border(1.dp, Color.Black, RoundedCornerShape(2.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SmallDeckLoadBtn(label: String, accentColor: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(Color(0xFF1B2838), RoundedCornerShape(2.dp))
            .border(1.dp, accentColor, RoundedCornerShape(2.dp))
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(label, color = accentColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TabButton(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(if (selected) BtnAmber else Color(0xFF2B303A), RoundedCornerShape(3.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) Color.Black else Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun formatTime(seconds: Double): String {
    val totalSec = seconds.toInt().coerceAtLeast(0)
    val mins = totalSec / 60
    val secs = totalSec % 60
    val hundredths = ((seconds - totalSec) * 100).toInt().coerceIn(0, 99)
    return String.format(Locale.US, "%02d:%02d.%02d", mins, secs, hundredths)
}
fun CenterFadeModule(
    modifier: Modifier,
    crossfader: Float,
    fadeTime: Float,
    onCrossfaderChange: (Float) -> Unit,
    onFadeTimeChange: (Float) -> Unit,
    onAutoFade: () -> Unit
) {
    Column(
        modifier = modifier
            .background(PanelMetal, RoundedCornerShape(4.dp))
            .border(1.dp, Color(0xFF3E4452), RoundedCornerShape(4.dp))
            .padding(5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text("CROSS FADE", color = Color.LightGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(LcdBlueBg, RoundedCornerShape(3.dp))
                .border(1.dp, Color(0xFF1C4E80), RoundedCornerShape(3.dp))
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = String.format(Locale.US, "%.1f s", fadeTime),
                color = LcdCyan,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("FADE TIME", color = Color.Gray, fontSize = 8.sp)
            Slider(
                value = fadeTime,
                onValueChange = onFadeTimeChange,
                valueRange = 1f..15f,
                modifier = Modifier.height(20.dp),
                colors = SliderDefaults.colors(thumbColor = LcdCyan, activeTrackColor = LcdCyan)
            )
        }
        Button(
            onClick = onAutoFade,
            colors = ButtonDefaults.buttonColors(containerColor = BtnAmber),
            contentPadding = PaddingValues(0.dp),
            shape = RoundedCornerShape(3.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
        ) {
            Text("AUTO FADE", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("MANUAL FADE", color = Color.Gray, fontSize = 8.sp)
            Slider(
                value = crossfader,
                onValueChange = onCrossfaderChange,
                valueRange = 0f..1f,
                modifier = Modifier.height(22.dp),
                colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = BtnAmber)
            )
        }
    }
}

@Composable
fun PlaylistPanel(
    playlist: List<PlaylistItem>,
    onAddFiles: () -> Unit,
    onClear: () -> Unit,
    onLoadA: (PlaylistItem) -> Unit,
    onLoadB: (PlaylistItem) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("PLAYLIST (${playlist.size})", color = LcdCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SmallDeckLoadBtn("+ ADD AUDIO", BtnGreen, onAddFiles)
                SmallDeckLoadBtn("CLEAR", BtnRed, onClear)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            itemsIndexed(playlist) { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1A1E26), RoundedCornerShape(3.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${index + 1}. ${item.title}",
                        color = Color.White,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    SmallDeckLoadBtn("LOAD A", LcdCyan) { onLoadA(item) }
                    SmallDeckLoadBtn("LOAD B", BtnAmber) { onLoadB(item) }
                }
            }
        }
    }
}

@Composable
fun MixerEqPanel() {
    var volA by remember { mutableFloatStateOf(0.85f) }
    var volB by remember { mutableFloatStateOf(0.85f) }
    var masterVol by remember { mutableFloatStateOf(0.9f) }

    var lowA by remember { mutableFloatStateOf(1f) }
    var midA by remember { mutableFloatStateOf(1f) }
    var highA by remember { mutableFloatStateOf(1f) }

    var lowB by remember { mutableFloatStateOf(1f) }
    var midB by remember { mutableFloatStateOf(1f) }
    var highB by remember { mutableFloatStateOf(1f) }

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("DECK A EQ & VOL", color = LcdCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            EqSlider("LOW", lowA) { lowA = it; AudioBridge.setEqA(lowA, midA, highA) }
            EqSlider("MID", midA) { midA = it; AudioBridge.setEqA(lowA, midA, highA) }
            EqSlider("HIGH", highA) { highA = it; AudioBridge.setEqA(lowA, midA, highA) }
            EqSlider("VOL", volA) { volA = it; AudioBridge.setVolumeA(volA) }
        }

        Column(
            modifier = Modifier.width(110.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("MASTER", color = BtnAmber, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Slider(
                value = masterVol,
                onValueChange = { masterVol = it; AudioBridge.setMasterVolume(it) },
                valueRange = 0f..1.2f,
                colors = SliderDefaults.colors(thumbColor = BtnAmber, activeTrackColor = BtnAmber)
            )
            Text(
                text = String.format(Locale.US, "%.0f%%", masterVol * 100f),
                color = Color.White,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text("DECK B EQ & VOL", color = BtnAmber, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            EqSlider("LOW", lowB) { lowB = it; AudioBridge.setEqB(lowB, midB, highB) }
            EqSlider("MID", midB) { midB = it; AudioBridge.setEqB(lowB, midB, highB) }
            EqSlider("HIGH", highB) { highB = it; AudioBridge.setEqB(lowB, midB, highB) }
            EqSlider("VOL", volB) { volB = it; AudioBridge.setVolumeB(volB) }
        }
    }
}

@Composable
private fun EqSlider(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.LightGray, fontSize = 8.sp, modifier = Modifier.width(28.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..1.5f,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = LcdCyan)
        )
    }
}

@Composable
fun SamplerPanel(onTriggerSample: (String, FloatArray) -> Unit) {
    val pads = remember {
        listOf(
            "KICK" to SampleSynth.kick(),
            "SNARE" to SampleSynth.snare(),
            "HI-HAT" to SampleSynth.hiHat(),
            "SIREN" to SampleSynth.siren(),
            "LASER" to SampleSynth.laser(),
            "AIRHORN" to SampleSynth.airhorn(),
            "SUB DROP" to SampleSynth.subDrop(),
            "BEEP" to SampleSynth.beep(),
            "NOISE" to SampleSynth.noiseBurst()
        )
    }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        for (row in 0 until 3) {
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (col in 0 until 3) {
                    val idx = row * 3 + col
                    val (name, data) = pads[idx]
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(Color(0xFF2B3240), RoundedCornerShape(3.dp))
                            .border(1.dp, LcdCyan.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
                            .clickable { onTriggerSample(name, data) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${idx + 1}. $name",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

object SampleSynth {
    private const val SR = 44100

    fun kick(): FloatArray = FloatArray(SR / 3) { i ->
        val t = i.toDouble() / SR
        val freq = 140.0 * exp(-t * 18.0) + 45.0
        (sin(2.0 * PI * freq * t) * exp(-t * 9.0)).toFloat()
    }

    fun snare(): FloatArray = FloatArray(SR / 4) { i ->
        val t = i.toDouble() / SR
        val tone = sin(2.0 * PI * 185.0 * t) * exp(-t * 25.0)
        val noise = Random.nextDouble(-1.0, 1.0) * exp(-t * 14.0)
        ((tone * 0.4 + noise * 0.6)).toFloat()
    }

    fun hiHat(): FloatArray = FloatArray(SR / 10) { i ->
        val t = i.toDouble() / SR
        (Random.nextDouble(-1.0, 1.0) * exp(-t * 45.0) * 0.5).toFloat()
    }

    fun siren(): FloatArray = FloatArray(SR) { i ->
        val t = i.toDouble() / SR
        val freq = 650.0 + 350.0 * sin(2.0 * PI * 3.0 * t)
        (sin(2.0 * PI * freq * t) * 0.5).toFloat()
    }

    fun laser(): FloatArray = FloatArray(SR / 3) { i ->
        val t = i.toDouble() / SR
        val freq = 1800.0 * exp(-t * 7.0) + 160.0
        (sin(2.0 * PI * freq * t) * exp(-t * 5.0) * 0.5).toFloat()
    }

    fun airhorn(): FloatArray = FloatArray((SR * 0.6).toInt()) { i ->
        val t = i.toDouble() / SR
        val f1 = 440.0
        val f2 = 554.37
        val env = ((1.0 - exp(-t * 40.0)) * if (t > 0.45) exp(-(t - 0.45) * 15.0) else 1.0)
        (((sin(2.0 * PI * f1 * t) + sin(2.0 * PI * f2 * t)) * 0.35 * env)).toFloat()
    }

    fun subDrop(): FloatArray = FloatArray((SR * 0.7).toInt()) { i ->
        val t = i.toDouble() / SR
        val freq = 95.0 * exp(-t * 4.5) + 32.0
        (sin(2.0 * PI * freq * t) * exp(-t * 3.0) * 0.7).toFloat()
    }

    fun beep(): FloatArray = FloatArray(SR / 6) { i ->
        val t = i.toDouble() / SR
        (sin(2.0 * PI * 1000.0 * t) * exp(-t * 12.0) * 0.4).toFloat()
    }

    fun noiseBurst(): FloatArray = FloatArray(SR / 5) { i ->
        val t = i.toDouble() / SR
        (Random.nextDouble(-1.0, 1.0) * exp(-t * 10.0) * 0.4).toFloat()
    }
}

object AudioDecoder {
    fun decodeToPcmFloat(context: android.content.Context, uri: Uri): FloatArray? {
        val extractor = android.media.MediaExtractor()
        return try {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
                extractor.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            } ?: return null

            var trackIndex = -1
            var format: android.media.MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val f = extractor.getTrackFormat(i)
                val mime = f.getString(android.media.MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    trackIndex = i
                    format = f
                    break
                }
            }
            if (trackIndex < 0 || format == null) return null

            extractor.selectTrack(trackIndex)
            val mime = format.getString(android.media.MediaFormat.KEY_MIME) ?: return null
            val codec = android.media.MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            val outSamples = ArrayList<Float>(1024 * 64)
            val bufferInfo = android.media.MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false

            while (!outputDone) {
                if (!inputDone) {
                    val inIdx = codec.dequeueInputBuffer(10_000)
                    if (inIdx >= 0) {
                        val inBuf = codec.getInputBuffer(inIdx)
                        val sampleSize = if (inBuf != null) extractor.readSampleData(inBuf, 0) else -1
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(inIdx, 0, 0, 0L, android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            val pts = extractor.sampleTime
                            codec.queueInputBuffer(inIdx, 0, sampleSize, pts, 0)
                            extractor.advance()
                        }
                    }
                }

                val outIdx = codec.dequeueOutputBuffer(bufferInfo, 10_000)
                if (outIdx >= 0) {
                    val outBuf = codec.getOutputBuffer(outIdx)
                    if (outBuf != null && bufferInfo.size > 0) {
                        outBuf.position(bufferInfo.offset)
                        outBuf.limit(bufferInfo.offset + bufferInfo.size)
                        val shortBuf = outBuf.order(java.nio.ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                        while (shortBuf.hasRemaining()) {
                            outSamples.add((shortBuf.get() / 32768f).coerceIn(-1f, 1f))
                        }
                    }
                    codec.releaseOutputBuffer(outIdx, false)
                    if ((bufferInfo.flags and android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        outputDone = true
                    }
                }
            }

            codec.stop()
            codec.release()
            outSamples.toFloatArray()
        } catch (_: Exception) {
            null
        } finally {
            extractor.release()
        }
    }
}

@Composable
fun MiniHardwareBtn(label: String, bgColor: Color, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(20.dp)
            .background(bgColor, RoundedCornerShape(2.dp))
            .border(1.dp, Color.Black, RoundedCornerShape(2.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SmallDeckLoadBtn(label: String, accentColor: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(Color(0xFF1B2838), RoundedCornerShape(2.dp))
            .border(1.dp, accentColor, RoundedCornerShape(2.dp))
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(label, color = accentColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TabButton(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(if (selected) BtnAmber else Color(0xFF2B303A), RoundedCornerShape(3.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) Color.Black else Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun formatTime(seconds: Double): String {
    val totalSec = seconds.toInt().coerceAtLeast(0)
    val mins = totalSec / 60
    val secs = totalSec % 60
    val hundredths = ((seconds - totalSec) * 100).toInt().coerceIn(0, 99)
    return String.format(Locale.US, "%02d:%02d.%02d", mins, secs, hundredths)
}
