package co.mide.circlevideo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;

public class GyroscopeTest extends AppCompatActivity {
    private RotationController rotationController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_gyroscope_test);
        ImageView compass = (ImageView)findViewById(R.id.imageview_compass);
        rotationController = new RotationController(this, compass::setRotation);
    }

    @Override
    public void onResume() {
        super.onResume();
        rotationController.init();
    }

    public void onPause() {
        super.onPause();
        rotationController.cleanUp();
    }
}
