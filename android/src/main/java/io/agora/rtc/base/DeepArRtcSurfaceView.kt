package io.agora.rtc.base

import ai.deepar.ar.*
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.Color
import android.media.Image
import android.opengl.GLSurfaceView
import android.os.Handler
import android.util.DisplayMetrics
import android.util.Log
import android.util.Size
import android.view.Surface
import android.widget.FrameLayout
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import io.agora.agora_rtc_engine.DeepARRenderer
import io.agora.rtc.RtcChannel
import io.agora.rtc.RtcEngine
import java.lang.ref.WeakReference
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ExecutionException

class DeepArRtcSurfaceView(
  context: Context, mRtcEngine: RtcEngine,activity: Activity
) : FrameLayout(context), AREventListener {
  private var surface: GLSurfaceView
  private var newActivity: Activity =activity;

  // private var canvas: VideoCanvas
  private var isMediaOverlay = false
  private var onTop = false
  private var channel: WeakReference<RtcChannel>? = null
  private var deepAR: DeepAR? = null
  private var renderer: DeepARRenderer? = null

  private val defaultLensFacing = CameraSelector.LENS_FACING_FRONT
  private val lensFacing = defaultLensFacing
  private var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>? = null
  private var buffers: Array<ByteBuffer?>? = null
  private var currentBuffer = 0
  private val NUMBER_OF_BUFFERS = 2

  init {
   // setBackgroundColor(Color.MAGENTA);
    deepAR = DeepAR(context)
    deepAR?.setLicenseKey("3eb6f6dedffd858320f3744f2f7fad629b4cd1d7479a64214d197a3a402ac1e96f531ee3a0d2634b")
    deepAR?.initialize(context, this)

    Handler().postDelayed({
      setupCamera();
    }, 5000)

    surface = GLSurfaceView(context)
    surface.setEGLContextClientVersion(2)
    surface.setEGLConfigChooser(8, 8, 8, 8, 16, 0)
    renderer = DeepARRenderer(deepAR, mRtcEngine)

    surface.setEGLContextFactory(DeepARRenderer.MyContextFactory(renderer))

    surface.setRenderer(renderer)
    surface.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY)




//    try {
//      surface = GLSurfaceView(context)
//    } catch (e: UnsatisfiedLinkError) {
//      throw RuntimeException("Please init RtcEngine first!")
//    }
//    surface.setBackgroundColor(Color.LTGRAY)
//   // canvas = VideoCanvas(surface)
    addView(surface)
  }


  private fun setupCamera() {
    cameraProviderFuture = ProcessCameraProvider.getInstance(newActivity)
    cameraProviderFuture!!.addListener(Runnable {
      try {
        val cameraProvider = cameraProviderFuture!!.get()
        //cameraProvider.list
        bindImageAnalysis(cameraProvider)
      } catch (e: ExecutionException) {
        e.printStackTrace()
      } catch (e: InterruptedException) {
        e.printStackTrace()
      }
    }, ContextCompat.getMainExecutor(newActivity))
  }

  private fun bindImageAnalysis(cameraProvider: ProcessCameraProvider) {
    val cameraPreset = CameraResolutionPreset.P640x480
    val width: Int
    val height: Int
    val orientation: Int = getScreenOrientation()
    if (orientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE || orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
      width = cameraPreset.width
      height = cameraPreset.height
    } else {
      width = cameraPreset.height
      height = cameraPreset.width
    }
    buffers = arrayOfNulls(NUMBER_OF_BUFFERS)
    for (i in 0 until NUMBER_OF_BUFFERS) {
      buffers!![i] = ByteBuffer.allocateDirect(width * height * 3)
      buffers!![i]!!.order(ByteOrder.nativeOrder())
      buffers!![i]!!.position(0)
    }
    val imageAnalysis = ImageAnalysis.Builder().setTargetResolution(Size(width, height))
      .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
    imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(newActivity),
      ImageAnalysis.Analyzer { image -> //image.getImageInfo().getTimestamp();
        val byteData: ByteArray
        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer
        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()
        byteData = ByteArray(ySize + uSize + vSize)

        //U and V are swapped
        yBuffer[byteData, 0, ySize]
        vBuffer[byteData, ySize, vSize]
        uBuffer[byteData, ySize + vSize, uSize]
        buffers!![currentBuffer]!!.put(byteData)
        buffers!![currentBuffer]!!.position(0)
        if (deepAR != null) {
          deepAR!!.receiveFrame(
            buffers!![currentBuffer],
            image.width, image.height,
            image.imageInfo.rotationDegrees,
            lensFacing == CameraSelector.LENS_FACING_FRONT,
            DeepARImageFormat.YUV_420_888,
            image.planes[1].pixelStride
          )
        }
        currentBuffer = (currentBuffer + 1) % NUMBER_OF_BUFFERS
        image.close()
      })
    val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
    cameraProvider.unbindAll()
    cameraProvider.bindToLifecycle((newActivity as LifecycleOwner), cameraSelector, imageAnalysis)
  }

  private fun getScreenOrientation(): Int {
//    val rotation: Int = getWindowManager().getDefaultDisplay().getRotation()
//    val dm = DisplayMetrics()
//    getWindowManager().getDefaultDisplay().getMetrics(dm)
//    val width = dm.widthPixels
//    val height = dm.heightPixels
//    val orientation: Int
//    // if the device's natural orientation is portrait:
//    orientation = if ((rotation == Surface.ROTATION_0
//        || rotation == Surface.ROTATION_180) && height > width ||
//      (rotation == Surface.ROTATION_90
//        || rotation == Surface.ROTATION_270) && width > height
//    ) {
//      when (rotation) {
//        Surface.ROTATION_0 -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
//        Surface.ROTATION_90 -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
//        Surface.ROTATION_180 -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
//        Surface.ROTATION_270 -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
//        else -> {
//          Log.e(
//            "ghjhj", "Unknown screen orientation. Defaulting to " +
//              "portrait."
//          )
//          ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
//        }
//      }
//    } else {
//      when (rotation) {
//        Surface.ROTATION_0 -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
//        Surface.ROTATION_90 -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
//        Surface.ROTATION_180 -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
//        Surface.ROTATION_270 -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
//        else -> {
//          Log.e(
//            "ghjhj", "Unknown screen orientation. Defaulting to " +
//              "landscape."
//          )
//          ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
//        }
//      }
//    }
    return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT //orientation
  }


  fun setZOrderMediaOverlay(isMediaOverlay: Boolean) {
//    this.isMediaOverlay = isMediaOverlay
//    try {
//      removeView(surface)
//      surface.setZOrderMediaOverlay(isMediaOverlay)
//      addView(surface)
//    } catch (e: Exception) {
//      e.printStackTrace()
//    }
  }

  fun setZOrderOnTop(onTop: Boolean) {
//    this.onTop = onTop
//    try {
//      removeView(surface)
//      surface.setZOrderOnTop(onTop)
//      addView(surface)
//    } catch (e: Exception) {
//      e.printStackTrace()
//    }
  }

  fun setData(engine: RtcEngine, channel: RtcChannel?, uid: Number) {
//    this.channel = if (channel != null) WeakReference(channel) else null
//    canvas.channelId = this.channel?.get()?.channelId()
//    canvas.uid = uid.toNativeUInt()
//    setupVideoCanvas(engine)
  }

  fun resetVideoCanvas(engine: RtcEngine) {
//    val canvas =
//      VideoCanvas(null, canvas.renderMode, canvas.channelId, canvas.uid, canvas.mirrorMode)
//    if (canvas.uid == 0) {
//      engine.setupLocalVideo(canvas)
//    } else {
//      engine.setupRemoteVideo(canvas)
//    }
  }

  private fun setupVideoCanvas(engine: RtcEngine) {
//    removeAllViews()
//    surface = RtcEngine.CreateRendererView(context.applicationContext)
//    surface.setZOrderMediaOverlay(isMediaOverlay)
//    surface.setZOrderOnTop(onTop)
//    addView(surface)
//    surface.layout(0, 0, width, height)
//    canvas.view = surface
//    if (canvas.uid == 0) {
//      engine.setupLocalVideo(canvas)
//    } else {
//      engine.setupRemoteVideo(canvas)
//    }
  }

  fun setRenderMode(engine: RtcEngine, @Annotations.AgoraVideoRenderMode renderMode: Int) {
//    canvas.renderMode = renderMode
//    setupRenderMode(engine)
  }

  fun setMirrorMode(engine: RtcEngine, @Annotations.AgoraVideoMirrorMode mirrorMode: Int) {
//    canvas.mirrorMode = mirrorMode
//    setupRenderMode(engine)
  }

  private fun setupRenderMode(engine: RtcEngine) {
//    if (canvas.uid == 0) {
//      engine.setLocalRenderMode(canvas.renderMode, canvas.mirrorMode)
//    } else {
//      channel?.get()?.let {
//        it.setRemoteRenderMode(canvas.uid, canvas.renderMode, canvas.mirrorMode)
//        return@setupRenderMode
//      }
//      engine.setRemoteRenderMode(canvas.uid, canvas.renderMode, canvas.mirrorMode)
//    }
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    val width: Int = MeasureSpec.getSize(widthMeasureSpec)
    val height: Int = MeasureSpec.getSize(heightMeasureSpec)
    surface.layout(0, 0, width, height)
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
  }

  override fun screenshotTaken(p0: Bitmap?) {
    TODO("Not yet implemented")
  }

  override fun videoRecordingStarted() {
    TODO("Not yet implemented")
  }

  override fun videoRecordingFinished() {
    TODO("Not yet implemented")
  }

  override fun videoRecordingFailed() {
    TODO("Not yet implemented")
  }

  override fun videoRecordingPrepared() {
    TODO("Not yet implemented")
  }

  override fun shutdownFinished() {
    //TODO("Not yet implemented")
  }

  override fun initialized() {
    //TODO("Not yet implemented")
  }

  override fun faceVisibilityChanged(p0: Boolean) {
    TODO("Not yet implemented")
  }

  override fun imageVisibilityChanged(p0: String?, p1: Boolean) {
    TODO("Not yet implemented")
  }

  override fun frameAvailable(p0: Image?) {
    TODO("Not yet implemented")
  }

  override fun error(p0: ARErrorType?, p1: String?) {
    TODO("Not yet implemented")
  }

  override fun effectSwitched(p0: String?) {
    TODO("Not yet implemented")
  }
}
