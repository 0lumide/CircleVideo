package co.mide.circlevideo;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.TextureView;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.bumptech.glide.Glide;

import java.io.File;

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
            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                //noinspection deprecation
                relativeLayout.getViewTreeObserver().removeGlobalOnLayoutListener(this);
            } else {
                relativeLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }

            resizeTextureView();
            if(isVideo) {
                imageView.setVisibility(View.GONE);
            }else {
                textureView.setVisibility(View.GONE);
                loadImage(recordedFile);
            }
        }
    };

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
                .fitCenter()
                .load(file)
                .into(imageView);
    }

    @Override
    public void onResume() {
        super.onResume();
        overridePendingTransition(0, 0);
        setToolbarVisibility(false);
        rotationController.init();


        if(isVideo) {
            textureView.setOnClickListener((view) -> setToolbarVisibility(false));
        }else {
            imageView.setOnClickListener((view) -> setToolbarVisibility(false));
        }

        if(isFirstRun()) {
            showInfoDialog();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        rotationController.cleanUp();

        textureView.setOnClickListener(null);
        imageView.setOnClickListener(null);

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
        float ratioX, ratioY;
        float previewRatioX, previewRatioY;
        int screenWidth = getScreenWidth();
        int screenHeight = getScreenHeight();

        float squared = (float)(Math.pow(screenHeight, 2) + Math.pow(screenWidth, 2));
        float maxLength = (float)Math.sqrt(squared);

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
            float aspectRatio = divideInts(recordedWidth, recordedHeight);
            float newHeight = ratioY * screenHeight;
            float newWidth = aspectRatio * newHeight;
            ratioX = newWidth/screenWidth;

        } else {
            //this means the preview got cut on the top and bottom
            ratioX = previewRatioX;
            float aspectRatio = divideInts(recordedWidth, recordedHeight);
            float newWidth = ratioX * screenWidth;
            float newHeight = newWidth / aspectRatio;
            ratioY = newHeight / screenHeight;
        }
        if(isVideo) {
            textureView.setScaleX(ratioX);
            textureView.setScaleY(ratioY);
        } else {
            imageView.setScaleX(ratioX);
            imageView.setScaleY(ratioY);
        }
    }



    public int getScreenWidth() {
        return  ((RelativeLayout) textureView.getParent()).getWidth();
    }

    public int getScreenHeight() {
        return  ((RelativeLayout) textureView.getParent()).getHeight();
    }

    private float divideInts(int numerator, int denominator) {
        return ((float)numerator)/denominator;
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
