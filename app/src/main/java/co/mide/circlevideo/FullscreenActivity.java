package co.mide.circlevideo;

import android.os.Build;
import android.os.Bundle;
import android.app.Activity;
import android.view.View;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.CompositeSubscription;

public class FullscreenActivity extends Activity {
    private CompositeSubscription subscriptions = new CompositeSubscription();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @SuppressWarnings("unused")
    void setToolbarVisibility(boolean makeVisible) {
        if(getWindow() == null) {
            return;
        }
        if (makeVisible) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        } else {
            int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                    | View.SYSTEM_UI_FLAG_FULLSCREEN; // hide status bar
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                uiOptions = uiOptions | View.SYSTEM_UI_FLAG_IMMERSIVE;
            }
            getWindow().getDecorView().setSystemUiVisibility(uiOptions);
        }
    }

    private void setupActionBarListener() {
        View decorView = getWindow().getDecorView();
        decorView.setOnSystemUiVisibilityChangeListener(visibility -> {
            if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                subscriptions.add(Observable.timer(2, TimeUnit.SECONDS)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe((time) -> setToolbarVisibility(false)));
            }
        });
    }

    @Override
    protected void onResume(){
        super.onResume();
        setupActionBarListener();
    }

    @Override
    protected void onPause() {
        super.onPause();
        subscriptions.clear();
    }
}
