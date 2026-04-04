import os

def d(path):
    os.makedirs(path, exist_ok=True)

def w(path, content):
    d(os.path.dirname(path))
    with open(path, 'w') as f:
        f.write(content)

b = 'BioSpace'

for path in [
    f'{b}/app/src/main/java/com/biospace/monitor',
    f'{b}/app/src/main/res/layout',
    f'{b}/app/src/main/res/values',
    f'{b}/app/src/main/res/drawable',
    f'{b}/app/src/main/assets',
    f'{b}/gradle/wrapper',
]:
    d(path)

w(f'{b}/settings.gradle', 'pluginManagement {\n    repositories {\n        google()\n        mavenCentral()\n        gradlePluginPortal()\n    }\n}\ndependencyResolutionManagement {\n    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)\n    repositories {\n        google()\n        mavenCentral()\n    }\n}\nrootProject.name = "BioSpace"\ninclude \':app\'\n')

w(f'{b}/build.gradle', 'plugins {\n    id \'com.android.application\' version \'8.1.0\' apply false\n}\n')

w(f'{b}/gradle.properties', 'org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8\nandroid.useAndroidX=true\nandroid.enableJetifier=true\n')

w(f'{b}/gradle/wrapper/gradle-wrapper.properties', 'distributionBase=GRADLE_USER_HOME\ndistributionPath=wrapper/dists\ndistributionUrl=https\\://services.gradle.org/distributions/gradle-8.0-bin.zip\nzipStoreBase=GRADLE_USER_HOME\nzipStorePath=wrapper/dists\n')

w(f'{b}/app/proguard-rules.pro', '')

w(f'{b}/app/build.gradle', '''plugins {
    id 'com.android.application'
}
android {
    namespace 'com.biospace.monitor'
    compileSdk 34
    defaultConfig {
        applicationId "com.biospace.monitor"
        minSdk 26
        targetSdk 34
        versionCode 1
        versionName "1.0"
    }
    buildTypes {
        debug { debuggable true }
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
}
dependencies {
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'androidx.core:core:1.12.0'
}
''')

w(f'{b}/app/src/main/AndroidManifest.xml', '''<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="com.biospace.monitor">
    <uses-permission android:name="android.permission.BLUETOOTH" android:maxSdkVersion="30"/>
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" android:maxSdkVersion="30"/>
    <uses-permission android:name="android.permission.BLUETOOTH_SCAN" android:usesPermissionFlags="neverForLocation"/>
    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT"/>
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
    <uses-permission android:name="android.permission.INTERNET"/>
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
    <uses-permission android:name="android.permission.WAKE_LOCK"/>
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE"/>
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC"/>
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED"/>
    <uses-permission android:name="android.permission.VIBRATE"/>
    <uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM"/>
    <uses-permission android:name="android.permission.USE_EXACT_ALARM"/>
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="28"/>
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32"/>
    <uses-feature android:name="android.hardware.bluetooth_le" android:required="true"/>
    <application android:allowBackup="true" android:icon="@drawable/ic_launcher"
        android:label="@string/app_name" android:theme="@style/Theme.BioSpace" android:usesCleartextTraffic="true">
        <activity android:name=".MainActivity" android:exported="true" android:screenOrientation="portrait">
            <intent-filter>
                <action android:name="android.intent.action.MAIN"/>
                <category android:name="android.intent.category.LAUNCHER"/>
            </intent-filter>
        </activity>
        <service android:name=".BleService" android:enabled="true" android:exported="false" android:foregroundServiceType="connectedDevice"/>
        <service android:name=".SpaceWeatherService" android:enabled="true" android:exported="false" android:foregroundServiceType="dataSync"/>
        <receiver android:name=".BootReceiver" android:enabled="true" android:exported="true">
            <intent-filter><action android:name="android.intent.action.BOOT_COMPLETED"/></intent-filter>
        </receiver>
        <receiver android:name=".NotificationActionReceiver" android:enabled="true" android:exported="false">
            <intent-filter>
                <action android:name="com.biospace.ACTION_LOG_NOW"/>
                <action android:name="com.biospace.ACTION_DISMISS"/>
            </intent-filter>
        </receiver>
    </application>
</manifest>
''')

