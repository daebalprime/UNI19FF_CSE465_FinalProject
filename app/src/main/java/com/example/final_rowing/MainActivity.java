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

import static java.lang.Integer.parseInt;

public class MainActivity extends AppCompatActivity {
    int samplingPeriodUs=50000;

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

    String cur_name = "default";
    boolean record_status = false;
    int trial = 0;
    int record_count = 0;

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
                    dt = (se.timestamp - timestamp) * NS2S;
                    // compensation filter coefficient.
                    float a = 0.1f;
                    filter_temp = (1/ a) * (mOrientation[1] - pitch) + filter_gyro[1];
                    pitch = pitch + (filter_temp*dt);
                    filter_temp = (1/ a) * (mOrientation[2] - roll) + filter_gyro[0];
                    roll = roll + (filter_temp*dt);
                    tv_mag1 = (TextView) findViewById(R.id.debug_mag);
                    tv_mag2 = (TextView) findViewById(R.id.debug_mag2);
                    result_mag = String.valueOf(pitch*10);
                    tv_mag1.setText("Pitch"+ result_mag);
                    result_mag = String.valueOf(roll*10);
                    tv_mag2.setText("Roll" + result_mag);
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
//                            SoundPool sound = new SoundPool.Builder()
//                                    .setMaxStreams(3)
//                                    .build();
//                            int soundId = sound.load(this, R.raw.prime, 1);
//                            sound.play(soundId, 1,1,1,0,1);
                        }
                        if(record_count == 0){
                            record_status = false;
                        }
                    }

                }
                timestamp = se.timestamp;
            }
        }
    };
    private ListView lvPeople;
    private DBHelper dbHelper;
    //---------------------DB Variables
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SM = (SensorManager) getSystemService(SENSOR_SERVICE);
        SM.registerListener(sL, SM.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), samplingPeriodUs);
        SM.registerListener(sL, SM.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), samplingPeriodUs);
        SM.registerListener(sL, SM.getDefaultSensor(Sensor.TYPE_GYROSCOPE), samplingPeriodUs);
        Button bt = (Button) findViewById(R.id.button);
        bt.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                // 시작 카운트
//                long time1 = System.nanoTime();
//                long time2 = System.nanoTime();
//                double latency = (time2-time1) / 1e6;
            }
        });
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
        //---------------------------------------------DB below
        //---------------------------------------------DB below
        //---------------------------------------------DB below
        //---------------------------------------------DB below
        //---------------------------------------------DB below
        Button btnCreateDatabase = (Button) findViewById(R.id.btnCreateButton);
        btnCreateDatabase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final EditText etDBName = new EditText(MainActivity.this);
                etDBName.setHint("DB명을 입력하세여");

                AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                dialog.setTitle("Database 이름 입력")
                        .setMessage("Database 이름입력")
                        .setView(etDBName)
                        .setPositiveButton("생성", new DialogInterface.OnClickListener() {
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
                        .setNeutralButton("취소", new DialogInterface.OnClickListener() {
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
                etName.setHint("이름을 입력하세요.");

                final EditText etTrial = new EditText(MainActivity.this);
                etTrial.setHint("몇 번째 시도인가요?");

                layout.addView(etName);
                layout.addView(etTrial);

                AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                dialog.setTitle("정보를 입력하세요").setView(layout)
                        .setPositiveButton("등록", new DialogInterface.OnClickListener() {
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
    }
}


