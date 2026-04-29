package app.otakureader.core.ui.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * Save a bitmap to the app's cache directory for sharing.
 *
 * @param context Application context
 * @param bitmap The bitmap to save
 * @param filename Optional filename (default: share_image.png)
 * @return Uri suitable for sharing via FileProvider
 */
fun saveBitmapToCache(
    context: Context,
    bitmap: Bitmap,
    filename: String = "share_image.png"
): Uri {
    val cacheDir = File(context.cacheDir, "share_images").apply { mkdirs() }
    val file = File(cacheDir, filename)

    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }

    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
}

/**
 * Create a share Intent for a captured stats image.
 *
 * @param imageUri The URI of the image to share
 * @param title Optional share title
 * @return Share Intent with FLAG_GRANT_READ_URI_PERMISSION set
 */
fun createShareIntent(imageUri: Uri, title: String = "My Reading Stats"): Intent {
    return Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, imageUri)
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, "Check out my reading stats on Otaku Reader!")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}