w(f'{b}/app/src/main/res/layout/activity_main.xml', '''<?xml version="1.0" encoding="utf-8"?>
<RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent" android:layout_height="match_parent" android:background="#0A0A12">
    <WebView android:id="@+id/webView" android:layout_width="match_parent" android:layout_height="match_parent"/>
</RelativeLayout>
''')

w(f'{b}/app/src/main/res/values/strings.xml', '<?xml version="1.0" encoding="utf-8"?>\n<resources>\n    <string name="app_name">BioSpace Monitor</string>\n</resources>\n')

w(f'{b}/app/src/main/res/values/themes.xml', '<?xml version="1.0" encoding="utf-8"?>\n<resources>\n    <style name="Theme.BioSpace" parent="android:Theme.Material.NoActionBar">\n        <item name="android:windowBackground">#0A0A12</item>\n        <item name="android:statusBarColor">#0A0A12</item>\n        <item name="android:navigationBarColor">#0A0A12</item>\n    </style>\n</resources>\n')

w(f'{b}/app/src/main/res/values/colors.xml', '<?xml version="1.0" encoding="utf-8"?>\n<resources>\n    <color name="deep_space">#0A0A12</color>\n    <color name="aurora">#00FFB3</color>\n    <color name="solar">#FF6B35</color>\n    <color name="plasma">#7B2FBE</color>\n</resources>\n')

w(f'{b}/app/src/main/res/drawable/ic_launcher.xml', '''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp"
    android:viewportWidth="108" android:viewportHeight="108">
    <path android:fillColor="#0A0A12" android:pathData="M0,0h108v108h-108z"/>
    <path android:strokeColor="#00FFB3" android:strokeWidth="3" android:fillColor="#00000000"
        android:pathData="M54,18 A36,36 0 1,1 53.9,18"/>
    <path android:strokeColor="#FF6B35" android:strokeWidth="4"
        android:strokeLineCap="round" android:strokeLineJoin="round"
        android:pathData="M22,54 L34,54 L40,36 L46,72 L52,44 L58,64 L64,54 L86,54"/>
</vector>
''')

# Java files
w(f'{b}/app/src/main/java/com/biospace/monitor/MainActivity.java', '''package com.biospace.monitor;
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.core.app.ActivityCompat;
public class MainActivity extends Activity {
    private WebView webView;
    private BroadcastReceiver dataReceiver;
    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActivityCompat.requestPermissions(this, new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.POST_NOTIFICATIONS
        }, 101);
        webView = findViewById(R.id.webView);
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccessFromFileURLs(true);
        s.setAllowUniversalAccessFromFileURLs(true);
        s.setCacheMode(WebSettings.LOAD_NO_CACHE);
        webView.setWebViewClient(new WebViewClient());
        webView.addJavascriptInterface(new Bridge(), "AndroidBridge");
        webView.loadUrl("file:///android_asset/biospace.html");
        dataReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context ctx, Intent intent) {
                String json = intent.getStringExtra("json");
                if (json != null) {
                    String js = "if(window.onNativeData)window.onNativeData(" + json + ")";
                    webView.post(() -> webView.evaluateJavascript(js, null));
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.biospace.WATCH_DATA");
        filter.addAction("com.biospace.SPACE_WEATHER");
        filter.addAction("com.biospace.ALERT");
        if (Build.VERSION.SDK_INT >= 26) {
            registerReceiver(dataReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(dataReceiver, filter);
        }
        startService(new Intent(this, BleService.class));
        startService(new Intent(this, SpaceWeatherService.class));
    }
    @Override protected void onDestroy() {
        super.onDestroy();
        try { unregisterReceiver(dataReceiver); } catch (Exception ignored) {}
    }
}
''')

w(f'{b}/app/src/main/java/com/biospace/monitor/Bridge.java', 'package com.biospace.monitor;\nimport android.webkit.JavascriptInterface;\npublic class Bridge {\n    @JavascriptInterface\n    public String getVersion() { return "1.0"; }\n}\n')

