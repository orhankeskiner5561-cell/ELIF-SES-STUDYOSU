package com.elif.sesstudyosu;
import android.Manifest;
import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.*;
import android.net.Uri;
import android.view.*;
import android.widget.*;
import java.io.File;
import java.util.*;
import org.json.*;
import java.net.*;
import java.io.*;

public class MainActivity extends Activity {
 final int BG=0xff100b1a, PANEL=0xff241b32, PURPLE=0xffad80ff, GOLD=0xfff4cf83;
 LinearLayout root; TextView status,musicName,recordName,level,lyricsView,recordList; ArrayList<Long> lyricTimes=new ArrayList<>(); ArrayList<String> lyricLines=new ArrayList<>(); Spinner mic; SeekBar seek; TextView timeLabel; boolean dragging=false; boolean silentMode=false; long silentStartMs=0; int silentBaseMs=0; int silentPositionMs=0; TextView silentButton;
 MediaPlayer player,recordPlayer; MediaRecorder recorder;
 AudioManager audio; ArrayList<AudioDeviceInfo> inputs=new ArrayList<>();
 File recorded; boolean recording=false; Handler handler=new Handler(Looper.getMainLooper());
 TextView label(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setPadding(18,14,18,14);root.addView(t);return t;}
 TextView button(String s,Runnable r){TextView t=new TextView(this);t.setText(s);t.setTextSize(17);t.setTextColor(Color.WHITE);t.setGravity(Gravity.CENTER);t.setPadding(18,20,18,20);GradientDrawable d=new GradientDrawable();d.setColor(PANEL);d.setCornerRadius(22);d.setStroke(2,PURPLE);t.setBackground(d);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(12,7,12,7);root.addView(t,p);t.setOnClickListener(v->r.run());return t;}
 void msg(String s){status.setText(s);}
 @Override public void onCreate(Bundle b){super.onCreate(b);
 getWindow().setStatusBarColor(0xff24202b);getWindow().setNavigationBarColor(BG);
 audio=(AudioManager)getSystemService(AUDIO_SERVICE);
 ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setFitsSystemWindows(true);
 root=new LinearLayout(this);root.setOrientation(1);root.setPadding(16,20,16,28);root.setBackgroundColor(BG);scroll.addView(root);setContentView(scroll);
 label("✦ ELİF",34,GOLD);
 label("KARAOKE  •  SES KAYIT STÜDYOSU  v0.5",13,PURPLE);
 status=label("Şarkınızı seçerek başlayın",15,Color.WHITE);
 musicName=label("Henüz şarkı seçilmedi",19,GOLD);
 button("♫  ŞARKI SEÇ",()->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("audio/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,10);});
 button("▶  OYNAT / DURAKLAT",()->{if(player==null){msg("Önce şarkı seçin");return;}if(silentMode){msg("Sessiz modda müzik çalmaz. KAYDET ile başlayın.");return;}if(player.isPlaying())player.pause();else player.start();});
 seek=new SeekBar(this);root.addView(seek);timeLabel=label("00:00 / 00:00",13,Color.LTGRAY);
 seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar b){dragging=true;}public void onStopTrackingTouch(SeekBar b){dragging=false;if(player!=null){player.seekTo(b.getProgress());if(silentMode&&recording){silentBaseMs=b.getProgress();silentStartMs=android.os.SystemClock.elapsedRealtime();}}}public void onProgressChanged(SeekBar b,int v,boolean user){}});
 label("ŞARKI SÖZLERİ",20,GOLD);
 lyricsView=label("Şarkı seçtiğinizde sözler otomatik aranır. Bulunursa müzikle birlikte akar.",24,Color.WHITE);
 lyricsView.setGravity(Gravity.CENTER);lyricsView.setMinHeight(220);lyricsView.setBackgroundColor(PANEL);
 button("↻  SÖZLERİ TEKRAR ARA",()->{if(currentSong!=null)autoLyrics(currentSong);else msg("Önce şarkı seçin");});
 silentButton=button("◌  SESSİZ KARAOKE: KAPALI",()->{if(recording){msg("Önce kaydı bitirin");return;}silentMode=!silentMode;if(player!=null&&player.isPlaying())player.pause();silentButton.setText(silentMode?"◉  SESSİZ KARAOKE: AÇIK":"◌  SESSİZ KARAOKE: KAPALI");msg(silentMode?"Hoparlör sessiz. Kayıtta sözler zamanla ilerleyecek.":"Normal oynatma modu.");});
 button("●  SÖYLE VE KAYDET",()->{if(recording)stopRecord();else startRecord();});
 level=label("Mikrofon hazır",13,PURPLE);
 recordName=label("Henüz vokal kaydı yok",14,Color.LTGRAY);
 button("▶  KAYDI DİNLE",()->playRecording());
 button("▤  KAYITLARIM",()->showRecordings());
 button("⚙  STÜDYO AYARLARI",()->showSettings());
 mic=new Spinner(this);refreshMics();
 handler.post(lyricsTick);
 }
 Uri currentSong;
 void showSettings(){
 LinearLayout panel=new LinearLayout(this);panel.setOrientation(1);panel.setPadding(24,12,24,8);
 TextView caption=new TextView(this);caption.setText("Mikrofon girişini seçin. Kablolu kulaklık daha düşük gecikme sağlar.");caption.setTextColor(Color.DKGRAY);panel.addView(caption);
 if(mic.getParent()!=null)((android.view.ViewGroup)mic.getParent()).removeView(mic);
 panel.addView(mic);refreshMics();
 new AlertDialog.Builder(this).setTitle("Stüdyo ayarları").setView(panel)
 .setNeutralButton("LRC DOSYASI", (d,w)->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("*/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,11);})
 .setPositiveButton("Tamam",null).show();
 }
 String formatTime(int ms){int sec=Math.max(0,ms/1000);return String.format(java.util.Locale.getDefault(),"%02d:%02d",sec/60,sec%60);}
 Runnable lyricsTick=new Runnable(){public void run(){if(player!=null){int timeline=(silentMode&&recording)?(int)Math.min(player.getDuration(),silentBaseMs+android.os.SystemClock.elapsedRealtime()-silentStartMs):player.getCurrentPosition();if(seek!=null&&!dragging){seek.setMax(Math.max(1,player.getDuration()));seek.setProgress(timeline);timeLabel.setText(formatTime(timeline)+" / "+formatTime(player.getDuration()));}if(((silentMode&&recording)||player.isPlaying())&&!lyricTimes.isEmpty()){long pos=timeline;int index=-1;for(int i=0;i<lyricTimes.size();i++){if(lyricTimes.get(i)<=pos)index=i;else break;}lyricsView.setText(index>=0?lyricLines.get(index):"♪");}}handler.postDelayed(this,200);}};
 void showRecordings(){File[] files=getFilesDir().listFiles((dir,name)->name.startsWith("elif-")&&name.endsWith(".m4a"));if(files==null||files.length==0){msg("Henüz kayıt yok");return;}Arrays.sort(files,(a,b)->Long.compare(b.lastModified(),a.lastModified()));String[] names=new String[files.length];for(int i=0;i<files.length;i++)names[i]=files[i].getName();new AlertDialog.Builder(this).setTitle("ELİF kayıtlarım").setItems(names,(d,which)->{recorded=files[which];recordName.setText("Seçili vokal kaydı");playRecording();}).setNegativeButton("Kapat",null).show();}
 void loadLyrics(Uri uri){lyricTimes.clear();lyricLines.clear();try(java.io.BufferedReader br=new java.io.BufferedReader(new java.io.InputStreamReader(getContentResolver().openInputStream(uri)))){String line;while((line=br.readLine())!=null){java.util.regex.Matcher m=java.util.regex.Pattern.compile("\\[(\\d{1,2}):(\\d{2})(?:\\.(\\d{1,3}))?\\]").matcher(line);int last=0;while(m.find()){int frac=m.group(3)==null?0:Integer.parseInt((m.group(3)+"00").substring(0,3));lyricTimes.add((Long.parseLong(m.group(1))*60+Long.parseLong(m.group(2)))*1000+frac);last=m.end();}if(last>0){String words=line.substring(last).trim();while(lyricLines.size()<lyricTimes.size())lyricLines.add(words);}}ArrayList<Integer> order=new ArrayList<>();for(int i=0;i<lyricTimes.size();i++)order.add(i);order.sort((a,b)->Long.compare(lyricTimes.get(a),lyricTimes.get(b)));ArrayList<Long> ts=new ArrayList<>();ArrayList<String> ls=new ArrayList<>();for(int i:order){ts.add(lyricTimes.get(i));ls.add(lyricLines.get(i));}lyricTimes=ts;lyricLines=ls;lyricsView.setText(ls.isEmpty()?"Bu dosyada zaman etiketli LRC satırı bulunamadı. [00:12.00] söz biçiminde dosya seçin.":"Söz yüklendi: "+ls.size()+" satır");}catch(Exception e){lyricsView.setText("LRC okunamadı: "+e.getMessage());}}
 void autoLyrics(Uri uri){
 lyricTimes.clear();lyricLines.clear();lyricsView.setText("Şarkı sözleri aranıyor...");
 new Thread(()->{
  String result="";String track="";String artist="";
  try{
   MediaMetadataRetriever retriever=new MediaMetadataRetriever();
   try{retriever.setDataSource(this,uri);track=retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);artist=retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);}finally{retriever.release();}
   if(track==null||track.trim().isEmpty()){String file=uri.getLastPathSegment();track=file==null?"":file.replaceAll("(?i)\\\\.mp3$","").replaceAll("^[0-9]+:","");}
   if(track.trim().isEmpty())throw new Exception("Şarkı adı bulunamadı");
   String url="https://lrclib.net/api/search?track_name="+java.net.URLEncoder.encode(track,"UTF-8")+(artist==null||artist.trim().isEmpty()?"":"&artist_name="+java.net.URLEncoder.encode(artist,"UTF-8"));
   HttpURLConnection conn=(HttpURLConnection)new URL(url).openConnection();conn.setConnectTimeout(8000);conn.setReadTimeout(8000);conn.setRequestProperty("User-Agent","ELIF-Ses-Studyosu/0.3");
   StringBuilder body=new StringBuilder();try(BufferedReader reader=new BufferedReader(new InputStreamReader(conn.getInputStream(),"UTF-8"))){String line;while((line=reader.readLine())!=null)body.append(line);}
   JSONArray arr=new JSONArray(body.toString());String synced=null;
   for(int i=0;i<arr.length();i++){JSONObject item=arr.getJSONObject(i);String candidate=item.optString("syncedLyrics","");if(!candidate.isEmpty()&&!candidate.equals("null")){synced=candidate;break;}}
   if(synced==null)throw new Exception("Bu şarkının senkronize sözleri bulunamadı");
   ArrayList<Long> times=new ArrayList<>();ArrayList<String> lines=new ArrayList<>();
   java.util.regex.Pattern pattern=java.util.regex.Pattern.compile("\\\\[(\\\\d{1,2}):(\\\\d{2})(?:\\\\.(\\\\d{1,3}))?\\\\]");
   for(String line:synced.split("\\n")){java.util.regex.Matcher m=pattern.matcher(line);if(m.find()){int frac=m.group(3)==null?0:Integer.parseInt((m.group(3)+"00").substring(0,3));times.add((Long.parseLong(m.group(1))*60+Long.parseLong(m.group(2)))*1000+frac);lines.add(line.substring(m.end()).trim());}}
   if(times.isEmpty())throw new Exception("Zamanlı sözler okunamadı");
   final ArrayList<Long> t=times;final ArrayList<String> l=lines;runOnUiThread(()->{lyricTimes=t;lyricLines=l;lyricsView.setText("Senkronize sözler hazır: "+l.size()+" satır");});
   return;
  }catch(Exception e){result=e.getMessage();}
  final String error=result;runOnUiThread(()->lyricsView.setText("Otomatik söz bulunamadı: "+error+". İsterseniz LRC dosyası seçin."));
 }).start();
}
void refreshMics(){inputs.clear();ArrayList<String> names=new ArrayList<>();names.add("Sistem varsayılan mikrofonu");for(AudioDeviceInfo d:audio.getDevices(AudioManager.GET_DEVICES_INPUTS)){inputs.add(d);names.add(d.getProductName()+" ("+d.getId()+")");}mic.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,names));}
 void startRecord(){if(recording)return;if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},22);return;}
 try{if(silentMode){if(player==null){msg("Önce MP3 seçin");return;}if(player.isPlaying())player.pause();silentBaseMs=0;player.seekTo(0);silentStartMs=android.os.SystemClock.elapsedRealtime();}recorded=new File(getFilesDir(),"elif-"+System.currentTimeMillis()+".m4a");recorder=new MediaRecorder();recorder.setAudioSource(MediaRecorder.AudioSource.MIC);recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);recorder.setAudioEncodingBitRate(128000);recorder.setAudioSamplingRate(44100);recorder.setOutputFile(recorded.getAbsolutePath());int index=mic.getSelectedItemPosition();if(index>0&&index<=inputs.size())recorder.setPreferredDevice(inputs.get(index-1));recorder.prepare();recorder.start();recording=true;msg(silentMode?"Sessiz karaoke kaydı başladı — müzik hoparlörden çalmıyor":"Kayıt yapılıyor...");handler.post(meterTick);}
 catch(Exception e){msg("Kayıt hatası: "+e.getMessage());release();}}
 Runnable meterTick=new Runnable(){public void run(){if(!recording||recorder==null)return;try{level.setText("Mikrofon seviyesi: "+recorder.getMaxAmplitude());}catch(Exception ignored){}handler.postDelayed(this,150);}};
 void release(){if(recorder!=null){try{recorder.release();}catch(Exception ignored){}recorder=null;}}
 void stopRecord(){if(!recording)return;recording=false;if(silentMode)silentPositionMs=(int)(silentBaseMs+android.os.SystemClock.elapsedRealtime()-silentStartMs);handler.removeCallbacks(meterTick);try{recorder.stop();recordName.setText("Vokal kaydı hazır");msg(silentMode?"Çıplak ses kaydı tamamlandı. MP3 ile miks sonraki aşamada.":"Kayıt tamamlandı");}catch(Exception e){msg("Kayıt tamamlanamadı: "+e.getMessage());if(recorded!=null)recorded.delete();recorded=null;}finally{release();level.setText("Mikrofon seviyesi: —");}}
 void playRecording(){if(recorded==null||!recorded.exists()){msg("Önce kayıt yapın");return;}try{if(recordPlayer!=null)recordPlayer.release();recordPlayer=new MediaPlayer();recordPlayer.setDataSource(recorded.getAbsolutePath());recordPlayer.prepare();recordPlayer.start();msg("Kayıt oynatılıyor");}catch(Exception e){msg("Oynatma hatası: "+e.getMessage());}}
 @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==11&&result==RESULT_OK&&data!=null){loadLyrics(data.getData());return;}if(request==10&&result==RESULT_OK&&data!=null){Uri uri=data.getData();if(uri==null)return;try{if(player!=null)player.release();player=new MediaPlayer();player.setDataSource(this,uri);player.prepare();currentSong=uri;String songName="Seçilen şarkı";try{MediaMetadataRetriever mr=new MediaMetadataRetriever();mr.setDataSource(this,uri);String title=mr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);String artist=mr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);if(title!=null&&!title.trim().isEmpty())songName=title+(artist==null?"":" — "+artist);mr.release();}catch(Exception ignored){}musicName.setText(songName);msg("Müzik hazır");autoLyrics(uri);}catch(Exception e){msg("Müzik açılamadı: "+e.getMessage());}}}
 @Override protected void onDestroy(){handler.removeCallbacks(lyricsTick);if(recording)stopRecord();if(player!=null)player.release();if(recordPlayer!=null)recordPlayer.release();super.onDestroy();}
}