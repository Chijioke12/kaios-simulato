package com.example.simulator;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.*;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import java.io.*;
import java.util.Base64;

public class MainActivity extends Activity {
    private WebView webView;
    private TextView keyLogger, appTitle, defaultText;
    private ScrollView logScroll, rootLayout;
    private Handler repeatHandler = new Handler();
    private static final int INITIAL_DELAY = 500; // Delay before repeat starts
    private static final int REPEAT_INTERVAL = 100; // Speed of fast navigation

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);
        keyLogger = findViewById(R.id.keyLogger);
        appTitle = findViewById(R.id.appTitle);
        defaultText = findViewById(R.id.defaultText);
        logScroll = findViewById(R.id.logScroll);
        rootLayout = findViewById(R.id.rootLayout);
        Switch themeSwitch = findViewById(R.id.themeSwitch);
        EditText urlInput = findViewById(R.id.urlInput);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        webView.setWebViewClient(new WebViewClient());

        themeSwitch.setOnCheckedChangeListener((b, isChecked) -> {
            if (isChecked) {
                rootLayout.setBackgroundColor(0xFFF1F5F9);
                appTitle.setTextColor(0xFF0F172A);
            } else {
                rootLayout.setBackgroundColor(0xFF0F172A);
                appTitle.setTextColor(0xFFFFFFFF);
            }
        });

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

        findViewById(R.id.btnClear).setOnClickListener(v -> {
            webView.loadUrl("about:blank");
            defaultText.setVisibility(View.VISIBLE);
            log("Screen Cleared");
        });

        findViewById(R.id.btnScreenshot).setOnClickListener(v -> saveScreenshot());

        // --- FAST NAVIGATION KEYS (DPAD & BACKSPACE) ---
        setupKey(R.id.btnUp, "ArrowUp", 38, KeyEvent.KEYCODE_DPAD_UP);
        setupKey(R.id.btnDown, "ArrowDown", 40, KeyEvent.KEYCODE_DPAD_DOWN);
        setupKey(R.id.btnLeft, "ArrowLeft", 37, KeyEvent.KEYCODE_DPAD_LEFT);
        setupKey(R.id.btnRight, "ArrowRight", 39, KeyEvent.KEYCODE_DPAD_RIGHT);
        setupKey(R.id.btnEnd, "Backspace", 8, KeyEvent.KEYCODE_DEL);

        // --- STANDARD KEYS ---
        setupKey(R.id.btnOk, "Enter", 13, KeyEvent.KEYCODE_ENTER);
        setupKey(R.id.btnSoftLeft, "SoftLeft", 112, KeyEvent.KEYCODE_F1);
        setupKey(R.id.btnSoftRight, "SoftRight", 113, KeyEvent.KEYCODE_F2);
        setupKey(R.id.btnCall, "Call", 114, KeyEvent.KEYCODE_CALL);

        // Numpad Loop
        int[] ids = {R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4, R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9, R.id.btnStar, R.id.btnHash};
        String[] names = {"0","1","2","3","4","5","6","7","8","9","*","#"};
        int[] akCodes = {7,8,9,10,11,12,13,14,15,16,17,18};

        for(int i=0; i<ids.length; i++) {
            setupKey(ids[i], names[i], 0, akCodes[i]);
        }
    }

    private void setupKey(int id, final String name, final int js, final int ak) {
        final View v = findViewById(id);
        if (v == null) return;

        v.setOnTouchListener(new View.OnTouchListener() {
            private boolean isFirstPress = true;
            private Runnable action = new Runnable() {
                @Override public void run() {
                    trigger(name, ak);
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

    private void trigger(String name, int ak) {
        log("Key: " + name);
        webView.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, ak));
        webView.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, ak));
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
            cv.put(MediaStore.Images.Media.DISPLAY_NAME, "kaios_sim_" + System.currentTimeMillis() + ".png");
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
            } catch (Exception e) {}
        }
    }
}
