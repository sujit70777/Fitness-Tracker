package com.example.senseit;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

public class Globe extends Application {
    //Defined notification related variables
    public static final String CHANNEL_ID = "sensor_value_channel";
    public static final String CHANNEL_NAME = "Sensor Values";
    public static final String CHANNEL_DESC = "Here four sensor values will be shown";
    public static Boolean inApp = false;

    @Override
    public void onCreate() {
        super.onCreate();

        //Creating notification Channel
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ){
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW);
            channel.setDescription(CHANNEL_DESC);
            NotificationManager nManager = getSystemService(NotificationManager.class);
            nManager.createNotificationChannel(channel);
        }
    }

}
