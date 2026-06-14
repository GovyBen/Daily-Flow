/*
 * This file is part of Daily Flow.
 *
 * Chart setup is adapted from Track & Graph's AndroidPlot UI layer at
 * commit 4bb925a731e0537f6330971853770e9aafb51983. Daily Flow replaces the
 * upstream graph configuration model with small Compose-facing chart models.
 */

package com.mhss.app.tracking.presentation.analytics.chart

import android.graphics.Color.TRANSPARENT
import android.view.View
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.androidplot.Plot
import com.androidplot.pie.PieChart
import com.androidplot.pie.PieRenderer
import com.androidplot.pie.Segment
import com.androidplot.pie.SegmentFormatter
import com.androidplot.xy.BarFormatter
import com.androidplot.xy.BarRenderer
import com.androidplot.xy.BoundaryMode
import com.androidplot.xy.LineAndPointFormatter
import com.androidplot.xy.SimpleXYSeries
import com.androidplot.xy.StepMode
import com.androidplot.xy.XYGraphWidget
import com.androidplot.xy.XYPlot
import com.mhss.app.tracking.R
import java.text.DecimalFormat
import java.text.FieldPosition
import java.text.Format
import java.text.ParsePosition
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

const val TRACKING_LINE_CHART_TAG = "tracking-line-chart"
const val TRACKING_BAR_CHART_TAG = "tracking-bar-chart"
const val TRACKING_PIE_CHART_TAG = "tracking-pie-chart"
const val TRACKING_CHART_EMPTY_TAG = "tracking-chart-empty"

@Composable
fun TrackingLineChart(
    values: List<TrackingChartValue>,
    title: String,
    modifier: Modifier = Modifier
) {
    val plottable = values.filter { it.value.isFinite() }
    if (plottable.isEmpty()) {
        TrackingChartEmpty(modifier)
        return
    }
    val color = plottable.firstNotNullOfOrNull(TrackingChartValue::color)
        ?: MaterialTheme.colorScheme.primary
    TrackingXYChart(
        values = plottable,
        title = title,
        color = color,
        isBar = false,
        modifier = modifier.testTag(TRACKING_LINE_CHART_TAG)
    )
}

@Composable
fun TrackingBarChart(
    values: List<TrackingChartValue>,
    title: String,
    modifier: Modifier = Modifier
) {
    val plottable = values.filter { it.value.isFinite() }
    if (plottable.isEmpty()) {
        TrackingChartEmpty(modifier)
        return
    }
    val color = plottable.firstNotNullOfOrNull(TrackingChartValue::color)
        ?: MaterialTheme.colorScheme.secondary
    TrackingXYChart(
        values = plottable,
        title = title,
        color = color,
        isBar = true,
        modifier = modifier.testTag(TRACKING_BAR_CHART_TAG)
    )
}

@Composable
fun TrackingPieChart(
    values: List<TrackingChartValue>,
    title: String,
    modifier: Modifier = Modifier
) {
    val plottable = values.filter { it.value.isFinite() && it.value > 0.0 }
    if (plottable.isEmpty()) {
        TrackingChartEmpty(modifier)
        return
    }

    val scheme = MaterialTheme.colorScheme
    val colors = plottable.mapIndexed { index, value ->
        value.color ?: when (index) {
            0 -> scheme.primary
            1 -> scheme.secondary
            2 -> scheme.tertiary
            3 -> scheme.error
            else -> Color.hsv((index * 137.5f) % 360f, 0.65f, 0.85f)
        }
    }
    val description = chartDescription(title, plottable)
    val chartHeight = chartHeight()
    val containerColor = scheme.surface.toArgb()
    val labelTextSize = with(LocalDensity.current) {
        MaterialTheme.typography.bodySmall.fontSize.toPx()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = description
            }
    ) {
        AndroidView(
            factory = { context ->
                PieChart(context, "").apply {
                    layoutManager.remove(legend)
                    layoutManager.remove(this.title)
                    setBorderStyle(Plot.BorderStyle.NONE, null, null)
                }
            },
            update = { plot ->
                plot.clear()
                plot.backgroundPaint.color = containerColor
                plottable.forEachIndexed { index, value ->
                    val formatter = SegmentFormatter(colors[index].toArgb()).apply {
                        labelPaint.color = TRANSPARENT
                        labelPaint.textSize = labelTextSize
                    }
                    plot.addSegment(Segment(value.label, value.value), formatter)
                }
                plot.redraw()
                plot.getRenderer(PieRenderer::class.java)
                    .setDonutSize(0f, PieRenderer.DonutMode.PERCENT)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight)
                .testTag(TRACKING_PIE_CHART_TAG)
        )
        TrackingChartLegend(plottable, colors)
    }
}

