package co.mide.circlevideo;

import android.content.Context;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.bumptech.glide.Glide;

import java.io.File;
import java.io.IOException;

public class VideoActivity extends FullscreenActivity {
    final static String PREVIEW_WIDTH = "co.mide.circlevideo.VideoActivity.PREVIEW_WIDTH";
    final static String PREVIEW_HEIGHT = "co.mide.circlevideo.VideoActivity.PREVIEW_HEIGHT";
    final static String VIDEO_WIDTH = "co.mide.circlevideo.VideoActivity.VIDEO_WIDTH";
    final static String VIDEO_HEIGHT = "co.mide.circlevideo.VideoActivity.VIDEO_HEIGHT";
    final static String VIDEO_FILE = "co.mide.circlevideo.VideoActivity.VIDEO_FILE";
    final static String IS_VIDEO = "co.mide.circlevideo.VideoActivity.IS_VIDEO";

    private int previewWidth, previewHeight;
    private int recordedWidth, recordedHeight;
    private boolean isVideo;

    private File recordedFile;

    private RotationController rotationController;
    private TextureView textureView;
    private ImageView imageView;
    private AlertDialog dialog;
    RelativeLayout relativeLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        extractBundleExtras();

        textureView = (TextureView) findViewById(R.id.texture_view);
        imageView = (ImageView) findViewById(R.id.imageview_picture);
        relativeLayout = (RelativeLayout) findViewById(R.id.layout_root);
        findViewById(R.id.image_close).setOnClickListener((view) -> finish());

        relativeLayout.getViewTreeObserver().addOnGlobalLayoutListener(layoutListener);

