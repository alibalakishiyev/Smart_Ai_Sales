package com.utils; // və ya com.utils

import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.button.MaterialButton;
import com.smart_ai_sales.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ReceiptWebViewDialog extends DialogFragment {

    private static final String ARG_URL = "url";
    private static final String ARG_DOC_ID = "doc_id";
    private static final String TAG = "WebViewDialog";

    private String url;
    private String docId;
    private WebView webView;
    private ProgressBar progressBar;
    private TextView tvTitle;
    private MaterialButton btnDownload;

    private OnReceiptDownloadedListener listener;
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface OnReceiptDownloadedListener {
        void onReceiptDownloaded(String filePath, String docId);
    }

    public static ReceiptWebViewDialog newInstance(String url, String docId) {
        ReceiptWebViewDialog dialog = new ReceiptWebViewDialog();
        Bundle args = new Bundle();
        args.putString(ARG_URL, url);
        args.putString(ARG_DOC_ID, docId);
        dialog.setArguments(args);
        return dialog;
    }

    public void setOnReceiptDownloadedListener(OnReceiptDownloadedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            url = getArguments().getString(ARG_URL);
            docId = getArguments().getString(ARG_DOC_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_receipt_webview, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        webView = view.findViewById(R.id.webView);
        progressBar = view.findViewById(R.id.progressBar);
        tvTitle = view.findViewById(R.id.tvTitle);
        btnDownload = view.findViewById(R.id.btnDownload);
        MaterialButton btnClose = view.findViewById(R.id.btnClose);

        tvTitle.setText("Fiskal kod: " + docId);

        btnClose.setOnClickListener(v -> dismiss());
        btnDownload.setOnClickListener(v -> downloadReceiptFromPage());

        setupWebView();
    }

    private void setupWebView() {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setDomStorageEnabled(true);
        webSettings.setSupportZoom(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);

        // JavaScript interfeysi əlavə et
        webView.addJavascriptInterface(new ReceiptJavaScriptInterface(), "Android");

        // Cookie manager
        CookieManager.getInstance().setAcceptCookie(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                progressBar.setVisibility(View.VISIBLE);
                btnDownload.setVisibility(View.GONE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);

                // Səhifə yükləndikdən sonra yükləmə düyməsini göstər
                btnDownload.setVisibility(View.VISIBLE);

                Log.d(TAG, "Səhifə yükləndi: " + url);

                // Səhifədə şəkil varmı yoxla
                checkForImages();
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                progressBar.setProgress(newProgress);
            }
        });

        webView.loadUrl(url);
    }

    /**
     * Səhifədə şəkil olub-olmadığını yoxla
     */
    private void checkForImages() {
        webView.evaluateJavascript(
                "javascript:(" +
                        "function() {" +
                        "   var images = document.getElementsByTagName('img');" +
                        "   if(images.length > 0) {" +
                        "       return 'found';" +
                        "   }" +
                        "   return 'notfound';" +
                        "})()",
                value -> {
                    if ("found".equals(value)) {
                        Toast.makeText(getContext(), "Şəkil tapıldı, yükləyə bilərsiniz", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    /**
     * Səhifədəki şəkili yüklə
     */
    private void downloadReceiptFromPage() {
        Toast.makeText(getContext(), "Şəkil yüklənir...", Toast.LENGTH_SHORT).show();
        btnDownload.setEnabled(false);

        // JavaScript ilə şəkili tap və base64-ə çevir
        String script =
                "javascript:(" +
                        "function() {" +
                        "   try {" +
                        "       var images = document.getElementsByTagName('img');" +
                        "       for(var i = 0; i < images.length; i++) {" +
                        "           var img = images[i];" +
                        "           if(img.src && img.width > 100) {" +
                        "               var canvas = document.createElement('canvas');" +
                        "               canvas.width = img.width;" +
                        "               canvas.height = img.height;" +
                        "               var ctx = canvas.getContext('2d');" +
                        "               ctx.drawImage(img, 0, 0);" +
                        "               var dataURL = canvas.toDataURL('image/jpeg', 0.9);" +
                        "               Android.onImageDownloaded(dataURL);" +
                        "               return;" +
                        "           }" +
                        "       }" +
                        "       Android.onImageDownloaded('notfound');" +
                        "   } catch(e) {" +
                        "       Android.onImageDownloaded('error:' + e.message);" +
                        "   }" +
                        "})()";

        webView.loadUrl(script);
    }

    /**
     * JavaScript interfeysi
     */
    private class ReceiptJavaScriptInterface {

        @JavascriptInterface
        public void onImageDownloaded(final String dataUrl) {
            mainHandler.post(() -> {
                btnDownload.setEnabled(true);

                if (dataUrl == null || dataUrl.isEmpty()) {
                    Toast.makeText(getContext(), "Şəkil tapılmadı", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (dataUrl.equals("notfound")) {
                    Toast.makeText(getContext(), "Səhifədə şəkil tapılmadı", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (dataUrl.startsWith("error:")) {
                    Toast.makeText(getContext(), "Xəta: " + dataUrl.substring(6), Toast.LENGTH_SHORT).show();
                    return;
                }

                if (dataUrl.startsWith("data:image")) {
                    // Base64 məlumatını yüklə
                    saveBase64Image(dataUrl);
                } else {
                    Toast.makeText(getContext(), "Dəstəklənməyən format", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    // ReceiptWebViewDialog klasında saveBase64Image() metodunu dəyişək:

    private void saveBase64Image(String dataUrl) {
        try {
            String base64Data = dataUrl.substring(dataUrl.indexOf(",") + 1);
            byte[] imageBytes = Base64.decode(base64Data, Base64.DEFAULT);

            String fileName = "receipt_" + docId + "_" + System.currentTimeMillis() + ".jpg";

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ üçün MediaStore
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/SmartAiSales");

                ContentResolver resolver = requireContext().getContentResolver();
                Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

                if (uri != null) {
                    OutputStream outputStream = resolver.openOutputStream(uri);
                    if (outputStream != null) {
                        outputStream.write(imageBytes);
                        outputStream.close();

                        Log.d(TAG, "MediaStore ilə yaddaşa yazıldı: " + uri.toString());

                        // Real fayl yolunu əldə etməyə çalış
                        String filePath = getRealPathFromURI(uri);

                        // ƏSAS: Listener-ə bildir
                        final String finalFilePath = filePath != null ? filePath : uri.toString();

                        new Handler(Looper.getMainLooper()).post(() -> {
                            Toast.makeText(getContext(), "Şəkil qalereyaya yükləndi", Toast.LENGTH_SHORT).show();

                            if (listener != null) {
                                Log.d(TAG, "Listener-ə bildirilir: " + finalFilePath);
                                listener.onReceiptDownloaded(finalFilePath, docId);
                            } else {
                                Log.e(TAG, "Listener NULL! OCR işləməyəcək");
                            }

                            dismiss();
                        });
                    }
                }
            } else {
                // Köhnə versiyalar üçün
                File picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                File appFolder = new File(picturesDir, "SmartAiSales");
                if (!appFolder.exists()) appFolder.mkdirs();

                File destinationFile = new File(appFolder, fileName);
                FileOutputStream fos = new FileOutputStream(destinationFile);
                fos.write(imageBytes);
                fos.close();

                // Qalareyaya əlavə et
                addToGallery(destinationFile.getAbsolutePath());

                final String filePath = destinationFile.getAbsolutePath();

                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(getContext(), "Şəkil qalereyaya yükləndi", Toast.LENGTH_SHORT).show();

                    if (listener != null) {
                        Log.d(TAG, "Listener-ə bildirilir: " + filePath);
                        listener.onReceiptDownloaded(filePath, docId);
                    }

                    dismiss();
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Xəta", e);
            Toast.makeText(getContext(), "Xəta: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // URI-dən real fayl yolunu almaq üçün köməkçi metod
    private String getRealPathFromURI(Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = requireContext().getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            String path = cursor.getString(columnIndex);
            cursor.close();
            return path;
        }
        return null;
    }



    /**
     * Şəkili qalareyaya əlavə et
     */
    private void addToGallery(String filePath) {
        Context context = getContext();
        if (context == null) return;

        // MediaScanner ilə qalareyaya əlavə et
        MediaScannerConnection.scanFile(context,
                new String[]{filePath},
                new String[]{"image/jpeg"},
                (path, uri) -> {
                    Log.d(TAG, "Qalareyaya əlavə edildi: " + path);

                    // Bildiriş göndər
                    if (uri != null) {
                        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                        mediaScanIntent.setData(uri);
                        context.sendBroadcast(mediaScanIntent);
                    }
                });
    }
}