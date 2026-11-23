package com.darslab.tv;

import android.app.Activity;
import android.app.UiModeManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

/**
 * Darslab TV: shows https://darslab.com/tv/ full screen. The page does everything (lessons list, sign-in,
 * Darslab Plus check, player); this class only gives it a window, the remote's keys and the Back button.
 */
public class MainActivity extends Activity {
    private static final String HOST = "darslab.com";
    private WebView web;
    private FrameLayout root;
    private View fullView;                              // a video shown full screen by the page
    private WebChromeClient.CustomViewCallback fullCb;

    private String startUrl() {
        String app = "android";
        if ("Amazon".equalsIgnoreCase(Build.MANUFACTURER)) app = "firetv";
        return "https://" + HOST + "/tv/?app=" + app;
    }

    private boolean isTv() {
        UiModeManager m = (UiModeManager) getSystemService(Context.UI_MODE_SERVICE);
        return m != null && m.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION;
    }

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        root = new FrameLayout(this);
        root.setBackgroundColor(0xFF0E1226);
        web = new WebView(this);
        web.setBackgroundColor(0xFF0E1226);
        root.addView(web, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(root);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);                // the 1920 px TV canvas is fitted to the screen
        s.setSupportZoom(false);
        s.setAllowFileAccess(false);
        s.setUserAgentString(s.getUserAgentString() + " DarslabTV/1.0" + (isTv() ? " TV" : ""));
        CookieManager.getInstance().setAcceptCookie(true);   // the sign-in is a cookie of darslab.com

        web.addJavascriptInterface(new Bridge(), "DarslabApp");
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                Uri u = r.getUrl();
                String h = u.getHost();
                if ("https".equals(u.getScheme()) && h != null && (h.equals(HOST) || h.equals("www." + HOST))) return false;
                if (!isTv()) { try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (Exception e) { /* no browser */ } }
                return true;                            // nothing outside darslab.com opens inside the app
            }
            @Override
            public void onReceivedError(WebView v, WebResourceRequest r, WebResourceError e) {
                if (r.isForMainFrame()) v.loadUrl("file:///android_asset/offline.html");
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onShowCustomView(View view, CustomViewCallback cb) {
                if (fullView != null) { cb.onCustomViewHidden(); return; }
                fullView = view; fullCb = cb;
                root.addView(view, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                web.setVisibility(View.GONE);
            }
            @Override
            public void onHideCustomView() { leaveFull(); }
        });

        if (saved != null) web.restoreState(saved); else web.loadUrl(startUrl());
        web.requestFocus();
    }

    private boolean leaveFull() {
        if (fullView == null) return false;
        root.removeView(fullView); fullView = null;
        web.setVisibility(View.VISIBLE);
        if (fullCb != null) { fullCb.onCustomViewHidden(); fullCb = null; }
        web.requestFocus();
        return true;
    }

    /** Back: first the full-screen video, then the page (player, subject list), and from the first screen the app closes. */
    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            if (e.getAction() == KeyEvent.ACTION_UP) {
                if (leaveFull()) return true;
                web.evaluateJavascript("(function(){try{return !!(window.DarslabTV&&DarslabTV.back())}catch(e){return false}})()",
                        v -> { if (!"true".equals(v)) finish(); });
            }
            return true;
        }
        return super.dispatchKeyEvent(e);
    }

    @Override protected void onSaveInstanceState(Bundle out) { super.onSaveInstanceState(out); web.saveState(out); }
    @Override protected void onPause() { super.onPause(); web.onPause(); CookieManager.getInstance().flush(); }
    @Override protected void onResume() { super.onResume(); web.onResume(); }
    @Override protected void onDestroy() { root.removeAllViews(); web.destroy(); super.onDestroy(); }

    /** What the pages may ask of the app. */
    private class Bridge {
        @JavascriptInterface public void retry() { runOnUiThread(() -> web.loadUrl(startUrl())); }
        @JavascriptInterface public String version() { return "1.0"; }
    }
}
