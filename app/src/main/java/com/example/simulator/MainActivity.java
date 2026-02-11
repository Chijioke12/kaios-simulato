package com.example.simulator;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends Activity {
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);
        EditText urlInput = findViewById(R.id.urlInput);
        Button btnLoad = findViewById(R.id.btnLoad);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);

        webView.setWebViewClient(new WebViewClient());

        btnLoad.setOnClickListener(v -> {
            String url = urlInput.getText().toString();
            if(!url.startsWith("http")) url = "http://" + url;
            webView.loadUrl(url);
        });

        // Setup Buttons to simulate hardware keys
        findViewById(R.id.btnUp).setOnClickListener(v -> sendKey(KeyEvent.KEYCODE_DPAD_UP));
        findViewById(R.id.btnDown).setOnClickListener(v -> sendKey(KeyEvent.KEYCODE_DPAD_DOWN));
        findViewById(R.id.btnLeft).setOnClickListener(v -> sendKey(KeyEvent.KEYCODE_DPAD_LEFT));
        findViewById(R.id.btnRight).setOnClickListener(v -> sendKey(KeyEvent.KEYCODE_DPAD_RIGHT));
        findViewById(R.id.btnOk).setOnClickListener(v -> sendKey(KeyEvent.KEYCODE_ENTER));
        findViewById(R.id.btnBack).setOnClickListener(v -> webView.goBack());
    }

    private void sendKey(int keyCode) {
        webView.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyCode));
        webView.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, keyCode));
    }
}
