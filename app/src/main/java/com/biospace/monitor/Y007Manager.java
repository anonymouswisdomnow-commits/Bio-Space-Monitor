package com.biospace.monitor;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.util.UUID;

public class Y007Manager {
    private static final String TAG = "Y007Manager";

    // Real UUIDs from nRF Connect log - com.jiuan.BPV24 protocol
    private static final UUID SERVICE_JIUAN  = UUID.fromString("636f6d2e-6a69-7561-6e2e-425056323400");
    private static final UUID CHAR_NOTIFY    = UUID.fromString("7365642e-6a69-7561-6e2e-425056323400");
    private static final UUID CHAR_WRITE     = UUID.fromString("7265632e-6a69-7561-6e2e-425056323400");
    private static final UUID CCCD           = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    // Protocol constants
    private static final int HEADER_AB  = 0xAB;
    private static final int HEADER_EA  = 0xEA;
    private static final int HEADER_FF  = 0xFF;
    private static final int TYPE_HEALTH = 0x31;
    private static final int SUBTYPE_HR       = 9;
    private static final int SUBTYPE_SPO2     = 10;
    private static final int SUBTYPE_BP       = 17;
    private static final int SUBTYPE_STEPS    = 18;
    private static final int SUBTYPE_SLEEP    = 33;
    private static final int SUBTYPE_CALORIES = 34;
    private static final int TYPE_MEASURE_BP  = 82;

    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner scanner;
    private BluetoothGatt gatt;
    private boolean scanning = false;
    private boolean connected = false;

    // Live data from watch
    private int liveSystolic = 0;
    private int liveDiastolic = 0;
    private int liveHR = 0;
    private float liveSpO2 = 0;
    private int liveSteps = 0;
    private int liveCalories = 0;
    private float liveSleep = 0;

    public interface Listener {
        void onConnected(String deviceName);
        void onDisconnected();
        void onScanStarted();
        void onReadingReceived(BiometricReading reading);
        void onDataUpdated();
    }

    private Listener listener;

