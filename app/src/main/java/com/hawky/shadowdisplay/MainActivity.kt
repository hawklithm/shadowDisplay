package com.hawky.shadowdisplay

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.hawky.shadowdisplay.models.DisplayMode
import com.hawky.shadowdisplay.models.TriggerMode
import com.hawky.shadowdisplay.permission.PermissionManager
import com.hawky.shadowdisplay.settings.SettingsManager

/**
 * 主Activity - 趣味屏保
 */
class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_DREAM_SETTINGS = 1001
        private const val PREF_LAUNCHED_FROM_PREVIEW = "launched_from_preview"
    }

    private lateinit var permissionManager: PermissionManager
    private lateinit var settings: SettingsManager
    private var launchedFromPreview = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 隐藏系统标题栏（ActionBar）和状态栏（StatusBar）
        supportActionBar?.hide()

        // 隐藏状态栏
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let {
                it.hide(android.view.WindowInsets.Type.statusBars())
                it.systemBarsBehavior = android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )
            @Suppress("DEPRECATION")
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }

        setContentView(R.layout.activity_main)

        // 初始化依赖
        permissionManager = PermissionManager.getInstance()
        settings = SettingsManager.getInstance(this)

        // 读取跳转来源标记（必须在settings初始化之后）
        launchedFromPreview = getSharedPreferences(TAG, Context.MODE_PRIVATE)
            .getBoolean(PREF_LAUNCHED_FROM_PREVIEW, false)

        // 如果是从系统屏保返回的，并且不是充电状态，显示全屏息屏
        if (launchedFromPreview) {
            Log.d(TAG, "从系统屏保返回，检查是否需要显示全屏息屏")
            getSharedPreferences(TAG, Context.MODE_PRIVATE).edit()
                .putBoolean(PREF_LAUNCHED_FROM_PREVIEW, false)
                .apply()
            launchedFromPreview = false

            if (!isCharging()) {
                Log.d(TAG, "未充电状态，启动全屏息屏显示")
                startDisplayActivity()
                finish()
                return
            }
        }

        setupUI()
        loadSettings()
    }

    override fun onResume() {
        super.onResume()
        loadSettings()
    }

    private fun setupUI() {
        // 立即启动屏保
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_preview).setOnClickListener {
            startDreamPreview()
        }

        // 屏保模式选择
        val rgDisplayMode = findViewById<RadioGroup>(R.id.rg_display_mode)
        rgDisplayMode.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                R.id.rb_digital_clock -> DisplayMode.DIGITAL_CLOCK
                R.id.rb_analog_clock -> DisplayMode.ANALOG_CLOCK
                R.id.rb_flip_clock -> DisplayMode.FLIP_CLOCK
                R.id.rb_wall_e_eyes -> DisplayMode.ROBOT_EYES_WALL_E
                R.id.rb_minion_eyes -> DisplayMode.ROBOT_EYES_MINION
                else -> DisplayMode.DIGITAL_CLOCK
            }
            settings.updateDisplayMode(mode)
            Log.d(TAG, "屏保模式已切换: ${mode.displayName}")
            Toast.makeText(this, "已切换到${mode.displayName}", Toast.LENGTH_SHORT).show()
        }

        // 打开屏保设置
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_open_dream_settings).setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        // 电池优化设置
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_battery_optimization).setOnClickListener {
            requestBatteryOptimization()
        }

        // 使用教程
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_tutorial).setOnClickListener {
            showTutorial()
        }
    }

    /**
     * 加载当前设置
     */
    private fun loadSettings() {
        val currentSettings = settings.getSettings()

        // 加载屏保模式
        val displayModeRbId = when (currentSettings.displayMode) {
            DisplayMode.DIGITAL_CLOCK -> R.id.rb_digital_clock
            DisplayMode.ANALOG_CLOCK -> R.id.rb_analog_clock
            DisplayMode.FLIP_CLOCK -> R.id.rb_flip_clock
            DisplayMode.ROBOT_EYES_WALL_E -> R.id.rb_wall_e_eyes
            DisplayMode.ROBOT_EYES_MINION -> R.id.rb_minion_eyes
        }
        findViewById<RadioGroup>(R.id.rg_display_mode).check(displayModeRbId)
    }

    /**
     * 启动屏保预览
     */
    private fun startDreamPreview() {
        Log.d(TAG, "启动屏保预览，充电状态: ${isCharging()}")

        // 标记是从立即启动按钮跳转的
        getSharedPreferences(TAG, Context.MODE_PRIVATE).edit()
            .putBoolean(PREF_LAUNCHED_FROM_PREVIEW, true)
            .apply()

        val currentSettings = settings.getSettings()

        // 如果是充电状态，跳转到系统屏保页面
        if (isCharging()) {
            try {
                val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    Intent(Settings.ACTION_DREAM_SETTINGS)
                } else {
                    null
                }

                intent?.let {
                    startActivityForResult(it, REQUEST_DREAM_SETTINGS)
                    Log.d(TAG, "已打开屏保设置，请选择\"趣味屏保\"进行预览")
                    Toast.makeText(this, "请选择\"趣味屏保\"进行预览", Toast.LENGTH_LONG).show()
                } ?: run {
                    Log.e(TAG, "当前系统版本不支持屏保功能")
                    Toast.makeText(this, "当前系统版本不支持屏保功能", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "启动屏保预览失败", e)
                Toast.makeText(this, "启动失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            // 未充电状态，直接启动全屏息屏显示
            Log.d(TAG, "未充电状态，启动全屏息屏显示")
            startDisplayActivity()
        }
    }

    /**
     * 启动全屏息屏显示
     */
    private fun startDisplayActivity() {
        val currentSettings = settings.getSettings()
        DisplayActivity.start(this, currentSettings.displayMode)
    }

    /**
     * 检查是否正在充电
     */
    private fun isCharging(): Boolean {
        val batteryStatus: Intent? = registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )

        val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
               status == BatteryManager.BATTERY_STATUS_FULL
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_DREAM_SETTINGS -> {
                Log.d(TAG, "屏保设置页面返回")
            }
            PermissionManager.REQUEST_OVERLAY_PERMISSION,
            PermissionManager.REQUEST_BATTERY_OPTIMIZATION -> {
                Log.d(TAG, "权限请求回调: requestCode=$requestCode, resultCode=$resultCode")
            }
        }
    }

    /**
     * 显示使用教程
     */
    private fun showTutorial() {
        Toast.makeText(this, "使用教程功能开发中", Toast.LENGTH_SHORT).show()
    }

    /**
     * 请求电池优化豁免
     */
    private fun requestBatteryOptimization() {
        Log.d(TAG, "请求电池优化豁免")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = android.net.Uri.parse("package:$packageName")
                }
                startActivity(intent)
                Log.d(TAG, "已打开电池优化设置页面")
            } catch (e: Exception) {
                Log.e(TAG, "打开电池优化设置失败", e)
                Toast.makeText(this, "打开设置失败", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "当前系统版本不支持此功能", Toast.LENGTH_SHORT).show()
        }
    }
}
