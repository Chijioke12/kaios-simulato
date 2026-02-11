package com.example.simulator;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import java.io.*;
import java.util.Base64;

public class MainActivity extends Activity {
    private WebView webView;
    private TextView appTitle, defaultText;
    private View rootLayout;
    private Handler repeatHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);
        appTitle = findViewById(R.id.appTitle);
        rootLayout = findViewById(R.id.rootLayout);
        defaultText = findViewById(R.id.defaultText);
        Switch themeSwitch = findViewById(R.id.themeSwitch);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient());

        // Theme Switch
        themeSwitch.setOnCheckedChangeListener((b, isChecked) -> {
            if (isChecked) {
                rootLayout.setBackgroundColor(0xFFF1F5F9);
                appTitle.setTextColor(0xFF0F172A);
            } else {
                rootLayout.setBackgroundColor(0xFF0F172A);
                appTitle.setTextColor(0xFFFFFFFF);
            }
        });

        // Load URL
        findViewById(R.id.btnLoad).setOnClickListener(v -> {
            webView.loadUrl("http://" + ((EditText)findViewById(R.id.urlInput)).getText().toString());
            defaultText.setVisibility(View.GONE);
        });

        // Screenshot
        findViewById(R.id.btnSaveIcon).setOnClickListener(v -> saveBitmap());

        // Map Keys (Restoring all 12 + System Keys)
        setupKey(R.id.btnUp, "ArrowUp", KeyEvent.KEYCODE_DPAD_UP);
        setupKey(R.id.btnDown, "ArrowDown", KeyEvent.KEYCODE_DPAD_DOWN);
        setupKey(R.id.btnLeft, "ArrowLeft", KeyEvent.KEYCODE_DPAD_LEFT);
        setupKey(R.id.btnRight, "ArrowRight", KeyEvent.KEYCODE_DPAD_RIGHT);
        setupKey(R.id.btnOk, "Enter", KeyEvent.KEYCODE_ENTER);
        setupKey(R.id.btnEnd, "Backspace", KeyEvent.KEYCODE_DEL);
        
        int[] ids = {R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4, R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9, R.id.btnStar, R.id.btnHash};
        int[] codes = {7,8,9,10,11,12,13,14,15,16,17,18};
        for(int i=0; i<ids.length; i++) setupKey(ids[i], "Num", codes[i]);
    }

    private void setupKey(int id, String name, int code) {
        View v = findViewById(id);
        if (v == null) return;
        v.setOnTouchListener((view, event) -> {
            if(event.getAction() == MotionEvent.ACTION_DOWN) {
                v.setPressed(true);
                webView.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, code));
                webView.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, code));
            } else if(event.getAction() == MotionEvent.ACTION_UP) {
                v.setPressed(false);
            }
            return true;
        });
    }

    private void saveBitmap() {
        try {
            Bitmap b = Bitmap.createBitmap(webView.getWidth(), webView.getHeight(), Bitmap.Config.ARGB_8888);
            webView.draw(new Canvas(b));
            ContentValues cv = new ContentValues();
            cv.put(MediaStore.Images.Media.DISPLAY_NAME, "icon.png");
            cv.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            cv.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);
            OutputStream os = getContentResolver().openOutputStream(getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv));
            b.compress(Bitmap.CompressFormat.PNG, 100, os);
            os.close();
            Toast.makeText(this, "Saved to Pictures!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {}
    }
}
