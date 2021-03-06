package knf.kuma.commons

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.AnimationDrawable
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.aesthetic.AestheticActivity
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.squareup.picasso.Callback
import knf.kuma.App
import knf.kuma.BuildConfig
import knf.kuma.R
import knf.kuma.custom.snackbar.SnackProgressBar
import knf.kuma.custom.snackbar.SnackProgressBarManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Connection
import org.jsoup.Jsoup
import xdroid.toaster.Toaster
import java.io.File

fun Toolbar.changeToolbarFont() {
    for (i in 0 until childCount) {
        val view = getChildAt(i)
        if (view is TextView && view.text == title) {
            view.typeface = ResourcesCompat.getFont(context, R.font.audiowide)
            break
        }
    }
}

fun getPackage(): String {
    return if (BuildConfig.BUILD_TYPE == "debug" || BuildConfig.BUILD_TYPE == "release" || BuildConfig.BUILD_TYPE == "playstore") "knf.kuma" else "knf.kuma.${BuildConfig.BUILD_TYPE}"
}

val getUpdateDir: String
    get() = when (BuildConfig.BUILD_TYPE) {
        "debug", "release" -> "release"
        else -> BuildConfig.BUILD_TYPE
    }

fun MaterialDialog.safeShow(func: MaterialDialog.() -> Unit): MaterialDialog {
    doOnUI {
        try {
            lifecycleOwner()
            this.func()
            this@safeShow.show()
        } catch (e: Exception) {
            //
        }
    }
    return this
}

fun MaterialDialog.safeShow() {
    doOnUI {
        try {
            lifecycleOwner()
            this@safeShow.show()
        } catch (e: Exception) {
            //
        }
    }
}

fun MaterialDialog.safeDismiss() {
    try {
        dismiss()
    } catch (exception: Exception) {
        //
    }
}

fun Snackbar.safeDismiss() {
    try {
        dismiss()
    } finally {
        //
    }
}

operator fun JSONArray.iterator(): Iterator<JSONObject> = (0 until length()).asSequence().map { get(it) as JSONObject }.iterator()

fun RecyclerView.verifyManager(size: Int = 115) {
    val manager = layoutManager
    if (manager is GridLayoutManager) {
        manager.spanCount = gridColumns(size)
        layoutManager = manager
    }
    setHasFixedSize(true)
}

fun <T> MutableList<T>.toArray(): Array<T> {
    return this.toArray()
}

val canGroupNotifications: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N

fun gridColumns(size: Int = 115): Int {
    val metrics = App.context.resources.displayMetrics
    val dpWidht = metrics.widthPixels / metrics.density
    return (dpWidht / size).toInt()
}

fun View.showSnackbar(text: String, duration: Int = Snackbar.LENGTH_SHORT): Snackbar {
    return Snackbar.make(this, text, duration).also { doOnUI { it.show() } }
}

fun SnackProgressBarManager.showProgressSnackbar(text: String, duration: Int = SnackProgressBarManager.LENGTH_SHORT) {
    val snackbar = SnackProgressBar(SnackProgressBar.TYPE_HORIZONTAL, text)
            .setIsIndeterminate(true)
            .setProgressMax(100)
            .setShowProgressPercentage(false)
    doOnUI { show(snackbar, duration) }
}

fun View.createSnackbar(text: String, duration: Int = Snackbar.LENGTH_SHORT): Snackbar {
    return Snackbar.make(this, text, duration)
}

val Int.asPx: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()

fun AestheticActivity.setDefaults() {
    AestheticUtils.setDefaults(applicationContext)
}

fun <T : View> Activity.bind(@IdRes res: Int): Lazy<T> {
    @Suppress("UNCHECKED_CAST")
    return lazy { findViewById<T>(res) }
}

fun <T : View> View.bind(@IdRes res: Int): Lazy<T> {
    @Suppress("UNCHECKED_CAST")
    return lazy { findViewById<T>(res) }
}

fun <T : View> bind(activity: AppCompatActivity, @IdRes res: Int): Lazy<T> {
    @Suppress("UNCHECKED_CAST")
    return lazy { activity.findViewById<T>(res) }
}

fun <T : View> optionalBind(activity: AppCompatActivity, @IdRes res: Int): Lazy<T?> {
    @Suppress("UNCHECKED_CAST")
    return lazy { activity.findViewById<T?>(res) }
}

fun <T : View> Activity.optionalBind(@IdRes res: Int): Lazy<T?> {
    @Suppress("UNCHECKED_CAST")
    return lazy { findViewById<T?>(res) }
}

fun Request.execute(): Response {
    return OkHttpClient().newBuilder().build().newCall(this).execute()
}

