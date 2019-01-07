package com.example.ning.arlinkplayerdemo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.opengl.GLSurfaceView;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity implements View.OnClickListener, SurfaceTexture.OnFrameAvailableListener {

    public static final int PIXEL_WIDTH = 1280;
    public static final int PIXEL_HEIGHT = 720;
    private static final String TAG = "Artosyn player";

    private static final String ACTION_USB_PERMISSION = "com.xaircraft.USB_PERMISSION";
    private PlayerThread mPlayer = null;
    private FiFoUtlis streamFiFo = null;
    private LinkedList<ByteBuffer> frameBufferQueue;
    static final int FILTER_NONE = 0;
    static final int FILTER_BLACK_WHITE = 1;
    static final int FILTER_BLUR = 2;
    static final int FILTER_SHARPEN = 3;
    static final int FILTER_EDGE_DETECT = 4;
    static final int FILTER_EMBOSS = 5;
    static final int FILTER_NASHVILLE=6;
    GLSurfaceView sfv_video = null;
    private static TextureMovieEncoder sVideoEncoder = new TextureMovieEncoder();
    FileOutputStream out = null;
    private AccessoryLink mAccessoryLink;
    private boolean mRecordingEnabled;  //判断是否开始录制：录制返回true
    public Surface surface;
    private CameraSurfaceRenderer mRenderer;


    static {
        System.loadLibrary("native-lib");
        System.loadLibrary("avcodec-57");
        System.loadLibrary("avdevice-57");
        System.loadLibrary("avformat-57");
        System.loadLibrary("avfilter-6");
        System.loadLibrary("avutil-55");
        System.loadLibrary("postproc-54");
        System.loadLibrary("swresample-2");
        System.loadLibrary("swscale-4");
    }

    private Button mSaveButton;
    private int screenWidth;
    private int screenHeight;
    private CameraHandler mCameraHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        File outputFile = new File(Environment.getExternalStorageDirectory().getPath() + "/camera-test.mp4");
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        frameBufferQueue = new LinkedList<>();
        out = null;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        sfv_video = findViewById(R.id.surface);
        mSaveButton = findViewById(R.id.save_button);
        mSaveButton.setOnClickListener(this);

        mCameraHandler = new CameraHandler(this);
        mRecordingEnabled = sVideoEncoder.isRecording();

        sfv_video.setEGLContextClientVersion(2);
        mRenderer = new CameraSurfaceRenderer(mCameraHandler, sVideoEncoder, outputFile);
        sfv_video.setRenderer(mRenderer);
        sfv_video.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);


        //  sfv_video.setSurfaceTextureListener(this);

        try {
            WindowManager wm = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
            screenWidth = wm.getDefaultDisplay().getWidth();
            screenHeight = wm.getDefaultDisplay().getHeight();
            Log.e("ArtosynPlayer", "screen width is:" + screenWidth + " screen height is:" + screenHeight);

        } catch (Exception e) {
            Log.e("ArtosynPlayer", e.toString());
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        filter.addAction(ACTION_USB_PERMISSION);
        registerReceiver(mUsbReceiver, filter);


        streamFiFo = new FiFoUtlis();

        mAccessoryLink = new AccessoryLink(this, new AccessoryLink.OnReceivedListener() {
            @Override
            public void onReceived(byte[] b) {

                if (b.length <= 0)
                    return;
                streamFiFo.FiFoWrite(b, b.length);

            }
        });

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (true) {
                        if (streamFiFo.getActualSize() > 0) {
                            // startVideoStream(MainActivity.this);
                            DecodeUtlis.getInstance().searchFrame(streamFiFo, new DecodeUtlis.FrameBufferListener() {
                                @Override
                                public void isPushFrameBuffer(ByteBuffer byteBuffer) {
                                    if (byteBuffer != null) {
                                        pushFrameBuffer(byteBuffer);
                                    }
                                }
                            });
                        }
                    }
                } catch (Exception e) {
                    Log.e("ArtosynPlayerError", "ERROR in searchFrame!!!!");
                    e.printStackTrace();
                }
            }
        }).start();

        sfv_video.queueEvent(new Runnable() {
            @Override
            public void run() {
                // notify the renderer that we want to change the encoder's state
                mRenderer.changeFilterMode(6);
            }
        });

    }

    /**
     * 对h264视频数据进行添加
     *
     * @param byteBuffer
     */
    private void pushFrameBuffer(ByteBuffer byteBuffer) {
        try {
            synchronized (frameBufferQueue) {
                frameBufferQueue.add(byteBuffer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 判断解码储存容器中是否有数据
     *
     * @return
     */
    public boolean hasFrameBuffer() {
        synchronized (frameBufferQueue) {
            return !frameBufferQueue.isEmpty();
        }
    }

    /**
     * 取解码容器中栈顶帧数据
     *
     * @return
     */
    public ByteBuffer dequeueFrameBuffer() {
        synchronized (frameBufferQueue) {
            return frameBufferQueue.removeFirst();
        }
    }


    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                //本地播放
                //   dummyTest();
                //aoa握手通信
                tryOpenAccessory();

            }
        };
        timer.schedule(task, 5);

        sfv_video.onResume();
        sfv_video.queueEvent(new Runnable() {
            @Override
            public void run() {
                mRenderer.setCameraPreviewSize(screenWidth, screenHeight);
            }
        });
        super.onResume();
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();

        sfv_video.queueEvent(new Runnable() {
            @Override
            public void run() {
                // Tell the renderer that it's about to be paused so it can clean up.
                mRenderer.notifyPausing();
            }
        });
        sfv_video.onPause();
        MainActivity.this.finish();
        onDestroy();
        System.exit(0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCameraHandler.invalidateHandler();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    protected void onStop() {
        super.onStop();
        Log.e("ActivityYuvOrRgbViewer", "onStop");
    }

    @Override
    public void onClick(View v) {

    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        sfv_video.requestRender();
    }


    private void tryOpenAccessory() {
        UsbManager systemService = (UsbManager) getSystemService(Context.USB_SERVICE);
        UsbAccessory[] accessoryList = systemService.getAccessoryList();
        if (accessoryList != null && accessoryList.length > 0) {
            mAccessoryLink.open();
        }
    }

    private int syncStream(byte[] data, int len) {
        int count = streamFiFo.FiFoRead(data, len);
        return count;
    }


    /**
     * usb接口通信广播
     */
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            //aoa握手通信
            if (UsbManager.ACTION_USB_ACCESSORY_ATTACHED == action) {
                Parcelable parcelableExtra = intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
                Log.i("TAG", "ATTACHED: " + parcelableExtra.toString());
            }

            if (UsbManager.ACTION_USB_ACCESSORY_DETACHED == action) {
                Parcelable parcelableExtra = intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);

                Log.i("TAG", "**DETACHED: " + parcelableExtra.toString());
            }

        }
    };


    /**
     * 视频流预览消息通道
     */
    static class CameraHandler extends Handler {
        public static final int MSG_SET_SURFACE_TEXTURE = 0;

        // Weak reference to the Activity; only access this from the UI thread.
        private WeakReference<MainActivity> mWeakActivity;

        public CameraHandler(MainActivity activity) {
            mWeakActivity = new WeakReference<MainActivity>(activity);
        }

        /**
         * 删除对活动的引用。 有用的偏执措施，以确保
         * 捕获尝试通过处理程序访问陈旧活动。
         *          
         */
        public void invalidateHandler() {
            mWeakActivity.clear();
        }

        @Override  // runs on UI thread
        public void handleMessage(Message inputMessage) {
            int what = inputMessage.what;
            Log.d(TAG, "CameraHandler [" + this + "]: what=" + what);

            MainActivity activity = mWeakActivity.get();
            if (activity == null) {
                Log.w(TAG, "CameraHandler.handleMessage: activity is null");
                return;
            }

            switch (what) {
                case MSG_SET_SURFACE_TEXTURE:
                    activity.handleSetSurfaceTexture((SurfaceTexture) inputMessage.obj);
                    break;
                default:
                    throw new RuntimeException("unknown msg " + what);
            }
        }
    }

    /**
     * 将SurfaceTexture连接到MediaCodec输出，然后开始预览。
     */
    private void handleSetSurfaceTexture(SurfaceTexture st) {
        st.setOnFrameAvailableListener(this);
        Surface surface = new Surface(st);
        if (mPlayer == null) {
            mPlayer = new PlayerThread(surface,this);
            mPlayer.start();
        }

    }


    native void stopVideo();

    //软解
    native void startVideoStream1(Object id, Surface surface);

}
