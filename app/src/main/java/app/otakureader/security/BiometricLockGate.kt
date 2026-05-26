package app.otakureader.security

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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    var locked by rememberSaveable { mutableStateOf(false) }
    var initialized by rememberSaveable { mutableStateOf(false) }
    var backgroundedAt by rememberSaveable { mutableLongStateOf(0L) }

    // Lock once when the feature is first known to be on (cold start). Disabling always unlocks.
    LaunchedEffect(enabled) {
        if (enabled && !initialized) {
            locked = true
            initialized = true
        } else if (!enabled) {
            locked = false
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, enabled, timeoutMillis) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> backgroundedAt = System.currentTimeMillis()
                Lifecycle.Event.ON_START -> {
                    if (enabled && backgroundedAt > 0L &&
                        System.currentTimeMillis() - backgroundedAt >= timeoutMillis
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

    content()

    if (enabled && locked) {
        val title = stringResource(R.string.biometric_lock_title)
        val subtitle = stringResource(R.string.biometric_lock_subtitle)
        BiometricLockOverlay(
            onUnlockClick = { activity?.let { promptUnlock(it, title, subtitle) { locked = false } } },
        )
        // Auto-prompt as soon as the lock appears. If there's no FragmentActivity or no
        // enrolled credential, don't trap the user behind a prompt that can never succeed.
        LaunchedEffect(Unit) {
            if (activity == null || !canAuthenticate(activity)) {
                locked = false
            } else {
                promptUnlock(activity, title, subtitle) { locked = false }
            }
        }
    }
}

@Composable
private fun BiometricLockOverlay(onUnlockClick: () -> Unit) {
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
            Button(onClick = onUnlockClick, modifier = Modifier.padding(top = 24.dp)) {
                Text(stringResource(R.string.biometric_lock_unlock))
            }
        }
    }
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
