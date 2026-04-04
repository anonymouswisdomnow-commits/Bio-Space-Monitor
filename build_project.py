import os
import shutil

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

# Copy gradle-wrapper.jar from repo (must be committed at gradle/wrapper/gradle-wrapper.jar)
src_jar = 'gradle/wrapper/gradle-wrapper.jar'
if not os.path.exists(src_jar):
    raise RuntimeError("gradle/wrapper/gradle-wrapper.jar not found - commit it to the repo!")
shutil.copy(src_jar, f'{b}/gradle/wrapper/gradle-wrapper.jar')
print(f"Copied gradle-wrapper.jar ({os.path.getsize(f'{b}/gradle/wrapper/gradle-wrapper.jar')} bytes)")

# Copy gradlew script from repo
src_gradlew = 'gradlew'
if not os.path.exists(src_gradlew):
    raise RuntimeError("gradlew not found - commit it to the repo!")
shutil.copy(src_gradlew, f'{b}/gradlew')
os.chmod(f'{b}/gradlew', 0o755)

# Wrapper properties
w(f'{b}/gradle/wrapper/gradle-wrapper.properties',
  'distributionBase=GRADLE_USER_HOME\n'
  'distributionPath=wrapper/dists\n'
  'distributionUrl=https\\://services.gradle.org/distributions/gradle-8.1.1-bin.zip\n'
  'zipStoreBase=GRADLE_USER_HOME\n'
  'zipStorePath=wrapper/dists\n')

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
