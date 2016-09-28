package co.mide.circlevideo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.ImageButton;

public class RecordButton extends ImageButton {
    private Paint paint;
    private RectF rect;
    private Handler updateHandler;
    private Handler stopHandler;
    private GestureDetector gestureDetector;
    private boolean isRecording = false;
    private float angleSweep = 0;
    private long startTime;
    private final int TIME_LIMIT = 10000;
    private RecordButtonListener recordListener;

    /**
     * Constructor
     * @param context the activity context
     */
    public RecordButton(Context context){
        super(context);
        init();
    }

    /**
     * Constructor
     * @param context the Activity Context
     * @param attr stuff
     */
    public RecordButton(Context context, AttributeSet attr){
        super(context, attr);
        init();
    }

    /**
     * Constructor
     * @param context the activity context
     * @param attr stuff
     * @param defStyle even more stuff
     */
    public RecordButton(Context context, AttributeSet attr, int defStyle){
        super(context, attr, defStyle);
        init();
    }

    /**
     * This function registers the recordListener object that implements the recordListener interface
     * the recordListener is called when a new audio starts recording or stops recording
     * @param listener the object that implements recordListener
     */
    public void setRecordListener(RecordButtonListener listener){
        if(listener == null) {
            isRecording = false;
        }
        recordListener = listener;
    }

    private void init(){
        //Initialize the paint object
        paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(10);
        paint.setColor(0xffdf181f);

        //Initialize the gesture detector
        gestureDetector = new GestureDetector(getContext(), new GestureListener());
    }

    /**
     * This initializes two handlers, kind of like an alarm, one that stops the recording
     * when the time limit is reached, and the other that updates the visualization
     */

    private void initializeHandlers(){
        //store the time the recording started, for updating the visualization
        startTime = System.currentTimeMillis();
        isRecording = true;

        //Creates the handler that updated the visualization
        updateHandler = new Handler();
        updateHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isRecording()) {
                    //stuff used in the visualization
                    angleSweep = ((System.currentTimeMillis() - startTime) * 360f) / TIME_LIMIT;

                    updateHandler.postDelayed(this, 10);
                } else
                    angleSweep = 0;
                invalidate();
            }
        }, 10);

        //Create the handler that stops the recording after the time limit is reached
        stopHandler = new Handler();
        stopHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                cleanUpHandlers();
            }
        }, TIME_LIMIT);
    }

    public boolean isRecording(){
        return isRecording;
    }

    /**
     * This function stops the handlers and stops the recording if recording.
     * It also resets the visualization
     */
    private void cleanUpHandlers(){
        //Stop recording if still recording
        if(isRecording()) {
            if(recordListener != null) {
                recordListener.onStopRecording();
            }
            isRecording = false;
        }

        //Stop the handlers
        try {
            updateHandler.removeCallbacks(null);
            stopHandler.removeCallbacks(null);
        }catch (NullPointerException e){
            //Do Nothing
        }

        //reset the visualization
        angleSweep = 0;
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event){
        //call the gesture detector and see if it consumed the touchEvent
        boolean result = gestureDetector.onTouchEvent(event);
        //if the touch event wasn't consumed pass it on to the super class
        if(!result) {
            result = super.onTouchEvent(event);
        }
        //Confirm that the record button is still pressed
        if((!isPressed() || (event.getAction() == MotionEvent.ACTION_UP)) && isRecording()) {
            //If record button is no longer pressed, stop the recording
            Log.v("Gesture", "stop " + event.getAction());
            isRecording = false;
            cleanUpHandlers();
            if(recordListener != null) {
                recordListener.onStopRecording();
            }
            getParent().requestDisallowInterceptTouchEvent(false);
        }
        return result;
    }

    @Override
    /**
     * Draws the red visualization arc
     */
    public void onDraw(Canvas canvas){
        if(rect == null)
            rect = new RectF(10, 10, getWidth() - 10, getHeight() - 10);
        super.onDraw(canvas);
        canvas.drawArc(rect, 270, angleSweep, false, paint);
    }

    //Class that detects gestures
    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDoubleTap (MotionEvent event){
            Log.v("Gesture", "doubleTap");
            return true;
        }

        @Override
        public void onLongPress (MotionEvent event){
            if(isEnabled()) {
                Log.v("Gesture", "longPress");
                getParent().requestDisallowInterceptTouchEvent(true);
                if(recordListener != null) {
                    recordListener.onStartRecording();
                }
                initializeHandlers();
                isRecording = true;
            }
        }

        @Override
        public boolean onSingleTapUp (MotionEvent event){
            if(recordListener != null) {
                recordListener.onSingleTap();
            }
            Log.v("Gesture", "SingleTapUp");
            return false;
        }
    }

    public interface RecordButtonListener {
        void onStartRecording();
        void onStopRecording();
        void onSingleTap();
    }
}
