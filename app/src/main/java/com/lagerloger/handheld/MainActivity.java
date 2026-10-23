package com.lagerloger.handheld;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.net.URI;

public class MainActivity extends Activity {
    private static final String SERVER = "http://192.168.1.129:3000";
    private static final String PICK_HUB = SERVER + "/m/collect";
    private static final String SEARCH = SERVER + "/m/search";
    private static final String HOST = "192.168.1.129";

    private WebView webView;
    private TextView errorText;
    private Button retryButton;
    private Button pickHubButton;
    private Button searchButton;
    private String currentUrl = PICK_HUB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(10, 10, 11));
        getWindow().setNavigationBarColor(Color.rgb(10, 10, 11));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(10, 10, 11));

        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(10, 10, 11));
        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setDatabaseEnabled(true);
        webView.getSettings().setLoadWithOverviewMode(false);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setTextZoom(100);
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleNavigation(view, request.getUrl().toString());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleNavigation(view, url);
            }

            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                currentUrl = normalizeUrl(url);
                updateTabState();
                setErrorVisible(false);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                currentUrl = normalizeUrl(url);
                updateTabState();
                installMobileAppRules();
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (request.isForMainFrame()) setErrorVisible(true);
            }
        });

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER);
        content.setPadding(dp(24), dp(24), dp(24), dp(24));
        content.setBackgroundColor(Color.rgb(10, 10, 11));

        TextView title = new TextView(this);
        title.setText("LagerLoger is not reachable");
        title.setTextColor(Color.WHITE);
        title.setTextSize(20);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        content.addView(title, new LinearLayout.LayoutParams(-1, -2));

        errorText = new TextView(this);
        errorText.setText("Connect the Zebra to the warehouse Wi-Fi, then try again.\n\n" + SERVER);
        errorText.setTextColor(Color.LTGRAY);
        errorText.setTextSize(14);
        errorText.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams messageParams = new LinearLayout.LayoutParams(-1, -2);
        messageParams.topMargin = dp(12);
        content.addView(errorText, messageParams);

        retryButton = new Button(this);
        retryButton.setText("Retry");
        retryButton.setOnClickListener(v -> webView.loadUrl(currentUrl));
        LinearLayout.LayoutParams retryParams = new LinearLayout.LayoutParams(-2, -2);
        retryParams.topMargin = dp(16);
        content.addView(retryButton, retryParams);
        content.setVisibility(View.GONE);

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.addView(webView, new LinearLayout.LayoutParams(-1, 0, 1f));
        body.addView(content, new LinearLayout.LayoutParams(-1, 0, 1f));
        root.addView(body, new LinearLayout.LayoutParams(-1, 0, 1f));

        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setGravity(Gravity.CENTER);
        tabs.setPadding(dp(8), dp(6), dp(8), dp(6));
        tabs.setBackgroundColor(Color.rgb(18, 22, 30));
        pickHubButton = createTab("Pick-Hub", v -> open(PICK_HUB));
        searchButton = createTab("Search", v -> open(SEARCH));
        tabs.addView(pickHubButton, new LinearLayout.LayoutParams(0, dp(52), 1f));
        tabs.addView(searchButton, new LinearLayout.LayoutParams(0, dp(52), 1f));
        root.addView(tabs, new LinearLayout.LayoutParams(-1, dp(64)));

        setContentView(root);
        webView.loadUrl(PICK_HUB);
    }

    private Button createTab(String text, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextSize(14);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(Color.LTGRAY);
        button.setBackground(makeBackground(Color.TRANSPARENT, 14));
        button.setOnClickListener(listener);
        return button;
    }

    private GradientDrawable makeBackground(int color, int radiusDp) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(color);
        background.setCornerRadius(dp(radiusDp));
        return background;
    }

    private void open(String url) {
        currentUrl = url;
        if (webView != null) webView.loadUrl(url);
    }

    private boolean handleNavigation(WebView view, String url) {
        try {
            URI uri = URI.create(url);
            if (!"http".equalsIgnoreCase(uri.getScheme())
                    || !HOST.equalsIgnoreCase(uri.getHost())
                    || uri.getPort() != 3000) {
                return true;
            }

            String path = uri.getPath() == null ? "/" : uri.getPath();
            if ("/m".equals(path) || "/m/".equals(path)) {
                view.loadUrl(PICK_HUB);
                return true;
            }
            if (isAllowedPath(path)) return false;
        } catch (Exception ignored) {
        }
        return true;
    }

    private String normalizeUrl(String url) {
        try {
            URI uri = URI.create(url);
            if (HOST.equalsIgnoreCase(uri.getHost()) && uri.getPort() == 3000
                    && ("/m".equals(uri.getPath()) || "/m/".equals(uri.getPath()))) {
                return PICK_HUB;
            }
            if (HOST.equalsIgnoreCase(uri.getHost()) && uri.getPort() == 3000 && isAllowedPath(uri.getPath())) return url;
        } catch (Exception ignored) {
        }
        return currentUrl == null ? PICK_HUB : currentUrl;
    }

    private boolean isAllowedPath(String path) {
        return "/m/collect".equals(path)
                || "/m/search".equals(path)
                || (path != null && path.startsWith("/m/search/"));
    }

    private void installMobileAppRules() {
        String script = "(function(){"
                + "var style=document.getElementById('lagerloger-wrapper-style');"
                + "if(!style){style=document.createElement('style');style.id='lagerloger-wrapper-style';"
                + "style.textContent='nav.m-glass{display:none!important}body>div{padding-bottom:0!important}';document.head.appendChild(style);}"
                + "if(window.__lagerlogerWrapperInstalled)return;window.__lagerlogerWrapperInstalled=true;"
                + "var allowed=function(p){return p==='/m/collect'||p==='/m/search'||p.indexOf('/m/search/')===0;};"
                + "var safe=function(u){try{var x=new URL(u,location.href);"
                + "if(x.origin!=='http://192.168.1.129:3000')return null;"
                + "if(x.pathname==='/m'||x.pathname==='/m/')return '/m/collect';"
                + "return allowed(x.pathname)?x.pathname+x.search+x.hash:null;}catch(e){return null;}};"
                + "var ps=history.pushState.bind(history),rs=history.replaceState.bind(history);"
                + "history.pushState=function(s,t,u){if(u!=null){var v=safe(u);if(!v)return;u=v;}return ps(s,t,u);};"
                + "history.replaceState=function(s,t,u){if(u!=null){var v=safe(u);if(!v)return;u=v;}return rs(s,t,u);};"
                + "document.addEventListener('click',function(e){var a=e.target&&e.target.closest?e.target.closest('a'):null;"
                + "if(!a||!a.href)return;var v=safe(a.href);if(!v){e.preventDefault();e.stopImmediatePropagation();return;}"
                + "if(v==='/m/collect'&&(location.pathname==='/m'||location.pathname==='/m/')){e.preventDefault();location.assign(v);}},true);"
                + "})();";
        webView.evaluateJavascript(script, null);
    }

    private void updateTabState() {
        if (pickHubButton == null || searchButton == null) return;
        boolean isSearch = currentUrl != null && currentUrl.contains("/m/search");
        styleTab(pickHubButton, !isSearch);
        styleTab(searchButton, isSearch);
    }

    private void styleTab(Button button, boolean active) {
        button.setTextColor(active ? Color.rgb(0, 229, 153) : Color.LTGRAY);
        button.setBackground(makeBackground(active ? Color.rgb(10, 10, 11) : Color.TRANSPARENT, 14));
    }

    private void setErrorVisible(boolean visible) {
        if (webView == null || errorText == null) return;
        ViewGroup parent = (ViewGroup) webView.getParent();
        if (parent == null || parent.getChildCount() < 2) return;
        webView.setVisibility(visible ? View.GONE : View.VISIBLE);
        parent.getChildAt(1).setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onBackPressed() {
        String path = currentUrl == null ? "" : currentUrl;
        if (path.contains("/m/search/") && webView != null) {
            webView.evaluateJavascript("history.back()", null);
        } else {
            open(PICK_HUB);
        }
    }
}
