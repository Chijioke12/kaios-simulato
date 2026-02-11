package com.example.simulator;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
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
    
    private static final int INITIAL_DELAY = 300; 
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
        
        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);
        webView.setWebViewClient(new WebViewClient());

        findViewById(R.id.btnLoad).setOnClickListener(v -> {
            String url = urlInput.getText().toString();
            if(!url.startsWith("http")) url = "http://" + url;
            webView.loadUrl(url);
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
            log("Screen Cleared");
        });

        findViewById(R.id.btnScreenshot).setOnClickListener(v -> saveScreenshot());

        // --- MAP KEYS ---
        setupKey(R.id.btnUp, "ArrowUp", 38);
        setupKey(R.id.btnDown, "ArrowDown", 40);
        setupKey(R.id.btnLeft, "ArrowLeft", 37);
        setupKey(R.id.btnRight, "ArrowRight", 39);
        setupKey(R.id.btnOk, "Enter", 13);
        setupKey(R.id.btnEnd, "Backspace", 8);
        setupKey(R.id.btnCall, "Call", 114);
        setupKey(R.id.btnSoftLeft, "SoftLeft", 112);
        setupKey(R.id.btnSoftRight, "SoftRight", 113);

        int[] ids = {R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4, R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9, R.id.btnStar, R.id.btnHash};
        String[] names = {"0","1","2","3","4","5","6","7","8","9","*","#"};
        int[] js = {48,49,50,51,52,53,54,55,56,57,42,35};
        for(int i=0; i<ids.length; i++) setupKey(ids[i], names[i], js[i]);
    }

    private void setupKey(int id, final String name, final int jsCode) {
        final View v = findViewById(id);
        if (v == null) return;
        
        v.setOnTouchListener(new View.OnTouchListener() {
            private Runnable action = new Runnable() {
                @Override public void run() {
                    trigger(name, jsCode);
                    repeatHandler.postDelayed(this, REPEAT_INTERVAL);
                }
            };

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setPressed(true);
                        v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                        trigger(name, jsCode); 
                        repeatHandler.postDelayed(action, INITIAL_DELAY);
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

    private void trigger(String name, int js) {
        log("Key: " + name);
        // We simulate BOTH keydown and keyup in one JS call to complete the action
        // We REMOVED dispatchKeyEvent to stop the double-firing
        String script = "(function(){" +
            "var d=new KeyboardEvent('keydown',{key:'"+name+"',keyCode:"+js+",bubbles:true});" +
            "var u=new KeyboardEvent('keyup',{key:'"+name+"',keyCode:"+js+",bubbles:true});" +
            "window.dispatchEvent(d);document.dispatchEvent(d);" +
            "window.dispatchEvent(u);document.dispatchEvent(u);" +
            "})();";
        webView.evaluateJavascript(script, null);
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
