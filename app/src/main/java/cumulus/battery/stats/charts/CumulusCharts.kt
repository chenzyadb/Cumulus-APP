package cumulus.battery.stats.charts

import android.graphics.Paint
import android.graphics.Path
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb

@Composable
fun SingleLineChart(
    modifier: Modifier,
    lineDataArray: IntArray,
    tickMax: Int,
    lineColor: Color
) {
    Canvas(
        modifier = modifier
    ) {
        val nativeCanvas = drawContext.canvas.nativeCanvas
        for (i in 0..4) {
            val y = 10f + (size.height - 20f) * i / 4
            drawLine(
                color = Color(0xFF888888),
                strokeWidth = 4f,
                start = Offset(x = 10f, y = y),
                end = Offset(x = size.width - 50f, y = y),
            )
        }
        val tickPaint = Paint().let {
            it.apply {
                style = Paint.Style.FILL
                strokeWidth = 4f
                textSize = 28f
                color = Color(0xFF888888).toArgb()
                isAntiAlias = true
            }
        }
        for (i in 1..4) {
            val text = (tickMax * i / 4).toString()
            val y = (size.height - 5f) - (size.height - 20f) * i / 4
            nativeCanvas.drawText(text, size.width - 40f, y, tickPaint)
        }
        val chartPaint = Paint().let {
            it.apply {
                style = Paint.Style.STROKE
                strokeWidth = 8f
                color = lineColor.toArgb()
                isAntiAlias = true
            }
        }
        if (lineDataArray.lastIndex > 2) {
            val chartPath = Path()
            val startX = 10f
            val startY = (size.height - 10f) - (size.height - 20f) * lineDataArray[0] / tickMax
            chartPath.moveTo(startX, startY)
            var previousPoint = Offset(startX, startY)
            for (i in 1..lineDataArray.lastIndex) {
                if (lineDataArray[i] <= tickMax && lineDataArray[i] >= 0) {
                    val x = 10f + (size.width - 60f) * i / lineDataArray.lastIndex
                    val y = (size.height - 10f) - (size.height - 20f) * lineDataArray[i] / tickMax
                    val currentPoint = Offset(x, y)
                    val bezierControlPoint1 = previousPoint + Offset((currentPoint.x - previousPoint.x) * 0.25f, 0f)
                    val bezierControlPoint2 = currentPoint - Offset((currentPoint.x - previousPoint.x) * 0.25f, 0f)
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
    Canvas(
        modifier = modifier
    ) {
        val nativeCanvas = drawContext.canvas.nativeCanvas
        for (i in 0..4) {
            val y = 10f + (size.height - 60f) * i / 4
            drawLine(
                color = Color(0xFF888888),
                strokeWidth = 4f,
                start = Offset(x = 80f, y = y),
                end = Offset(x = size.width - 80f, y = y),
            )
        }
        val tickPaint = Paint().let {
            it.apply {
                style = Paint.Style.FILL
                strokeWidth = 4f
                textSize = 28f
                color = Color(0xFF888888).toArgb()
                isAntiAlias = true
            }
        }
        for (i in 1..4) {
            val text = (tick0Max * i / 4).toString()
            val y = (size.height - 45f) - (size.height - 60f) * i / 4
            nativeCanvas.drawText(text, 0f, y, tickPaint)
        }
        for (i in 1..4) {
            val text = (tick1Max * i / 4).toString()
            val y = (size.height - 45f) - (size.height - 60f) * i / 4
            nativeCanvas.drawText(text, size.width - 70f, y, tickPaint)
        }
        if (line0DataArray.lastIndex > 2) {
            val chartPath = Path()
            val startX = 80f
            val startY = (size.height - 50f) - (size.height - 60f) * line0DataArray[0] / tick0Max
            chartPath.moveTo(startX, startY)
            var previousPoint = Offset(startX, startY)
            for (i in 1..line0DataArray.lastIndex) {
                if (line0DataArray[i] <= tick0Max && line0DataArray[i] >= 0) {
                    val x = startX + (size.width - 160f) * i / line0DataArray.lastIndex
                    val y = (size.height - 50f) - (size.height - 60f) * line0DataArray[i] / tick0Max
                    val currentPoint = Offset(x, y)
                    val bezierControlPoint1 = previousPoint + Offset((currentPoint.x - previousPoint.x) * 0.25f, 0f)
                    val bezierControlPoint2 = currentPoint - Offset((currentPoint.x - previousPoint.x) * 0.25f, 0f)
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
                    strokeWidth = 4f
                    color = line0Color.toArgb()
                    isAntiAlias = true
                }
            }
            nativeCanvas.drawPath(chartPath, chartPaint)
        }
        if (line1DataArray.lastIndex > 2) {
            val chartPath = Path()
            val startX = 80f
            val startY = (size.height - 50f) - (size.height - 60f) * line1DataArray[0] / tick1Max
            chartPath.moveTo(startX, startY)
            var previousPoint = Offset(startX, startY)
            for (i in 1..line1DataArray.lastIndex) {
                if (line1DataArray[i] <= tick1Max && line1DataArray[i] >= 0) {
                    val x = startX + (size.width - 160f) * i / line1DataArray.lastIndex
                    val y = (size.height - 50f) - (size.height - 60f) * line1DataArray[i] / tick1Max
                    val currentPoint = Offset(x, y)
                    val bezierControlPoint1 = previousPoint + Offset((currentPoint.x - previousPoint.x) * 0.25f, 0f)
                    val bezierControlPoint2 = currentPoint - Offset((currentPoint.x - previousPoint.x) * 0.25f, 0f)
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
                    strokeWidth = 4f
                    color = line1Color.toArgb()
                    isAntiAlias = true
                }
            }
            nativeCanvas.drawPath(chartPath, chartPaint)
        }
        drawRect(
            color = line0Color,
            topLeft = Offset(100f, size.height - 40f),
            size = Size(width = 50f, height = 40f)
        )
        val line0TitlePaint = Paint().let {
            it.apply {
                style = Paint.Style.FILL
                strokeWidth = 4f
                textSize = 28f
                color = line0Color.toArgb()
                isAntiAlias = true
            }
        }
        nativeCanvas.drawText(line0Title, 160f, size.height - 10f, line0TitlePaint)
        drawRect(
            color = line1Color,
            topLeft = Offset((size.width - 160f) / 2 + 20f, size.height - 40f),
            size = Size(width = 50f, height = 40f)
        )
        val line1TitlePaint = Paint().let {
            it.apply {
                style = Paint.Style.FILL
                strokeWidth = 4f
                textSize = 28f
                color = line1Color.toArgb()
                isAntiAlias = true
            }
        }
        nativeCanvas.drawText(line1Title, (size.width - 160f) / 2 + 80f, size.height - 10f, line1TitlePaint)
    }
}