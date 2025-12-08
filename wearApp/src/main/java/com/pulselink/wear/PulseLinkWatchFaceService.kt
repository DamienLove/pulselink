package com.pulselink.wear

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.graphics.Typeface
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.SurfaceHolder
import androidx.wear.watchface.CanvasType
import androidx.wear.watchface.ComplicationSlot
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.Renderer
import androidx.wear.watchface.TapEvent
import androidx.wear.watchface.TapType
import androidx.wear.watchface.WatchFace
import androidx.wear.watchface.WatchFaceService
import androidx.wear.watchface.WatchFaceType
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.wear.watchface.style.UserStyleSchema
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.time.ZonedDateTime
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

class PulseLinkWatchFaceService : WatchFaceService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    override fun createUserStyleSchema(): UserStyleSchema = UserStyleSchema(emptyList())

    override fun createComplicationSlotsManager(userStyleRepository: CurrentUserStyleRepository): ComplicationSlotsManager {
        return ComplicationSlotsManager(emptyList(), userStyleRepository)
    }

    override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: androidx.wear.watchface.WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ): WatchFace {

        val renderer = PulseLinkRenderer(
            service = this,
            surfaceHolder = surfaceHolder,
            watchState = watchState,
            currentUserStyleRepository = currentUserStyleRepository
        )

        val watchFace = WatchFace(WatchFaceType.ANALOG, renderer)

        watchFace.setTapListener(object : WatchFace.TapListener {
            override fun onTapEvent(tapType: Int, tapEvent: TapEvent, complicationSlot: ComplicationSlot?) {
                if (tapType != 2) return // 2 == TAP_TYPE_UP
                when (renderer.hitTest(tapEvent.xPos.toFloat(), tapEvent.yPos.toFloat())) {
                    HitRegion.CENTER -> toggleEmergency(renderer)
                    HitRegion.CHECK_IN -> sendCommand(PAYLOAD_CHECKIN)
                    HitRegion.NONE -> {}
                }
            }
        })

        return watchFace
    }

    private fun toggleEmergency(renderer: PulseLinkRenderer) {
        val newActive = !renderer.emergencyActive
        renderer.setEmergencyActive(newActive)
        sendCommand(if (newActive) PAYLOAD_EMERGENCY else PAYLOAD_CANCEL)
        vibrate(if (newActive) longArrayOf(0, 160, 80, 200, 80, 240) else longArrayOf(0, 80, 80, 80))
    }

    private fun sendCommand(payload: String) {
        scope.launch {
            val client = Wearable.getMessageClient(this@PulseLinkWatchFaceService)
            val nodes = Wearable.getNodeClient(this@PulseLinkWatchFaceService).connectedNodes.await()
            nodes.forEach { node -> client.sendMessage(node.id, ACTION_PATH, payload.toByteArray()) }
        }
    }

    private fun vibrate(pattern: LongArray) {
        val vibrator = getSystemService(VibratorManager::class.java)?.defaultVibrator
            ?: getSystemService(Vibrator::class.java)
        vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
    }

    companion object {
        private const val ACTION_PATH = "/pulselink/action"
        private const val PAYLOAD_EMERGENCY = "EMERGENCY"
        private const val PAYLOAD_CANCEL = "CANCEL"
        private const val PAYLOAD_CHECKIN = "CHECKIN"
    }
}

private enum class HitRegion { CENTER, CHECK_IN, NONE }

