package com.mhss.app.tracking.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.mhss.app.tracking.data.database.entity.RecordTemplateEntity
import com.mhss.app.tracking.data.database.entity.TemplateFieldEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackingTemplateDao {

    @Query(
        """
        SELECT * FROM tracking_templates
        WHERE is_active = 1
        ORDER BY display_order, created_at_epoch_milli, id
        """
    )
    fun observeActiveTemplates(): Flow<List<RecordTemplateEntity>>

    @Query("SELECT * FROM tracking_templates WHERE id = :id")
    suspend fun getTemplate(id: String): RecordTemplateEntity?

    @Query(
        """
        SELECT * FROM tracking_template_fields
        WHERE template_id = :templateId
        ORDER BY display_order, id
        """
    )
    suspend fun getFields(templateId: String): List<TemplateFieldEntity>

    @Insert
    suspend fun insertTemplate(template: RecordTemplateEntity)

    @Insert
    suspend fun insertFields(fields: List<TemplateFieldEntity>)

    @Update
    suspend fun updateTemplate(template: RecordTemplateEntity)

    @Query(
        """
        UPDATE tracking_templates
        SET is_active = 0, updated_at_epoch_milli = :updatedAtEpochMilli
        WHERE id = :id
        """
    )
    suspend fun deactivateTemplate(id: String, updatedAtEpochMilli: Long): Int
}
