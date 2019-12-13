package com.example.final_rowing;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;


import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;


import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import android.os.CountDownTimer;
import android.os.Vibrator;
import android.view.View;

import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Locale;

import db.DBHelper;
import vo.Person;

import java.util.LinkedList;

import static java.lang.Integer.parseInt;

public class MainActivity extends AppCompatActivity {
    double time=3;
    int samplingPeriodUs=50000; // sampling rate: 0.05sec

    private float[] mR = new float[9];
    private float[] mOrientation = new float[3];
    private float[] filter_mag = new float[3];
    private float[] filter_acc = new float[3];
    private float[] filter_gyro = new float[3];

    private LinkedList<double[]> sensor_data=new LinkedList<double[]>();

    TextView tv_mag1;
    TextView tv_mag2;
    String result_mag;
    double timestamp;
    double timestamp_init = 0;
    double dt;
    double filter_temp;
    static final float NS2S = 1.0f/1000000000.0f;
    boolean flag_acc = false;
    boolean flag_mag = false;
    boolean flag_init = false;
    boolean flag_running = false;
    double pitch = 0, roll = 0;


//    String cur_name = "default";
//    boolean record_status = false;
//    int trial = 0;
//    int record_count = 0;

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
                    filter_temp = (1/ a) * (mOrientation[1] - pitch) + filter_gyro[1];
                    pitch = pitch + (filter_temp*dt);
                    filter_temp = (1/ a) * (mOrientation[2] - roll) + filter_gyro[0];
                    roll = roll + (filter_temp*dt);
                    tv_mag1 = (TextView) findViewById(R.id.debug_mag);
                    tv_mag2 = (TextView) findViewById(R.id.debug_mag2);
                    result_mag = String.valueOf(pitch);
                    tv_mag1.setText("Pitch"+ result_mag);
                    result_mag = String.valueOf(roll);
                    tv_mag2.setText("Roll" + result_mag);

                    double[] temp = {roll,timestamp-timestamp_init};
                    sensor_data.add(temp);
                    if(sensor_data.size()>time*1000000/samplingPeriodUs){
                        sensor_data.remove();
                    }
                    double [] lm_t1= sensor_data.get(57);
                    double [] lm_t2 = sensor_data.get(58);
                    double [] lm_t3 = sensor_data.get(59);
                    double lean_avgt = 0;
                    double lean_ttt = 0;
                    double lean_prevtt = 0;

                    double stroke_time;
                    double recovery_time;

                    int lean_count = 1;
                    double[] recovery_max = {-1,0};
                    double[] recovery_temp = {-1,0};

                    boolean flag_lean = false;
                    boolean flag_recovery = false;

                    boolean flag_init = false;


                    if(lm_t3[0]<-1.4){ // 뒤로 재꼈을 때 local max값 보정
                        if(((lm_t1[0] < lm_t2[0]) && (lm_t2[0] > lm_t3[0]))
                                ||(((lm_t1[0] > lm_t2[0]) && (lm_t2[0] < lm_t3[0])))){
                            lean_avgt += lm_t3[0];
                            lean_ttt += lm_t3[1];
                            lean_count++;
                            flag_lean = true;
                        }
                    }
                    else{
                        if(flag_lean){
                            if(flag_init){ // 첫 한바퀴만 아니면 비율 계산,,,

                                stroke_time = lean_ttt-lean_prevtt;
                                recovery_time = lean_ttt-recovery_max[1];
                                if(stroke_time/recovery_time < 0.2){
                                    // 리커버리 급함!
                                }
                                recovery_max[0] = -1;
                            }
                            lean_avgt /= lean_count;
                            lean_ttt /= lean_count;
                            lean_count = 1;
                            lean_prevtt = lean_ttt;
                            flag_lean = false; // evaluate and prevent.
                            if(lean_avgt > -1.92 && lean_avgt < -1.83){
                                //경고! 허리를 좀더 눕거나 너무눕지 말 것
                            }

                            flag_init = true;
                            lean_ttt = 0;
                            lean_avgt = 0;
                        }
                        if(lm_t3[0]>-0.1){ // 앞으로 쑤그렸을 때
                            flag_recovery = true;
                            if(lm_t3[0] > recovery_max[0]){
                                recovery_temp = lm_t3;
                            }
                        }
                        else if(flag_recovery){ // 쑤그렸다가 허리가 뒤로 나갈 때
                            flag_recovery = false;
                            recovery_max = lm_t3;
                            recovery_temp[0] = -1;
                            if(){
                                //허리가 너무 쑤그러졌거나 덜들어갔어!
                            }
                        }
                    }

