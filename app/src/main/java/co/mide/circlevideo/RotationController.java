package co.mide.circlevideo;

import android.content.Context;
import android.hardware.SensorManager;
import android.support.annotation.Nullable;
import android.view.Choreographer;

import org.hitlabnz.sensor_fusion_demo.orientationProvider.CalibratedGyroscopeProvider;
import org.hitlabnz.sensor_fusion_demo.orientationProvider.OrientationProvider;

/**
 * Class for handling gyroscope event.
 * Based heavily on http://stackoverflow.com/a/10369974/2057884
 */
class RotationController {
    private OrientationProvider orientationProvider;
    private AngleChangeListener angleChangeListener;
    private Choreographer choreographer;
    private Choreographer.FrameCallback frameCallback;
    private float[] values = new float[3];
    private Context context;


    RotationController(Context context) {
        this.context = context;
        this.choreographer = Choreographer.getInstance();
        frameCallback = createFrameCallback(choreographer);
    }

    void setAngleChangeListener(@Nullable AngleChangeListener angleChangeListener) {
        this.angleChangeListener = angleChangeListener;
    }

    void init() {
        SensorManager sensorManager
                = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        //CalibratedGyroscopeProvider is relative to the device unlike others in the library
        orientationProvider = new CalibratedGyroscopeProvider(sensorManager);
        orientationProvider.start();
        choreographer.postFrameCallback(frameCallback);
    }

    void cleanUp() {
        choreographer.removeFrameCallback(frameCallback);
        orientationProvider.stop();
    }

    private float startAngle = 0f;
    private Choreographer.FrameCallback createFrameCallback(Choreographer choreographer) {
        return new Choreographer.FrameCallback() {
            @Override
            public void doFrame(long frameTimeNanos) {
                orientationProvider.getEulerAngles(values);

                //Convert radians to degrees
                for(int i = 0; i < values.length; i++) {
                    values[i] = (float)Math.toDegrees(values[i]);
                }

                //This is the relevant device rotation
                float zeta = values[0];
                if(angleChangeListener != null) {
                    angleChangeListener.angleChange(startAngle - zeta);
                } else {
                    //This isn't really the start angle, it's the angle before start angle
                    //but I don't want to add the complexity of using a flag,
                    // and the angle couldn't have changed that much between one frame
                    startAngle = zeta;
                }
                choreographer.postFrameCallback(this);
            }
        };
    }

    interface AngleChangeListener{
        void angleChange(float degree);
    }
}
