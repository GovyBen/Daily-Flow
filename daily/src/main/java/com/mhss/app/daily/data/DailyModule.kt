package com.mhss.app.daily.data

import com.mhss.app.daily.domain.repository.DailyItemRepository
import com.mhss.app.daily.domain.repository.DashboardPanelRepository
import com.mhss.app.daily.domain.usecase.ArchiveDailyItemUseCase
import com.mhss.app.daily.domain.usecase.CompleteDailyItemUseCase
import com.mhss.app.daily.domain.usecase.CreateDailyItemUseCase
import com.mhss.app.daily.domain.usecase.DeleteDailyItemUseCase
import com.mhss.app.daily.domain.usecase.DisableDailyItemCalendarSyncUseCase
import com.mhss.app.daily.domain.usecase.EnsureDefaultDashboardPanelsUseCase
import com.mhss.app.daily.domain.usecase.GetDailyItemUseCase
import com.mhss.app.daily.domain.usecase.MigrateLegacyTasksToDailyItemsUseCase
import com.mhss.app.daily.domain.usecase.MoveDashboardPanelUseCase
import com.mhss.app.daily.domain.usecase.ObserveDailyItemsUseCase
import com.mhss.app.daily.domain.usecase.ObserveDashboardPanelsUseCase
import com.mhss.app.daily.domain.usecase.ReconcileDailyItemCalendarSyncUseCase
import com.mhss.app.daily.domain.usecase.ReopenDailyItemUseCase
import com.mhss.app.daily.domain.usecase.ResetDashboardPanelsUseCase
import com.mhss.app.daily.domain.usecase.SaveDashboardPanelConfigUseCase
import com.mhss.app.daily.domain.usecase.SearchDailyItemsUseCase
import com.mhss.app.daily.domain.usecase.SyncDailyItemToCalendarUseCase
import com.mhss.app.daily.domain.usecase.UpdateDailyItemCalendarSyncUseCase
import com.mhss.app.daily.domain.usecase.UpdateDailyItemUseCase
import com.mhss.app.daily.domain.usecase.UpdateDashboardPanelUseCase
import com.mhss.app.daily.domain.validation.DailyItemValidator
import com.mhss.app.daily.presentation.DailyItemDetailsViewModel
import com.mhss.app.daily.presentation.DailyItemEditorViewModel
import com.mhss.app.daily.presentation.DailyItemsViewModel
import com.mhss.app.daily.presentation.DashboardEditViewModel
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val dailyModule = module {
    single { DailyItemValidator() }
    single<DailyItemRepository> {
        RoomDailyItemRepository(
            database = get(),
            dailyItemDao = get(),
            ioDispatcher = get<CoroutineDispatcher>(named("ioDispatcher")),
            defaultDispatcher = get<CoroutineDispatcher>(named("defaultDispatcher"))
        )
    }
    single<DashboardPanelRepository> {
        RoomDashboardPanelRepository(
            database = get(),
            dashboardPanelDao = get(),
            ioDispatcher = get<CoroutineDispatcher>(named("ioDispatcher"))
        )
    }

    single { SyncDailyItemToCalendarUseCase(get(), get()) }
    single { DisableDailyItemCalendarSyncUseCase(get()) }
    single { ReconcileDailyItemCalendarSyncUseCase(get(), get()) }
    single { ObserveDailyItemsUseCase(get()) }
    single { GetDailyItemUseCase(get()) }
    single { SearchDailyItemsUseCase(get()) }
    single { CreateDailyItemUseCase(get(), get(), get(), get()) }
    single { UpdateDailyItemUseCase(get(), get(), get(), get(), get()) }
    single { CompleteDailyItemUseCase(get(), get(), get(), get(), get()) }
    single { ReopenDailyItemUseCase(get(), get()) }
    single { ArchiveDailyItemUseCase(get(), get(), get(), get()) }
    single { DeleteDailyItemUseCase(get(), get(), get(), get()) }
    single { UpdateDailyItemCalendarSyncUseCase(get()) }
    single { MigrateLegacyTasksToDailyItemsUseCase(get(), get(), get()) }

    single { ObserveDashboardPanelsUseCase(get()) }
    single { EnsureDefaultDashboardPanelsUseCase(get()) }
    single { ResetDashboardPanelsUseCase(get()) }
    single { UpdateDashboardPanelUseCase(get()) }
    single { MoveDashboardPanelUseCase(get()) }
    single { SaveDashboardPanelConfigUseCase(get()) }

    viewModel { DailyItemsViewModel(get(), get(), get()) }
    viewModel { parameters ->
        DailyItemDetailsViewModel(
            itemId = parameters.get(),
            getDailyItem = get(),
            completeDailyItem = get(),
            reopenDailyItem = get(),
            archiveDailyItem = get(),
            deleteDailyItem = get(),
            syncDailyItemToCalendar = get(),
            disableDailyItemCalendarSync = get()
        )
    }
    viewModel { parameters ->
        DailyItemEditorViewModel(
            itemId = parameters.get(),
            getDailyItem = get(),
            createDailyItem = get(),
            updateDailyItem = get(),
            syncDailyItemToCalendar = get()
        )
    }
    viewModel { DashboardEditViewModel(get(), get(), get(), get(), get()) }
}
