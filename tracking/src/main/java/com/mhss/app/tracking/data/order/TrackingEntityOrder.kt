package com.mhss.app.tracking.data.order

import com.mhss.app.tracking.data.database.entity.RecordTemplateEntity
import com.mhss.app.tracking.data.database.entity.TemplateFieldEntity
import com.mhss.app.tracking.data.database.entity.TrackerOptionEntity

object TrackingEntityOrder {

    fun templates(items: Iterable<RecordTemplateEntity>): List<RecordTemplateEntity> {
        return items.sortedWith(
            compareBy<RecordTemplateEntity> { it.displayOrder }
                .thenBy { it.createdAtEpochMilli }
                .thenBy { it.id }
        )
    }

    fun fields(items: Iterable<TemplateFieldEntity>): List<TemplateFieldEntity> {
        return items.sortedWith(
            compareBy<TemplateFieldEntity> { it.displayOrder }
                .thenBy { it.id }
        )
    }

    fun options(items: Iterable<TrackerOptionEntity>): List<TrackerOptionEntity> {
        return items.sortedWith(
            compareBy<TrackerOptionEntity> { it.displayOrder }
                .thenBy { it.id }
        )
    }
}
