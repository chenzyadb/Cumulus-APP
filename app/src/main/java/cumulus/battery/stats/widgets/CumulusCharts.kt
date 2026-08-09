package cumulus.battery.stats.widgets

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

typealias DataPointList = List<Pair<Float, Float>>
typealias DataPointMutableList = MutableList<Pair<Float, Float>>

private const val MAX_CHART_DATA_POINTS = 500
private const val REGULAR_CHART_TICK_INTERVALS = 3
private const val FIXED_RANGE_TICK_INTERVALS = 4
private val NICE_STEP_MULTIPLIERS = doubleArrayOf(1.0, 1.5, 2.0, 2.5, 3.0, 4.0, 5.0, 6.0, 8.0, 10.0)
private val AVERAGE_LINE_COLOR = Color(0xFF666666)

private fun NormalizeDataPoints(dataPoints: DataPointList): DataPointList {
    if (dataPoints.size <= 1) {
        return dataPoints
    }

    val normalizedDataPoints: DataPointMutableList = mutableListOf()
    for (dataPoint in dataPoints.sortedBy { it.first }) {
        if (normalizedDataPoints.isNotEmpty() &&
            normalizedDataPoints.last().first == dataPoint.first
        ) {
            normalizedDataPoints[normalizedDataPoints.lastIndex] = dataPoint
        } else {
            normalizedDataPoints.add(dataPoint)
        }
    }
    return normalizedDataPoints
}

private fun SimplifyDataPoints(dataPoints: DataPointList): DataPointList {
    if (dataPoints.size <= MAX_CHART_DATA_POINTS) {
        return dataPoints
    }

    val simplifiedDataPoints: DataPointMutableList = mutableListOf()
    simplifiedDataPoints.add(dataPoints.first())
    val bucketSize = (dataPoints.size - 2).toDouble() / (MAX_CHART_DATA_POINTS - 2)
    var selectedIndex = 0
    for (bucketIndex in 0 until MAX_CHART_DATA_POINTS - 2) {
        val nextBucketStart = ((bucketIndex + 1) * bucketSize).toInt() + 1
        val nextBucketEnd = minOf(((bucketIndex + 2) * bucketSize).toInt() + 1, dataPoints.size)
        var averageX = 0.0
        var averageY = 0.0
        for (i in nextBucketStart until nextBucketEnd) {
            averageX += dataPoints[i].first.toDouble()
            averageY += dataPoints[i].second.toDouble()
        }

        val averageCount = nextBucketEnd - nextBucketStart
        if (averageCount > 0) {
            averageX /= averageCount
            averageY /= averageCount
        }

        val bucketStart = (bucketIndex * bucketSize).toInt() + 1
        val bucketEnd = minOf(nextBucketStart, dataPoints.size - 1)
        val pointAX = dataPoints[selectedIndex].first.toDouble()
        val pointAY = dataPoints[selectedIndex].second.toDouble()
        var maxArea = -1.0
        var maxAreaIndex = bucketStart
        for (i in bucketStart until bucketEnd) {
            val area = abs(
                (pointAX - averageX) * (dataPoints[i].second.toDouble() - pointAY) -
                        (pointAX - dataPoints[i].first.toDouble()) * (averageY - pointAY)
            )
            if (area > maxArea) {
                maxArea = area
                maxAreaIndex = i
            }
        }

        simplifiedDataPoints.add(dataPoints[maxAreaIndex])
        selectedIndex = maxAreaIndex
    }
    simplifiedDataPoints.add(dataPoints.last())
    return simplifiedDataPoints
}

private fun GetDataPointsTickValues(
    dataPoints: DataPointList,
    yAxisMaxValue: Int? = null
): List<Int> {
    if (yAxisMaxValue != null) {
        require(yAxisMaxValue > 0) { "yAxisMaxValue must be greater than zero" }
        return (0..FIXED_RANGE_TICK_INTERVALS).map { interval ->
            (yAxisMaxValue.toDouble() * interval / FIXED_RANGE_TICK_INTERVALS).roundToInt()
        }.distinct()
    }

    var dataMax = 0f
    for (dataPoint in dataPoints) {
        if (dataPoint.second > dataMax) {
            dataMax = dataPoint.second
        }
    }

    val rawStep = dataMax.toDouble() / REGULAR_CHART_TICK_INTERVALS
    var magnitude = 1.0
    var niceStep = 0.0
    while (niceStep == 0.0) {
        for (multiplier in NICE_STEP_MULTIPLIERS) {
            val step = magnitude * multiplier
            if (step >= rawStep && step % 1.0 == 0.0) {
                niceStep = step
                break
            }
        }
        magnitude *= 10.0
    }

    val tickMax = maxOf(ceil(dataMax / niceStep).toInt() * niceStep.toInt(), 1)
    val tickValues = mutableListOf<Int>()
    var tickValue = 0
    while (tickValue <= tickMax) {
        tickValues.add(tickValue)
        tickValue += niceStep.toInt()
    }

    return tickValues
}