    public Y007Manager(Context context) {
        this.context = context;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public void setListener(Listener l) { this.listener = l; }

    public void startScan() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) return;
        scanner = bluetoothAdapter.getBluetoothLeScanner();
        if (scanner == null) return;
        scanning = true;
        if (listener != null) listener.onScanStarted();
        scanner.startScan(scanCallback);
        // Stop scan after 15 seconds
        mainHandler.postDelayed(this::stopScan, 15000);
    }

    public void stopScan() {
        if (scanner != null && scanning) {
            scanner.stopScan(scanCallback);
            scanning = false;
        }
    }

    public void connectTo(BluetoothDevice device) {
        stopScan();
        if (gatt != null) {
            gatt.disconnect();
            gatt.close();
        }
        gatt = device.connectGatt(context, false, gattCallback);
    }

    public void connectByMac(String mac) {
        try {
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(mac);
            connectTo(device);
        } catch (Exception e) {
            Log.e(TAG, "Invalid MAC: " + mac);
        }
    }

    public void disconnect() {
        stopScan();
        if (gatt != null) {
            gatt.disconnect();
            gatt.close();
            gatt = null;
        }
        connected = false;
    }

    public boolean isConnected() { return connected; }

    // ---- Continuous monitoring ----
    private final Runnable bpRunnable = new Runnable() {
        @Override
        public void run() {
            sendCommand(new byte[]{(byte)0xAB, 0x00, 0x04, (byte)0xFF, 0x52, 0x00, 0x00, 0x52});
            mainHandler.postDelayed(this, 2 * 60 * 1000);
        }
    };

    private void sendCommand(byte[] cmd) {
        if (gatt == null || !connected) return;
        BluetoothGattService service = gatt.getService(SERVICE_JIUAN);
        if (service == null) return;
        BluetoothGattCharacteristic c = service.getCharacteristic(CHAR_WRITE);
        if (c == null) return;
        c.setValue(cmd);
        gatt.writeCharacteristic(c);
    }

    public void startContinuousMonitoring() {
        // Start real-time HR + SpO2 streaming
        sendCommand(new byte[]{(byte)0xAB, 0x00, 0x04, (byte)0xFF, 0x51, 0x00, 0x00, 0x51});
        // Auto BP every 2 minutes
        mainHandler.removeCallbacks(bpRunnable);
        mainHandler.postDelayed(bpRunnable, 5000);
    }

    public void stopContinuousMonitoring() {
        mainHandler.removeCallbacks(bpRunnable);
        sendCommand(new byte[]{(byte)0xAB, 0x00, 0x04, (byte)0xFF, 0x51, 0x01, 0x00, 0x52});
    }

    // Current reading snapshot
    public BiometricReading getCurrentReading(SpaceWeatherData space) {
        BiometricReading r = new BiometricReading();
        r.timestamp = System.currentTimeMillis();
        r.systolic = liveSystolic;
        r.diastolic = liveDiastolic;
        r.heartRate = liveHR;
        r.spo2 = liveSpO2;
        r.steps = liveSteps;
        r.calories = liveCalories;
        r.sleepHours = liveSleep;
        r.bodyTemp = 98.6f; // Will come from temp sensor if available
        if (space != null) {
            r.kpAtReading = space.kpIndex;
            r.bzAtReading = space.bzComponent;
            r.spaceRiskAtReading = space.ansRiskLevel;
        }
        return r;
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            String name = device.getName();
            // Match Y007 by name prefix or MAC
            if (name != null && (name.startsWith("S700") || name.startsWith("BP") ||
                name.startsWith("BPL") || name.equals("BP Doctor FIT"))) {
                Log.d(TAG, "Found Y007 device: " + name + " " + device.getAddress());
                connectTo(device);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e(TAG, "Scan failed: " + errorCode);
            scanning = false;
        }
    };

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt g, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                connected = true;
                String name = g.getDevice().getName();
                if (name == null) name = "Y007";
                final String dName = name;
                Log.d(TAG, "Connected to " + dName);
                g.discoverServices();
                mainHandler.post(() -> { if (listener != null) listener.onConnected(dName); });
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                connected = false;
                stopContinuousMonitoring();
                Log.d(TAG, "Disconnected");
                mainHandler.post(() -> { if (listener != null) listener.onDisconnected(); });
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt g, int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) return;

            BluetoothGattService service = g.getService(SERVICE_JIUAN);
            if (service != null) {
                enableNotify(g, service.getCharacteristic(CHAR_NOTIFY));
                Log.d(TAG, "BP3L jiuan service found, notifications enabled");
                // Start continuous monitoring after short delay
                mainHandler.postDelayed(() -> startContinuousMonitoring(), 1500);
            } else {
                Log.e(TAG, "BP3L jiuan service NOT found - check UUID");
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt g, BluetoothGattCharacteristic c) {
            parseData(c.getValue());
        }
    };

    private void enableNotify(BluetoothGatt g, BluetoothGattCharacteristic c) {
        if (c == null) return;
        g.setCharacteristicNotification(c, true);
        BluetoothGattDescriptor desc = c.getDescriptor(CCCD);
        if (desc != null) {
            desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            g.writeDescriptor(desc);
        }
    }

    private void parseData(byte[] data) {
        if (data == null || data.length < 5) return;

        int header = data[0] & 0xFF;
        if (header != HEADER_AB && header != HEADER_EA && header != HEADER_FF) return;

        int dataType = data[4] & 0xFF;
        boolean notify = false;

        if (dataType == TYPE_HEALTH && data.length >= 6) {
            int subType = data[5] & 0xFF;
            switch (subType) {
                case SUBTYPE_BP:
                    // Byte positions from DataSyncMgr$saveMeasureBP$1.java:
                    // list.get(11) = systolic, list.get(12) = diastolic
                    if (data.length >= 13) {
                        int sys = data[11] & 0xFF;
                        int dia = data[12] & 0xFF;
                        if (sys > 0 && dia > 0) {
                            liveSystolic = sys;
                            liveDiastolic = dia;
                            notify = true;
                            Log.d(TAG, "BP data: " + sys + "/" + dia);
                        }
                    }
                    break;
                case SUBTYPE_HR:
                    if (data.length >= 8) {
                        liveHR = data[7] & 0xFF;
                        Log.d(TAG, "HR data: " + liveHR);
                        notify = true;
                    }
                    break;
                case SUBTYPE_SPO2:
                    if (data.length >= 8) {
                        liveSpO2 = data[7] & 0xFF;
                        Log.d(TAG, "SpO2 data: " + liveSpO2);
                        notify = true;
                    }
                    break;
                case SUBTYPE_STEPS:
                    if (data.length >= 10) {
                        liveSteps = ((data[6] & 0xFF) << 24) | ((data[7] & 0xFF) << 16) |
                                    ((data[8] & 0xFF) << 8) | (data[9] & 0xFF);
                        Log.d(TAG, "Steps: " + liveSteps);
                        notify = true;
                    }
                    break;
                case SUBTYPE_CALORIES:
                    if (data.length >= 8) {
                        liveCalories = ((data[6] & 0xFF) << 8) | (data[7] & 0xFF);
                        notify = true;
                    }
                    break;
                case SUBTYPE_SLEEP:
                    if (data.length >= 8) {
                        liveSleep = (data[6] & 0xFF) + (data[7] & 0xFF) / 60.0f;
                        notify = true;
                    }
                    break;
            }
        } else if (dataType == TYPE_MEASURE_BP && data.length >= 13) {
            // User pressed watch button to measure BP
            int sys = data[11] & 0xFF;
            int dia = data[12] & 0xFF;
            if (sys > 0 && dia > 0) {
                liveSystolic = sys;
                liveDiastolic = dia;
                Log.d(TAG, "Measured BP: " + sys + "/" + dia);
                final BiometricReading r = getCurrentReading(null);
                mainHandler.post(() -> { if (listener != null) listener.onReadingReceived(r); });
            }
        }

        if (notify) {
            final BiometricReading autoReading = getCurrentReading(null);
            mainHandler.post(() -> {
                if (listener != null) {
                    listener.onDataUpdated();
                    listener.onReadingReceived(autoReading);
                }
            });
        }
    }
}
