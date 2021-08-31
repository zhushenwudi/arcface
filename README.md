1.在主工程的清单文件中添加

    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.READ_PHONE_STATE" />
    <uses-permission android:name="android.permission.CAMERA" />


    <provider
        android:name="androidx.core.content.FileProvider"
        android:authorities="com.ilab.wanlicabinet.fileprovider"
        android:exported="false"
        android:grantUriPermissions="true"
        tools:replace="name,authorities,grantUriPermissions,exported">

        <meta-data
            android:name="android.support.FILE_PROVIDER_PATHS"
            android:resource="@xml/file_paths"
            tools:replace="name,resource" />
    </provider>

2.在 res/xml 中新建 file_paths.xml，并填入

    <?xml version="1.0" encoding="utf-8"?>
    <paths>
        <external-path
            name="external"
            path="/" />
        <root-path
            name="root_path"
            path="." />
    </paths>

3.混淆

    -keep class com.arcsoft.face.** {*;}
    -keep class com.ilab.arcface.** {*;}

4.继承自 ArcBaseActivity 的类需要在清单文件中添加

    <activity
        android:name=".ui.activity.xxxxActivity"
        android:hardwareAccelerated="true"
        android:screenOrientation="landscape"
        android:launchMode="singleInstance"/>
