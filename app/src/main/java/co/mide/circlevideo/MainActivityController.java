package co.mide.circlevideo;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.ExifInterface;
import android.util.Log;
import android.view.TextureView;
import android.view.View;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

@SuppressWarnings("deprecation")
class MainActivityController {
    private MainActivity mainActivity;
    private int[] videoSize = new int[2];
    private int[] imageSize = new int[2];

    MainActivityController(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    private View.OnClickListener textureSurfaceClickListener = (view) -> {
        if(mainActivity.getCamera() != null) {
            mainActivity.getCamera().autoFocus(null);
        }
        mainActivity.setToolbarVisibility(false);
    };

    View.OnClickListener getTextureClickListener() {
        return textureSurfaceClickListener;
    }

    private RecordButton.RecordButtonListener recordListener
            = new RecordButton.RecordButtonListener() {
        @Override
        public void onStartRecording() {
            Log.d("dbug", "start");
        }

        @Override
        public void onStopRecording() {
            Log.d("dbug", "end");
            mainActivity.launchVideoActivity();
        }

        @Override
        public void onSingleTap() {
            mainActivity.getCamera().takePicture(null, null, (data, camera) -> {
                FileOutputStream fos = null;
                try {
                    File file = new File(mainActivity.getFilesDir(),
                            System.currentTimeMillis() + ".jpg");
                    fos = mainActivity.openFileOutput(file.getName(), Context.MODE_PRIVATE);
                    fos.write(data);
                    ExifInterface exif = new ExifInterface(file.getAbsolutePath());
                    exif.setAttribute(ExifInterface.TAG_ORIENTATION,
                            String.valueOf(ExifInterface.ORIENTATION_ROTATE_90));
                    exif.saveAttributes();
                    mainActivity.setCapturedFile(file);
                    mainActivity.launchVideoActivityForImage();
                } catch (IOException e) {
                    e.printStackTrace();
                    mainActivity.showErrorMessage();
                } finally {
                    if(fos != null) {
                        try {
                            fos.close();
                        }catch (IOException e) {
                            //Like seriously??>
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
            Log.d("dbug", "created");
            mainActivity.setSurface(surface);
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
            Log.d("dbug", "destroyed");
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

    int[] getVideoSize() {
        return videoSize.clone();
    }

    int[] getImageSize() {
        return imageSize.clone();
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
        //switched width and height for the same reason
        videoSize[0] = size[1];
        videoSize[1] = size[0];
        //todo setMediarecorder stuff

        size = getGoodSize(param.getSupportedPictureSizes());
        param.setPictureSize(size[0], size[1]);
        //switched width and height for the same reason
        imageSize[0] = size[1];
        imageSize[1] = size[0];
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
}
