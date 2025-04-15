package com.example.senseit;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Calendar;
import java.util.Date;

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

public class MainActivity extends AppCompatActivity implements SensorEventListener, View.OnClickListener {
    //Defined variables
    RelativeLayout light_card, proxy_card, accelerometer_card, gyro_card;
    TextView heart_rate_value, accelerometer_value, gyro_value;
    DatabaseHelper dHelper;

    Button startButton, stopButton;
    //Sensor related variables
    SensorManager sensorManager;
    Sensor heart_rate_sensor, accelerometer_sensor, gyroscope_sensor;
    SensorValue sensor_values;
    Handler handler;
    Runnable runnable;
    String csv;
    //Customs
    Boolean goingHistory;
    Boolean serviceStopped;

    String serviceStart;
    Date currentTime;
    SharedPreferences sharedpreferences;
    public static final String MyPREFERENCES = "MyPrefs" ;
    public static final String ServiceStart = "serviceStart";
    @SuppressLint("BatteryLife")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO); // tp turn of the default night mode
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();

        // To ignore battery optimization for foreground service
        // To whitelist the application
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent();
            String packageName = getPackageName();
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
                startActivity(intent);
            }
        }

        // Custom Binding and initializing to OnclickListener

        startButton = findViewById(R.id.startButton);
        stopButton = findViewById(R.id.stopButton);
        proxy_card = findViewById(R.id.heart_rate_card);
        proxy_card.setOnClickListener(this);
        accelerometer_card = findViewById(R.id.accelerometer_card);
        accelerometer_card.setOnClickListener(this);
        gyro_card = findViewById(R.id.gyro_card);
        gyro_card.setOnClickListener(this);


        heart_rate_value = findViewById(R.id.heart_rate_value);
        accelerometer_value = findViewById(R.id.accelerometer_value);
        gyro_value = findViewById(R.id.gyro_value);

        // Sensor binding
        sensorManager = (SensorManager) getSystemService(Service.SENSOR_SERVICE);
        sensor_values = new SensorValue();

//        if(sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)!=null) {
//            light_sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
//        }else{
//            light_value.setText(R.string.not_available_light);
//        }
        if (sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE) != null) {
            heart_rate_sensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
        } else {
            heart_rate_value.setText(R.string.not_available_proxy);
        }
        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            accelerometer_sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        } else {
            accelerometer_value.setText(R.string.not_available_accelerometer);
        }
        if (sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null) {
            gyroscope_sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        } else {
            gyro_value.setText(R.string.not_available_gyro);
        }
//        heart_rate_sensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
//        accelerometer_sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
//        gyroscope_sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        sensorManager.registerListener(this, heart_rate_sensor, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, accelerometer_sensor, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, gyroscope_sensor, SensorManager.SENSOR_DELAY_GAME);


        //Saving data every 5 minutes when in application
//        handler = new Handler(); // handler to save the sensor data to database every 5 minute
//        final int delay =3000; // 1000 milliseconds == 1 second
//        runnable = new Runnable() {
//            public void run() {
//                Log.d("DONE", "save_data1");
//                Log.d("DONE", Arrays.toString(sensor_values.accelerometer_value));
////                long[] unused = new DatabaseHelper(MainActivity.this).insertData(sensor_values);
//                handler.postDelayed(this, delay);
//            }
//        };
//        handler.postDelayed(runnable,delay);

