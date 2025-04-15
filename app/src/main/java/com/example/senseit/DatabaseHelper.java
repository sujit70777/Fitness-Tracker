package com.example.senseit;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.text.Html;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {

    //Table related variables
    private static final String DATABASE_NAME = "SENSEit.db";
    private static final String TABLE_NAME = "sensor_value_timestamp";
    private static final Integer VERSION_NUMBER = 3;
    public final Context context;

    //COLUMN NAMES
    private static final String ID = "_id";
    private static final String TIME = "time";
    private static final String SENSOR_ID = "sensor_id";
    private static final String VALUE_X= "value_x";
    private static final String VALUE_Y= "value_y";
    private static final String VALUE_Z= "value_z";

    //SQL Commands
    private static final String CREATE_TABLE = "CREATE TABLE "+TABLE_NAME+"( "+ID+" INTEGER PRIMARY KEY AUTOINCREMENT, "
                                                +SENSOR_ID+" INTEGER, "
                                                +TIME+" TIME, "
                                                +VALUE_X+" DOUBLE, "
                                                +VALUE_Y+" DOUBLE, "
                                                +VALUE_Z+" DOUBLE ); "; // SQL command for creating table

    private static final String DROP_TABLE = "DROP TABLE IF EXISTS "+TABLE_NAME; // SQL command for drop table
    private static final String SELECT_ALL = "SELECT * FROM "+TABLE_NAME; // SQL command to fetch all data
    // SQL command to extract specific sensor data
//    private static final String SELECT_LIGHT = "SELECT * FROM "+TABLE_NAME+" WHERE SENSOR_ID = 1";
    private static final String SELECT_HEARTRATE = "SELECT * FROM "+TABLE_NAME+" WHERE SENSOR_ID = 1";
    private static final String SELECT_ACCELEROMETER = "SELECT * FROM "+TABLE_NAME+" WHERE SENSOR_ID = 2";
    private static final String SELECT_GYRO = "SELECT * FROM "+TABLE_NAME+" WHERE SENSOR_ID = 3";
    private static final String DELETE_DATA = "DELETE FROM "+TABLE_NAME+" WHERE SENSOR_ID = "; // SQL command to delete data



    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, VERSION_NUMBER);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            Toast.makeText(context, Html.fromHtml("<font color='"+ Color.YELLOW +"' >" + "WELCOME" + "</font>"), Toast.LENGTH_SHORT).show();
            db.execSQL(CREATE_TABLE); // Creating table for the database
        }catch (Exception e){
            Toast.makeText(context, "Exception:" + e, Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        try {
            db.execSQL(DROP_TABLE); // Dropping table to upgrade to any changes
            onCreate(db);
        }catch (Exception e){
            Toast.makeText(context, "Exception:" + e, Toast.LENGTH_SHORT).show();
        }
    }

    public long[] insertData(SensorValue v){ // inserting data to Database

        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        String currentDateTime = new SimpleDateFormat("YYYY-MM-DD HH:mm", Locale.getDefault()).format(new Date());

        //Creating 4 content values to insert 4 sensor values to database
        long[] row_ids = new long[3];
//        contentValues.put(SENSOR_ID, 1);
//        contentValues.put(VALUE_X, v.light_value );
//        contentValues.put(TIME, currentDateTime);
//        row_ids[0] = sqLiteDatabase.insert(TABLE_NAME, null, contentValues);

        contentValues.put(SENSOR_ID, 1);
        contentValues.put(VALUE_X, v.heart_rate_value );
        contentValues.put(TIME, currentDateTime);
        row_ids[0] = sqLiteDatabase.insert(TABLE_NAME, null, contentValues);

        contentValues.put(SENSOR_ID, 2);
        contentValues.put(VALUE_X, v.accelerometer_value[0] );
        contentValues.put(VALUE_Y, v.accelerometer_value[1] );
        contentValues.put(VALUE_Z, v.accelerometer_value[2] );
        contentValues.put(TIME, currentDateTime);
        row_ids[1] = sqLiteDatabase.insert(TABLE_NAME, null, contentValues);

        contentValues.put(SENSOR_ID, 3);
        contentValues.put(VALUE_X, v.gyro_value[0] );
        contentValues.put(VALUE_Y, v.gyro_value[1] );
        contentValues.put(VALUE_Z, v.gyro_value[2] );
        contentValues.put(TIME, currentDateTime);
        row_ids[2] = sqLiteDatabase.insert(TABLE_NAME, null, contentValues);

        return row_ids;
    }

    public Cursor getAllData(){
        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
        return sqLiteDatabase.rawQuery(SELECT_ALL, null);
    }
//    public Cursor getLightData(){
//        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
//        return sqLiteDatabase.rawQuery(SELECT_LIGHT, null);
//    }
    public Cursor getProxyData(){
        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
        return sqLiteDatabase.rawQuery(SELECT_HEARTRATE, null);
    }
    public Cursor getAccelerometerData(){
        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
        return sqLiteDatabase.rawQuery(SELECT_ACCELEROMETER, null);
    }
    public Cursor getGyroData(){
        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
        return sqLiteDatabase.rawQuery(SELECT_GYRO, null);
    }

    public boolean delete_data(int sensor_id){ // To delete data for a specific sensor
        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
        String where = SENSOR_ID + "=" + sensor_id;
        return sqLiteDatabase.delete(TABLE_NAME, where, null) != 0;
    }

    public boolean delete_all_data(){ // To delete data for a specific sensor
        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();

        return sqLiteDatabase.delete(TABLE_NAME, null, null) != 0;
    }
}
