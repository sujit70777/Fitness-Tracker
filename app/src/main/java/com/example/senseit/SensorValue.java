package com.example.senseit;

import java.io.Serializable;

public class SensorValue implements Serializable {

    public double heart_rate_value = 0.00;
    public double[] accelerometer_value = {0.00, 0.00, 0.00};
    public double[] gyro_value = {0.00, 0.00, 0.00};

    public SensorValue() {
    }

    public SensorValue( double heart_rate_value, double[] accelerometer_value, double[] gyro_value) {
        this.heart_rate_value = heart_rate_value;
        this.accelerometer_value = accelerometer_value;
        this.gyro_value = gyro_value;
    }
}