                    /*=========================================collecting data
                    if(record_status && record_count > 0){
                        Person person = new Person();
                        person.setName(cur_name);
                        person.setTimestamp(se.timestamp);
                        person.setOrientR(roll);
                        person.setOrientP(pitch);
                        person.setTrial(trial);
                        person.setAccX(filter_acc[0]);
                        person.setAccY(filter_acc[1]);
                        person.setAccZ(filter_acc[2]);
                        dbHelper.addPerson(person);
                        record_count--;
                        if(record_count % 1000 == 0) {
                            Vibrator vib = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                            vib.vibrate(2000);
                              SoundPool sound = new SoundPool.Builder()
                                      .setMaxStreams(3)
                                      .build();
                              int soundId = sound.load(this, R.raw.prime, 1);
                              sound.play(soundId, 1,1,1,0,1);
                        }
                        if(record_count == 0){
                            record_status = false;
                        }
                    }
                    */
                }
                timestamp = se.timestamp;
            }
        }
    };
//    private ListView lvPeople;
//    private DBHelper dbHelper;
    //---------------------DB Variables
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SM = (SensorManager) getSystemService(SENSOR_SERVICE);
        SM.registerListener(sL, SM.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), samplingPeriodUs);
        SM.registerListener(sL, SM.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), samplingPeriodUs);
        SM.registerListener(sL, SM.getDefaultSensor(Sensor.TYPE_GYROSCOPE), samplingPeriodUs);
        final Button bt = (Button) findViewById(R.id.button);
        final TextView tv_start = (TextView)findViewById(R.id.debug_start);
        bt.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                if (flag_running) {
                    flag_running = false;
                    bt.setText("Start Running");
                } else {
                    CountDownTimer countDownTimer = new CountDownTimer(3000, 100) {
                        public void onTick(long millisUntilFinished) {
                            Vibrator vib = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                            vib.vibrate(400);
                            bt.setText(String.format(Locale.getDefault(), "%d초후 측정이 시작됩니다.", 1+ millisUntilFinished / 1000L));
                            if(roll<-1.00 && roll>-1.23){
                                tv_start.setText("OK");
                            }
                            else{
                                tv_start.setText("XXXXXXXXXXXXXXXXXXXXX");
                            }
                        }
                        public void onFinish() {
                            flag_init = true;
                            flag_running = true;
                            bt.setText("Stop Running");
                        }
                    }.start();
                }
            }
        });

        /*//---------------------------------------------DB below
        final Button btnStartRecoding = (Button) findViewById(R.id.btnStartRecoding);
        btnStartRecoding.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {

                if (record_status) {
                    record_status = false;
                    btnStartRecoding.setText("기록시작");
                } else {
                    CountDownTimer countDownTimer = new CountDownTimer(3000, 1000) {
                        public void onTick(long millisUntilFinished) {
                            Vibrator vib = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                            vib.vibrate(500);
                            btnStartRecoding.setText(String.format(Locale.getDefault(), "%d초후 측정이 시작됩니다.", 1+ millisUntilFinished / 1000L));
                        }
                        public void onFinish() {
                            record_status = true;
                            record_count = 90000;
                            Vibrator vib = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                            vib.vibrate(2000);
                            btnStartRecoding.setText("기록종료");
                        }
                    }.start();
                }
            }
        });
        Button btnCreateDatabase = (Button) findViewById(R.id.btnCreateButton);
        btnCreateDatabase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final EditText etDBName = new EditText(MainActivity.this);
                etDBName.setHint("Please enter the new DB name.");

                AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                dialog.setTitle("Database Name")
                        .setMessage("Database Name?")
                        .setView(etDBName)
                        .setPositiveButton("Create", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (etDBName.getText().toString().length() > 0) {
                                    dbHelper = new DBHelper(
                                            MainActivity.this,
                                            etDBName.getText().toString(),
                                            null, 1) {
                                        @Override
                                        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

                                        }
                                    };
                                    dbHelper.testDB();
                                }
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
        //----------btnCreate
        Button btnInsertDatabase = (Button) findViewById(R.id.btnInsertButton);
        btnInsertDatabase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LinearLayout layout = new LinearLayout(MainActivity.this);
                layout.setOrientation(LinearLayout.VERTICAL);

                final EditText etName = new EditText(MainActivity.this);
                etName.setHint("NAME?");

                final EditText etTrial = new EditText(MainActivity.this);
                etTrial.setHint("RATE?");

                layout.addView(etName);
                layout.addView(etTrial);

                AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                dialog.setTitle("Please fill these.").setView(layout)
                        .setPositiveButton("Register", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                cur_name = etName.getText().toString();
                                trial = parseInt(etTrial.getText().toString());
                            }
                        })
                        .setNeutralButton("취소", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        })
                        .create()
                        .show();
            }
        });
        */
    }
}