//         csv = (Environment.getExternalStorageDirectory().getAbsolutePath() + "/MyCsvFile.csv");
        sharedpreferences = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedpreferences.edit();
        Boolean ser = sharedpreferences.getBoolean(ServiceStart, false);
            if(ser){
                startButton.setVisibility(View.GONE);
                stopButton.setVisibility(View.VISIBLE);
            }else {
                stopButton.setVisibility(View.GONE);
                startButton.setVisibility(View.VISIBLE);
            }
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // Starting the foreground services with the live notification when application is minimized
                Toast.makeText(getApplicationContext(), Html.fromHtml("<font color='" + getResources().getColor(R.color.dark_yellow) + "' >" + "Service Started!" + "</font>"), Toast.LENGTH_SHORT).show();
                Intent serviceIntent = new Intent(getApplicationContext(), ForegroundProcess.class);
                serviceIntent.putExtra("sensor_values", sensor_values);
                serviceIntent.putExtra("bool", true);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent);
                }
                Globe.inApp = false;
                startButton.setVisibility(View.GONE);
                stopButton.setVisibility(View.VISIBLE);

                editor.putBoolean(ServiceStart , true);
                editor.commit();
                
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Toast.makeText(getApplicationContext(), Html.fromHtml("<font color='" + Color.GREEN + "' >" + "Service Stopped!" + "</font>"), Toast.LENGTH_SHORT).show();
                Intent serviceIntent = new Intent(getApplicationContext(), ForegroundProcess.class);
                serviceIntent.putExtra("sensor_values", sensor_values);
                serviceIntent.putExtra("bool", false);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent);
                }
                stopButton.setVisibility(View.GONE);
                startButton.setVisibility(View.VISIBLE);
                editor.putBoolean(ServiceStart , false);
                editor.commit();
            }


        });
    }

    @Override
    protected void onPause() {
//        Toast.makeText(this, "onPause", Toast.LENGTH_SHORT).show();

//        if (!goingHistory && !serviceStopped) {
//            // Starting the foreground services with the live notification when application is minimized
//            Intent serviceIntent = new Intent(this, ForegroundProcess.class);
//            serviceIntent.putExtra("sensor_values", sensor_values);
//            serviceIntent.putExtra("bool", true);
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                startForegroundService(serviceIntent);
//            }
//            Globe.inApp = false;
//        }
//        sensorManager.unregisterListener(this);
        super.onPause();

    }

    @Override
    protected void onStart() {
        super.onStart();
//        Toast.makeText(this, "onStart", Toast.LENGTH_SHORT).show();
        // Registering the sensors
//        sensorManager.registerListener(this, light_sensor, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, heart_rate_sensor, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, accelerometer_sensor, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, gyroscope_sensor, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Foreground service is being killed when application opens
//        goingHistory = false;
//        serviceStopped = false;
//        if (!Globe.inApp) {
//            Toast.makeText(this, "onResume", Toast.LENGTH_SHORT).show();
//            Intent serviceIntent = new Intent(this, ForegroundProcess.class);
//            serviceIntent.putExtra("sensor_values", sensor_values);
//            serviceIntent.putExtra("bool", false);
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                startForegroundService(serviceIntent);
//            }
//        }
    }


    @Override
    public void onSensorChanged(SensorEvent event) {

        // Getting values from sensors on every changes
//        if(event.sensor.getType() == Sensor.TYPE_LIGHT){
//            light_value.setText(String.valueOf(event.values[0]));
//        }
        if (event.sensor.getType() == Sensor.TYPE_HEART_RATE) {
            heart_rate_value.setText("HEART RATE\n" + String.valueOf(Double.parseDouble(String.format("%.2f", event.values[0]))));
        }
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            accelerometer_value.setText("ACCELEROMETER\nX:" + String.format("%.2f", event.values[0]) + "  Y:" + String.format("%.2f", event.values[1]) + "  Z:" + String.format("%.2f", event.values[2]));//+ "(m/s^2)"
        }
        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            gyro_value.setText("GYROSCOPE\nX:" + String.format("%.2f", event.values[0]) + "  Y:" + String.format("%.2f", event.values[1]) + "  Z:" + String.format("%.2f", event.values[2]));//+ " (rad/s)"

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sensorManager.unregisterListener(this); // unregister the sensors on destroy

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        return true;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.stop_fg: // Menu button to stop the foreground service
                if (!serviceStopped) {
                    Toast.makeText(this, Html.fromHtml("<font color='" + Color.RED + "' >" + "Background Notification Service Stopped!" + "</font>"), Toast.LENGTH_SHORT).show();
                    item.setTitle("START NOTIFY SERVICE");
                    item.setIcon(R.drawable.ic_baseline_circle_24);
                    serviceStopped = true;
                } else {
                    serviceStopped = false;
                    item.setTitle("STOP NOTIFY SERVICE");
                    item.setIcon(R.drawable.ic_baseline_warning_24_red);
                    Toast.makeText(this, Html.fromHtml("<font color='" + getResources().getColor(R.color.dark_yellow) + "' >" + "Background Notification Service Started!" + "</font>"), Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.exit: // To exit the application
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("Are you sure you want to exit?")
                        .setNegativeButton("No", null)
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
//                                sensorManager.unregisterListener(MainActivity.this);
                                finishAffinity();
                            }
                        }).show();
                break;

        }
        return true;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {

        // response to click on cards
        switch (view.getId()) {
//            case R.id.light_card:
//                goToHistory(1);
//                break;
//            case R.id.heart_rate_card:
//                goToHistory(2);
//                break;
//            case R.id.accelerometer_card:
//                goToHistory(3);
//                break;
//            case R.id.gyro_card:
//                goToHistory(4);
//                break;
        }

    }

    private void goToHistory(int id) {

        // To history activity
        goingHistory = true;
        Intent history_intent = new Intent(MainActivity.this, HistoryActivity.class);
        history_intent.putExtra("sensor_id", id);
        startActivity(history_intent);
    }

    String timeNow = "";

    public void onSyncClick(View view) {
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

        ApiService apiService = retrofit.create(ApiService.class);

        RequestBody requestBody = RequestBody.create(MediaType.parse("text/csv"), csvFile);
        Call<ResponseBody> call = apiService.uploadCsv(str, requestBody);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    // Handle success

                    Log.d("done", "response.isSuccessful " + response.toString());
//                    Boolean deletedata = dHelper.delete_all_data();
//                    if (deletedata) {
                    Toast.makeText(getApplicationContext(), "Sync Successfull", Toast.LENGTH_SHORT).show();
//                    }
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