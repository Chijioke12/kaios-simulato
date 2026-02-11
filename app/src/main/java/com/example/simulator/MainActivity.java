package com.example.simulator;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.*;
import android.view.KeyEvent;
import android.view.View;
import android.widget.*;
import java.io.*;
import java.util.Base64;

public class MainActivity extends Activity {
    private WebView webView;
    private TextView keyLogger, defaultText;
    private ScrollView logScroll;
    private static final int FILE_PICKER_CODE = 101;

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
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                defaultText.setVisibility(View.GONE);
                log("URL Loaded: " + url);
            }
        });

        findViewById(R.id.btnLoad).setOnClickListener(v -> {
            String url = urlInput.getText().toString();
            if(!url.startsWith("http")) url = "http://" + url;
            webView.loadUrl(url);
        });

        findViewById(R.id.btnUpload).setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("text/html");
            startActivityForResult(i, FILE_PICKER_CODE);
        });

        // Key Mappings
        map(R.id.btnSoftLeft, "SoftLeft", 112, KeyEvent.KEYCODE_F1);
        map(R.id.btnSoftRight, "SoftRight", 113, KeyEvent.KEYCODE_F2);
        map(R.id.btnUp, "ArrowUp", 38, KeyEvent.KEYCODE_DPAD_UP);
        map(R.id.btnDown, "ArrowDown", 40, KeyEvent.KEYCODE_DPAD_DOWN);
        map(R.id.btnLeft, "ArrowLeft", 37, KeyEvent.KEYCODE_DPAD_LEFT);
        map(R.id.btnRight, "ArrowRight", 39, KeyEvent.KEYCODE_DPAD_RIGHT);
        map(R.id.btnOk, "Enter", 13, KeyEvent.KEYCODE_ENTER);
        map(R.id.btnEnd, "Backspace", 8, KeyEvent.KEYCODE_DEL);

        // Numpad 0-9, *, #
        map(R.id.btn1, "1", 49, KeyEvent.KEYCODE_1);
        map(R.id.btn2, "2", 50, KeyEvent.KEYCODE_2);
        map(R.id.btn3, "3", 51, KeyEvent.KEYCODE_3);
        map(R.id.btn4, "4", 52, KeyEvent.KEYCODE_4);
        map(R.id.btn5, "5", 53, KeyEvent.KEYCODE_5);
        map(R.id.btn6, "6", 54, KeyEvent.KEYCODE_6);
        map(R.id.btn7, "7", 55, KeyEvent.KEYCODE_7);
        map(R.id.btn8, "8", 56, KeyEvent.KEYCODE_8);
        map(R.id.btn9, "9", 57, KeyEvent.KEYCODE_9);
        map(R.id.btn0, "0", 48, KeyEvent.KEYCODE_0);
        map(R.id.btnStar, "*", 42, KeyEvent.KEYCODE_STAR);
        map(R.id.btnHash, "#", 35, KeyEvent.KEYCODE_POUND);
    }

    private void map(int id, String name, int js, int android) {
        View v = findViewById(id);
        if (v == null) return;
        v.setOnClickListener(view -> {
            log("Key: " + name);
            String script = "window.dispatchEvent(new KeyboardEvent('keydown',{key:'"+name+"',keyCode:"+js+",bubbles:true}));";
            webView.evaluateJavascript(script, null);
            webView.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, android));
            webView.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, android));
        });
    }

    private void log(String m) {
        keyLogger.append("\n> " + m);
        logScroll.post(() -> logScroll.fullScroll(View.FOCUS_DOWN));
    }

    @Override
    protected void onActivityResult(int req, int res, Intent d) {
        if (req == FILE_PICKER_CODE && res == RESULT_OK && d != null) {
            try {
                InputStream is = getContentResolver().openInputStream(d.getData());
                byte[] b = new byte[is.available()];
                is.read(b);
                is.close();
                String base64 = Base64.getEncoder().encodeToString(b);
                webView.loadUrl("data:text/html;base64," + base64);
            } catch (Exception e) { log("Upload Error"); }
        }
    }
}
