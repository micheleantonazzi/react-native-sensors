package com.sensors;

import android.os.Bundle;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.support.annotation.Nullable;
import android.view.Surface;
import android.view.WindowManager;
import android.content.Context;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.modules.core.DeviceEventManagerModule;

public class AbsoluteRotationVector extends ReactContextBaseJavaModule implements SensorEventListener {

  private final ReactApplicationContext reactContext;
  private final SensorManager sensorManager;
  private final Sensor sensor;
  private double lastReading = (double) System.currentTimeMillis();
  private int interval;
  private Arguments arguments;
  private int logLevel = 0;

  public AbsoluteRotationVector(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
    this.sensorManager = (SensorManager)reactContext.getSystemService(reactContext.SENSOR_SERVICE);
    this.sensor = this.sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
  }

  // RN Methods
  @ReactMethod
  public void isAvailable(Promise promise) {
    if (this.sensor == null) {
      // No sensor found, throw error
      promise.reject(new RuntimeException("No AbsoluteRotationVector found"));
      return;
    }
    promise.resolve(null);
  }

  @ReactMethod
  public void setUpdateInterval(int newInterval) {
    this.interval = newInterval;
  }

  @ReactMethod
  public void setLogLevel(int newLevel) {
    this.logLevel = newLevel;
  }

  @ReactMethod
  public void startUpdates() {
    // Milisecond to Mikrosecond conversion
    sensorManager.registerListener(this, sensor, this.interval * 1000);
  }

  @ReactMethod
  public void stopUpdates() {
    sensorManager.unregisterListener(this);
  }

  @Override
  public String getName() {
    return "AbsoluteRotationVector";
  }

  // SensorEventListener Interface
  private void sendEvent(String eventName, @Nullable WritableMap params) {
    try {
      this.reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
        .emit(eventName, params);
    } catch (RuntimeException e) {
      Log.e("ERROR", "java.lang.RuntimeException: Trying to invoke Javascript before CatalystInstance has been set!");
    }
  }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
      double tempMs = (double) System.currentTimeMillis();
      if (tempMs - lastReading >= interval){
        lastReading = tempMs;

        Sensor mySensor = sensorEvent.sensor;
        WritableMap map = arguments.createMap();

        if (mySensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
          float[] rotationMatrix = new float[9];
          SensorManager.getRotationMatrixFromVector(rotationMatrix, sensorEvent.values);

          final int worldAxisForDeviceAxisX;
          final int worldAxisForDeviceAxisY;

          int screenOrientation = ((WindowManager) reactContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getOrientation();

          // Remap the axes as if the device screen was the instrument panel,
          // and adjust the rotation matrix for the device orientation.
          switch (screenOrientation) {
            case Surface.ROTATION_0:
            default:
              worldAxisForDeviceAxisX = SensorManager.AXIS_X;
              worldAxisForDeviceAxisY = SensorManager.AXIS_Z;
              break;
            case Surface.ROTATION_90:
              worldAxisForDeviceAxisX = SensorManager.AXIS_Z;
              worldAxisForDeviceAxisY = SensorManager.AXIS_MINUS_X;
              break;
            case Surface.ROTATION_180:
              worldAxisForDeviceAxisX = SensorManager.AXIS_MINUS_X;
              worldAxisForDeviceAxisY = SensorManager.AXIS_MINUS_Z;
              break;
            case Surface.ROTATION_270:
              worldAxisForDeviceAxisX = SensorManager.AXIS_MINUS_Z;
              worldAxisForDeviceAxisY = SensorManager.AXIS_X;
              break;
          }

          float[] adjustedRotationMatrix = new float[9];
          SensorManager.remapCoordinateSystem(rotationMatrix, worldAxisForDeviceAxisX,
                  worldAxisForDeviceAxisY, adjustedRotationMatrix);

          // Transform rotation matrix into azimuth/pitch/roll
          float[] orientation = new float[3];
          SensorManager.getOrientation(adjustedRotationMatrix, orientation);

          map.putDouble("x", orientation[0]);
          map.putDouble("y", orientation[1]);
          map.putDouble("z", orientation[2]);
          map.putDouble("timestamp", (double) System.currentTimeMillis());
          sendEvent("AbsoluteRotationVector", map);
        }
      }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
