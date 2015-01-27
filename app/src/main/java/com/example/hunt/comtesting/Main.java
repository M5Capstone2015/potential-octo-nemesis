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
    private static int[] mSampleRates = new int[]{8000, 11025, 22050, 44100};
    //AudioRecord _aru = null;
    SerialDecoder _decoder = null;
    short[] buffer = null;

    List<Short> data = new ArrayList<Short>();

    public void saveClick(View v) {
        String text = dataTextView.getText().toString();
        WriteToFile(text);
    }


    public void readClick(View v) {
        if (_decoder != null)
        {
            nData data = _decoder.Read();
        }
    }

    public void clearClick(View v) {
        dataTextView.setText("");
        hijackTextView.setText("");
        data.clear();
    }

    public void processClick(View v) {
        if (_decoder != null)
        {
            /*
            _decoder.startAudioIO();
            */

            long startTime = System.nanoTime(); // Start timer

            List<Integer> freqs = _decoder.LowLevelProccess();

            long endTime = System.nanoTime();
            long duration = (endTime - startTime) / 1000000;

            String text = "\nHiJack Data:";
            text += ("Num data points:  " + String.valueOf(freqs.size()));
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
        dataTextView = (TextView) findViewById(R.id.dataText);
        hijackTextView = (TextView) findViewById(R.id.hijackData);
        phoneDataTextView = (TextView) findViewById(R.id.phoneData);

        _decoder = new SerialDecoder();
        _decoder.regTextViews(dataTextView, hijackTextView);

        int fre = _decoder.getSampleRate();
        phoneDataTextView.setText("Sample Frequency:  " + String.valueOf(fre));
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

