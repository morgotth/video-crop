package com.dd.sample.crop;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.SpinnerAdapter;
import android.widget.Toast;

import com.dd.widget.TextureVideoView;

public class SampleActivity extends Activity implements View.OnClickListener,
        ActionBar.OnNavigationListener {

    private Uri video;
    private TextureVideoView mTextureVideoView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        video = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.mov_bbb);

        initView();
        initActionBar();

        if (!isWIFIOn(getBaseContext())) {
            Toast.makeText(getBaseContext(), "You need internet connection to stream video",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void initActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        actionBar.setDisplayShowTitleEnabled(false);

        SpinnerAdapter mSpinnerAdapter = ArrayAdapter.createFromResource(this, R.array.action_list,
                android.R.layout.simple_spinner_dropdown_item);
        actionBar.setListNavigationCallbacks(mSpinnerAdapter, this);
    }

    private void initView() {
        mTextureVideoView = (TextureVideoView) findViewById(R.id.cropTextureView);

        findViewById(R.id.btnPlay).setOnClickListener(this);
        findViewById(R.id.btnPause).setOnClickListener(this);
        findViewById(R.id.btnStop).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnPlay:
                mTextureVideoView.start();
                break;
            case R.id.btnPause:
                mTextureVideoView.pause();
                break;
            case R.id.btnStop:
                mTextureVideoView.stop();
                break;
        }
    }

    final int indexCropCenter = 0;
    final int indexCropTop = 1;
    final int indexCropBottom = 2;

    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        switch (itemPosition) {
            case indexCropCenter:
                mTextureVideoView.stop();
                mTextureVideoView.setScaleType(TextureVideoView.ScaleType.CENTER_CROP);
                mTextureVideoView.setVideoURI(video);
                mTextureVideoView.start();
                break;
            case indexCropTop:
                mTextureVideoView.stop();
                mTextureVideoView.setScaleType(TextureVideoView.ScaleType.TOP);
                mTextureVideoView.setVideoURI(video);
                mTextureVideoView.start();
                break;
            case indexCropBottom:
                mTextureVideoView.stop();
                mTextureVideoView.setScaleType(TextureVideoView.ScaleType.BOTTOM);
                mTextureVideoView.setVideoURI(video);
                mTextureVideoView.start();
                break;
        }
        return true;
    }

    public static boolean isWIFIOn(Context context) {
        ConnectivityManager connMgr =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        return (networkInfo != null && networkInfo.isConnected());
    }
}
