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
 *
 * ## Security note (CodeQL `java/android/insecure-local-authentication`)
 *
 * CodeQL flags this callback as "insecure" because the success result only toggles a UI
 * state — it isn't used to unwrap a key or unlock encrypted storage. That is intentional:
 * this is a **privacy gate**, not a security boundary. Hidden-category rows in Room are not
 * encrypted at rest; the only thing biometric auth protects against here is over-the-shoulder
 * viewing in the library UI. There is no cryptographic material to bind a `CryptoObject` to,
 * so the canonical CodeQL fix (passing a `BiometricPrompt.CryptoObject` and using its
 * `Cipher`/`Signature` post-auth) is not applicable. The CodeQL alert should be dismissed as
 * "Won't fix — UI privacy gate, not protecting cryptographic material."
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
