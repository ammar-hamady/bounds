package com.example.bounds.ui.screens

import android.app.ActivityManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bounds.service.AppBlockingService
import com.example.bounds.ui.theme.BoundsTheme

/**
 * Full-screen Activity that appears when a blocked app is brought to the foreground.
 *
 * Explains the block context (zone name, blocked app), offers:
 *  - "Go to Home" — dismisses and sends the user to the launcher.
 *  - "Let me in once" — sends a one-time bypass to [AppBlockingService] for 5 minutes.
 *
 * Launched by [AppBlockingService] with FLAG_ACTIVITY_NEW_TASK whenever a blocked app
 * is detected in the foreground.
 */
class BlockedOverlayActivity : ComponentActivity() {

    companion object {
        const val EXTRA_BLOCKED_PACKAGE = "blocked_package"
        const val EXTRA_BLOCKED_APP_LABEL = "blocked_app_label"
        const val EXTRA_ZONE_NAME = "zone_name"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val blockedPackage = intent.getStringExtra(EXTRA_BLOCKED_PACKAGE) ?: ""
        val appLabel = intent.getStringExtra(EXTRA_BLOCKED_APP_LABEL)
            ?: resolveAppLabel(blockedPackage)
        val zoneName = intent.getStringExtra(EXTRA_ZONE_NAME) ?: "your zone"

        setContent {
            BoundsTheme(darkTheme = true) {
                BlockedOverlayScreen(
                    appLabel = appLabel,
                    zoneName = zoneName,
                    onGoHome = { goHome() },
                    onBypassOnce = { bypassOnce(blockedPackage) }
                )
            }
        }
    }

    private fun resolveAppLabel(packageName: String): String = try {
        val info = packageManager.getApplicationInfo(packageName, 0)
        packageManager.getApplicationLabel(info).toString()
    } catch (e: Exception) {
        packageName.substringAfterLast('.')
    }

    private fun goHome() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }

    private fun bypassOnce(packageName: String) {
        val bypassIntent = Intent(this, AppBlockingService::class.java).apply {
            action = AppBlockingService.ACTION_BYPASS_ONCE
            putExtra(AppBlockingService.EXTRA_BYPASS_PACKAGE, packageName)
        }
        startService(bypassIntent)
        finish()
    }
}

@Composable
private fun BlockedOverlayScreen(
    appLabel: String,
    zoneName: String,
    onGoHome: () -> Unit,
    onBypassOnce: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE8101010)),  // ~91% opaque dark scrim
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1A1A1A)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Lock icon
                Text(
                    text = "🔒",
                    fontSize = 48.sp
                )

                // App name
                Text(
                    text = appLabel,
                    color = Color(0xFFFFC107),     // Amber accent
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                // Explanation
                Text(
                    text = "This app is blocked while you're in",
                    color = Color(0xFFBBBBBB),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )

                // Zone name pill
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xFF2A2A2A)
                ) {
                    Text(
                        text = "📍  $zoneName",
                        color = Color(0xFFFFC107),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Primary action — go home
                Button(
                    onClick = onGoHome,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFC107),
                        contentColor   = Color(0xFF101010)
                    )
                ) {
                    Text(
                        text = "Go to Home Screen",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                // Secondary action — 5-minute bypass
                OutlinedButton(
                    onClick = onBypassOnce,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF888888)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333333))
                ) {
                    Text(
                        text = "Let me in once  (5 min)",
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}
