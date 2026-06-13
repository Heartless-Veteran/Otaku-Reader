package app.otakureader.security

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.SystemClock
import android.provider.Settings
import java.util.Calendar
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.otakureader.R

/**
 * Wraps [content] with an optional biometric / device-credential lock.
 *
 * When [enabled], the app starts locked and re-locks after it has been in the background for at
 * least [timeoutMillis]. While locked, an opaque overlay covers the content and a
 * [BiometricPrompt] is shown. Unlock requires a strong biometric or the device PIN/pattern.
 */
@Composable
fun BiometricLockGate(
    enabled: Boolean,
    timeoutMillis: Long,
    scheduleEnabled: Boolean = false,
    startHour: Int = 0,
    endHour: Int = 0,
    activeDays: Set<Int> = emptySet(),
    content: @Composable () -> Unit,
) {
    val effectivelyEnabled = enabled && (!scheduleEnabled || isWithinLockSchedule(startHour, endHour, activeDays))
    val context = LocalContext.current
    // LocalContext may be a ContextWrapper (theme wrapper, Hilt/navigation wrapper); walk the
    // chain to find the hosting FragmentActivity rather than casting directly.
    val activity = remember(context) { context.findFragmentActivity() }

    // Deliberately remember (NOT rememberSaveable): saved state survives process death, so after
    // the OS kills the app in the background and restores it, rememberSaveable restored
    // initialized=true / locked=false and the app came back UNLOCKED — a lock bypass. With
    // remember, any Activity recreation (process restore, and also rotation) re-runs the
    // cold-start lock guard below, erring toward locked. Mihon's app lock behaves the same way.
    var locked by remember { mutableStateOf(false) }
    var initialized by remember { mutableStateOf(false) }
    var backgroundedAt by remember { mutableLongStateOf(0L) }

    // Lock once when the feature is first known to be on (cold start). Disabling resets the
    // one-shot guard so re-enabling within the same process locks again immediately.
    LaunchedEffect(effectivelyEnabled) {
        if (effectivelyEnabled && !initialized) {
            locked = true
            initialized = true
        } else if (!effectivelyEnabled) {
            locked = false
            initialized = false
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, effectivelyEnabled, timeoutMillis) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                // elapsedRealtime() is monotonic — unaffected by clock changes / NTP sync.
                Lifecycle.Event.ON_STOP -> backgroundedAt = SystemClock.elapsedRealtime()
                Lifecycle.Event.ON_START -> {
                    if (effectivelyEnabled && backgroundedAt > 0L &&
                        SystemClock.elapsedRealtime() - backgroundedAt >= timeoutMillis
                    ) {
                        locked = true
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Gate composition: while locked, do NOT compose content() at all. Previously content() was
    // always composed and the overlay merely drawn on top, so protected screens were fully built
    // (and briefly visible during transitions) behind the lock. Composing it only when unlocked
    // keeps sensitive UI out of the tree entirely while locked.
    if (effectivelyEnabled && locked) {
        val title = stringResource(R.string.biometric_lock_title)
        val subtitle = stringResource(R.string.biometric_lock_subtitle)
        val authAvailable = activity != null && canAuthenticate(activity)
        BiometricLockOverlay(
            authAvailable = authAvailable,
            onUnlockClick = { activity?.let { promptUnlock(it, title, subtitle) { locked = false } } },
            onOpenSecuritySettings = {
                activity?.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
            },
        )
        // Auto-prompt when authentication is possible. When it is NOT (no enrolled credential
        // / unsupported hardware), keep the app locked — never silently expose content while
        // the lock is enabled; the overlay guides the user to set up device security.
        LaunchedEffect(authAvailable) {
            if (authAvailable) {
                activity?.let { promptUnlock(it, title, subtitle) { locked = false } }
            }
        }
    } else {
        content()
    }
}

@Composable
private fun BiometricLockOverlay(
    authAvailable: Boolean,
    onUnlockClick: () -> Unit,
    onOpenSecuritySettings: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.padding(bottom = 24.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.biometric_lock_title),
                style = MaterialTheme.typography.titleMedium,
            )
            if (authAvailable) {
                Button(onClick = onUnlockClick, modifier = Modifier.padding(top = 24.dp)) {
                    Text(stringResource(R.string.biometric_lock_unlock))
                }
            } else {
                Text(
                    text = stringResource(R.string.biometric_lock_no_auth),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Button(onClick = onOpenSecuritySettings, modifier = Modifier.padding(top = 24.dp)) {
                    Text(stringResource(R.string.biometric_lock_open_settings))
                }
            }
        }
    }
}

private fun Context.findFragmentActivity(): FragmentActivity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is FragmentActivity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

/**
 * Returns true if the current wall-clock time falls within the lock enforcement window.
 *
 * [startHour] and [endHour] are 0-23. When [startHour] > [endHour] the window wraps midnight
 * (e.g., start=22, end=8 enforces lock from 22:00 to 07:59).
 * [activeDays] uses [Calendar] day constants (Calendar.MONDAY = 2 … Calendar.SUNDAY = 1).
 * An empty [activeDays] set means all days are active.
 */
private fun isWithinLockSchedule(startHour: Int, endHour: Int, activeDays: Set<Int>): Boolean {
    val now = Calendar.getInstance()
    val dayOfWeek = now.get(Calendar.DAY_OF_WEEK)
    val hour = now.get(Calendar.HOUR_OF_DAY)
    val dayAllowed = activeDays.isEmpty() || dayOfWeek in activeDays
    val hourAllowed = if (startHour <= endHour) {
        hour in startHour until endHour
    } else {
        hour >= startHour || hour < endHour
    }
    return dayAllowed && hourAllowed
}

private const val ALLOWED_AUTHENTICATORS = BIOMETRIC_STRONG or DEVICE_CREDENTIAL

private fun canAuthenticate(activity: FragmentActivity): Boolean =
    BiometricManager.from(activity).canAuthenticate(ALLOWED_AUTHENTICATORS) ==
        BiometricManager.BIOMETRIC_SUCCESS

private fun promptUnlock(
    activity: FragmentActivity,
    title: String,
    subtitle: String,
    onSuccess: () -> Unit,
) {
    val executor = ContextCompat.getMainExecutor(activity)
    val prompt = BiometricPrompt(
        activity,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }
        },
    )
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle(title)
        .setSubtitle(subtitle)
        .setAllowedAuthenticators(ALLOWED_AUTHENTICATORS)
        .build()
    prompt.authenticate(info)
}
