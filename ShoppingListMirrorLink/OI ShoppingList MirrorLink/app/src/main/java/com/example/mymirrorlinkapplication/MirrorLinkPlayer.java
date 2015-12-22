package com.example.mymirrorlinkapplication;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

import com.mirrorlink.android.commonapi.Defs;
import com.mirrorlink.android.commonapi.IConnectionListener;
import com.mirrorlink.android.commonapi.IConnectionManager;
import com.mirrorlink.android.commonapi.IContextListener;
import com.mirrorlink.android.commonapi.IContextManager;
import com.mirrorlink.lib.MirrorLinkApplicationContext;
import com.mirrorlink.lib.ServiceReadyCallback;

import java.io.FileDescriptor;

public class MirrorLinkPlayer {
    final String TAG = "MirrorLinkPlayer";
    MirrorLinkApplicationContext mContext = null;
    MediaPlayer mMediaPlayer = new MediaPlayer();
    AudioManager mAudioManager = null;
    int[] mAudioCategories = new int[]{};
    boolean mMirrorLinkActive = false;
    boolean mMirrorLinkMediaOutActive = false;
    boolean mMirrorLinkBlocked = false;
    boolean mMirrorLinkPlayingWhenBlocked = false;

    IContextManager mContextManager = null;
    IContextListener mContextListener = new IContextListener.Stub() {
        @Override
        public void onFramebufferBlocked(int i, Bundle bundle) throws RemoteException {

        }

        @Override
        public void onAudioBlocked(int i) throws RemoteException {
            Log.d(TAG, "onAudioBlocked " + i);
            mMirrorLinkBlocked = true;
            if (!mMirrorLinkPlayingWhenBlocked)
                mMirrorLinkPlayingWhenBlocked = mMediaPlayer.isPlaying();
            mMediaPlayer.pause();
        }

        @Override
        public void onFramebufferUnblocked() throws RemoteException {

        }

        @Override
        public void onAudioUnblocked() throws RemoteException {
            Log.d(TAG, "onAudioUnBlocked ");
            if(mMirrorLinkBlocked && mMirrorLinkPlayingWhenBlocked) {
                mMirrorLinkPlayingWhenBlocked = false;
                mMediaPlayer.start();
            }
        }
    };


    IConnectionManager mConnectionManager = null;
    IConnectionListener mConnectionManagerListener = new IConnectionListener.Stub() {
        @Override
        public void onMirrorLinkSessionChanged(boolean connected) throws RemoteException {
            if(mMirrorLinkActive != connected) {
                mMirrorLinkActive = connected;
                if(!mMirrorLinkActive) {
                    mMediaPlayer.pause();
                }
            }
        }

        @Override
        public void onAudioConnectionsChanged(Bundle bundle) throws RemoteException {
            boolean mediaOutStatus =bundle.getInt(Defs.AudioConnections.MEDIA_AUDIO_OUT, Defs.AudioConnections.MEDIA_OUT_NONE) != Defs.AudioConnections.MEDIA_OUT_NONE;

            if(mediaOutStatus != mMirrorLinkMediaOutActive) {
                mMirrorLinkMediaOutActive = mediaOutStatus;
                if (mMirrorLinkActive && !mMirrorLinkMediaOutActive) {
                    mMediaPlayer.pause();
                }
            }
        }

        @Override
        public void onRemoteDisplayConnectionChanged(int i) throws RemoteException {

        }
    };
    MirrorLinkPlayer(Context c){
        mAudioManager = (AudioManager)c.getSystemService(Context.AUDIO_SERVICE);

        ApplicationContext.getContext(new ServiceReadyCallback() {
            @Override
            public void connected(MirrorLinkApplicationContext mirrorLinkApplicationContext) {
                mContext = mirrorLinkApplicationContext;
                mContextManager = mirrorLinkApplicationContext.registerContextManager(this, mContextListener);
                mConnectionManager = mirrorLinkApplicationContext.registerConnectionManager(this, mConnectionManagerListener);
            }
        });
    }

    void play(int[] audioCategories, FileDescriptor f){
        try {
            if (AudioManager.AUDIOFOCUS_REQUEST_GRANTED == mAudioManager.requestAudioFocus(mAudioFocusListener,
                                                                AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN))
            {
                mMirrorLinkBlocked = false;
                mAudioCategories = audioCategories;
                mMediaPlayer.reset();
                mMediaPlayer.setDataSource(f);
                mMediaPlayer.prepare();
                mMediaPlayer.start();
                mMediaPlayer.setLooping(true);
                if (mContextManager != null) {
                    mContextManager.setAudioContextInformation(true, mAudioCategories, true);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    void stop(){
        if (mMediaPlayer != null){
            mMediaPlayer.stop();
            mMediaPlayer.reset();

            if(mContextManager !=null){
                try {
                    mContextManager.setAudioContextInformation(false, mAudioCategories, true);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }

    void release(){
        mMediaPlayer.release();
        mMediaPlayer = null;
        try {
            mContextManager.setAudioContextInformation(false, mAudioCategories, true);
        }catch (RemoteException e){
            e.printStackTrace();
        }
        mContext.unregisterContextManager(this, mContextListener);
        mContext.unregisterConnectionManager(this, mConnectionManagerListener);
        mAudioManager.abandonAudioFocus(mAudioFocusListener);
    }

    AudioManager.OnAudioFocusChangeListener mAudioFocusListener = new AudioManager.OnAudioFocusChangeListener() {
        public void onAudioFocusChange(int focusChange) {

            Log.d(TAG, "onAudioFocusChange" + focusChange);
            if (mMediaPlayer == null)
                return;

            switch(focusChange) {
                case AudioManager.AUDIOFOCUS_LOSS:
                    if (mMediaPlayer.isPlaying())
                        mMediaPlayer.pause();
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    //mMediaPlayer.fadedown();
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    if (mMediaPlayer.isPlaying())
                        mMediaPlayer.pause();
                    break;
                case AudioManager.AUDIOFOCUS_GAIN:
                    if (!mMediaPlayer.isPlaying())
                        mMediaPlayer.start();
                    break;
            }
        }
    };



}
