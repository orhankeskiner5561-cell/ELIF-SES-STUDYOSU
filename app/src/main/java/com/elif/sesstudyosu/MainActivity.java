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

public class MainActivity extends Activity {
 final int BG=0xff100b1a, PANEL=0xff241b32, PURPLE=0xffad80ff, GOLD=0xfff4cf83;
 LinearLayout root; TextView status,musicName,recordName,level,lyricsView,recordList; ArrayList<Long> lyricTimes=new ArrayList<>(); ArrayList<String> lyricLines=new ArrayList<>(); Spinner mic;
 MediaPlayer player,recordPlayer; MediaRecorder recorder;
 AudioManager audio; ArrayList<AudioDeviceInfo> inputs=new ArrayList<>();
 File recorded; boolean recording=false; Handler handler=new Handler(Looper.getMainLooper());
 TextView label(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setPadding(18,14,18,14);root.addView(t);return t;}
 TextView button(String s,Runnable r){TextView t=new TextView(this);t.setText(s);t.setTextSize(17);t.setTextColor(Color.WHITE);t.setGravity(Gravity.CENTER);t.setPadding(18,20,18,20);GradientDrawable d=new GradientDrawable();d.setColor(PANEL);d.setCornerRadius(22);d.setStroke(2,PURPLE);t.setBackground(d);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(12,7,12,7);root.addView(t,p);t.setOnClickListener(v->r.run());return t;}
 void msg(String s){status.setText(s);}
 @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);audio=(AudioManager)getSystemService(AUDIO_SERVICE);
 ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);root=new LinearLayout(this);root.setOrientation(1);root.setPadding(16,22,16,24);scroll.setFitsSystemWindows(true);root.setBackgroundColor(BG);scroll.addView(root);setContentView(scroll);
 label("✦  ELİF",34,GOLD);label("PROFESYONEL SES STÜDYOSU  •  v0.2",13,PURPLE);
 status=label("Stüdyo hazır",15,Color.WHITE);
 label("MÜZİK",19,GOLD);musicName=label("Henüz müzik seçilmedi",15,Color.LTGRAY);
 button("♫  TELEFONDAN MÜZİK SEÇ",()->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("audio/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,10);});
 button("▶  MÜZİĞİ OYNAT / DURAKLAT",()->{if(player==null){msg("Önce müzik seçin");return;}if(player.isPlaying())player.pause();else player.start();});
 label("MİKROFON GİRİŞİ",19,GOLD);mic=new Spinner(this);root.addView(mic);button("↻  MİKROFONLARI YENİLE",()->refreshMics());refreshMics();
 label("VOKAL KAYDI",19,GOLD);level=label("Mikrofon seviyesi: —",15,PURPLE);
 button("●  KAYDI BAŞLAT",()->startRecord());button("■  KAYDI DURDUR",()->stopRecord());
 recordName=label("Henüz kayıt yok",15,Color.LTGRAY);
 button("▶  SON KAYDI DİNLE",()->playRecording());
 button("▤  TÜM KAYITLAR",()->showRecordings());
 label("KARAOKE / SÖZLER",19,GOLD);button("♫  LRC ŞARKI SÖZÜ DOSYASI SEÇ",()->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("*/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,11);});lyricsView=label("Senkronize sözler için şarkıya ait .lrc dosyasını seçin.",17,GOLD);handler.post(lyricsTick);
 label("Kulaklıkla müziği dinlerken KAYDI BAŞLAT ile ayrı vokal dosyası oluşturabilirsiniz. Bluetooth kulaklıkta gecikme olabilir.",13,Color.LTGRAY);label("Auto-Tune, otomatik söz bulma, miks ve mastering henüz yok.",13,Color.GRAY);
 }
 Runnable lyricsTick=new Runnable(){public void run(){if(player!=null&&player.isPlaying()&&!lyricTimes.isEmpty()){long pos=player.getCurrentPosition();int index=-1;for(int i=0;i<lyricTimes.size();i++){if(lyricTimes.get(i)<=pos)index=i;else break;}lyricsView.setText(index>=0?lyricLines.get(index):"♪");}handler.postDelayed(this,200);}};
 void showRecordings(){File[] files=getFilesDir().listFiles((dir,name)->name.startsWith("elif-")&&name.endsWith(".m4a"));if(files==null||files.length==0){msg("Henüz kayıt yok");return;}Arrays.sort(files,(a,b)->Long.compare(b.lastModified(),a.lastModified()));String[] names=new String[files.length];for(int i=0;i<files.length;i++)names[i]=files[i].getName();new AlertDialog.Builder(this).setTitle("ELİF kayıtlarım").setItems(names,(d,which)->{recorded=files[which];recordName.setText("Seçili kayıt: "+recorded.getName());playRecording();}).setNegativeButton("Kapat",null).show();}
 void loadLyrics(Uri uri){lyricTimes.clear();lyricLines.clear();try(java.io.BufferedReader br=new java.io.BufferedReader(new java.io.InputStreamReader(getContentResolver().openInputStream(uri)))){String line;while((line=br.readLine())!=null){java.util.regex.Matcher m=java.util.regex.Pattern.compile("\\[(\\d{1,2}):(\\d{2})(?:\\.(\\d{1,3}))?\\]").matcher(line);int last=0;while(m.find()){int frac=m.group(3)==null?0:Integer.parseInt((m.group(3)+"00").substring(0,3));lyricTimes.add((Long.parseLong(m.group(1))*60+Long.parseLong(m.group(2)))*1000+frac);last=m.end();}if(last>0){String words=line.substring(last).trim();while(lyricLines.size()<lyricTimes.size())lyricLines.add(words);}}ArrayList<Integer> order=new ArrayList<>();for(int i=0;i<lyricTimes.size();i++)order.add(i);order.sort((a,b)->Long.compare(lyricTimes.get(a),lyricTimes.get(b)));ArrayList<Long> ts=new ArrayList<>();ArrayList<String> ls=new ArrayList<>();for(int i:order){ts.add(lyricTimes.get(i));ls.add(lyricLines.get(i));}lyricTimes=ts;lyricLines=ls;lyricsView.setText(ls.isEmpty()?"Bu dosyada zaman etiketli LRC satırı bulunamadı. [00:12.00] söz biçiminde dosya seçin.":"Söz yüklendi: "+ls.size()+" satır");}catch(Exception e){lyricsView.setText("LRC okunamadı: "+e.getMessage());}}
 void refreshMics(){inputs.clear();ArrayList<String> names=new ArrayList<>();names.add("Sistem varsayılan mikrofonu");for(AudioDeviceInfo d:audio.getDevices(AudioManager.GET_DEVICES_INPUTS)){inputs.add(d);names.add(d.getProductName()+" ("+d.getId()+")");}mic.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,names));}
 void startRecord(){if(recording)return;if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},22);return;}
 try{recorded=new File(getFilesDir(),"elif-"+System.currentTimeMillis()+".m4a");recorder=new MediaRecorder();recorder.setAudioSource(MediaRecorder.AudioSource.MIC);recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);recorder.setAudioEncodingBitRate(128000);recorder.setAudioSamplingRate(44100);recorder.setOutputFile(recorded.getAbsolutePath());int index=mic.getSelectedItemPosition();if(index>0&&index<=inputs.size())recorder.setPreferredDevice(inputs.get(index-1));recorder.prepare();recorder.start();recording=true;msg("Kayıt yapılıyor...");handler.post(meterTick);}
 catch(Exception e){msg("Kayıt hatası: "+e.getMessage());release();}}
 Runnable meterTick=new Runnable(){public void run(){if(!recording||recorder==null)return;try{level.setText("Mikrofon seviyesi: "+recorder.getMaxAmplitude());}catch(Exception ignored){}handler.postDelayed(this,150);}};
 void release(){if(recorder!=null){try{recorder.release();}catch(Exception ignored){}recorder=null;}}
 void stopRecord(){if(!recording)return;recording=false;handler.removeCallbacks(meterTick);try{recorder.stop();recordName.setText("Kaydedildi: "+recorded.getName());msg("Kayıt tamamlandı");}catch(Exception e){msg("Kayıt tamamlanamadı: "+e.getMessage());if(recorded!=null)recorded.delete();recorded=null;}finally{release();level.setText("Mikrofon seviyesi: —");}}
 void playRecording(){if(recorded==null||!recorded.exists()){msg("Önce kayıt yapın");return;}try{if(recordPlayer!=null)recordPlayer.release();recordPlayer=new MediaPlayer();recordPlayer.setDataSource(recorded.getAbsolutePath());recordPlayer.prepare();recordPlayer.start();msg("Kayıt oynatılıyor");}catch(Exception e){msg("Oynatma hatası: "+e.getMessage());}}
 @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==11&&result==RESULT_OK&&data!=null){loadLyrics(data.getData());return;}if(request==10&&result==RESULT_OK&&data!=null){Uri uri=data.getData();if(uri==null)return;try{if(player!=null)player.release();player=new MediaPlayer();player.setDataSource(this,uri);player.prepare();musicName.setText("Seçilen: "+uri.getLastPathSegment());msg("Müzik yüklendi");}catch(Exception e){msg("Müzik açılamadı: "+e.getMessage());}}}
 @Override protected void onDestroy(){handler.removeCallbacks(lyricsTick);if(recording)stopRecord();if(player!=null)player.release();if(recordPlayer!=null)recordPlayer.release();super.onDestroy();}
}