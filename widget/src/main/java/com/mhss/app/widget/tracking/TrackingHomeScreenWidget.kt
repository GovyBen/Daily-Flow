package com.mhss.app.widget.tracking

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.mhss.app.tracking.domain.model.TrackingTemplateSummary
import com.mhss.app.ui.R
import com.mhss.app.widget.largeBackgroundBasedOnVersion
import com.mhss.app.widget.largeInnerBackgroundBasedOnVersion

@Composable
fun TrackingHomeScreenWidget(templates: List<TrackingTemplateSummary>) {
    val context = LocalContext.current
    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .largeBackgroundBasedOnVersion()
    ) {
        Column(modifier = GlanceModifier.padding(8.dp)) {
            Text(
                text = context.getString(R.string.tracking_templates),
                style = TextStyle(
                    color = GlanceTheme.colors.onSecondaryContainer,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                ),
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .clickable(actionRunCallback<NavigateToTrackingTemplatesAction>())
            )
            Spacer(GlanceModifier.height(8.dp))
            if (templates.isEmpty()) {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .largeInnerBackgroundBasedOnVersion()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = context.getString(R.string.tracking_widget_empty),
                        style = TextStyle(
                            color = GlanceTheme.colors.secondary,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    )
                    Spacer(GlanceModifier.height(8.dp))
                    Text(
                        text = context.getString(R.string.tracking_widget_configure),
                        style = TextStyle(
                            color = GlanceTheme.colors.onSecondaryContainer,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        ),
                        modifier = GlanceModifier.clickable(
                            actionRunCallback<NavigateToTrackingTemplatesAction>()
                        )
                    )
                }
            } else {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .largeInnerBackgroundBasedOnVersion()
                        .padding(6.dp)
                ) {
                    templates.forEachIndexed { index, template ->
                        TrackingWidgetTemplateItem(template)
                        if (index != templates.lastIndex) {
                            Spacer(GlanceModifier.height(4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackingWidgetTemplateItem(template: TrackingTemplateSummary) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(GlanceTheme.colors.secondaryContainer)
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .clickable(
                actionRunCallback<TrackingTemplateClickAction>(
                    actionParametersOf(trackingTemplateId to template.id)
                )
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = GlanceModifier
                .size(12.dp)
                .background(ColorProvider(Color(template.color)))
        ) {}
        Spacer(GlanceModifier.width(8.dp))
        Text(
            text = template.name,
            maxLines = 1,
            style = TextStyle(
                color = GlanceTheme.colors.onSecondaryContainer,
                fontSize = 14.sp
            )
        )
    }
}
