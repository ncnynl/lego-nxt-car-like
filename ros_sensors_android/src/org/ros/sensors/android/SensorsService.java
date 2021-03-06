package org.ros.sensors.android;

import java.util.List;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Looper;
import android.os.SystemClock;

import org.ros.message.Time;
import org.ros.message.sensor_msgs.Imu;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Publisher;
import org.ros.namespace.GraphName;


public class SensorsService implements NodeMain {

  private ImuThread imuThread;
  private SensorListener sensorListener;
  private SensorManager sensorManager;
  private Publisher<Imu> publisher;
  protected Node node_;
  
  private class ImuThread extends Thread {
          private final SensorManager sensorManager;
          private SensorListener sensorListener;
          private Looper threadLooper;
          
          private final Sensor accelSensor;
          private final Sensor gyroSensor;
          private final Sensor quatSensor;
          
          private ImuThread(SensorManager sensorManager, SensorListener sensorListener){
                  this.sensorManager = sensorManager;
                  this.sensorListener = sensorListener;
                  this.accelSensor = this.sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                  this.gyroSensor = this.sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
                  this.quatSensor = this.sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
          }
          
            public void run() {
                Looper.prepare();
                this.threadLooper = Looper.myLooper();
                this.sensorManager.registerListener(this.sensorListener, this.accelSensor, SensorManager.SENSOR_DELAY_FASTEST);
                this.sensorManager.registerListener(this.sensorListener, this.gyroSensor, SensorManager.SENSOR_DELAY_FASTEST);
                this.sensorManager.registerListener(this.sensorListener, this.quatSensor, SensorManager.SENSOR_DELAY_FASTEST);
                Looper.loop();
            }
            
            public void shutdown(){
                this.sensorManager.unregisterListener(this.sensorListener);
                if(this.threadLooper != null){
                    this.threadLooper.quit();
                }
            }
        }
  
  private class SensorListener implements SensorEventListener {

    private Publisher<Imu> publisher;
    
    private boolean hasAccel;
    private boolean hasGyro;
    private boolean hasQuat;
    
    private long accelTime;
    private long gyroTime;
    private long quatTime;
    
    private Imu imu;

    private SensorListener(Publisher<Imu> publisher, boolean hasAccel, boolean hasGyro, boolean hasQuat) {
      this.publisher = publisher;
      this.hasAccel = hasAccel;
      this.hasGyro = hasGyro;
      this.hasQuat = hasQuat;
      this.accelTime = 0;
      this.gyroTime = 0;
      this.quatTime = 0;
      this.imu = new Imu();
    }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
                if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
                this.imu.linear_acceleration.x = event.values[0];
                this.imu.linear_acceleration.y = event.values[1];
                this.imu.linear_acceleration.z = event.values[2];
                this.imu.linear_acceleration_covariance[0] = 0.01; // TODO Make Parameter
                this.imu.linear_acceleration_covariance[4] = 0.01;
                this.imu.linear_acceleration_covariance[8] = 0.01;
                        this.accelTime = event.timestamp;
                } else if(event.sensor.getType() == Sensor.TYPE_GYROSCOPE){
                this.imu.angular_velocity.x = event.values[0];
                this.imu.angular_velocity.y = event.values[1];
                this.imu.angular_velocity.z = event.values[2];
                this.imu.angular_velocity_covariance[0] = 0.0025; // TODO Make Parameter
                this.imu.angular_velocity_covariance[4] = 0.0025;
                this.imu.angular_velocity_covariance[8] = 0.0025;
                this.gyroTime = event.timestamp;
                } else if(event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR){
                float[] quaternion = new float[4];
                SensorManager.getQuaternionFromVector(quaternion, event.values);
                    this.imu.orientation.w = quaternion[0];
                this.imu.orientation.x = quaternion[1];
                this.imu.orientation.y = quaternion[2];
                this.imu.orientation.z = quaternion[3];
                this.imu.orientation_covariance[0] = 0.001; // TODO Make Parameter
                this.imu.orientation_covariance[4] = 0.001;
                this.imu.orientation_covariance[8] = 0.001;
                this.quatTime = event.timestamp;
                }
                
                // Currently storing event times in case I filter them in the future.  Otherwise they are used to determine if all sensors have reported.
                if((this.accelTime != 0 || !this.hasAccel) && (this.gyroTime != 0 || !this.hasGyro) && (this.quatTime != 0 || !this.hasQuat)){
                        // Convert event.timestamp (nanoseconds uptime) into system time, use that as the header stamp
                        long time_delta_millis = System.currentTimeMillis() - SystemClock.uptimeMillis();
                        this.imu.header.stamp = node_.getCurrentTime();//Time.fromMillis(time_delta_millis + event.timestamp/1000000);
                        this.imu.header.frame_id = "imu"; // TODO Make parameter
                        
                        publisher.publish(this.imu);
                        
                        // Reset times
                        this.accelTime = 0;
                        this.gyroTime = 0;
                        this.quatTime = 0;
                }
        }



  }

  public SensorsService (SensorManager manager) {
          this.sensorManager = manager;
  }

@Override
public GraphName getDefaultNodeName() {
   return new GraphName("android/Sensors");
}  
@Override
public void onStart(Node node) {
  try {
        this.node_ = node;
	    this.publisher = node.newPublisher("android/imu", "sensor_msgs/Imu");
        
        // Determine if we have the various needed sensors
        boolean hasAccel = false;
        boolean hasGyro = false;
        boolean hasQuat = false;
        List<Sensor> accelList = this.sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
        if(accelList.size() > 0){
                hasAccel = true;
        }
        List<Sensor> gyroList = this.sensorManager.getSensorList(Sensor.TYPE_GYROSCOPE);
        if(gyroList.size() > 0){
                hasGyro = true;
        }
        List<Sensor> quatList = this.sensorManager.getSensorList(Sensor.TYPE_ROTATION_VECTOR);
        if(quatList.size() > 0){
                hasQuat = true;
        }
        this.sensorListener = new SensorListener(publisher, hasAccel, hasGyro, hasQuat);
        this.imuThread = new ImuThread(this.sensorManager, sensorListener);
        this.imuThread.start();
  } catch (Exception e) {
    if (node != null) {
      node.getLog().fatal(e);
    } else {
      e.printStackTrace();
    }
  }
}

@Override
public void onShutdown(Node arg0) {
        this.imuThread.shutdown();
        try {
                this.imuThread.join();
        } catch (InterruptedException e) {
                e.printStackTrace();
        }
}

@Override
public void onShutdownComplete(Node arg0) {
}
}
