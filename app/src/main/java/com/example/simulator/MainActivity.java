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

        // Standard WebView Settings for Simulators
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(false);

        webView.setWebViewClient(new WebViewClient());

        btnLoad.setOnClickListener(v -> {
            String url = urlInput.getText().toString();
            if(!url.startsWith("http")) url = "http://" + url;
            webView.loadUrl(url);
        });

        // --- KEY MAPPINGS ---
        
        // Softkeys (KaiOS SoftLeft/Right are usually mapped to F1/F2 or SoftLeft/Right in specialized browsers)
        findViewById(R.id.btnSoftLeft).setOnClickListener(v -> sendKey(KeyEvent.KEYCODE_SOFT_LEFT));
        findViewById(R.id.btnSoftRight).setOnClickListener(v -> sendKey(KeyEvent.KEYCODE_SOFT_RIGHT));

        // D-Pad
        findViewById(R.id.btnUp).setOnClickListener(v -> sendKey(KeyEvent.KEYCODE_DPAD_UP));
        findViewById(R.id.btnDown).setOnClickListener(v -> sendKey(KeyEvent.KEYCODE_DPAD_DOWN));
        findViewById(R.id.btnLeft).setOnClickListener(v -> sendKey(KeyEvent.KEYCODE_DPAD_LEFT));
        findViewById(R.id.btnRight).setOnClickListener(v -> sendKey(KeyEvent.KEYCODE_DPAD_RIGHT));
        findViewById(R.id.btnOk).setOnClickListener(v -> sendKey(KeyEvent.KEYCODE_ENTER));

        // Numpad
        findViewById(R.id.btn1).setOnClickListener(v -> sendKey(KeyEvent.KEYCODE_1));
        findViewById(R.id.btn2).setOnClickListener(v -> sendKey(KeyEvent.KEYCODE_2));
        findViewById(R.id.btn3).setOnClickListener(v -> sendKey(KeyEvent.KEYCODE_3));
        findViewById(R.id.btn4).setOnClickListener(v -> sendKey(KeyEvent.KEYCODE_4));
        findViewById(R.id.btn5).setOnClickListener(v -> sendKey(KeyEvent.KEYCODE_5));
        findViewById(R.id.btn6).setOnClickListener(v -> sendKey(KeyEvent.KEYCODE_6));
        findViewById(R.id.btn7).setOnClickListener(v -> sendKey(KeyEvent.KEYCODE_7));
        findViewById(R.id.btn8).setOnClickListener(v -> sendKey(KeyEvent.KEYCODE_8));
        findViewById(R.id.btn9).setOnClickListener(v -> sendKey(KeyEvent.KEYCODE_9));
        findViewById(R.id.btn0).setOnClickListener(v -> sendKey(KeyEvent.KEYCODE_0));
        findViewById(R.id.btnStar).setOnClickListener(v -> sendKey(KeyEvent.KEYCODE_STAR));
        findViewById(R.id.btnHash).setOnClickListener(v -> sendKey(KeyEvent.KEYCODE_POUND));

        // Call & End & Back
        findViewById(R.id.btnCall).setOnClickListener(v -> sendKey(KeyEvent.KEYCODE_CALL));
        findViewById(R.id.btnEnd).setOnClickListener(v -> sendKey(KeyEvent.KEYCODE_ENDCALL));
        findViewById(R.id.btnBack).setOnClickListener(v -> sendKey(KeyEvent.KEYCODE_DEL));
    }

    private void sendKey(int keyCode) {
        // We dispatch both Down and Up to simulate a full click
        webView.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyCode));
        webView.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, keyCode));
    }

    // Prevents the app from closing when "Back" is pressed, navigates WebView instead
    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}