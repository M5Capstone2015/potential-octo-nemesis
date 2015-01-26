package com.example.hunt.comtesting;

import android.media.AudioRecord;

import java.util.ArrayList;
import java.util.List;

public class AudioReceiver {
    ///////////////////////////////////////////////
    // Constructors
    ///////////////////////////////////////////////
    public AudioReceiver(AudioRecord aru)
    {
        _audioRecord = aru;
    }


    ///////////////////////////////////////////////
    // Constants
    ///////////////////////////////////////////////

    // Most Android devices support 'CD-quality' sampling frequencies.
    final private int _sampleFrequency = 44100;

    // HiJack is powered at 21kHz
    private int _powerFrequency = 21000;

    // IO is FSK-modulated at either 613 or 1226 Hz (0 / 1)
    final private int _ioBaseFrequency = 613;

    ///////////////////////////////////////////////
    // Main interfaces
    ///////////////////////////////////////////////

    // Used for receiving and transmitting the
    // primitive data elements (1s and 0s)
    //private OutgoingSource _source = null;
    //private IncomingSink _sink = null;


    // For Audio Output
    //AudioTrack _audioTrack;
    //Thread _outputThread;

    // For Audio Input
    AudioRecord _audioRecord;
    Thread _inputThread;

    ///////////////////////////////////////////////
    // Output state
    ///////////////////////////////////////////////

    // For performance reasons we batch update
    // the audio output buffer with this many bits.
    final private int _bitsInBuffer = 100;

    // To ensure efficient computation we create some buffers.
    private short[] _outHighHighBuffer;
    private short[] _outLowLowBuffer;
    private short[] _outHighLowBuffer;
    private short[] _outLowHighBuffer;

    private int _outBitBufferPos = 0;
    private int _powerFrequencyPos = 0;

    private short[] _stereoBuffer;
    private short[] _recBuffer;

    private boolean _isInitialized = false;
    private boolean _isRunning = false;
    private boolean _stop = false;

    ///////////////////////////////////////////////
    // Input state
    ///////////////////////////////////////////////

    private enum SearchState { ZERO_CROSS, NEGATIVE_PEAK, POSITIVE_PEAK };
    private SearchState _searchState = SearchState.ZERO_CROSS;

    // Part of a circular buffer to find the peak of each
    // signal.
    private int _toMean[] = new int[] {0, 0, 0};
    private int _toMeanPos = 0;

    // Part of a circular buffer to keep track of any
    // bias in the signal over the past 144 measurements
    private int _biasArray[] = new int[144];
    private boolean _biasArrayFull = false;
    private double _biasMean = 0.0;
    private int _biasArrayPos = 0;

    // Keeps track of the maximal value between two
    // zero-crossings to find the distance between
    // peaks
    private int _edgeDistance = 0;

    private List<Integer> _freqStack = new ArrayList<Integer>();

    ///////////////////////////////////////////////
    // Processors
    ///////////////////////////////////////////////

    private void processInputBuffer(int shortsRead) {


        // We are basically trying to figure out where the edges are here,
        // in order to find the distance between them and pass that on to
        // the higher levels.
        double meanVal = 0.0;
        //System.out.println("shortsRead:  " + shortsRead);
        for (int i = 0; i < shortsRead; i++) {

            meanVal = addAndReturnMean(_recBuffer[i]) - addAndReturnBias(_recBuffer[i]);
            _edgeDistance++;

            // Cold boot we simply set the search based on
            // where the first region is located.
            if (_searchState == SearchState.ZERO_CROSS) {
                _searchState = meanVal < 0 ? SearchState.NEGATIVE_PEAK : SearchState.POSITIVE_PEAK;
            }

            // Have we just seen a zero transistion?
            if ((meanVal < 0 && _searchState == SearchState.POSITIVE_PEAK) ||
                    (meanVal > 0 && _searchState == SearchState.NEGATIVE_PEAK)) {

                //_sink.handleNextBit(_edgeDistance, _searchState == SearchState.POSITIVE_PEAK);
                _freqStack.add(_edgeDistance);
                _edgeDistance = 0;
                _searchState = (_searchState == SearchState.NEGATIVE_PEAK) ? SearchState.POSITIVE_PEAK : SearchState.NEGATIVE_PEAK;
            }
        }

        //System.out.println("func finished");
    }

    ///////////////////////////////////////////////
    // Incoming Bias and Smoothing Functions
    ///////////////////////////////////////////////

    private double addAndReturnMean(int in) {
        _toMean[_toMeanPos++] = in;
        _toMeanPos = _toMeanPos % _toMean.length;

        double sum = 0.0;

        for (int i = 0; i < _toMean.length; i++) {
            sum += _toMean[i];
        }

        return sum / _toMean.length;
    }

    private double addAndReturnBias(int in) {
        if (_biasArrayFull) {
            _biasMean -= (double)_biasArray[_biasArrayPos] / (double)_biasArray.length;
        }

        _biasArray[_biasArrayPos++] = in;
        _biasMean += (double)in / (double)_biasArray.length;

        // If we're at the end of the bias array we move the
        // position back to 0 and recalculate the mean from scratch
        // keep small inaccuracies from influencing it.
        if (_biasArrayPos == _biasArray.length) {
            double totalSum = 0.0;
            for (int i = 0; i < _biasArray.length; i++) {
                totalSum += _biasArray[i];
            }
            _biasMean = totalSum / (double)_biasArray.length;
            _biasArrayPos = 0;
            _biasArrayFull = true;
        }

        return _biasMean;
    }

    ///////////////////////////////////////////////
    // Audio Interface
    // Note: these exist primarily to pass control to
    //       the responsible subfunctions. NO code here.
    ///////////////////////////////////////////////

