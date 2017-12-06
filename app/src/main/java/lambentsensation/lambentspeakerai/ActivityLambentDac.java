package lambentsensation.lambentspeakerai;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import com.google.android.things.bluetooth.BluetoothProfileManager;
import com.google.android.things.pio.Gpio;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ActivityLambentDac extends Activity {
    private static final String TAG = "ActivityLambentDac";

    // List & adapter to store and display the history of Assistant Requests.

    private AudioFormat mAudioOutputFormat;
    private static final int SAMPLE_RATE = 16000;
    private static final int DEFAULT_VOLUME = 100;
    private int bitRate;
    private int sampleRate;
    private int channelMask = 2;
    private int encoding;
    private int mAudioOutputBufferSize;
    private AudioDeviceInfo audioInputDevice = null;
    private AudioDeviceInfo audioOutputDevice = null;
    private ArrayList<ByteBuffer> mAssistantResponses = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Audio routing configuration: use default routing.
        audioInputDevice = findAudioDevice(AudioManager.GET_DEVICES_INPUTS, AudioDeviceInfo.TYPE_BUILTIN_SPEAKER);
        if (audioInputDevice == null) {
            Log.e(TAG, "failed to find I2S audio input device, using default");
        }else {
            Log.e(TAG, "found I2S audio input device");
        }
        audioOutputDevice = findAudioDevice(AudioManager.GET_DEVICES_OUTPUTS, AudioDeviceInfo.TYPE_BUILTIN_SPEAKER);
        if (audioOutputDevice == null) {
            Log.e(TAG, "failed to found I2S audio output device, using default");
        }else {
            Log.e(TAG, "found I2S audio output device");
        }



        readMediaInfo();
        init();

        try {
            play();
        }catch (Exception e)
        {
            Log.e(TAG, "Exception::",e);
        }


    }



    private void readMediaInfo()
    {
        MediaExtractor mex = new MediaExtractor();
        try {
            Uri uri=Uri.parse("android.resource://"+getPackageName()+"/raw/music");
            mex.setDataSource(this,uri,null);
//            mex.setDataSource(R.raw.music);// the adresss location of the sound on sdcard.
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        MediaFormat mf = mex.getTrackFormat(0);

        bitRate = mf.getInteger(MediaFormat.KEY_BIT_RATE);
        sampleRate = mf.getInteger(MediaFormat.KEY_SAMPLE_RATE);
//        channelMask =  mf.getInteger(MediaFormat.KEY_CHANNEL_MASK);
        encoding =  AudioFormat.ENCODING_PCM_16BIT;

        Log.e(TAG,"bitrate::"+bitRate);
        Log.e(TAG,"sampleRate::"+sampleRate);
        Log.e(TAG,"channelMask::"+channelMask);
    }




    private void init()
    {
        final int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
        mAudioOutputFormat = new AudioFormat.Builder()
                .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
                .setEncoding(audioEncoding)
                .setSampleRate(sampleRate)
                .build();

        mAudioOutputBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                channelMask,
                audioEncoding);
    }

    public void play() throws IOException {

        // create a new AudioTrack to workaround audio routing issues.
        AudioTrack audioTrack = new AudioTrack.Builder()
                .setAudioFormat(mAudioOutputFormat)
                .setBufferSizeInBytes(mAudioOutputBufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build();
        if (audioOutputDevice != null) {
            audioTrack.setPreferredDevice(audioOutputDevice);
        }
        audioTrack.setVolume(AudioTrack.getMaxVolume());
        audioTrack.play();

        Log.e(TAG,"Start");
        InputStream ins = getResources().openRawResource(R.raw.music);
        int size = 0;
        // Read the entire resource into a local byte buffer.
        byte[] buffer = inputStreamToByteArray(ins);
        Log.e(TAG,"buffer size::"+buffer.length);
        ByteBuffer bbInt = ByteBuffer.wrap(buffer);
        audioTrack.write(bbInt, bbInt.remaining(),
                AudioTrack.WRITE_BLOCKING);

//        while((size=ins.read(buffer,0,1))>=0){
//            Log.e(TAG,"hello");
//
//            audioTrack.write(buffer, size,
//                    AudioTrack.WRITE_BLOCKING);
//        }

        Log.e(TAG,"End");

//        for (ByteBuffer audioData : mAssistantResponses) {
//            final ByteBuffer buf = audioData;
//            mConversationHandler.post(new Runnable() {
//                @Override
//                public void run() {
//                    mConversationCallback.onAudioSample(buf);
//                }
//            });

//        }

        audioTrack.stop();
        audioTrack.release();
    }


    public byte[] inputStreamToByteArray(InputStream inStream) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = inStream.read(buffer)) > 0) {
            baos.write(buffer, 0, bytesRead);
        }
        return baos.toByteArray();
    }


    private AudioDeviceInfo findAudioDevice(int deviceFlag, int deviceType) {
        AudioManager manager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        AudioDeviceInfo[] adis = manager.getDevices(deviceFlag);
        for (AudioDeviceInfo adi : adis) {
            if (adi.getType() == deviceType) {
                return adi;
            }
        }
        return null;
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");

        // we intentionally leave the Bluetooth adapter enabled, so that other samples can use it
        // without having to initialize it.
    }




}