package com.biospace.monitor;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import java.util.UUID;

public class WatchAutomation {
    // Standard Heart Rate Service UUID
    private static final UUID HR_SERVICE_UUID = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb");
    private static final UUID HR_CHAR_UUID = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");

    public void startScheduledFetch(Context context, String deviceAddress) {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothDevice device = adapter.getRemoteDevice(deviceAddress);

        device.connectGatt(context, true, new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    gatt.discoverServices();
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                BluetoothGattCharacteristic hrChar = gatt.getService(HR_SERVICE_UUID).getCharacteristic(HR_CHAR_UUID);
                gatt.readCharacteristic(hrChar);
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    int heartRate = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1);
                    // Pass this to the BioSpaceBrain immediately
                    System.out.println("Automated HR Sync: " + heartRate);
                }
                gatt.disconnect(); // Save watch battery by disconnecting until next interval
            }
        });
    }
}
