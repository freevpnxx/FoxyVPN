package dev.vulpes.tunnel

import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import dev.vulpes.tunnel.data.AppLogger
import dev.vulpes.tunnel.data.model.ConnectionState
import dev.vulpes.tunnel.vpn.FoxyVpnService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Quick Settings tile for connecting and disconnecting without opening the app.
 *
 * Connecting has a wrinkle: the first time, Android requires the VPN consent dialog, and a tile
 * cannot show it. So when consent has not been granted yet the tile hands off to the activity
 * instead of starting the service behind the user's back.
 */
class FoxyTileService : TileService() {

    private var scope: CoroutineScope? = null

    override fun onStartListening() {
        super.onStartListening()
        render(FoxyVpnService.state.value)

        // Keep the tile honest while the shade is open; the tunnel can drop on its own.
        scope?.cancel()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        scope?.launch {
            FoxyVpnService.state.collect { render(it) }
        }
    }

    override fun onStopListening() {
        scope?.cancel()
        scope = null
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        when (FoxyVpnService.state.value) {
            ConnectionState.DISCONNECTED -> connect()
            else -> {
                AppLogger.i(TAG, "tile: disconnecting")
                FoxyVpnService.stop(this)
            }
        }
    }

    private fun connect() {
        // Null means the user has already granted VPN permission to this app, so the service can
        // be started directly. Anything else is the consent dialog, which needs a real activity.
        if (VpnService.prepare(this) == null) {
            AppLogger.i(TAG, "tile: consent already granted, starting the tunnel")
            FoxyVpnService.start(this)
        } else {
            AppLogger.i(TAG, "tile: handing off to the app for the VPN consent dialog")
            launchApp()
        }
    }

    private fun launchApp() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(
                PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE),
            )
        } else {
            @Suppress("DEPRECATION", "StartActivityAndCollapseDeprecated")
            startActivityAndCollapse(intent)
        }
    }

    private fun render(state: ConnectionState) {
        val tile = qsTile ?: return
        tile.state = when (state) {
            ConnectionState.CONNECTED -> Tile.STATE_ACTIVE
            ConnectionState.CONNECTING -> Tile.STATE_UNAVAILABLE
            ConnectionState.DISCONNECTED -> Tile.STATE_INACTIVE
        }
        tile.label = getString(
            when (state) {
                ConnectionState.CONNECTED -> R.string.tile_connected
                ConnectionState.CONNECTING -> R.string.tile_connecting
                ConnectionState.DISCONNECTED -> R.string.tile_disconnected
            },
        )
        tile.contentDescription = tile.label
        tile.updateTile()
    }

    private companion object {
        const val TAG = "FoxyTileService"
    }
}
