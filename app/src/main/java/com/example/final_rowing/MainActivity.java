package com.example.final_rowing;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.LinkedList;
import java.util.List;

import db.DBHelper;
import vo.Person;

import static java.lang.Integer.parseInt;

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
    //---------------------DB Variables
    private Button btnCreateDatabase;
    private Button btnInsertDatabase;
    private Button btnSelectAllData;
    private ListView lvPeople;
    private DBHelper dbHelper;
    //---------------------DB Variables
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
//                long time2 = System.nanoTime();
//                double latency = (time2-time1) / 1e6;
            }
        });
        //---------------------------------------------DB below
        btnCreateDatabase = (Button) findViewById(R.id.btnCreateButton);
        btnCreateDatabase.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                final EditText etDBName = new EditText(MainActivity.this);
                etDBName.setHint("DB명을 입력하세여");

                AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                dialog.setTitle("Database 이름 입력")
                        .setMessage("Database 이름입력")
                        .setView(etDBName)
                        .setPositiveButton("생성", new DialogInterface.OnClickListener(){
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if(etDBName.getText().toString().length() > 0){
                                    dbHelper= new DBHelper(
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
        btnInsertDatabase = (Button) findViewById(R.id.btnInsertButton);
        btnInsertDatabase.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                LinearLayout layout = new LinearLayout(MainActivity.this);
                layout.setOrientation(LinearLayout.VERTICAL);

                final EditText etName = new EditText(MainActivity.this);
                etName.setHint("이름을 입력하세요.");

                final EditText etAge = new EditText(MainActivity.this);
                etAge.setHint("나이를 입력하세요.");

                final EditText etPhone = new EditText(MainActivity.this);
                etPhone.setHint("전화번호를 입력하세요.");
                layout.addView(etName);
                layout.addView(etAge);
                layout.addView(etPhone);
                AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                dialog.setTitle("정보를 입력하세요") .setView(layout)
                        .setPositiveButton("등록", new DialogInterface.OnClickListener() {
                            @Override public void onClick(DialogInterface dialog, int which) {
                                String name = etName.getText().toString();
                                String age = etAge.getText().toString();
                                String phone = etPhone.getText().toString();
                                if( dbHelper == null ) {
                                    dbHelper = new DBHelper(
                                            MainActivity.this,
                                            "TEST",
                                            null, 1){
                                        @Override
                                        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                                        }
                                    };
                                }
                                Person person = new Person();
                                person.setName(name);
                                person.setAge(parseInt(age) );
                                person.setPhone(phone);
                                dbHelper.addPerson(person);
                            }
                        })
                        .setNeutralButton("취소", new DialogInterface.OnClickListener() {
                            @Override public void onClick(DialogInterface dialog, int which) {

                            }
                        })
                        .create()
                        .show();
            }
        });
        lvPeople = (ListView) findViewById(R.id.lvPeople);
        btnSelectAllData = (Button) findViewById(R.id.btnSelectAllData);
        btnSelectAllData.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                // ListView를 보여준다.
                lvPeople.setVisibility(View.VISIBLE);
                // DB Helper가 Null이면 초기화 시켜준다.
                if( dbHelper == null ) {
                    dbHelper = new DBHelper(
                            MainActivity.this,
                            "TEST",
                            null , 1){};
                }
                // 1. Person 데이터를 모두 가져온다.
                List people = dbHelper.getAllPersonData();
                // 2. ListView에 Person 데이터를 모두 보여준다.
                lvPeople.setAdapter(new PersonListAdapter(people, MainActivity.this));
            }
        });
    }
    private class PersonListAdapter extends BaseAdapter {
        private List people;
        private Context context;
        /** * 생성자 * @param people : Person List * @param context */
        public PersonListAdapter(List people, Context context) {
            this.people = people; this.context = context;
        }
        @Override
        public int getCount() {
            return this.people.size();
        }
        @Override public Object getItem(int position) {
            return this.people.get(position);
        }
        @Override public long getItemId(int position) {
            return position;
        }
        @Override public View getView(int position, View convertView, ViewGroup parent) {
            Holder holder = null;
            if( convertView == null ) {
                // convertView가 없으면 초기화합니다.
                convertView = new LinearLayout(context);
                ((LinearLayout) convertView).setOrientation(LinearLayout.HORIZONTAL);
                TextView tvId = new TextView(context);
                tvId.setPadding(10, 0, 20, 0);
                tvId.setTextColor(Color.rgb(0, 0, 0));
                TextView tvName = new TextView(context);
                tvName.setPadding(20, 0, 20, 0);
                tvName.setTextColor(Color.rgb(0, 0, 0));
                TextView tvAge = new TextView(context);
                tvAge.setPadding(20, 0, 20, 0);
                tvAge.setTextColor(Color.rgb(0, 0, 0));
                TextView tvPhone = new TextView(context);
                tvPhone.setPadding(20, 0, 20, 0);
                tvPhone.setTextColor(Color.rgb(0, 0, 0));
                ((LinearLayout) convertView).addView(tvId);
                ((LinearLayout) convertView).addView(tvName);
                ( (LinearLayout) convertView).addView(tvAge);
                ( (LinearLayout) convertView).addView(tvPhone);
                holder = new Holder();
                holder.tvId = tvId;
                holder.tvName = tvName;
                holder.tvAge = tvAge;
                holder.tvPhone = tvPhone;
                convertView.setTag(holder);
            }
            else {
//             convertView가 있으면 홀더를 꺼냅니다.
                holder = (Holder) convertView.getTag();
            } // 한명의 데이터를 받아와서 입력합니다.
            Person person = (Person) getItem(position);
            holder.tvId.setText(person.get_id() + "");
            holder.tvName.setText(person.getName());
            holder.tvAge.setText(person.getAge() + "");
            holder.tvPhone.setText(person.getPhone());
            return convertView;
        }
    } /** * 홀더 */
    private class Holder {
        public TextView tvId;
        public TextView tvName;
        public TextView tvAge;
        public TextView tvPhone;
    }
}