w(f'{b}/app/src/main/java/com/biospace/monitor/BleService.java', '''package com.biospace.monitor;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.*;
import android.bluetooth.le.*;
import android.content.Intent;
import android.os.*;
import java.util.UUID;
@SuppressLint("MissingPermission")
public class BleService extends Service {
    private static final String CHANNEL_ID = "biospace_ble";
    private static final UUID SVC_HR      = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb");
    private static final UUID CHR_HR_MEAS = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");
    private static final UUID SVC_SPO2    = UUID.fromString("00001822-0000-1000-8000-00805f9b34fb");
    private static final UUID CHR_SPO2    = UUID.fromString("00002a5f-0000-1000-8000-00805f9b34fb");
    private static final UUID SVC_BP      = UUID.fromString("00001810-0000-1000-8000-00805f9b34fb");
    private static final UUID CHR_BP_MEAS = UUID.fromString("00002a35-0000-1000-8000-00805f9b34fb");
    private static final UUID SVC_BATT    = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb");
    private static final UUID CHR_BATT    = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");
    private static final UUID SVC_DEVINFO = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb");
    private static final UUID CHR_FW      = UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb");
    private static final UUID SVC_VENDOR   = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    private static final UUID CHR_V_NOTIFY = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");
    private static final UUID CHR_V_WRITE  = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
    private static final UUID DESC_CCC     = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private static final byte[] CMD_BP_START     = {(byte)0xAB,0x00,0x04,(byte)0xFF,(byte)0x31,0x00,0x00,(byte)0xCA};
    private static final byte[] CMD_HRV_START    = {(byte)0xAB,0x00,0x04,(byte)0xFF,(byte)0x32,0x00,0x00,(byte)0xCB};
    private static final byte[] CMD_STRESS_START = {(byte)0xAB,0x00,0x04,(byte)0xFF,(byte)0x33,0x00,0x00,(byte)0xCC};
    private static final byte[] CMD_SLEEP_GET    = {(byte)0xAB,0x00,0x04,(byte)0xFF,(byte)0x34,0x00,0x00,(byte)0xCD};
    private BluetoothAdapter btAdapter;
    private BluetoothLeScanner scanner;
    private BluetoothGatt gatt;
    private String targetMac;
    private final Handler handler = new Handler(Looper.getMainLooper());
    @Override public void onCreate() {
        super.onCreate();
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "BioSpace BLE", NotificationManager.IMPORTANCE_LOW);
        getSystemService(NotificationManager.class).createNotificationChannel(ch);
        startForeground(1, new Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("BioSpace Monitor").setContentText("BLE active")
            .setSmallIcon(android.R.drawable.ic_menu_compass).build());
    }
    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_STICKY;
        switch (intent.getAction() == null ? "" : intent.getAction()) {
            case "CONNECT":        targetMac = intent.getStringExtra("mac"); startScan(); break;
            case "DISCONNECT":     disconnect(); break;
            case "SCAN":           startScan(); break;
            case "REQUEST_BP":     writeVendor(CMD_BP_START); break;
            case "REQUEST_HRV":    writeVendor(CMD_HRV_START); break;
            case "REQUEST_STRESS": writeVendor(CMD_STRESS_START); break;
            case "REQUEST_SLEEP":  writeVendor(CMD_SLEEP_GET); break;
        }
        return START_STICKY;
    }
    private void startScan() {
        if (btAdapter == null || !btAdapter.isEnabled()) { broadcast("{\"type\":\"error\",\"msg\":\"Bluetooth disabled\"}"); return; }
        scanner = btAdapter.getBluetoothLeScanner();
        broadcast("{\"type\":\"status\",\"status\":\"scanning\"}");
        ScanSettings ss = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
        scanner.startScan(null, ss, scanCb);
        handler.postDelayed(this::stopScan, 20000);
    }
    private void stopScan() { if (scanner != null) scanner.stopScan(scanCb); }
    private final ScanCallback scanCb = new ScanCallback() {
        @Override public void onScanResult(int t, ScanResult r) {
            BluetoothDevice dev = r.getDevice();
            String name = dev.getName() != null ? dev.getName() : "";
            String mac  = dev.getAddress();
            broadcast("{\"type\":\"scan_result\",\"name\":\"" + name + "\",\"mac\":\"" + mac + "\",\"rssi\":" + r.getRssi() + "}");
            if ((targetMac != null && targetMac.equalsIgnoreCase(mac)) || name.contains("BP Doctor") || name.contains("Y007")) {
                stopScan(); gatt = dev.connectGatt(BleService.this, false, gattCb, BluetoothDevice.TRANSPORT_LE);
            }
        }
    };
    private void disconnect() {
        if (gatt != null) { gatt.disconnect(); gatt.close(); gatt = null; }
        broadcast("{\"type\":\"status\",\"status\":\"disconnected\"}");
    }
    private final BluetoothGattCallback gattCb = new BluetoothGattCallback() {
        @Override public void onConnectionStateChange(BluetoothGatt g, int st, int newSt) {
            if (newSt == BluetoothProfile.STATE_CONNECTED) {
                broadcast("{\"type\":\"status\",\"status\":\"connected\",\"ts\":" + System.currentTimeMillis() + "}");
                handler.postDelayed(() -> g.discoverServices(), 600);
            } else {
                broadcast("{\"type\":\"status\",\"status\":\"disconnected\",\"ts\":" + System.currentTimeMillis() + "}");
                handler.postDelayed(() -> { if (targetMac != null) startScan(); }, 5000);
            }
        }
        @Override public void onServicesDiscovered(BluetoothGatt g, int st) {
            if (st != BluetoothGatt.GATT_SUCCESS) return;
            handler.postDelayed(() -> enableNotify(g, SVC_HR, CHR_HR_MEAS), 300);
            handler.postDelayed(() -> enableNotify(g, SVC_SPO2, CHR_SPO2), 600);
            handler.postDelayed(() -> enableNotify(g, SVC_BP, CHR_BP_MEAS), 900);
            handler.postDelayed(() -> enableNotify(g, SVC_VENDOR, CHR_V_NOTIFY), 1200);
            handler.postDelayed(() -> readChar(g, SVC_BATT, CHR_BATT), 1500);
            handler.postDelayed(() -> readChar(g, SVC_DEVINFO, CHR_FW), 1800);
        }
        @Override public void onCharacteristicChanged(BluetoothGatt g, BluetoothGattCharacteristic c) { parseChar(c); }
        @Override public void onCharacteristicRead(BluetoothGatt g, BluetoothGattCharacteristic c, int st) { if (st == BluetoothGatt.GATT_SUCCESS) parseChar(c); }
    };
    private void enableNotify(BluetoothGatt g, UUID svc, UUID chr) {
        BluetoothGattService s = g.getService(svc); if (s == null) return;
        BluetoothGattCharacteristic c = s.getCharacteristic(chr); if (c == null) return;
        g.setCharacteristicNotification(c, true);
        BluetoothGattDescriptor d = c.getDescriptor(DESC_CCC);
        if (d != null) { d.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE); g.writeDescriptor(d); }
    }
    private void readChar(BluetoothGatt g, UUID svc, UUID chr) {
        BluetoothGattService s = g.getService(svc); if (s == null) return;
        BluetoothGattCharacteristic c = s.getCharacteristic(chr); if (c != null) g.readCharacteristic(c);
    }
    private void writeVendor(byte[] cmd) {
        if (gatt == null) return;
        BluetoothGattService s = gatt.getService(SVC_VENDOR); if (s == null) return;
        BluetoothGattCharacteristic c = s.getCharacteristic(CHR_V_WRITE); if (c == null) return;
        c.setValue(cmd); c.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        gatt.writeCharacteristic(c);
    }
    private void parseChar(BluetoothGattCharacteristic c) {
        UUID uuid = c.getUuid(); byte[] data = c.getValue();
        if (data == null || data.length == 0) return;
        String json;
        if (CHR_HR_MEAS.equals(uuid)) {
            int flags = data[0] & 0xFF;
            int hr = (flags & 0x01) == 0 ? (data[1] & 0xFF) : ((data[2]&0xFF)<<8|(data[1]&0xFF));
            int hrv = data.length > 4 ? ((data[4]&0xFF)<<8|(data[3]&0xFF)) : -1;
            json = "{\"type\":\"hr\",\"hr\":" + hr + ",\"hrv\":" + hrv + ",\"ts\":" + System.currentTimeMillis() + "}";
        } else if (CHR_BP_MEAS.equals(uuid)) {
            boolean kpa = (data[0] & 0x01) != 0;
            int sys = ((data[2]&0xFF)<<8|(data[1]&0xFF)); if (kpa) sys /= 10;
            int dia = ((data[4]&0xFF)<<8|(data[3]&0xFF)); if (kpa) dia /= 10;
            json = "{\"type\":\"bp\",\"sys\":" + sys + ",\"dia\":" + dia + ",\"ts\":" + System.currentTimeMillis() + "}";
        } else if (CHR_SPO2.equals(uuid)) {
            json = "{\"type\":\"spo2\",\"spo2\":" + (data[1]&0xFF) + ",\"ts\":" + System.currentTimeMillis() + "}";
        } else if (CHR_BATT.equals(uuid)) {
            json = "{\"type\":\"battery\",\"level\":" + (data[0]&0xFF) + ",\"ts\":" + System.currentTimeMillis() + "}";
        } else if (CHR_FW.equals(uuid)) {
            json = "{\"type\":\"firmware\",\"version\":\"" + new String(data) + "\",\"ts\":" + System.currentTimeMillis() + "}";
        } else if (CHR_V_NOTIFY.equals(uuid)) {
            json = parseVendor(data);
        } else {
            StringBuilder sb = new StringBuilder();
            for (byte bv : data) sb.append(String.format("%02X", bv));
            json = "{\"type\":\"raw\",\"hex\":\"" + sb + "\",\"ts\":" + System.currentTimeMillis() + "}";
        }
        broadcast(json);
    }
    private String parseVendor(byte[] data) {
        if (data.length < 5) return "{\"type\":\"vendor_short\"}";
        int cmd = data[4] & 0xFF;
        if (cmd == 0x31) return "{\"type\":\"bp\",\"sys\":" + (data.length>5?data[5]&0xFF:0) + ",\"dia\":" + (data.length>6?data[6]&0xFF:0) + ",\"ts\":" + System.currentTimeMillis() + "}";
        if (cmd == 0x32) return "{\"type\":\"hrv\",\"sdnn\":" + (data.length>6?((data[6]&0xFF)<<8|(data[5]&0xFF)):0) + ",\"rmssd\":" + (data.length>8?((data[8]&0xFF)<<8|(data[7]&0xFF)):0) + ",\"ts\":" + System.currentTimeMillis() + "}";
        if (cmd == 0x33) return "{\"type\":\"stress\",\"level\":" + (data.length>5?data[5]&0xFF:0) + ",\"ts\":" + System.currentTimeMillis() + "}";
        if (cmd == 0x34) return parseSleep(data);
        if (cmd == 0x40) return "{\"type\":\"activity\",\"steps\":" + (data.length>9?((data[9]&0xFF)<<24|(data[8]&0xFF)<<16|(data[7]&0xFF)<<8|(data[6]&0xFF)):0) + ",\"ts\":" + System.currentTimeMillis() + "}";
        return "{\"type\":\"vendor\",\"cmd\":" + cmd + ",\"ts\":" + System.currentTimeMillis() + "}";
    }
    private String parseSleep(byte[] data) {
        StringBuilder stages = new StringBuilder("[");
        int apneas = 0, prevSpo2 = 98;
        for (int i = 5; i < data.length - 1; i += 3) {
            int min = data[i]&0xFF, stage = data[i+1]&0xFF, spo2 = data.length>i+2?data[i+2]&0xFF:98;
            if (i > 5) stages.append(",");
            stages.append("{\"m\":").append(min).append(",\"s\":").append(stage).append(",\"spo2\":").append(spo2).append("}");
            if (stage == 0 && spo2 < 90 && prevSpo2 >= 90) apneas++;
            prevSpo2 = spo2;
        }
        return "{\"type\":\"sleep\",\"stages\":" + stages + "],\"apnea_events\":" + apneas + ",\"ts\":" + System.currentTimeMillis() + "}";
    }
    private void broadcast(String json) { Intent i = new Intent("com.biospace.WATCH_DATA"); i.putExtra("json", json); sendBroadcast(i); }
    @Override public IBinder onBind(Intent i) { return null; }
}
''')

