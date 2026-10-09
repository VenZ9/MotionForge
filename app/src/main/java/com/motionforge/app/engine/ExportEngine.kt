package com.motionforge.app.engine

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.motionforge.app.model.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

object ExportEngine {

    data class ExportConfig(
        val width: Int = 1920,
        val height: Int = 1080,
        val fps: Int = 30,
        val bitrate: Int = 8_000_000, // 8 Mbps
        val outputFile: File
    )

    suspend fun exportProjectToMp4(
        context: Context,
        project: Project,
        config: ExportConfig,
        cancelFlag: AtomicBoolean,
        onProgress: (progress: Float, frame: Int, totalFrames: Int) -> Unit
    ): Result<File> = withContext(Dispatchers.Default) {
        var mediaCodec: MediaCodec? = null
        var mediaMuxer: MediaMuxer? = null
        var trackIndex = -1
        var muxerStarted = false

        try {
            val totalFrames = ((project.durationMs / 1000f) * config.fps).toInt().coerceAtLeast(1)
            val frameDurationUs = (1_000_000L / config.fps)

            // Setup MediaFormat for H.264 / AVC
            val format = MediaFormat.createVideoFormat(
                MediaFormat.MIMETYPE_VIDEO_AVC,
                config.width,
                config.height
            ).apply {
                setInteger(
                    MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
                )
                setInteger(MediaFormat.KEY_BIT_RATE, config.bitrate)
                setInteger(MediaFormat.KEY_FRAME_RATE, config.fps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1) // 1 second keyframe interval
            }

            mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            mediaCodec.start()

            mediaMuxer = MediaMuxer(config.outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            val bufferInfo = MediaCodec.BufferInfo()
            val bitmap = Bitmap.createBitmap(config.width, config.height, Bitmap.Config.ARGB_8888)
            val androidCanvas = android.graphics.Canvas(bitmap)
            val composeCanvas = Canvas(androidCanvas)
            val drawScope = CanvasDrawScope()

            val argbPixels = IntArray(config.width * config.height)
            val yuvBuffer = ByteArray(config.width * config.height * 3 / 2)

            var frameIndex = 0

            while (frameIndex < totalFrames && !cancelFlag.get() && coroutineContext.isActive) {
                val currentTimeMs = (frameIndex * 1000L) / config.fps

                // Clear and render frame
                bitmap.eraseColor(android.graphics.Color.BLACK)
                drawScope.draw(
                    density = Density(1f),
                    layoutDirection = LayoutDirection.Ltr,
                    canvas = composeCanvas,
                    size = androidx.compose.ui.geometry.Size(config.width.toFloat(), config.height.toFloat())
                ) {
                    RenderEngine.renderComposition(
                        drawScope = this,
                        project = project,
                        timeMs = currentTimeMs,
                        canvasWidth = config.width.toFloat(),
                        canvasHeight = config.height.toFloat()
                    )
                }

                // Convert ARGB Bitmap to YUV420
                bitmap.getPixels(argbPixels, 0, config.width, 0, 0, config.width, config.height)
                encodeYUV420SP(yuvBuffer, argbPixels, config.width, config.height)

                // Feed to MediaCodec input buffer
                val inputBufferIndex = mediaCodec.dequeueInputBuffer(10_000L)
                if (inputBufferIndex >= 0) {
                    val inputBuffer = mediaCodec.getInputBuffer(inputBufferIndex)
                    inputBuffer?.clear()
                    inputBuffer?.put(yuvBuffer)
                    val ptsUs = frameIndex * frameDurationUs
                    val flags = if (frameIndex == totalFrames - 1) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0
                    mediaCodec.queueInputBuffer(inputBufferIndex, 0, yuvBuffer.size, ptsUs, flags)
                    frameIndex++
                }

                // Drain output from MediaCodec
                var outputIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 2500L)
                while (outputIndex >= 0) {
                    val outputBuffer = mediaCodec.getOutputBuffer(outputIndex)
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                        bufferInfo.size = 0
                    }

                    if (bufferInfo.size != 0 && outputBuffer != null) {
                        if (!muxerStarted) {
                            val newFormat = mediaCodec.outputFormat
                            trackIndex = mediaMuxer.addTrack(newFormat)
                            mediaMuxer.start()
                            muxerStarted = true
                        }

                        outputBuffer.position(bufferInfo.offset)
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                        mediaMuxer.writeSampleData(trackIndex, outputBuffer, bufferInfo)
                    }

                    mediaCodec.releaseOutputBuffer(outputIndex, false)
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        break
                    }
                    outputIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0L)
                }

                val progress = frameIndex.toFloat() / totalFrames.toFloat()
                onProgress(progress, frameIndex, totalFrames)
            }

            // Drain remaining frames until EOS
            var eosReached = false
            while (!eosReached && !cancelFlag.get()) {
                val outputIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 10_000L)
                if (outputIndex >= 0) {
                    val outputBuffer = mediaCodec.getOutputBuffer(outputIndex)
                    if (bufferInfo.size != 0 && outputBuffer != null && muxerStarted) {
                        outputBuffer.position(bufferInfo.offset)
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                        mediaMuxer.writeSampleData(trackIndex, outputBuffer, bufferInfo)
                    }
                    mediaCodec.releaseOutputBuffer(outputIndex, false)
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        eosReached = true
                    }
                } else if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED && !muxerStarted) {
                    trackIndex = mediaMuxer.addTrack(mediaCodec.outputFormat)
                    mediaMuxer.start()
                    muxerStarted = true
                } else if (outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    break
                }
            }

            bitmap.recycle()

            if (cancelFlag.get()) {
                config.outputFile.delete()
                return@withContext Result.failure(Exception("Export canceled by user"))
            }

            Result.success(config.outputFile)
        } catch (e: Exception) {
            config.outputFile.delete()
            Result.failure(e)
        } finally {
            try {
                mediaCodec?.stop()
                mediaCodec?.release()
            } catch (_: Exception) {}
            try {
                if (muxerStarted) {
                    mediaMuxer?.stop()
                }
                mediaMuxer?.release()
            } catch (_: Exception) {}
        }
    }

    private fun encodeYUV420SP(yuv420sp: ByteArray, argb: IntArray, width: Int, height: Int) {
        val frameSize = width * height
        var yIndex = 0
        var uvIndex = frameSize
        var index = 0

        for (j in 0 until height) {
            for (i in 0 until width) {
                val c = argb[index++]
                val r = (c shr 16) and 0xff
                val g = (c shr 8) and 0xff
                val b = c and 0xff

                val y = ((66 * r + 129 * g + 25 * b + 128) shr 8) + 16
                val u = ((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128
                val v = ((112 * r - 94 * g - 18 * b + 128) shr 8) + 128

                yuv420sp[yIndex++] = (if (y < 0) 0 else if (y > 255) 255 else y).toByte()

                if (j % 2 == 0 && i % 2 == 0) {
                    yuv420sp[uvIndex++] = (if (u < 0) 0 else if (u > 255) 255 else u).toByte()
                    yuv420sp[uvIndex++] = (if (v < 0) 0 else if (v > 255) 255 else v).toByte()
                }
            }
        }
    }
}
