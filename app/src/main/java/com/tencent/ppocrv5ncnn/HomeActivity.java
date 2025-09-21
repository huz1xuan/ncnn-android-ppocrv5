package com.tencent.ppocrv5ncnn;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HomeActivity extends Activity {
    
    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private static final int REQUEST_STORAGE_PERMISSION = 101;
    private static final int REQUEST_TAKE_PHOTO = 200;
    private static final int REQUEST_SELECT_IMAGE = 201;
    
    private String currentPhotoPath;
    private boolean useServerModel = true; // 默认使用Server模型
    private Button btnModelSwitch;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        
        initViews();
    }
    
    private void initViews() {
        Button btnTakePhoto = findViewById(R.id.btnTakePhoto);
        Button btnSelectImage = findViewById(R.id.btnSelectImage);
        Button btnRealTimeOCR = findViewById(R.id.btnRealTimeOCR);
        btnModelSwitch = findViewById(R.id.btnModelSwitch);
        
        btnTakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkCameraPermissionAndTakePhoto();
            }
        });
        
        btnSelectImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkStoragePermissionAndSelectImage();
            }
        });
        
        btnRealTimeOCR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HomeActivity.this, MainActivity.class));
            }
        });
        
        btnModelSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleModel();
            }
        });
        
        updateModelButton();
    }
    
    private void toggleModel() {
        useServerModel = !useServerModel;
        updateModelButton();
        
        String message = useServerModel ? "已切换到Server模型（高精度）" : "已切换到Mobile模型（高速度）";
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    
    private void updateModelButton() {
        if (useServerModel) {
            btnModelSwitch.setText("Server模型");
            btnModelSwitch.setBackgroundResource(R.drawable.button_model_selector);
        } else {
            btnModelSwitch.setText("Mobile模型");
            btnModelSwitch.setBackgroundResource(R.drawable.button_secondary);
        }
    }
    
    private void checkCameraPermissionAndTakePhoto() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 
                REQUEST_CAMERA_PERMISSION);
        } else {
            takePhoto();
        }
    }
    
    private void checkStoragePermissionAndSelectImage() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 
                REQUEST_STORAGE_PERMISSION);
        } else {
            selectImage();
        }
    }
    
    private void takePhoto() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(this, "创建图片文件失败", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                    "com.tencent.ppocrv5ncnn.fileprovider",
                    photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }
    
    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_SELECT_IMAGE);
    }
    
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir("Pictures");
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        switch (requestCode) {
            case REQUEST_CAMERA_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    takePhoto();
                } else {
                    Toast.makeText(this, "需要相机权限才能拍照", Toast.LENGTH_SHORT).show();
                }
                break;
            case REQUEST_STORAGE_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    selectImage();
                } else {
                    Toast.makeText(this, "需要存储权限才能选择图片", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode == RESULT_OK) {
            String imagePath = null;
            
            switch (requestCode) {
                case REQUEST_TAKE_PHOTO:
                    imagePath = currentPhotoPath;
                    break;
                case REQUEST_SELECT_IMAGE:
                    if (data != null && data.getData() != null) {
                        Uri selectedImageUri = data.getData();
                        imagePath = ImageUtils.getPathFromUri(this, selectedImageUri);
                    }
                    break;
            }
            
            if (imagePath != null) {
                Intent intent = new Intent(this, ResultActivity.class);
                intent.putExtra("imagePath", imagePath);
                intent.putExtra("useServerModel", useServerModel); // 使用用户选择的模型
                startActivity(intent);
            }
        }
    }
}