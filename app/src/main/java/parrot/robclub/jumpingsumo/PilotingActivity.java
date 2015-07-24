package parrot.robclub.jumpingsumo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.parrot.arsdk.arcommands.ARCOMMANDS_JUMPINGSUMO_ANIMATIONS_JUMP_TYPE_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_DEVICE_STATE_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_DICTIONARY_KEY_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_ERROR_ENUM;
import com.parrot.arsdk.arcontroller.ARControllerArgumentDictionary;
import com.parrot.arsdk.arcontroller.ARControllerDictionary;
import com.parrot.arsdk.arcontroller.ARControllerException;
import com.parrot.arsdk.arcontroller.ARDeviceController;
import com.parrot.arsdk.arcontroller.ARDeviceControllerListener;
import com.parrot.arsdk.arcontroller.ARDeviceControllerStreamListener;
import com.parrot.arsdk.arcontroller.ARFrame;
import com.parrot.arsdk.ardiscovery.ARDISCOVERY_PRODUCT_ENUM;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDevice;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceNetService;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;
import com.parrot.arsdk.ardiscovery.ARDiscoveryException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Author: nguyenquockhai create on 16/07/2015 at Robotics Club.
 * Desc:
 */
public class PilotingActivity extends Activity implements ARDeviceControllerListener, ARDeviceControllerStreamListener, SurfaceHolder.Callback{
    private static String TAG = PilotingActivity.class.getSimpleName();
    public static String EXTRA_DEVICE_SERVICE = "pilotingActivity.extra.device.service";

    public ARDeviceController deviceController;
    public ARDiscoveryDeviceService service;
    public ARDiscoveryDevice device;

    private Button jumBt;
    private Button turnLeftBt;
    private Button turnRightBt;

    private Button forwardBt;
    private Button backBt;

    private TextView batteryLabel;

    private AlertDialog alertDialog;

    private RelativeLayout view;

    // video vars
    private static final String VIDEO_MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC;
    private static final int VIDEO_DEQUEUE_TIMEOUT = 33000;
    private static final int VIDEO_WIDTH = 640;
    private static final int VIDEO_HEIGHT = 368;
    private SurfaceView sfView;
    private MediaCodec mediaCodec;
    private Lock readyLock;
    private boolean isCodecConfigured = false;
    private ByteBuffer csdBuffer;
    private boolean waitForIFrame = true;
    private ByteBuffer [] buffers;



