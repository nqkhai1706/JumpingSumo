package parrot.robclub.jumpingsumo;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Author: nguyenquockhai create on 28/07/2015 at Robotics Club.
 * Desc: This is a SurfaceView for this play Jpeg frames
 */
public class JpegView extends SurfaceView implements SurfaceHolder.Callback
{
    private static String TAG = JpegView.class.getSimpleName();

    private Bitmap bitmap = null;
    private SurfaceHolder holder;

    private Lock readyLock = null;

    public JpegView(Context context)
    {
        super(context);
        this.holder = getHolder();
        this.holder.addCallback(this);
        this.readyLock = new ReentrantLock();
    }

    public void setBitmap(Bitmap bmp){
        this.readyLock.lock();

        this.bitmap = bmp;
        this.drawBitmap(this.holder);

        this.readyLock.unlock();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {
        readyLock.lock();

        this.drawBitmap(holder);

        readyLock.unlock();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {
        readyLock.lock();

        this.drawBitmap(holder);

        readyLock.unlock();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder)
    {
        readyLock.lock();

        this.drawBitmap(holder);
        this.bitmap = null;

        readyLock.unlock();
    }


    private void drawBitmap(SurfaceHolder holder) {
        Log.i(TAG, "Trying to draw...");

        synchronized (holder) {
            Canvas canvas = holder.lockCanvas();
            if (canvas == null) {
                Log.e(TAG, "Cannot draw onto the canvas as it's null");
            } else {
                drawMyStuff(canvas);
                holder.unlockCanvasAndPost(canvas);
            }
        }
    }

    private void drawMyStuff(final Canvas canvas) {
        Log.i(TAG, "Drawing...");
        canvas.drawColor(Color.BLACK);
        if (bitmap != null)
            canvas.drawBitmap(bitmap, 0, 0, null);
    }
}
