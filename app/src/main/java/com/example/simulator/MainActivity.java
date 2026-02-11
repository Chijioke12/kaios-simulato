package com.example.simulator;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.*;
import android.view.KeyEvent;
import android.widget.*;
import java.io.*;
import java.util.Base64;

public class MainActivity extends Activity {
    private WebView webView;
    private TextView keyLogger, defaultText;
    private static final int FILE_PICKER_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);
        keyLogger = findViewById(R.id.keyLogger);
        defaultText = findViewById(R.id.defaultText);
        EditText urlInput = findViewById(R.id.urlInput);

        // WEBVIEW CONFIG
        webView.setBackgroundColor(0xFF000000); // Start Dark
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                defaultText.setVisibility(android.view.View.GONE);
                log("Page Loaded: " + url);
            }
        });

        // LOAD URL
        findViewById(R.id.btnLoad).setOnClickListener(v -> {
            String url = urlInput.getText().toString();
            if(!url.startsWith("http")) url = "http://" + url;
            webView.loadUrl(url);
        });

        // UPLOAD HTML FILE
        findViewById(R.id.btnUpload).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("text/html");
            startActivityForResult(intent, FILE_PICKER_CODE);
        });

        // --- KEY INJECTION ---
        // Mapping KaiOS Software keys (usually F1/F2 or custom events)
        map(R.id.btnSoftLeft, "SoftLeft", 112);  // F1
        map(R.id.btnSoftRight, "SoftRight", 113); // F2
        map(R.id.btnOk, "Enter", 13);
        map(R.id.btnUp, "ArrowUp", 38);
        map(R.id.btnDown, "ArrowDown", 40);
        map(R.id.btnLeft, "ArrowLeft", 37);
        map(R.id.btnRight, "ArrowRight", 39);
        // ... map numbers similarly
    }

    private void map(int resId, String name, int jsCode) {
        findViewById(resId).setOnClickListener(v -> {
            log("Key: " + name);
            // We inject a REAL JavaScript keyboard event so KaiOS apps respond
            String js = "window.dispatchEvent(new KeyboardEvent('keydown', {key:'" + name + "', keyCode:" + jsCode + "}));";
            webView.evaluateJavascript(js, null);
            
            // Also send native Android key for standard WebViews
            int androidCode = translateToAndroid(jsCode);
            webView.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, androidCode));
            webView.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, androidCode));
        });
    }

    private void log(String msg) {
        keyLogger.append("\n> " + msg);
        // Auto-scroll logic
        final int scrollAmount = keyLogger.getLayout().getLineTop(keyLogger.getLineCount()) - keyLogger.getHeight();
        if (scrollAmount > 0) keyLogger.scrollTo(0, scrollAmount);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILE_PICKER_CODE && resultCode == RESULT_OK) {
            Uri uri = data.getData();
            try {
                InputStream is = getContentResolver().openInputStream(uri);
                byte[] buffer = new byte[is.available()];
                is.read(buffer);
                is.close();
                String content = new String(buffer);
                // Load as data URL to avoid permission issues
                String encoded = Base64.getEncoder().encodeToString(content.getBytes());
                webView.loadUrl("data:text/html;base64," + encoded);
            } catch (Exception e) { log("Error loading file"); }
        }
    }

    private int translateToAndroid(int js) {
        if(js == 38) return KeyEvent.KEYCODE_DPAD_UP;
        if(js == 40) return KeyEvent.KEYCODE_DPAD_DOWN;
        if(js == 13) return KeyEvent.KEYCODE_ENTER;
        return KeyEvent.KEYCODE_UNKNOWN;
    }
}