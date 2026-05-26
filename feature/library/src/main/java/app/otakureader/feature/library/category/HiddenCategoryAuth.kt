package app.otakureader.feature.library.category

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Requests biometric / device-credential auth before revealing hidden categories (#932).
 *
 * NOTE: this duplicates the lightweight biometric primitive from PR #959's `BiometricLockGate`
 * on purpose (per the chosen approach); the two should be consolidated once #959 lands on main.
 *
 * If the host isn't a [FragmentActivity] (BiometricPrompt's requirement) or no credential is
 * enrolled, it falls back to granting access so the user is never permanently locked out — the
 * gate hardens once the app's Activity is a FragmentActivity.
 */
internal object HiddenCategoryAuth {

    private const val AUTHENTICATORS = BIOMETRIC_STRONG or DEVICE_CREDENTIAL

    fun authenticate(context: Context, onResult: (Boolean) -> Unit) {
        val activity = context.findFragmentActivity()
        if (activity == null || !canAuthenticate(activity)) {
            onResult(true)
            return
        }
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onResult(true)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onResult(false)
                }
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock hidden categories")
            .setSubtitle("Confirm your identity to view hidden categories")
            .setAllowedAuthenticators(AUTHENTICATORS)
            .build()
        prompt.authenticate(info)
    }

    private fun canAuthenticate(activity: FragmentActivity): Boolean =
        BiometricManager.from(activity).canAuthenticate(AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS

    private fun Context.findFragmentActivity(): FragmentActivity? {
        var ctx: Context? = this
        while (ctx is ContextWrapper) {
            if (ctx is FragmentActivity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }
}
