plugins { id("com.android.application"); kotlin("android") }

android { namespace="com.animame.editor"; compileSdk=35
    defaultConfig { applicationId="com.animame.editor"; minSdk=26; targetSdk=35; versionCode=1; versionName="0.1.0" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-ktx:1.10.0")
    implementation("com.google.zxing:core:3.5.3")
}
