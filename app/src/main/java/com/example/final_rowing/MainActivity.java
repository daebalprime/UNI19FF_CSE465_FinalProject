package com.example.final_rowing;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.DialogInterface;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Bundle;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;
import java.util.LinkedList;

import static java.lang.Integer.parseInt;

public class MainActivity extends AppCompatActivity {
    Context mContext;

    int samplingPeriodUs=50000; // sampling rate: 0.05sec

    private float[] mR = new float[9];
    private float[] mOrientation = new float[3];
    private float[] filter_mag = new float[3];
    private float[] filter_acc = new float[3];
    private float[] filter_gyro = new float[3];

    private LinkedList<double[]> sensor_data=new LinkedList<double[]>();

    TextView tv_mag2;

    String result_mag;
    double timestamp;
    double timestamp_init = 0;
    double dt;
    double filter_temp;
    static final float NS2S = 1.0f/1000000000.0f;
    boolean flag_acc = false;
    boolean flag_mag = false;

    boolean flag_running = false;
    double roll = 0;

    boolean flag_lean = false;
    boolean flag_recovery = false;
    boolean flag_init = false;

    double lean_avgt = 0;
    double lean_ttt = 0;
    int lean_count = 1;

    int s1, s2, s3, s4;
    SoundPool sp;

    double bmax = -1.00, bmin = -1.50,fmax = -2.3, fmin = -1.7;

    double[] recovery_max = {-1, 0};
    double[] recovery_temp = {-1, 0};

