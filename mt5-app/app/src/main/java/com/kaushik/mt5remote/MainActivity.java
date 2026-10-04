package com.kaushik.mt5remote;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;

/**
 * Thin shell around the scalper's control page (mt5_control.py on your Windows PC/VPS).
 * Asks once for the server address, remembers it, and shows the page full screen.
 * Long-press anywhere to change the address.
 */
public class MainActivity extends Activity {
    private static final String PREFS = "mt5remote";
    private static final String KEY_URL = "server_url";
    private WebView web;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        web.setWebChromeClient(new WebChromeClient()); // enables the page's confirm()/alert() dialogs
        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, WebResourceRequest req, WebResourceError err) {
                if (req.isForMainFrame()) askForUrl("Couldn't reach the server (" + err.getDescription() + ").");
            }
        });
        web.setOnLongClickListener(v -> { askForUrl(null); return true; });
        setContentView(web);

        String url = prefs().getString(KEY_URL, "");
        if (url.isEmpty()) askForUrl(null); else web.loadUrl(url);
    }

    private SharedPreferences prefs() {
        return getSharedPreferences(PREFS, MODE_PRIVATE);
    }

    private void askForUrl(String problem) {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        input.setHint("http://100.101.102.103:8765");
        input.setText(prefs().getString(KEY_URL, ""));
        new AlertDialog.Builder(this)
            .setTitle("Control server address")
            .setMessage((problem == null ? "" : problem + "\n\n")
                + "The address printed by mt5_control.py on your PC (Tailscale or home Wi-Fi).")
            .setView(input)
            .setCancelable(false)
            .setPositiveButton("Connect", (d, w) -> {
                String url = input.getText().toString().trim();
                if (!url.startsWith("http")) url = "http://" + url;
                prefs().edit().putString(KEY_URL, url).apply();
                web.loadUrl(url);
            })
            .show();
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) web.goBack(); else super.onBackPressed();
    }
}
