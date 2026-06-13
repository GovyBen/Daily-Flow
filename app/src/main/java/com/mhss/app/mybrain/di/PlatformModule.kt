package com.mhss.app.mybrain.di

import android.util.Log
import com.mhss.app.mybrain.dataStore
import com.mhss.app.mybrain.data.security.AndroidKeystoreSecretStore
import com.mhss.app.mybrain.data.security.LegacySecretMigration
import com.mhss.app.mybrain.data.security.redactSensitiveHeaders
import com.mhss.app.preferences.domain.repository.SecretStore
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.logging.Logger
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

val platformModule = module {
    single { androidContext().dataStore }
    single<SecretStore> {
        AndroidKeystoreSecretStore(
            dataStore = get(),
            ioDispatcher = get(named("ioDispatcher"))
        )
    }
    single { LegacySecretMigration(dataStore = get(), secretStore = get()) }
    single { OkHttp.create() }
    single<Logger> { AndroidHttpLogger() }

}

class AndroidHttpLogger: Logger {
    override fun log(message: String) {
        Log.i("Ktor", message.redactSensitiveHeaders())
    }
}
