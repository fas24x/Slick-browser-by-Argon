package com.slick.browser.ui.effects

import android.graphics.BlurMaskFilter
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slick.browser.ui.theme.KanitLogoFamily
import com.slick.browser.ui.theme.LocalSlickColors
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

val NormalFogPalette = listOf(
    Color(0xFF4A90E2),
    Color(0xFF6C63FF),
    Color(0xFF9B51E0),
    Color(0xFFD6458B),
    Color(0xFFF26B5E)
)

val IncognitoFogPalette = listOf(
    Color(0xFFFFFFFF),
    Color(0xFFD8D8D8),
    Color(0xFFEFEFEF),
    Color(0xFFA8A8A8),
    Color(0xFFFFFFFF)
)

@Composable
fun rememberFogPhase(enabled: Boolean): Float {
    if (!enabled) return 1.2f
    val infiniteTransition = rememberInfiniteTransition(label = "slick_fog_loop")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "fog_phase"
    )
    return phase
}

/**
 * Рисует мягкий рассеянный градиентный туман (ауру) без каких-либо жёстких рамок.
 * Использует нативный BlurMaskFilter для создания настоящего объёмного свечения вокруг или внутри формы.
 */
fun DrawScope.drawDiffusedFogClusters(
    phase: Float,
    isIncognito: Boolean,
    isMonoLogo: Boolean,
    fallbackColor: Color,
    blurRadiusPx: Float,
    alphaMultiplier: Float = 0.85f,
    spreadPaddingPx: Float = 0f,
    progressFraction: Float = 1f,
    burstExpansion: Float = 0f
) {
    if (alphaMultiplier <= 0.01f || progressFraction <= 0.005f) return

    val palette = when {
        isMonoLogo -> listOf(fallbackColor, fallbackColor.copy(alpha = 0.7f))
        isIncognito -> IncognitoFogPalette
        else -> NormalFogPalette
    }

    val safeBlur = (blurRadiusPx * (1f + burstExpansion * 0.85f)).coerceAtLeast(8f)

    drawIntoCanvas { canvas ->
        val frameworkPaint = Paint().asFrameworkPaint().apply {
            isAntiAlias = true
            style = android.graphics.Paint.Style.FILL
            maskFilter = BlurMaskFilter(safeBlur, BlurMaskFilter.Blur.NORMAL)
        }

        val activeWidth = (size.width + spreadPaddingPx * 2f) * progressFraction.coerceIn(0f, 1f)
        val startX = -spreadPaddingPx
        val centerY = size.height / 2f

        val blobCount = 7
        for (i in 0 until blobCount) {
            val norm = i.toFloat() / (blobCount - 1).coerceAtLeast(1)
            val driftX = sin(phase + i * 1.15f) * (size.width * 0.08f)
            val driftY = cos(phase * 1.3f + i * 1.7f) * (size.height * 0.28f)

            val cx = startX + activeWidth * norm + driftX
            val cy = centerY + driftY

            val baseRadius = (size.height * 0.58f + spreadPaddingPx * 0.9f) *
                (0.88f + 0.22f * sin(phase * 1.5f + i * 0.9f)) *
                (1f + burstExpansion * 0.95f)

            val color = palette[i % palette.size].copy(
                alpha = (alphaMultiplier * (1f + burstExpansion * 0.35f)).coerceIn(0f, 1f)
            )
            frameworkPaint.color = color.toArgb()

            canvas.nativeCanvas.drawCircle(cx, cy, baseRadius.coerceAtLeast(10f), frameworkPaint)
        }
    }
}

/**
 * Модификатор рассеянного тумана вокруг любого компонента (капсулы поиска, активной карточки,
 * контекстной таблетки, шапки меню «Три точки», диалогов).
 */
fun Modifier.diffusedFogAura(
    phase: Float,
    isIncognito: Boolean,
    isMonoLogo: Boolean,
    enabled: Boolean = true,
    blurRadius: Dp = 22.dp,
    spread: Dp = 6.dp,
    alpha: Float = 0.82f,
    progressFraction: Float = 1f,
    burstExpansion: Float = 0f
): Modifier = if (!enabled) {
    this
} else {
    this.drawBehind {
        drawDiffusedFogClusters(
            phase = phase,
            isIncognito = isIncognito,
            isMonoLogo = isMonoLogo,
            fallbackColor = Color.White,
            blurRadiusPx = blurRadius.toPx(),
            alphaMultiplier = alpha,
            spreadPaddingPx = spread.toPx(),
            progressFraction = progressFraction,
            burstExpansion = burstExpansion
        )
    }
}

