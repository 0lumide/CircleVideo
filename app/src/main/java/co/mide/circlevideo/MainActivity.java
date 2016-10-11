package co.mide.circlevideo;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import com.google.firebase.crash.FirebaseCrash;

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
        FirebaseCrash.log("MainActivity onCreate");
        setContentView(R.layout.activity_main);
        textureView = (TextureView) findViewById(R.id.texture_view);
        recordButton = (RecordButton) findViewById(R.id.record_button);
        controller = new MainActivityController(this);
        initCameraSwitch();
    }

    @Override
    public void onPause() {
        super.onPause();
        FirebaseCrash.log("MainActivity onPause");
        if(camera != null) {
            camera.release();
            camera = null;
        }
        recordButton.setRecordListener(null);
        textureView.setOnClickListener(null);
        textureView.setSurfaceTextureListener(null);
    }

    @Override
    public void onResume() {
        super.onResume();
        FirebaseCrash.log("MainActivity onResume");
        setToolbarVisibility(false);
        recordButton.setRecordListener(controller.getRecordListener());
        textureView.setOnClickListener(controller.getTextureClickListener());

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (needsPermissions()) {
                requestPermissions(PERMISSIONS, CAMERA_REQUEST_CODE);
            } else {
                openCamera();
            }
        }else{
            openCamera();
        }
    }

    private final String[] PERMISSIONS
            = new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};

    @TargetApi(23)
    private boolean needsPermissions() {
        boolean needPermission = false;
        for(String permission : PERMISSIONS){
            needPermission |= checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED;
        }
        return needPermission;
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
            if(textureView.getSurfaceTexture() == null) {
                textureView.setSurfaceTextureListener(controller.getSurfaceTextureListener());
            } else {
                controller.setupCamera(camera);
            }
        }catch(RuntimeException e){
            //todo show dialog saying camera error
            FirebaseCrash.log("failed to open Camera");
            e.printStackTrace();
        }
    }

    Camera openCamera() {
        openCamera(controller.getCameraId());
        return camera;
    }

    File createFile(String extension) {
        File file = new File(getFilesDir(), System.currentTimeMillis() + "." + extension);
        this.capturedMedia = file;
        return file;
    }

    void showErrorMessage() {
        //todo
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
        startActivity(VideoActivity.getStartIntent(this, true, capturedMedia,
                textureView.getWidth(), textureView.getHeight() ,
                controller.getVideoWidth(), controller.getVideoHeight()));
    }

    void launchVideoActivityForImage() {
        startActivity(VideoActivity.getStartIntent(this, false, capturedMedia,
                textureView.getWidth(), textureView.getHeight() ,
                controller.getImageWidth(), controller.getImageHeight()));
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
