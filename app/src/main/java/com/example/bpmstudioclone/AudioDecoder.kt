package com.example.bpmstudioclone

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import java.nio.ByteBuffer
import java.nio.ByteOrder

object AudioDecoder {
    fun decodeToPcmFloat(context: Context, uri: Uri): FloatArray {
        val extractor = MediaExtractor()
        val source = context.contentResolver.openFileDescriptor(uri, "r")
            ?: throw IllegalArgumentException("Unable to open audio file")

        try {
            source.use { descriptor ->
                extractor.setDataSource(descriptor.fileDescriptor)
            }

            val trackIndex = (0 until extractor.trackCount).firstOrNull { index ->
                extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)
                    ?.startsWith("audio/") == true
            } ?: throw IllegalArgumentException("No audio track found")

            extractor.selectTrack(trackIndex)
            val format = extractor.getTrackFormat(trackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME)
                ?: throw IllegalArgumentException("Unsupported audio format")
            val channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceAtLeast(1)

            val codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            val decoded = ArrayList<Short>()
            val bufferInfo = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false

            try {
                while (!outputDone) {
                    if (!inputDone) {
                        val inputIndex = codec.dequeueInputBuffer(10_000)
                        if (inputIndex >= 0) {
                            val inputBuffer = codec.getInputBuffer(inputIndex)
                                ?: continue
                            val sampleSize = extractor.readSampleData(inputBuffer, 0)
                            if (sampleSize < 0) {
                                codec.queueInputBuffer(inputIndex, 0, 0, 0,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                inputDone = true
                            } else {
                                codec.queueInputBuffer(inputIndex, 0, sampleSize,
                                    extractor.sampleTime, 0)
                                extractor.advance()
                            }
                        }
                    }

                    val outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10_000)
                    if (outputIndex >= 0) {
                        val outputBuffer = codec.getOutputBuffer(outputIndex)
                        if (outputBuffer != null && bufferInfo.size > 0) {
                            val bytes = ByteArray(bufferInfo.size)
                            outputBuffer.position(bufferInfo.offset)
                            outputBuffer.get(bytes)

                            val samples = ByteBuffer.wrap(bytes)
                                .order(ByteOrder.LITTLE_ENDIAN)
                                .asShortBuffer()
                            while (samples.hasRemaining()) {
                                var sum = 0
                                for (channel in 0 until channelCount) {
                                    if (!samples.hasRemaining()) break
                                    sum += samples.get().toInt()
                                }
                                decoded.add((sum / channelCount).toShort())
                            }
                        }
                        codec.releaseOutputBuffer(outputIndex, false)
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            outputDone = true
                        }
                    } else if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        // Channel count is already known from the source format.
                    }
                }
            } finally {
                codec.stop()
                codec.release()
            }

            val sourceRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val targetRate = 44_100
            if (sourceRate == targetRate) {
                return FloatArray(decoded.size) { index -> decoded[index] / 32768f }
            }

            val outputSize = ((decoded.size.toLong() * targetRate) / sourceRate).toInt()
            return FloatArray(outputSize) { index ->
                val sourceIndex = ((index.toLong() * sourceRate) / targetRate)
                    .toInt()
                    .coerceIn(0, decoded.lastIndex)
                decoded[sourceIndex] / 32768f
            }
        } finally {
            extractor.release()
        }
    }
}