w(f'{b}/app/src/main/java/com/biospace/monitor/SpaceWeatherService.java', '''package com.biospace.monitor;
import android.app.*;
import android.content.Intent;
import android.os.*;
import java.io.*;
import java.net.*;
import org.json.*;
public class SpaceWeatherService extends Service {
    private static final String CHANNEL_ID = "biospace_space";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private double lastKp = -1;
    @Override public void onCreate() {
        super.onCreate();
        NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "Space Weather", NotificationManager.IMPORTANCE_LOW);
        getSystemService(NotificationManager.class).createNotificationChannel(ch);
        startForeground(2, new Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("BioSpace").setContentText("Monitoring space weather")
            .setSmallIcon(android.R.drawable.ic_menu_compass).build());
    }
    @Override public int onStartCommand(Intent i, int f, int id) { poll(); return START_STICKY; }
    private void poll() {
        new Thread(() -> { try { fetchKp(); } catch (Exception ignored) {} try { fetchWind(); } catch (Exception ignored) {} }).start();
        handler.postDelayed(this::poll, 300000);
    }
    private void fetchKp() throws Exception {
        JSONArray arr = new JSONArray(get("https://services.swpc.noaa.gov/json/planetary_k_index_1m.json"));
        JSONObject o = arr.getJSONObject(arr.length()-1);
        double kp = o.optDouble("kp_index", 0);
        broadcast("com.biospace.SPACE_WEATHER", "{\"type\":\"kp\",\"kp\":" + kp + ",\"time\":\"" + o.optString("time_tag") + "\",\"ts\":" + System.currentTimeMillis() + "}");
        if (kp != lastKp) { lastKp = kp; if (kp >= 6) NotificationHelper.sendAlert(this, "SOLAR STORM", "Kp=" + kp, "kp"); else if (kp >= 4) NotificationHelper.sendAlert(this, "Space Weather Alert", "Kp=" + kp, "kp"); }
    }
    private void fetchWind() throws Exception {
        JSONArray arr = new JSONArray(get("https://services.swpc.noaa.gov/json/rtsw/rtsw_wind_1m.json"));
        JSONObject o = arr.getJSONObject(arr.length()-1);
        double speed = o.optDouble("proton_speed",0), bz = o.optDouble("bz_gsm",0), density = o.optDouble("proton_density",0);
        broadcast("com.biospace.SPACE_WEATHER", "{\"type\":\"solar_wind\",\"speed\":" + speed + ",\"bz\":" + bz + ",\"density\":" + density + ",\"orientation\":\"" + (bz<0?"South":"North") + "\",\"ts\":" + System.currentTimeMillis() + "}");
    }
    private void broadcast(String action, String json) { Intent i = new Intent(action); i.putExtra("json", json); sendBroadcast(i); }
    private String get(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(8000); c.setReadTimeout(8000);
        BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
        StringBuilder sb = new StringBuilder(); String line;
        while ((line = br.readLine()) != null) sb.append(line);
        br.close(); c.disconnect(); return sb.toString();
    }
    @Override public IBinder onBind(Intent i) { return null; }
    @Override public void onDestroy() { super.onDestroy(); handler.removeCallbacksAndMessages(null); }
}
''')

