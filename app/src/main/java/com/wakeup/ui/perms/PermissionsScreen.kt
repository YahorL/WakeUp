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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.wakeup.ui.components.noRippleClickable
import com.wakeup.ui.theme.Screen
import com.wakeup.ui.theme.Wake

/** Returns true when all alarm-critical permissions are satisfied. */
fun alarmPermissionsGranted(context: Context): Boolean {
    val notif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    else true
    val exact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()
    else true
    return notif && exact
}

@Composable
fun PermissionsScreen(onAllGranted: () -> Unit) {
    val context = LocalContext.current
    val colors = Wake.colors
    val fonts = Wake.fonts
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

    Screen {
        Column(
            Modifier.fillMaxSize().systemBarsPadding().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            Text("Permissions needed", color = colors.text, fontFamily = fonts.display, fontWeight = FontWeight.Medium, fontSize = 30.sp, letterSpacing = (-0.5).sp)
            Text(
                "WakeUp needs these to ring reliably over the lock screen.",
                color = colors.textDim, fontFamily = fonts.ui, fontSize = 14.5.sp, lineHeight = 22.sp,
            )
            Spacer(Modifier.height(6.dp))

            AccentButton("Allow notifications") {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                else refresh++
            }
            AccentButton("Allow exact alarms") {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
                }
            }
            OutlineButton("Disable battery optimization") {
                context.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}")))
            }

            Spacer(Modifier.weight(1f))
            Box(Modifier.fillMaxWidth().noRippleClickable { refresh++ }.padding(12.dp), contentAlignment = Alignment.Center) {
                Text("I've granted these — continue", color = colors.textDim, fontFamily = fonts.ui, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun AccentButton(label: String, onClick: () -> Unit) {
    val colors = Wake.colors
    Box(
        Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(16.dp)).background(colors.accent).noRippleClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = colors.onAccent, fontFamily = Wake.fonts.ui, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    }
}

@Composable
private fun OutlineButton(label: String, onClick: () -> Unit) {
    val colors = Wake.colors
    Box(
        Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(16.dp))
            .border(1.5.dp, colors.hairStrong, RoundedCornerShape(16.dp)).noRippleClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = colors.text, fontFamily = Wake.fonts.ui, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    }
}
