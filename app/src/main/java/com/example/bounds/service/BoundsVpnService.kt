package com.example.bounds.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.net.ConnectivityManager
import android.net.Network
import android.net.VpnService
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.bounds.BoundsApplication
import com.example.bounds.R
import com.example.bounds.model.WebsiteEnforcementState
import com.example.bounds.model.WebsiteEnforcementStatus
import com.example.bounds.util.DomainBlocklist
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.atomic.AtomicLong

/**
 * Local DNS-only VPN. It routes only the synthetic DNS address through the
 * TUN, so ordinary allowed traffic continues through Android's normal network.
 * The service never stores or logs DNS names.
 */
class BoundsVpnService : VpnService() {

    companion object {
        const val ACTION_START = "com.example.bounds.action.START_VPN"
        const val ACTION_STOP = "com.example.bounds.action.STOP_VPN"
        const val EXTRA_ZONE_ID = "vpn_zone_id"
        const val EXTRA_BLOCKED_DOMAINS = "vpn_blocked_domains"

        private const val CHANNEL_ID = "bounds_website_vpn"
        private const val NOTIFICATION_ID = 7
        private const val TUN_DNS_ADDRESS = "10.0.0.1"
        private const val TUN_CLIENT_ADDRESS = "10.0.0.2"
        private const val UPSTREAM_DNS = "1.1.1.1"
        private const val DNS_PORT = 53
        private const val BLOCK_FEEDBACK_DURATION_MS = 3_000L
        private const val BLOCK_FEEDBACK_COOLDOWN_MS = 15_000L
        private val BLOCK_FEEDBACK_CALLBACK_TOKEN = Any()
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val feedbackGeneration = AtomicLong(0)
    private lateinit var windowManager: WindowManager
    private var tunInterface: ParcelFileDescriptor? = null
    private var worker: Job? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var activeZoneId: String? = null
    private var activeDomains: List<String> = emptyList()
    private var stoppingIntentionally = false
    private var blockFeedbackView: View? = null
    private var hideBlockFeedbackRunnable: Runnable? = null
    private var restoreNotificationRunnable: Runnable? = null
    private val lastBlockFeedbackAt = mutableMapOf<String, Long>()

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            markError("Website VPN restarted without a zone policy. Re-enter the zone or retry from Settings.")
            return START_NOT_STICKY
        }
        when (intent?.action) {
            ACTION_STOP -> {
                stoppingIntentionally = true
                closeTunnel()
                updateReadyState()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_START -> {
                startForeground(NOTIFICATION_ID, notification())
                stoppingIntentionally = false
                val zoneId = intent.getStringExtra(EXTRA_ZONE_ID) ?: run {
                    markError("Website protection could not start because its zone policy is missing.")
                    stopSelf()
                    return START_NOT_STICKY
                }
                val domains = intent.getStringArrayListExtra(EXTRA_BLOCKED_DOMAINS)
                    ?.let(DomainBlocklist::canonicalizeAll)
                    .orEmpty()
                if (domains.isEmpty()) {
                    markError("Website protection could not start because this zone has no valid domains.")
                    stopSelf()
                    return START_NOT_STICKY
                }
                establishTunnel(zoneId, domains)
            }
        }
        return START_STICKY
    }

    override fun onRevoke() {
        stoppingIntentionally = true
        markDisplaced("VPN access was revoked. Re-approve Bounds to resume website blocking.")
        closeTunnel()
        stopSelf()
        super.onRevoke()
    }

