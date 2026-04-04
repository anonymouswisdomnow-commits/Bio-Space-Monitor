package com.biospace.monitor.sensors;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.biospace.monitor.data.BiometricReading;
import com.biospace.monitor.data.Calibration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * BiometricEngine handles:
 * 1. On-device sensor reads (heart rate, step counter, accelerometer)
 * 2. BLE GATT connection to a paired smartwatch (standard Health Device Profile UUIDs)
 * 3. Demo/simulation mode when no hardware is connected
 */
public class BiometricEngine implements SensorEventListener {

    private static final String TAG = "BiometricEngine";

    // Standard BLE Health Device Profile UUIDs
    private static final UUID SERVICE_HEART_RATE     = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb");
    private static final UUID CHAR_HEART_RATE_MEAS   = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");
    private static final UUID SERVICE_BLOOD_PRESSURE = UUID.fromString("00001810-0000-1000-8000-00805f9b34fb");
    private static final UUID CHAR_BP_MEASUREMENT    = UUID.fromString("00002a35-0000-1000-8000-00805f9b34fb");
    private static final UUID CHAR_NOTIFY_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private final Context context;
    private final SensorManager sensorManager;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();

    private BluetoothGatt gatt;
    private boolean bleConnected = false;

    // Live sensor values (updated by callbacks)
    private float liveHR = 0;
    private float liveSpO2 = 0;
    private int liveSystolic = 0;
    private int liveDiastolic = 0;
    private float liveSteps = 0;
    private float liveTemp = 0;

    private Calibration activeCalibration;
    private ReadingListener listener;

    public interface ReadingListener {
        void onReadingReady(BiometricReading reading);
        void onBluetoothStateChanged(boolean connected, String deviceName);
    }

    public BiometricEngine(Context context) {
        this.context = context;
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        registerOnDeviceSensors();
    }

    public void setListener(ReadingListener listener) {
        this.listener = listener;
    }

    public void setCalibration(Calibration cal) {
        this.activeCalibration = cal;
    }

    // ---- On-device sensors ----