val safeContext: Context
    get() = App.context

val isTV: Boolean get() = App.context.resources.getBoolean(R.bool.isTv)

@ColorInt
fun Int.toColor(): Int {
    return ContextCompat.getColor(App.context, this)
}

fun ImageView.load(link: String?, callback: Callback? = null) {
    PicassoSingle.get().load(link).into(this, callback)
}

fun ImageView.load(uri: Uri?, callback: Callback? = null) {
    PicassoSingle.get().load(uri).into(this, callback)
}

fun noCrash(enableLog: Boolean = true, func: () -> Unit): String? {
    return try {
        func()
        null
    } catch (e: Exception) {
        if (enableLog)
            e.printStackTrace()
        e.message
    }
}

fun String?.toast() {
    if (!this.isNullOrEmpty())
        Toaster.toast(this)
}

fun doOnUI(enableLog: Boolean = true, func: () -> Unit) {
    GlobalScope.launch(Dispatchers.Main) {
        noCrash(enableLog) {
            func()
        }
    }
}

fun <T> MutableList<T>.removeAll(vararg elements: Collection<T>) {
    elements.forEach {
        removeAll(it)
    }
}

infix fun <T> Collection<T>?.notSameContent(collection: Collection<T>?) =
        collection.let {
            !(this != null && it != null && this.size == it.size && this.containsAll<T>(it))
        }

infix fun <T> Collection<T>?.isSameContent(collection: Collection<T>?) =
        collection.let {
            this != null && it != null && this.size == it.size && this.containsAll<T>(it)
        }

fun NotificationCompat.Builder.create(func: NotificationCompat.Builder.() -> Unit): NotificationCompat.Builder {
    this.func()
    return this
}

fun File.safeDelete(log: Boolean = false) {
    try {
        delete()
    } catch (e: Exception) {
        if (log)
            e.printStackTrace()
    }
}

fun RecyclerView.removeAllDecorations() {
    while (itemDecorationCount > 0)
        removeItemDecorationAt(0)
}

fun Any?.isNull(): Boolean {
    return this == null
}

fun Any?.notNull(): Boolean {
    return this != null
}

@UiThread
fun ImageView.setAnimatedResource(@DrawableRes res: Int) {
    setImageResource(res)
    val animated = drawable as AnimationDrawable
    animated.callback = this
    animated.setVisible(true, true)
    animated.start()
}

fun FloatingActionButton.forceHide() {
    val params = layoutParams as? CoordinatorLayout.LayoutParams
    params?.behavior = null
    requestLayout()
    visibility = View.GONE
}

fun jsoupCookies(url: String?): Connection = Jsoup.connect(url).cookies(BypassUtil.getMapCookie(App.context)).userAgent(BypassUtil.userAgent).timeout(PrefsUtil.timeoutTime.toInt() * 1000)

fun okHttpCookies(url: String, method: String = "GET"): Request = Request.Builder().apply {
    url(url)
    method(method, if (method == "POST") RequestBody.create(MediaType.get("text/plain"), "") else null)
    header("User-Agent", BypassUtil.userAgent)
    header("Cookie", BypassUtil.getStringCookie(App.context))
}.build()

const val SDK_INT_Q = 1000000

fun isHostValid(hostName: String): Boolean {
    if (BuildConfig.DEBUG)
        Log.e("Hostname", hostName)
    return when (hostName) {
        "fex.net",
        "api.crashlytics.com",
        "e.crashlytics.com",
        "sdk-android.ad.smaato.net",
        "cdn.myanimelist.net",
        "myanimelist.cdn-dena.com",
        "settings.crashlytics.com",
        "somoskudasai.com",
        "animeflv.net",
        "github.com",
        "raw.githubusercontent.com",
        "cdn.animeflv.net",
        "m.animeflv.net",
        "s1.animeflv.net",
        "streamango.com",
        "ok.ru",
        "www.rapidvideo.com",
        "www.yourupload.com" -> true
        else -> isVideoHostName(hostName)
    }
}

private fun isVideoHostName(hostName: String): Boolean {
    return when {
        hostName.contains("google.com") -> true
        hostName.contains("fex.net") ||
                hostName.contains("content-na.drive.amazonaws.com") ||
                hostName.contains("mediafire") ||
                hostName.contains("fruithosted.net") ||
                hostName.contains("mp4upload.com") ||
                hostName.contains("storage.googleapis.com") ||
                hostName.contains("playercdn.net") ||
                hostName.contains("vidcache.net") ||
                hostName.contains("fembed.com") ||
                hostName.contains("leasewebcdn.me") ||
                hostName.contains("fvs.io") -> true
        else -> false
    }
}