/**
 * Модификатор рассеянного туманного свечения вдоль края открытой панели (верхней или нижней)
 * вместо жёсткой полосы ::before / ::after.
 */
fun Modifier.edgeFogGlow(
    phase: Float,
    isIncognito: Boolean,
    isMonoLogo: Boolean,
    isTopEdge: Boolean,
    enabled: Boolean = true
): Modifier = if (!enabled) {
    this
} else {
    this.drawWithContent {
        drawContent()
        val palette = if (isMonoLogo) {
            listOf(Color.White.copy(alpha = 0.5f))
        } else if (isIncognito) {
            IncognitoFogPalette
        } else {
            NormalFogPalette
        }
        val edgeY = if (isTopEdge) 0f else size.height
        val blurPx = 22.dp.toPx()

        drawIntoCanvas { canvas ->
            val paint = Paint().asFrameworkPaint().apply {
                isAntiAlias = true
                style = android.graphics.Paint.Style.FILL
                maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
            }
            val count = 6
            for (i in 0 until count) {
                val frac = i.toFloat() / (count - 1)
                val x = size.width * frac + sin(phase + i * 1.2f) * 28.dp.toPx()
                val y = edgeY + cos(phase * 1.4f + i) * 4.dp.toPx()
                val r = 28.dp.toPx()
                paint.color = palette[i % palette.size].copy(alpha = 0.85f).toArgb()
                canvas.nativeCanvas.drawCircle(x, y, r, paint)
            }
        }
    }
}

/**
 * Всплеск самого тумана (вместо колец shockwave) при открытии панелей или смене режима Инкогнито:
 * туманное свечение резко расширяется из области события и плавно стягивается обратно.
 */
@Composable
fun FogBurstOverlay(
    triggerCount: Int,
    originYRatio: Float,
    phase: Float,
    isIncognito: Boolean,
    isMonoLogo: Boolean,
    powerSave: Boolean
) {
    if (powerSave || isMonoLogo) return

    val burstAnim = remember { Animatable(0f) }

    LaunchedEffect(triggerCount) {
        if (triggerCount > 0) {
            burstAnim.snapTo(0.01f)
            burstAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 210, easing = FastOutSlowInEasing)
            )
            burstAnim.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing)
            )
        }
    }

    val burstValue = burstAnim.value
    if (burstValue <= 0.01f) return

    val palette = if (isIncognito) IncognitoFogPalette else NormalFogPalette

    Canvas(modifier = Modifier.fillMaxSize()) {
        val centerY = size.height * originYRatio.coerceIn(0.05f, 0.95f)
        val centerX = size.width / 2f
        val blurPx = (42.dp.toPx() * (0.7f + burstValue * 0.9f)).coerceAtLeast(16f)

        drawIntoCanvas { canvas ->
            val paint = Paint().asFrameworkPaint().apply {
                isAntiAlias = true
                style = android.graphics.Paint.Style.FILL
                maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
            }

            val blobCount = 6
            for (i in 0 until blobCount) {
                val angle = (i.toFloat() / blobCount) * (2f * PI.toFloat()) + phase * 0.5f
                val spreadDist = size.width * 0.32f * burstValue
                val bx = centerX + cos(angle) * spreadDist
                val by = centerY + sin(angle) * (spreadDist * 0.45f)
                val radius = (size.width * 0.24f) * (0.4f + burstValue * 0.95f)

                paint.color = palette[i % palette.size]
                    .copy(alpha = (0.72f * burstValue).coerceIn(0f, 0.85f))
                    .toArgb()

                canvas.nativeCanvas.drawCircle(bx, by, radius, paint)
            }
        }
    }
}

/**
 * Логотип `slick` (Kanit Italic 900) БЕЗ пульсации масштаба (никаких scale-колебаний в покое).
 * Внутри букв плавно перетекают только цвета рассеянного тумана.
 * Жесты:
 * - Долгое нажатие (520 мс) -> Глобальные настройки браузера.
 * - Одиночный тап -> запускает скрытый таймер ровно на 1 секунду (1000 мс) БЕЗ полоски под логотипом.
 * - Свайп влево/вправо в течение этой 1 секунды -> включает/выключает режим Инкогнито с упругим сдвигом.
 */
