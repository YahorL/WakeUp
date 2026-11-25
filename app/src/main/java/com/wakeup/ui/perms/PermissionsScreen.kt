package com.wakeup.ui.perms

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.wakeup.ui.components.GlassRecipe
import com.wakeup.ui.components.Label
import com.wakeup.ui.components.PrimaryButton
import com.wakeup.ui.components.WakeIcon
import com.wakeup.ui.components.glass
import com.wakeup.ui.theme.OrbLayout
import com.wakeup.ui.theme.Screen
import com.wakeup.ui.theme.Wake
import com.wakeup.ui.theme.pressable
import com.wakeup.ui.theme.riseIn

/** Returns true when all alarm-critical permissions are satisfied. */
fun alarmPermissionsGranted(context: Context): Boolean {
    val notifications = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    val exactAlarms = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()
    return notifications && exactAlarms && Settings.canDrawOverlays(context)
}

@Composable
fun PermissionsScreen(onAllGranted: () -> Unit) {
    val context = LocalContext.current
    val colors = Wake.colors
    var refresh by remember { mutableStateOf(0) }
    val granted = remember(refresh) { alarmPermissionsGranted(context) }

    // Re-check permissions whenever we return to this screen (e.g. back from the
    // exact-alarm or battery-optimization Settings pages, which have no result callback).
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(granted) { if (granted) onAllGranted() }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { refresh++ }

    // Opens the system settings page where this app is granted [action].
    fun openSettings(action: String) =
        context.startActivity(Intent(action, Uri.parse("package:${context.packageName}")))

    Screen(OrbLayout.LIST) {
        Column(
            Modifier.fillMaxSize().systemBarsPadding().padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
        ) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Label("Permissions needed", 36f, Modifier.riseIn(0), tracking = -0.03f, lineHeight = 1.05f)
                Label(
                    "WakeUp needs these to ring reliably over the lock screen.", 14f,
                    Modifier.riseIn(60), color = colors.textSecondary, lineHeight = 1.45f,
                )
                Spacer(Modifier.height(12.dp))

                PermissionRow("Allow notifications", Modifier.riseIn(120)) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        refresh++
                    }
                }
                PermissionRow("Allow exact alarms", Modifier.riseIn(180)) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) openSettings(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                }
                PermissionRow("Allow display over other apps", Modifier.riseIn(240)) {
                    openSettings(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                }
                PermissionRow("Disable battery optimization", Modifier.riseIn(300), optional = true) {
                    openSettings(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                }
            }
            PrimaryButton("I've granted these — continue", { refresh++ }, Modifier.padding(top = 16.dp).riseIn(380))
        }
    }
}

@Composable
private fun PermissionRow(label: String, modifier: Modifier = Modifier, optional: Boolean = false, onClick: () -> Unit) {
    Row(
        modifier.fillMaxWidth().pressable(onClick = onClick)
            .glass(GlassRecipe.CARD, RoundedCornerShape(24.dp)).padding(horizontal = 18.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Label(label, 16f, weight = FontWeight.Medium)
            if (optional) Label("Optional, but keeps alarms on time", 12f, color = Wake.colors.textSecondary)
        }
        WakeIcon(WakeIcon.ArrowRight, size = 18.dp, color = Wake.colors.textSecondary, stroke = 1.8f)
    }
}
