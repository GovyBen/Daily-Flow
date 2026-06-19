package com.mhss.app.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mhss.app.domain.model.CalendarDay

@Composable
fun CalendarDayCell(
    day: CalendarDay,
    hasTrackingRecords: Boolean,
    isSelected: Boolean,
    isToday: Boolean,
    onDaySelected: (CalendarDay) -> Unit,
    trackingValues: List<CalendarTrackingValue> = emptyList()
) {
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        else -> Color.Transparent
    }
    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
        !day.isCurrentMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        else -> MaterialTheme.colorScheme.onSurface
    }
    val eventColors = remember(day.events) {
        day.events.map { it.color }.distinct()
    }

    CalendarDayCellContent(
        text = day.date.day.toString(),
        textColor = if (isToday && !isSelected) MaterialTheme.colorScheme.primary else textColor,
        backgroundColor = backgroundColor,
        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
        eventColors = eventColors,
        hasTrackingRecords = hasTrackingRecords,
        trackingValues = trackingValues,
        onClick = { onDaySelected(day) }
    )
}

@Composable
fun EmptyCalendarDayCell() {
    CalendarDayCellContent(
        text = "",
        textColor = Color.Transparent,
        backgroundColor = Color.Transparent,
        fontWeight = FontWeight.Normal,
        eventColors = emptyList(),
        hasTrackingRecords = false,
        trackingValues = emptyList()
    )
}

@Composable
fun CalendarDayCellContent(
    text: String,
    textColor: Color,
    backgroundColor: Color,
    fontWeight: FontWeight,
    eventColors: List<Int>,
    hasTrackingRecords: Boolean,
    trackingValues: List<CalendarTrackingValue> = emptyList(),
    onClick: (() -> Unit) = {}
) {
    Column(
        modifier = Modifier
            .padding(2.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(vertical = 3.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            fontWeight = fontWeight
        )
        EventDots(colors = eventColors)
        if (trackingValues.isNotEmpty()) {
            Spacer(Modifier.height(2.dp))
            trackingValues.take(2).forEach { tv ->
                TrackingValueIndicator(tv)
            }
            if (trackingValues.size > 2) {
                Text(
                    text = "+${trackingValues.size - 2}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 7.sp
                )
            }
        } else if (hasTrackingRecords) {
            // Fall back to simple bar indicator when no detailed values available
            Spacer(Modifier.height(5.dp))
            TrackingIndicator(visible = true)
        }
    }
}

@Composable
private fun TrackingValueIndicator(value: CalendarTrackingValue) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            modifier = Modifier.size(5.dp),
            shape = CircleShape,
            color = Color(value.templateColor)
        ) {}
        Spacer(Modifier.width(2.dp))
        Text(
            text = value.templateName,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 7.sp
        )
    }
}

@Composable
private fun TrackingIndicator(visible: Boolean) {
    Box(
        modifier = Modifier
            .width(12.dp)
            .height(3.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(
                if (visible) MaterialTheme.colorScheme.secondary else Color.Transparent
            )
    )
}

@Composable
fun EventDots(colors: List<Int>) {
    if (colors.isEmpty()) {
        Spacer(Modifier.height(4.dp))
    } else {
        val indicators = if (colors.size <= 3) colors else colors.take(3)
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            indicators.forEach { colorValue ->
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(Color(colorValue))
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CalendarDayCellContentPreview() {
    MaterialTheme {
        Box(
            Modifier.width(40.dp)
        ) {
            CalendarDayCellContent(
                text = "15",
                textColor = MaterialTheme.colorScheme.onPrimaryContainer,
                backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                fontWeight = FontWeight.Bold,
                eventColors = listOf(0xFFEA4335.toInt(), 0xFF34A853.toInt(), 0xFF4285F4.toInt()),
                hasTrackingRecords = true,
                onClick = {}
            )
        }
    }
}
