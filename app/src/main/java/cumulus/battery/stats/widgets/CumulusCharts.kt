package cumulus.battery.stats.widgets

import android.graphics.Paint
import android.graphics.Path
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext

typealias DataPointList = List<Pair<UInt, UInt>>
typealias DataPointMutableList = MutableList<Pair<UInt, UInt>>

private fun SimplifyDataPoints(dataPoints: DataPointList): DataPointList {
    if (dataPoints.size > 1000) {
        val simplifiedDataPoints: DataPointMutableList = mutableListOf()
        val factor = dataPoints.size.toDouble() / 500.0
        for (i in 0 until 500) {
            simplifiedDataPoints.add(dataPoints[(factor * i).toInt()])
        }
        simplifiedDataPoints.add(dataPoints.last())
        return simplifiedDataPoints
    }
    return dataPoints
}

private fun GetDataPointsTickMax(dataPoints: DataPointList): UInt {
    var tickMax = 10U
    dataPoints.forEach { dataPoint ->
        if (dataPoint.second > tickMax) {
            tickMax = dataPoint.second
        }
    }
    if ((tickMax % 10U) == 0U) {
        return tickMax
    }
    return ((tickMax / 10U + 1U) * 10U)
}

private fun DpToPx(density: Float, dp: Float): Float {
    return (dp * density)
}

@Composable
fun SingleLineChart(
    modifier: Modifier,
    dataPointList: DataPointList,
    lineColor: Color
) {
    val density = LocalContext.current.resources.displayMetrics.density
    val primaryColor = MaterialTheme.colorScheme.primary
    val tickMarkColor = Color(0xFFAAAAAA)
    val dataPoints = SimplifyDataPoints(dataPointList)
    val tickMax = GetDataPointsTickMax(dataPoints)

    Canvas(
        modifier = modifier
    ) {
        val nativeCanvas = drawContext.canvas.nativeCanvas

        for (i in 0..4) {
            val y = DpToPx(density, 5f) + (size.height - DpToPx(density, 10f)) * i / 4
            drawLine(
                color = tickMarkColor,
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
                color = primaryColor.toArgb()
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
        if (dataPoints.lastIndex > 2) {
            val chartPath = Path()
            val startX = 0f
            val startY = (size.height - DpToPx(density, 5f)) -
                    (size.height - DpToPx(density, 10f)) *
                    (dataPoints[0].second.toFloat() / tickMax.toFloat())
            chartPath.moveTo(startX, startY)
            var previousPoint = Offset(startX, startY)
            for (i in 1..dataPoints.lastIndex) {
                val x = (size.width - DpToPx(density, 20f)) *
                        (dataPoints[i].first.toFloat() / dataPoints.last().first.toFloat())
                val y = (size.height - DpToPx(density, 5f)) -
                        (size.height - DpToPx(density, 10f)) *
                        (dataPoints[i].second.toFloat() / tickMax.toFloat())
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
            nativeCanvas.drawPath(chartPath, chartPaint)
        }
    }
}

@Composable
fun MultiLineChart(
    modifier: Modifier,
    dataPointList0: DataPointList,
    dataPointList1: DataPointList,
    line0Color: Color,
    line1Color: Color,
    line0Title: String,
    line1Title: String
) {
    val density = LocalContext.current.resources.displayMetrics.density
    val primaryColor = MaterialTheme.colorScheme.primary
    val tickMarkColor = Color(0xFFAAAAAA)
    val line0DataPoints = SimplifyDataPoints(dataPointList0)
    val line1DataPoints = SimplifyDataPoints(dataPointList1)
    val line0TickMax = GetDataPointsTickMax(line0DataPoints)
    val line1TickMax = GetDataPointsTickMax(line1DataPoints)

    Canvas(
        modifier = modifier
    ) {
        val nativeCanvas = drawContext.canvas.nativeCanvas

        for (i in 0..4) {
            val y = DpToPx(density, 5f) + (size.height - DpToPx(density, 30f)) * i / 4
            drawLine(
                color = tickMarkColor,
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
                color = primaryColor.toArgb()
                isAntiAlias = true
            }
        }
        for (i in 0..4) {
            val tickValue = line0TickMax.toDouble() * i / 4
            if (tickValue % 1.0 == 0.0) {
                val text = tickValue.toInt().toString()
                val y = (size.height - DpToPx(density, 22f)) -
                        (size.height - DpToPx(density, 30f)) * i / 4
                nativeCanvas.drawText(text, 0f, y, tickPaint)
            }
        }
        for (i in 1..4) {
            val tickValue = line1TickMax.toDouble() * i / 4
            if (tickValue % 1.0 == 0.0) {
                val text = tickValue.toInt().toString()
                val y = (size.height - DpToPx(density, 22f)) -
                        (size.height - DpToPx(density, 30f)) * i / 4
                nativeCanvas.drawText(text, (size.width - DpToPx(density, 15f)), y, tickPaint)
            }
        }

        if (line0DataPoints.lastIndex > 2) {
            val chartPath = Path()
            val startX = DpToPx(density, 20f)
            val startY = (size.height - DpToPx(density, 25f)) -
                    (size.height - DpToPx(density, 30f)) *
                    (line0DataPoints[0].second.toFloat() / line0TickMax.toFloat())
            chartPath.moveTo(startX, startY)
            var previousPoint = Offset(startX, startY)
            for (i in 1..line0DataPoints.lastIndex) {
                val x = DpToPx(density, 20f) + (size.width - DpToPx(density, 40f)) *
                        (line0DataPoints[i].first.toFloat() / line0DataPoints.last().first.toFloat())
                val y = (size.height - DpToPx(density, 25f)) -
                        (size.height - DpToPx(density, 30f)) *
                        (line0DataPoints[i].second.toFloat() / line0TickMax.toFloat())
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

        if (line1DataPoints.lastIndex > 2) {
            val chartPath = Path()
            val startX = DpToPx(density, 20f)
            val startY = (size.height - DpToPx(density, 25f)) -
                    (size.height - DpToPx(density, 30f)) *
                    (line1DataPoints[0].second.toFloat() / line1TickMax.toFloat())
            chartPath.moveTo(startX, startY)
            var previousPoint = Offset(startX, startY)
            for (i in 1..line1DataPoints.lastIndex) {
                val x = DpToPx(density, 20f) + (size.width - DpToPx(density, 40f)) *
                        (line1DataPoints[i].first.toFloat() / line1DataPoints.last().first.toFloat())
                val y = (size.height - DpToPx(density, 25f)) -
                        (size.height - DpToPx(density, 30f)) *
                        (line1DataPoints[i].second.toFloat() / line1TickMax.toFloat())
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