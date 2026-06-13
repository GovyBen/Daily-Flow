package com.mhss.app.tracking.domain.defaults

import com.mhss.app.tracking.domain.model.CounterConfig
import com.mhss.app.tracking.domain.model.DurationConfig
import com.mhss.app.tracking.domain.model.MultiSelectConfig
import com.mhss.app.tracking.domain.model.ScaleConfig
import com.mhss.app.tracking.domain.model.TextConfig
import com.mhss.app.tracking.domain.model.TrackingFieldDraft
import com.mhss.app.tracking.domain.model.TrackingOptionDraft
import com.mhss.app.tracking.domain.model.TrackingTemplateDraft
import com.mhss.app.tracking.domain.model.TrackingTrackerDraft

object DefaultTrackingTemplates {
    const val FITNESS_TEMPLATE_ID = "daily-flow-default-fitness"
    const val MOOD_TEMPLATE_ID = "daily-flow-default-mood"
    const val HABIT_COUNT_TEMPLATE_ID = "daily-flow-default-habit-count"

    val templates: List<TrackingTemplateDraft> = listOf(
        fitnessTemplate(),
        moodTemplate(),
        habitCountTemplate()
    )

    private fun fitnessTemplate() = TrackingTemplateDraft(
        id = FITNESS_TEMPLATE_ID,
        name = "健身",
        description = "记录训练部位、时长和备注",
        icon = "fitness_center",
        color = 0xFF6750A4,
        displayOrder = 0,
        fields = listOf(
            TrackingFieldDraft(
                id = "$FITNESS_TEMPLATE_ID-field-parts",
                trackerId = "$FITNESS_TEMPLATE_ID-tracker-parts",
                tracker = TrackingTrackerDraft(
                    id = "$FITNESS_TEMPLATE_ID-tracker-parts",
                    name = "训练部位",
                    config = MultiSelectConfig(),
                    options = listOf(
                        "胸",
                        "背",
                        "肩",
                        "手臂",
                        "臀",
                        "腿",
                        "核心"
                    ).mapIndexed { index, label ->
                        TrackingOptionDraft(
                            id = "$FITNESS_TEMPLATE_ID-option-$index",
                            label = label,
                            displayOrder = index
                        )
                    }
                ),
                displayOrder = 0,
                required = true
            ),
            TrackingFieldDraft(
                id = "$FITNESS_TEMPLATE_ID-field-duration",
                trackerId = "$FITNESS_TEMPLATE_ID-tracker-duration",
                tracker = TrackingTrackerDraft(
                    id = "$FITNESS_TEMPLATE_ID-tracker-duration",
                    name = "训练时长",
                    config = DurationConfig(),
                    unit = "分钟"
                ),
                displayOrder = 1
            ),
            TrackingFieldDraft(
                id = "$FITNESS_TEMPLATE_ID-field-note",
                trackerId = "$FITNESS_TEMPLATE_ID-tracker-note",
                tracker = TrackingTrackerDraft(
                    id = "$FITNESS_TEMPLATE_ID-tracker-note",
                    name = "备注",
                    config = TextConfig(maximumLength = 2_000)
                ),
                displayOrder = 2
            )
        )
    )

    private fun moodTemplate() = TrackingTemplateDraft(
        id = MOOD_TEMPLATE_ID,
        name = "心情",
        description = "用 1 至 10 分记录当下感受",
        icon = "mood",
        color = 0xFF386A20,
        displayOrder = 1,
        fields = listOf(
            TrackingFieldDraft(
                id = "$MOOD_TEMPLATE_ID-field-score",
                trackerId = "$MOOD_TEMPLATE_ID-tracker-score",
                tracker = TrackingTrackerDraft(
                    id = "$MOOD_TEMPLATE_ID-tracker-score",
                    name = "心情评分",
                    config = ScaleConfig(minimum = 1.0, maximum = 10.0, step = 1.0),
                    unit = "分"
                ),
                displayOrder = 0,
                required = true
            ),
            TrackingFieldDraft(
                id = "$MOOD_TEMPLATE_ID-field-note",
                trackerId = "$MOOD_TEMPLATE_ID-tracker-note",
                tracker = TrackingTrackerDraft(
                    id = "$MOOD_TEMPLATE_ID-tracker-note",
                    name = "备注",
                    config = TextConfig(maximumLength = 2_000)
                ),
                displayOrder = 1
            )
        )
    )

    private fun habitCountTemplate() = TrackingTemplateDraft(
        id = HABIT_COUNT_TEMPLATE_ID,
        name = "习惯次数",
        description = "中性记录日常行为次数，不提供健康判断",
        icon = "repeat",
        color = 0xFF006A6A,
        displayOrder = 2,
        fields = listOf(
            TrackingFieldDraft(
                id = "$HABIT_COUNT_TEMPLATE_ID-field-social",
                trackerId = "$HABIT_COUNT_TEMPLATE_ID-tracker-social",
                tracker = TrackingTrackerDraft(
                    id = "$HABIT_COUNT_TEMPLATE_ID-tracker-social",
                    name = "社交次数",
                    config = CounterConfig(minimum = 0)
                ),
                displayOrder = 0
            ),
            TrackingFieldDraft(
                id = "$HABIT_COUNT_TEMPLATE_ID-field-smoking",
                trackerId = "$HABIT_COUNT_TEMPLATE_ID-tracker-smoking",
                tracker = TrackingTrackerDraft(
                    id = "$HABIT_COUNT_TEMPLATE_ID-tracker-smoking",
                    name = "吸烟次数",
                    config = CounterConfig(minimum = 0)
                ),
                displayOrder = 1
            ),
            TrackingFieldDraft(
                id = "$HABIT_COUNT_TEMPLATE_ID-field-note",
                trackerId = "$HABIT_COUNT_TEMPLATE_ID-tracker-note",
                tracker = TrackingTrackerDraft(
                    id = "$HABIT_COUNT_TEMPLATE_ID-tracker-note",
                    name = "备注",
                    config = TextConfig(maximumLength = 2_000)
                ),
                displayOrder = 2
            )
        )
    )
}
