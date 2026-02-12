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
    
    // Exact timings for perfect long-press feel
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
        
        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);
        s.setJavaScriptCanOpenWindowsAutomatically(true);
        
        webView.setWebViewClient(new WebViewClient());

        findViewById(R.id.btnLoad).setOnClickListener(v -> {
            webView.loadUrl("http://" + urlInput.getText().toString());
            defaultText.setVisibility(View.GONE);
            webView.requestFocus();
        });

        findViewById(R.id.btnUpload).setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("text/html");
            startActivityForResult(i, 101);
        });

        findViewById(R.id.btnClear).setOnClickListener(v -> {
            webView.loadUrl("about:blank");
            defaultText.setVisibility(View.VISIBLE);
            log("Cleared");
        });

        findViewById(R.id.btnScreenshot).setOnClickListener(v -> saveScreenshot());

        // --- NAVIGATION: HYBRID + REPEAT ---
        // Svelte needs Native to move focus, Games need JS to move characters
        setupKey(R.id.btnUp, "ArrowUp", 38, KeyEvent.KEYCODE_DPAD_UP, true);
        setupKey(R.id.btnDown, "ArrowDown", 40, KeyEvent.KEYCODE_DPAD_DOWN, true);
        setupKey(R.id.btnLeft, "ArrowLeft", 37, KeyEvent.KEYCODE_DPAD_LEFT, true);
        setupKey(R.id.btnRight, "ArrowRight", 39, KeyEvent.KEYCODE_DPAD_RIGHT, true);
        setupKey(R.id.btnOk, "Enter", 13, KeyEvent.KEYCODE_ENTER, true);

        // --- SYSTEM: HYBRID + REPEAT ---
        setupKey(R.id.btnSoftLeft, "SoftLeft", 112, KeyEvent.KEYCODE_F1, true);
        setupKey(R.id.btnSoftRight, "SoftRight", 113, KeyEvent.KEYCODE_F2, true);
        setupKey(R.id.btnCall, "Call", 114, KeyEvent.KEYCODE_CALL, true);
        setupKey(R.id.btnEnd, "Backspace", 8, KeyEvent.KEYCODE_DEL, true);

        // --- CHARACTERS: NATIVE ONLY + REPEAT ---
        // Native-only prevents double-typing in Svelte/Pure JS
        int[] ids = {R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4, R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9, R.id.btnStar, R.id.btnHash};
        int[] ak = {7,8,9,10,11,12,13,14,15,16,17,18};
        for(int i=0; i<ids.length; i++) setupKey(ids[i], String.valueOf(i), 0, ak[i], false);
    }

    private void setupKey(int id, final String name, final int js, final int ak, final boolean useJS) {
        View v = findViewById(id);
        if (v == null) return;

        v.setOnTouchListener(new View.OnTouchListener() {
            private boolean isFirst = true;
            private Runnable repeatAction = new Runnable() {
                @Override public void run() {
                    dispatch(name, js, ak, KeyEvent.ACTION_DOWN, useJS);
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
                    repeatHandler.post(repeatAction);
                } else if(event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                    v.setPressed(false);
                    repeatHandler.removeCallbacks(repeatAction);
                    dispatch(name, js, ak, KeyEvent.ACTION_UP, useJS);
                }
                return true;
            }
        });
    }

    private void dispatch(String name, int js, int ak, int action, boolean useJS) {
        if (useJS) {
            String type = (action == KeyEvent.ACTION_DOWN) ? "keydown" : "keyup";
            // SMART SVELTE KEYBOARD BRIDGE: 
            // If focus hits an input during navigation, force a focus/click to pop keyboard.
            String script = "var e = new KeyboardEvent('"+type+"', {key:'"+name+"', keyCode:"+js+", which:"+js+", bubbles:true});" +
                            "Object.defineProperty(e, 'keyCode', {get:function(){return "+js+";}});" +
                            "window.dispatchEvent(e); document.dispatchEvent(e);" +
                            "if(document.activeElement.tagName === 'INPUT' || document.activeElement.tagName === 'TEXTAREA') {" +
                            "  document.activeElement.focus();" +
                            "}";
            webView.evaluateJavascript(script, null);
        }

        // Native signals are mandatory for Apps and Svelte Focus management
        webView.dispatchKeyEvent(new KeyEvent(action, ak));
        if(action == KeyEvent.ACTION_DOWN) log("Key: " + name);
    }

    private void log(String m) {
        keyLogger.append("\n> " + m);
        logScroll.post(() -> logScroll.fullScroll(View.FOCUS_DOWN));
    }

    private void saveScreenshot() {
        try {
            Bitmap b = Bitmap.createBitmap(webView.getWidth(), webView.getHeight(), Bitmap.Config.ARGB_8888);
            webView.draw(new Canvas(b));
            ContentValues cv = new ContentValues();
            cv.put(MediaStore.Images.Media.DISPLAY_NAME, "sim_" + System.currentTimeMillis() + ".png");
            cv.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            cv.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);
            OutputStream os = getContentResolver().openOutputStream(getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv));
            b.compress(Bitmap.CompressFormat.PNG, 100, os);
            os.close();
            log("Saved Screenshot!");
        } catch (Exception e) {}
    }

    @Override
    protected void onActivityResult(int req, int res, Intent d) {
        if (req == 101 && res == RESULT_OK && d != null) {
            try {
                InputStream is = getContentResolver().openInputStream(d.getData());
                byte[] b = new byte[is.available()];
                is.read(b);
                is.close();
                webView.loadUrl("data:text/html;base64," + Base64.getEncoder().encodeToString(b));
                defaultText.setVisibility(View.GONE);
                webView.requestFocus();
            } catch (Exception e) {}
        }
    }
}
