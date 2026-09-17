package world.hachimi.app.di

import org.koin.core.context.startKoin
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.koin.mp.KoinPlatformTools
import world.hachimi.app.BuildKonfig
import world.hachimi.app.api.ApiClient
import world.hachimi.app.logging.Logger
import world.hachimi.app.model.GlobalStore
import world.hachimi.app.player.OhosMediaSession
import world.hachimi.app.player.OhosPlayer
import world.hachimi.app.player.PlayerEngine
import world.hachimi.app.storage.MyDataStore
import world.hachimi.app.storage.MyDataStoreImpl
import world.hachimi.app.storage.SongCache
import world.hachimi.app.storage.SongCacheImpl

fun initKoin() {
    if (KoinPlatformTools.defaultContext().getOrNull() != null) return
    val koin = startKoin {
        modules(appModule)
    }
    koin.koin.get<GlobalStore>().initialize()
    // The media session is a nice-to-have: never let it keep the app from starting.
    runCatching { koin.koin.get<OhosMediaSession>().initialize() }
        .onFailure { Logger.e("OhosMediaSession", "Cannot initialize the media session", it) }
}

val appModule = module {
    single { ApiClient(BuildKonfig.API_BASE_URL) }
    single<MyDataStore> { MyDataStoreImpl() }
    single<PlayerEngine> { OhosPlayer() }
    // PlayerService is owned by GlobalStore, not registered on its own.
    single { OhosMediaSession(get<GlobalStore>().player) }
    single<SongCache> { SongCacheImpl() }
    singleOf(::GlobalStore)
    applyViewModels()
}
