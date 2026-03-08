package com.hawky.shadowdisplay

import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.hawky.shadowdisplay.models.TriggerMode
import com.hawky.shadowdisplay.settings.SettingsManager

/**
 * 屏保设置页面
 */
class SettingsActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SettingsActivity"
    }

    private lateinit var settings: SettingsManager

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

        setContentView(R.layout.activity_settings)

        settings = SettingsManager.getInstance(this)

        setupUI()
        loadSettings()
    }

    override fun onResume() {
        super.onResume()
        loadSettings()
    }

    private fun setupUI() {
        // 屏保触发模式选择
        val rgTriggerMode = findViewById<RadioGroup>(R.id.rg_trigger_mode)
        rgTriggerMode.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                R.id.rb_charging_only -> TriggerMode.CHARGING_ONLY
                R.id.rb_always -> TriggerMode.ALWAYS
                else -> TriggerMode.CHARGING_ONLY
            }
            settings.updateTriggerMode(mode)
            Log.d(TAG, "触发模式已切换: ${mode.displayName}")
            Toast.makeText(this, "已切换到${mode.displayName}", Toast.LENGTH_SHORT).show()
        }

        // 自动旋转开关
        val switchAutoRotation = findViewById<SwitchCompat>(R.id.switch_auto_rotation)
        switchAutoRotation.setOnCheckedChangeListener { _, isChecked ->
            settings.updateAutoRotation(isChecked)
            Log.d(TAG, "自动旋转已${if (isChecked) "开启" else "关闭"}")
            Toast.makeText(this, "自动旋转已${if (isChecked) "开启" else "关闭"}", Toast.LENGTH_SHORT).show()
        }

        // 自动亮度开关
        val switchAutoBrightness = findViewById<SwitchCompat>(R.id.switch_auto_brightness)
        switchAutoBrightness.setOnCheckedChangeListener { _, isChecked ->
            settings.updateAutoBrightness(isChecked)
            Log.d(TAG, "自动亮度已${if (isChecked) "开启" else "关闭"}")
            Toast.makeText(this, "自动亮度已${if (isChecked) "开启" else "关闭"}", Toast.LENGTH_SHORT).show()
        }

        // 烧屏保护开关
        val switchBurnInProtection = findViewById<SwitchCompat>(R.id.switch_burn_in_protection)
        switchBurnInProtection.setOnCheckedChangeListener { _, isChecked ->
            settings.updateBurnInProtection(isChecked)
            Log.d(TAG, "烧屏保护已${if (isChecked) "开启" else "关闭"}")
            Toast.makeText(this, "烧屏保护已${if (isChecked) "开启" else "关闭"}", Toast.LENGTH_SHORT).show()
        }

        // 打开系统屏保设置
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_open_system_settings).setOnClickListener {
            openDreamSettings()
        }

        // 返回按钮
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_back).setOnClickListener {
            finish()
        }
    }

    /**
     * 加载当前设置
     */
    private fun loadSettings() {
        val currentSettings = settings.getSettings()

        // 加载触发模式
        val triggerModeRbId = when (currentSettings.triggerMode) {
            TriggerMode.CHARGING_ONLY -> R.id.rb_charging_only
            TriggerMode.ALWAYS -> R.id.rb_always
        }
        findViewById<RadioGroup>(R.id.rg_trigger_mode).check(triggerModeRbId)

        // 加载自动旋转设置
        findViewById<SwitchCompat>(R.id.switch_auto_rotation).isChecked = currentSettings.autoRotation

        // 加载自动亮度设置
        findViewById<SwitchCompat>(R.id.switch_auto_brightness).isChecked = currentSettings.autoBrightness

        // 加载烧屏保护设置
        findViewById<SwitchCompat>(R.id.switch_burn_in_protection).isChecked = currentSettings.burnInProtection
    }

    /**
     * 打开系统屏保设置
     */
    private fun openDreamSettings() {
        Log.d(TAG, "打开系统屏保设置")

        try {
            val dreamIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Intent(Settings.ACTION_DREAM_SETTINGS)
            } else {
                null
            }

            dreamIntent?.let {
                Log.d(TAG, "成功打开屏保设置页面")
                Toast.makeText(this, "请在设置中启用\"趣味屏保\"", Toast.LENGTH_LONG).show()
                startActivity(it)
            } ?: run {
                Log.e(TAG, "当前系统版本不支持屏保功能")
                Toast.makeText(this, "当前系统版本不支持屏保功能", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            Log.e(TAG, "打开屏保设置失败", e)
            Toast.makeText(this, "打开设置失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
