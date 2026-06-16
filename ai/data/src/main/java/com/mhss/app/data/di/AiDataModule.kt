package com.mhss.app.data.di

import com.mhss.app.data.EmptyAiClient
import com.mhss.app.data.repository.AiRepositoryImpl
import com.mhss.app.data.tools.*
import com.mhss.app.domain.repository.AiRepository
import com.mhss.app.domain.use_case.ProposalExecutor
import com.mhss.app.domain.use_case.SendAiMessageUseCase
import com.mhss.app.domain.use_case.SendAiPromptUseCase
import org.koin.dsl.module

val aiDataModule = module {
    // Domain use cases
    factory { SendAiMessageUseCase(get()) }
    factory { SendAiPromptUseCase(get()) }
    factory { ProposalExecutor(get(), get(), get(), get()) }

    // Toolsets
    factory { TaskToolSet(get(), get(), get(), get(), get()) }
    factory { CalendarToolSet(get(), get(), get(), get(), get()) }
    factory { DiaryToolSet(get(), get(), get()) }
    factory { NoteToolSet(get(), get(), get(), get(), get(), get(), get()) }
    factory { BookmarkToolSet(get(), get()) }
    factory { TrackingToolSet(get()) }
    factory { UtilToolSet() }

    // Repository & client
    single<AiRepository> {
        AiRepositoryImpl(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get())
    }
    single { EmptyAiClient }
}