private fun GetDataPointsAverageValue(dataPoints: DataPointList): Float? {
    if (dataPoints.isEmpty()) {
        return null
    }

    var total = 0f
    for (dataPoint in dataPoints) {
        total += dataPoint.second
    }
    return total / dataPoints.size
}

private fun RemoveNearestTickLabelToAverage(
    tickValues: List<Int>,
    averageValue: Float?
): List<Int> {
    if (averageValue == null || tickValues.size <= 2) {
        return tickValues
    }

    val nearestTick = tickValues
        .filter { it != 0 }
        .minByOrNull { abs(it.toFloat() - averageValue) }
        ?: return tickValues
    return tickValues.filter { it != nearestTick }
}

private fun FormatTickValue(value: Float): String {
    val roundedValue = value.roundToInt()
    return if (abs(value - roundedValue.toFloat()) < 0.05f) {
        roundedValue.toString()
    } else {
        String.format("%.1f", value)
    }
}

private fun DpToPx(density: Float, dp: Float): Float {
    return (dp * density)
}

private fun CreateTextPaint(density: Float, color: Color, textAlign: Paint.Align): Paint {
    val paint = Paint()
    paint.style = Paint.Style.FILL
    paint.textSize = DpToPx(density, 8f)
    paint.color = color.toArgb()
    paint.isAntiAlias = true
    paint.textAlign = textAlign
    return paint
}

private fun GetYForValue(
    value: Float,
    tickMax: Float,
    chartTop: Float,
    chartBottom: Float
): Float {
    return chartBottom - (chartBottom - chartTop) * value / tickMax
}

private fun AverageLinePathEffect(density: Float): PathEffect {
    return PathEffect.dashPathEffect(
        intervals = floatArrayOf(DpToPx(density, 3f), DpToPx(density, 3f)),
        phase = 0f
    )
}

private fun GetChartPoints(
    dataPoints: DataPointList,
    tickMax: Float,
    chartLeft: Float,
    chartRight: Float,
    chartTop: Float,
    chartBottom: Float
): List<Offset> {
    val minX = dataPoints.first().first.toDouble()
    val rangeX = (dataPoints.last().first.toDouble() - minX).coerceAtLeast(1.0)
    val points = mutableListOf<Offset>()
    for (dataPoint in dataPoints) {
        points.add(
            Offset(
                x = chartLeft + (chartRight - chartLeft) *
                        ((dataPoint.first.toDouble() - minX) / rangeX).toFloat(),
                y = GetYForValue(dataPoint.second, tickMax, chartTop, chartBottom)
            )
        )
    }
    return points
}

private fun BuildChartPaths(
    dataPoints: DataPointList,
    tickMax: Float,
    chartLeft: Float,
    chartRight: Float,
    chartTop: Float,
    chartBottom: Float
): Pair<Path, Path> {
    val points = GetChartPoints(dataPoints, tickMax, chartLeft, chartRight, chartTop, chartBottom)
    val linePath = Path()
    val fillPath = Path()
    linePath.moveTo(points[0].x, points[0].y)
    fillPath.moveTo(points[0].x, chartBottom)
    fillPath.lineTo(points[0].x, points[0].y)
    for (i in 1..points.lastIndex) {
        val previousPoint = points[i - 1]
        val currentPoint = points[i]
        val controlOffsetX = (currentPoint.x - previousPoint.x) / 2f
        val controlPoint1 = Offset(
            x = previousPoint.x + controlOffsetX,
            y = previousPoint.y
        )
        val controlPoint2 = Offset(
            x = currentPoint.x - controlOffsetX,
            y = currentPoint.y
        )
        linePath.cubicTo(
            controlPoint1.x, controlPoint1.y,
            controlPoint2.x, controlPoint2.y,
            currentPoint.x, currentPoint.y
        )
        fillPath.cubicTo(
            controlPoint1.x, controlPoint1.y,
            controlPoint2.x, controlPoint2.y,
            currentPoint.x, currentPoint.y
        )
    }
    fillPath.lineTo(points.last().x, chartBottom)
    fillPath.close()
    return Pair(linePath, fillPath)
}

