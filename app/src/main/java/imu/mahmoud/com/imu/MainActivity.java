package imu.mahmoud.com.imu;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    SensorManager mSensorManager;
    TextView timestamps;
    TextView values;

    FileOutputStream fileOutputStream;
    OutputStreamWriter outputStreamWriter;

    Sensor sensor;

    ToggleButton button;
    Button button2;
    ToggleButton button3;
    boolean isDiscontinuousWriteEnabled = false;
    boolean nullifyQuats = false;

    File file;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
        ActivityCompat.requestPermissions(this, permissions, 1);

        button = findViewById(R.id.button);
        button2 = findViewById(R.id.button2);
        button3 = findViewById(R.id.button3);

        button.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b) {
                    startIMU();
                } else {
                    stopIMU();
                }
            }
        });

        button3.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b) {
                    isDiscontinuousWriteEnabled = true;
                    button.setEnabled(false);

                    nullifyQuats = true;
                    startIMU();
                } else {
                    isDiscontinuousWriteEnabled = false;
                    button.setEnabled(true);

                    stopIMU();
                }
            }
        });

        button2.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if(motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    nullifyQuats = false;
                } else if(motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    nullifyQuats = true;
                }
                return false;
            }
        });

        timestamps = findViewById(R.id.timestamp);
        values = findViewById(R.id.values);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1:
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //Granted.
                    File documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
                    File appDir = new File(documentsDir, "imu_records/");
                    appDir.mkdirs();
                    file = new File(appDir, "imu_record.csv");
                    try {
                        file.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    // Denied.
                    Log.e(TAG, "Create file denied");
                }
                break;
        }
    }

    @Override
    protected void onDestroy() {
        try {
            outputStreamWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if(sensorEvent.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            long timestamp = Calendar.getInstance().getTime().getTime();
            timestamps.setText(String.format(Locale.US, "%d", timestamp));
            String str = "";

            values.setText(String.format(Locale.US, "%10.7f\n%10.7f\n%10.7f\n%10.7f",
                    sensorEvent.values[3],
                    sensorEvent.values[0],
                    sensorEvent.values[1],
                    sensorEvent.values[2]));

            // timestamp,getTracing,w,x,y,z
            str = String.format(Locale.US, "%d,%10.7f,%10.7f,%10.7f,%10.7f,%d\n",
                    timestamp,
                    sensorEvent.values[3],
                    sensorEvent.values[0],
                    sensorEvent.values[1],
                    sensorEvent.values[2],
                    nullifyQuats?0:1);

            try {
                outputStreamWriter.write(str);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private void startIMU() {
        Log.i(TAG, "IMU started");

        try {
            fileOutputStream = new FileOutputStream(file);
            outputStreamWriter = new OutputStreamWriter(fileOutputStream);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        // Initialize sensor
        sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST);
    }

    private void stopIMU() {
        Log.i(TAG, "IMU stopped");
        mSensorManager.unregisterListener(this);
        try {
            outputStreamWriter.flush();
            outputStreamWriter.close();
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
