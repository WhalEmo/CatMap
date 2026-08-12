package com.beem.catmap

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView

class WarningMessage(
    private val context: Context,
    private val sefafMi: Boolean = true
) {
    private var dialog: Dialog? = null

    private var yuklemeEkrani: FrameLayout? = null
    private var durumTextView: TextView? = null
    private var basariliTik: ImageView? = null
    private var basarisizCarpi: ImageView? = null
    private var yuklemeBar: ProgressBar? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private var autoDismissRunnable: Runnable? = null

    init {
        initDialog()
    }

    private fun initDialog() {
        val newDialog = Dialog(context)
        newDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        newDialog.setContentView(R.layout.giris_yukleme)
        newDialog.setCancelable(false)

        newDialog.window?.let { window ->
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            window.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        yuklemeEkrani = newDialog.findViewById(R.id.progressOverlay)
        durumTextView = newDialog.findViewById(R.id.progressMessage)
        basariliTik = newDialog.findViewById(R.id.successIcon)
        basarisizCarpi = newDialog.findViewById(R.id.basariDegilIcon)
        yuklemeBar = newDialog.findViewById(R.id.progressBar)

        if (sefafMi) {
            yuklemeEkrani?.background = null
        }

        dialog = newDialog
    }

    fun YuklemeDurum(mesaj: String) {
        runOnMain {
            cancelScheduledDismiss()
            showDialogSafely()

            yuklemeEkrani?.visibility = View.VISIBLE
            basariliTik?.visibility = View.GONE
            basarisizCarpi?.visibility = View.GONE
            yuklemeBar?.visibility = View.VISIBLE
            durumTextView?.text = mesaj
        }
    }

    fun BasariliDurum(mesaj: String, kacMilisaniye: Int = 1500) {
        runOnMain {
            cancelScheduledDismiss()
            showDialogSafely()

            yuklemeEkrani?.visibility = View.VISIBLE
            basariliTik?.visibility = View.VISIBLE
            basarisizCarpi?.visibility = View.GONE
            yuklemeBar?.visibility = View.GONE
            durumTextView?.text = mesaj

            scheduleDismiss(kacMilisaniye)
        }
    }

    fun BasarisizDurum(mesaj: String, kacMilisaniye: Int = 1500) {
        runOnMain {
            cancelScheduledDismiss()
            showDialogSafely()

            yuklemeEkrani?.visibility = View.VISIBLE
            basariliTik?.visibility = View.GONE
            basarisizCarpi?.visibility = View.VISIBLE
            yuklemeBar?.visibility = View.GONE
            durumTextView?.text = mesaj

            scheduleDismiss(kacMilisaniye)
        }
    }

    fun kapat() {
        runOnMain {
            cancelScheduledDismiss()
            dismissSafely()
        }
    }

    private fun showDialogSafely() {
        if (!isContextValid()) return
        val currentDialog = dialog ?: return

        if (!currentDialog.isShowing) {
            try {
                currentDialog.show()
                currentDialog.window?.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun dismissSafely() {
        val currentDialog = dialog ?: return
        if (currentDialog.isShowing) {
            try {
                currentDialog.dismiss()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun scheduleDismiss(delayMillis: Int) {
        val runnable = Runnable { dismissSafely() }
        autoDismissRunnable = runnable
        mainHandler.postDelayed(runnable, delayMillis.toLong())
    }

    private fun cancelScheduledDismiss() {
        autoDismissRunnable?.let {
            mainHandler.removeCallbacks(it)
            autoDismissRunnable = null
        }
    }

    private fun isContextValid(): Boolean {
        if (context is Activity) {
            return !context.isFinishing && !context.isDestroyed
        }
        return true
    }

    private inline fun runOnMain(crossinline action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action()
        } else {
            mainHandler.post { action() }
        }
    }
}