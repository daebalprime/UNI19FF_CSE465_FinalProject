package db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import vo.Person;

public abstract class DBHelper extends SQLiteOpenHelper {
    public static final int DB_VERSION = 3;
    private Context context;

    public DBHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version){
        super(context, name, factory, version);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db){
        StringBuffer sb = new StringBuffer();
        sb.append(" CREATE TABLE TEST_TABLE ( ");
        sb.append(" _ID INTEGER PRIMARY KEY AUTOINCREMENT, ");
        sb.append(" NAME TEXT, ");
        sb.append(" TRIAL INTEGER, ");
        sb.append(" TIMESTAMP REAL, ");
        sb.append(" ORIENTR REAL, ");
        sb.append(" ORIENTP REAL, ");
        sb.append(" ACCX REAL, ");
        sb.append(" ACCY REAL, ");
        sb.append(" ACCZ REAL ) ");

        db.execSQL(sb.toString());

        Toast.makeText(context, "Table 생성완료", Toast.LENGTH_SHORT).show();
    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
        Toast.makeText(context, "버젼이 올라갔습니다!" , Toast.LENGTH_SHORT).show();
    }
    public void testDB(){
        SQLiteDatabase db = getReadableDatabase();
    }

    public void addPerson(Person person){
        SQLiteDatabase db = getWritableDatabase();
        StringBuffer sb = new StringBuffer();
        sb.append(" INSERT INTO TEST_TABLE ( ");
        sb.append(" NAME, TRIAL, TIMESTAMP, ORIENTR, ORIENTP, ACCX, ACCY, ACCZ ) ");
        sb.append(" VALUES ( ?, ?, ?, ?, ?, ?, ?, ?) ");
        db.execSQL(sb.toString(),
                new Object[]{ person.getName(),
                        person.getTrial(),
                        person.getTimestamp(),
                        person.getOrientR(),
                        person.getOrientP(),
                        person.getAccX(),
                        person.getAccY(),
                        person.getAccZ()});
        Toast.makeText(context, "Insert 완료", Toast.LENGTH_SHORT).show();
    }
    public List getAllPersonData(){
        StringBuffer sb = new StringBuffer();
        sb.append(" SELECT _ID, NAME, TRIAL, TIMESTAMP, ORIENTR, ORIENTP FROM TEST_TABLE ");
        // 읽기 전용 DB 객체를 만든다.
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(sb.toString(), null);
        List people = new ArrayList();
        Person person = null;
        // moveToNext 다음에 데이터가 있으면 true 없으면 false
        while( cursor.moveToNext() ) {
            person = new Person();
            person.set_id(cursor.getInt(0));
            person.setName(cursor.getString(1));
            person.setTrial(cursor.getInt(2));
            person.setTimestamp(cursor.getDouble(3));
            person.setOrientR(cursor.getDouble(4));
            person.setOrientP(cursor.getDouble(5));
            people.add(person);
        }
        return people;
    }
}
