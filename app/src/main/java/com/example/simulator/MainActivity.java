package com.example.simulator;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import java.io.*;
import java.util.Base64;

public class MainActivity extends Activity {
    private WebView webView;
    private TextView keyLogger, defaultText;
    private ScrollView logScroll;
    private Handler repeatHandler = new Handler();
    
    private static final int INITIAL_DELAY = 400; 
    private static final int REPEAT_INTERVAL = 120;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);
        keyLogger = findViewById(R.id.keyLogger);
        logScroll = findViewById(R.id.logScroll);
        defaultText = findViewById(R.id.defaultText);
        EditText urlInput = findViewById(R.id.urlInput);

        // WebView Settings
        webView.setBackgroundColor(0xFF1E1E1E);
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        
        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);
        
        webView.setWebViewClient(new WebViewClient());

        // Load Logic with localhost -> 127.0.0.1 fix
        findViewById(R.id.btnLoad).setOnClickListener(v -> {
            String url = urlInput.getText().toString().trim();
            if (url.isEmpty()) url = "localhost:5173";

            if (url.contains("localhost")) {
                url = url.replace("localhost", "127.0.0.1");
            }

            if (!url.startsWith("http")) url = "http://" + url;

            log("Loading: " + url);
            webView.loadUrl(url);
            defaultText.setVisibility(View.GONE);
            webView.requestFocus();
        });

        findViewById(R.id.btnClear).setOnClickListener(v -> {
            webView.loadUrl("about:blank");
            defaultText.setVisibility(View.VISIBLE);
            log("Cleared");
        });

        findViewById(R.id.btnScreenshot).setOnClickListener(v -> saveScreenshot());

        // --- NATIVE KEY MAPPINGS ---
        setupKey(R.id.btnUp, "Up", KeyEvent.KEYCODE_DPAD_UP);
        setupKey(R.id.btnDown, "Down", KeyEvent.KEYCODE_DPAD_DOWN);
        setupKey(R.id.btnLeft, "Left", KeyEvent.KEYCODE_DPAD_LEFT);
        setupKey(R.id.btnRight, "Right", KeyEvent.KEYCODE_DPAD_RIGHT);
        setupKey(R.id.btnOk, "OK", KeyEvent.KEYCODE_ENTER);
        
        // System Keys (Standard KaiOS mapping)
        setupKey(R.id.btnSoftLeft, "SoftLeft", KeyEvent.KEYCODE_F1);
        setupKey(R.id.btnSoftRight, "SoftRight", KeyEvent.KEYCODE_F2);
        setupKey(R.id.btnCall, "Call", KeyEvent.KEYCODE_CALL);
        setupKey(R.id.btnEnd, "CLR", KeyEvent.KEYCODE_DEL);

        // Numpad
        int[] ids = {R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4, R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9, R.id.btnStar, R.id.btnHash};
        int[] codes = {
            KeyEvent.KEYCODE_0, KeyEvent.KEYCODE_1, KeyEvent.KEYCODE_2, 
            KeyEvent.KEYCODE_3, KeyEvent.KEYCODE_4, KeyEvent.KEYCODE_5, 
            KeyEvent.KEYCODE_6, KeyEvent.KEYCODE_7, KeyEvent.KEYCODE_8, 
            KeyEvent.KEYCODE_9, KeyEvent.KEYCODE_STAR, KeyEvent.KEYCODE_POUND
        };
        String[] labels = {"0","1","2","3","4","5","6","7","8","9","*","#"};
        
        for(int i=0; i<ids.length; i++) {
            setupKey(ids[i], labels[i], codes[i]);
        }
    }

    private void setupKey(int id, final String name, final int keyCode) {
        View v = findViewById(id);
        if (v == null) return;
        v.setOnTouchListener(new View.OnTouchListener() {
            private boolean isFirst = true;
            private Runnable repeatAction = new Runnable() {
                @Override public void run() {
                    webView.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyCode));
                    long delay = isFirst ? INITIAL_DELAY : REPEAT_INTERVAL;
                    isFirst = false;
                    repeatHandler.postDelayed(this, delay);
                }
            };
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    v.setPressed(true);
                    v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                    log("Press: " + name);
                    isFirst = true;
                    repeatHandler.post(repeatAction);
                } else if(event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                    v.setPressed(false);
                    repeatHandler.removeCallbacks(repeatAction);
                    webView.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, keyCode));
                }
                return true;
            }
        });
    }

    private void log(String m) {
        runOnUiThread(() -> {
            keyLogger.append("\n> " + m);
            logScroll.post(() -> logScroll.fullScroll(View.FOCUS_DOWN));
        });
    }

    private void saveScreenshot() {
        try {
            Bitmap b = Bitmap.createBitmap(webView.getWidth(), webView.getHeight(), Bitmap.Config.ARGB_8888);
            webView.draw(new Canvas(b));
            ContentValues cv = new ContentValues();
            cv.put(MediaStore.Images.Media.DISPLAY_NAME, "sim_" + System.currentTimeMillis() + ".png");
            cv.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            cv.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);
            Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv);
            OutputStream os = getContentResolver().openOutputStream(uri);
            b.compress(Bitmap.CompressFormat.PNG, 100, os);
            os.close();
            log("Saved Screenshot!");
        } catch (Exception e) { log("Capture Failed"); }
    }
}
