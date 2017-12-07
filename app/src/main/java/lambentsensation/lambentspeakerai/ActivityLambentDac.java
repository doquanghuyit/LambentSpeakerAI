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
import java.util.Base64;
import java.util.List;
import java.util.Objects;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;

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

    private Decoder mDecoder;
    private AudioTrack mAudioTrack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Audio routing configuration: use default routing.

        audioOutputDevice = findAudioDevice(AudioManager.GET_DEVICES_OUTPUTS, AudioDeviceInfo.TYPE_BUS);
        if (audioOutputDevice == null) {
            Log.e(TAG, "failed to found I2S audio output device, using default");
        }else {
            Log.e(TAG,"getType::::"+audioOutputDevice.getType());
            Log.e(TAG,"getId::::"+audioOutputDevice.getId());
            Log.e(TAG,"getProductName::::"+audioOutputDevice.getProductName());
            Log.e(TAG,"getSampleRates::::"+audioOutputDevice.getSampleRates().length);
            Log.e(TAG, "found I2S audio output device");
        }



//        readMediaInfo();
//        init();
//
//        try {
//            play();
//        }catch (Exception e)
//        {
//            Log.e(TAG, "Exception::",e);
//        }

        playMp3();


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

//        bitRate = mf.getInteger(MediaFormat.KEY_BIT_RATE);
        bitRate =192;
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
                AudioFormat.CHANNEL_IN_STEREO,
                audioEncoding);
    }

    public void play() throws IOException {

        // create a new AudioTrack to workaround audio routing issues.
        AudioTrack audioTrack = new AudioTrack.Builder()
                .setAudioFormat(mAudioOutputFormat)
                .setBufferSizeInBytes(mAudioOutputBufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build();
//        if (audioOutputDevice != null) {
//            audioTrack.setPreferredDevice(audioOutputDevice);
//        }
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


    public void playMp3()
    {
        final int sampleRate = 44100;
        final int minBufferSize = AudioTrack.getMinBufferSize(sampleRate,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT);

        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufferSize,
                AudioTrack.MODE_STREAM);

        if (audioOutputDevice != null) {
            mAudioTrack.setPreferredDevice(audioOutputDevice);
        }

        mDecoder = new Decoder();
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    InputStream in = getResources().openRawResource(R.raw.music);
                    Bitstream bitstream = new Bitstream(in);

                    final int READ_THRESHOLD = 2147483647;
                    int framesReaded = READ_THRESHOLD;

                    Header header;
                    for(; framesReaded-- > 0 && (header = bitstream.readFrame()) != null;) {
                        SampleBuffer sampleBuffer = (SampleBuffer) mDecoder.decodeFrame(header, bitstream);
                        short[] buffer = sampleBuffer.getBuffer();
                        mAudioTrack.write(buffer, 0, buffer.length);
                        bitstream.closeFrame();
                    }

                    mAudioTrack.stop();
                } catch (Exception e) {
                    e.printStackTrace();
                    mAudioTrack.stop();
                }
            }
        });
        thread.start();

        mAudioTrack.play();
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
        AudioDeviceInfo adireturn = null;
        Log.e(TAG,"adis size::"+adis.length+"::deviceType::"+deviceType);
        for (AudioDeviceInfo adi : adis) {
            Log.e(TAG,"getType::"+adi.getType());
            Log.e(TAG,"getId::"+adi.getId());
            Log.e(TAG,"getProductName::"+adi.getProductName());
            Log.e(TAG,"getSampleRates::"+adi.getSampleRates().length);
            if (adi.getType() == deviceType) {
                adireturn =  adi;
            }
        }
        return adireturn;
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");

        // we intentionally leave the Bluetooth adapter enabled, so that other samples can use it
        // without having to initialize it.
    }




}