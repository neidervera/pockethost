package com.example.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

data class NetworkInfo(
    val ipAddress: String,
    val interfaceName: String,
    val networkType: NetworkType,
    val isWifiConnected: Boolean,
    val isHotspotActive: Boolean,
    val isPublicOrOpen: Boolean = false,
    val networkLabel: String
)

enum class NetworkType {
    WIFI,
    HOTSPOT,
    ETHERNET,
    LOOPBACK,
    NONE
}

object NetworkUtils {

    /**
     * Resolves the primary local IPv4 address.
     * Prioritizes Wi-Fi (wlan0), Hotspot/Tethering (ap0/rndis), Ethernet (eth0), and falls back to 127.0.0.1.
     */
    fun getLocalIpAddress(context: Context): NetworkInfo {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            
            // Priority order: Wi-Fi, Hotspot/AP, Ethernet, cellular/other
            var wifiIp: Pair<String, String>? = null
            var apIp: Pair<String, String>? = null
            var ethIp: Pair<String, String>? = null
            var otherIp: Pair<String, String>? = null

            for (networkInterface in interfaces) {
                if (networkInterface.isLoopback || !networkInterface.isUp) continue

                val name = networkInterface.name.lowercase()
                val addresses = Collections.list(networkInterface.inetAddresses)

                for (addr in addresses) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val hostAddress = addr.hostAddress ?: continue
                        when {
                            name.startsWith("wlan") -> wifiIp = Pair(hostAddress, networkInterface.displayName)
                            name.startsWith("ap") || name.startsWith("softap") || name.startsWith("tether") -> {
                                apIp = Pair(hostAddress, networkInterface.displayName)
                            }
                            name.startsWith("eth") -> ethIp = Pair(hostAddress, networkInterface.displayName)
                            else -> if (otherIp == null) otherIp = Pair(hostAddress, networkInterface.displayName)
                        }
                    }
                }
            }

            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val activeNetwork = connectivityManager?.activeNetwork
            val capabilities = activeNetwork?.let { connectivityManager.getNetworkCapabilities(it) }
            val isWifi = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            val isEthernet = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true

            return when {
                wifiIp != null -> {
                    NetworkInfo(
                        ipAddress = wifiIp.first,
                        interfaceName = wifiIp.second,
                        networkType = NetworkType.WIFI,
                        isWifiConnected = isWifi,
                        isHotspotActive = false,
                        networkLabel = "Red Wi-Fi Local"
                    )
                }
                apIp != null -> {
                    NetworkInfo(
                        ipAddress = apIp.first,
                        interfaceName = apIp.second,
                        networkType = NetworkType.HOTSPOT,
                        isWifiConnected = false,
                        isHotspotActive = true,
                        networkLabel = "Punto de Acceso (Hotspot)"
                    )
                }
                ethIp != null -> {
                    NetworkInfo(
                        ipAddress = ethIp.first,
                        interfaceName = ethIp.second,
                        networkType = NetworkType.ETHERNET,
                        isWifiConnected = false,
                        isHotspotActive = false,
                        networkLabel = "Conexión Ethernet"
                    )
                }
                otherIp != null -> {
                    NetworkInfo(
                        ipAddress = otherIp.first,
                        interfaceName = otherIp.second,
                        networkType = NetworkType.WIFI,
                        isWifiConnected = isWifi,
                        isHotspotActive = false,
                        networkLabel = "Red Local (${otherIp.second})"
                    )
                }
                else -> {
                    NetworkInfo(
                        ipAddress = "127.0.0.1",
                        interfaceName = "lo",
                        networkType = NetworkType.LOOPBACK,
                        isWifiConnected = false,
                        isHotspotActive = false,
                        networkLabel = "Solo dispositivo (Loopback)"
                    )
                }
            }
        } catch (e: Exception) {
            return NetworkInfo(
                ipAddress = "127.0.0.1",
                interfaceName = "lo",
                networkType = NetworkType.LOOPBACK,
                isWifiConnected = false,
                isHotspotActive = false,
                networkLabel = "127.0.0.1"
            )
        }
    }

    /**
     * Flow that emits whenever network state changes (connection drop, Wi-Fi reconnection, IP change).
     */
    fun observeNetworkState(context: Context): Flow<NetworkInfo> = callbackFlow {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        
        trySend(getLocalIpAddress(context))

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(getLocalIpAddress(context))
            }

            override fun onLost(network: Network) {
                trySend(getLocalIpAddress(context))
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                trySend(getLocalIpAddress(context))
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        try {
            connectivityManager?.registerNetworkCallback(request, callback)
        } catch (e: Exception) {
            // Fallback for restricted environments
        }

        awaitClose {
            try {
                connectivityManager?.unregisterNetworkCallback(callback)
            } catch (e: Exception) {
                // Ignore unregister errors
            }
        }
    }
}
