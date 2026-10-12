package com.nishat.servernotify;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.app.AlertDialog;
import android.text.InputType;
import android.view.ViewGroup;

public class MainActivity extends Activity {
  private WebView web;
  private SharedPreferences prefs;
  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
    prefs=getSharedPreferences("chat_admin", MODE_PRIVATE);
    if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
      requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 40);
    web=new WebView(this); web.setLayoutParams(new ViewGroup.LayoutParams(-1,-1)); setContentView(web);
    WebSettings s=web.getSettings(); s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true); s.setDatabaseEnabled(true); s.setAllowFileAccess(true); s.setLoadsImagesAutomatically(true);
    web.setWebViewClient(new WebViewClient()); web.setWebChromeClient(new WebChromeClient());
    web.addJavascriptInterface(new Bridge(), "AndroidBridge");
    if(prefs.getString("server_url", "").isEmpty()) showServerDialog(); else web.loadUrl("file:///android_asset/index.html");
  }
  private void showServerDialog(){
    EditText input=new EditText(this); input.setSingleLine(true); input.setHint("https://chat.example.com"); input.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
    new AlertDialog.Builder(this).setTitle("নিজস্ব VPS সার্ভার URL").setMessage("HTTPS URL দিন (শেষে / দেবেন না)।").setView(input)
      .setCancelable(false).setPositiveButton("সংরক্ষণ",(d,w)->{String u=input.getText().toString().trim().replaceAll("/+$", ""); if(u.matches("(?i)^https://[^ ]+")){prefs.edit().putString("server_url",u).apply(); web.loadUrl("file:///android_asset/index.html");}else showServerDialog();}).show();
  }
  public class Bridge {
    @JavascriptInterface public String getServerUrl(){return prefs.getString("server_url", "");}
    @JavascriptInterface public String getAdminToken(){return prefs.getString("admin_token", "");}
    @JavascriptInterface public void setAdminToken(String token){if(token==null||token.trim().isEmpty())return; prefs.edit().putString("admin_token",token.trim()).apply(); startNotificationService();}
    @JavascriptInterface public void clearAdminToken(){prefs.edit().remove("admin_token").apply(); stopService(new Intent(MainActivity.this,NotificationService.class));}
    @JavascriptInterface public void setServerUrl(String url){if(url!=null&&url.matches("(?i)^https://[^ ]+"))prefs.edit().putString("server_url",url.replaceAll("/+$", "")).apply();}
  }
  private void startNotificationService(){Intent i=new Intent(this,NotificationService.class); if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);}
  @Override protected void onResume(){super.onResume(); if(prefs!=null&&!prefs.getString("admin_token","").isEmpty()&&!prefs.getString("server_url","").isEmpty())startNotificationService();}
  @Override public void onBackPressed(){if(web!=null&&web.canGoBack())web.goBack();else super.onBackPressed();}
}
