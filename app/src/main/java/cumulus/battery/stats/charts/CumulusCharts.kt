package cumulus.battery.stats.charts

import android.graphics.Paint
import android.graphics.Path
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext

private fun DpToPx(density: Float, dp: Float): Float {
    return (dp * density)
}

@Composable
fun SingleLineChart(
    modifier: Modifier,
    lineDataArray: IntArray,
    tickMax: Int,
    lineColor: Color
) {
    val density = LocalContext.current.resources.displayMetrics.density
    Canvas(
        modifier = modifier
    ) {
        val nativeCanvas = drawContext.canvas.nativeCanvas

        for (i in 0..4) {
            val y = DpToPx(density, 5f) + (size.height - DpToPx(density, 10f)) * i / 4
            drawLine(
                color = Color(0xFFAAAAAA),
                strokeWidth = DpToPx(density, 1f),
                start = Offset(x = 0f, y = y),
                end = Offset(x = size.width - DpToPx(density, 20f), y = y),
                pathEffect = PathEffect.dashPathEffect(
                    intervals = floatArrayOf(DpToPx(density, 1f), DpToPx(density, 1f)),
                    phase = 0f
                )
            )
        }

        val tickPaint = Paint().let {
            it.apply {
                style = Paint.Style.FILL
                strokeWidth = DpToPx(density, 1f)
                textSize = DpToPx(density, 8f)
                color = Color(0xFFAAAAAA).toArgb()
                isAntiAlias = true
            }
        }
        for (i in 0..4) {
            val tickValue = tickMax.toDouble() * i / 4
            if (tickValue % 1.0 == 0.0) {
                val text = tickValue.toInt().toString()
                val y = (size.height - DpToPx(density, 2f)) -
                        (size.height - DpToPx(density, 10f)) * i / 4
                nativeCanvas.drawText(text, size.width - DpToPx(density, 15f), y, tickPaint)
            }
        }

        val chartPaint = Paint().let {
            it.apply {
                style = Paint.Style.STROKE
                strokeWidth = DpToPx(density, 2f)
                color = lineColor.toArgb()
                isAntiAlias = true
            }
        }
        if (lineDataArray.lastIndex > 2) {
            val chartPath = Path()
            val startY =
                (size.height - DpToPx(density, 5f)) -
                        (size.height - DpToPx(density, 10f)) * lineDataArray[0] / tickMax
            chartPath.moveTo(0f, startY)
            var previousPoint = Offset(0f, startY)
            for (i in 1..lineDataArray.lastIndex) {
                if (lineDataArray[i] <= tickMax && lineDataArray[i] >= 0) {
                    val x = (size.width - DpToPx(density, 20f)) * i / lineDataArray.lastIndex
                    val y = (size.height - DpToPx(density, 5f)) -
                            (size.height - DpToPx(density, 10f)) * lineDataArray[i] / tickMax
                    val currentPoint = Offset(x, y)
                    val bezierControlPoint1 =
                        previousPoint + Offset((currentPoint.x - previousPoint.x) * 0.25f, 0f)
                    val bezierControlPoint2 =
                        currentPoint - Offset((currentPoint.x - previousPoint.x) * 0.25f, 0f)
                    chartPath.cubicTo(
                        bezierControlPoint1.x,
                        bezierControlPoint1.y,
                        bezierControlPoint2.x,
                        bezierControlPoint2.y,
                        currentPoint.x,
                        currentPoint.y
                    )
                    previousPoint = currentPoint
                }
            }
            nativeCanvas.drawPath(chartPath, chartPaint)
        }
    }
}

