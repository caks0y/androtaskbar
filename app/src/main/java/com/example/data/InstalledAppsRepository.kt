package com.example.data

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.model.FreeformWindowPreset
import com.example.model.InstalledApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class InstalledAppsRepository(private val context: Context) {

    private val iconCache = ConcurrentHashMap<String, ImageBitmap>()
    private var launchCascadeCounter = 0

    suspend fun loadLaunchableApps(customLabels: Map<String, String>): List<InstalledApp> =
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }

            val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentActivities(
                    mainIntent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(mainIntent, 0)
            }

            val ownPackage = context.packageName

            resolved
                .asSequence()
                .filter { it.activityInfo.packageName != ownPackage }
                .distinctBy { it.activityInfo.packageName }
                .map { resolveInfo ->
                    val pkg = resolveInfo.activityInfo.packageName
                    val activityName = resolveInfo.activityInfo.name
                    val label = resolveInfo.loadLabel(pm)?.toString().orEmpty().ifBlank { pkg }
                    val isSystem = (resolveInfo.activityInfo.applicationInfo.flags and
                        ApplicationInfo.FLAG_SYSTEM) != 0

                    val cachedIcon = iconCache[pkg] ?: run {
                        val drawable = runCatching { resolveInfo.loadIcon(pm) }.getOrNull()
                        drawable?.let { drawableToImageBitmap(it, 128) }?.also {
                            iconCache[pkg] = it
                        }
                    }

                    InstalledApp(
                        packageName = pkg,
                        activityName = activityName,
                        originalLabel = label,
                        customLabel = customLabels[pkg],
                        icon = cachedIcon,
                        isSystemApp = isSystem
                    )
                }
                .sortedBy { it.displayLabel.lowercase() }
                .toList()
        }

    fun getAppDrawable(packageName: String): Drawable? =
        runCatching { context.packageManager.getApplicationIcon(packageName) }.getOrNull()

    fun launchApp(
        packageName: String,
        activityName: String? = null,
        freeform: Boolean,
        preset: FreeformWindowPreset = FreeformWindowPreset.TABLET_WIDE
    ): Boolean {
        val pm = context.packageManager
        val intent = if (!activityName.isNullOrBlank()) {
            Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                component = ComponentName(packageName, activityName)
            }
        } else {
            pm.getLaunchIntentForPackage(packageName)
        } ?: return false

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)

        if (!freeform) {
            return runCatching {
                context.startActivity(intent)
                true
            }.getOrDefault(false)
        }

        val options = buildFreeformActivityOptions(preset)
        return runCatching {
            if (options != null) {
                context.startActivity(intent, options.toBundle())
            } else {
                context.startActivity(intent)
            }
            true
        }.getOrElse {
            runCatching {
                context.startActivity(intent)
                true
            }.getOrDefault(false)
        }
    }

    fun openAppInfo(packageName: String) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    @SuppressLint("PrivateApi")
    private fun buildFreeformActivityOptions(preset: FreeformWindowPreset): ActivityOptions? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return null

        val metrics = context.resources.displayMetrics
        val screenW = metrics.widthPixels.coerceAtLeast(800)
        val screenH = metrics.heightPixels.coerceAtLeast(600)

        val (targetW, targetH) = when (preset) {
            FreeformWindowPreset.TABLET_WIDE -> (screenW * 0.62f).toInt() to (screenH * 0.68f).toInt()
            FreeformWindowPreset.TABLET_COMPACT -> (screenW * 0.44f).toInt() to (screenH * 0.62f).toInt()
            FreeformWindowPreset.PHONE_PORTRAIT -> (screenW * 0.32f).toInt().coerceAtLeast(480) to
                (screenH * 0.78f).toInt()
        }

        val cascadeOffset = (launchCascadeCounter % 5) * (36 * metrics.density).toInt()
        launchCascadeCounter++

        val maxLeft = (screenW - targetW).coerceAtLeast(0)
        val maxTop = (screenH - targetH - (56 * metrics.density).toInt()).coerceAtLeast(0)

        val left = (((screenW - targetW) / 2) + cascadeOffset - (48 * metrics.density).toInt())
            .coerceIn(0, maxLeft)
        val top = (((screenH - targetH) / 2) + cascadeOffset - (48 * metrics.density).toInt())
            .coerceIn(0, maxTop)

        val bounds = Rect(left, top, left + targetW, top + targetH)

        return runCatching {
            val options = ActivityOptions.makeBasic()
            options.setLaunchBounds(bounds)
            // WINDOWING_MODE_FREEFORM = 5 in AOSP / HarmonyOS framework
            runCatching {
                val method = ActivityOptions::class.java.getMethod(
                    "setLaunchWindowingMode",
                    Int::class.javaPrimitiveType
                )
                method.invoke(options, 5)
            }
            options
        }.getOrNull()
    }

    private fun drawableToImageBitmap(drawable: Drawable, targetSizePx: Int): ImageBitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            val src = drawable.bitmap
            if (src.width <= targetSizePx && src.height <= targetSizePx) {
                return src.asImageBitmap()
            }
        }
        val width = drawable.intrinsicWidth.takeIf { it in 1..targetSizePx } ?: targetSizePx
        val height = drawable.intrinsicHeight.takeIf { it in 1..targetSizePx } ?: targetSizePx
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap.asImageBitmap()
    }
}