w(f'{b}/app/src/main/java/com/biospace/monitor/NotificationHelper.java', '''package com.biospace.monitor;
import android.app.*;
import android.content.*;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
public class NotificationHelper {
    private static final String CH = "biospace_alerts";
    private static int id = 1000;
    public static void sendAlert(Context ctx, String title, String body, String tag) {
        NotificationChannel ch = new NotificationChannel(CH, "BioSpace Alerts", NotificationManager.IMPORTANCE_HIGH);
        ctx.getSystemService(NotificationManager.class).createNotificationChannel(ch);
        Intent open = new Intent(ctx, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(ctx, 0, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        try { NotificationManagerCompat.from(ctx).notify(id++, new NotificationCompat.Builder(ctx, CH)
            .setContentTitle(title).setContentText(body).setSmallIcon(android.R.drawable.ic_menu_compass)
            .setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true).setContentIntent(pi).build()); } catch (SecurityException ignored) {}
    }
}
''')

w(f'{b}/app/src/main/java/com/biospace/monitor/NotificationActionReceiver.java', 'package com.biospace.monitor;\nimport android.content.*;\npublic class NotificationActionReceiver extends BroadcastReceiver {\n    @Override public void onReceive(Context ctx, Intent intent) {}\n}\n')

w(f'{b}/app/src/main/java/com/biospace/monitor/BootReceiver.java', 'package com.biospace.monitor;\nimport android.content.*;\npublic class BootReceiver extends BroadcastReceiver {\n    @Override public void onReceive(Context ctx, Intent intent) {\n        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {\n            ctx.startForegroundService(new Intent(ctx, BleService.class));\n            ctx.startForegroundService(new Intent(ctx, SpaceWeatherService.class));\n        }\n    }\n}\n')

# Copy HTML from repo
import shutil
shutil.copy('biospace.html', f'{b}/app/src/main/assets/biospace.html')

print("All files written successfully!")
