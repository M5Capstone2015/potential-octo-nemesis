package com.example.hunt.comtesting;

import android.app.Activity;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;


public class Main extends Activity {

    TextView dataTextView = null;
    TextView hijackTextView = null;
    TextView phoneDataTextView = null;
    private static int[] mSampleRates = new int[] {8000, 11025, 22050, 44100};
    AudioRecord _aru = null;
    AudioReceiver reader = null;
    short[] buffer = null;
    List<Short> data = new ArrayList<Short>();

    public void saveClick(View v) {
        String text = dataTextView.getText().toString();
        WriteToFile(text);
    }


    public void readClick(View v)
    {
        int shortsRead = 0;
        // Run AudioRecord and Save to file. Print average to and num read.
        if (_aru != null)
        {
            _aru.startRecording();
            shortsRead = _aru.read(buffer, 0, buffer.length);

            //double average = Average(buffer);
            //textView.setText(String.valueOf(shortsRead) + "\nAverage: " + String.valueOf(average));
        }
        else
        {
            //textView.setText("AudioRecord is null");
        }

        data.clear();
        String text = "";
        if (buffer.length > 10)
            for (int i = 0; i < shortsRead; i++)
            {
                data.add(buffer[i]);
                String num = "" + i;
                String val = Short.toString(buffer[i]);
                text += "\n" + num + ", " + val;
            }
        //processInputBuffer(shortsRead2);
        dataTextView.setText(text);
    }

    public void clearClick(View v) {
        dataTextView.setText("");
        hijackTextView.setText("");
        data.clear();
    }

    public void processClick(View v) {
        if (reader != null)
        {
            reader.startAudioIO();

            long startTime = System.nanoTime(); // Start timer
            List<Integer> freqs = reader.fakeAudioRead(data);
            long endTime = System.nanoTime();
            long duration = (endTime - startTime) /1000000;

            String text = "\nHiJack Data:";
            text += ("\nNum freq coefficients:  " + String.valueOf(freqs.size()));
            text += ("\nCalculation time:" + String.valueOf(duration) + " ms");

            for (int i : freqs)
                text = text + "\n" + String.valueOf(i);

            hijackTextView.setText(text);
        }
        else {
            hijackTextView.setText("\nReader object null.");
        }
    }

    private void initialize()
    {
        int recBufferSize =
                AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        buffer = new short[recBufferSize * 10];
        _aru = findAudioRecord();
        reader = new AudioReceiver(_aru);

        dataTextView = (TextView) findViewById(R.id.dataText);
        hijackTextView = (TextView) findViewById(R.id.hijackData);
        phoneDataTextView = (TextView) findViewById(R.id.phoneData);

        int fre =_aru.getSampleRate();
        phoneDataTextView.setText("Sample Frequency:  " + String.valueOf(fre));
    }

    private double Average(short[] bytes)
    {
        double sum = 0;
        for (int i = 0; i < bytes.length; i++) {
            Short s = bytes[i];
            sum = sum + s.doubleValue();
        }
        return sum / bytes.length;
    }

    public void WriteToFile(String content)
    {
        OutputStream fos;
        try {
            fos = openFileOutput("dump.csv", Context.MODE_WORLD_READABLE);
            fos.write(content.getBytes());
            fos.close();
        }
        catch (FileNotFoundException e)
        {
        }
        catch (IOException e)
        {
        }
    }

    private AudioRecord findAudioRecord()
    {
        for (int rate : mSampleRates)
        {
            for (short audioFormat : new short[] { AudioFormat.ENCODING_PCM_8BIT, AudioFormat.ENCODING_PCM_16BIT })
            {
                for (short channelConfig : new short[] { AudioFormat.CHANNEL_IN_MONO, AudioFormat.CHANNEL_IN_STEREO })
                {
                    try {
                        //Log.d(C.TAG, "Attempting rate " + rate + "Hz, bits: " + audioFormat + ", channel: "
                        //+ channelConfig);
                        int bufferSize = AudioRecord.getMinBufferSize(rate, channelConfig, audioFormat);

                        if (bufferSize != AudioRecord.ERROR_BAD_VALUE) {
                            // check if we can instantiate and have a success
                            AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, rate, channelConfig, audioFormat, bufferSize);

                            if (recorder.getState() == AudioRecord.STATE_INITIALIZED)
                                return recorder;
                        }
                    } catch (Exception e) {
                        //Log.e(C.TAG, rate + "Exception, keep trying.",e);
                    }
                }
            }
        }
        return null;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initialize();
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

