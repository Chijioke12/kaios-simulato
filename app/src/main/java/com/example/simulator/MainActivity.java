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
        s.setAllowFileAccess(true);
        
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                defaultText.setVisibility(View.GONE);
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

        // --- KEY MAPPINGS ---
        map(R.id.btnSoftLeft, "SoftLeft", 112, KeyEvent.KEYCODE_F1);
        map(R.id.btnSoftRight, "SoftRight", 113, KeyEvent.KEYCODE_F2);
        map(R.id.btnUp, "ArrowUp", 38, KeyEvent.KEYCODE_DPAD_UP);
        map(R.id.btnDown, "ArrowDown", 40, KeyEvent.KEYCODE_DPAD_DOWN);
        map(R.id.btnLeft, "ArrowLeft", 37, KeyEvent.KEYCODE_DPAD_LEFT);
        map(R.id.btnRight, "ArrowRight", 39, KeyEvent.KEYCODE_DPAD_RIGHT);
        map(R.id.btnOk, "Enter", 13, KeyEvent.KEYCODE_ENTER);
        
        // Red Button (End) mapped to Backspace
        map(R.id.btnEnd, "Backspace", 8, KeyEvent.KEYCODE_DEL);
        map(R.id.btnCall, "Call", 10, KeyEvent.KEYCODE_CALL);
        
        // Full Numpad
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

    private void map(int resId, String name, int jsCode, int androidCode) {
        View btn = findViewById(resId);
        if (btn == null) return;
        btn.setOnClickListener(v -> {
            log("Key: " + name);
            // Inject JavaScript Keyboard Events
            String js = "window.dispatchEvent(new KeyboardEvent('keydown', {key:'" + name + "', keyCode:" + jsCode + ", bubbles:true}));" +
                        "window.dispatchEvent(new KeyboardEvent('keyup', {key:'" + name + "', keyCode:" + jsCode + ", bubbles:true}));";
            webView.evaluateJavascript(js, null);
            
            // Dispatch Native Android Key Events
            webView.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, androidCode));
            webView.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, androidCode));
        });
    }

    private void log(String msg) {
        keyLogger.append("\n> " + msg);
        // Basic scroll logic for logger
        keyLogger.post(() -> {
            final int scrollAmount = keyLogger.getLayout().getLineTop(keyLogger.getLineCount()) - keyLogger.getHeight();
            if (scrollAmount > 0) keyLogger.scrollTo(0, scrollAmount);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILE_PICKER_CODE && resultCode == RESULT_OK && data != null) {
            try {
                InputStream is = getContentResolver().openInputStream(data.getData());
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] b = new byte[1024];
                int len;
                while ((len = is.read(b)) != -1) bos.write(b, 0, len);
                String encoded = Base64.getEncoder().encodeToString(bos.toByteArray());
                webView.loadUrl("data:text/html;base64," + encoded);
                is.close();
            } catch (Exception e) { log("Upload Failed"); }
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
