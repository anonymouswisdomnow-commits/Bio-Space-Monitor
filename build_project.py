"""
BioSpace Monitor - Native Java Android build script.
Run this once from your repo root, then push. GitHub Actions builds the APK.
Requires: gradlew and gradle/wrapper/gradle-wrapper.jar committed to the repo.
"""
import os, shutil

def d(path): os.makedirs(path, exist_ok=True)
def w(path, content):
    d(os.path.dirname(path))
    with open(path, 'w', encoding='utf-8') as f: f.write(content)
def cp(src, dst):
    d(os.path.dirname(dst))
    shutil.copy2(src, dst)

b = 'BioSpace'

# ── Gradle wrapper ────────────────────────────────────────────
for path in [f'{b}/app/src/main/java/com/biospace/monitor/ui/dashboard',
             f'{b}/app/src/main/java/com/biospace/monitor/ui/calibrate',
             f'{b}/app/src/main/java/com/biospace/monitor/ui/log',
             f'{b}/app/src/main/java/com/biospace/monitor/ui/environment',
             f'{b}/app/src/main/java/com/biospace/monitor/ui/ai',
             f'{b}/app/src/main/java/com/biospace/monitor/ui/report',
             f'{b}/app/src/main/java/com/biospace/monitor/data',
             f'{b}/app/src/main/java/com/biospace/monitor/sensors',
             f'{b}/app/src/main/res/layout',
             f'{b}/app/src/main/res/values',
             f'{b}/app/src/main/res/color',
             f'{b}/app/src/main/res/drawable',
             f'{b}/app/src/main/res/menu',
             f'{b}/app/src/main/res/xml',
             f'{b}/gradle/wrapper']:
    d(path)

# Copy gradle files from repo root
if not os.path.exists('gradle/wrapper/gradle-wrapper.jar'):
    raise RuntimeError("gradle/wrapper/gradle-wrapper.jar missing — commit it to the repo")
if not os.path.exists('gradlew'):
    raise RuntimeError("gradlew missing — commit it to the repo")

cp('gradle/wrapper/gradle-wrapper.jar', f'{b}/gradle/wrapper/gradle-wrapper.jar')
cp('gradlew', f'{b}/gradlew')
os.chmod(f'{b}/gradlew', 0o755)

w(f'{b}/gradle/wrapper/gradle-wrapper.properties',
  'distributionBase=GRADLE_USER_HOME\n'
  'distributionPath=wrapper/dists\n'
  'distributionUrl=https\\://services.gradle.org/distributions/gradle-8.1.1-bin.zip\n'
  'zipStoreBase=GRADLE_USER_HOME\n'
  'zipStorePath=wrapper/dists\n')

# ── Root build files ──────────────────────────────────────────
w(f'{b}/build.gradle', "plugins { id 'com.android.application' version '8.1.0' apply false }\n")
w(f'{b}/settings.gradle', """pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "BioSpace"
include ':app'
""")
w(f'{b}/gradle.properties',
  'org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8\n'
  'android.useAndroidX=true\n'
  'android.enableJetifier=true\n')

# ── app/build.gradle ─────────────────────────────────────────
w(f'{b}/app/build.gradle', """plugins { id 'com.android.application' }
android {
    namespace 'com.biospace.monitor'
    compileSdk 34
    defaultConfig {
        applicationId "com.biospace.monitor"
        minSdk 26; targetSdk 34
        versionCode 1; versionName "1.0"
    }
    buildTypes { release { minifyEnabled false } }
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
}
dependencies {
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'androidx.core:core:1.12.0'
    implementation 'com.google.android.material:material:1.9.0'
    implementation 'androidx.recyclerview:recyclerview:1.3.2'
    implementation 'androidx.lifecycle:lifecycle-livedata:2.6.2'
    implementation 'androidx.lifecycle:lifecycle-viewmodel:2.6.2'
    implementation 'androidx.room:room-runtime:2.6.1'
    annotationProcessor 'androidx.room:room-compiler:2.6.1'
    implementation 'androidx.fragment:fragment:1.6.2'
}
""")

# ── Copy all Java source files ────────────────────────────────
java_src_files = [
    # data
    'app/src/main/java/com/biospace/monitor/data/BiometricReading.java',
    'app/src/main/java/com/biospace/monitor/data/EnvironmentSnapshot.java',
    'app/src/main/java/com/biospace/monitor/data/Calibration.java',
    'app/src/main/java/com/biospace/monitor/data/BiometricDao.java',
    'app/src/main/java/com/biospace/monitor/data/EnvironmentDao.java',
    'app/src/main/java/com/biospace/monitor/data/AppDatabase.java',
    'app/src/main/java/com/biospace/monitor/data/BiometricRepository.java',
    # sensors
    'app/src/main/java/com/biospace/monitor/sensors/BiometricEngine.java',
    'app/src/main/java/com/biospace/monitor/sensors/WeatherService.java',
    'app/src/main/java/com/biospace/monitor/sensors/GeminiService.java',
    # ui
    'app/src/main/java/com/biospace/monitor/BioSpaceApp.java',
    'app/src/main/java/com/biospace/monitor/MainActivity.java',
    'app/src/main/java/com/biospace/monitor/ui/dashboard/DashboardFragment.java',
    'app/src/main/java/com/biospace/monitor/ui/calibrate/CalibrateFragment.java',
    'app/src/main/java/com/biospace/monitor/ui/log/LogFragment.java',
    'app/src/main/java/com/biospace/monitor/ui/environment/EnvironmentFragment.java',
    'app/src/main/java/com/biospace/monitor/ui/ai/AiFragment.java',
    'app/src/main/java/com/biospace/monitor/ui/report/ReportFragment.java',
]
res_files = [
    'app/src/main/AndroidManifest.xml',
    'app/src/main/res/layout/activity_main.xml',
    'app/src/main/res/layout/fragment_dashboard.xml',
    'app/src/main/res/layout/fragment_log.xml',
    'app/src/main/res/layout/fragment_calibrate.xml',
    'app/src/main/res/layout/fragment_environment.xml',
    'app/src/main/res/layout/fragment_ai.xml',
    'app/src/main/res/layout/fragment_report.xml',
    'app/src/main/res/layout/item_log_entry.xml',
    'app/src/main/res/values/colors.xml',
    'app/src/main/res/values/strings.xml',
    'app/src/main/res/values/themes.xml',
    'app/src/main/res/color/nav_selector.xml',
    'app/src/main/res/menu/bottom_nav_menu.xml',
    'app/src/main/res/drawable/pulse_dot.xml',
    'app/src/main/res/xml/network_security_config.xml',
    'app/src/main/res/xml/file_provider_paths.xml',
]

all_files = java_src_files + res_files
missing = []
for f in all_files:
    src = f  # relative to repo root
    dst = os.path.join(b, f)
    if os.path.exists(src):
        cp(src, dst)
    else:
        missing.append(src)

if missing:
    print("WARNING: These source files were not found (commit them to the repo):")
    for m in missing: print(" ", m)
else:
    print(f"All {len(all_files)} source files copied.")

print("\nBioSpace Native project built successfully.")
print("Push to GitHub to trigger the Actions build.")
