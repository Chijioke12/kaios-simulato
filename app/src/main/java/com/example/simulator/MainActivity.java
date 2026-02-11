package com.example.simulator;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
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
    private static final int INITIAL_DELAY = 500; // Time before repeat starts
    private static final int REPEAT_INTERVAL = 150; // Speed of repeat

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);
        keyLogger = findViewById(R.id.keyLogger);
        logScroll = findViewById(R.id.logScroll);
        defaultText = findViewById(R.id.defaultText);
        EditText urlInput = findViewById(R.id.urlInput);

        // DARK MODE PREPARATION
        webView.setBackgroundColor(0xFF1E1E1E); 
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        webView.setWebViewClient(new WebViewClient());

        // GO BUTTON
        findViewById(R.id.btnLoad).setOnClickListener(v -> {
            String url = urlInput.getText().toString();
            if(!url.startsWith("http")) url = "http://" + url;
            webView.loadUrl(url);
            defaultText.setVisibility(View.GONE);
        });

        // UPLOAD BUTTON
        findViewById(R.id.btnUpload).setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("text/html");
            startActivityForResult(i, 101);
        });

        // CLEAR BUTTON
        findViewById(R.id.btnClear).setOnClickListener(v -> {
            webView.loadUrl("about:blank");
            defaultText.setVisibility(View.VISIBLE);
            log("Screen Cleared");
        });

        // --- MAP KEYS WITH HAPTICS & SMART REPEAT ---
        setupKey(R.id.btnUp, "ArrowUp", 38, KeyEvent.KEYCODE_DPAD_UP);
        setupKey(R.id.btnDown, "ArrowDown", 40, KeyEvent.KEYCODE_DPAD_DOWN);
        setupKey(R.id.btnLeft, "ArrowLeft", 37, KeyEvent.KEYCODE_DPAD_LEFT);
        setupKey(R.id.btnRight, "ArrowRight", 39, KeyEvent.KEYCODE_DPAD_RIGHT);
        setupKey(R.id.btnOk, "Enter", 13, KeyEvent.KEYCODE_ENTER);
        setupKey(R.id.btnEnd, "Backspace", 8, KeyEvent.KEYCODE_DEL);
        setupKey(R.id.btnCall, "Call", 114, KeyEvent.KEYCODE_CALL);
        setupKey(R.id.btnSoftLeft, "SoftLeft", 112, KeyEvent.KEYCODE_F1);
        setupKey(R.id.btnSoftRight, "SoftRight", 113, KeyEvent.KEYCODE_F2);

        // Numpad IDs (Map these similarly)
        int[] numIds = {R.id.btn1, R.id.btn2, R.id.btn3}; // Add all IDs
        String[] numNames = {"1","2","3"};
        int[] js = {49,50,51};
        int[] ak = {8,9,10};
        for(int i=0; i<numIds.length; i++) setupKey(numIds[i], numNames[i], js[i], ak[i]);
    }

    private void setupKey(int id, String name, int js, int android) {
        View v = findViewById(id);
        if (v == null) return;

        v.setOnTouchListener(new View.OnTouchListener() {
            private boolean isFirstPress = true;
            private Runnable action = new Runnable() {
                @Override public void run() {
                    trigger(name, js, android, v);
                    // The first repeat happens after INITIAL_DELAY, then every REPEAT_INTERVAL
                    long nextDelay = isFirstPress ? INITIAL_DELAY : REPEAT_INTERVAL;
                    isFirstPress = false;
                    repeatHandler.postDelayed(this, nextDelay);
                }
            };

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setPressed(true); // Visual feedback
                        v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); // Vibration
                        isFirstPress = true;
                        repeatHandler.post(action);
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        v.setPressed(false);
                        repeatHandler.removeCallbacks(action);
                        return true;
                }
                return false;
            }
        });
    }

    private void trigger(String name, int js, int android, View v) {
        log("Key: " + name);
        String script = "window.dispatchEvent(new KeyboardEvent('keydown',{key:'"+name+"',keyCode:"+js+",bubbles:true}));";
        webView.evaluateJavascript(script, null);
        webView.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, android));
        webView.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, android));
    }

    private void log(String m) {
        keyLogger.append("\n> " + m);
        logScroll.post(() -> logScroll.fullScroll(View.FOCUS_DOWN));
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
            } catch (Exception e) { log("Error"); }
        }
    }
}