package com.kuro.signreader;

import android.os.Environment;
import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import android.content.Intent;
import android.provider.Settings;
import android.os.Environment;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;

public class MainActivity extends Activity {
    EditText appPkg;
    Button btnGet, btnSave;
    TextView resultBase64, resultCpp;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        // Check permissions for Android 10 and above (Scoped Storage)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }

        // For Android 11 and above (MANAGE_EXTERNAL_STORAGE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                // If permission is not granted, request it
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                startActivityForResult(intent, 1);
            }
        }

        appPkg = findViewById(R.id.appPkg);
        resultBase64 = findViewById(R.id.resultBase64);
        resultCpp = findViewById(R.id.resultCpp);

        btnGet = findViewById(R.id.btnGetSign);
        btnGet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    PackageManager packageManager = getPackageManager();
                    PackageInfo packageInfo = packageManager.getPackageInfo(appPkg.getText().toString(), PackageManager.GET_SIGNING_CERTIFICATES);

                    Signature[] signatures = packageInfo.signingInfo.getApkContentsSigners();

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    DataOutputStream dos = new DataOutputStream(baos);
                    dos.writeByte(signatures.length);

                    StringBuilder sb = new StringBuilder();
                    sb.append("std::vector<std::vector<uint8_t>> apk_signatures {");
                    for (Signature value : signatures) {
                        sb.append("{");
                        dos.writeInt(value.toByteArray().length);
                        dos.write(value.toByteArray());
                        for (int j = 0; j < value.toByteArray().length; j++) {
                            sb.append(String.format("0x%02X", value.toByteArray()[j]));
                            if (j != value.toByteArray().length - 1) {
                                sb.append(",");
                            }
                        }
                        sb.append("}");
                    }
                    sb.append("};");

                    dos.close();
                    baos.close();

                    resultBase64.setText("Base64: " + Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT));
                    resultCpp.setText("C++: " + sb.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        btnSave = findViewById(R.id.btnSave);
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    String path = appPkg.getText().toString() + "_signatures.txt";
                    StringBuilder sb = new StringBuilder();
                    sb.append(resultBase64.getText().toString() + "\n");
                    sb.append(resultCpp.getText().toString() + "\n");

                    FileOutputStream fos;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        // Save to app's external directory (Scoped Storage)
                        File appDir = getExternalFilesDir(null);
                        File file = new File(appDir, path);
                        fos = new FileOutputStream(file);
                    } else {
                        // Legacy external storage (Android 9 and below)
                        fos = new FileOutputStream(new File("/sdcard", path));
                    }

                    fos.write(sb.toString().getBytes());
                    fos.close();
                    Toast.makeText(MainActivity.this, "Saved to " + path, Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            // Handle the result of the permission request
            if (Environment.isExternalStorageManager()) {
                // Permission granted, proceed with file operations
                Toast.makeText(this, "Permission granted!", Toast.LENGTH_SHORT).show();
            } else {
                // Permission denied
                Toast.makeText(this, "Permission denied!", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
