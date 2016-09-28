package co.mide.circlevideo;

import android.content.Context;
import android.hardware.SensorManager;

import org.hitlabnz.sensor_fusion_demo.orientationProvider.GravityCompassProvider;
import org.hitlabnz.sensor_fusion_demo.orientationProvider.OrientationProvider;
import org.hitlabnz.sensor_fusion_demo.representation.Quaternion;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

/**
 * Class for handling gyroscope event.
 * Based heavily on http://stackoverflow.com/a/10369974/2057884
 */
class RotationController {
    private OrientationProvider orientationProvider;

    private Context context;
    private AngleChangeListener angleChangeListener;
    private Subscription subscription;
    private float[] angles = new float[3];


    RotationController(Context context, AngleChangeListener angleChangeListener) {
        this.context = context;
        this.angleChangeListener = angleChangeListener;
    }

    private float firstAngle = Float.POSITIVE_INFINITY;
    private Quaternion quaternion = new Quaternion();

    void init() {
        SensorManager sensorManager
                = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        orientationProvider = new GravityCompassProvider(sensorManager);
        orientationProvider.start();
        subscription = Observable.interval(40, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((time) -> {
                    orientationProvider.getEulerAngles(angles);
                    orientationProvider.getQuaternion(quaternion);
                    if(firstAngle == Float.POSITIVE_INFINITY) {
                        firstAngle = calcZeta(quaternion.getW(), quaternion.getX(),
                                quaternion.getY(), quaternion.getZ());
                    }else{
                        float zeta = calcZeta(quaternion.getW(), quaternion.getX(),
                                quaternion.getY(), quaternion.getZ());
                        angleChangeListener.angleChange(firstAngle - zeta);
                    }

                });
    }

    private float calcPhi(float q0, float q1, float q2, float q3){
        return (float)Math.toDegrees(Math.atan2(2*(q0*q1 + q2+q3), 1 - 2*(q1*q1 + q2*q2)));
    }

    private float calcTheta(float q0, float q1, float q2, float q3){
        return (float)Math.toDegrees(Math.asin(2*(q0*q2 - q3*q1)));
    }

    private float calcZeta(float q0, float q1, float q2, float q3){
        return (float)Math.toDegrees(Math.atan2(2*(q0*q3 + q1*q2), 1 - 2*(q2*q2 + q3*q3)));
    }

    void cleanUp() {
        subscription.unsubscribe();
        orientationProvider.stop();
    }

    interface AngleChangeListener{
        void angleChange(float degree);
    }
}
