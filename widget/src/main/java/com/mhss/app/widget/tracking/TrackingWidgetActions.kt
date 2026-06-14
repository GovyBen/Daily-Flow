package com.mhss.app.widget.tracking

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.mhss.app.util.Constants

class NavigateToTrackingTemplatesAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Constants.TRACKING_TEMPLATES_URI.toUri())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

class TrackingTemplateClickAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        parameters[trackingTemplateId]?.let { templateId ->
            val uri = "${Constants.TRACKING_QUICK_RECORD_URI}/${Uri.encode(templateId)}".toUri()
            context.startActivity(
                Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}

val trackingTemplateId = ActionParameters.Key<String>("trackingTemplateId")
