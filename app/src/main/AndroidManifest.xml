<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />

    <!-- Tik atsarginiam variantui: kai nuotraukoje nėra EXIF GPS -->
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />

    <!-- osmdroid žemėlapio kešui senesniuose Android (API < 29) -->
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
        android:maxSdkVersion="28" />

    <!--
      READ_MEDIA_IMAGES NEREIKIA. Kai naudotojas pasidalina nuotrauka,
      sistema kartu perduoda laikiną prieigą prie to konkretaus URI.
    -->

    <application
        android:allowBackup="false"
        android:label="@string/app_name"
        android:icon="@mipmap/ic_launcher"
        android:theme="@style/Theme.GreitasPranesimas">

        <activity
            android:name=".ShareActivity"
            android:exported="true"
            android:excludeFromRecents="true"
            android:launchMode="singleTask">

            <intent-filter>
                <action android:name="android.intent.action.SEND" />
                <category android:name="android.intent.category.DEFAULT" />
                <data android:mimeType="image/*" />
            </intent-filter>

            <intent-filter>
                <action android:name="android.intent.action.SEND_MULTIPLE" />
                <category android:name="android.intent.category.DEFAULT" />
                <data android:mimeType="image/*" />
            </intent-filter>
        </activity>

        <activity
            android:name=".SettingsActivity"
            android:exported="true"
            android:label="@string/app_name">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <!--
          ShareActivity sąmoningai NETURI <category android:name="android.intent.category.LAUNCHER" />
          Programėlė neturi savo ikonos meniu — ji egzistuoja tik "Bendrinti" sąraše.
          Jei nori ir įprasto paleidimo (pvz., nustatymams), pridėk atskirą
          SettingsActivity su LAUNCHER filtru.
        -->

    </application>
</manifest>
