package com.example.simulator;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.*;
import android.widget.*;
import java.io.*;
import java.util.Base64;

public class MainActivity extends Activity {
    private WebView webView;
    private TextView keyLogger, defaultText;
    private ScrollView logScroll;
    private Handler repeatHandler = new Handler();
    
    // Custom timings for that "real phone" feel
    private static final int INITIAL_DELAY = 400; 
    private static final int REPEAT_INTERVAL = 100;

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
        webView.setWebViewClient(new WebViewClient());

        findViewById(R.id.btnLoad).setOnClickListener(v -> {
            String url = urlInput.getText().toString();
            if(!url.startsWith("http")) url = "http://" + url;
            webView.loadUrl(url);
            defaultText.setVisibility(View.GONE);
            wakeUpWebView();
        });

        findViewById(R.id.btnUpload).setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("text/html");
            startActivityForResult(i, 101);
        });

        findViewById(R.id.btnClear).setOnClickListener(v -> {
            webView.loadUrl("about:blank");
            defaultText.setVisibility(View.VISIBLE);
            log("Screen Cleared");
        });

        findViewById(R.id.btnScreenshot).setOnClickListener(v -> saveScreenshot());

        // --- MAP ALL KEYS WITH THE NEW HYBRID LOGIC ---
        setupHybridKey(R.id.btnUp, "ArrowUp", 38, KeyEvent.KEYCODE_DPAD_UP);
        setupHybridKey(R.id.btnDown, "ArrowDown", 40, KeyEvent.KEYCODE_DPAD_DOWN);
        setupHybridKey(R.id.btnLeft, "ArrowLeft", 37, KeyEvent.KEYCODE_DPAD_LEFT);
        setupHybridKey(R.id.btnRight, "ArrowRight", 39, KeyEvent.KEYCODE_DPAD_RIGHT);
        setupHybridKey(R.id.btnOk, "Enter", 13, KeyEvent.KEYCODE_ENTER);
        setupHybridKey(R.id.btnEnd, "Backspace", 8, KeyEvent.KEYCODE_DEL);
        setupHybridKey(R.id.btnSoftLeft, "SoftLeft", 112, KeyEvent.KEYCODE_F1);
        setupHybridKey(R.id.btnSoftRight, "SoftRight", 113, KeyEvent.KEYCODE_F2);
        setupHybridKey(R.id.btnCall, "Call", 114, KeyEvent.KEYCODE_F3);

        int[] ids = {R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4, R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9, R.id.btnStar, R.id.btnHash};
        String[] names = {"0","1","2","3","4","5","6","7","8","9","*","#"};
        int[] js = {48,49,50,51,52,53,54,55,56,57,42,35};
        int[] ak = {7,8,9,10,11,12,13,14,15,16,17,18};
        for(int i=0; i<ids.length; i++) setupHybridKey(ids[i], names[i], js[i], ak[i]);
    }

    private void setupHybridKey(int id, final String name, final int jsCode, final int akCode) {
        View v = findViewById(id);
        if (v == null) return;

        v.setOnTouchListener(new View.OnTouchListener() {
            private boolean isFirstPress = true;
            private Runnable repeatRunnable = new Runnable() {
                @Override public void run() {
                    trigger(name, jsCode, akCode, KeyEvent.ACTION_DOWN);
                    long delay = isFirstPress ? INITIAL_DELAY : REPEAT_INTERVAL;
                    isFirstPress = false;
                    repeatHandler.postDelayed(this, delay);
                }
            };

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setPressed(true);
                        v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                        isFirstPress = true;
                        repeatHandler.post(repeatRunnable);
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        v.setPressed(false);
                        repeatHandler.removeCallbacks(repeatRunnable);
                        trigger(name, jsCode, akCode, KeyEvent.ACTION_UP);
                        return true;
                }
                return false;
            }
        });
    }

    private void trigger(String name, int js, int ak, int action) {
        String type = (action == KeyEvent.ACTION_DOWN) ? "keydown" : "keyup";
        
        // HYBRID INJECTION: Only run JS if NOT in a text input (Prevents double fire in Apps)
        String script = "var active = document.activeElement.tagName; " +
                        "if(active !== 'INPUT' && active !== 'TEXTAREA') { " +
                        "  var e = new KeyboardEvent('" + type + "', {key:'" + name + "', keyCode:" + js + ", which:" + js + ", bubbles:true});" +
                        "  Object.defineProperty(e, 'keyCode', {get:function(){return " + js + ";}}); " +
                        "  window.dispatchEvent(e); document.dispatchEvent(e);" +
                        "}";
        webView.evaluateJavascript(script, null);

        // Always send native event for scrolling, navigation, and typing
        webView.dispatchKeyEvent(new KeyEvent(action, ak));
        if(action == KeyEvent.ACTION_DOWN) log("Key: " + name);
    }

    private void wakeUpWebView() {
        webView.requestFocus();
        long time = SystemClock.uptimeMillis();
        webView.dispatchTouchEvent(MotionEvent.obtain(time, time, MotionEvent.ACTION_DOWN, 0, 0, 0));
        webView.dispatchTouchEvent(MotionEvent.obtain(time, time, MotionEvent.ACTION_UP, 0, 0, 0));
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
            log("Saved to Gallery!");
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
                wakeUpWebView();
            } catch (Exception e) {}
        }
    }
}
