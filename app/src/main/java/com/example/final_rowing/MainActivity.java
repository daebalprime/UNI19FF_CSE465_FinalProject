package com.example.final_rowing;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.LinkedList;

public class MainActivity extends AppCompatActivity {
    double time=3;
    int samplingPeriodUs=50000;
    private float a = 0.1f; // compensation filter coefficient.

    private LinkedList<float[]> list_acc=new LinkedList<float[]>();
    private LinkedList<float[]> list_mag=new LinkedList<float[]>();
    private float[] mR = new float[9];
    private float[] mOrientation = new float[3];
    private float[] filter_mag = new float[3];
    private float[] filter_acc = new float[3];
    private float[] filter_gyro = new float[3];

    TextView tv_mag1;
    TextView tv_mag2;
    String result_mag;
    double timestamp;
    double dt;
    double filter_temp;
    static final float NS2S = 1.0f/1000000000.0f;
    boolean flag_acc = false;
    boolean flag_mag = false;
    double pitch = 0, roll = 0;

    SensorManager SM;
    SensorEventListener sL = new SensorEventListener() {
        @Override
        public void onAccuracyChanged(Sensor arg0, int arg1) {
        }

        @Override
        public void onSensorChanged(SensorEvent se) {
            float[] temp = {se.values[0], se.values[1], se.values[2]};
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
                    dt = (se.timestamp - timestamp) * NS2S;
                    filter_temp = (1/a) * (mOrientation[1] - pitch) + filter_gyro[1];
                    pitch = pitch + (filter_temp*dt);
                    filter_temp = (1/a) * (mOrientation[2] - roll) + filter_gyro[0];
                    roll = roll + (filter_temp*dt);
                    tv_mag1 = (TextView) findViewById(R.id.debug_mag);
                    tv_mag2 = (TextView) findViewById(R.id.debug_mag2);
                    result_mag = String.valueOf(pitch*10);
                    tv_mag1.setText("Pitch"+ result_mag);
                    result_mag = String.valueOf(roll*10);
                    tv_mag2.setText("Roll" + result_mag);
                }
                timestamp = se.timestamp;
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SM = (SensorManager)getSystemService(SENSOR_SERVICE);
        SM.registerListener(sL, SM.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),samplingPeriodUs);
        SM.registerListener(sL, SM.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),samplingPeriodUs);
        SM.registerListener(sL, SM.getDefaultSensor(Sensor.TYPE_GYROSCOPE),samplingPeriodUs);
        Button bt=(Button)findViewById(R.id.button);
        bt.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v){
                // 시작 카운트
//                long time1 = System.nanoTime();
//                for(int i=1;i<data.size()-1;i++) {
//                    float[] temp = data.get(i);

//                for(int i=0;i<localmaxmin.size();i++){
//                    float[] temp4=localmaxmin.get(i);
//                    frequency += temp4[1];
//                }
//                magnitude=magnitude/data.size()-2;
//
//                long time2 = System.nanoTime();
//                double latency = (time2-time1) / 1e6;
            }
        });
    }
}