    override fun onDestroy() {
        closeTunnel()
        hideBlockFeedback()
        serviceScope.cancel()
        if (!stoppingIntentionally &&
            (applicationContext as BoundsApplication).websiteEnforcement.value.status ==
            WebsiteEnforcementStatus.ACTIVE
        ) {
            markError("Website protection stopped unexpectedly. Try again from Settings.")
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = super.onBind(intent)

    private fun establishTunnel(zoneId: String, domains: List<String>) {
        if (VpnService.prepare(this) != null) {
            markConsentRequired("VPN approval is no longer available. Approve Bounds again in Settings.")
            stopSelf()
            return
        }
        closeTunnel()
        val builder = Builder()
            .setSession("Bounds website blocking")
            .setMtu(1500)
            .addAddress(TUN_CLIENT_ADDRESS, 32)
            .addRoute(TUN_DNS_ADDRESS, 32)
            .addDnsServer(TUN_DNS_ADDRESS)
        runCatching { builder.addDisallowedApplication(packageName) }

        val established = runCatching { builder.establish() }.getOrNull()
        if (established == null) {
            stoppingIntentionally = true
            markDisplaced(
                "Bounds could not take VPN ownership. Disable another or always-on VPN, then try again."
            )
            stopSelf()
            return
        }
        tunInterface = established
        activeZoneId = zoneId
        activeDomains = domains
        val fd = established.fileDescriptor
        val app = applicationContext as BoundsApplication
        app.setWebsiteEnforcement(
            WebsiteEnforcementState(
                status = WebsiteEnforcementStatus.ACTIVE,
                message = "Website blocking is active for this zone.",
                activeZoneId = zoneId,
                domains = domains
            )
        )
        registerNetworkCallback()
        val workerGeneration = feedbackGeneration.get()
        worker = serviceScope.launch {
            val input = FileInputStream(fd)
            val output = FileOutputStream(fd)
            try {
                    val packet = ByteArray(32767)
                    while (isActive) {
                        val length = input.read(packet)
                        if (length > 0) {
                            handlePacket(
                                packet = packet.copyOf(length),
                                output = output,
                                blockedDomains = domains,
                                workerGeneration = workerGeneration
                            )
                        }
                    }
            } finally {
                runCatching { input.close() }
                runCatching { output.close() }
            }
        }
    }

    private fun handlePacket(
        packet: ByteArray,
        output: FileOutputStream,
        blockedDomains: List<String>,
        workerGeneration: Long
    ) {
        val query = DnsPacket.parse(packet) ?: return
        val matchedDomain = blockedDomains.firstOrNull {
            DomainBlocklist.matchesDomain(query.name, it)
        }
        val response = if (matchedDomain != null) {
            showBlockFeedback(matchedDomain, workerGeneration)
            DnsPacket.nxdomain(query)
        } else {
            forward(query)
        } ?: return
        runCatching { output.write(DnsPacket.ipv4UdpResponse(query, response)) }
    }

    private fun showBlockFeedback(blockedDomain: String, workerGeneration: Long) {
        val showRunnable = Runnable {
            if (
                feedbackGeneration.get() != workerGeneration ||
                tunInterface == null
            ) return@Runnable
            showBlockFeedbackOnMain(blockedDomain)
        }
        mainHandler.postAtTime(
            showRunnable,
            BLOCK_FEEDBACK_CALLBACK_TOKEN,
            SystemClock.uptimeMillis()
        )
    }

    private fun showBlockFeedbackOnMain(blockedDomain: String) {
        val now = System.currentTimeMillis()
        val lastShown = lastBlockFeedbackAt[blockedDomain] ?: 0L
        if (now - lastShown < BLOCK_FEEDBACK_COOLDOWN_MS) return
        lastBlockFeedbackAt[blockedDomain] = now

        getSystemService(NotificationManager::class.java).notify(
            NOTIFICATION_ID,
            notification(
                title = "Bounds blocked $blockedDomain",
                text = "Website blocked by your active zone"
            )
        )
        restoreNotificationRunnable?.let(mainHandler::removeCallbacks)
        val restoreRunnable = Runnable {
            restoreNotificationRunnable = null
            if (tunInterface != null) {
                getSystemService(NotificationManager::class.java).notify(
                    NOTIFICATION_ID,
                    notification()
                )
            }
        }
        restoreNotificationRunnable = restoreRunnable
        mainHandler.postAtTime(
            restoreRunnable,
            BLOCK_FEEDBACK_CALLBACK_TOKEN,
            SystemClock.uptimeMillis() + BLOCK_FEEDBACK_DURATION_MS
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            return
        }
        hideBlockFeedback()

        val banner = createBlockFeedbackBanner(blockedDomain)
        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP
        }

        runCatching {
            windowManager.addView(banner, params)
            blockFeedbackView = banner
            val hideRunnable = Runnable { hideBlockFeedback() }
            hideBlockFeedbackRunnable = hideRunnable
            mainHandler.postAtTime(
                hideRunnable,
                BLOCK_FEEDBACK_CALLBACK_TOKEN,
                SystemClock.uptimeMillis() + BLOCK_FEEDBACK_DURATION_MS
            )
        }
    }

    private fun createBlockFeedbackBanner(blockedDomain: String): View {
        val outer = FrameLayout(this).apply {
            setPadding(dp(16), dp(16), dp(16), 0)
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(14), dp(18), dp(14))
            elevation = dp(8).toFloat()
            background = GradientDrawable().apply {
                setColor(Color.rgb(13, 22, 38))
                cornerRadius = dp(16).toFloat()
                setStroke(dp(1), Color.rgb(255, 193, 7))
            }
        }
        content.addView(TextView(this).apply {
            text = "Website blocked by Bounds"
            setTextColor(Color.WHITE)
            textSize = 15f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        content.addView(TextView(this).apply {
            text = blockedDomain
            setTextColor(Color.rgb(255, 193, 7))
            textSize = 13f
            setPadding(0, dp(3), 0, 0)
        })
        outer.addView(
            content,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        )
        return outer
    }

    private fun hideBlockFeedback() {
        hideBlockFeedbackRunnable?.let(mainHandler::removeCallbacks)
        hideBlockFeedbackRunnable = null
        blockFeedbackView?.let { view ->
            runCatching { windowManager.removeView(view) }
        }
        blockFeedbackView = null
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun forward(query: DnsPacket): ByteArray? {
        return try {
            DatagramSocket().use { socket ->
                protect(socket)
                socket.soTimeout = 2_000
                val address = InetAddress.getByName(UPSTREAM_DNS)
                socket.send(DatagramPacket(query.dnsPayload, query.dnsPayload.size, address, DNS_PORT))
                val buffer = ByteArray(4096)
                val response = DatagramPacket(buffer, buffer.size)
                socket.receive(response)
                response.data.copyOf(response.length)
            }
        } catch (_: Exception) {
            val app = applicationContext as BoundsApplication
            app.setWebsiteEnforcement(
                WebsiteEnforcementState(
                    status = WebsiteEnforcementStatus.ERROR,
                    message = "The upstream DNS connection failed. Check the network, then retry from Settings.",
                    activeZoneId = activeZoneId,
                    domains = activeDomains
                )
            )
            null
        }
    }

    private fun closeTunnel() {
        feedbackGeneration.incrementAndGet()
        mainHandler.removeCallbacksAndMessages(BLOCK_FEEDBACK_CALLBACK_TOKEN)
        hideBlockFeedback()
        worker?.cancel()
        worker = null
        networkCallback?.let { callback ->
            runCatching {
                getSystemService(ConnectivityManager::class.java)
                    .unregisterNetworkCallback(callback)
            }
        }
        networkCallback = null
        runCatching { tunInterface?.close() }
        tunInterface = null
        activeZoneId = null
        activeDomains = emptyList()
        lastBlockFeedbackAt.clear()
        restoreNotificationRunnable = null
    }

    private fun registerNetworkCallback() {
        val connectivity = getSystemService(ConnectivityManager::class.java) ?: return
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onLost(network: Network) {
                val app = applicationContext as BoundsApplication
                app.setWebsiteEnforcement(
                    WebsiteEnforcementState(
                        status = WebsiteEnforcementStatus.ERROR,
                        message = "The active network was lost. Reconnect, then retry website protection.",
                        activeZoneId = activeZoneId,
                        domains = activeDomains
                    )
                )
            }

            override fun onAvailable(network: Network) {
                if (tunInterface != null && activeZoneId != null) {
                    (applicationContext as BoundsApplication).setWebsiteEnforcement(
                        WebsiteEnforcementState(
                            status = WebsiteEnforcementStatus.ACTIVE,
                            message = "Website blocking is active for this zone.",
                            activeZoneId = activeZoneId,
                            domains = activeDomains
                        )
                    )
                }
            }
        }
        networkCallback = callback
        runCatching { connectivity.registerDefaultNetworkCallback(callback) }
            .onFailure { networkCallback = null }
    }

    private fun updateReadyState() {
        val app = applicationContext as BoundsApplication
        val status = if (VpnService.prepare(this) == null) WebsiteEnforcementStatus.READY
        else WebsiteEnforcementStatus.CONSENT_REQUIRED
        app.setWebsiteEnforcement(
            WebsiteEnforcementState(
                status = status,
                message = if (status == WebsiteEnforcementStatus.READY) {
                    "VPN consent is ready. Website rules activate inside a zone."
                } else {
                    "VPN consent is required before Bounds can block websites."
                }
            )
        )
    }

    private fun markConsentRequired(message: String) {
        (applicationContext as BoundsApplication).setWebsiteEnforcement(
            WebsiteEnforcementState(WebsiteEnforcementStatus.CONSENT_REQUIRED, message)
        )
    }

    private fun markError(message: String) {
        (applicationContext as BoundsApplication).setWebsiteEnforcement(
            WebsiteEnforcementState(WebsiteEnforcementStatus.ERROR, message)
        )
    }

    private fun markDisplaced(message: String) {
        (applicationContext as BoundsApplication).setWebsiteEnforcement(
            WebsiteEnforcementState(WebsiteEnforcementStatus.DISPLACED, message)
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Website protection",
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
    }

    private fun notification(
        title: String = "Bounds website protection",
        text: String = "Filtering configured domains"
    ): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_favorite)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    private data class DnsPacket(
        val sourceAddress: ByteArray,
        val sourcePort: Int,
        val dnsPayload: ByteArray,
        val name: String,
        val questionEnd: Int
    ) {
        companion object {
            fun parse(packet: ByteArray): DnsPacket? {
                if (packet.size < 28 || (packet[0].toInt() and 0xf0) != 0x40) return null
                val headerLength = (packet[0].toInt() and 0x0f) * 4
                if (packet.size < headerLength + 8 + 12) return null
                if ((packet[9].toInt() and 0xff) != 17) return null
                val udpOffset = headerLength
                val destinationPort = unsignedShort(packet, udpOffset + 2)
                if (destinationPort != DNS_PORT) return null
                val dnsOffset = udpOffset + 8
                val dnsPayload = packet.copyOfRange(dnsOffset, packet.size)
                val nameResult = readName(dnsPayload, 12) ?: return null
                if (nameResult.second + 4 > dnsPayload.size) return null
                return DnsPacket(
                    sourceAddress = packet.copyOfRange(12, 16),
                    sourcePort = unsignedShort(packet, udpOffset),
                    dnsPayload = dnsPayload,
                    name = nameResult.first,
                    questionEnd = nameResult.second + 4
                )
            }

            fun nxdomain(query: DnsPacket): ByteArray {
                val response = query.dnsPayload.copyOfRange(0, query.questionEnd)
                response[2] = ((response[2].toInt() or 0x80) and 0xff).toByte()
                response[3] = ((response[3].toInt() and 0x70) or 0x03).toByte()
                response[4] = 0
                response[5] = 1
                response[6] = 0
                response[7] = 0
                response[8] = 0
                response[9] = 0
                return response
            }

            fun ipv4UdpResponse(query: DnsPacket, dns: ByteArray): ByteArray {
                val udpLength = 8 + dns.size
                val packet = ByteArray(20 + udpLength)
                packet[0] = 0x45
                packet[2] = ((packet.size shr 8) and 0xff).toByte()
                packet[3] = (packet.size and 0xff).toByte()
                packet[8] = 64
                packet[9] = 17
                packet[12] = 10
                packet[13] = 0
                packet[14] = 0
                packet[15] = 1
                query.sourceAddress.copyInto(packet, 16)
                putShort(packet, 20, DNS_PORT)
                putShort(packet, 22, query.sourcePort)
                putShort(packet, 24, udpLength)
                dns.copyInto(packet, 28)
                putShort(packet, 10, checksum(packet, 0, 20))
                // UDP checksum 0 is valid for IPv4 and avoids a second pseudo-header pass.
                return packet
            }

            private fun readName(payload: ByteArray, start: Int): Pair<String, Int>? {
                var offset = start
                val labels = mutableListOf<String>()
                repeat(128) {
                    if (offset >= payload.size) return null
                    val length = payload[offset++].toInt() and 0xff
                    if (length == 0) return labels.joinToString("." ) to offset
                    if (length > 63 || offset + length > payload.size) return null
                    labels += String(payload, offset, length, Charsets.US_ASCII)
                    offset += length
                }
                return null
            }

            private fun unsignedShort(bytes: ByteArray, offset: Int): Int =
                ((bytes[offset].toInt() and 0xff) shl 8) or (bytes[offset + 1].toInt() and 0xff)

            private fun putShort(bytes: ByteArray, offset: Int, value: Int) {
                bytes[offset] = (value shr 8).toByte()
                bytes[offset + 1] = value.toByte()
            }

            private fun checksum(bytes: ByteArray, offset: Int, length: Int): Int {
                var sum = 0L
                var i = offset
                while (i < offset + length) {
                    sum += unsignedShort(bytes, i)
                    i += 2
                }
                while ((sum ushr 16) != 0L) sum = (sum and 0xffff) + (sum ushr 16)
                return sum.inv().toInt() and 0xffff
            }
        }
    }
}