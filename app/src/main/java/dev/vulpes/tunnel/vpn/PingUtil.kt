package dev.vulpes.tunnel.vpn

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.InetSocketAddress
import java.net.Socket

object PingUtil {
    private const val TIMEOUT_MS = 1_500

    suspend fun measureTcpLatencyMs(host: String, port: Int): Int? = withContext(Dispatchers.IO) {
        withTimeoutOrNull(TIMEOUT_MS.toLong()) {
            runCatching {
                val start = System.nanoTime()
                Socket().use { socket ->
                    socket.bind(InetSocketAddress(0))
                    dev.vulpes.tunnel.data.ControlPlaneHttp.socketProtector?.invoke(socket)
                    socket.connect(InetSocketAddress(host, port), TIMEOUT_MS)
                }
                ((System.nanoTime() - start) / 1_000_000L).toInt()
            }.onFailure {
                dev.vulpes.tunnel.data.AppLogger.d("PingUtil", "ping failed to $host:$port: ${it.message}")
            }.getOrNull()
        }
    }

    /**
     * @param latencyMs median of the successful samples, so a single outlier cannot dominate.
     * @param jitterMs mean deviation between consecutive samples; lower means steadier.
     */
    data class Sample(val latencyMs: Int, val jitterMs: Int, val samples: Int)

    /**
     * Probes the server several times and reports both latency and jitter. Returns null only when
     * every round failed, so a flaky probe does not hide an otherwise reachable server.
     */
    suspend fun sample(host: String, port: Int, rounds: Int = 3): Sample? {
        val readings = (1..rounds).mapNotNull { measureTcpLatencyMs(host, port) }
        if (readings.isEmpty()) return null

        val sorted = readings.sorted()
        val median = sorted[sorted.size / 2]

        val jitter = if (readings.size > 1) {
            readings.zipWithNext { a, b -> kotlin.math.abs(a - b) }.average().toInt()
        } else {
            0
        }
        return Sample(latencyMs = median, jitterMs = jitter, samples = readings.size)
    }
}
