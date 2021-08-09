package io.agora.rtc.base

import ai.deepar.ar.*
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.media.Image
import android.util.Size
import android.view.SurfaceView
import android.widget.FrameLayout
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import io.agora.rtc.RtcChannel
import io.agora.rtc.RtcEngine
import io.agora.rtc.video.VideoCanvas
import java.lang.ref.WeakReference
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import java.util.concurrent.ExecutionException


class DeepArSurfaceView(
  context: Context
) : FrameLayout(context), AREventListener {
  private var surface: SurfaceView
  private var canvas: VideoCanvas
  private var isMediaOverlay = false
  private var onTop = false
  private var deepAR: DeepAR? = null
  private var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>? = null
  private var channel: WeakReference<RtcChannel>? = null
  private val currentMask = 0
  var masks: ArrayList<String>? = null
  private var buffers: Array<ByteBuffer?> = arrayOf<ByteBuffer?>()
  private val NUMBER_OF_BUFFERS = 2
  private var currentBuffer = 0
  private val defaultLensFacing = CameraSelector.LENS_FACING_FRONT
  private val lensFacing: Int = defaultLensFacing

  init {
    try {
      surface = RtcEngine.CreateRendererView(context)
    } catch (e: UnsatisfiedLinkError) {
      throw RuntimeException("Please init RtcEngine first!")
    }
    canvas = VideoCanvas(surface)
    addView(surface)
  }

  fun setZOrderMediaOverlay(isMediaOverlay: Boolean) {
    this.isMediaOverlay = isMediaOverlay
    try {
      removeView(surface)
      surface.setZOrderMediaOverlay(isMediaOverlay)
      addView(surface)
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  override fun screenshotTaken(bitmap: Bitmap?) {}

  override fun videoRecordingStarted() {}

  override fun videoRecordingFinished() {}

  override fun videoRecordingFailed() {}

  override fun videoRecordingPrepared() {}

  override fun shutdownFinished() {}

  override fun faceVisibilityChanged(b: Boolean) {}

  override fun imageVisibilityChanged(s: String?, b: Boolean) {}

  override fun frameAvailable(image: Image?) {}

  override fun error(arErrorType: ARErrorType?, s: String?) {}

  override fun effectSwitched(s: String?) {}

  override fun initialized() {
    deepAR!!.switchEffect("mask", "file:///android_asset/beauty_without_deform")
  }

  public fun initializeDeepAR() {
    deepAR = DeepAR(context)
    deepAR?.setLicenseKey("24c2bce175ea21aeb640c46a4ee2e385f4f1c54912e759e5dd723470bc91d3180c9c8cad3dcf8de8")
    deepAR?.initialize(context, this)
    initializeFilters()
  }

  private fun initializeFilters() {
    masks = ArrayList()
    masks!!.add("none")
    masks!!.add("beauty_without_deform")
    masks!!.add("beauty_without_eyelashes")
  }


  fun setZOrderOnTop(onTop: Boolean) {
    this.onTop = onTop
    try {
      removeView(surface)
      surface.setZOrderOnTop(onTop)
      addView(surface)
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  fun setData(engine: RtcEngine, channel: RtcChannel?, uid: Number) {
    this.channel = if (channel != null) WeakReference(channel) else null
    canvas.channelId = this.channel?.get()?.channelId()
    canvas.uid = uid.toNativeUInt()
    setupVideoCanvas(engine)
  }

  fun resetVideoCanvas(engine: RtcEngine) {
    val canvas =
      VideoCanvas(null, canvas.renderMode, canvas.channelId, canvas.uid, canvas.mirrorMode)
    if (canvas.uid == 0) {
      engine.setupLocalVideo(canvas)
    } else {
      engine.setupRemoteVideo(canvas)
    }
  }

  private fun setupVideoCanvas(engine: RtcEngine) {
    removeAllViews()
    surface = RtcEngine.CreateRendererView(context.applicationContext)
    surface.setZOrderMediaOverlay(isMediaOverlay)
    surface.setZOrderOnTop(onTop)
    addView(surface)
    surface.layout(0, 0, width, height)
    canvas.view = surface
    if (canvas.uid == 0) {
      engine.setupLocalVideo(canvas)
    } else {
      engine.setupRemoteVideo(canvas)
    }
  }

  fun setRenderMode(engine: RtcEngine, @Annotations.AgoraVideoRenderMode renderMode: Int) {
    canvas.renderMode = renderMode
    setupRenderMode(engine)
  }

  fun setMirrorMode(engine: RtcEngine, @Annotations.AgoraVideoMirrorMode mirrorMode: Int) {
    canvas.mirrorMode = mirrorMode
    setupRenderMode(engine)
  }

  fun setDeepArLicenseKey(key: String) {
    //  canvas.mirrorMode = mirrorMode
    //  setupRenderMode(engine)
  }

  private fun setupRenderMode(engine: RtcEngine) {
    if (canvas.uid == 0) {
      engine.setLocalRenderMode(canvas.renderMode, canvas.mirrorMode)
    } else {
      channel?.get()?.let {
        it.setRemoteRenderMode(canvas.uid, canvas.renderMode, canvas.mirrorMode)
        return@setupRenderMode
      }
      engine.setRemoteRenderMode(canvas.uid, canvas.renderMode, canvas.mirrorMode)
    }
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    val width: Int = MeasureSpec.getSize(widthMeasureSpec)
    val height: Int = MeasureSpec.getSize(heightMeasureSpec)
    surface.layout(0, 0, width, height)
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
  }

  public fun setupCamera() {
    cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    cameraProviderFuture!!.addListener(object : Runnable {
      override fun run() {
        try {
          val cameraProvider: ProcessCameraProvider = cameraProviderFuture!!.get()
          bindImageAnalysis(cameraProvider)
        } catch (e: ExecutionException) {
          e.printStackTrace()
        } catch (e: InterruptedException) {
          e.printStackTrace()
        }
      }
    }, ContextCompat.getMainExecutor(context))
  }

  private fun bindImageAnalysis(cameraProvider: ProcessCameraProvider) {
    val cameraPreset: CameraResolutionPreset = CameraResolutionPreset.P640x480
    val width: Int
    val height: Int
    val orientation: Int =0 // getScreenOrientation()
    if (orientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE || orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
      width = cameraPreset.getWidth()
      height = cameraPreset.getHeight()
    } else {
      width = cameraPreset.getHeight()
      height = cameraPreset.getWidth()
    }
    buffers = arrayOfNulls(NUMBER_OF_BUFFERS)
    for (i in 0 until NUMBER_OF_BUFFERS) {
      buffers[i] = ByteBuffer.allocateDirect(width * height * 3)
      buffers[i]!!.order(ByteOrder.nativeOrder())
      buffers[i]!!.position(0)
    }
    val imageAnalysis: ImageAnalysis = ImageAnalysis.Builder().setTargetResolution(Size(width, height)).setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
    imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context), object : ImageAnalysis.Analyzer {
      override fun analyze(image: ImageProxy) {
        //image.getImageInfo().getTimestamp();
        val byteData: ByteArray
        val yBuffer: ByteBuffer = image.getPlanes()[0].getBuffer()
        val uBuffer: ByteBuffer = image.getPlanes()[1].getBuffer()
        val vBuffer: ByteBuffer = image.getPlanes()[2].getBuffer()
        val ySize: Int = yBuffer.remaining()
        val uSize: Int = uBuffer.remaining()
        val vSize: Int = vBuffer.remaining()
        byteData = ByteArray(ySize + uSize + vSize)

        //U and V are swapped
        yBuffer.get(byteData, 0, ySize)
        vBuffer.get(byteData, ySize, vSize)
        uBuffer.get(byteData, ySize + vSize, uSize)
        buffers[currentBuffer]!!.put(byteData)
        buffers[currentBuffer]!!.position(0)
        if (deepAR != null) {
         // ByteBuffer buffer, int width, int height, int orientation, boolean mirror
         // ByteBuffer buffer, int width, int height, int orientation, boolean mirror, long timestamp
          deepAR!!.receiveFrame(
            buffers[currentBuffer],
            image.width,
            image.height,
            image.imageInfo.rotationDegrees,
            lensFacing == CameraSelector.LENS_FACING_FRONT,
            DeepARImageFormat.YUV_420_888,
            image.planes[1].pixelStride
          )
        }
        currentBuffer = (currentBuffer + 1) % NUMBER_OF_BUFFERS
        image.close()
      }
    })
    val cameraSelector: CameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
    cameraProvider.unbindAll()
    cameraProvider.bindToLifecycle((this as LifecycleOwner?)!!, cameraSelector, imageAnalysis)
  }

//  private fun getScreenOrientation(): Int {
//    val rotation: Int = getWindowManager().getDefaultDisplay().getRotation()
//    val dm: DisplayMetrics = DisplayMetrics()
//    getWindowManager().getDefaultDisplay().getMetrics(dm)
//    val width: Int = dm.widthPixels
//    val height: Int = dm.heightPixels
//    val orientation: Int
//    // if the device's natural orientation is portrait:
//    if (((rotation == Surface.ROTATION_0
//        || rotation == Surface.ROTATION_180)) && height > width ||
//      ((rotation == Surface.ROTATION_90
//        || rotation == Surface.ROTATION_270)) && width > height) {
//      when (rotation) {
//        Surface.ROTATION_0 -> orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
//        Surface.ROTATION_90 -> orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
//        Surface.ROTATION_180 -> orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
//        Surface.ROTATION_270 -> orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
//        else -> {
//          Log.e(MainActivity.TAG, "Unknown screen orientation. Defaulting to " +
//            "portrait.")
//          orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
//        }
//      }
//    } else {
//      when (rotation) {
//        Surface.ROTATION_0 -> orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
//        Surface.ROTATION_90 -> orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
//        Surface.ROTATION_180 -> orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
//        Surface.ROTATION_270 -> orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
//        else -> {
//          Log.e(MainActivity.TAG, "Unknown screen orientation. Defaulting to " +
//            "landscape.")
//          orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
//        }
//      }
//    }
//    return orientation
//  }
}
