package com.nishat.servernotify;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.os.Handler;
import android.os.Looper;
import androidx.core.app.NotificationCompat;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import org.json.JSONObject;
import java.util.concurrent.TimeUnit;

public class NotificationService extends Service {
  private static final String CHANNEL="chat_messages", FOREGROUND="chat_connection";
  private static final String FIRESTORE_URL="https://firestore.googleapis.com/v1/projects/nishatmutationautofill-3bde4/databases/(default)/documents/config/chat";
  private OkHttpClient client; private WebSocket socket; private boolean stopping=false, fetchingServer=false; private final Handler handler=new Handler(Looper.getMainLooper()); private final Runnable retryTask=()->{if(!stopping&&socket==null)connect();};
  @Override public void onCreate(){super.onCreate(); createChannels(); startForeground(101, build("নোটিফিকেশন সার্ভিস চালু", "সার্ভারের সাথে সংযোগের চেষ্টা চলছে", true)); client=new OkHttpClient.Builder().pingInterval(25, TimeUnit.SECONDS).retryOnConnectionFailure(true).build(); connect();}
  @Override public int onStartCommand(Intent i,int flags,int id){if(socket==null)connect();return START_STICKY;}
  private void createChannels(){if(Build.VERSION.SDK_INT>=26){NotificationManager nm=getSystemService(NotificationManager.class);nm.createNotificationChannel(new NotificationChannel(CHANNEL,"চ্যাট মেসেজ",NotificationManager.IMPORTANCE_HIGH));nm.createNotificationChannel(new NotificationChannel(FOREGROUND,"সার্ভার সংযোগ",NotificationManager.IMPORTANCE_LOW));}}
  private Notification build(String title,String text,boolean ongoing){Intent open=new Intent(this,MainActivity.class);PendingIntent pi=PendingIntent.getActivity(this,0,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);return new NotificationCompat.Builder(this,ongoing?FOREGROUND:CHANNEL).setSmallIcon(android.R.drawable.sym_action_chat).setContentTitle(title).setContentText(text).setContentIntent(pi).setAutoCancel(!ongoing).setOngoing(ongoing).setPriority(ongoing?NotificationCompat.PRIORITY_LOW:NotificationCompat.PRIORITY_HIGH).build();}
  private void connect(){if(stopping||fetchingServer||socket!=null)return;SharedPreferences p=getSharedPreferences("chat_admin",MODE_PRIVATE);String base=p.getString("server_url","");String token=p.getString("admin_token","");if(token.isEmpty()){stopSelf();return;}if(base.isEmpty()){fetchingServer=true;Request fr=new Request.Builder().url(FIRESTORE_URL).get().build();client.newCall(fr).enqueue(new okhttp3.Callback(){@Override public void onFailure(okhttp3.Call call,java.io.IOException e){fetchingServer=false;if(!stopping)handler.postDelayed(retryTask,5000);}@Override public void onResponse(okhttp3.Call call,Response response)throws java.io.IOException{String url="";try{if(response.isSuccessful()){JSONObject doc=new JSONObject(response.body().string());JSONObject fields=doc.optJSONObject("fields");JSONObject field=fields==null?null:fields.optJSONObject("serverUrl");if(field!=null)url=field.optString("stringValue","");}}catch(Exception ignored){}finally{response.close();}fetchingServer=false;if(url.matches("(?i)^https://[^ ]+")){getSharedPreferences("chat_admin",MODE_PRIVATE).edit().putString("server_url",url.replaceAll("/+$","")).apply();handler.post(()->connect());}else if(!stopping)handler.postDelayed(retryTask,5000);}});return;}String wsUrl=base.replaceFirst("(?i)^https://","wss://").replaceFirst("(?i)^http://","ws://")+"/admin-ws?token="+android.net.Uri.encode(token);Request req=new Request.Builder().url(wsUrl).build();socket=client.newWebSocket(req,new WebSocketListener(){
    @Override public void onOpen(WebSocket ws,Response r){if(socket==ws) {NotificationManager nm=getSystemService(NotificationManager.class);nm.notify(101,build("নোটিফিকেশন চালু আছে","চ্যাট সার্ভারের সাথে সংযুক্ত",true));}}
    @Override public void onMessage(WebSocket ws,String text){try{JSONObject m=new JSONObject(text);String t=m.optString("t");if("msg".equals(t)){JSONObject msg=m.optJSONObject("msg");if(msg!=null&&!"admin".equals(msg.optString("from"))){String body=msg.optString("text");JSONObject f=msg.optJSONObject("file");if(body.isEmpty()&&f!=null)body="ফাইল পাঠানো হয়েছে: "+f.optString("name","ফাইল");if(body.isEmpty())body="নতুন মেসেজ এসেছে";showMessage("নতুন মেসেজ",body);}}else if("group_msg".equals(t)){JSONObject msg=m.optJSONObject("message");if(msg!=null&&msg.optString("gid","public").equals("public")&&!"admin".equals(msg.optString("from"))){String body=msg.optString("text");JSONObject f=msg.optJSONObject("file");if(body.isEmpty()&&f!=null)body="ফাইল পাঠানো হয়েছে: "+f.optString("name","ফাইল");showMessage("গ্রুপে নতুন মেসেজ",body.isEmpty()?"নতুন গ্রুপ মেসেজ এসেছে":body);}}else if("denied".equals(t)){getSharedPreferences("chat_admin",MODE_PRIVATE).edit().remove("admin_token").apply();showMessage("লগইন প্রয়োজন","অ্যাডমিন টোকেন যাচাই করুন");stopSelf();}}catch(Exception ignored){}}
    @Override public void onClosed(WebSocket ws,int code,String reason){if(socket==ws)socket=null;if(!stopping)handler.postDelayed(retryTask,5000);}
    @Override public void onFailure(WebSocket ws,Throwable e,Response r){if(socket==ws)socket=null;if(!stopping)handler.postDelayed(retryTask,5000);}
  });}
  private void showMessage(String title,String text){NotificationManager nm=getSystemService(NotificationManager.class);int id=(int)(System.currentTimeMillis()%100000)+200;nm.notify(id,build(title,text,false));}
  @Override public void onTaskRemoved(Intent root){super.onTaskRemoved(root);}
  @Override public void onDestroy(){stopping=true;handler.removeCallbacks(retryTask);if(socket!=null)socket.close(1000,"service stopped");if(client!=null)client.dispatcher().executorService().shutdown();super.onDestroy();}
  @Override public IBinder onBind(Intent i){return null;}
}
