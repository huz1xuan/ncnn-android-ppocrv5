package com.tencent.ppocrv5ncnn;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class ResultActivity extends Activity {
    
    private ImageView imageView;
    private TextView textResult;
    private Button btnCopyResult;
    private Button btnBackHome;
    
    private String imagePath;
    private boolean useServerModel;
    private PPOCRv5Ncnn ppocrv5ncnn;
    private String recognitionResult = "";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);
        
        initViews();
        getIntentData();
        setupToolbar();
        loadAndDisplayImage();
        performOCR();
    }
    
    private void initViews() {
        imageView = findViewById(R.id.imageView);
        textResult = findViewById(R.id.textResult);
        btnCopyResult = findViewById(R.id.btnCopyResult);
        btnBackHome = findViewById(R.id.btnBackHome);
        
        btnCopyResult.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyResultToClipboard();
            }
        });
        
        btnBackHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
    
    private void getIntentData() {
        Intent intent = getIntent();
        imagePath = intent.getStringExtra("imagePath");
        useServerModel = intent.getBooleanExtra("useServerModel", true);
    }
    
    private void setupToolbar() {
        // 使用普通的ActionBar而不是Toolbar
        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
            getActionBar().setTitle("识别结果");
        }
    }
    
    private void loadAndDisplayImage() {
        if (imagePath != null) {
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
            }
        }
    }
    
    private void performOCR() {
        new OCRTask().execute();
    }
    
    private class OCRTask extends AsyncTask<Void, Void, String> {
        
        @Override
        protected void onPreExecute() {
            String modelName = useServerModel ? "Server模型" : "Mobile模型";
            textResult.setText("正在使用" + modelName + "识别中...");
        }
        
        @Override
        protected String doInBackground(Void... voids) {
            try {
                // 初始化PPOCRv5Ncnn
                ppocrv5ncnn = new PPOCRv5Ncnn();
                
                // 根据用户选择使用模型
                int modelid = useServerModel ? 1 : 0; // 1=server, 0=mobile
                int sizeid = 2; // 480像素
                int cpugpu = 0; // CPU
                
                boolean loadSuccess = ppocrv5ncnn.loadModel(getAssets(), modelid, sizeid, cpugpu);
                if (!loadSuccess) {
                    return "模型加载失败";
                }
                
                // 读取图片并转换为字节数组
                Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
                if (bitmap == null) {
                    return "无法读取图片文件";
                }
                
                // 转换为RGB格式
                Bitmap rgbBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false);
                int width = rgbBitmap.getWidth();
                int height = rgbBitmap.getHeight();
                
                // 获取像素数据
                int[] pixels = new int[width * height];
                rgbBitmap.getPixels(pixels, 0, width, 0, 0, width, height);
                
                // 转换为BGR字节数组 (OpenCV格式)
                byte[] imageData = new byte[width * height * 3];
                for (int i = 0; i < pixels.length; i++) {
                    int pixel = pixels[i];
                    int r = (pixel >> 16) & 0xFF;
                    int g = (pixel >> 8) & 0xFF;
                    int b = pixel & 0xFF;
                    
                    // BGR格式
                    imageData[i * 3] = (byte) b;
                    imageData[i * 3 + 1] = (byte) g;
                    imageData[i * 3 + 2] = (byte) r;
                }
                
                // 执行OCR识别
                String result = ppocrv5ncnn.recognizeImage(imageData, width, height);
                return result != null ? result : "识别失败";
                
            } catch (Exception e) {
                e.printStackTrace();
                return "识别过程中发生错误: " + e.getMessage();
            }
        }
        
        @Override
        protected void onPostExecute(String result) {
            recognitionResult = result;
            textResult.setText(result);
            
            if (result.equals("识别失败") || result.contains("错误")) {
                Toast.makeText(ResultActivity.this, "识别失败，请重试", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void copyResultToClipboard() {
        if (!recognitionResult.isEmpty() && !recognitionResult.equals("正在识别中...")) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("OCR结果", recognitionResult);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "结果已复制到剪贴板", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "暂无可复制的内容", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ppocrv5ncnn != null) {
            // 清理资源
            ppocrv5ncnn = null;
        }
    }
}