        @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_piloting);

        initIHM();
        initVideoVars();

        Intent intent = getIntent();
        service = intent.getParcelableExtra(EXTRA_DEVICE_SERVICE);

        //create the device
        try
        {
            device = new ARDiscoveryDevice();

            ARDiscoveryDeviceNetService netDeviceService = (ARDiscoveryDeviceNetService) service.getDevice();

            device.initWifi(ARDISCOVERY_PRODUCT_ENUM.ARDISCOVERY_PRODUCT_JS, netDeviceService.getName(), netDeviceService.getIp(), netDeviceService.getPort());
        }
        catch (ARDiscoveryException e)
        {
            e.printStackTrace();
            Log.e(TAG, "Error: " + e.getError());
        }


        if (device != null)
        {
            try
            {
                //create the deviceController
                deviceController = new ARDeviceController (device);
                deviceController.addListener (this);
                deviceController.addStreamListener(this);
            }
            catch (ARControllerException e)
            {
                e.printStackTrace();
            }
        }
    }

    private void initIHM ()
    {
        view = (RelativeLayout) findViewById(R.id.piloting_view);

        jumBt = (Button) findViewById(R.id.jumBt);
        jumBt.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setPressed(true);
                        if (deviceController != null) {
                            deviceController.getFeatureJumpingSumo().sendAnimationsJump(ARCOMMANDS_JUMPINGSUMO_ANIMATIONS_JUMP_TYPE_ENUM.ARCOMMANDS_JUMPINGSUMO_ANIMATIONS_JUMP_TYPE_HIGH);
                        }
                        break;

                    case MotionEvent.ACTION_UP:
                        v.setPressed(false);
                        break;

                    default:

                        break;
                }

                return true;
            }
        });


        turnRightBt = (Button) findViewById(R.id.turnRightBt);
        turnRightBt.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                switch (event.getAction())
                {
                    case MotionEvent.ACTION_DOWN:
                        v.setPressed(true);
                        if (deviceController != null)
                        {
                            deviceController.getFeatureJumpingSumo().setPilotingPCMDTurn((byte) 50);
                            deviceController.getFeatureJumpingSumo().setPilotingPCMDFlag((byte) 1);

                        }
                        break;

                    case MotionEvent.ACTION_UP:
                        v.setPressed(false);
                        if (deviceController != null)
                        {
                            deviceController.getFeatureJumpingSumo().setPilotingPCMDTurn((byte) 0);
                        }
                        break;

                    default:

                        break;
                }

                return true;
            }
        });

        turnLeftBt = (Button) findViewById(R.id.turnLeftBt);
        turnLeftBt.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                switch (event.getAction())
                {
                    case MotionEvent.ACTION_DOWN:
                        v.setPressed(true);
                        if (deviceController != null)
                        {
                            deviceController.getFeatureJumpingSumo().setPilotingPCMDTurn((byte) -50);
                            deviceController.getFeatureJumpingSumo().setPilotingPCMDFlag((byte) 1);
                        }
                        break;

                    case MotionEvent.ACTION_UP:
                        v.setPressed(false);
                        if (deviceController != null)
                        {
                            deviceController.getFeatureJumpingSumo().setPilotingPCMDTurn((byte) 0);
                        }
                        break;

                    default:

                        break;
                }

                return true;
            }
        });

        forwardBt = (Button) findViewById(R.id.forwardBt);
        forwardBt.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                switch (event.getAction())
                {
                    case MotionEvent.ACTION_DOWN:
                        v.setPressed(true);
                        if (deviceController != null)
                        {
                            deviceController.getFeatureJumpingSumo().setPilotingPCMDSpeed((byte) 50);
                            deviceController.getFeatureJumpingSumo().setPilotingPCMDFlag((byte) 1);
                        }
                        break;

                    case MotionEvent.ACTION_UP:
                        v.setPressed(false);
                        if (deviceController != null)
                        {
                            deviceController.getFeatureJumpingSumo().setPilotingPCMDSpeed((byte) 0);
                            deviceController.getFeatureJumpingSumo().setPilotingPCMDFlag((byte) 0);
                        }
                        break;

                    default:

                        break;
                }

                return true;
            }
        });
        backBt = (Button) findViewById(R.id.backBt);
        backBt.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                switch (event.getAction())
                {
                    case MotionEvent.ACTION_DOWN:
                        v.setPressed(true);
                        if (deviceController != null) {
                            deviceController.getFeatureJumpingSumo().setPilotingPCMDSpeed((byte) -50);
                            deviceController.getFeatureJumpingSumo().setPilotingPCMDFlag((byte) 1);
                        }
                        break;

                    case MotionEvent.ACTION_UP:
                        v.setPressed(false);
                        if (deviceController != null)
                        {
                            deviceController.getFeatureJumpingSumo().setPilotingPCMDSpeed((byte) 0);
                            deviceController.getFeatureJumpingSumo().setPilotingPCMDFlag((byte) 0);
                        }
                        break;

                    default:

                        break;
                }

                return true;
            }
        });

        batteryLabel = (TextView) findViewById(R.id.batteryLabel);
    }

    @Override
    public void onStart()
    {
        super.onStart();

        //start the deviceController
        if (deviceController != null)
        {
            final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(PilotingActivity.this);

            // set title
            alertDialogBuilder.setTitle("Connecting ...");


            // create alert dialog
            alertDialog = alertDialogBuilder.create();
            alertDialog.show();

            ARCONTROLLER_ERROR_ENUM error = deviceController.start();

            if (error != ARCONTROLLER_ERROR_ENUM.ARCONTROLLER_OK)
            {
                finish();
            }
        }
    }

    private void stopDeviceController()
    {
        if (deviceController != null)
        {
            final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(PilotingActivity.this);

            // set title
            alertDialogBuilder.setTitle("Disconnecting ...");

            // show it
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // create alert dialog
                    alertDialog = alertDialogBuilder.create();
                    alertDialog.show();

                    ARCONTROLLER_ERROR_ENUM error = deviceController.stop();

                    if (error != ARCONTROLLER_ERROR_ENUM.ARCONTROLLER_OK) {
                        finish();
                    }
                }
            });
            //alertDialog.show();

        }
    }

    @Override
    protected void onStop()
    {
        if (deviceController != null)
        {
            deviceController.stop();
        }

        super.onStop();
    }

    @Override
    public void onBackPressed()
    {
        stopDeviceController();
    }

    public void onUpdateBattery(final int percent)
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                batteryLabel.setText(String.format("%d%%", percent));
            }
        });

    }


    @Override
    public void onStateChanged (ARDeviceController deviceController, ARCONTROLLER_DEVICE_STATE_ENUM newState, ARCONTROLLER_ERROR_ENUM error)
    {
        Log.i(TAG, "onStateChanged ... newState:" + newState + " error: " + error);

        switch (newState)
        {
            case ARCONTROLLER_DEVICE_STATE_RUNNING:
                //The deviceController is started
                Log.i(TAG, "ARCONTROLLER_DEVICE_STATE_RUNNING .....");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //alertDialog.hide();
                        alertDialog.dismiss();
                    }
                });
                deviceController.getFeatureJumpingSumo().sendMediaStreamingVideoEnable((byte)1);
                break;

            case ARCONTROLLER_DEVICE_STATE_STOPPED:
                //The deviceController is stoped
                Log.i(TAG, "ARCONTROLLER_DEVICE_STATE_STOPPED ....." );

                deviceController.dispose();
                deviceController = null;

                runOnUiThread(new Runnable() {
                    @Override
                    public void run()
                    {
                        //alertDialog.hide();
                        alertDialog.dismiss();
                        finish();
                    }
                });
                break;

            default:
                break;
        }
    }


    @Override
    public void onCommandReceived(ARDeviceController deviceController, ARCONTROLLER_DICTIONARY_KEY_ENUM commandKey, ARControllerDictionary elementDictionary)
    {
        if (elementDictionary != null)
        {
            if (commandKey == ARCONTROLLER_DICTIONARY_KEY_ENUM.ARCONTROLLER_DICTIONARY_KEY_COMMON_COMMONSTATE_BATTERYSTATECHANGED)
            {
                ARControllerArgumentDictionary<Object> args = elementDictionary.get(ARControllerDictionary.ARCONTROLLER_DICTIONARY_SINGLE_KEY);

                if (args != null)
                {
                    Integer batValue = (Integer) args.get("arcontroller_dictionary_key_common_commonstate_batterystatechanged_percent");

                    onUpdateBattery(batValue);
                }
            }
        }
        else
        {
            Log.e(TAG, "elementDictionary is null");
        }
    }

    @Override
    public void onFrameReceived(ARDeviceController deviceController, ARFrame frame)
    {
        byte[] data = frame.getByteData();
        ByteArrayInputStream ins = new ByteArrayInputStream(data);
        Bitmap bmp = BitmapFactory.decodeStream(ins);

        ImageView imgView = (ImageView) findViewById(R.id.imageView);

        FrameDisplay fDisplay = new FrameDisplay(imgView, bmp);
        fDisplay.execute();
    }


    @Override
    public void onFrameTimeout(ARDeviceController deviceController)
    {
        Log.i(TAG, "onFrameTimeout ..... ");
    }

    //region video
    public void initVideoVars()
    {
        readyLock = new ReentrantLock();
        applySetupVideo();
    }


    private void applySetupVideo()
    {
        String deviceModel = Build.DEVICE;
        Log.d(TAG, "configuring HW video codec for device: [" + deviceModel + "]");
        sfView = new SurfaceView(getApplicationContext());
        sfView.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
        sfView.getHolder().addCallback(this);

        view.addView(sfView, 0);
    }

    @SuppressLint("NewApi")
    public void reset()
    {
        /* This will be run either before or after decoding a frame. */
        readyLock.lock();

        view.removeView(sfView);
        sfView = null;

        releaseMediaCodec();

        readyLock.unlock();
    }

    /**
     * Configure and start media codec
     * @param type
     */
    @SuppressLint("NewApi")
    private void initMediaCodec(String type)
    {
        try
        {
            mediaCodec = MediaCodec.createDecoderByType(type);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        if (csdBuffer != null)
        {
            configureMediaCodec();
        }
    }

    @SuppressLint("NewApi")
    private void configureMediaCodec()
    {
        MediaFormat format = MediaFormat.createVideoFormat(VIDEO_MIME_TYPE, VIDEO_WIDTH, VIDEO_HEIGHT);
        format.setByteBuffer("csd-0", csdBuffer);
        //format.setInteger(MediaFormat.KEY_FRAME_RATE, 15);

        mediaCodec.configure(format, sfView.getHolder().getSurface(), null, 0);
        mediaCodec.start();

        buffers = mediaCodec.getInputBuffers();


        isCodecConfigured = true;
    }

    @SuppressLint("NewApi")
    private void releaseMediaCodec()
    {
        if ((mediaCodec != null) && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN))
        {
            if (isCodecConfigured)
            {
                mediaCodec.stop();
                mediaCodec.release();
            }
            isCodecConfigured = false;
            mediaCodec = null;
        }
    }

    public ByteBuffer getCSD(ARFrame frame)
    {
        int spsSize;
        if (frame.isIFrame())
        {
            byte[] data = frame.getByteData();
            int searchIndex;
            // we'll need to search the "00 00 00 01" pattern to find each header size
            // Search start at index 4 to avoid finding the SPS "00 00 00 01" tag
            for (searchIndex = 4; searchIndex <= frame.getDataSize() - 4; searchIndex ++)
            {
                if (0 == data[searchIndex  ] &&
                        0 == data[searchIndex+1] &&
                        0 == data[searchIndex+2] &&
                        1 == data[searchIndex+3])
                {
                    break;  // PPS header found
                }
            }
            spsSize = searchIndex;

            // Search start at index 4 to avoid finding the PSS "00 00 00 01" tag
            for (searchIndex = spsSize+4; searchIndex <= frame.getDataSize() - 4; searchIndex ++)
            {
                if (0 == data[searchIndex  ] &&
                        0 == data[searchIndex+1] &&
                        0 == data[searchIndex+2] &&
                        1 == data[searchIndex+3])
                {
                    break;  // frame header found
                }
            }
            int csdSize = searchIndex;

            byte[] csdInfo = new byte[csdSize];
            System.arraycopy(data, 0, csdInfo, 0, csdSize);
            return ByteBuffer.wrap(csdInfo);
        }
        return null;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {
        readyLock.lock();
        initMediaCodec(VIDEO_MIME_TYPE);
        readyLock.unlock();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {
    }


    @SuppressLint("NewApi")
    @Override
    public void surfaceDestroyed(SurfaceHolder holder)
    {
        readyLock.lock();
        releaseMediaCodec();
        readyLock.unlock();
    }

    //endregion video
}

class FrameDisplay extends AsyncTask<Void, Void, Bitmap>
{
    private final WeakReference<ImageView> imageViewReference;
    private final Bitmap bitmap;

    public FrameDisplay(ImageView imageView, Bitmap bmp) {
        // Use a WeakReference to ensure the ImageView can be garbage collected
        imageViewReference = new WeakReference<ImageView>(imageView);
        bitmap = bmp;
    }

    // Decode image in background.
    @Override
    protected Bitmap doInBackground(Void... params) {
        return bitmap;
    }

    // Once complete, see if ImageView is still around and set bitmap.
    @Override
    protected void onPostExecute(Bitmap bmp) {
        if (bmp != null) {
            final ImageView imageView = imageViewReference.get();
            if (imageView != null) {
                imageView.setImageBitmap(bmp);
            }
        }
    }
}



