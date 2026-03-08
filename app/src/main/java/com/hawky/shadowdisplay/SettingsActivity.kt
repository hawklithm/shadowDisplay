package com.hawky.shadowdisplay

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
        val rbId = when (currentSettings.triggerMode) {
            TriggerMode.CHARGING_ONLY -> R.id.rb_charging_only
            TriggerMode.ALWAYS -> R.id.rb_always
        }
        findViewById<RadioGroup>(R.id.rg_trigger_mode).check(rbId)
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
