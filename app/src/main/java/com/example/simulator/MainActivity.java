package com.example.simulator;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.webkit.*;
import android.widget.*;
import java.io.OutputStream;

public class MainActivity extends Activity {
    private WebView webView;
    private TextView keyLogger, appTitle;
    private ScrollView rootLayout;
    private LinearLayout phoneChassis;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);
        keyLogger = findViewById(R.id.keyLogger);
        appTitle = findViewById(R.id.appTitle);
        rootLayout = findViewById(R.id.rootLayout);
        phoneChassis = findViewById(R.id.phoneChassis);
        Switch themeSwitch = findViewById(R.id.themeSwitch);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient());

        // THEME TOGGLE LOGIC
        themeSwitch.setOnCheckedChangeListener((btn, isChecked) -> {
            if (isChecked) {
                rootLayout.setBackgroundColor(0xFFF8FAFC);
                appTitle.setTextColor(0xFF0F172A);
                themeSwitch.setTextColor(0xFF0F172A);
            } else {
                rootLayout.setBackgroundColor(0xFF0F172A);
                appTitle.setTextColor(0xFFFFFFFF);
                themeSwitch.setTextColor(0xFFFFFFFF);
            }
        });

        // GENERATE ICON (HTML/CSS ART)
        findViewById(R.id.btnGenIcon).setOnClickListener(v -> {
            String htmlIcon = "<html><body style='background:#1e1e1e;display:flex;justify-content:center;align-items:center;height:100vh;margin:0;'>" +
                "<div style='width:180px;height:180px;background:linear-gradient(145deg,#2d3748,#1a202c);border-radius:40px;display:flex;flex-direction:column;align-items:center;padding:20px;box-shadow:0 10px 20px rgba(0,0,0,0.5);'>" +
                "<div style='width:120px;height:80px;background:#000;border:2px solid #4a5568;border-radius:8px;'></div>" +
                "<div style='width:45px;height:45px;background:#16a34a;border-radius:50%;margin-top:15px;box-shadow:inset 0 2px 4px rgba(255,255,255,0.2);'></div>" +
                "</div></body></html>";
            webView.loadData(htmlIcon, "text/html", "UTF-8");
            findViewById(R.id.defaultText).setVisibility(View.GONE);
        });

        // SAVE ICON TO GALLERY
        findViewById(R.id.btnSaveIcon).setOnClickListener(v -> saveIcon());
        
        findViewById(R.id.btnLoad).setOnClickListener(v -> {
            webView.loadUrl("http://" + ((EditText)findViewById(R.id.urlInput)).getText().toString());
            findViewById(R.id.defaultText).setVisibility(View.GONE);
        });
    }

    private void saveIcon() {
        try {
            // Capture WebView as Bitmap
            Bitmap bitmap = Bitmap.createBitmap(webView.getWidth(), webView.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            webView.draw(canvas);

            // Save to MediaStore (Gallery)
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, "kaios_simulator_icon.png");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);

            Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri != null) {
                OutputStream out = getContentResolver().openOutputStream(uri);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                out.close();
                Toast.makeText(this, "Icon Saved to Pictures folder!", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error saving icon", Toast.LENGTH_SHORT).show();
        }
    }
}
