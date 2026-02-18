package com.example.rtc

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.nio.ByteBuffer

class MainActivity : AppCompatActivity() {

    private val TAG = "ScreenCapture"
    private val REQUEST_CODE_SCREEN_CAPTURE = 1001

    private lateinit var mediaProjectionManager: MediaProjectionManager
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    private lateinit var captureBtn: Button
    private lateinit var resultImageView: ImageView
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        captureBtn = findViewById(R.id.btn_capture)
        resultImageView = findViewById(R.id.iv_result)

        // 1. 获取 MediaProjectionManager 系统服务
        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        captureBtn.setOnClickListener {
            startScreenCapture()
        }
    }

    /**
     * 发起屏幕捕获权限请求
     */
    private fun startScreenCapture() {
        // createScreenCaptureIntent() 会返回一个用于请求用户授权的 Intent [citation:1][citation:4]
        val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
        startActivityForResult(captureIntent, REQUEST_CODE_SCREEN_CAPTURE)
    }

    /**
     * 处理用户授权结果
     */
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SCREEN_CAPTURE) {
            if (resultCode == Activity.RESULT_OK) {
                Log.d(TAG, "用户授权成功")
                // 2. 用户授权成功，获取 MediaProjection 对象 [citation:4][citation:10]
                mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data!!)
                // 3. 开始准备并执行屏幕捕获
                setupAndCaptureScreen()
            } else {
                Log.e(TAG, "用户拒绝授权")
                Toast.makeText(this, "需要屏幕录制权限才能截图", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 配置 ImageReader 和 VirtualDisplay 并执行一次截图
     */
    private fun setupAndCaptureScreen() {
        // 获取屏幕的宽高和密度
        val metrics = resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val densityDpi = metrics.densityDpi

        // 4. 创建 ImageReader，用于接收屏幕的原始图像数据 [citation:10]
        //    参数：宽、高、像素格式（RGBA）、最大图像数（这里设为2，提高容错）
        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)

        // 5. 设置 OnImageAvailableListener，当有新图像时会回调
        imageReader?.setOnImageAvailableListener({ reader ->
            // 注意：这个回调在非UI线程，需要切换到主线程处理UI
            mainHandler.post {
                captureAndDisplayImage(reader)
            }
        }, null)

        // 6. 创建 VirtualDisplay，将 MediaProjection 和 ImageReader 的 Surface 连接起来 [citation:1][citation:10]
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCaptureDisplay", // 名称
            width, // 宽度
            height, // 高度
            densityDpi, // 像素密度 DPI
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, // 标志位，自动镜像屏幕内容
            imageReader?.surface, // 关键：将数据输出到 ImageReader 的 Surface
            null, // 可选回调
            null  // 可选 Handler
        )

        Log.d(TAG, "VirtualDisplay 创建成功，准备截图...")
    }

    /**
     * 从 ImageReader 中获取最新的一帧并显示为 Bitmap
     */
    private fun captureAndDisplayImage(reader: ImageReader) {
        var image: Image? = null
        try {
            // 获取最新的图像 [citation:10]
            image = reader.acquireLatestImage()
            if (image == null) {
                Log.e(TAG, "获取到的图像为空")
                return
            }

            Log.d(TAG, "成功获取图像，格式: ${image.format}, 大小: ${image.width}x${image.height}")

            // 将 Image 转换为 Bitmap
            val bitmap = imageToBitmap(image)
            if (bitmap != null) {
                // 显示 Bitmap
                resultImageView.setImageBitmap(bitmap)
                Toast.makeText(this, "截图成功", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            Log.e(TAG, "处理图像时出错: ${e.message}")
        } finally {
            // 重要：必须关闭 Image 对象以释放资源，否则会阻塞 ImageReader 获取新图像 [citation:10]
            image?.close()
        }
    }

    /**
     * 将 Image (RGBA格式) 转换为 Bitmap
     * 这里处理了因内存对齐可能导致的宽高不一致问题 [citation:10]
     */
    private fun imageToBitmap(image: Image): Bitmap? {
        // 对于 RGBA_8888 格式，planes 数组中第一个元素包含了所有像素数据
        val planes = image.planes
        val buffer: ByteBuffer = planes[0].buffer

        val width = image.width
        val height = image.height
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride

        // 计算因内存对齐产生的额外宽度偏移量 [citation:10]
        val rowPadding = rowStride - pixelStride * width

        // 创建 Bitmap。如果存在内存对齐，实际需要的宽度会比 width 略大
        // 公式: width + rowPadding / pixelStride 得到包含填充的实际像素宽度
        val bitmap = if (rowPadding == 0) {
            // 大多数情况内存是对齐的，直接创建
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        } else {
            // 处理内存未对齐的情况
            Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888)
        }

        // 将 buffer 中的数据拷贝到 bitmap 中
        bitmap.copyPixelsFromBuffer(buffer)
        return bitmap
    }

    /**
     * 释放资源
     */
    private fun releaseResources() {
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.close()
        imageReader = null
        mediaProjection?.stop() // 停止投影，释放相关资源 [citation:10]
        mediaProjection = null
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseResources()
    }
}