@Composable
private fun TrackingXYChart(
    values: List<TrackingChartValue>,
    title: String,
    color: Color,
    isBar: Boolean,
    modifier: Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val onSurface = scheme.onSurface.toArgb()
    val gridColor = scheme.onSurfaceVariant.copy(alpha = 0.28f).toArgb()
    val containerColor = scheme.surface.toArgb()
    val labelTextSize = with(LocalDensity.current) {
        MaterialTheme.typography.bodySmall.fontSize.toPx()
    }
    val description = chartDescription(title, values)
    val chartHeight = chartHeight()

    AndroidView(
        factory = { context ->
            XYPlot(context, "").apply {
                layoutManager.remove(legend)
                layoutManager.remove(rangeTitle)
                layoutManager.remove(domainTitle)
                layoutManager.remove(this.title)
                setBorderStyle(Plot.BorderStyle.NONE, null, null)
                setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            }
        },
        update = { plot ->
            plot.clear()
            setupXYPlot(
                plot = plot,
                labels = values.map(TrackingChartValue::label),
                values = values.map(TrackingChartValue::value),
                onSurface = onSurface,
                gridColor = gridColor,
                containerColor = containerColor,
                labelTextSize = labelTextSize,
                isBar = isBar
            )
            val series = SimpleXYSeries(
                values.indices.map(Int::toDouble),
                values.map(TrackingChartValue::value),
                title
            )
            if (isBar) {
                plot.addSeries(series, BarFormatter(color.toArgb(), onSurface))
                plot.getRenderer(BarRenderer::class.java).apply {
                    setBarGroupWidth(
                        BarRenderer.BarGroupWidthMode.FIXED_GAP,
                        max(2f, labelTextSize / 3f)
                    )
                    barOrientation = BarRenderer.BarOrientation.SIDE_BY_SIDE
                }
            } else {
                plot.addSeries(
                    series,
                    LineAndPointFormatter(
                        color.toArgb(),
                        color.toArgb(),
                        null,
                        null
                    ).apply {
                        linePaint.strokeWidth = max(3f, labelTextSize / 4f)
                        vertexPaint.strokeWidth = max(8f, labelTextSize / 1.5f)
                    }
                )
            }
            plot.redraw()
        },
        modifier = modifier
            .fillMaxWidth()
            .height(chartHeight)
            .semantics {
                contentDescription = description
            }
    )
}

private fun setupXYPlot(
    plot: XYPlot,
    labels: List<String>,
    values: List<Double>,
    onSurface: Int,
    gridColor: Int,
    containerColor: Int,
    labelTextSize: Float,
    isBar: Boolean
) {
    plot.setPlotMargins(0f, 0f, 0f, 0f)
    plot.setPlotPadding(0f, 0f, 0f, 0f)
    plot.backgroundPaint.color = containerColor
    plot.graph.backgroundPaint.color = containerColor
    plot.graph.gridBackgroundPaint.color = containerColor
    plot.graph.domainGridLinePaint.color = gridColor
    plot.graph.rangeGridLinePaint.color = gridColor
    plot.graph.domainOriginLinePaint.color = onSurface
    plot.graph.rangeOriginLinePaint.color = onSurface
    plot.graph.setMargins(0f, 12f, 0f, 0f)
    plot.graph.setPadding(labelTextSize * 3.2f, 0f, 0f, labelTextSize * 2.8f)

    plot.graph.getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).apply {
        format = IndexedLabelFormat(labels)
        rotation = -28f
        paint.color = onSurface
        paint.textSize = labelTextSize
    }
    plot.graph.getLineLabelStyle(XYGraphWidget.Edge.LEFT).apply {
        format = DecimalFormat("0.##")
        paint.color = onSurface
        paint.textSize = labelTextSize
    }
    plot.graph.setLineLabelEdges(
        XYGraphWidget.Edge.BOTTOM,
        XYGraphWidget.Edge.LEFT
    )
    val domainPadding = if (isBar) 0.55 else 0.15
    plot.setDomainBoundaries(
        -domainPadding,
        max(domainPadding, labels.lastIndex + domainPadding),
        BoundaryMode.FIXED
    )
    plot.setDomainStep(
        StepMode.INCREMENT_BY_VAL,
        max(1.0, ceil(labels.size / 6.0))
    )
    plot.setRangeStep(StepMode.SUBDIVIDE, 6.0)

    val dataMinimum = values.min()
    val dataMaximum = values.max()
    val minimum = if (isBar) min(dataMinimum, 0.0) else dataMinimum
    val maximum = if (isBar) max(dataMaximum, 0.0) else dataMaximum
    val padding = max((maximum - minimum) * 0.08, 0.1)
    plot.setRangeBoundaries(
        minimum - padding,
        maximum + padding,
        BoundaryMode.FIXED
    )
}

@Composable
private fun TrackingChartLegend(
    values: List<TrackingChartValue>,
    colors: List<Color>
) {
    val formatter = DecimalFormat("0.##")
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        values.forEachIndexed { index, value ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    modifier = Modifier.size(12.dp),
                    color = colors[index],
                    shape = MaterialTheme.shapes.extraSmall
                ) {}
                Text(
                    text = "${value.label}: ${formatter.format(value.value)}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun TrackingChartEmpty(modifier: Modifier) {
    Text(
        text = stringResource(R.string.tracking_chart_no_data),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp)
            .testTag(TRACKING_CHART_EMPTY_TAG),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyLarge
    )
}

@Composable
private fun chartHeight() = (220f * min(LocalDensity.current.fontScale, 1.6f)).dp

private fun chartDescription(
    title: String,
    values: List<TrackingChartValue>
): String {
    val formatter = DecimalFormat("0.##")
    val details = values.take(12).joinToString { value ->
        "${value.label} ${formatter.format(value.value)}"
    }
    return "$title. $details"
}

private class IndexedLabelFormat(
    private val labels: List<String>
) : Format() {
    override fun format(
        obj: Any,
        toAppendTo: StringBuffer,
        pos: FieldPosition
    ): StringBuffer {
        val index = (obj as Number).toDouble().toInt()
        return toAppendTo.append(labels.getOrElse(index) { "" })
    }

    override fun parseObject(source: String, pos: ParsePosition): Any? = null
}