@Composable
fun SlickLiquidLogo(
    phase: Float,
    isIncognito: Boolean,
    isMonoLogo: Boolean,
    powerSave: Boolean,
    onOpenSettings: () -> Unit,
    onToggleIncognito: () -> Unit,
    modifier: Modifier = Modifier
) {
    val slickColors = LocalSlickColors.current
    val scope = rememberCoroutineScope()

    var armedUntilMillis by remember { mutableLongStateOf(0L) }
    val swipeOffsetX = remember { Animatable(0f) }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startPos = down.position
                    val downTime = System.currentTimeMillis()
                    val wasArmedOnDown = downTime <= armedUntilMillis

                    var longPressFired = false
                    var movedTooFar = false
                    var totalDx = 0f
                    var totalDy = 0f

                    val longPressJob: Job = scope.launch {
                        delay(520L)
                        if (!movedTooFar) {
                            longPressFired = true
                            armedUntilMillis = 0L
                            onOpenSettings()
                        }
                    }

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) {
                            change.consume()
                            break
                        }
                        val delta = change.position - startPos
                        totalDx = delta.x
                        totalDy = delta.y

                        if (abs(totalDx) > 14.dp.toPx() || abs(totalDy) > 14.dp.toPx()) {
                            movedTooFar = true
                            longPressJob.cancel()
                        }
                    }

                    longPressJob.cancel()

                    if (!longPressFired) {
                        val now = System.currentTimeMillis()
                        val isSwipeActive = wasArmedOnDown && now <= armedUntilMillis + 350L
                        val swipeThresholdPx = 32.dp.toPx()

                        if (isSwipeActive && abs(totalDx) > swipeThresholdPx && abs(totalDx) > abs(totalDy)) {
                            armedUntilMillis = 0L
                            val direction = if (totalDx > 0f) 1f else -1f
                            onToggleIncognito()
                            scope.launch {
                                swipeOffsetX.animateTo(
                                    targetValue = direction * 44.dp.toPx(),
                                    animationSpec = tween(durationMillis = 130)
                                )
                                swipeOffsetX.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                )
                            }
                        } else if (abs(totalDx) < 14.dp.toPx() && abs(totalDy) < 14.dp.toPx()) {
                            // Одиночный тап взводит скрытый 1-секундный таймер (1000 мс) без полоски
                            armedUntilMillis = System.currentTimeMillis() + 1000L
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "slick",
            style = TextStyle(
                fontFamily = KanitLogoFamily,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
                fontSize = 88.sp,
                letterSpacing = 1.5.sp,
                color = slickColors.fg
            ),
            maxLines = 1,
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 6.dp)
                .graphicsLayer {
                    translationX = swipeOffsetX.value
                    // Обязательный Offscreen-буфер, чтобы туман рисовался строго внутри букв через BlendMode.SrcIn
                    compositingStrategy = CompositingStrategy.Offscreen
                }
                .drawWithContent {
                    drawContent()
                    if (!isMonoLogo && !powerSave) {
                        val palette = if (isIncognito) IncognitoFogPalette else NormalFogPalette
                        val baseTint = if (isIncognito) Color(0xFFB8B8B8) else Color(0xFF7B61FF)

                        // Базовый слой внутри глифов
                        drawRect(
                            color = baseTint,
                            blendMode = BlendMode.SrcIn
                        )

                        // Плавно перетекающие сгустки рассеянного тумана внутри букв
                        drawIntoCanvas { canvas ->
                            val paint = Paint().asFrameworkPaint().apply {
                                isAntiAlias = true
                                style = android.graphics.Paint.Style.FILL
                                xfermode = android.graphics.PorterDuffXfermode(
                                    android.graphics.PorterDuff.Mode.SRC_ATOP
                                )
                                maskFilter = BlurMaskFilter(
                                    28.dp.toPx(),
                                    BlurMaskFilter.Blur.NORMAL
                                )
                            }

                            val count = 8
                            for (i in 0 until count) {
                                val normX = ((i.toFloat() / count) + (phase / (2f * PI.toFloat())) * 0.35f) % 1f
                                val bx = size.width * normX + sin(phase * 1.2f + i) * 18.dp.toPx()
                                val by = size.height * 0.5f + cos(phase * 1.5f + i * 1.9f) * (size.height * 0.38f)
                                val br = (size.height * 0.52f) + sin(phase * 1.8f + i) * 10.dp.toPx()

                                paint.color = palette[i % palette.size].toArgb()
                                canvas.nativeCanvas.drawCircle(bx, by, br, paint)
                            }
                        }
                    }
                }
        )
    }
}
