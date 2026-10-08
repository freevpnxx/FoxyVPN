package dev.vulpes.tunnel

import dev.vulpes.tunnel.data.LocaleManager
import io.netty.util.ResourceLeakDetector
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import dev.vulpes.tunnel.data.CrashReporter
import dev.vulpes.tunnel.data.FxaAuthRepository
import dev.vulpes.tunnel.data.GuardianClient
import dev.vulpes.tunnel.data.ProxyStateStore
import dev.vulpes.tunnel.data.ServerListClient
import dev.vulpes.tunnel.data.SettingsStore
import dev.vulpes.tunnel.data.TokenStore
import dev.vulpes.tunnel.vpn.upstream.NettyLoggingBridge
import org.conscrypt.Conscrypt
import java.security.Security

class FoxyVpnApp : Application() {

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(
            LocaleManager.wrap(newBase, SettingsStore(newBase).appLanguage),
        )
    }

    lateinit var tokenStore: TokenStore
    lateinit var proxyStateStore: ProxyStateStore
    lateinit var settingsStore: SettingsStore
    lateinit var guardianClient: GuardianClient
    lateinit var serverListClient: ServerListClient
    lateinit var authRepository: FxaAuthRepository

    override fun onCreate() {
        super.onCreate()

        CrashReporter.install(this)
        CrashReporter.replayLastCrashIfAny(this)

        NettyLoggingBridge.install()
        if (BuildConfig.DEBUG) {
            // Paranoid level samples every ByteBuf and reports the full allocation trace on a
            // leak. Far too slow to ship, but exactly what is wanted in a debug build.
            ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID)
        }
        installConscrypt()
        tokenStore = TokenStore(this)
        proxyStateStore = ProxyStateStore(this)
        settingsStore = SettingsStore(this)
        guardianClient = GuardianClient()
        serverListClient = ServerListClient()
        authRepository = FxaAuthRepository(tokenStore)
        createNotificationChannel()
    }

    private fun installConscrypt() {
        runCatching {
            Security.insertProviderAt(Conscrypt.newProvider(), 1)
        }.onFailure {
            Log.w("FoxyVpnApp", "failed to install Conscrypt security provider", it)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            VPN_NOTIFICATION_CHANNEL_ID,
            "VPN status",
            NotificationManager.IMPORTANCE_LOW,
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        const val VPN_NOTIFICATION_CHANNEL_ID = "foxyvpn_status"
    }
}
