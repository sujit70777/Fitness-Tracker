package com.example.senseit;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.NotificationCompat;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static com.example.senseit.Globe.CHANNEL_ID;

import com.opencsv.CSVWriter;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public class ForegroundProcess extends Service implements SensorEventListener {
    SensorValue sensor_values;
    public Boolean aBoolean;
    PendingIntent pendingIntent;
    Handler handler, synchandler;
    PowerManager.WakeLock wakeLock;
    Runnable runnable, syncrunnable;

    //Sensor variables
    SensorManager sensorManager;
    Sensor heart_rate_sensor, accelerometer_sensor, gyroscope_sensor;
    private static final int NOTIFICATION_ID = 101;

    //Database related variables
    DatabaseHelper databaseHelper, testHelper;
    Intent intent;
    int count, waitCount;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("DONE", "onStartCommand");
        // registering the sensors
//        sensorManager.registerListener(this, light_sensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, heart_rate_sensor, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, accelerometer_sensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, gyroscope_sensor, SensorManager.SENSOR_DELAY_NORMAL);

        sensor_values = (SensorValue) intent.getSerializableExtra("sensor_values");
        aBoolean = intent.getBooleanExtra("bool", true);
        Intent notifyIntent = new Intent(this, MainActivity.class);
        notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        pendingIntent = PendingIntent.getActivity(this, 11, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = createNotification();

//        handler = new Handler(); // handler to save the sensor data to database every 5 minute
//        final int delay =10000; // 1000 milliseconds == 1 second
//        runnable = new Runnable() {
//            public void run() {
//                Log.d("DONE", "save_data onStartCommand");
//                Log.d("DONE", Arrays.toString(sensor_values.accelerometer_value));
////                save_data(sensor_values);
////                handler.postDelayed(this, delay);
//            }
//        };
//        handler.postDelayed(runnable,delay);

        if (aBoolean) {
            startForeground(NOTIFICATION_ID, notification);
        } else {
            stopForeground(true); // Stops foreground service for a button click on mainActivity
            sensorManager.unregisterListener(this);
            handler.removeCallbacks(runnable);
//            synchandler.removeCallbacks(syncrunnable);


        }
        return START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        Log.d("DONE", "FORGROUND SERVICE");
//        sensor_values =(SensorValue) intent.getSerializableExtra("sensor_values");
        // Creating a wake lock service
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "SENSEit::WakelockTag");
        wakeLock.acquire(10 * 60 * 1000L /*10 minutes*/);

        sensorManager = (SensorManager) getSystemService(Service.SENSOR_SERVICE);
//        if(sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)!=null) {
//            light_sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
//        }
        if (sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE) != null) {
            heart_rate_sensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
        }
        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            accelerometer_sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        if (sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null) {
            gyroscope_sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        }

//        testHelper = new DatabaseHelper(this);
//
//        Cursor testCursor = testHelper.getProxyData();
//
//        while (testCursor.moveToNext()){
//            String s = testCursor.getString(2)+" ,X - "+testCursor.getString(3)+",Y - "+testCursor.getString(4)+" ,Z - "+testCursor.getString(5);
//            Log.d("done", s);
//        }

        count = 0;
        waitCount = Config.SensorDataSyncInterval / Config.SensorDataReadingInterval;

        //Database related
        databaseHelper = new DatabaseHelper(this);
        SQLiteDatabase sqLiteDatabase = databaseHelper.getWritableDatabase();

        handler = new Handler(); // handler to save the sensor data to database every 5 minute
        final int delay = Config.SensorDataReadingInterval; // 1000 milliseconds == 1 second
        runnable = new Runnable() {
            public void run() {
                Log.d("DONE", "save_data");
                Log.d("DONE", Arrays.toString(sensor_values.accelerometer_value));
                save_data(sensor_values);

                if (waitCount < count) {
                    onSync();
                    Log.d("DONE", "sync_data----------------------");
                    count = 0;
                }
                count++;
                handler.postDelayed(this, delay);
            }
        };
        handler.postDelayed(runnable, delay);