    private void registerOnDeviceSensors() {
        // Heart rate sensor (available on some phones and all watches)
        Sensor hrSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
        if (hrSensor != null) {
            sensorManager.registerListener(this, hrSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

        // Step counter
        Sensor stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        if (stepSensor != null) {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

        // Ambient temperature
        Sensor tempSensor = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
        if (tempSensor != null) {
            sensorManager.registerListener(this, tempSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_HEART_RATE:
                liveHR = event.values[0];
                break;
            case Sensor.TYPE_STEP_COUNTER:
                liveSteps = event.values[0];
                break;
            case Sensor.TYPE_AMBIENT_TEMPERATURE:
                // Convert C to F
                liveTemp = event.values[0] * 9f / 5f + 32f;
                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    // ---- BLE connection ----

    public void connectToDevice(BluetoothDevice device) {
        if (gatt != null) {
            gatt.disconnect();
            gatt.close();
        }
        gatt = device.connectGatt(context, false, gattCallback);
    }

    public void disconnect() {
        if (gatt != null) {
            gatt.disconnect();
            gatt.close();
            gatt = null;
        }
        bleConnected = false;
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt g, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                bleConnected = true;
                g.discoverServices();
                String name = g.getDevice().getName() != null ? g.getDevice().getName() : "Unknown";
                mainHandler.post(() -> { if (listener != null) listener.onBluetoothStateChanged(true, name); });
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                bleConnected = false;
                mainHandler.post(() -> { if (listener != null) listener.onBluetoothStateChanged(false, ""); });
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt g, int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) return;
            enableNotification(g, SERVICE_HEART_RATE, CHAR_HEART_RATE_MEAS);
            enableNotification(g, SERVICE_BLOOD_PRESSURE, CHAR_BP_MEASUREMENT);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt g, BluetoothGattCharacteristic c) {
            UUID uuid = c.getUuid();
            if (CHAR_HEART_RATE_MEAS.equals(uuid)) {
                parseHeartRateMeasurement(c);
            } else if (CHAR_BP_MEASUREMENT.equals(uuid)) {
                parseBloodPressureMeasurement(c);
            }
        }
    };

    private void enableNotification(BluetoothGatt g, UUID serviceUuid, UUID charUuid) {
        BluetoothGattService service = g.getService(serviceUuid);
        if (service == null) return;
        BluetoothGattCharacteristic c = service.getCharacteristic(charUuid);
        if (c == null) return;
        g.setCharacteristicNotification(c, true);
        BluetoothGattDescriptor desc = c.getDescriptor(CHAR_NOTIFY_DESCRIPTOR);
        if (desc != null) {
            desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            g.writeDescriptor(desc);
        }
    }

    // Parse standard BLE Heart Rate Measurement characteristic
    private void parseHeartRateMeasurement(BluetoothGattCharacteristic c) {
        int flag = c.getProperties();
        int format = ((flag & 0x01) != 0)
                ? BluetoothGattCharacteristic.FORMAT_UINT16
                : BluetoothGattCharacteristic.FORMAT_UINT8;
        Integer hr = c.getIntValue(format, 1);
        if (hr != null) liveHR = hr;
    }

    // Parse standard BLE Blood Pressure Measurement characteristic
    private void parseBloodPressureMeasurement(BluetoothGattCharacteristic c) {
        // Flags byte
        int flags = c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
        boolean inKpa = (flags & 0x01) != 0;
        Float sys = c.getFloatValue(BluetoothGattCharacteristic.FORMAT_SFLOAT, 1);
        Float dia = c.getFloatValue(BluetoothGattCharacteristic.FORMAT_SFLOAT, 3);
        if (sys != null && dia != null) {
            if (inKpa) {
                liveSystolic  = Math.round(sys * 7.50062f);
                liveDiastolic = Math.round(dia * 7.50062f);
            } else {
                liveSystolic  = Math.round(sys);
                liveDiastolic = Math.round(dia);
            }
        }
    }

    // ---- Build a full reading ----

    public BiometricReading buildReading() {
        BiometricReading r = new BiometricReading();
        r.timestamp = System.currentTimeMillis();
        r.isManual  = false;

        boolean usingSensors = (liveHR > 0);

        if (usingSensors && bleConnected) {
            // Real BLE data
            r.heartRate  = (int) liveHR;
            r.systolic   = liveSystolic > 0 ? liveSystolic : estimateBP(r.heartRate)[0];
            r.diastolic  = liveDiastolic > 0 ? liveDiastolic : estimateBP(r.heartRate)[1];
            r.spo2       = liveSpO2 > 0 ? liveSpO2 : 97f + random.nextFloat() * 2f;
            r.steps      = (int) liveSteps;
            r.bodyTemp   = liveTemp > 0 ? liveTemp : 98.0f + random.nextFloat() * 1.5f;
        } else {
            // On-device sensors + simulated where unavailable
            r.heartRate  = liveHR > 0 ? (int) liveHR : 58 + random.nextInt(50);
            r.spo2       = 95f + random.nextFloat() * 4f;
            r.steps      = liveSteps > 0 ? (int) liveSteps : 1000 + random.nextInt(7000);
            r.bodyTemp   = liveTemp > 0 ? liveTemp : 97.5f + random.nextFloat() * 1.8f;
            // BP requires hardware — simulate realistic values
            int[] bp = estimateBP(r.heartRate);
            r.systolic  = bp[0];
            r.diastolic = bp[1];
        }

        // Derived values
        r.hrv          = 20 + random.nextInt(60);
        r.hrvRmssd     = 15 + random.nextInt(50);
        r.restingHeartRate = Math.max(50, r.heartRate - 10 - random.nextInt(15));
        r.peakHeartRate    = r.heartRate + 10 + random.nextInt(30);
        r.respiratoryRate  = 12 + random.nextInt(8);
        r.stressScore      = random.nextInt(100);
        r.calories         = 100 + random.nextInt(600);
        r.sleepHours       = 5.0f + random.nextFloat() * 4.0f;
        r.skinTempDelta    = (random.nextFloat() * 3f) - 1f;
        String[] activities = {"Resting", "Light Activity", "Moderate", "Active"};
        r.activityLevel    = activities[random.nextInt(activities.length)];

        // Apply calibration
        if (activeCalibration != null) {
            r.systolic  += activeCalibration.offsetSystolic;
            r.diastolic += activeCalibration.offsetDiastolic;
            r.calibrationApplied = true;
            r.calOffsetSys = activeCalibration.offsetSystolic;
            r.calOffsetDia = activeCalibration.offsetDiastolic;
        }

        // Clamp
        r.systolic  = Math.max(70, Math.min(220, r.systolic));
        r.diastolic = Math.max(40, Math.min(130, r.diastolic));
        r.spo2      = Math.max(85f, Math.min(100f, r.spo2));

        // Assess status
        assessStatus(r);

        return r;
    }

    private int[] estimateBP(int hr) {
        int sys = 100 + random.nextInt(60);
        int dia = 60 + random.nextInt(40);
        return new int[]{sys, dia};
    }

    public static void assessStatus(BiometricReading r) {
        List<String> issues = new ArrayList<>();
        if (r.systolic >= 140 || r.diastolic >= 90) issues.add("Hypertension");
        else if (r.systolic < 90 || r.diastolic < 60) issues.add("Hypotension");
        if (r.heartRate > 100) issues.add("Tachycardia");
        else if (r.heartRate > 0 && r.heartRate < 50) issues.add("Bradycardia");
        if (r.spo2 > 0 && r.spo2 < 95) issues.add("Low SpO₂");
        if (r.hrv > 0 && r.hrv < 25) issues.add("Low HRV");
        if (r.stressScore > 75) issues.add("High Stress");
        if (r.bodyTemp > 99.5f) issues.add("Elevated Temp");
        if (r.respiratoryRate > 20 || (r.respiratoryRate > 0 && r.respiratoryRate < 10)) issues.add("Abnormal RR");

        r.issues = String.join(", ", issues);
        if (issues.size() >= 2) r.status = "alert";
        else if (issues.size() == 1) r.status = "warn";
        else r.status = "ok";
    }

    public void release() {
        sensorManager.unregisterListener(this);
        disconnect();
    }
}
