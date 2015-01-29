package com.example.hunt.comtesting;

import android.app.Activity;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
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
    TextView hijackStats = null;
    //private static int[] mSampleRates = new int[] {8000, 11025, 22050, 44100};
    private static int[] mSampleRates = new int[] {44100, 22050, 11025, 8000};
    AudioRecord _aru = null;
    AudioReceiver reader = null;
    short[] buffer = null;

    List<Short> data = new ArrayList<>();

    public void saveClick(View v) {
        String text = dataTextView.getText().toString();
        String hijackText = hijackTextView.getText().toString();
        WriteToFile(text, "dump.csv");
        WriteToFile(hijackText, "hijack_dump.csv");
    }


    public void readClick(View v)
    {
        int shortsRead;
        if (_aru == null)
            return;

        data.clear();

        //for (int j = 0; j < 6; j++) // Take 6 samples
        //{
            long startTime = System.nanoTime(); // Start timer
            _aru.startRecording();
            shortsRead = _aru.read(buffer, 0, buffer.length);
            for (int i = 0; i < shortsRead; i++) { // Copy data from buffer into stack.
                data.add(buffer[i]);
            }
            long endTime = System.nanoTime();
            long duration = (endTime - startTime) /1000000;
            // TODO Change this to setting the read(_,0,_) param to the offset. hopefully this will be faster
        //}

        String text = "Read time:  " + String.valueOf(duration) + " ms"; // Write the last sample to the ScrollView
        if (shortsRead > 300)
        for (int i = 0; i < 300; i++)
        {
            String num = "" + i;
            String val = Short.toString(buffer[i]);
            text += "\n" + num + ", " + val;
        }

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


            NewDecoder decoder = new NewDecoder();
            String res = decoder.HandleData(freqs);

            String statText = "\nHiJack Data:";
            statText += ("Num data points:  " + String.valueOf(data.size()));
            statText += ("\nNum freq coefficients:  " + String.valueOf(freqs.size()));
            statText += ("\nCalculation time:" + String.valueOf(duration) + " ms");
            statText += ("\n---RESULTS----");
            statText += ("\n" + res);
            statText += ("\n--------------");

            hijackStats.setText(statText);

            String text = "";
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
        hijackStats = (TextView) findViewById(R.id.hijackStats);

        int fre =_aru.getSampleRate();
        phoneDataTextView.setText("Sample Frequency:  " + String.valueOf(fre));
    }

    /*
    private double Average(short[] bytes)
    {
        double sum = 0;
        for (int i = 0; i < bytes.length; i++) {
            Short s = bytes[i];
            sum = sum + s.doubleValue();
        }
        return sum / bytes.length;
    }
    */

    public void WriteToFile(String content, String filename)
    {
        OutputStream fos;
        try {
            fos = openFileOutput(filename, Context.MODE_WORLD_READABLE);
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

