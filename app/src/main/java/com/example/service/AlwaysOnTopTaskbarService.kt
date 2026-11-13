package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.GridLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextClock
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.content.res.ResourcesCompat
import com.example.MainActivity
import com.example.R
import com.example.data.DesktopPreferencesRepository
import com.example.data.InstalledAppsRepository
import com.example.model.DesktopPreferences
import com.example.model.InstalledApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlin.math.abs

class AlwaysOnTopTaskbarService : Service() {

    companion object {
        const val ACTION_START = "com.example.action.START_OVERLAY_TASKBAR"
        const val ACTION_STOP = "com.example.action.STOP_OVERLAY_TASKBAR"
        const val EXTRA_OPEN_ACTION = "com.example.extra.OPEN_ACTION"
        const val OPEN_ACTION_TASK_SWITCHER = "task_switcher"
        const val OPEN_ACTION_THIS_PC = "this_pc"
        const val OPEN_ACTION_BROWSER = "browser"
        const val OPEN_ACTION_SETTINGS = "settings"

        private const val CHANNEL_ID = "winpad_always_on_top_channel"
        private const val NOTIFICATION_ID = 4102

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

        private val _isDesktopInForeground = MutableStateFlow(false)
        val isDesktopInForeground: StateFlow<Boolean> = _isDesktopInForeground.asStateFlow()

        private val _isOverlayVisible = MutableStateFlow(false)
        val isOverlayVisible: StateFlow<Boolean> = _isOverlayVisible.asStateFlow()

        fun setDesktopForeground(inForeground: Boolean) {
            _isDesktopInForeground.value = inForeground
        }

        fun start(context: Context) {
            val intent = Intent(context, AlwaysOnTopTaskbarService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, AlwaysOnTopTaskbarService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var windowManager: WindowManager
    private lateinit var prefsRepo: DesktopPreferencesRepository
    private lateinit var appsRepo: InstalledAppsRepository

    private var taskbarView: View? = null
    private var drawerView: View? = null
    private var collapsedX = 0
    private var collapsedY = -1
    private var drawerParams: WindowManager.LayoutParams? = null

    private var currentPrefs = DesktopPreferences()
    private var cachedApps: List<InstalledApp> = emptyList()
    private var isCollapsed = false
    private var isDrawerOpen = false
    private var observerJob: Job? = null
    private val robotoTypeface by lazy {
        runCatching { ResourcesCompat.getFont(this, R.font.roboto) }.getOrNull()
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        prefsRepo = DesktopPreferencesRepository(applicationContext)
        appsRepo = InstalledAppsRepository(applicationContext)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            serviceScope.launch {
                prefsRepo.setAlwaysOnTopOverlayEnabled(false)
                stopSelf()
            }
            return START_NOT_STICKY
        }

        if (!Settings.canDrawOverlays(this)) {
            serviceScope.launch {
                prefsRepo.setAlwaysOnTopOverlayEnabled(false)
                stopSelf()
            }
            return START_NOT_STICKY
        }

        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        _isServiceRunning.value = true

        if (observerJob == null) {
            observerJob = serviceScope.launch {
                cachedApps = appsRepo.loadLaunchableApps(emptyMap())
                var initializedCollapse = false
                combine(prefsRepo.preferencesFlow, _isDesktopInForeground) { prefs, inForeground ->
                    prefs to inForeground
                }.collectLatest { (prefs, inForeground) ->
                    currentPrefs = prefs
                    if (!initializedCollapse) {
                        initializedCollapse = true
                        isCollapsed = prefs.overlayAutoCollapse
                    }
                    if (!prefs.alwaysOnTopOverlayEnabled) {
                        stopSelf()
                        return@collectLatest
                    }
                    if (inForeground) {
                        // Main bottom TaskbarBar in MainActivity is active: hide overlay to avoid duplicate taskbars
                        isDrawerOpen = false
                        removeDrawerView()
                        removeTaskbarView()
                        return@collectLatest
                    }
                    cachedApps = appsRepo.loadLaunchableApps(prefs.customLabels)
                    renderTaskbarOverlay()
                    if (isDrawerOpen) {
                        renderQuickDrawerOverlay()
                    }
                }
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        _isServiceRunning.value = false
        _isOverlayVisible.value = false
        removeDrawerView()
        removeTaskbarView()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun renderTaskbarOverlay() {
        if (!Settings.canDrawOverlays(this) || _isDesktopInForeground.value) return

        removeTaskbarView()

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val blurSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            windowManager.isCrossWindowBlurEnabled
        @Suppress("DEPRECATION")
        val blurFlag = if (blurSupported) WindowManager.LayoutParams.FLAG_BLUR_BEHIND else 0

        val params = if (isCollapsed) {
            if (collapsedY < 0) collapsedY = dp(16)
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or blurFlag,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                x = collapsedX
                y = collapsedY
                if (blurSupported && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    blurBehindRadius = dp(24)
                }
            }
        } else {
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                dp(48),
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or blurFlag,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.FILL_HORIZONTAL
                x = 0
                y = 0
                if (blurSupported && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    blurBehindRadius = dp(24)
                }
            }
        }

        val root = if (isCollapsed) buildCollapsedPill(params) else buildExpandedBar()
        taskbarView = root
        val added = runCatching { windowManager.addView(root, params) }.isSuccess
        _isOverlayVisible.value = added
    }

    private fun buildCollapsedPill(params: WindowManager.LayoutParams): View {
        val isDark = currentPrefs.darkTheme
        val alphaByte = (currentPrefs.overlayOpacity * 255).toInt().coerceIn(115, 255)
        val bgColor = if (isDark) Color.argb(alphaByte, 20, 26, 38) else Color.argb(alphaByte, 243, 247, 255)
        val borderColor = if (isDark) Color.argb(95, 96, 205, 255) else Color.argb(95, 0, 120, 212)
        val textColor = if (isDark) Color.WHITE else Color.parseColor("#0F172A")

        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(20).toFloat()
                setColor(bgColor)
                setStroke(dp(1), borderColor)
            }
            elevation = dp(10).toFloat()

            addView(ImageView(context).apply {
                setImageResource(R.drawable.ic_win11_start)
                layoutParams = LinearLayout.LayoutParams(dp(20), dp(20)).apply {
                    marginEnd = dp(8)
                }
            })

            addView(TextView(context).apply {
                text = "Start"
                setTextColor(textColor)
                typeface = robotoTypeface
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
                paint.isFakeBoldText = true
            })

            attachDragAndTap(this, params) {
                isCollapsed = false
                renderTaskbarOverlay()
            }
        }
    }

    private fun buildExpandedBar(): View {
        val isDark = currentPrefs.darkTheme
        val blurSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            windowManager.isCrossWindowBlurEnabled
        val maxAlpha = if (blurSupported) 222f else 248f
        val alphaByte = (currentPrefs.overlayOpacity * maxAlpha).toInt().coerceIn(135, 255)
        val bgColor = if (isDark) Color.argb(alphaByte, 18, 24, 36) else Color.argb(alphaByte, 240, 245, 252)
        val borderColor = if (isDark) Color.argb(82, 255, 255, 255) else Color.argb(56, 15, 23, 42)
        val textColor = if (isDark) Color.parseColor("#F3F6FC") else Color.parseColor("#0F172A")
        val subTextColor = if (isDark) Color.parseColor("#A9B6CB") else Color.parseColor("#475569")
        val activeSlotBg = if (isDark) Color.argb(46, 255, 255, 255) else Color.argb(28, 15, 23, 42)
        val badgeBg = if (isDark) Color.argb(30, 255, 255, 255) else Color.argb(18, 15, 23, 42)
        val accentColor = if (isDark) Color.parseColor("#60CDFF") else Color.parseColor("#0078D4")

        // Root vertical stack: crisp 1dp Windows 11 top hairline + 47dp flush full-width taskbar row
        val rootStack = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(48)
            )
            setBackgroundColor(bgColor)
            elevation = dp(16).toFloat()
        }

