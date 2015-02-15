package com.dd.widget;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/*
 *    The MIT License (MIT)
 *
 *   Copyright (c) 2014 Danylyk Dmytro
 *   Copyright (c) 2014 Jacob Tabak
 *
 *   Permission is hereby granted, free of charge, to any person obtaining a copy
 *   of this software and associated documentation files (the "Software"), to deal
 *   in the Software without restriction, including without limitation the rights
 *   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the Software is
 *   furnished to do so, subject to the following conditions:
 *
 *   The above copyright notice and this permission notice shall be included in all
 *   copies or substantial portions of the Software.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *   IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *   FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *   AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *   LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *   OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *   SOFTWARE.
 */
public class TextureVideoView extends TextureView
        implements TextureView.SurfaceTextureListener {

    // Log tag
    private static final String TAG = TextureVideoView.class.getName();

    private MediaPlayer mMediaPlayer;

    private float mVideoWidth;
    private float mVideoHeight;

    private boolean mIsPlayCalled;
    private boolean mPrepared;
    private Surface mSurface;
    private ScaleType mScaleType;

    private MediaPlayer.OnInfoListener mOnInfoListener;
    private MediaPlayer.OnPreparedListener mOnPreparedListener;
    private MediaPlayer.OnCompletionListener mOnCompletionListener;
    private MediaPlayer.OnErrorListener mOnErrorListener;

    public enum ScaleType {
        CENTER_CROP, TOP, BOTTOM
    }

    public TextureVideoView(Context context) {
        super(context);
        initView();
    }

    public TextureVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public TextureVideoView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView();
    }

    private void initView() {
        // Support for Android studio designer tool
        if (isInEditMode()) return;

        release();
        setScaleType(ScaleType.CENTER_CROP);
        setSurfaceTextureListener(this);
    }

    public void setScaleType(ScaleType scaleType) {
        mScaleType = scaleType;
    }

    private void updateTextureViewSize() {
        float viewWidth = getWidth();
        float viewHeight = getHeight();

        float scaleX = 1.0f;
        float scaleY = 1.0f;

        if (mVideoWidth > viewWidth && mVideoHeight > viewHeight) {
            scaleX = mVideoWidth / viewWidth;
            scaleY = mVideoHeight / viewHeight;
        } else if (mVideoWidth < viewWidth && mVideoHeight < viewHeight) {
            scaleY = viewWidth / mVideoWidth;
            scaleX = viewHeight / mVideoHeight;
        } else if (viewWidth > mVideoWidth) {
            scaleY = (viewWidth / mVideoWidth) / (viewHeight / mVideoHeight);
        } else if (viewHeight > mVideoHeight) {
            scaleX = (viewHeight / mVideoHeight) / (viewWidth / mVideoWidth);
        }

        // Calculate pivot points, in our case crop from center
        int pivotPointX;
        int pivotPointY;

        switch (mScaleType) {
            case TOP:
                pivotPointX = 0;
                pivotPointY = 0;
                break;
            case BOTTOM:
                pivotPointX = (int) (viewWidth);
                pivotPointY = (int) (viewHeight);
                break;
            case CENTER_CROP:
                pivotPointX = (int) (viewWidth / 2);
                pivotPointY = (int) (viewHeight / 2);
                break;
            default:
                pivotPointX = (int) (viewWidth / 2);
                pivotPointY = (int) (viewHeight / 2);
                break;
        }

        Matrix matrix = new Matrix();
        matrix.setScale(scaleX, scaleY, pivotPointX, pivotPointY);

        setTransform(matrix);
    }

    private void release() {
        if (mMediaPlayer != null) {
            mMediaPlayer.setSurface(null);
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        mPrepared = false;
    }

    /**
     * Sets video path.
     *
     * @param path the path of the video.
     */
    public void setVideoPath(File path) {
        setVideoURI(Uri.fromFile(path));
    }

    /**
     * Sets video URI.
     *
     * @param uri the URI of the video.
     */
    public void setVideoURI(Uri uri) {
        setVideoURI(uri, null);
    }

    /**
     * Sets video URI using specific headers.
     *
     * @param uri     the URI of the video.
     * @param headers the headers for the URI request.
     *                Note that the cross domain redirection is allowed by default, but that can be
     *                changed with key/value pairs through the headers parameter with
     *                "android-allow-cross-domain-redirect" as the key and "0" or "1" as the value
     *                to disallow or allow cross domain redirection.
     */
    public void setVideoURI(Uri uri, Map<String, String> headers) {
        prepare(uri, headers);
    }

    private void prepare(Uri uri, Map<String, String> headers) {
        release();
        try {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setOnInfoListener(new MediaPlayer.OnInfoListener() {
                @Override
                public boolean onInfo(MediaPlayer mp, int what, int extra) {
                    if (mOnInfoListener != null) {
                        return mOnInfoListener.onInfo(mp, what, extra);
                    }
                    return false;
                }
            });
            mMediaPlayer.setOnVideoSizeChangedListener(
                    new MediaPlayer.OnVideoSizeChangedListener() {
                        @Override
                        public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
                            mVideoWidth = width;
                            mVideoHeight = height;
                            updateTextureViewSize();
                        }
                    }
            );
            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mPrepared = true;

                    if (mIsPlayCalled && mSurface != null) {
                        mMediaPlayer.setSurface(mSurface);
                        Log.d(TAG, "Player is prepared, playing video.");
                        start();
                    }

                    if (mOnPreparedListener != null) {
                        mOnPreparedListener.onPrepared(mp);
                    }
                }
            });
            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    if (mOnCompletionListener != null) {
                        mOnCompletionListener.onCompletion(mp);
                    }
                }
            });
            mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    if (mOnErrorListener != null) {
                        return mOnErrorListener.onError(mp, what, extra);
                    }
                    return false;
                }
            });

            mMediaPlayer.setDataSource(getContext(), uri, headers);
            // Manual preparation because we haven't used MediaPlayer.create
            mMediaPlayer.prepareAsync();

        } catch (IOException e) {
            Log.d(TAG, e.getMessage());
        } catch (IllegalStateException e) {
            Log.d(TAG, e.toString());
        }
    }

    /**
     * Play or resume video. Video will be played as soon as view is available and media player is
     * prepared.
     *
     * If video is stopped or ended and start() method was called, video will start over.
     */
    public void start() {
        mIsPlayCalled = true;
        if (isInPlaybackState() && !mMediaPlayer.isPlaying()) {
            mIsPlayCalled = false;
            mMediaPlayer.start();
        }
    }

    /**
     * Pause video.
     * If video is already paused, stopped or ended nothing will happen.
     */
    public void pause() {
        mIsPlayCalled = false;
        if (isInPlaybackState()) {
            mMediaPlayer.pause();
        }
    }

    /**
     * Stop video.
     * If video is already stopped or ended nothing will happen.
     */
    public void stop() {
        mIsPlayCalled = false;
        if (isInPlaybackState()) {
            mMediaPlayer.seekTo(0);
            mMediaPlayer.pause();
        }
    }

    public int getDuration() {
        if (isInPlaybackState()) {
            return mMediaPlayer.getDuration();
        }
        return -1;
    }

    public int getCurrentPosition() {
        if (isInPlaybackState()) {
            return mMediaPlayer.getCurrentPosition();
        }
        return 0;
    }

    public void seekTo(int pos) {
        if (isInPlaybackState()) {
            mMediaPlayer.seekTo(pos);
        }
    }

    public boolean isPlaying() {
        return isInPlaybackState() && mMediaPlayer.isPlaying();
    }

    private boolean isInPlaybackState() {
        return mMediaPlayer != null && mPrepared;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        mSurface = new Surface(surfaceTexture);
        if (isInPlaybackState()) {
            mMediaPlayer.setSurface(mSurface);
            if (mIsPlayCalled) {
                Log.d(TAG, "View is available and start() was called.");
                start();
            }
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        mSurface = null;
        if (mMediaPlayer != null) {
            release();
        }
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }

    public void setOnErrorListener(MediaPlayer.OnErrorListener onErrorListener) {
        this.mOnErrorListener = onErrorListener;
    }

    public void setOnCompletionListener(MediaPlayer.OnCompletionListener onCompletionListener) {
        this.mOnCompletionListener = onCompletionListener;
    }

    public void setOnPreparedListener(MediaPlayer.OnPreparedListener onPreparedListener) {
        this.mOnPreparedListener = onPreparedListener;
    }

    public void setOnInfoListener(MediaPlayer.OnInfoListener onInfoListener) {
        this.mOnInfoListener = onInfoListener;
    }
}
