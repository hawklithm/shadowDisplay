package com.hawky.shadowdisplay.views

import android.content.Context
import android.graphics.*
import android.util.Log
import android.view.TextureView
import java.text.SimpleDateFormat
import java.util.*

/**
 * 方案E：使用 TextureView 渲染
 * TextureView 使用硬件加速，但不同于普通 View 的渲染管道
 */
class LockScreenTextureView(context: Context) : TextureView(context) {

    companion object {
        private const val TAG = "LockScreenTextureView"
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    init {
        setSurfaceTextureListener(object : SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                Log.d(TAG, "SurfaceTexture 可用: ${width}x${height}")
                startRendering(surface, width, height)
            }

            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
                Log.d(TAG, "SurfaceTexture 尺寸变化: ${width}x${height}")
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                Log.d(TAG, "SurfaceTexture 已销毁")
                return true
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                // SurfaceTexture 更新时
            }
        })
    }

    private fun startRendering(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
        val surface = android.view.Surface(surfaceTexture)
        val renderThread = Thread {
            try {
                val canvas = surface.lockCanvas(null) as Canvas
                try {
                    drawContent(canvas, width, height)
                    surface.unlockCanvasAndPost(canvas)
                } catch (e: Exception) {
                    Log.e(TAG, "渲染失败", e)
                }
            } catch (e: Exception) {
                Log.e(TAG, "获取 Canvas 失败", e)
            }
            surface.release()
        }
        renderThread.start()
    }

    private fun drawContent(canvas: Canvas, width: Int, height: Int) {
        val centerX = width / 2f
        val centerY = height / 2f

        // 清空画布
        canvas.drawColor(Color.BLACK)

        // 绘制时间
        val paint = Paint().apply {
            color = Color.WHITE
            textSize = 120f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val time = timeFormat.format(Date())
        canvas.drawText(time, centerX, centerY, paint)

        // 绘制日期
        paint.textSize = 40f
        paint.color = Color.GRAY
        val date = dateFormat.format(Date())
        canvas.drawText(date, centerX, centerY + 80, paint)

        Log.d(TAG, "内容已绘制: $time $date")
    }
}
