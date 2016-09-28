package co.mide.circlevideo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import java.io.File;

@SuppressWarnings("deprecation")
public class MainActivity extends FullscreenActivity {
    private TextureView textureView;
    private Camera camera;
    private RecordButton recordButton;
    private SurfaceTexture surface;
    private MainActivityController controller;
    private File capturedMedia;
    private final int CAMERA_REQUEST_CODE = 88;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textureView = (TextureView) findViewById(R.id.texture_view);
        recordButton = (RecordButton) findViewById(R.id.record_button);
        controller = new MainActivityController(this);
        initCameraSwitch();
    }

    @Override
    public void onPause() {
        super.onPause();
        if(camera != null) {
            camera.release();
            camera = null;
        }
        recordButton.setRecordListener(null);
        textureView.setOnClickListener(null);
        textureView.setSurfaceTextureListener(null);
    }

    void setCapturedFile(File file) {
        capturedMedia = file;
    }

    @Override
    public void onResume() {
        super.onResume();
        setToolbarVisibility(false);
        recordButton.setRecordListener(controller.getRecordListener());
        textureView.setOnClickListener(controller.getTextureClickListener());

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
            }
        }else{
            openCamera();
        }
    }

    private void initCameraSwitch() {
        ImageButton switchButton = (ImageButton) findViewById(R.id.image_button_switch);
        if(Camera.getNumberOfCameras() == 1){
            switchButton.setVisibility(View.INVISIBLE);
        } else {
            switchButton.setOnClickListener((view) -> controller.toggleCamera());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            }
        }
    }

    void openCamera(int cameraId)  {
        try{
            if(camera != null){
                camera.stopPreview();
                camera.release();
                camera = null;
            }
            camera = Camera.open(cameraId);
            //set listener after camera is available or else there'll be problems
            textureView.setSurfaceTextureListener(controller.getSurfaceTextureListener());
            controller.setupCamera(camera);
        }catch(RuntimeException e){
            //todo show dialog saying camera error
            Log.e("Open Camera", "failed to open Camera");
            e.printStackTrace();
        }
    }

    Camera openCamera() {
        if(Camera.getNumberOfCameras() > 1) {
            openCamera(Camera.CameraInfo.CAMERA_FACING_BACK);
        } else if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)) {
            openCamera(Camera.CameraInfo.CAMERA_FACING_FRONT);
        } else {
            openCamera(Camera.CameraInfo.CAMERA_FACING_BACK);
        }
        return camera;
    }

    void showErrorMessage() {

    }

    void scalePreviewTextureView(int width, int height) {
        int screenWidth = getScreenWidth();
        int screenHeight = getScreenHeight();

        float squared = (float)(Math.pow(screenHeight, 2) + Math.pow(screenWidth, 2));
        float maxLength = (float)Math.sqrt(squared);

        float ratioX, ratioY;
        if(width < height) {
            ratioX = maxLength/width;
            ratioY = (ratioX*height)/screenHeight;
        } else {
            ratioY = maxLength/height;
            ratioX = (ratioY*width)/screenWidth;
        }
        //before scaling up recycler view is the same size as parent hence why scaling rather than
        //setting exact dimension
        getTextureView().setScaleX(ratioX);
        getTextureView().setScaleY(ratioY);
    }

    void launchVideoActivity() {
        int[] size = controller.getVideoSize();
        startActivity(VideoActivity.getStartIntent(this, true, capturedMedia,
                textureView.getWidth(), textureView.getHeight() ,
                size[0], size[1]));
    }

    void launchVideoActivityForImage() {
        int[] size = controller.getImageSize();
        Log.d("dbug", "size: "+size[0]+", "+size[1]);
        startActivity(VideoActivity.getStartIntent(this, false, capturedMedia,
                textureView.getWidth(), textureView.getHeight() ,
                size[0], size[1]));
    }

    void launchTestActivity() {
        startActivity(new Intent(this, GyroscopeTest.class));
    }

    public int getScreenWidth() {
        return  ((RelativeLayout) textureView.getParent()).getWidth();
    }

    public int getScreenHeight() {
        return  ((RelativeLayout) textureView.getParent()).getHeight();
    }

    public Camera getCamera() {
        return camera;
    }

    public void setSurface(SurfaceTexture surface) {
        this.surface = surface;
    }

    public SurfaceTexture getSurface() {
        return surface;
    }

    public TextureView getTextureView() {
        return textureView;
    }
}
