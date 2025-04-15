package com.example.senseit;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;

public class HistoryActivity extends AppCompatActivity {

    public ListView listView;
    public DatabaseHelper databaseHelper;
    public Integer sensor_id;
    TextView title;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        Objects.requireNonNull(getSupportActionBar()).setTitle("HISTORY"); // Changing title name
        getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Back to home option enabled

        Globe.inApp = true;

        //Bindings
        listView = findViewById(R.id.l_view);
        databaseHelper = new DatabaseHelper(this);
        title = findViewById(R.id.history_title);

        sensor_id = getIntent().getIntExtra("sensor_id", -1);
        loadData(sensor_id);
    }

    private void loadData(int id) { // Load data from database to show on listView
        ArrayList<String> column1 = new ArrayList<>();
        ArrayList<String> column2 = new ArrayList<>();
        Cursor results;
        switch (id) {
//            case 1:
//                results = databaseHelper.getLightData();
//                title.setText(getString(R.string.light_history_title));
//                break;
            case 2:
                title.setText(getString(R.string.proxy_history_title));
                results = databaseHelper.getProxyData();
                break;
            case 3:
                title.setText(getString(R.string.acc_history_title));
                results = databaseHelper.getAccelerometerData();
                break;
            case 4:
                title.setText(getString(R.string.gyro_history_title));
                results = databaseHelper.getGyroData();
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + id);
        }


        if (results.getCount() == 0) { // Add data to listView
            Toast.makeText(this, "No Data Available for this sensor!", Toast.LENGTH_SHORT).show();
        } else {
            if (id == 1 || id == 2) {
                while (results.moveToNext()) {
                    column1.add(results.getString(2));
                    column2.add(results.getString(3));
                }
            } else {
                while (results.moveToNext()) {
                    column1.add(results.getString(2));
                    column2.add("X: " + results.getString(3) + "  Y: " + results.getString(4) + "  Z: " + results.getString(5));
                }
            }
        }
        Collections.reverse(column1);
        Collections.reverse(column2);

        CustomAdapter adapter = new CustomAdapter(this, column1, column2);
        listView.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.history_options_menu, menu);
        for(int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            SpannableString spanString = new SpannableString(menu.getItem(i).getTitle().toString());
            spanString.setSpan(new ForegroundColorSpan(Color.RED), 0,     spanString.length(), 0); //fix the color to white
            item.setTitle(spanString);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        // To clear all data for a specific sensor with a confirmation dialogue
        if (item.getItemId() == R.id.clear) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Want to clear all data for this Sensor?")
                    .setNegativeButton("No", null)
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if(databaseHelper.delete_data(sensor_id)){
                                ArrayList<String> null1 = new ArrayList<>();
                                ArrayList<String> null2 = new ArrayList<>();
                                Toast.makeText(HistoryActivity.this, "SUCCESSFULLY DELETED!", Toast.LENGTH_SHORT).show();
                                CustomAdapter adapter = new CustomAdapter(HistoryActivity.this, null1, null2);
                                listView.setAdapter(adapter);
                            }else {
                                Toast.makeText(HistoryActivity.this, Html.fromHtml("<font color='"+ Color.RED +"' >" + "NO DATA FOUND!" + "</font>"), Toast.LENGTH_SHORT).show();
                            }
                        }
                    }).show();
        }
        return true;
    }
}