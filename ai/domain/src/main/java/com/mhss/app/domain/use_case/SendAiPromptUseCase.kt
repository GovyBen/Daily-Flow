package com.mhss.app.domain.use_case

import com.mhss.app.domain.model.AssistantResult
import com.mhss.app.domain.repository.AiRepository
import org.koin.core.annotation.Factory
import java.io.IOException

@Factory
class SendAiPromptUseCase(private val aiRepository: AiRepository) {
    suspend operator fun invoke(prompt: String): AssistantResult<String> {
        return try {
            aiRepository.sendPrompt(prompt)
        } catch (e: IOException) {
            AssistantResult.InternetError
        } catch (e: Exception) {
            AssistantResult.OtherError()
        }
    }
}
