package parrot.robclub.jumpingsumo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Author: nguyenquockhai create on 28/07/2015 at Robotics Club.
 * Desc:
 */
public class JpegView extends SurfaceView implements SurfaceHolder.Callback
{
    private Bitmap bitmap = null;
    private SurfaceHolder holder;

    public JpegView(Context context)
    {
        super(context);
        holder = getHolder();
        setWillNotDraw(false);
    }

    public void setBitmap(Bitmap bmp){
        bitmap= bmp;
        Canvas canvas = holder.lockCanvas();
        draw(canvas);
        holder.unlockCanvasAndPost(canvas);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (this.bitmap == null)
            return;
        //Rect rect = new Rect(0, 0, this.getWidth(), this.getHeight());
        canvas.drawColor(Color.BLACK);
        canvas.drawBitmap(this.bitmap, 0, 0, null);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {
        //readyLock.lock();
        //readyLock.unlock();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder)
    {
        //readyLock.lock();
        //readyLock.unlock();
    }
}
