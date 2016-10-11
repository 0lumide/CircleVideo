package co.mide.circlevideo;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.ExifInterface;
import android.media.MediaRecorder;
import android.view.TextureView;
import android.view.View;

import com.google.firebase.crash.FirebaseCrash;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

@SuppressWarnings("deprecation")
class MainActivityController {
    private MainActivity mainActivity;
    private int[] videoSize = new int[2];
    private int[] imageSize = new int[2];
    private MediaRecorder mediaRecorder;

    MainActivityController(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    private View.OnClickListener textureSurfaceClickListener = (view) -> {
        if(mainActivity.getCamera() != null) {
            mainActivity.getCamera().autoFocus(null);
        }
    };

    View.OnClickListener getTextureClickListener() {
        return textureSurfaceClickListener;
    }

    private void resetMediaRecorder() {
        if(mediaRecorder != null) {
            mediaRecorder.reset();
            mediaRecorder.release();
            mediaRecorder = null;
        }
        if(mainActivity.getCamera() != null) {
            mainActivity.getCamera().lock();
        }
    }

    private RecordButton.RecordButtonListener recordListener
            = new RecordButton.RecordButtonListener() {
        @Override
        public void onStartRecording() {
            mediaRecorder = new MediaRecorder();
            mainActivity.getCamera().unlock();
            mediaRecorder.setCamera(mainActivity.getCamera());
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
            CamcorderProfile profile
                    = CamcorderProfile.get(getCameraId(), CamcorderProfile.QUALITY_HIGH);
            profile.videoFrameWidth = videoSize[0];
            profile.videoFrameHeight = videoSize[1];
            mediaRecorder.setProfile(profile);
            mediaRecorder.setOrientationHint(90);
            File file = mainActivity.createFile("mp4");
            mediaRecorder.setOutputFile(file.getPath());
            try {
                mediaRecorder.prepare();
                mediaRecorder.start();
            }catch (IllegalStateException | IOException e) {
                e.printStackTrace();
                resetMediaRecorder();
                mainActivity.showErrorMessage();
            }
        }

        @Override
        public void onStopRecording() {
            if(mediaRecorder != null) {
                mediaRecorder.stop();
            }
            resetMediaRecorder();
            mainActivity.launchVideoActivity();
        }

        @Override
        public void onSingleTap() {
            mainActivity.getCamera().takePicture(null, null, (data, camera) -> {
                FileOutputStream fos = null;
                try {
                    File file = mainActivity.createFile("jpg");
                    fos = mainActivity.openFileOutput(file.getName(), Context.MODE_PRIVATE);
                    fos.write(data);
                    ExifInterface exif = new ExifInterface(file.getAbsolutePath());
                    exif.setAttribute(ExifInterface.TAG_ORIENTATION,
                            String.valueOf(ExifInterface.ORIENTATION_ROTATE_90));
                    exif.saveAttributes();
                    mainActivity.launchVideoActivityForImage();
                } catch (IOException e) {
                    e.printStackTrace();
                    mainActivity.showErrorMessage();
                } finally {
                    if(fos != null) {
                        try {
                            fos.close();
                        }catch (IOException e) {
                            //Like seriously??
                            e.printStackTrace();
                        }
                    }
                }
            });
        }
    };

    RecordButton.RecordButtonListener getRecordListener() {
        return recordListener;
    }

    private TextureView.SurfaceTextureListener
            surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            mainActivity.setSurface(surface);
            FirebaseCrash.log("onSurfaceTextureAvailable  camera avail: "
                    +(mainActivity.getCamera() != null));
            if(mainActivity.getCamera() != null) {
                setupCamera(mainActivity.getCamera());
            }
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            // Ignored, Camera does all the work for us
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            // Invoked every time there's a new Camera preview frame
        }
    };

    TextureView.SurfaceTextureListener getSurfaceTextureListener() {
        return surfaceTextureListener;
    }


    private int[] getGoodSize(List<Camera.Size> sizes) {
        int maxWidth = 0;
        int maxHeight = 0;
        for (Camera.Size size: sizes) {
            if (size.width >= maxWidth && size.height >= maxHeight) {
                maxWidth = size.width;
                maxHeight = size.height;
            }
        }
        return new int[]{maxWidth, maxHeight};
    }

    /**
     * @return the width of the rotated video
     */
    int getVideoWidth() {
        return videoSize[1];
    }

    /**
     * @return the height of the rotated video
     */
    int getVideoHeight() {
        return videoSize[0];
    }

    /**
     * @return the width of the rotated image
     */
    int getImageWidth() {
        return imageSize[1];
    }

    /**
     * @return the height of the rotated image
     */
    int getImageHeight() {
        return imageSize[0];
    }

    void setupCamera(final Camera camera) {
        Camera.Parameters param = camera.getParameters();
        param.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);

        int[] size = getGoodSize(param.getSupportedPreviewSizes());
        //switched width and height because layout is portrait
        mainActivity.scalePreviewTextureView(size[1], size[0]);

        param.setPreviewSize(size[0], size[1]);
        if(param.getSupportedVideoSizes() != null) {
            size = getGoodSize(param.getSupportedVideoSizes());
        }

        videoSize = size.clone();

        size = getGoodSize(param.getSupportedPictureSizes());
        param.setPictureSize(size[0], size[1]);

        imageSize = size.clone();
        camera.setParameters(param);

        if(mainActivity.getSurface() != null) {
            try {
                camera.setPreviewTexture(mainActivity.getSurface());
            } catch (IOException e) {
                e.printStackTrace();
            }
            camera.setDisplayOrientation(90);
            camera.startPreview();
        }
    }

    private boolean isBackCamera = true;

    int getCameraId() {
        if(mainActivity.getCamera() == null) {
            PackageManager pm = mainActivity.getPackageManager();
            if (Camera.getNumberOfCameras() > 1) {
                isBackCamera = true;
                return Camera.CameraInfo.CAMERA_FACING_BACK;
            } else if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)) {
                isBackCamera = false;
                return Camera.CameraInfo.CAMERA_FACING_FRONT;
            } else {
                isBackCamera = true;
                return Camera.CameraInfo.CAMERA_FACING_BACK;
            }
        } else {
            if(isBackCamera) {
                return Camera.CameraInfo.CAMERA_FACING_BACK;
            } else {
                return Camera.CameraInfo.CAMERA_FACING_FRONT;
            }
        }
    }

    void toggleCamera() {
        isBackCamera = !isBackCamera;
        mainActivity.openCamera(getCameraId());
    }
}
