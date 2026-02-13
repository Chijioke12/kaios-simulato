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

        webView.setBackgroundColor(0xFF1E1E1E);
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        
        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);
        webView.setWebViewClient(new WebViewClient());

        findViewById(R.id.btnLoad).setOnClickListener(v -> {
            String url = urlInput.getText().toString().trim();
            if (url.isEmpty()) url = "localhost:5173";
            if (url.contains("localhost")) url = url.replace("localhost", "127.0.0.1");
            if (!url.startsWith("http")) url = "http://" + url;
            webView.loadUrl(url);
            defaultText.setVisibility(View.GONE);
            webView.requestFocus();
        });

        findViewById(R.id.btnClear).setOnClickListener(v -> {
            webView.loadUrl("about:blank");
            defaultText.setVisibility(View.VISIBLE);
        });

        // --- MAPPINGS ---
        
        // 1. NATIVE ONLY (Perfect for D-pad & Numpad, no double fire)
        setupKey(R.id.btnUp, "Up", KeyEvent.KEYCODE_DPAD_UP, 0, false);
        setupKey(R.id.btnDown, "Down", KeyEvent.KEYCODE_DPAD_DOWN, 0, false);
        setupKey(R.id.btnLeft, "Left", KeyEvent.KEYCODE_DPAD_LEFT, 0, false);
        setupKey(R.id.btnRight, "Right", KeyEvent.KEYCODE_DPAD_RIGHT, 0, false);
        setupKey(R.id.btnOk, "Enter", KeyEvent.KEYCODE_ENTER, 0, false);

        // 2. JS ONLY (Required for SoftKeys so the web app sees "SoftLeft" string)
        setupKey(R.id.btnSoftLeft, "SoftLeft", 0, 112, true);
        setupKey(R.id.btnSoftRight, "SoftRight", 0, 113, true);
        setupKey(R.id.btnCall, "Call", 0, 114, true);
        setupKey(R.id.btnEnd, "Backspace", KeyEvent.KEYCODE_DEL, 8, false); // Backspace works best native

        // 3. NUMPAD (Native Only)
        int[] ids = {R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4, R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9, R.id.btnStar, R.id.btnHash};
        int[] codes = {7,8,9,10,11,12,13,14,15,16,17,18};
        for(int i=0; i<ids.length; i++) setupKey(ids[i], String.valueOf(i), codes[i], 0, false);
    }

    private void setupKey(int id, String name, int nativeCode, int jsCode, boolean useJS) {
        View v = findViewById(id);
        if (v == null) return;
        v.setOnTouchListener(new View.OnTouchListener() {
            private boolean isFirst = true;
            private Runnable action = new Runnable() {
                @Override public void run() {
                    trigger(name, nativeCode, jsCode, KeyEvent.ACTION_DOWN, useJS);
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
                    isFirst = true;
                    repeatHandler.post(action);
                } else if(event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                    v.setPressed(false);
                    repeatHandler.removeCallbacks(action);
                    trigger(name, nativeCode, jsCode, KeyEvent.ACTION_UP, useJS);
                }
                return true;
            }
        });
    }

    private void trigger(String name, int ak, int js, int action, boolean useJS) {
        if (useJS) {
            String type = (action == KeyEvent.ACTION_DOWN) ? "keydown" : "keyup";
            String script = "var e = new KeyboardEvent('"+type+"', {key:'"+name+"', keyCode:"+js+", which:"+js+", bubbles:true});" +
                            "Object.defineProperty(e, 'keyCode', {get:function(){return "+js+";}});" +
                            "window.dispatchEvent(e); document.dispatchEvent(e);";
            webView.evaluateJavascript(script, null);
        } else {
            webView.dispatchKeyEvent(new KeyEvent(action, ak));
        }
        if(action == KeyEvent.ACTION_DOWN) log("Key: " + name);
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
        } catch (Exception e) {}
    }
}
