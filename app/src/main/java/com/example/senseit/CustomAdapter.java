package com.example.senseit;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class CustomAdapter extends BaseAdapter {

    ArrayList<String> column1;
    ArrayList<String> column2;
    Context context;

    CustomAdapter(Context context, ArrayList<String> column1, ArrayList<String> column2){

        this.context = context;
        this.column1 = column1;
        this.column2 = column2;

    }
    @Override
    public int getCount() {
        return column1.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if(convertView == null){
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.listview_history, parent, false);
        }
        if (position % 2 == 1) { // Giving a background shade to rows
            convertView.setBackgroundColor(Color.parseColor("#ebebef"));
        } else {
            convertView.setBackgroundColor(Color.parseColor("#e5e5ea"));
        }
        TextView column1_view = (TextView) convertView.findViewById(R.id.column1);
        TextView column2_view = (TextView) convertView.findViewById(R.id.column2);

        column1_view.setText(column1.get(position));
        column2_view.setText(column2.get(position));

        return convertView;
    }
}
