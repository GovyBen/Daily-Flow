package com.mhss.app.presentation

import android.text.format.DateFormat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mhss.app.domain.model.CalendarDay
import com.mhss.app.domain.model.CalendarEvent
import com.mhss.app.tracking.domain.model.TrackingCalendarRecord
import com.mhss.app.ui.R
import com.mhss.app.util.date.formattedEventsDayName
import java.util.Date

@Composable
fun DayEventsList(
    modifier: Modifier = Modifier,
    state: LazyListState,
    selectedDate: CalendarDay,
    trackingRecords: List<TrackingCalendarRecord>,
    hasCalendarPermission: Boolean,
    onEventClick: (CalendarEvent) -> Unit
) {
    val target = DayDetails(selectedDate, trackingRecords, hasCalendarPermission)
    AnimatedContent(
        targetState = target,
        contentKey = { it.day.date.toEpochDays() },
        transitionSpec = {
            if (targetState.day.date < initialState.day.date) {
                slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
            } else {
                slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
            }
        },
    ) { details ->
        LazyColumn(
            modifier = modifier,
            state = state,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = details.day.date.formattedEventsDayName,
                    style = MaterialTheme.typography.headlineSmall
                )
            }
            item {
                Text(
                    text = stringResource(R.string.calendar_events_section),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (!details.hasCalendarPermission) {
                item {
                    Text(
                        text = stringResource(R.string.calendar_events_permission_required),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (details.day.events.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.no_events),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(
                    items = details.day.events,
                    key = { "event-${it.id}-${it.start}" }
                ) { event ->
                    CalendarEventItem(
                        event = event,
                        onClick = onEventClick
                    )
                }
            }
            item {
                Text(
                    text = stringResource(R.string.calendar_records_section),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (details.records.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.calendar_no_records),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(
                    items = details.records,
                    key = { "record-${it.id}" }
                ) { record ->
                    TrackingCalendarRecordItem(record)
                }
            }
        }
    }
}

@Composable
private fun TrackingCalendarRecordItem(record: TrackingCalendarRecord) {
    val context = LocalContext.current
    val time = DateFormat.getTimeFormat(context).format(Date(record.occurredAtEpochMilli))
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.elevatedCardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .height(34.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(record.templateColor))
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = record.templateName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = time,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                record.note?.takeIf(String::isNotBlank)?.let { note ->
                    Text(
                        text = note,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private data class DayDetails(
    val day: CalendarDay,
    val records: List<TrackingCalendarRecord>,
    val hasCalendarPermission: Boolean
)