private class PulseLinkRenderer(
    private val service: WatchFaceService,
    surfaceHolder: SurfaceHolder,
    private val watchState: androidx.wear.watchface.WatchState,
    currentUserStyleRepository: CurrentUserStyleRepository
) : Renderer.CanvasRenderer(
    surfaceHolder,
    currentUserStyleRepository,
    watchState,
    CanvasType.HARDWARE,
    16L,
    false
) {

    @Volatile var emergencyActive: Boolean = false
        private set

    private val goldOuter = intArrayOf(
        Color.parseColor("#F2D48D"),
        Color.parseColor("#D5A84D"),
        Color.parseColor("#F7E4AF"),
        Color.parseColor("#D5A84D"),
        Color.parseColor("#F2D48D")
    )
    private val goldPositions = floatArrayOf(0f, 0.25f, 0.5f, 0.75f, 1f)
    private val faceDark = Color.parseColor("#0C0F16")
    private val markerTeal = Color.parseColor("#00C7B1")
    private val markerRed = Color.parseColor("#E45757")
    private val markerWhite = Color.parseColor("#ECE7D5")
    private val markerOutline = Color.parseColor("#C7A45A")
    private val handHour = Color.parseColor("#D8B063")
    private val handMinute = Color.parseColor("#EDE4CE")
    private val glowCenter = Color.parseColor("#55FFC300")

    private val paint = Paint().apply { isAntiAlias = true }
    private val markerPath = Path()
    private val tmpRect = RectF()

    private var cx = 0f
    private var cy = 0f
    private var radius = 0f
    private var checkInRect = RectF()

    private val shieldBitmap: Bitmap by lazy {
        BitmapFactory.decodeResource(service.resources, R.drawable.ic_wear_logo)
    }

    fun setEmergencyActive(active: Boolean) {
        emergencyActive = active
        invalidate()
    }

    override fun render(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime
    ) {
        cx = bounds.exactCenterX()
        cy = bounds.exactCenterY()
        radius = min(bounds.width(), bounds.height()) / 2f * 0.95f

        val ambient = watchState.isAmbient.value == true

        drawBezel(canvas)
        drawFace(canvas, ambient)
        drawMarkers(canvas, ambient)
        drawHands(canvas, zonedDateTime, ambient)
        drawShield(canvas, ambient)
        drawCheckIn(canvas, ambient)
    }

    override fun renderHighlightLayer(canvas: Canvas, bounds: Rect, zonedDateTime: ZonedDateTime) {
        // No highlight layer elements.
    }

    private fun drawBezel(canvas: Canvas) {
        paint.shader = SweepGradient(cx, cy, goldOuter, goldPositions)
        paint.style = Paint.Style.FILL
        canvas.drawCircle(cx, cy, radius, paint)
    }

    private fun drawFace(canvas: Canvas, ambient: Boolean) {
        paint.shader = null
        paint.color = faceDark
        canvas.drawCircle(cx, cy, radius * 0.90f, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = radius * 0.003f
        paint.color = Color.WHITE.withAlphaF(if (ambient) 0.08f else 0.18f)
        listOf(0.34f, 0.48f, 0.64f).forEach {
            canvas.drawCircle(cx, cy, radius * it, paint)
        }

        if (!ambient) {
            val glowRadius = radius * 0.55f
            paint.shader = RadialGradient(
                cx,
                cy,
                glowRadius,
                intArrayOf(Color.TRANSPARENT, glowCenter, Color.TRANSPARENT),
                floatArrayOf(0f, 0.45f, 1f),
                Shader.TileMode.CLAMP
            )
            paint.style = Paint.Style.FILL
            canvas.drawCircle(cx, cy, glowRadius, paint)
        }
    }

    private fun drawMarkers(canvas: Canvas, ambient: Boolean) {
        val pattern = listOf(
            markerWhite,
            markerTeal,
            markerWhite,
            markerTeal,
            markerWhite,
            markerRed,
            markerTeal,
            markerRed,
            markerTeal,
            markerWhite,
            markerTeal,
            markerWhite
        )
        val size = radius * 0.075f
        paint.style = Paint.Style.FILL
        paint.shader = null
        pattern.forEachIndexed { idx, color ->
            val angle = Math.toRadians((idx * 30.0) - 90.0)
            val x = cx + radius * 0.82f * cos(angle).toFloat()
            val y = cy + radius * 0.82f * sin(angle).toFloat()
            markerPath.reset()
            markerPath.moveTo(x, y - size / 2)
            markerPath.lineTo(x + size / 2, y)
            markerPath.lineTo(x, y + size / 2)
            markerPath.lineTo(x - size / 2, y)
            markerPath.close()
            val fill = if (ambient) color.desaturate(0.4f) else color
            paint.color = fill
            canvas.drawPath(markerPath, paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = size * 0.12f
            paint.color = if (ambient) markerOutline.desaturate(0.5f) else markerOutline
            canvas.drawPath(markerPath, paint)
            paint.style = Paint.Style.FILL
        }
    }

    private fun drawHands(canvas: Canvas, time: ZonedDateTime, ambient: Boolean) {
        val millis = time.nano / 1_000_000
        val second = time.second + millis / 1000f
        val minute = time.minute + second / 60f
        val hour = (time.hour % 12) + minute / 60f

        fun hand(lengthScale: Float, width: Float, angleDeg: Float, color: Int) {
            paint.color = color
            paint.strokeWidth = width
            paint.style = Paint.Style.STROKE
            paint.strokeCap = Paint.Cap.ROUND
            val angle = Math.toRadians(angleDeg - 90.0)
            val x = cx + radius * lengthScale * cos(angle).toFloat()
            val y = cy + radius * lengthScale * sin(angle).toFloat()
            canvas.drawLine(cx, cy, x, y, paint)
        }

        hand(0.46f, radius * 0.014f, hour * 30f, handHour)
        hand(0.64f, radius * 0.008f, minute * 6f, handMinute)
        if (!ambient) {
            hand(0.70f, radius * 0.004f, second * 6f, markerWhite.withAlphaF(0.6f))
        }

        paint.style = Paint.Style.FILL
        paint.color = markerWhite
        canvas.drawCircle(cx, cy, radius * 0.018f, paint)
        paint.color = faceDark
        canvas.drawCircle(cx, cy, radius * 0.009f, paint)
    }

    private fun drawShield(canvas: Canvas, ambient: Boolean) {
        val size = (radius * 0.50f).toInt()
        val left = (cx - size / 2).toInt()
        val top = (cy - size / 2).toInt()
        paint.alpha = if (ambient) 180 else 255
        val scaled = Bitmap.createScaledBitmap(shieldBitmap, size, size, true)
        canvas.drawBitmap(scaled, left.toFloat(), top.toFloat(), paint)
    }

    private fun drawCheckIn(canvas: Canvas, ambient: Boolean) {
        val pillWidth = radius * 0.78f
        val pillHeight = radius * 0.14f
        checkInRect = RectF(
            cx - pillWidth / 2,
            cy + radius * 0.52f,
            cx + pillWidth / 2,
            cy + radius * 0.52f + pillHeight
        )

        paint.shader = null
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#151926")
        paint.alpha = if (ambient) 120 else 200
        canvas.drawRoundRect(checkInRect, pillHeight / 2, pillHeight / 2, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = Color.parseColor("#D6B060")
        paint.alpha = if (ambient) 150 else 255
        canvas.drawRoundRect(checkInRect, pillHeight / 2, pillHeight / 2, paint)

        // Check icon
        val iconX = checkInRect.left + pillHeight * 0.75f
        val iconY = checkInRect.centerY()
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = pillHeight * 0.12f
        paint.color = Color.parseColor("#D6B060")
        paint.strokeCap = Paint.Cap.ROUND
        val tick = Path().apply {
            moveTo(iconX - pillHeight * 0.20f, iconY)
            lineTo(iconX - pillHeight * 0.04f, iconY + pillHeight * 0.20f)
            lineTo(iconX + pillHeight * 0.22f, iconY - pillHeight * 0.18f)
        }
        canvas.drawPath(tick, paint)

        paint.style = Paint.Style.FILL
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.textSize = radius * 0.065f
        paint.color = Color.parseColor("#D6B060")
        val textY = checkInRect.centerY() - (paint.descent() + paint.ascent()) / 2
        canvas.drawText("CHECK IN", checkInRect.centerX() + pillHeight * 0.25f, textY, paint)
    }

    fun hitTest(x: Float, y: Float): HitRegion {
        val dx = x - cx
        val dy = y - cy
        val dist = kotlin.math.sqrt(dx * dx + dy * dy)
        if (dist <= radius * 0.30f) return HitRegion.CENTER
        if (checkInRect.contains(x, y)) return HitRegion.CHECK_IN
        return HitRegion.NONE
    }

    private fun Int.withAlphaF(alpha: Float): Int {
        val a = (Color.alpha(this) * alpha).toInt()
        return Color.argb(a, Color.red(this), Color.green(this), Color.blue(this))
    }

    private fun Int.desaturate(factor: Float): Int {
        val r = Color.red(this)
        val g = Color.green(this)
        val b = Color.blue(this)
        val gray = (0.3f * r + 0.59f * g + 0.11f * b).toInt()
        val mix = { c: Int -> (c * (1 - factor) + gray * factor).toInt() }
        return Color.argb(Color.alpha(this), mix(r), mix(g), mix(b))
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T =
    kotlinx.coroutines.suspendCancellableCoroutine { cont ->
        addOnSuccessListener { result -> cont.resume(result) {} }
        addOnFailureListener { error -> cont.resumeWithException(error) }
    }