    AudioAttributes audioAttributes = new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build();
    Vibrator vib;
    SensorManager SM;
    SensorEventListener sL = new SensorEventListener() {
        @Override
        public void onAccuracyChanged(Sensor arg0, int arg1) {
        }
        @Override
        public void onSensorChanged(SensorEvent se) {
            switch(se.sensor.getType()){
                case Sensor.TYPE_ACCELEROMETER:
                    if(!flag_acc) {
                        filter_acc = se.values;
                        flag_acc = true;
                    }
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                    if(!flag_mag){
                        filter_mag = se.values;
                        flag_mag = true;
                    }
                    break;
                case Sensor.TYPE_GYROSCOPE:
                    filter_gyro = se.values;
                    break;
            }

            if(flag_mag && flag_acc){ // complementary filter for sensor values.
                SensorManager.getRotationMatrix(mR, null, filter_acc, filter_mag);
                SensorManager.getOrientation(mR, mOrientation);
                flag_acc = false;
                flag_mag = false;
                if(se.timestamp != 0) {
                    if(flag_init){
                        flag_init =false;
                        timestamp_init = se.timestamp;
                    }
                    dt = (se.timestamp - timestamp) * NS2S;
                    // compensation filter coefficient.
                    float a = 0.1f;
                    filter_temp = (1/ a) * (mOrientation[2] - roll) + filter_gyro[0];
                    roll = roll + (filter_temp*dt);

                    tv_mag2 = findViewById(R.id.debug_mag2);

                    if(!flag_running && (roll < -1.55334 && roll > -1.58825)){
                        vib.vibrate(100); // posture correction
                    }
                    result_mag = String.valueOf(roll);
                    tv_mag2.setText("Roll" + result_mag);

                    double[] temp = {roll,timestamp-timestamp_init};
                    sensor_data.add(temp);
                    if(sensor_data.size()>4){
                        sensor_data.remove();
                    }
                    if(flag_running) {
                        double[] lm_t2 = sensor_data.get(1);
                        double[] lm_t3 = sensor_data.get(2);
                        if (roll > -1.52) { // 80degree
                            if(Math.abs(lm_t3[0]-lm_t2[0])<1.0) { // filtering noise
                                    lean_avgt += lm_t3[0];
                                    lean_count++;
                                    flag_lean = true;
                            }
                            if (flag_recovery) {
                                flag_recovery = false;
                                recovery_max = recovery_temp;
                                Log.e("LOG", "RECOVERY:" + String.format("%d", (int) (-(90+Math.toDegrees(fmax)))));
                                Log.e("LOG", "RECOVERY:" + String.format("%d", (int)(-(90+Math.toDegrees(recovery_max[0])))));
                                Log.e("LOG", "RECOVERY:" + String.format("%d", (int)(-(90+Math.toDegrees(fmin)))));
                                if (recovery_max[0] <  fmax || recovery_max[0] > fmin) {
                                    sp.play(s2,1f,1f,0,0,1f);
                                }
                                else{
                                    sp.play(s4,1f,1f,0,0,1f);
                                }
                                recovery_temp[0] = -1;
                            }
                        }
                        else {
                            if (flag_lean) {
                                lean_avgt /= lean_count;
                                lean_count = 1;
                                flag_lean = false; // evaluate and prevent.
                                Log.e("LOG", "LEAN_AVGT:" + String.format("%d", (int)(-(90+Math.toDegrees(bmax)))));
                                Log.e("LOG", "LEAN_AVGT:" + String.format("%d", (int)(-(90+Math.toDegrees(lean_avgt)))));
                                Log.e("LOG", "LEAN_AVGT:" + String.format("%d", (int)(-(90+Math.toDegrees(bmin)))));
                                if (lean_avgt > bmax || lean_avgt < bmin) {
                                    sp.play(s1,1f,1f,0,0,1f);
                                }
                                else {
                                    sp.play(s3,1f,1f,0,0,1f);
                                }
                                lean_ttt = 0;
                                lean_avgt = 0;
                            }
                            if (roll < -1.52) { // 앞으로 쑤그렸을 때,-95도
                                flag_recovery = true;
                                if (lm_t3[0] < recovery_temp[0]) {
                                    recovery_temp = lm_t3; // local minimum finding
                                }
                            }

                        }
                    }
                }
                timestamp = se.timestamp;
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mContext = getApplicationContext();
        sp = new SoundPool.Builder().setAudioAttributes(audioAttributes).setMaxStreams(4).build();
        s1 = sp.load(mContext, R.raw.a1, 1);
        s2 = sp.load(MainActivity.this, R.raw.a2, 1);
        s3 = sp.load(MainActivity.this, R.raw.c1, 1);
        s4 = sp.load(MainActivity.this, R.raw.c2, 1);
        vib = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SM = (SensorManager) getSystemService(SENSOR_SERVICE);
        SM.registerListener(sL, SM.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), samplingPeriodUs);
        SM.registerListener(sL, SM.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), samplingPeriodUs);
        SM.registerListener(sL, SM.getDefaultSensor(Sensor.TYPE_GYROSCOPE), samplingPeriodUs);
        final Button bt = findViewById(R.id.button);
        Button btnInsertDatabase = (Button) findViewById(R.id.btnInsertButton);
        btnInsertDatabase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LinearLayout layout = new LinearLayout(MainActivity.this);
                layout.setOrientation(LinearLayout.VERTICAL);
                final EditText et1 = new EditText(MainActivity.this);
                et1.setHint("BMAX : " + (-(90+Math.toDegrees(bmax))));
                final EditText et2 = new EditText(MainActivity.this);
                et2.setHint("BMIN : " + (-(90+Math.toDegrees(bmin))));
                final EditText et3 = new EditText(MainActivity.this);
                et3.setHint("FMAX : " + (-(90+Math.toDegrees(fmax))));
                final EditText et4 = new EditText(MainActivity.this);
                et4.setHint("FMIN : " + (-(90+Math.toDegrees(fmin))));
                layout.addView(et1);
                layout.addView(et2);
                layout.addView(et3);
                layout.addView(et4);
                AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                dialog.setTitle("Set your range.").setView(layout)
                        .setPositiveButton("Register", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                int t1 = parseInt(et1.getText().toString());
                                int t2 = parseInt(et2.getText().toString());
                                int t3 = parseInt(et3.getText().toString());
                                int t4 = parseInt(et4.getText().toString());
                                bmax = Math.toRadians(-(90+t1));
                                bmin = Math.toRadians(-(90+t2));
                                fmax = Math.toRadians(-(90+t3));
                                fmin = Math.toRadians(-(90+t4));
                            }
                        })
                        .setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .create()
                        .show();
            }
        });
        bt.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                if (flag_running) {
                    flag_running = false;
                    flag_init = false;
                    bt.setText("Start Running");
                } else {
                    CountDownTimer countDownTimer = new CountDownTimer(3000, 1000) {
                        public void onTick(long millisUntilFinished) {
                            bt.setText(String.format(Locale.getDefault(), "%d초후 측정이 시작됩니다.", 1+ millisUntilFinished / 1000L));
                        }
                        public void onFinish() {
                            flag_running = true;
                            bt.setText("Stop Running");
                        }
                    }.start();
                }
            }
        });
    }
}


