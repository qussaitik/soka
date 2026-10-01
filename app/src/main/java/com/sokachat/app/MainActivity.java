package com.sokachat.app;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.PermissionRequest;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "SokaChat";
    private static final String START_URL = "https://alshaqi.lovable.app/room";
    private static final int RC_MEDIA = 1001;
    private static final int RC_FILE = 1002;

    private FrameLayout container;
    private WebView webView;
    private ValueCallback<Uri[]> fileCallback;
    private String pendingUrl;
    private boolean rendererRecovering = false;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        container = new FrameLayout(this);
        container.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        setContentView(container);

        requestMediaPermissions();
        createWebView();

        if (state != null) {
            webView.restoreState(state);
        } else {
            webView.loadUrl(START_URL);
        }
    }

    private void requestMediaPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            boolean mic = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED;
            boolean cam = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED;
            if (mic || cam) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA},
                        RC_MEDIA);
            }
        }
    }

    private void createWebView() {
        webView = new WebView(this);
        webView.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        container.addView(webView);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setJavaScriptCanOpenWindowsAutomatically(true);
        s.setSupportMultipleWindows(true);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setLoadWithOverviewMode(false);
        s.setUseWideViewPort(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        s.setUserAgentString(s.getUserAgentString() + " SokaChatAndroid/1.0");

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new SafeWebViewClient());
        webView.setWebChromeClient(new SafeChromeClient());

        webView.setDownloadListener(new DownloadListener() {
            @Override public void onDownloadStart(String url, String userAgent, String contentDisposition,
                                                  String mimetype, long contentLength) {
                try {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(i);
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "تعذر فتح الملف", Toast.LENGTH_SHORT).show();
                }
            }
        });

        webView.setOnLongClickListener(v -> false);
        webView.setBackgroundColor(0xFF000000);
    }

    private class SafeWebViewClient extends WebViewClient {
        @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            Uri u = request.getUrl();
            String scheme = u.getScheme();
            if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
                view.loadUrl(u.toString());
                return true;
            }
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, u));
            } catch (ActivityNotFoundException ignored) {}
            return true;
        }

        @Override public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url.startsWith("http://") || url.startsWith("https://")) {
                view.loadUrl(url);
                return true;
            }
            try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
            catch (Exception ignored) {}
            return true;
        }

        @Override public void onPageStarted(WebView view, String url, Bitmap favicon) {
            pendingUrl = url;
        }

        @Override public void onReceivedError(WebView view, WebResourceRequest req, WebResourceError err) {
            Log.e(TAG, "Web error: " + err.getDescription());
        }

        @Override public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
            Log.e(TAG, "Renderer gone. crashed=" + detail.didCrash());
            recoverRenderer();
            return true;
        }
    }

    private class SafeChromeClient extends WebChromeClient {
        @Override public void onPermissionRequest(final PermissionRequest request) {
            runOnUiThread(() -> {
                String origin = request.getOrigin() != null ? request.getOrigin().toString() : "";
                if (origin.startsWith("https://alshaqi.lovable.app") ||
                    origin.startsWith("https://lovable.app")) {
                    request.grant(request.getResources());
                } else {
                    request.deny();
                }
            });
        }

        @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback,
                                                   FileChooserParams params) {
            if (fileCallback != null) fileCallback.onReceiveValue(null);
            fileCallback = callback;
            try {
                Intent intent = params.createIntent();
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                startActivityForResult(intent, RC_FILE);
                return true;
            } catch (Exception e) {
                fileCallback = null;
                return false;
            }
        }

        @Override public boolean onCreateWindow(WebView view, boolean dialog, boolean userGesture, Message resultMsg) {
            WebView.HitTestResult hit = view.getHitTestResult();
            if (hit != null && hit.getExtra() != null) {
                view.loadUrl(hit.getExtra());
            }
            return false;
        }
    }

    private void recoverRenderer() {
        if (rendererRecovering) return;
        rendererRecovering = true;

        runOnUiThread(() -> {
            String restoreUrl = pendingUrl != null ? pendingUrl : START_URL;

            if (webView != null) {
                container.removeView(webView);
                try { webView.stopLoading(); } catch (Exception ignored) {}
                try { webView.destroy(); } catch (Exception ignored) {}
                webView = null;
            }

            createWebView();
            rendererRecovering = false;

            if (restoreUrl != null && !restoreUrl.isEmpty()) {
                webView.loadUrl(restoreUrl);
            } else {
                webView.loadUrl(START_URL);
            }
        });
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_FILE) {
            if (fileCallback != null) {
                Uri[] results = null;
                if (resultCode == Activity.RESULT_OK && data != null) {
                    if (data.getClipData() != null) {
                        int n = data.getClipData().getItemCount();
                        results = new Uri[n];
                        for (int i = 0; i < n; i++) results[i] = data.getClipData().getItemAt(i).getUri();
                    } else if (data.getData() != null) {
                        results = new Uri[]{data.getData()};
                    }
                }
                fileCallback.onReceiveValue(results);
                fileCallback = null;
            }
        }
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        if (webView != null) webView.saveState(out);
        super.onSaveInstanceState(out);
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override protected void onDestroy() {
        if (webView != null) {
            container.removeView(webView);
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