@Composable
fun SingleLineChart(
    modifier: Modifier,
    dataPointList: DataPointList,
    lineColor: Color,
    yAxisMaxValue: Int? = null
) {
    val density = LocalContext.current.resources.displayMetrics.density
    val primaryColor = MaterialTheme.colorScheme.primary
    val tickMarkColor = Color(0xFFAAAAAA)
    val dataPoints = SimplifyDataPoints(NormalizeDataPoints(dataPointList))
    val tickValues = GetDataPointsTickValues(dataPoints, yAxisMaxValue)
    val tickMax = tickValues.last().toFloat()

    Canvas(
        modifier = modifier
    ) {
        val nativeCanvas = drawContext.canvas.nativeCanvas
        val chartLeft = DpToPx(density, 2f)
        val chartRight = size.width - DpToPx(density, 20f)
        val chartTop = DpToPx(density, 5f)
        val chartBottom = size.height - DpToPx(density, 5f)

        val gridColor = tickMarkColor.copy(alpha = 0.5f)
        for (tickValue in tickValues) {
            val y = GetYForValue(tickValue.toFloat(), tickMax, chartTop, chartBottom)
            drawLine(
                color = gridColor,
                strokeWidth = DpToPx(density, 0.5f),
                start = Offset(x = 0f, y = y),
                end = Offset(x = chartRight, y = y),
                pathEffect = PathEffect.dashPathEffect(
                    intervals = floatArrayOf(DpToPx(density, 4f), DpToPx(density, 4f)),
                    phase = 0f
                )
            )
        }

        val tickPaint = CreateTextPaint(density, primaryColor, Paint.Align.RIGHT)
        for (tickValue in tickValues) {
            val y = GetYForValue(tickValue.toFloat(), tickMax, chartTop, chartBottom) +
                    DpToPx(density, 3f)
            nativeCanvas.drawText(
                tickValue.toString(),
                size.width - DpToPx(density, 2f),
                y,
                tickPaint
            )
        }

        if (dataPoints.size >= 2) {
            val (linePath, fillPath) = BuildChartPaths(
                dataPoints = dataPoints,
                tickMax = tickMax,
                chartLeft = chartLeft,
                chartRight = chartRight,
                chartTop = chartTop,
                chartBottom = chartBottom
            )
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(lineColor.copy(alpha = 0.25f), lineColor.copy(alpha = 0f)),
                    startY = chartTop,
                    endY = chartBottom
                )
            )
            drawPath(
                path = linePath,
                color = lineColor,
                style = Stroke(
                    width = DpToPx(density, 2f),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
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
    val line0DataPoints = SimplifyDataPoints(NormalizeDataPoints(dataPointList0))
    val line1DataPoints = SimplifyDataPoints(NormalizeDataPoints(dataPointList1))
    val line0TickValues = GetDataPointsTickValues(line0DataPoints)
    val line1TickValues = GetDataPointsTickValues(line1DataPoints)
    val line0TickMax = line0TickValues.last().toFloat()
    val line1TickMax = line1TickValues.last().toFloat()
    val line0AverageValue = GetDataPointsAverageValue(line0DataPoints)
    val line1AverageValue = GetDataPointsAverageValue(line1DataPoints)
    val line0TickLabelValues = RemoveNearestTickLabelToAverage(line0TickValues, line0AverageValue)
    val line1TickLabelValues = RemoveNearestTickLabelToAverage(line1TickValues, line1AverageValue)

    Canvas(
        modifier = modifier
    ) {
        val nativeCanvas = drawContext.canvas.nativeCanvas
        val chartLeft = DpToPx(density, 20f)
        val chartRight = size.width - DpToPx(density, 20f)
        val chartTop = DpToPx(density, 5f)
        val chartBottom = size.height - DpToPx(density, 25f)

        val gridColor = tickMarkColor.copy(alpha = 0.5f)
        for (tickValue in line0TickValues) {
            val y = GetYForValue(tickValue.toFloat(), line0TickMax, chartTop, chartBottom)
            drawLine(
                color = gridColor,
                strokeWidth = DpToPx(density, 0.5f),
                start = Offset(x = chartLeft, y = y),
                end = Offset(x = chartRight, y = y),
                pathEffect = PathEffect.dashPathEffect(
                    intervals = floatArrayOf(DpToPx(density, 4f), DpToPx(density, 4f)),
                    phase = 0f
                )
            )
        }

        val leftTickPaint = CreateTextPaint(density, primaryColor, Paint.Align.LEFT)
        for (tickValue in line0TickLabelValues) {
            val y = GetYForValue(tickValue.toFloat(), line0TickMax, chartTop, chartBottom) +
                    DpToPx(density, 3f)
            nativeCanvas.drawText(tickValue.toString(), DpToPx(density, 2f), y, leftTickPaint)
        }
        val rightTickPaint = CreateTextPaint(density, primaryColor, Paint.Align.RIGHT)
        for (tickValue in line1TickLabelValues.drop(1)) {
            val y = GetYForValue(tickValue.toFloat(), line1TickMax, chartTop, chartBottom) +
                    DpToPx(density, 3f)
            nativeCanvas.drawText(
                tickValue.toString(),
                size.width - DpToPx(density, 2f),
                y,
                rightTickPaint
            )
        }

        if (line0AverageValue != null) {
            val averageY = GetYForValue(line0AverageValue, line0TickMax, chartTop, chartBottom)
            drawLine(
                color = AVERAGE_LINE_COLOR,
                strokeWidth = DpToPx(density, 1f),
                start = Offset(x = chartLeft, y = averageY),
                end = Offset(x = chartRight, y = averageY),
                pathEffect = AverageLinePathEffect(density)
            )
            val averageTickPaint = CreateTextPaint(density, AVERAGE_LINE_COLOR, Paint.Align.LEFT)
            nativeCanvas.drawText(
                FormatTickValue(line0AverageValue),
                DpToPx(density, 2f),
                averageY - DpToPx(density, 2f),
                averageTickPaint
            )
        }
        if (line1AverageValue != null) {
            val averageY = GetYForValue(line1AverageValue, line1TickMax, chartTop, chartBottom)
            drawLine(
                color = AVERAGE_LINE_COLOR,
                strokeWidth = DpToPx(density, 1f),
                start = Offset(x = chartLeft, y = averageY),
                end = Offset(x = chartRight, y = averageY),
                pathEffect = AverageLinePathEffect(density)
            )
            val averageTickPaint = CreateTextPaint(density, AVERAGE_LINE_COLOR, Paint.Align.RIGHT)
            nativeCanvas.drawText(
                FormatTickValue(line1AverageValue),
                size.width - DpToPx(density, 2f),
                averageY - DpToPx(density, 2f),
                averageTickPaint
            )
        }

        val line0Paths = if (line0DataPoints.size >= 2) {
            BuildChartPaths(
                dataPoints = line0DataPoints,
                tickMax = line0TickMax,
                chartLeft = chartLeft,
                chartRight = chartRight,
                chartTop = chartTop,
                chartBottom = chartBottom
            )
        } else {
            null
        }
        val line1Paths = if (line1DataPoints.size >= 2) {
            BuildChartPaths(
                dataPoints = line1DataPoints,
                tickMax = line1TickMax,
                chartLeft = chartLeft,
                chartRight = chartRight,
                chartTop = chartTop,
                chartBottom = chartBottom
            )
        } else {
            null
        }
        if (line0Paths != null) {
            drawPath(
                path = line0Paths.second,
                brush = Brush.verticalGradient(
                    colors = listOf(line0Color.copy(alpha = 0.15f), line0Color.copy(alpha = 0f)),
                    startY = chartTop,
                    endY = chartBottom
                )
            )
        }
        if (line1Paths != null) {
            drawPath(
                path = line1Paths.second,
                brush = Brush.verticalGradient(
                    colors = listOf(line1Color.copy(alpha = 0.15f), line1Color.copy(alpha = 0f)),
                    startY = chartTop,
                    endY = chartBottom
                )
            )
        }
        val lineStroke = Stroke(
            width = DpToPx(density, 2f),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
        if (line0Paths != null) {
            drawPath(path = line0Paths.first, color = line0Color, style = lineStroke)
        }
        if (line1Paths != null) {
            drawPath(path = line1Paths.first, color = line1Color, style = lineStroke)
        }

        drawRoundRect(
            color = line0Color,
            topLeft = Offset(DpToPx(density, 30f), size.height - DpToPx(density, 15f)),
            size = Size(width = DpToPx(density, 15f), height = DpToPx(density, 10f)),
            cornerRadius = CornerRadius(DpToPx(density, 2f))
        )
        val line0TitlePaint = CreateTextPaint(density, line0Color, Paint.Align.LEFT)
        nativeCanvas.drawText(
            line0Title,
            DpToPx(density, 50f),
            size.height - DpToPx(density, 8f),
            line0TitlePaint
        )

        drawRoundRect(
            color = line1Color,
            topLeft = Offset(
                (size.width - DpToPx(density, 40f)) / 2,
                size.height - DpToPx(density, 15f)
            ),
            size = Size(width = DpToPx(density, 15f), height = DpToPx(density, 10f)),
            cornerRadius = CornerRadius(DpToPx(density, 2f))
        )
        val line1TitlePaint = CreateTextPaint(density, line1Color, Paint.Align.LEFT)
        nativeCanvas.drawText(
            line1Title,
            (size.width - DpToPx(density, 40f)) / 2 + DpToPx(density, 20f),
            size.height - DpToPx(density, 8f),
            line1TitlePaint
        )
    }
}
