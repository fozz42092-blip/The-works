package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import com.example.data.model.GeneratedVideoClip
import com.example.data.model.VideoScenePlan
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.AspectRatio
import com.google.firebase.ai.type.ImageConfig
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ImageSize
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.io.File
import kotlin.math.roundToInt

/**
 * Generates real playable H.264 MP4 short video clips from text (blog posts, scripts,
 * product descriptions, or site reports) or uploaded photos (product ads / property animations)
 * using Gemini 3.8 Flash storyboarding + Gemini Nano Banana 2.1 visual keyframe synthesis +
 * hardware-backed MediaCodec/MediaMuxer MP4 encoding.
 */
class AiVideoStudioRepository {

    private val storyboardSchema = Schema.obj(
        properties = mapOf(
            "videoTitle" to Schema.string(description = "Catchy short video title"),
            "scriptSummary" to Schema.string(description = "Concise 1-2 sentence narration overview"),
            "scenes" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "sceneNumber" to Schema.integer(),
                        "headline" to Schema.string(description = "3-5 word kinetic overlay headline"),
                        "voiceoverCaption" to Schema.string(description = "Short 8-14 word bottom caption"),
                        "visualPrompt" to Schema.string(description = "Detailed cinematic scene visual prompt"),
                        "cameraMotion" to Schema.enumeration(
                            values = listOf("zoom_in", "pan_right", "zoom_out"),
                            description = "Motion direction across the scene clip"
                        )
                    )
                ),
                description = "Exactly 3 ordered storyboard scenes for a 9-second vertical/square video clip"
            )
        )
    )

    private val storyboardModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = storyboardSchema
            }
        )
    }

    private val frameGeneratorModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-nano-banana-2.1",
            generationConfig = generationConfig {
                responseModalities = listOf(ResponseModality.IMAGE)
                imageConfig = ImageConfig.builder()
                    .setAspectRatio(AspectRatio.LANDSCAPE_16x9)
                    .setImageSize(ImageSize.SIZE_1K)
                    .build()
            }
        )
    }

    suspend fun generateVideoClip(
        context: Context,
        sourceType: String, // "script" | "product_ad" | "blog_post" | "site_report"
        promptText: String,
        referencePhoto: Bitmap?,
        onProgress: (String) -> Unit
    ): Result<GeneratedVideoClip> {
        return try {
            onProgress("Step 1/3: Directing 3-scene storyboard with Gemini...")
            val storyboardPrompt = """
                Turn this ${sourceType.replace('_', ' ')} into a punchy 3-scene short video clip (~9 seconds total).
                ${if (referencePhoto != null) "The user attached a reference product/site photo: keep the core subject authentic while animating dynamic camera angles and dramatic storytelling." else "Design 3 cohesive cinematic scenes."}
                Input text:
                "$promptText"
            """.trimIndent()

            val sbResponse = withTimeout(60_000L) {
                storyboardModel.generateContent(
                    content {
                        if (referencePhoto != null) image(referencePhoto)
                        text(storyboardPrompt)
                    }
                )
            }

            val json = JSONObject(sbResponse.text ?: throw IllegalStateException("Empty storyboard response."))
            val title = json.optString("videoTitle", "WaterTable AI Clip")
            val scriptSummary = json.optString("scriptSummary", "")
            val scenesArr = json.optJSONArray("scenes") ?: throw IllegalStateException("No scenes returned.")
            val scenes = buildList {
                for (i in 0 until minOf(3, scenesArr.length())) {
                    val obj = scenesArr.getJSONObject(i)
                    add(
                        VideoScenePlan(
                            sceneNumber = obj.optInt("sceneNumber", i + 1),
                            headline = obj.optString("headline", "Scene ${i + 1}"),
                            voiceoverCaption = obj.optString("voiceoverCaption", ""),
                            visualPrompt = obj.optString("visualPrompt", promptText),
                            cameraMotion = obj.optString("cameraMotion", "zoom_in")
                        )
                    )
                }
            }
            if (scenes.isEmpty()) throw IllegalStateException("Storyboard returned 0 scenes.")

            onProgress("Step 2/3: Synthesizing ${scenes.size} motion keyframes with Nano Banana 2.1...")
            val keyframes = mutableListOf<Bitmap>()
            for ((index, scene) in scenes.withIndex()) {
                onProgress("Rendering AI visual keyframe ${index + 1}/${scenes.size}...")
                val frameResp = withTimeout(60_000L) {
                    if (referencePhoto != null) {
                        frameGeneratorModel.generateContent(
                            content {
                                image(referencePhoto)
                                text("Re-stage this exact subject into scene ${index + 1} for a dynamic video commercial/reel: ${scene.visualPrompt}. Photorealistic, dramatic studio/editorial lighting, no text overlays.")
                            }
                        )
                    } else {
                        frameGeneratorModel.generateContent(
                            "Cinematic 16:9 film still for scene ${index + 1}: ${scene.visualPrompt}. Photorealistic, rich lighting, no watermarks or text."
                        )
                    }
                }
                val imgPart = frameResp.candidates.firstOrNull()
                    ?.content?.parts?.filterIsInstance<ImagePart>()
                    ?.firstOrNull()
                    ?: throw IllegalStateException("Image model did not return a frame for scene ${index + 1}.")
                keyframes.add(imgPart.image)
            }

            onProgress("Step 3/3: Encoding smooth H.264 MP4 video clip...")
            val outputFile = withContext(Dispatchers.IO) {
                encodeScenesToMp4(context, scenes, keyframes)
            }

            Result.success(
                GeneratedVideoClip(
                    title = title,
                    sourceType = sourceType,
                    mp4FilePath = outputFile.absolutePath,
                    durationSeconds = scenes.size * SCENE_SECONDS,
                    scenes = scenes,
                    scriptSummary = scriptSummary
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Video generation timed out", e)
            Result.failure(IllegalStateException("AI video generation timed out after 60s. Please retry."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Video generation failed", e)
            Result.failure(e)
        }
    }

    private fun encodeScenesToMp4(
        context: Context,
        scenes: List<VideoScenePlan>,
        keyframes: List<Bitmap>
    ): File {
        val dir = File(context.cacheDir, "generated_videos").apply { mkdirs() }
        val outFile = File(dir, "wt_clip_${System.currentTimeMillis()}.mp4")
        if (outFile.exists()) outFile.delete()

        val width = 896
        val height = 512
        val fps = 20
        val framesPerScene = SCENE_SECONDS * fps
        val frameDurationUs = 1_000_000L / fps

        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, 2_200_000)
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }

        val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        val muxer = MediaMuxer(outFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var muxerStarted = false
        var trackIndex = -1
        var inputSurface: android.view.Surface? = null

        try {
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            inputSurface = codec.createInputSurface()
            codec.start()

            val bufferInfo = MediaCodec.BufferInfo()
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            val scrimPaint = Paint()
            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 30f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val captionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#E0F2FE")
                textSize = 20f
            }
            val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#CC0284C7")
            }

            fun drainEncoder(endOfStream: Boolean) {
                if (endOfStream) codec.signalEndOfInputStream()
                while (true) {
                    val outIdx = codec.dequeueOutputBuffer(bufferInfo, 10_000L)
                    when {
                        outIdx == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                            if (!endOfStream) break
                        }
                        outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            if (muxerStarted) throw IllegalStateException("Format changed twice")
                            trackIndex = muxer.addTrack(codec.outputFormat)
                            muxer.start()
                            muxerStarted = true
                        }
                        outIdx >= 0 -> {
                            val encoded = codec.getOutputBuffer(outIdx)
                            if (encoded != null && bufferInfo.size > 0 && muxerStarted) {
                                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                                    encoded.position(bufferInfo.offset)
                                    encoded.limit(bufferInfo.offset + bufferInfo.size)
                                    muxer.writeSampleData(trackIndex, encoded, bufferInfo)
                                }
                            }
                            codec.releaseOutputBuffer(outIdx, false)
                            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) break
                        }
                    }
                }
            }

            var frameIndexGlobal = 0L
            for (sIdx in scenes.indices) {
                val scene = scenes[sIdx]
                val bmp = keyframes[sIdx]
                val nextBmp = keyframes.getOrNull(sIdx + 1)

                for (f in 0 until framesPerScene) {
                    val t = f.toFloat() / framesPerScene.toFloat()
                    val canvas: Canvas = inputSurface.lockCanvas(null)
                    try {
                        drawAnimatedSceneFrame(
                            canvas = canvas,
                            w = width,
                            h = height,
                            bmp = bmp,
                            nextBmp = nextBmp,
                            progress = t,
                            motion = scene.cameraMotion,
                            headline = scene.headline,
                            caption = scene.voiceoverCaption,
                            sceneLabel = "SCENE ${sIdx + 1}/${scenes.size}",
                            paint = paint,
                            scrimPaint = scrimPaint,
                            titlePaint = titlePaint,
                            captionPaint = captionPaint,
                            pillPaint = pillPaint
                        )
                    } finally {
                        inputSurface.unlockCanvasAndPost(canvas)
                    }
                    drainEncoder(false)
                    frameIndexGlobal++
                }
            }

            drainEncoder(true)
            Log.d(TAG, "Encoded ${frameIndexGlobal} frames to ${outFile.absolutePath} (${outFile.length()} bytes)")
            return outFile
        } finally {
            try { codec.stop() } catch (_: Exception) {}
            try { codec.release() } catch (_: Exception) {}
            try { inputSurface?.release() } catch (_: Exception) {}
            if (muxerStarted) {
                try { muxer.stop() } catch (_: Exception) {}
            }
            try { muxer.release() } catch (_: Exception) {}
        }
    }

    private fun drawAnimatedSceneFrame(
        canvas: Canvas,
        w: Int,
        h: Int,
        bmp: Bitmap,
        nextBmp: Bitmap?,
        progress: Float,
        motion: String,
        headline: String,
        caption: String,
        sceneLabel: String,
        paint: Paint,
        scrimPaint: Paint,
        titlePaint: Paint,
        captionPaint: Paint,
        pillPaint: Paint
    ) {
        canvas.drawColor(Color.parseColor("#06253A"))
        val scale = when (motion) {
            "zoom_out" -> 1.18f - 0.14f * progress
            "pan_right" -> 1.14f
            else -> 1.02f + 0.16f * progress
        }
        val panXFraction = if (motion == "pan_right") (progress - 0.5f) * 0.08f else 0f

        paint.alpha = 255
        drawScaledBitmap(canvas, bmp, w, h, scale, panXFraction, paint)

        // Smooth crossfade on last 15% of scene into next scene
        if (nextBmp != null && progress > 0.85f) {
            val fadeAlpha = (((progress - 0.85f) / 0.15f) * 255f).roundToInt().coerceIn(0, 255)
            paint.alpha = fadeAlpha
            drawScaledBitmap(canvas, nextBmp, w, h, 1.02f, 0f, paint)
            paint.alpha = 255
        }

        // Cinematic bottom gradient scrim
        scrimPaint.shader = LinearGradient(
            0f, h * 0.45f, 0f, h.toFloat(),
            Color.TRANSPARENT, Color.parseColor("#DD061B2B"),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, h * 0.45f, w.toFloat(), h.toFloat(), scrimPaint)

        // Scene badge pill
        val badgeRect = RectF(28f, 24f, 178f, 56f)
        canvas.drawRoundRect(badgeRect, 16f, 16f, pillPaint)
        val badgePaint = Paint(captionPaint).apply {
            color = Color.WHITE
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(sceneLabel, 46f, 46f, badgePaint)

        // Headline & Voiceover Caption
        canvas.drawText(headline.take(44), 28f, h - 62f, titlePaint)
        canvas.drawText(caption.take(72), 28f, h - 26f, captionPaint)
    }

    private fun drawScaledBitmap(
        canvas: Canvas,
        bmp: Bitmap,
        w: Int,
        h: Int,
        scale: Float,
        panXFraction: Float,
        paint: Paint
    ) {
        val baseScale = maxOf(w.toFloat() / bmp.width.toFloat(), h.toFloat() / bmp.height.toFloat()) * scale
        val dw = bmp.width * baseScale
        val dh = bmp.height * baseScale
        val left = (w - dw) / 2f + (panXFraction * w)
        val top = (h - dh) / 2f
        val dst = RectF(left, top, left + dw, top + dh)
        canvas.drawBitmap(bmp, null, dst, paint)
    }

    private companion object {
        const val TAG = "AiVideoStudioRepo"
        const val SCENE_SECONDS = 3
    }
}
