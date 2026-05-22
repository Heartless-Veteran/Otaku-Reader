package app.otakureader.data.worker

import android.content.Context
import app.otakureader.domain.scheduler.CoverRefreshScheduler
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CoverRefreshSchedulerImpl @Inject constructor() : CoverRefreshScheduler {
    override fun schedule(context: Context) {
        CoverRefreshWorker.enqueue(context)
    }
}
