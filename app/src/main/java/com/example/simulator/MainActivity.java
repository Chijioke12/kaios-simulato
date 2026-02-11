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

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                defaultText.setVisibility(android.view.View.GONE);
                log("Loaded: " + url);
            }
        });

        findViewById(R.id.btnLoad).setOnClickListener(v -> {
            String url = urlInput.getText().toString();
            if(!url.startsWith("http")) url = "http://" + url;
            webView.loadUrl(url);
        });

        findViewById(R.id.btnUpload).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("text/html");
            startActivityForResult(intent, FILE_PICKER_CODE);
        });

        // Setup All Mappings
        map(R.id.btnSoftLeft, "SoftLeft", 112, KeyEvent.KEYCODE_F1);
        map(R.id.btnSoftRight, "SoftRight", 113, KeyEvent.KEYCODE_F2);
        map(R.id.btnUp, "ArrowUp", 38, KeyEvent.KEYCODE_DPAD_UP);
        map(R.id.btnDown, "ArrowDown", 40, KeyEvent.KEYCODE_DPAD_DOWN);
        map(R.id.btnLeft, "ArrowLeft", 37, KeyEvent.KEYCODE_DPAD_LEFT);
        map(R.id.btnRight, "ArrowRight", 39, KeyEvent.KEYCODE_DPAD_RIGHT);
        map(R.id.btnOk, "Enter", 13, KeyEvent.KEYCODE_ENTER);
        map(R.id.btnBack, "Backspace", 8, KeyEvent.KEYCODE_DEL);
        
        // Numpad Mapping
        map(R.id.btn1, "1", 49, KeyEvent.KEYCODE_1);
        map(R.id.btn2, "2", 50, KeyEvent.KEYCODE_2);
        map(R.id.btn3, "3", 51, KeyEvent.KEYCODE_3);
        map(R.id.btn0, "0", 48, KeyEvent.KEYCODE_0);
    }

    private void map(int resId, String name, int jsCode, int androidCode) {
        findViewById(resId).setOnClickListener(v -> {
            log("Key: " + name);
            String js = "window.dispatchEvent(new KeyboardEvent('keydown', {key:'" + name + "', keyCode:" + jsCode + "}));";
            webView.evaluateJavascript(js, null);
            webView.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, androidCode));
            webView.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, androidCode));
        });
    }

    private void log(String msg) {
        keyLogger.append("\n> " + msg);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILE_PICKER_CODE && resultCode == RESULT_OK) {
            try {
                InputStream is = getContentResolver().openInputStream(data.getData());
                byte[] buffer = new byte[is.available()];
                is.read(buffer);
                is.close();
                String encoded = Base64.getEncoder().encodeToString(buffer);
                webView.loadUrl("data:text/html;base64," + encoded);
            } catch (Exception e) { log("Upload Failed"); }
        }
    }
}
