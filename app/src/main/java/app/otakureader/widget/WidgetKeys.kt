package app.otakureader.widget

import androidx.glance.action.ActionParameters
import app.otakureader.util.DeepLinkHandler

object WidgetKeys {
    val MANGA_ID_KEY = ActionParameters.Key<Long>(DeepLinkHandler.EXTRA_MANGA_ID)
    val CHAPTER_ID_KEY = ActionParameters.Key<Long>(DeepLinkHandler.EXTRA_CHAPTER_ID)
}