//        synchandler = new Handler(); // handler to save the sensor data to database every 5 minute
//        final int syncdelay = Config.SensorDataSyncInterval; // 1000 milliseconds == 1 second
//        syncrunnable = new Runnable() {
//            public void run() {
//                Log.d("DONE", "sync_data");
//                onSync();
//                handler.postDelayed(this, syncdelay);
//            }
//        };
//        synchandler.postDelayed(runnable,syncdelay);


    }

    private Notification createNotification() {

        // creating notification for the foreground service with the live sensor values updated
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.senseit_logo)
                .setContentTitle(getString(R.string.sensing_title) + ":")
                .setContentText("Click expand to see the live values")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Heart Rate sensor value: " + sensor_values.heart_rate_value
                                + "\nAccelerometer sensor values: X:" + sensor_values.accelerometer_value[0] + " Y:" + sensor_values.accelerometer_value[1] + " Z:" + sensor_values.accelerometer_value[2]
                                + "\nGyroscope sensor values: X:" + sensor_values.gyro_value[0] + " Y:" + sensor_values.gyro_value[1] + " Z:" + sensor_values.gyro_value[2]))
                .setAutoCancel(false)
                .setOngoing(true)
                .setWhen(System.currentTimeMillis())
                .setContentIntent(pendingIntent)
                .setOnlyAlertOnce(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sensorManager.unregisterListener(this);
        stopForeground(true);
        wakeLock.release();
        handler.removeCallbacks(runnable);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    @SuppressLint("DefaultLocale")
    @Override
    public void onSensorChanged(SensorEvent event) {

//        if(event.sensor.getType() == Sensor.TYPE_LIGHT){
//            sensor_values.light_value = event.values[0];
//        }
        if (event.sensor.getType() == Sensor.TYPE_HEART_RATE) {
            Log.d("DONE", Arrays.toString(sensor_values.accelerometer_value));
            sensor_values.heart_rate_value = Double.parseDouble(String.format("%.2f", event.values[0]));
        }
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            sensor_values.accelerometer_value[0] = Double.parseDouble(String.format("%.2f", event.values[0]));
            sensor_values.accelerometer_value[1] = Double.parseDouble(String.format("%.2f", event.values[1]));
            sensor_values.accelerometer_value[2] = Double.parseDouble(String.format("%.2f", event.values[2]));
        }
        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            sensor_values.gyro_value[0] = Double.parseDouble(String.format("%.2f", event.values[0]));
            sensor_values.gyro_value[1] = Double.parseDouble(String.format("%.2f", event.values[1]));
            sensor_values.gyro_value[2] = Double.parseDouble(String.format("%.2f", event.values[2]));
        }

        NotificationManager notificationManager = (NotificationManager)
                getSystemService(Context.NOTIFICATION_SERVICE);
        if (aBoolean) {
            //startForeground(NOTIFICATION_ID, createNotification());
            notificationManager.notify(NOTIFICATION_ID, createNotification());
        } else {
            stopForeground(true);
            sensorManager.unregisterListener(this);
            wakeLock.release();
            handler.removeCallbacks(runnable);
        }
    }

    public void save_data(SensorValue sensor_values) { // Saving the data to Database
        long[] row_ids = databaseHelper.insertData(sensor_values);

        for (long row_id : row_ids) { // Checking for error when inserting data as its returns -1 when gives an error otherwise the row number
            if (row_id == -1) {
                Toast.makeText(this, Html.fromHtml("<font color='" + Color.RED + "' >" + "ERROR INSERTION!" + "</font>"), Toast.LENGTH_SHORT).show();
                break;
            }
        }
        //Toast.makeText(this, ""+ Arrays.toString(row_ids), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


    String timeNow = "";
    Date currentTime;
    DatabaseHelper dHelper;
    String csv;

    public void onSync() {
        currentTime = Calendar.getInstance().getTime();

        timeNow = currentTime.toString();
        dHelper = new DatabaseHelper(this);

        Cursor testCursor = dHelper.getAllData();

//        while (testCursor.moveToNext()){
//            String s = testCursor.getString(1)+ ", "+testCursor.getString(2)+ ", "+" ,X - "+testCursor.getString(3)+",Y - "+testCursor.getString(4)+" ,Z - "+testCursor.getString(5);
//            Log.d("done", s);
//        }
        Log.d("done", testCursor.getCount() + "");
        Log.d("done", Arrays.toString(testCursor.getColumnNames()));
        csv = (Environment.getExternalStorageDirectory().getAbsolutePath() + "/MyCsvFile.csv");
        Log.d("done", csv);
        CSVWriter writer = null;

        try {
            writer = new CSVWriter(new FileWriter(csv));

            List<String[]> data = new ArrayList<String[]>();
            data.add(testCursor.getColumnNames());


            while (testCursor.moveToNext()) {
                //Which column you want to exprort
                String sen = "";
                if (testCursor.getString(1).equals("1")) {
                    sen = "HEART RATE";
                } else if (testCursor.getString(1).equals("2")) {
                    sen = "ACCELEROMETER";

                } else if (testCursor.getString(1).equals("3")) {
                    sen = "GYROSCOPE";
                }
                String arrStr[] = {testCursor.getString(0), sen, testCursor.getString(2), testCursor.getString(3), testCursor.getString(4), testCursor.getString(5)};
                data.add(arrStr);
            }

            writer.writeAll(data); // data is adding to csv

            writer.close();
            testCursor.close();
//            callRead();
        } catch (IOException e) {
            e.printStackTrace();
        }
        uploadCsv(new File(csv));


    }


    public interface ApiService {

        @PUT("{name}")
        Call<ResponseBody> uploadCsv(@Path("name") String name, @Body RequestBody requestBody);
    }

    public static class getTime {
        static Date c = Calendar.getInstance().getTime();
        public static final String s = "" + c.toString();
    }


    private static final String BASE_URL = Config.BaseUrl;

    public void uploadCsv(File csvFile) {
        Date c = Calendar.getInstance().getTime();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-mm-dd_hh:mm:ss");
        String strDate = dateFormat.format(c);
        String str = strDate + ".csv";
        OkHttpClient okHttpClient = new OkHttpClient.Builder().build();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ForegroundProcess.ApiService apiService = retrofit.create(ForegroundProcess.ApiService.class);

        RequestBody requestBody = RequestBody.create(MediaType.parse("text/csv"), csvFile);
        Call<ResponseBody> call = apiService.uploadCsv(str, requestBody);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    // Handle success

                    Log.d("done", "response.isSuccessful " + response.toString());
                    Boolean deletedata = dHelper.delete_all_data();
                    if (deletedata) {
                        Toast.makeText(getApplicationContext(), "Sync Successfull", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // Handle error
                    Log.d("done", "response.UNSuccessful " + response.toString());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                // Handle failure
            }
        });
    }

}
