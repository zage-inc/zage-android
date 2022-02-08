package com.tryzage.zageandroid

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.net.http.SslError
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.*

import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.widget.FrameLayout
import java.io.FileNotFoundException
import java.net.URL
import java.util.concurrent.Executors

/**
 * The Zage class that allows integration with the Zage payment flow
 */
class Zage constructor(
    private val context: AppCompatActivity,
    private val publicKey: String
) {
    private var PROD_APP_URL = "https://production.zage.dev/checkout"
    private var SB_APP_URL = "https://sandbox.zage.dev/checkout"

    /**
     * Given a payment token, onComplete, and onExit handler, open the Zage payment flow
     */
    fun openPayment(token: String, onComplete: (Any) -> Unit, onExit: () -> Unit) {
        var webView: WebView = WebView(context)

        // Helper function to close the webview
        fun closeWebview() {
            context.runOnUiThread {
                var group = webView.parent as ViewGroup
                group.removeView(webView)
            }
        }

        webView.settings.javaScriptEnabled = true

        // Make the webview transparent
        webView.setBackgroundColor(Color.TRANSPARENT)

        // Route the request to sandbox or prod
        var zageApp: String
        if (publicKey.startsWith("sandbox_")) {
            zageApp = SB_APP_URL
        } else {
            zageApp = PROD_APP_URL
        }
        webView.loadUrl(zageApp)

        // This class acts as the bridge between the javascript running in the webview and the native Android code
        class JSInterface() {
            @JavascriptInterface
            fun completedPayment(res: String) {
                // On payment completion handler
                closeWebview()
                onComplete(res)
            }

            @JavascriptInterface
            fun exitedPayment() {
                // on payment exited handler
                closeWebview()
                onExit()
            }
        }
        // Add the javascript interface to the weview
        webView.addJavascriptInterface(JSInterface(), "Android")

        context.addContentView(webView, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        FixSoftKeyboard.assistActivity(webView)

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageFinished(view, url);

                // Url to retrieve javascript to inject
                val url = "https://api.zage.dev/v0/v0-android.js"
                val executor = Executors.newSingleThreadExecutor()
                val handler = Handler(Looper.getMainLooper())

                executor.execute {
                    try {
                        var res = URL(url).readText()
                        handler.post {
                            // Inject obfuscated javascript into webView and call openPayment.
                            webView.evaluateJavascript(res, null)
                            webView.evaluateJavascript("openPayment('$token', '$publicKey')", null)
                        }
                    } catch (e: FileNotFoundException) {
                        // If the javascript fetch failed for some reason, close the webview and call on exit
                        closeWebview()
                        onExit()
                    }
                }
            }

            override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler?,
                error: SslError?
            ) {
                handler?.proceed();
            }
        }
    }

    /**
     * Helper class to help fix keyboard covering UI issue
     */
    private class FixSoftKeyboard private constructor(webView: WebView) {
        private val mChildOfContent: View
        private var usableHeightPrevious = 0
        private val frameLayoutParams: FrameLayout.LayoutParams
        private val restoreHeight: Int

        private fun possiblyResizeChildOfContent() {
            val usableHeightNow = computeUsableHeight()
            if (usableHeightNow != usableHeightPrevious) {
                val usableHeightSansKeyboard = mChildOfContent.rootView.height
                val heightDifference = usableHeightSansKeyboard - usableHeightNow
                if (heightDifference > usableHeightSansKeyboard / 4) {
                    // keyboard probably just became visible
                    frameLayoutParams.height = usableHeightSansKeyboard - heightDifference
                } else {
                    // keyboard probably just became hidden
                    frameLayoutParams.height = restoreHeight;
                }
                mChildOfContent.requestLayout()
                usableHeightPrevious = usableHeightNow
            }
        }

        private fun computeUsableHeight(): Int {
            val r = Rect()
            mChildOfContent.getWindowVisibleDisplayFrame(r)
            return r.bottom - r.top
        }

        companion object {
            fun assistActivity(webView: WebView) {
                FixSoftKeyboard(webView)
            }
        }

        init {
            mChildOfContent = webView
            mChildOfContent.viewTreeObserver.addOnGlobalLayoutListener { possiblyResizeChildOfContent() }
            frameLayoutParams = mChildOfContent.layoutParams as FrameLayout.LayoutParams
            restoreHeight = frameLayoutParams.height;
        }
    }
}