        View view = isVideo ? textureView : imageView;
        rotationController = new RotationController(this, view::setRotation);
    }

    ViewTreeObserver.OnGlobalLayoutListener layoutListener
            = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            relativeLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);

            resizeTextureView();
            if(isVideo) {
                imageView.setVisibility(View.GONE);
            }else {
                textureView.setVisibility(View.GONE);
                loadImage(recordedFile);
            }
        }
    };

    MediaPlayer mediaPlayer;
    public void onSurfaceReady(SurfaceTexture surface) {
        Surface s = new Surface(surface);

        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setLooping(true);
            mediaPlayer.setDataSource(recordedFile.getAbsolutePath());
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setSurface(s);
            mediaPlayer.setOnErrorListener((mp, what, extra)->{
                //todo show error dialog
                return true;
            });
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IllegalStateException | IOException e) {
            // TODO show error dialog
            e.printStackTrace();
        }
    }
    private void extractBundleExtras() {
        previewHeight = getIntent().getIntExtra(PREVIEW_HEIGHT, -1);
        previewWidth = getIntent().getIntExtra(PREVIEW_WIDTH, -1);
        recordedWidth = getIntent().getIntExtra(VIDEO_WIDTH, -1);
        recordedHeight = getIntent().getIntExtra(VIDEO_HEIGHT, -1);
        recordedFile = new File(getIntent().getStringExtra(VIDEO_FILE));
        isVideo = getIntent().getBooleanExtra(IS_VIDEO, true);
    }

    private void loadImage(File file) {
        Glide.with(this)
                .fromFile()
                .load(file)
                .fitCenter()
                .into(imageView);
    }

    private TextureView.SurfaceTextureListener
            surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            onSurfaceReady(surface);
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

    @Override
    public void onResume() {
        super.onResume();
        overridePendingTransition(0, 0);
        setToolbarVisibility(false);
        rotationController.init();

        if(isVideo) {
            if (textureView.getSurfaceTexture() == null) {
                textureView.getSurfaceTexture();
                textureView.setSurfaceTextureListener(surfaceTextureListener);
            } else {
                onSurfaceReady(textureView.getSurfaceTexture());
            }
        }

        if(isFirstRun()) {
            showInfoDialog();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        rotationController.cleanUp();
        if(mediaPlayer != null) {
            mediaPlayer.stop();
        }

        if(isVideo) {
            textureView.setSurfaceTextureListener(null);
        }

        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        overridePendingTransition(0, 0);
    }

    static Intent getStartIntent(Context context, boolean isVideo, File file,
                                 int previewWidth, int previewHeight,
                                 int videoWidth, int videoHeight) {
        Intent intent = new Intent(context, VideoActivity.class);
        intent.putExtra(IS_VIDEO, isVideo);
        intent.putExtra(PREVIEW_HEIGHT, previewHeight);
        intent.putExtra(PREVIEW_WIDTH, previewWidth);
        intent.putExtra(VIDEO_HEIGHT, videoHeight);
        intent.putExtra(VIDEO_WIDTH, videoWidth);
        intent.putExtra(VIDEO_FILE, file.getAbsolutePath());
        return intent;
    }

    private void resizeTextureView() {
        double ratioX, ratioY;
        double previewRatioX, previewRatioY;
        int screenWidth = getScreenWidth();
        int screenHeight = getScreenHeight();

        Log.d("dbug", "orig aspect ratio: "+divideInts(recordedWidth, recordedHeight));
        double squared = Math.pow(screenHeight, 2) + Math.pow(screenWidth, 2);
        double maxLength = Math.sqrt(squared);

        if(previewWidth < previewHeight) {
            previewRatioX = maxLength/previewWidth;
            previewRatioY = (previewRatioX*previewHeight)/screenHeight;
        } else {
            previewRatioY = maxLength/previewHeight;
            previewRatioX = (previewRatioY*previewWidth)/screenWidth;
        }

        if(divideInts(previewHeight, previewWidth) > divideInts(recordedHeight, recordedWidth)) {
            //this means the preview got cut on the sides
            ratioY = previewRatioY;
            double aspectRatio = divideInts(recordedWidth, recordedHeight);
            double newHeight = ratioY * screenHeight;
            double newWidth = aspectRatio * newHeight;
            ratioX = newWidth / screenWidth;

        } else {
            //this means the preview got cut on the top and bottom
            ratioX = previewRatioX;
            double aspectRatio = divideInts(recordedWidth, recordedHeight);
            double newWidth = ratioX * screenWidth;
            double newHeight = newWidth / aspectRatio;
            ratioY = newHeight / screenHeight;
        }
        if(isVideo) {
            textureView.setScaleX((float) ratioX);
            textureView.setScaleY((float) ratioY);
        } else {
            imageView.setScaleX((float) ratioX);
            imageView.setScaleY((float) ratioY);
        }
        Log.d("dbug", "new aspect ratio: "+((ratioX*previewWidth)/(ratioY*previewHeight)));
    }



    public int getScreenWidth() {
        return  ((RelativeLayout) textureView.getParent()).getWidth();
    }

    public int getScreenHeight() {
        return  ((RelativeLayout) textureView.getParent()).getHeight();
    }

    private double divideInts(int numerator, int denominator) {
        return ((double)numerator)/denominator;
    }

    private void showInfoDialog() {
        dialog = new AlertDialog.Builder(this)
                .setMessage(R.string.first_run_message)
                .setNeutralButton(android.R.string.ok, (dialog, which) -> setFirstRunComplete())
                .create();
        dialog.show();
    }

    private final String SHARED_PREF_KEY = "co.mide.circlevideo.VideoActivity.SHARED_PREF";
    private final String IS_FIRST_RUN_KEY = "co.mide.circlevideo.VideoActivity.IS_FIRST_RUN";

    private boolean isFirstRun() {
        return getSharedPreferences(SHARED_PREF_KEY, MODE_PRIVATE)
                .getBoolean(IS_FIRST_RUN_KEY, true);
    }

    private void setFirstRunComplete() {
        getSharedPreferences(SHARED_PREF_KEY, MODE_PRIVATE)
                .edit()
                .putBoolean(IS_FIRST_RUN_KEY, false)
                .apply();
    }
}