@Composable
fun MultiLineChart(
    modifier: Modifier,
    line0DataArray: IntArray,
    line1DataArray: IntArray,
    tick0Max: Int,
    tick1Max: Int,
    line0Color: Color,
    line1Color: Color,
    line0Title: String,
    line1Title: String
) {
    val density = LocalContext.current.resources.displayMetrics.density
    Canvas(
        modifier = modifier
    ) {
        val nativeCanvas = drawContext.canvas.nativeCanvas

        for (i in 0..4) {
            val y = DpToPx(density, 5f) + (size.height - DpToPx(density, 30f)) * i / 4
            drawLine(
                color = Color(0xFFAAAAAA),
                strokeWidth = DpToPx(density, 1f),
                start = Offset(x = DpToPx(density, 20f), y = y),
                end = Offset(x = size.width - DpToPx(density, 20f), y = y),
                pathEffect = PathEffect.dashPathEffect(
                    intervals = floatArrayOf(DpToPx(density, 1f), DpToPx(density, 1f)),
                    phase = 0f
                )
            )
        }

        val tickPaint = Paint().let {
            it.apply {
                style = Paint.Style.FILL
                strokeWidth = DpToPx(density, 1f)
                textSize = DpToPx(density, 8f)
                color = Color(0xFFAAAAAA).toArgb()
                isAntiAlias = true
            }
        }
        for (i in 0..4) {
            val tickValue = tick0Max.toDouble() * i / 4
            if (tickValue % 1.0 == 0.0) {
                val text = tickValue.toInt().toString()
                val y = (size.height - DpToPx(density, 22f)) -
                        (size.height - DpToPx(density, 30f)) * i / 4
                nativeCanvas.drawText(text, 0f, y, tickPaint)
            }
        }
        for (i in 1..4) {
            val tickValue = tick1Max.toDouble() * i / 4
            if (tickValue % 1.0 == 0.0) {
                val text = tickValue.toInt().toString()
                val y = (size.height - DpToPx(density, 22f)) -
                        (size.height - DpToPx(density, 30f)) * i / 4
                nativeCanvas.drawText(text, (size.width - DpToPx(density, 15f)), y, tickPaint)
            }
        }

        if (line0DataArray.lastIndex > 2) {
            val chartPath = Path()
            val startX = DpToPx(density, 20f)
            val startY = (size.height - DpToPx(density, 25f)) -
                    (size.height - DpToPx(density, 30f)) * line0DataArray[0] / tick0Max
            chartPath.moveTo(startX, startY)
            var previousPoint = Offset(startX, startY)
            for (i in 1..line0DataArray.lastIndex) {
                if (line0DataArray[i] <= tick0Max && line0DataArray[i] >= 0) {
                    val x =
                        startX + (size.width - DpToPx(density, 40f)) * i / line0DataArray.lastIndex
                    val y = (size.height - DpToPx(density, 25f)) -
                            (size.height - DpToPx(density, 30f)) * line0DataArray[i] / tick0Max
                    val currentPoint = Offset(x, y)
                    val bezierControlPoint1 =
                        previousPoint + Offset((currentPoint.x - previousPoint.x) * 0.25f, 0f)
                    val bezierControlPoint2 =
                        currentPoint - Offset((currentPoint.x - previousPoint.x) * 0.25f, 0f)
                    chartPath.cubicTo(
                        bezierControlPoint1.x,
                        bezierControlPoint1.y,
                        bezierControlPoint2.x,
                        bezierControlPoint2.y,
                        currentPoint.x,
                        currentPoint.y
                    )
                    previousPoint = currentPoint
                }
            }
            val chartPaint = Paint().let {
                it.apply {
                    style = Paint.Style.STROKE
                    strokeWidth = DpToPx(density, 2f)
                    color = line0Color.toArgb()
                    isAntiAlias = true
                }
            }
            nativeCanvas.drawPath(chartPath, chartPaint)
        }

        if (line1DataArray.lastIndex > 2) {
            val chartPath = Path()
            val startX = DpToPx(density, 20f)
            val startY = (size.height - DpToPx(density, 25f)) -
                    (size.height - DpToPx(density, 30f)) * line1DataArray[0] / tick1Max
            chartPath.moveTo(startX, startY)
            var previousPoint = Offset(startX, startY)
            for (i in 1..line1DataArray.lastIndex) {
                if (line1DataArray[i] <= tick1Max && line1DataArray[i] >= 0) {
                    val x =
                        startX + (size.width - DpToPx(density, 40f)) * i / line1DataArray.lastIndex
                    val y = (size.height - DpToPx(density, 25f)) -
                            (size.height - DpToPx(density, 30f)) * line1DataArray[i] / tick1Max
                    val currentPoint = Offset(x, y)
                    val bezierControlPoint1 =
                        previousPoint + Offset((currentPoint.x - previousPoint.x) * 0.25f, 0f)
                    val bezierControlPoint2 =
                        currentPoint - Offset((currentPoint.x - previousPoint.x) * 0.25f, 0f)
                    chartPath.cubicTo(
                        bezierControlPoint1.x,
                        bezierControlPoint1.y,
                        bezierControlPoint2.x,
                        bezierControlPoint2.y,
                        currentPoint.x,
                        currentPoint.y
                    )
                    previousPoint = currentPoint
                }
            }
            val chartPaint = Paint().let {
                it.apply {
                    style = Paint.Style.STROKE
                    strokeWidth = DpToPx(density, 2f)
                    color = line1Color.toArgb()
                    isAntiAlias = true
                }
            }
            nativeCanvas.drawPath(chartPath, chartPaint)
        }

        drawRect(
            color = line0Color,
            topLeft = Offset(DpToPx(density, 30f), size.height - DpToPx(density, 15f)),
            size = Size(width = DpToPx(density, 15f), height = DpToPx(density, 10f))
        )
        val line0TitlePaint = Paint().let {
            it.apply {
                style = Paint.Style.FILL
                strokeWidth = DpToPx(density, 1f)
                textSize = DpToPx(density, 8f)
                color = line0Color.toArgb()
                isAntiAlias = true
            }
        }
        nativeCanvas.drawText(
            line0Title,
            DpToPx(density, 50f),
            size.height - DpToPx(density, 8f),
            line0TitlePaint
        )

        drawRect(
            color = line1Color,
            topLeft = Offset(
                (size.width - DpToPx(density, 40f)) / 2,
                size.height - DpToPx(density, 15f)
            ),
            size = Size(width = DpToPx(density, 15f), height = DpToPx(density, 10f))
        )
        val line1TitlePaint = Paint().let {
            it.apply {
                style = Paint.Style.FILL
                strokeWidth = DpToPx(density, 1f)
                textSize = DpToPx(density, 8f)
                color = line1Color.toArgb()
                isAntiAlias = true
            }
        }
        nativeCanvas.drawText(
            line1Title,
            (size.width - DpToPx(density, 40f)) / 2 + DpToPx(density, 20f),
            size.height - DpToPx(density, 8f),
            line1TitlePaint
        )
    }
}