    Runnable _inputProcessor = new Runnable() {
        public void run() {
            Thread.currentThread().setPriority(Thread.MIN_PRIORITY);

            while (!_stop) {
                int shortsRead = _audioRecord.read(_recBuffer, 0, _recBuffer.length);
                processInputBuffer(shortsRead);
            }
        }
    };

    ///////////////////////////////////////////////
    // Public Interface
    ///////////////////////////////////////////////
    public int getPowerFrequency() {
        return _powerFrequency;
    }

    public void setPowerFrequency(int powerFrequency) {
        _powerFrequency = powerFrequency;
    }


    /*
    public void registerIncomingSink(IncomingSink sink) {
        if (_isRunning) {
            throw new UnsupportedOperationException("AudioIO must be stopped to set a new sink.");
        }
        _sink = sink;
    }
    */

    public void initialize() {
        // Create buffers to hold what a high and low
        // frequency waveform looks like
        int bufferSize = getBufferSize();

        _outHighHighBuffer = new short[bufferSize];
        _outHighLowBuffer = new short[bufferSize];
        _outLowHighBuffer = new short[bufferSize];
        _outLowLowBuffer = new short[bufferSize];

        for (int i = 0; i < bufferSize; i++) {

            // NOTE: We bound this due to some weird java issues with casting to
            // shorts.+
            _outHighHighBuffer[i] = (short) (
                    boundToShort(Math.sin((double)i * (double)2 * Math.PI * (double)_ioBaseFrequency / (double)_sampleFrequency) * Short.MAX_VALUE)
            );

            _outHighLowBuffer[i] = (short) (
                    boundToShort(Math.sin((double)i * (double)4 * Math.PI * (double)_ioBaseFrequency / (double)_sampleFrequency) * Short.MAX_VALUE)
            );

            _outLowLowBuffer[i] = (short) (
                    boundToShort(Math.sin((double)(i + bufferSize) * (double)2 * Math.PI * (double)_ioBaseFrequency / (double)_sampleFrequency) * Short.MAX_VALUE)
            );

            _outLowHighBuffer[i] = (short) (
                    boundToShort(Math.sin((double)(i + bufferSize/2) * (double)4 * Math.PI * (double)_ioBaseFrequency / (double)_sampleFrequency) * Short.MAX_VALUE)
            );
        }

        _isInitialized = true;
    }

    public void startAudioIO() {

        if (!_isInitialized) {
            initialize();
        }

        if (_isRunning) {
            return;
        }

        _stop = false;

        attachAudioResources();

        _audioRecord.startRecording();

        _inputThread = new Thread(_inputProcessor);
        //_inputThread.start(); // disabled for debuging....

        // DEBUG
        fakeAudioRead(); // Commentiig this out for now just to test this class.
        // DEBUG

    }

    //
    // DEBUG
    //
    public List<Integer> fakeAudioRead()  // Change this to return any freq coefficients.
    {
        _freqStack.clear();

        int shortsRead = _audioRecord.read(_recBuffer, 0, _recBuffer.length);
        processInputBuffer(shortsRead);
        return _freqStack;
        //_recBuffer = _audioRecord.read();
        //System.out.println("data size: " + _recBuffer.length);
        //processInputBuffer(_recBuffer.length);
    }

    public List<Integer> fakeAudioRead(List<Short> data)  // Change this to return any freq coefficients.
    {
        _freqStack.clear();

        _recBuffer = new short[data.size() + 1];

        for (int i = 0; i < data.size(); i++)
            _recBuffer[i] = data.get(i);

        processInputBuffer(data.size());
        return _freqStack;
    }




    //
    // DEBUG
    //
    /*
    public void printSink()
    {
        FakeSink fs = (FakeSink) _sink;
        fs.Print();
    }
    */

    public void stopAudioIO() {
        _stop = true;

        try {
            //_outputThread.join();
            _inputThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        releaseAudioResources();

        _isRunning = false;
    }

    ///////////////////////////////////////////////
    // Support functions
    ///////////////////////////////////////////////

    private void attachAudioResources() {
        int bufferSize = getBufferSize();

        // The stereo buffer should be large enough to ensure
        // that scheduling doesn't mess it up.
        _stereoBuffer = new short[bufferSize * _bitsInBuffer];

        // COUMMENTED OUT FOR DEBUGGING
		/*
		_audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
					     _sampleFrequency,
					     AudioFormat.CHANNEL_OUT_STEREO,
                			     AudioFormat.ENCODING_PCM_16BIT,
					     44100,
                			     AudioTrack.MODE_STREAM);
		*/

        int recBufferSize = 5;
        //int recBufferSize =
        //AudioRecord.getMinBufferSize(_sampleFrequency, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);

		/*
		_audioRecord = new AudioRecord(MediaRecorder.AudioSource.DEFAULT,
		        _sampleFrequency, AudioFormat.CHANNEL_IN_MONO,
		        AudioFormat.ENCODING_PCM_16BIT, recBufferSize);
			*/
        //_audioRecord = new FakeAudioRecord();

        _recBuffer = new short[recBufferSize * 10];
    }

    private void releaseAudioResources() {
        //_audioTrack.release();
        _audioRecord.release();

        //_audioTrack = null;
        _audioRecord = null;

        _stereoBuffer = null;
        _recBuffer = null;
    }

    private double boundToShort(double in) {
        return (in >= 32786.0) ? 32786.0 : (in <= -32786.0 ? -32786.0 : in );
    }

    private int getBufferSize() {
        return _sampleFrequency / _ioBaseFrequency / 2;
    }


}
