package io.agora.rtc.base;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.ActivityInfo;
import android.opengl.GLSurfaceView;
import android.util.Size;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;

import android.graphics.Canvas;

import com.google.common.util.concurrent.ListenableFuture;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ExecutionException;

import ai.deepar.ar.CameraResolutionPreset;
import ai.deepar.ar.DeepAR;
import ai.deepar.ar.DeepARImageFormat;
import io.agora.agora_rtc_engine.AgoraSurfaceView;
import io.agora.rtc.RtcEngine;
import io.flutter.plugin.platform.PlatformView;

public class DeepArCameraView extends GLSurfaceView implements LifecycleOwner{

  private LifecycleRegistry lifecycleRegistry;
  private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
  private Context context;
  private AgoraSurfaceView parentView;
  private ByteBuffer[] buffers;
  private int currentBuffer = 0;
  private static final int NUMBER_OF_BUFFERS=2;
  private int defaultLensFacing = CameraSelector.LENS_FACING_FRONT;
  private int lensFacing = defaultLensFacing;
  //private GLSurfaceView surfaceView;
  private DeepARRenderer renderer;
  private DeepAR deepAR;

  public DeepArCameraView(@NonNull Context context, AgoraSurfaceView parentView, RtcEngine engine) {
    super(context);
    this.context = context;
    this.parentView = parentView;
    setWillNotDraw(false);
    lifecycleRegistry = new LifecycleRegistry(this);
    lifecycleRegistry.setCurrentState(Lifecycle.State.CREATED);
    lifecycleRegistry.setCurrentState(Lifecycle.State.STARTED);

    deepAR = new DeepAR(context);
    deepAR.setLicenseKey("24c2bce175ea21aeb640c46a4ee2e385f4f1c54912e759e5dd723470bc91d3180c9c8cad3dcf8de8");
    deepAR.initialize(context, parentView);

    //surfaceView = new GLSurfaceView(context);
    this.setEGLContextClientVersion(2);
    this.setEGLConfigChooser(8,8,8,8,16,0);
    renderer = new DeepARRenderer(deepAR, engine);
    this.setEGLContextFactory(new DeepARRenderer.MyContextFactory(renderer));
    this.setRenderer(renderer);
    this.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    //this.addView(surfaceView);
  }

//  @Override
//  protected void onDraw(Canvas canvas){
//    super.onDraw(canvas);
//    //canvas.drawColor(0xffff0000);
//    //canvas.drawBitmap(aliens, x, y, null);
//  }

  public void setup() {
    setupCamera();

    //this.addView(surfaceView);

    //engine.setExternalVideoSource(true, true, true);

    //final Button btn = findViewById(R.id.startCall);
    //mRtcEngine.setExternalVideoSource(true, true, true);
//    btn.setOnClickListener(new View.OnClickListener() {
//      @Override
//      public void onClick(View v) {
//        if (callInProgress) {
//          callInProgress = false;
//          renderer.setCallInProgress(false);
//          mRtcEngine.leaveChannel();
//          onRemoteUserLeft();
//          btn.setText("Start the call");
//        } else {
//          callInProgress = true;
//          joinChannel();
//          btn.setText("End the call");
//        }
//      }
//    });

  }

  public void setupCamera() {
    cameraProviderFuture = ProcessCameraProvider.getInstance(context);
    cameraProviderFuture.addListener(new Runnable() {
      @Override
      public void run() {
        try {
          ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
          bindImageAnalysis(cameraProvider);
        } catch (ExecutionException | InterruptedException e) {
          e.printStackTrace();
        }
      }
    }, ContextCompat.getMainExecutor(context));
  }


  private void bindImageAnalysis(@NonNull ProcessCameraProvider cameraProvider) {
    CameraResolutionPreset cameraPreset = CameraResolutionPreset.P640x480;
    int width;
    int height;
    int orientation = ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT;
    if (orientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE || orientation ==ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE){
      width = cameraPreset.getWidth();
      height =  cameraPreset.getHeight();
    } else {
      width = cameraPreset.getHeight();
      height = cameraPreset.getWidth();
    }
    height = 100;
    width = 100;
    buffers = new ByteBuffer[NUMBER_OF_BUFFERS];
    for (int i = 0; i < NUMBER_OF_BUFFERS; i++) {
      buffers[i] = ByteBuffer.allocateDirect(width * height * 3);
      buffers[i].order(ByteOrder.nativeOrder());
      buffers[i].position(0);
    }

    ImageAnalysis imageAnalysis = new ImageAnalysis.Builder().setTargetResolution(new Size(width, height)).setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();
    imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context), new ImageAnalysis.Analyzer() {
      @Override
      public void analyze(@NonNull ImageProxy image) {
        //image.getImageInfo().getTimestamp();
        byte[] byteData;
        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
        ByteBuffer uBuffer = image.getPlanes()[1].getBuffer();
        ByteBuffer vBuffer = image.getPlanes()[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byteData = new byte[ySize + uSize + vSize];

        //U and V are swapped
        yBuffer.get(byteData, 0, ySize);
        vBuffer.get(byteData, ySize, vSize);
        uBuffer.get(byteData, ySize + vSize, uSize);

        buffers[currentBuffer].put(byteData);
        buffers[currentBuffer].position(0);
        if(deepAR != null) {
          deepAR.receiveFrame(buffers[currentBuffer],
            image.getWidth(), image.getHeight(),
            image.getImageInfo().getRotationDegrees(),
            lensFacing == CameraSelector.LENS_FACING_FRONT,
            DeepARImageFormat.YUV_420_888,
            image.getPlanes()[1].getPixelStride()
          );
        }
        currentBuffer = ( currentBuffer + 1 ) % NUMBER_OF_BUFFERS;
        image.close();
      }
    });

    CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(lensFacing).build();
    cameraProvider.unbindAll();
    cameraProvider.bindToLifecycle((LifecycleOwner)this, cameraSelector, imageAnalysis);
  }


  private LifecycleOwner getLifecycleOwner(Context context)
  {
    if (context == null)
    {
      return null;
    }
    else if (context instanceof ContextWrapper)
    {
      if (context instanceof LifecycleOwner)
      {
        return (LifecycleOwner) context;
      }
      else
      {
        return getLifecycleOwner(((ContextWrapper) context).getBaseContext());
      }
    }
    return null;
  }


  @NonNull
  @Override
  public Lifecycle getLifecycle() {
    return lifecycleRegistry;
  }
}
