package com.kuro.signreader;

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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;

public class MainActivity extends Activity {
    EditText appPkg;
    Button btnGet, btnSave;
    TextView resultBase64, resultCpp;

    private static final int REQUEST_CODE_STORAGE_PERMISSION = 1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        // Request permissions based on SDK version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!isStoragePermissionGranted()) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_STORAGE_PERMISSION);
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

                    Signature[] signatures = packageInfo.signatures;

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
                    // Check if permissions are granted
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !isStoragePermissionGranted()) {
                        // Prompt user to grant storage permissions
                        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                        startActivityForResult(intent, REQUEST_CODE_STORAGE_PERMISSION);
                        return;
                    }

                    String path = appPkg.getText().toString() + "_signatures.txt";
                    StringBuilder sb = new StringBuilder();
                    sb.append(resultBase64.getText().toString() + "\n");
                    sb.append(resultCpp.getText().toString() + "\n");

                    // Saving the signature data to a file in a safe location for scoped storage
                    File file = new File(getExternalFilesDir(null), path);
                    FileOutputStream fos = new FileOutputStream(file);
                    fos.write(sb.toString().getBytes());
                    fos.close();
                    Toast.makeText(MainActivity.this, "Saved to " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    // Check if storage permission is granted
    private boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            return checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
