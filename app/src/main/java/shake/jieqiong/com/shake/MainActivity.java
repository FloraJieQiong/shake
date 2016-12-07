package shake.jieqiong.com.shake;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.util.Log;

public class MainActivity extends Activity {

    private static final String TAG = "###MainActivity";
    private static final int SHAKE_START = 1;
    private static final int SHAKE_END = 2;

    SensorManager sensorManager;
    ShakeSensorListener shakeListener;
    private boolean isShake;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SHAKE_START:
                    isShake = true;
                    Log.d(TAG, "handleMessage:: start shake SHAKE_START");

                    break;
                case SHAKE_END:
                    Log.d(TAG, "handleMessage:: end shake SHAKE_END");
                    mHandler.removeMessages(SHAKE_END);
                    isShake = false;
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //获取 sensorManager
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        shakeListener = new ShakeSensorListener();
    }


    @Override
    protected void onResume() {
        super.onResume();
        //register AccelerometerSensor
        if (null != sensorManager) {
            Log.v(TAG, "onResume:: register TYPE_ACCELEROMETER");
            sensorManager.registerListener(shakeListener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (null != sensorManager) {
            sensorManager.unregisterListener(shakeListener);
        }
    }

    private class ShakeSensorListener implements SensorEventListener {
        //检测的时间间隔
        static final int UPDATE_INTERVAL = 100;
        //上一次检测的时间
        long mLastUpdateTime;
        //上一次检测时，加速度在x、y、z方向上的分量，用于和当前加速度比较求差。
        float mLastX, mLastY, mLastZ;

        //摇晃检测阈值，决定了对摇晃的敏感程度，越小越敏感。
        public int shakeThreshold = 500;
        private static final int ACCELERATE_VALUE = 20;

        @Override
        public void onSensorChanged(SensorEvent event) {

            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                long currentTime = System.currentTimeMillis();
                long diffTime = currentTime - mLastUpdateTime;
                if (diffTime < UPDATE_INTERVAL) {
                    return;
                }
                mLastUpdateTime = currentTime;
                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];
                float deltaX = x - mLastX;
                float deltaY = y - mLastY;
                float deltaZ = z - mLastZ;
                mLastX = x;
                mLastY = y;
                mLastZ = z;
                float delta = (float) (
                        Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ) / diffTime *
                        10000);
                // 当加速度的差值大于指定的阈值，认为这是一个摇晃
                if (delta > shakeThreshold) {
                    Log.v(TAG, "onSensorChanged:: shake");
                    Vibrator vibrator = (Vibrator) getSystemService(Service.VIBRATOR_SERVICE);
                    vibrator.vibrate(100);

                    if (!isShake) {
                        mHandler.sendEmptyMessage(SHAKE_START);
                    }
                    mHandler.removeMessages(SHAKE_END);
                } else if (isShake &&
                           (Math.abs(x) < ACCELERATE_VALUE && Math.abs(y) < ACCELERATE_VALUE &&
                            Math.abs(z) < ACCELERATE_VALUE)) {
                    mHandler.sendEmptyMessageDelayed(SHAKE_END, 300);
                }
            }

        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    }
}
