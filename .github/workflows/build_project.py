import os
import shutil
import urllib.request
import zipfile
import stat

def d(path):
    os.makedirs(path, exist_ok=True)

def w(path, content):
    d(os.path.dirname(path))
    with open(path, 'w') as f:
        f.write(content)

b = 'BioSpace'

# Create directory structure
for path in [
    f'{b}/app/src/main/java/com/biospace/monitor',
    f'{b}/app/src/main/res/layout',
    f'{b}/app/src/main/res/values',
    f'{b}/app/src/main/res/drawable',
    f'{b}/app/src/main/assets',
    f'{b}/gradle/wrapper',
]:
    d(path)

# --- Gradle wrapper setup ---
GRADLE_VERSION = '8.1.1'
GRADLE_ZIP_URL = f'https://services.gradle.org/distributions/gradle-{GRADLE_VERSION}-bin.zip'
GRADLE_ZIP_PATH = f'/tmp/gradle-{GRADLE_VERSION}-bin.zip'
WRAPPER_JAR_DEST = f'{b}/gradle/wrapper/gradle-wrapper.jar'

print(f"Downloading Gradle {GRADLE_VERSION}...")
urllib.request.urlretrieve(GRADLE_ZIP_URL, GRADLE_ZIP_PATH)
print(f"Downloaded: {os.path.getsize(GRADLE_ZIP_PATH)} bytes")

print("Extracting gradle-wrapper.jar...")
with zipfile.ZipFile(GRADLE_ZIP_PATH, 'r') as z:
    # Find the wrapper jar inside the zip
    jar_entries = [n for n in z.namelist() if 'gradle-wrapper' in n and n.endswith('.jar')]
    print(f"Found jar entries: {jar_entries}")
    if not jar_entries:
        raise RuntimeError("Could not find gradle-wrapper.jar inside the zip!")
    jar_entry = jar_entries[0]
    with z.open(jar_entry) as src, open(WRAPPER_JAR_DEST, 'wb') as dst:
        dst.write(src.read())

jar_size = os.path.getsize(WRAPPER_JAR_DEST)
print(f"Extracted gradle-wrapper.jar: {jar_size} bytes")
if jar_size < 10000:
    raise RuntimeError(f"gradle-wrapper.jar is suspiciously small ({jar_size} bytes) - extraction failed!")

# Download the gradlew script
GRADLEW_URL = f'https://raw.githubusercontent.com/gradle/gradle/v{GRADLE_VERSION}/gradlew'
print(f"Downloading gradlew script...")
urllib.request.urlretrieve(GRADLEW_URL, f'{b}/gradlew')
os.chmod(f'{b}/gradlew', os.stat(f'{b}/gradlew').st_mode | stat.S_IEXEC | stat.S_IXGRP | stat.S_IXOTH)

# Wrapper properties
w(f'{b}/gradle/wrapper/gradle-wrapper.properties',
  f'distributionBase=GRADLE_USER_HOME\n'
  f'distributionPath=wrapper/dists\n'
  f'distributionUrl=https\\://services.gradle.org/distributions/gradle-{GRADLE_VERSION}-bin.zip\n'
  f'zipStoreBase=GRADLE_USER_HOME\n'
  f'zipStorePath=wrapper/dists\n')

# Build configs
w(f'{b}/settings.gradle',
  'pluginManagement {\n'
  '    repositories {\n'
  '        google()\n'
  '        mavenCentral()\n'
  '        gradlePluginPortal()\n'
  '    }\n'
  '}\n'
  'dependencyResolutionManagement {\n'
  '    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)\n'
  '    repositories {\n'
  '        google()\n'
  '        mavenCentral()\n'
  '    }\n'
  '}\n'
  'rootProject.name = "BioSpace"\n'
  'include ":app"\n')

w(f'{b}/build.gradle',
  'plugins {\n'
  '    id "com.android.application" version "8.1.0" apply false\n'
  '}\n')

w(f'{b}/gradle.properties',
  'org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8\n'
  'android.useAndroidX=true\n'
  'android.enableJetifier=true\n')

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
        release {
            minifyEnabled false
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
    implementation 'com.google.android.material:material:1.9.0'
}
''')

# Copy or create HTML asset
if os.path.exists('biospace.html'):
    shutil.copy('biospace.html', f'{b}/app/src/main/assets/biospace.html')
else:
    w(f'{b}/app/src/main/assets/biospace.html',
      '<html><body><h1>BioSpace Monitor</h1></body></html>')

# AndroidManifest
w(f'{b}/app/src/main/AndroidManifest.xml', '''<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.INTERNET" />
    <application
        android:label="BioSpace Monitor"
        android:theme="@style/Theme.AppCompat.Light.NoActionBar">
        <activity android:name=".MainActivity" android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
''')

# Values
w(f'{b}/app/src/main/res/values/strings.xml',
  '<?xml version="1.0" encoding="utf-8"?>\n<resources>\n    <string name="app_name">BioSpace Monitor</string>\n</resources>\n')

# MainActivity
w(f'{b}/app/src/main/java/com/biospace/monitor/MainActivity.java', '''package com.biospace.monitor;
import android.os.Bundle;
import android.webkit.WebView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WebView webView = new WebView(this);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadUrl("file:///android_asset/biospace.html");
        setContentView(webView);
    }
}
''')

print("All files written successfully!")