        rootStack.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(1)
            )
            setBackgroundColor(borderColor)
        })

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            setPadding(dp(10), 0, dp(10), 0)
        }
        rootStack.addView(container)

        // Left balancing area when centered (matches TaskbarBar)
        if (currentPrefs.taskbarCentered) {
            container.addView(View(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
            })
        }

        // Authentic Windows 11 4-Pane Start Button (borderless flat style + bottom active bar)
        val startBtn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dp(38), dp(38))
            background = GradientDrawable().apply {
                cornerRadius = dp(6).toFloat()
                setColor(if (isDrawerOpen) activeSlotBg else Color.TRANSPARENT)
            }
            addView(ImageView(context).apply {
                setImageResource(R.drawable.ic_win11_start)
                layoutParams = LinearLayout.LayoutParams(dp(22), dp(22))
            })
            if (isDrawerOpen) {
                addView(View(context).apply {
                    layoutParams = LinearLayout.LayoutParams(dp(14), dp(2)).apply {
                        topMargin = dp(3)
                    }
                    background = GradientDrawable().apply {
                        cornerRadius = dp(2).toFloat()
                        setColor(accentColor)
                    }
                })
            }
            setOnClickListener {
                isDrawerOpen = !isDrawerOpen
                if (isDrawerOpen) renderQuickDrawerOverlay() else removeDrawerView()
                renderTaskbarOverlay()
            }
        }
        container.addView(startBtn)

        // Task View / Alt+Tab Switcher Button
        val desktopBtn = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dp(38), dp(38)).apply {
                marginStart = dp(2)
            }
            background = GradientDrawable().apply {
                cornerRadius = dp(6).toFloat()
                setColor(Color.TRANSPARENT)
            }
            addView(ImageView(context).apply {
                setImageResource(R.drawable.ic_taskbar_taskview)
                layoutParams = LinearLayout.LayoutParams(dp(20), dp(20))
            })
            setOnClickListener {
                openDesktopAction(OPEN_ACTION_TASK_SWITCHER)
            }
        }
        container.addView(desktopBtn)

        // Windows 11 Explorer Shortcut Button
        val explorerBtn = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dp(38), dp(38)).apply {
                marginStart = dp(2)
            }
            background = GradientDrawable().apply {
                cornerRadius = dp(6).toFloat()
                setColor(Color.TRANSPARENT)
            }
            addView(ImageView(context).apply {
                setImageResource(R.drawable.ic_taskbar_explorer)
                layoutParams = LinearLayout.LayoutParams(dp(21), dp(21))
            })
            setOnClickListener {
                openDesktopAction(OPEN_ACTION_THIS_PC)
            }
        }
        container.addView(explorerBtn)

        // Windows 11 Built-in Browser Shortcut Button
        val browserBtn = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dp(38), dp(38)).apply {
                marginStart = dp(2)
            }
            background = GradientDrawable().apply {
                cornerRadius = dp(6).toFloat()
                setColor(Color.TRANSPARENT)
            }
            addView(ImageView(context).apply {
                setImageResource(R.drawable.ic_win11_browser)
                layoutParams = LinearLayout.LayoutParams(dp(21), dp(21))
            })
            setOnClickListener {
                openDesktopAction(OPEN_ACTION_BROWSER)
            }
        }
        container.addView(browserBtn)

        // Windows 11 Settings Shortcut Button
        val settingsBtn = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dp(38), dp(38)).apply {
                marginStart = dp(2)
            }
            background = GradientDrawable().apply {
                cornerRadius = dp(6).toFloat()
                setColor(Color.TRANSPARENT)
            }
            addView(ImageView(context).apply {
                setImageResource(R.drawable.ic_win11_settings)
                layoutParams = LinearLayout.LayoutParams(dp(21), dp(21))
            })
            setOnClickListener {
                openDesktopAction(OPEN_ACTION_SETTINGS)
            }
        }
        container.addView(settingsBtn)

        // Pinned Apps + Separated Recent Apps Tray Strip
        val (pinnedOverlayApps, recentOverlayApps) = resolveOverlayAppGroups()
        val scrollStrip = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            layoutParams = if (currentPrefs.taskbarCentered) {
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginStart = dp(4)
                    marginEnd = dp(4)
                }
            } else {
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = dp(4)
                    marginEnd = dp(4)
                }
            }
        }
        val iconsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        fun buildOverlayAppButton(app: InstalledApp, isRecent: Boolean, isMostRecent: Boolean): View? {
            val drawable = appsRepo.getAppDrawable(app.packageName) ?: return null
            return LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(dp(38), dp(38)).apply {
                    marginEnd = dp(2)
                }
                background = GradientDrawable().apply {
                    cornerRadius = dp(6).toFloat()
                    setColor(if (isRecent && isMostRecent) activeSlotBg else Color.TRANSPARENT)
                }
                addView(ImageView(context).apply {
                    setImageDrawable(drawable)
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    contentDescription = app.displayLabel
                    layoutParams = LinearLayout.LayoutParams(dp(22), dp(22))
                })
                if (isRecent) {
                    addView(View(context).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            dp(if (isMostRecent) 12 else 5),
                            dp(2)
                        ).apply {
                            topMargin = dp(3)
                        }
                        background = GradientDrawable().apply {
                            cornerRadius = dp(2).toFloat()
                            setColor(if (isMostRecent) accentColor else subTextColor)
                        }
                    })
                }
                setOnClickListener {
                    removeDrawerView()
                    isDrawerOpen = false
                    appsRepo.launchApp(
                        packageName = app.packageName,
                        activityName = app.activityName,
                        freeform = currentPrefs.freeformLaunchEnabled,
                        preset = currentPrefs.freeformPreset
                    )
                    serviceScope.launch { prefsRepo.recordRecentApp(app.packageName) }
                }
            }
        }

        pinnedOverlayApps.forEach { app ->
            buildOverlayAppButton(app, isRecent = false, isMostRecent = false)?.let { iconsRow.addView(it) }
        }

        if (recentOverlayApps.isNotEmpty()) {
            iconsRow.addView(View(this).apply {
                layoutParams = LinearLayout.LayoutParams(dp(1), dp(20)).apply {
                    marginStart = dp(4)
                    marginEnd = dp(5)
                }
                setBackgroundColor(borderColor)
            })
            recentOverlayApps.forEachIndexed { idx, app ->
                buildOverlayAppButton(app, isRecent = true, isMostRecent = idx == 0)?.let {
                    iconsRow.addView(it)
                }
            }
        }
        scrollStrip.addView(iconsRow)
        container.addView(scrollStrip)

        // Right system tray container (fills remaining space when centered)
        val rightTray = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            layoutParams = if (currentPrefs.taskbarCentered) {
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            } else {
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
        }

        // Freeform Window Mode Toggle Badge
        val modeChip = TextView(this).apply {
            text = if (currentPrefs.freeformLaunchEnabled) "WIN" else "FULL"
            setTextColor(if (currentPrefs.freeformLaunchEnabled) accentColor else textColor)
            typeface = robotoTypeface
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f)
            paint.isFakeBoldText = true
            setPadding(dp(8), dp(6), dp(8), dp(6))
            background = GradientDrawable().apply {
                cornerRadius = dp(6).toFloat()
                setColor(
                    if (currentPrefs.freeformLaunchEnabled) accentColor.withAlpha(36) else badgeBg
                )
            }
            setOnClickListener {
                serviceScope.launch {
                    prefsRepo.setFreeformLaunchEnabled(!currentPrefs.freeformLaunchEnabled)
                }
            }
        }
        rightTray.addView(modeChip)

        // Stacked Windows 11 Clock (HH:mm + dd.MM)
        val clockStack = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            setPadding(dp(8), 0, dp(6), 0)
            addView(TextClock(context).apply {
                format24Hour = "HH:mm"
                format12Hour = "h:mm"
                setTextColor(textColor)
                typeface = robotoTypeface
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
                paint.isFakeBoldText = true
            })
            addView(TextClock(context).apply {
                format24Hour = "dd.MM"
                format12Hour = "MM/dd"
                setTextColor(subTextColor)
                typeface = robotoTypeface
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f)
            })
        }
        rightTray.addView(clockStack)

        // Collapse to Pill Button
        val collapseBtn = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            setPadding(dp(6), dp(6), dp(6), dp(6))
            layoutParams = LinearLayout.LayoutParams(dp(30), dp(30))
            background = GradientDrawable().apply {
                cornerRadius = dp(6).toFloat()
                setColor(Color.TRANSPARENT)
            }
            addView(ImageView(context).apply {
                setImageResource(R.drawable.ic_taskbar_collapse)
                setColorFilter(textColor)
                layoutParams = LinearLayout.LayoutParams(dp(16), dp(16))
            })
            setOnClickListener {
                removeDrawerView()
                isDrawerOpen = false
                isCollapsed = true
                renderTaskbarOverlay()
            }
        }
        rightTray.addView(collapseBtn)
        container.addView(rightTray)

        return rootStack
    }

    private fun openDesktopAction(action: String) {
        removeDrawerView()
        isDrawerOpen = false
        val homeIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(EXTRA_OPEN_ACTION, action)
        }
        startActivity(homeIntent)
    }

    private fun renderQuickDrawerOverlay() {
        if (!Settings.canDrawOverlays(this)) return
        removeDrawerView()

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val isDark = currentPrefs.darkTheme
        val blurSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            windowManager.isCrossWindowBlurEnabled
        val drawerAlpha = if (blurSupported) 212 else 246
        val bgColor = if (isDark) Color.argb(drawerAlpha, 22, 30, 46) else Color.argb(drawerAlpha + 6, 246, 250, 255)
        val borderColor = if (isDark) Color.argb(92, 255, 255, 255) else Color.argb(56, 15, 23, 42)
        val textColor = if (isDark) Color.parseColor("#F3F6FC") else Color.parseColor("#0F172A")
        val hintColor = if (isDark) Color.parseColor("#94A3B8") else Color.parseColor("#64748B")
        val inputBg = if (isDark) Color.argb(46, 255, 255, 255) else Color.argb(24, 15, 23, 42)
        val accentColor = if (isDark) Color.parseColor("#60CDFF") else Color.parseColor("#0078D4")

        val drawerRoot = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = GradientDrawable().apply {
                cornerRadius = dp(18).toFloat()
                setColor(bgColor)
                setStroke(dp(1), borderColor)
            }
            elevation = dp(20).toFloat()
            isFocusableInTouchMode = true
        }

        // Start Menu Header
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(10))
        }
        header.addView(ImageView(this).apply {
            setImageResource(R.drawable.ic_win11_start)
            layoutParams = LinearLayout.LayoutParams(dp(20), dp(20)).apply {
                marginEnd = dp(8)
            }
        })
        header.addView(TextView(this).apply {
            text = if (currentPrefs.language.code == "tr") "Başlat Menüsü" else "Start Menu"
            setTextColor(textColor)
            typeface = robotoTypeface
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
            paint.isFakeBoldText = true
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        header.addView(TextView(this).apply {
            text = "✕"
            setTextColor(textColor)
            typeface = robotoTypeface
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setPadding(dp(10), dp(4), dp(6), dp(4))
            setOnClickListener {
                isDrawerOpen = false
                removeDrawerView()
                renderTaskbarOverlay()
            }
        })
        drawerRoot.addView(header)

        // Search Bar
        val searchBox = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(6), dp(10), dp(6))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(42)
            ).apply {
                bottomMargin = dp(12)
            }
            background = GradientDrawable().apply {
                cornerRadius = dp(21).toFloat()
                setColor(inputBg)
                setStroke(dp(1), accentColor.withAlpha(110))
            }
        }

        val searchInput = EditText(this).apply {
            hint = if (currentPrefs.language.code == "tr") {
                "Yüklü uygulamalarda ara..."
            } else {
                "Search installed apps..."
            }
            setHintTextColor(hintColor)
            setTextColor(textColor)
            typeface = robotoTypeface
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            background = null
            isSingleLine = true
            setPadding(0, 0, dp(8), 0)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val clearBtn = TextView(this).apply {
            text = "✕"
            setTextColor(hintColor)
            typeface = robotoTypeface
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            setPadding(dp(6), dp(4), dp(4), dp(4))
            visibility = View.GONE
            setOnClickListener { searchInput.setText("") }
        }
        searchBox.addView(searchInput)
        searchBox.addView(clearBtn)
        drawerRoot.addView(searchBox)

        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(356), dp(290))
        }
        val grid = GridLayout(this).apply {
            columnCount = 4
        }

        fun populateGrid(query: String) {
            grid.removeAllViews()
            val filtered = cachedApps
                .filterNot { currentPrefs.hiddenPackages.contains(it.packageName) }
                .filter {
                    query.isBlank() ||
                        it.displayLabel.contains(query, ignoreCase = true) ||
                        it.packageName.contains(query, ignoreCase = true)
                }

            if (filtered.isEmpty()) {
                grid.addView(TextView(this).apply {
                    text = if (currentPrefs.language.code == "tr") {
                        "Eşleşen uygulama bulunamadı"
                    } else {
                        "No matching apps found"
                    }
                    setTextColor(hintColor)
                    typeface = robotoTypeface
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
                    setPadding(dp(12), dp(24), dp(12), dp(24))
                })
                return
            }

            filtered.forEach { app ->
                val item = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER_HORIZONTAL
                    setPadding(dp(6), dp(8), dp(6), dp(8))
                    layoutParams = GridLayout.LayoutParams().apply {
                        width = dp(86)
                        height = LinearLayout.LayoutParams.WRAP_CONTENT
                    }
                    setOnClickListener {
                        isDrawerOpen = false
                        removeDrawerView()
                        renderTaskbarOverlay()
                        appsRepo.launchApp(
                            packageName = app.packageName,
                            activityName = app.activityName,
                            freeform = currentPrefs.freeformLaunchEnabled,
                            preset = currentPrefs.freeformPreset
                        )
                        serviceScope.launch { prefsRepo.recordRecentApp(app.packageName) }
                    }
                }
                val drawable = appsRepo.getAppDrawable(app.packageName)
                if (drawable != null) {
                    item.addView(ImageView(this).apply {
                        setImageDrawable(drawable)
                        layoutParams = LinearLayout.LayoutParams(dp(38), dp(38))
                    })
                }
                item.addView(TextView(this).apply {
                    text = app.displayLabel
                    setTextColor(textColor)
                    typeface = robotoTypeface
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f)
                    maxLines = 1
                    ellipsize = TextUtils.TruncateAt.END
                    gravity = Gravity.CENTER
                    setPadding(0, dp(5), 0, 0)
                })
                grid.addView(item)
            }
        }

        populateGrid("")

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val q = s?.toString().orEmpty()
                clearBtn.visibility = if (q.isEmpty()) View.GONE else View.VISIBLE
                populateGrid(q)
            }

            override fun afterTextChanged(s: Editable?) = Unit
        })

        scroll.addView(grid)
        drawerRoot.addView(scroll)

        // Windroid Start Menu Footer Strip
        val footerDivider = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(1)
            ).apply {
                topMargin = dp(8)
                bottomMargin = dp(8)
            }
            setBackgroundColor(borderColor)
        }
        drawerRoot.addView(footerDivider)

        val footer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(4), dp(2), dp(4), 0)
        }
        footer.addView(ImageView(this).apply {
            setImageResource(R.drawable.img_winpad_icon)
            layoutParams = LinearLayout.LayoutParams(dp(22), dp(22)).apply {
                marginEnd = dp(8)
            }
        })
        footer.addView(TextView(this).apply {
            text = getString(R.string.app_name)
            setTextColor(textColor)
            typeface = robotoTypeface
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            paint.isFakeBoldText = true
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        footer.addView(TextView(this).apply {
            text = if (currentPrefs.language.code == "tr") "Masaüstü" else "Desktop"
            setTextColor(accentColor)
            typeface = robotoTypeface
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            paint.isFakeBoldText = true
            setPadding(dp(8), dp(4), dp(4), dp(4))
            setOnClickListener {
                isDrawerOpen = false
                removeDrawerView()
                renderTaskbarOverlay()
                val homeIntent = Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                startActivity(homeIntent)
            }
        })
        drawerRoot.addView(footer)

        drawerRoot.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_OUTSIDE) {
                isDrawerOpen = false
                removeDrawerView()
                renderTaskbarOverlay()
                true
            } else {
                false
            }
        }

        drawerRoot.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                isDrawerOpen = false
                removeDrawerView()
                renderTaskbarOverlay()
                true
            } else {
                false
            }
        }

        @Suppress("DEPRECATION")
        val blurFlag = if (blurSupported) WindowManager.LayoutParams.FLAG_BLUR_BEHIND else 0
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                blurFlag,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            x = 0
            y = dp(56)
            if (blurSupported && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                blurBehindRadius = dp(30)
            }
        }

        drawerParams = params
        drawerView = drawerRoot
        runCatching { windowManager.addView(drawerRoot, params) }
    }

    private fun resolveOverlayAppGroups(): Pair<List<InstalledApp>, List<InstalledApp>> {
        val byPkg = cachedApps.associateBy { it.packageName }
        val hidden = currentPrefs.hiddenPackages
        val pinned = currentPrefs.pinnedTaskbarPackages
            .filterNot { hidden.contains(it) }
            .mapNotNull { byPkg[it] }
            .ifEmpty { cachedApps.filterNot { hidden.contains(it.packageName) }.take(4) }
            .take(5)
        val pinnedSet = pinned.map { it.packageName }.toSet()

        val allRecents = currentPrefs.recentPackages
            .filterNot { hidden.contains(it) }
            .mapNotNull { byPkg[it] }
        val unpinnedRecents = allRecents.filterNot { pinnedSet.contains(it.packageName) }
        val recents = (unpinnedRecents.ifEmpty { allRecents }).take(4)
        return pinned to recents
    }

    private fun attachDragAndTap(
        view: View,
        params: WindowManager.LayoutParams,
        onTap: () -> Unit
    ) {
        var initialX = 0
        var initialY = 0
        var touchStartX = 0f
        var touchStartY = 0f
        var moved = false

        view.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    touchStartX = event.rawX
                    touchStartY = event.rawY
                    moved = false
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - touchStartX).toInt()
                    val dy = (event.rawY - touchStartY).toInt()
                    if (abs(dx) > dp(6) || abs(dy) > dp(6)) {
                        moved = true
                    }
                    params.x = initialX + dx
                    // Gravity.BOTTOM uses y as upward offset from bottom
                    params.y = (initialY - dy).coerceAtLeast(dp(4))
                    collapsedX = params.x
                    collapsedY = params.y
                    runCatching { windowManager.updateViewLayout(taskbarView, params) }
                    true
                }

                MotionEvent.ACTION_UP -> {
                    if (!moved) {
                        onTap()
                    }
                    true
                }

                else -> false
            }
        }
    }

    private fun removeTaskbarView() {
        taskbarView?.let { view ->
            runCatching { windowManager.removeView(view) }
        }
        taskbarView = null
        _isOverlayVisible.value = false
    }

    private fun removeDrawerView() {
        drawerView?.let { view ->
            runCatching { windowManager.removeView(view) }
        }
        drawerView = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.overlay_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.overlay_notification_channel_desc)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openDesktopIntent = PendingIntent.getActivity(
            this,
            101,
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopOverlayIntent = PendingIntent.getService(
            this,
            102,
            Intent(this, AlwaysOnTopTaskbarService::class.java).apply {
                action = ACTION_STOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.overlay_notification_title))
            .setContentText(getString(R.string.overlay_notification_text))
            .setOngoing(true)
            .setContentIntent(openDesktopIntent)
            .addAction(0, getString(R.string.overlay_action_open_desktop), openDesktopIntent)
            .addAction(0, getString(R.string.overlay_action_stop), stopOverlayIntent)
            .build()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun Int.withAlpha(alpha: Int): Int =
        Color.argb(alpha, Color.red(this), Color.green(this), Color.blue(this))
}
