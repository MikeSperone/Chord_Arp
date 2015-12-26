package com.mikesperone.chordarp;


import java.io.File;
import java.io.IOException;

import org.puredata.android.io.AudioParameters;
import org.puredata.android.io.PdAudio;
import org.puredata.android.utils.PdUiDispatcher;
import org.puredata.core.PdBase;
import org.puredata.core.utils.IoUtils;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
//import android.util.Range;
import android.view.View;
//import android.view.View.OnClickListener;
import android.widget.Button;
//import android.widget.LinearLayout;
import android.widget.SeekBar;
//import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.ToggleButton;
import android.widget.TextView;

import org.florescu.android.rangeseekbar.RangeSeekBar;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private PdUiDispatcher dispatcher;

    private TextView volText;
    private TextView tempoText;

    private RangeSeekBar octaves;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            initGui();
            initPd();
            loadPatch();
            setDefaults();
        } catch (IOException e) {
            Log.e(TAG, e.toString());
            finish();
        }
    }

    private void setDefaults() {
        float octBaseInit = 3f;
        float octRangeInit = 6f;
        //initial values
        octaves.setRangeValues(1, 8);
        octaves.setSelectedMinValue(octBaseInit);
        octaves.setSelectedMaxValue(octRangeInit);
    }

    private void initGui() {
        setContentView(R.layout.activity_main);

        SeekBar Vol = (SeekBar) findViewById(R.id.Vol);
        Vol.setOnSeekBarChangeListener(new Sliders());

        SeekBar Tempo = (SeekBar) findViewById(R.id.Tempo);
        Tempo.setOnSeekBarChangeListener(new Sliders());

        volText = (TextView)findViewById(R.id.volText);
        tempoText = (TextView)findViewById(R.id.tempoText);

        octaves = (RangeSeekBar) findViewById(R.id.octaveRange);
        octaves.setOnRangeSeekBarChangeListener(new RangeSeekBar.OnRangeSeekBarChangeListener<Integer>() {
            @Override
            public void onRangeSeekBarValuesChanged(RangeSeekBar<?> bar, Integer minValue, Integer maxValue) {
                OctaveRangeDoStuff(minValue, maxValue - minValue);
            }
        });
    }

    public void onChordClick(View v) {
        Button button = (Button) v;
        String tag = button.getTag().toString();
        //Log.d(TAG, tag);
        triggerChord(tag);
    }

    public void onIoClick(View v) {
        ToggleButton IO = (ToggleButton) v;
        if (IO.isChecked()) {
            PdBase.sendFloat("onOff", 1);
            //Log.d(TAG, "ON");
            IO.setChecked(true);
        } else {
            PdBase.sendFloat("onOff",  0);
            //Log.d(TAG, "OFF");
            IO.setChecked(false);
        }
    }

    public void onTimbreClick(View v) {
        ToggleButton timbre = (ToggleButton) v;
        if (timbre.isChecked()) {
            PdBase.sendFloat("timbre", 0);
            //Log.d(TAG, "sawtooth wave");
            timbre.setChecked(true);
        } else {
            PdBase.sendFloat("timbre",  1);
            //Log.d(TAG, "sine wave");
            timbre.setChecked(false);
        }
    }

    private void triggerChord(String chordName) {
        PdBase.sendBang(chordName);
    }

    public void OctaveRangeDoStuff(int min, int range) {
        //Log.d(TAG, "User selected new range values: MIN=" + min + ", MAX=" + max);
        PdBase.sendFloat("octaveBase", min);
        PdBase.sendFloat("octaveRange", range);
    }

    private class Sliders implements SeekBar.OnSeekBarChangeListener {
        private float tempo_amount;
        private float vol_level;
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            switch(seekBar.getId()) {
                case R.id.Vol:
                    vol_level = (float) (progress / 100.);
                    PdBase.sendFloat("volume", vol_level);
                    String vol_string = Float.toString(vol_level);
                    volText.setText(vol_string);
                    break;
                case R.id.Tempo:
                    tempo_amount = (float) (progress/10.);
                    String tempo_string = Float.toString(tempo_amount);
                    tempoText.setText(tempo_string);
                    tempo_amount = 10 - tempo_amount;
                    PdBase.sendFloat("tempo", tempo_amount);
                    break;
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            // TODO Auto-generated method stub
        }
    }

    private void initPd() throws IOException {
        int sampleRate = AudioParameters.suggestSampleRate();
        PdAudio.initAudio(sampleRate, 0, 2, 8, true);

        dispatcher = new PdUiDispatcher();
        PdBase.setReceiver(dispatcher);
    }

    private void loadPatch() throws IOException {
        File dir = getFilesDir();
        IoUtils.extractZipResource(getResources().openRawResource(R.raw.chordarp), dir, true);
        File patchFile = new File(dir, "chordarp.pd");

        PdBase.openPatch(patchFile.getAbsolutePath());
    }

    @Override
    protected void onResume() {
        super.onResume();
        PdAudio.startAudio(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        PdAudio.stopAudio();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        PdAudio.release();
        PdBase.release();
    }

}
