package com.example.simulator;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);
        keyLogger = findViewById(R.id.keyLogger);
        logScroll = findViewById(R.id.logScroll);
        defaultText = findViewById(R.id.defaultText);
        EditText urlInput = findViewById(R.id.urlInput);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        webView.setWebViewClient(new WebViewClient());

        findViewById(R.id.btnLoad).setOnClickListener(v -> {
            String url = urlInput.getText().toString();
            if(!url.startsWith("http")) url = "http://" + url;
            webView.loadUrl(url);
            defaultText.setVisibility(View.GONE);
        });

        findViewById(R.id.btnUpload).setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("text/html");
            startActivityForResult(i, 101);
        });

        // --- MAP KEYS WITH AUTO-REPEAT ---
        setupKey(R.id.btnUp, "ArrowUp", 38, KeyEvent.KEYCODE_DPAD_UP);
        setupKey(R.id.btnDown, "ArrowDown", 40, KeyEvent.KEYCODE_DPAD_DOWN);
        setupKey(R.id.btnLeft, "ArrowLeft", 37, KeyEvent.KEYCODE_DPAD_LEFT);
        setupKey(R.id.btnRight, "ArrowRight", 39, KeyEvent.KEYCODE_DPAD_RIGHT);
        setupKey(R.id.btnOk, "Enter", 13, KeyEvent.KEYCODE_ENTER);
        setupKey(R.id.btnEnd, "Backspace", 8, KeyEvent.KEYCODE_DEL);
        setupKey(R.id.btnCall, "Call", 114, KeyEvent.KEYCODE_CALL); // Call is F3 in many JS emus
        
        setupKey(R.id.btnSoftLeft, "SoftLeft", 112, KeyEvent.KEYCODE_F1);
        setupKey(R.id.btnSoftRight, "SoftRight", 113, KeyEvent.KEYCODE_F2);

        // Numpad (Repeat usually not needed for numbers but added for consistency)
        int[] numIds = {R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4, R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9, R.id.btnStar, R.id.btnHash};
        String[] numNames = {"0","1","2","3","4","5","6","7","8","9","*","#"};
        int[] jsCodes = {48,49,50,51,52,53,54,55,56,57,42,35};
        int[] androidCodes = {7,8,9,10,11,12,13,14,15,16,17,18};

        for(int i=0; i<numIds.length; i++) {
            setupKey(numIds[i], numNames[i], jsCodes[i], androidCodes[i]);
        }
    }

    private void setupKey(int id, String name, int js, int android) {
        View v = findViewById(id);
        if (v == null) return;

        v.setOnTouchListener(new View.OnTouchListener() {
            private Runnable action = new Runnable() {
                @Override public void run() {
                    trigger(name, js, android);
                    repeatHandler.postDelayed(this, 120); // Repeat every 120ms
                }
            };

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        repeatHandler.post(action);
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        repeatHandler.removeCallbacks(action);
                        return true;
                }
                return false;
            }
        });
    }

    private void trigger(String name, int js, int android) {
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
