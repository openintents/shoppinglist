package com.example.mymirrorlinkapplication;

import android.content.res.AssetFileDescriptor;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import com.mirrorlink.android.commonapi.Defs;
import com.mirrorlink.android.commonapi.IConnectionListener;
import com.mirrorlink.android.commonapi.IConnectionManager;
import com.mirrorlink.lib.MirrorLinkApplicationContext;
import com.mirrorlink.lib.ServiceReadyCallback;

import java.io.IOException;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {
    final String TAG = "MainActivityFragment";
    MirrorLinkApplicationContext mMirrorLinkContext = null;
    MirrorLinkPlayer mMirrorLinkPlayer = null;
    CheckBox mMirrorLinkConnectionStatus = null;
    Button mPlayButton = null;
    Button mStopButton = null;

    static final int[] mAudioCategories = new int[] { Defs.ContextInformation.APPLICATION_CATEGORY_MEDIA_MUSIC };

    public MainActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_main, container, false);
        mMirrorLinkConnectionStatus = (CheckBox) v.findViewById(R.id.isMirrorLinkConnected);
        mPlayButton = (Button) v.findViewById(R.id.playButton);
        mStopButton = (Button) v.findViewById(R.id.stopButton);

        mPlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    AssetFileDescriptor audioFile = getActivity().getAssets().openFd("bensound-theelevatorbossanova.mp3");
                    mMirrorLinkPlayer.play(mAudioCategories, audioFile.getFileDescriptor());
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        });

        mStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMirrorLinkPlayer.stop();
            }
        });



        return v;
    }
    IConnectionManager mConnectionManager = null;
    IConnectionListener mConnectionManagerListener = new IConnectionListener.Stub() {
        @Override
        public void onMirrorLinkSessionChanged(boolean connected) throws RemoteException {
            showMirrorLinkConnectionStatus(connected);
        }

        @Override
        public void onAudioConnectionsChanged(Bundle bundle) throws RemoteException {

        }

        @Override
        public void onRemoteDisplayConnectionChanged(int i) throws RemoteException {

        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        mMirrorLinkPlayer = new MirrorLinkPlayer(getActivity());

        ApplicationContext.getContext(new ServiceReadyCallback() {
            @Override
            public void connected(MirrorLinkApplicationContext mirrorLinkApplicationContext) {
                mConnectionManager = mirrorLinkApplicationContext.registerConnectionManager(this, mConnectionManagerListener);

                try {
                    showMirrorLinkConnectionStatus(mConnectionManager.isMirrorLinkSessionEstablished());
                }catch (RemoteException e){
                    e.printStackTrace();
                }
                Log.d(TAG, "Service connected and ready to use");
            }
        });
    }

    private void showMirrorLinkConnectionStatus(boolean connected){
        if (mMirrorLinkConnectionStatus != null)
            mMirrorLinkConnectionStatus.setChecked(connected);
        if (connected)
            Toast.makeText(getActivity(), "MirrorLink is connected", Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(getActivity(), "MirrorLink is not connected", Toast.LENGTH_SHORT).show();
    }


    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if (mMirrorLinkContext != null) {
            mMirrorLinkContext.unregisterConnectionManager(this, mConnectionManagerListener);
        }
        mMirrorLinkPlayer.release();
        mMirrorLinkPlayer = null;
    }
}
