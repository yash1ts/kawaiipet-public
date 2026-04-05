package com.kawaiipet.app.overlay

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.kawaiipet.app.KawaiiPetApplication
import com.kawaiipet.app.R
import com.kawaiipet.app.audio.AudioPipeline
import com.kawaiipet.app.audio.ModelManager
import com.kawaiipet.app.llm.ConversationManager
import com.kawaiipet.app.pet.PetAnimationController
import com.kawaiipet.app.pet.PetViewModel
import com.kawaiipet.app.ui.MainActivity
import com.kawaiipet.app.util.PreferenceManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.math.roundToInt
import javax.inject.Inject

@AndroidEntryPoint
class OverlayService : Service() {

    @Inject lateinit var conversationManager: ConversationManager
    @Inject lateinit var audioPipeline: AudioPipeline
    @Inject lateinit var modelManager: ModelManager
    @Inject lateinit var preferenceManager: PreferenceManager
    @Inject lateinit var animationController: PetAnimationController

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(serviceJob + Dispatchers.Main.immediate)

    private lateinit var windowManager: WindowManager
    private var overlayView: ComposeView? = null
    private val lifecycleOwner = OverlayLifecycleOwner()
    private lateinit var petViewModel: PetViewModel
    private lateinit var overlayLayoutParams: WindowManager.LayoutParams
    private var closeDragHintView: ComposeView? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildNotification())

        val (sttId, ttsId) = runBlocking(Dispatchers.IO) {
            preferenceManager.getSttModelId() to preferenceManager.getTtsModelId()
        }
        audioPipeline.schedulePetVoiceModelPrepare(
            scope = serviceScope,
            sttId = sttId,
            ttsId = ttsId,
            loadStt = sttId.isNotBlank() && modelManager.isModelDownloaded(sttId),
            loadTts = ttsId.isNotBlank() && modelManager.isModelDownloaded(ttsId)
        )

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        lifecycleOwner.onCreate()
        lifecycleOwner.onStart()
        lifecycleOwner.onResume()

        petViewModel = createPetViewModel()
        attachOverlay()
    }

    private fun createPetViewModel(): PetViewModel {
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                PetViewModel(
                    applicationContext,
                    conversationManager,
                    audioPipeline,
                    preferenceManager,
                    modelManager,
                    animationController
                ) as T
        }
        return ViewModelProvider(lifecycleOwner, factory)[PetViewModel::class.java]
    }

    private fun attachOverlay() {
        overlayLayoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 300
        }

        overlayView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)

            setContent {
                OverlayContent(
                    petViewModel = petViewModel,
                    animationController = animationController,
                    onDrag = { dx, dy ->
                        overlayLayoutParams.x += dx.toInt()
                        overlayLayoutParams.y += dy.toInt()
                        windowManager.updateViewLayout(this, overlayLayoutParams)
                    },
                    onPetDragStart = { showCloseDragHint() },
                    onPetDragEnd = {
                        tryDismissIfReleasedOverCloseHint()
                        hideCloseDragHint()
                    },
                    onRequestFocus = { focusable -> setFocusable(focusable) },
                    onDismiss = { stopSelf() }
                )
            }
        }

        windowManager.addView(overlayView, overlayLayoutParams)
    }

    private fun closeStripHeightPx(): Int =
        (CLOSE_STRIP_HEIGHT_DP * resources.displayMetrics.density).roundToInt()

    private fun screenHeightPx(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val wm = getSystemService(WINDOW_SERVICE) as WindowManager
            wm.currentWindowMetrics.bounds.height()
        } else {
            @Suppress("DEPRECATION")
            resources.displayMetrics.heightPixels
        }

    private fun showCloseDragHint() {
        if (closeDragHintView != null) return
        val heightPx = closeStripHeightPx()
        val hint = ComposeView(this).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setContent { OverlayCloseDragHint() }
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            heightPx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM
        }
        windowManager.addView(hint, params)
        closeDragHintView = hint
    }

    private fun hideCloseDragHint() {
        closeDragHintView?.let { v ->
            try {
                windowManager.removeView(v)
            } catch (e: Exception) {
                Log.w(TAG, "remove close drag hint", e)
            }
            closeDragHintView = null
        }
    }

    private fun tryDismissIfReleasedOverCloseHint() {
        val main = overlayView ?: return
        val loc = IntArray(2)
        main.getLocationOnScreen(loc)
        val contentBottom = loc[1] + main.height
        val sh = screenHeightPx()
        val zoneTop = sh - closeStripHeightPx()
        if (contentBottom >= zoneTop) {
            stopSelf()
        }
    }

    private fun setFocusable(focusable: Boolean) {
        overlayLayoutParams.flags = if (focusable) {
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        } else {
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        }
        overlayView?.let { windowManager.updateViewLayout(it, overlayLayoutParams) }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        petViewModel.cleanup()
        hideCloseDragHint()
        overlayView?.let { windowManager.removeView(it) }
        overlayView = null
        lifecycleOwner.onPause()
        lifecycleOwner.onStop()
        lifecycleOwner.onDestroy()
    }

    private fun buildNotification(): Notification {
        val tapIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, KawaiiPetApplication.NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.drawable.ic_pet_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(tapIntent)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val TAG = "OverlayService"
        private const val NOTIFICATION_ID = 1
        private const val CLOSE_STRIP_HEIGHT_DP = 140f
    }
}
