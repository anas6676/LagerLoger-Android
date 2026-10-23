package com.lagerloger.handheld;

import android.app.Activity;
import android.content.Intent;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.net.URI;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private static final String SCAN_ACTION = "com.lagerloger.handheld.SCAN";
    private static final String SCAN_DATA_EXTRA = "com.symbol.datawedge.data_string";
    private static final String SEARCH_SCAN_INPUT = "input[placeholder='READY TO SCAN...']";
    private static final String CRQ_SCAN_INPUT = "input[placeholder*='Zebra Laser Ready'],input[placeholder*='Typing mode active']";
    private static final String SERVER = "http://192.168.1.129:3000";
    private static final String PICK_HUB = SERVER + "/m/collect";
    private static final String SEARCH = SERVER + "/m/search";
    private static final String CRQ_SCANNER = SERVER + "/m/crq-scanner";
    private static final String GOODS_RECEIPT = SERVER + "/m/receipt";
    private static final String HOST = "192.168.1.129";

    private WebView webView;
    private TextView errorText;
    private Button retryButton;
    private Button pickHubButton;
    private Button searchButton;
    private Button crqScannerButton;
    private Button receiptButton;
    private String currentUrl = PICK_HUB;
    private String pendingScan;

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
                deliverPendingScan();
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

        LinearLayout brandBar = new LinearLayout(this);
        brandBar.setOrientation(LinearLayout.HORIZONTAL);
        brandBar.setGravity(Gravity.CENTER_VERTICAL);
        brandBar.setPadding(dp(12), dp(4), dp(12), dp(4));
        brandBar.setBackgroundColor(Color.rgb(15, 29, 23));

        ImageView brandLogo = new ImageView(this);
        brandLogo.setImageResource(com.lagerloger.handheld.R.drawable.crate_captain);
        brandLogo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        brandBar.addView(brandLogo, new LinearLayout.LayoutParams(dp(48), dp(48)));

        LinearLayout brandCopy = new LinearLayout(this);
        brandCopy.setOrientation(LinearLayout.VERTICAL);
        brandCopy.setPadding(dp(10), 0, 0, 0);
        TextView brandName = new TextView(this);
        brandName.setText("LagerLoger");
        brandName.setTextColor(Color.rgb(248, 244, 230));
        brandName.setTextSize(16);
        brandName.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        brandCopy.addView(brandName, new LinearLayout.LayoutParams(-2, -2));
        TextView brandCompany = new TextView(this);
        brandCompany.setText("INT  ·  WAREHOUSE");
        brandCompany.setTextColor(Color.rgb(170, 205, 181));
        brandCompany.setTextSize(10);
        brandCompany.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        brandCopy.addView(brandCompany, new LinearLayout.LayoutParams(-2, -2));
        brandBar.addView(brandCopy, new LinearLayout.LayoutParams(-2, -2));

        root.addView(brandBar, new LinearLayout.LayoutParams(-1, dp(56)));
        root.addView(body, new LinearLayout.LayoutParams(-1, 0, 1f));

        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setGravity(Gravity.CENTER);
        tabs.setPadding(dp(8), dp(6), dp(8), dp(6));
        tabs.setBackgroundColor(Color.rgb(18, 22, 30));
        pickHubButton = createTab("Pick-Hub", v -> open(PICK_HUB));
        searchButton = createTab("Search", v -> open(SEARCH));
        crqScannerButton = createTab("CRQ Scan", v -> open(CRQ_SCANNER));
        receiptButton = createTab("Receipt", v -> open(GOODS_RECEIPT));
        tabs.addView(pickHubButton, new LinearLayout.LayoutParams(0, dp(52), 1f));
        tabs.addView(searchButton, new LinearLayout.LayoutParams(0, dp(52), 1f));
        tabs.addView(crqScannerButton, new LinearLayout.LayoutParams(0, dp(52), 1f));
        tabs.addView(receiptButton, new LinearLayout.LayoutParams(0, dp(52), 1f));
        root.addView(tabs, new LinearLayout.LayoutParams(-1, dp(64)));

        setContentView(root);
        webView.loadUrl(PICK_HUB);
        handleDataWedgeIntent(getIntent());
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

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleDataWedgeIntent(intent);
    }

    private void handleDataWedgeIntent(Intent intent) {
        if (intent == null || !SCAN_ACTION.equals(intent.getAction()) || webView == null) return;
        String data = intent.getStringExtra(SCAN_DATA_EXTRA);
        if (data == null || data.trim().isEmpty()) return;

        pendingScan = data.trim();
        String selectorCheck = "(function(){if(location.pathname==='/m/receipt')return !!document.querySelector('input[tabindex=\"-1\"],input[placeholder^=\"Type Serial Number\"]');var s=location.pathname==='/m/crq-scanner'?\""
                + CRQ_SCAN_INPUT + "\":\"" + SEARCH_SCAN_INPUT + "\";return !!document.querySelector(s);})()";
        webView.evaluateJavascript(selectorCheck, value -> {
            if ("true".equals(value)) {
                deliverPendingScan();
            } else {
                webView.evaluateJavascript("location.pathname", path -> {
                    if ("\"/m/crq-scanner\"".equals(path)) {
                        pendingScan = null;
                        android.widget.Toast.makeText(this, "Open a batch and its scan screen first.", android.widget.Toast.LENGTH_SHORT).show();
                    } else if (!"\"/m/receipt\"".equals(path)) {
                        open(SEARCH);
                    }
                });
            }
        });
    }

    private void deliverPendingScan() {
        if (pendingScan == null || webView == null) return;
        String scan = JSONObject.quote(pendingScan);
        String script = "(function(value){"
                + "if(location.pathname==='/m/receipt'){"
                + "for(var i=0;i<value.length;i++){var key=value.charAt(i);window.dispatchEvent(new KeyboardEvent('keydown',{key:key,code:'',keyCode:key.charCodeAt(0),which:key.charCodeAt(0),bubbles:true,cancelable:true}));}"
                + "window.dispatchEvent(new KeyboardEvent('keydown',{key:'Enter',code:'Enter',keyCode:13,which:13,bubbles:true,cancelable:true}));return true;}"
                + "var selector=location.pathname==='/m/crq-scanner'?\"" + CRQ_SCAN_INPUT + "\":\"" + SEARCH_SCAN_INPUT + "\";"
                + "var input=document.querySelector(selector);"
                + "if(!input)return false;"
                + "input.focus();input.select();"
                + "var setter=Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,'value').set;"
                + "setter.call(input,value);input.dispatchEvent(new Event('input',{bubbles:true}));"
                + "setTimeout(function(){input.focus();"
                + "input.dispatchEvent(new KeyboardEvent('keydown',{key:'Enter',code:'Enter',keyCode:13,which:13,bubbles:true,cancelable:true}));"
                + "input.dispatchEvent(new KeyboardEvent('keyup',{key:'Enter',code:'Enter',keyCode:13,which:13,bubbles:true,cancelable:true}));"
                + "},100);return true;}) (" + scan + ");";
        webView.evaluateJavascript(script, result -> {
            if ("true".equals(result)) pendingScan = null;
        });
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
                || (path != null && path.startsWith("/m/search/"))
                || "/m/receipt".equals(path)
                || "/m/crq-scanner".equals(path);
    }

    private void installMobileAppRules() {
        String script = "(function(){"
                + "var style=document.getElementById('lagerloger-wrapper-style');"
                + "if(!style){style=document.createElement('style');style.id='lagerloger-wrapper-style';"
                + "style.textContent='nav.m-glass{display:none!important}body>div{padding-bottom:0!important}';document.head.appendChild(style);}"
                + "if(window.__lagerlogerWrapperInstalled)return;window.__lagerlogerWrapperInstalled=true;"
                + "var allowed=function(p){return p==='/m/collect'||p==='/m/search'||p.indexOf('/m/search/')===0||p==='/m/crq-scanner'||p==='/m/receipt';};"
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
        if (pickHubButton == null || searchButton == null || crqScannerButton == null || receiptButton == null) return;
        boolean isCrqScanner = currentUrl != null && currentUrl.contains("/m/crq-scanner");
        boolean isReceipt = currentUrl != null && currentUrl.contains("/m/receipt");
        boolean isSearch = !isCrqScanner && !isReceipt && currentUrl != null && currentUrl.contains("/m/search");
        styleTab(pickHubButton, !isSearch && !isCrqScanner && !isReceipt);
        styleTab(searchButton, isSearch);
        styleTab(crqScannerButton, isCrqScanner);
        styleTab(receiptButton, isReceipt);
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
