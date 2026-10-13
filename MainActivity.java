package id.ruanghari.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.NumberFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class MainActivity extends Activity {
    private final int BLUE = 0xFF527DA8, PALE_BLUE = 0xFFEAF2F9, GREEN = 0xFF6C9D85;
    private final int PALE_GREEN = 0xFFEAF4EE, BG = 0xFFF5F7F9, INK = 0xFF243746, MUTED = 0xFF71808D;
    private SharedPreferences prefs;
    private ArrayList<JSONObject> activities = new ArrayList<>();
    private ArrayList<JSONObject> savings = new ArrayList<>();
    private ArrayList<JSONObject> goals = new ArrayList<>();
    private LocalDate selectedDate = LocalDate.now();
    private String activeTab = "agenda";
    private LinearLayout root, content, topBar, nav;
    private final DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", new Locale("id","ID"));
    private final DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }
        prefs = getSharedPreferences("ruanghari", MODE_PRIVATE);
        loadData();
        createNotificationChannelIfNeeded();
        askNotificationPermission();
        rescheduleUpcoming();
        render();
    }

    private void createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= 26) {
            android.app.NotificationManager nm = (android.app.NotificationManager)getSystemService(NOTIFICATION_SERVICE);
            android.app.NotificationChannel ch = new android.app.NotificationChannel(
                ReminderReceiver.CHANNEL_ID, "Pengingat kegiatan", android.app.NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("Notifikasi dan alarm kegiatan dari Ruang Hari");
            nm.createNotificationChannel(ch);
        }
    }

    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 88);
        }
    }

    private void loadData() {
        activities = parseArray(prefs.getString("activities", "[]"));
        savings = parseArray(prefs.getString("savings", "[]"));
        goals = parseArray(prefs.getString("goals", "[]"));
    }
    private ArrayList<JSONObject> parseArray(String raw) {
        ArrayList<JSONObject> list = new ArrayList<>();
        try { JSONArray a = new JSONArray(raw); for (int i=0;i<a.length();i++) list.add(a.getJSONObject(i)); } catch(Exception ignored) {}
        return list;
    }
    private void saveData() {
        prefs.edit().putString("activities", toArray(activities).toString())
            .putString("savings", toArray(savings).toString())
            .putString("goals", toArray(goals).toString()).apply();
    }
    private JSONArray toArray(ArrayList<JSONObject> list) {
        JSONArray a = new JSONArray(); for (JSONObject o:list) a.put(o); return a;
    }

    private void render() {
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(BG);
        setContentView(root);
        topBar = new LinearLayout(this); topBar.setGravity(Gravity.CENTER_VERTICAL); topBar.setPadding(dp(20),dp(15),dp(20),dp(12));
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        TextView logo = new TextView(this); logo.setText("◉"); logo.setTextColor(BLUE); logo.setTextSize(26); logo.setGravity(Gravity.CENTER);
        logo.setBackground(round(PALE_BLUE, 16)); LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(48),dp(48)); lp.setMargins(0,0,dp(12),0); topBar.addView(logo,lp);
        LinearLayout titles = new LinearLayout(this); titles.setOrientation(LinearLayout.VERTICAL);
        TextView name = text("Ruang Hari",20,INK,true); titles.addView(name);
        TextView subtitle = text("Atur hari, jaga tujuan.",12,MUTED,false); titles.addView(subtitle);
        topBar.addView(titles,new LinearLayout.LayoutParams(0,-2,1));
        TextView date = text("▦",24,BLUE,true); date.setGravity(Gravity.CENTER); date.setOnClickListener(v -> pickDate());
        topBar.addView(date,new LinearLayout.LayoutParams(dp(42),dp(42)));
        root.addView(topBar);
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true);
        content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(dp(18),dp(4),dp(18),dp(20));
        scroll.addView(content); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        buildContent();
        buildNav();
    }

    private void buildContent() {
        content.removeAllViews();
        if ("agenda".equals(activeTab)) buildAgenda();
        else if ("savings".equals(activeTab)) buildSavings();
        else buildGoals();
    }

    private void buildAgenda() {
        card(content, PALE_BLUE);
        TextView greeting = text("Jadwal harimu",22,INK,true); content.addView(greeting);
        TextView dateLine = text(selectedDate.format(dateFmt),13,MUTED,false); dateLine.setPadding(0,dp(4),0,dp(12)); content.addView(dateLine);
        LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL);
        Button prev = softButton("‹ Kemarin", PALE_BLUE, BLUE); prev.setOnClickListener(v->{selectedDate=selectedDate.minusDays(1);render();}); row.addView(prev,new LinearLayout.LayoutParams(0,dp(42),1));
        Button next = softButton("Besok ›", PALE_GREEN, GREEN); next.setOnClickListener(v->{selectedDate=selectedDate.plusDays(1);render();}); LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(0,dp(42),1); nlp.setMargins(dp(8),0,0,0); row.addView(next,nlp);
        content.addView(row); content.addView(space(8));
        Button add = solidButton("+ Tambah kegiatan", BLUE); add.setOnClickListener(v->showActivityDialog(null)); content.addView(add);
        content.addView(space(14));
        ArrayList<JSONObject> todays = new ArrayList<>();
        for(JSONObject o:activities) if(selectedDate.toString().equals(o.optString("date"))) todays.add(o);
        Collections.sort(todays,(a,b)->a.optString("time","23:59").compareTo(b.optString("time","23:59")));
        if(todays.isEmpty()) {
            LinearLayout empty = panel(); empty.setGravity(Gravity.CENTER); empty.setPadding(dp(20),dp(26),dp(20),dp(26));
            TextView icon=text("☁",34,BLUE,true); icon.setGravity(Gravity.CENTER); empty.addView(icon);
            TextView msg=text("Hari masih kosong",16,INK,true); msg.setGravity(Gravity.CENTER); empty.addView(msg);
            TextView hint=text("Tambahkan kegiatan agar rencanamu tercatat.",13,MUTED,false); hint.setGravity(Gravity.CENTER); empty.addView(hint);
            content.addView(empty);
        } else {
            for(JSONObject o:todays) {
                LinearLayout item = panel(); item.setOrientation(LinearLayout.HORIZONTAL); item.setGravity(Gravity.CENTER_VERTICAL);
                TextView time = text(o.optString("time","--:--"),14,BLUE,true); time.setGravity(Gravity.CENTER);
                time.setBackground(round(PALE_BLUE,12)); item.addView(time,new LinearLayout.LayoutParams(dp(66),dp(48)));
                LinearLayout info = new LinearLayout(this); info.setOrientation(LinearLayout.VERTICAL); info.setPadding(dp(12),0,dp(5),0);
                TextView title=text(o.optString("title","Kegiatan"),15,INK,true); info.addView(title);
                String note=o.optString("note","");
                if(!note.isEmpty()) info.addView(text(note,12,MUTED,false));
                if(o.optBoolean("done",false)) info.addView(text("✓ Selesai",11,GREEN,true));
                item.addView(info,new LinearLayout.LayoutParams(0,-2,1));
                TextView more=text("⋮",24,MUTED,true); more.setGravity(Gravity.CENTER); more.setOnClickListener(v->activityOptions(o)); item.addView(more,new LinearLayout.LayoutParams(dp(28),dp(42)));
                LinearLayout.LayoutParams ilp=new LinearLayout.LayoutParams(-1,-2); ilp.setMargins(0,0,0,dp(9)); content.addView(item,ilp);
            }
        }
    }

    private void buildSavings() {
        card(content, PALE_GREEN);
        content.addView(text("Tabungan harian",22,INK,true));
        content.addView(text("Catat uang masuk dan pantau kebiasaan menabung.",13,MUTED,false));
        long total=0; for(JSONObject o:savings) total+=o.optLong("amount",0);
        content.addView(space(12));
        content.addView(text("TOTAL TERSIMPAN",11,GREEN,true));
        content.addView(text(rupiah(total),30,INK,true));
        content.addView(space(12));
        Button add=solidButton("+ Catat tabungan",GREEN); add.setOnClickListener(v->showSavingDialog()); content.addView(add);
        content.addView(space(14));
        content.addView(text("Riwayat tabungan",16,INK,true)); content.addView(space(6));
        ArrayList<JSONObject> list=new ArrayList<>(savings);
        Collections.sort(list,(a,b)->b.optString("date","").compareTo(a.optString("date","")));
        if(list.isEmpty()) content.addView(emptyPanel("Belum ada tabungan","Setiap setoran kecil tetap berarti."));
        for(JSONObject o:list) {
            LinearLayout p=panel(); p.setOrientation(LinearLayout.HORIZONTAL); p.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL);
            l.addView(text(o.optString("label","Tabungan"),15,INK,true));
            l.addView(text(o.optString("date",""),12,MUTED,false));
            p.addView(l,new LinearLayout.LayoutParams(0,-2,1));
            p.addView(text("+"+rupiah(o.optLong("amount",0)),15,GREEN,true));
            content.addView(p,new LinearLayout.LayoutParams(-1,-2));
        }
    }

    private void buildGoals() {
        card(content, PALE_BLUE);
        content.addView(text("Target yang ingin dicapai",22,INK,true));
        content.addView(text("Pecah tujuan besar menjadi progres yang terlihat.",13,MUTED,false));
        content.addView(space(12));
        Button add=solidButton("+ Tambah target",BLUE); add.setOnClickListener(v->showGoalDialog()); content.addView(add);
        content.addView(space(14));
        if(goals.isEmpty()) content.addView(emptyPanel("Belum ada target","Tambahkan target belajar, kebiasaan, atau tabungan."));
        for(JSONObject g:goals) {
            LinearLayout p=panel(); p.setOrientation(LinearLayout.VERTICAL);
            LinearLayout line=new LinearLayout(this); line.setGravity(Gravity.CENTER_VERTICAL);
            line.addView(text(g.optString("title","Target"),16,INK,true),new LinearLayout.LayoutParams(0,-2,1));
            TextView menu=text("⋮",23,MUTED,true); menu.setOnClickListener(v->goalOptions(g)); line.addView(menu);
            p.addView(line);
            String desc=g.optString("desc",""); if(!desc.isEmpty()) p.addView(text(desc,12,MUTED,false));
            int target=Math.max(1,g.optInt("target",1)), progress=Math.min(target,Math.max(0,g.optInt("progress",0)));
            TextView amount=text(progress+" / "+target+"  •  "+(int)(100.0*progress/target)+"%",12,GREEN,true);
            amount.setPadding(0,dp(10),0,dp(5)); p.addView(amount);
            ProgressBar bar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal); bar.setMax(target); bar.setProgress(progress);
            if(Build.VERSION.SDK_INT>=21) bar.setProgressTintList(android.content.res.ColorStateList.valueOf(GREEN));
            p.addView(bar,new LinearLayout.LayoutParams(-1,dp(7)));
            content.addView(p,new LinearLayout.LayoutParams(-1,-2));
        }
    }

    private void buildNav() {
        nav=new LinearLayout(this); nav.setOrientation(LinearLayout.HORIZONTAL); nav.setPadding(dp(8),dp(8),dp(8),dp(8)); nav.setBackgroundColor(0xFFFFFFFF);
        addNavItem("◷","Agenda","agenda"); addNavItem("▤","Tabungan","savings"); addNavItem("◎","Target","goals");
        root.addView(nav,new LinearLayout.LayoutParams(-1,dp(68)));
    }
    private void addNavItem(String icon,String label,String tab) {
        LinearLayout item=new LinearLayout(this); item.setOrientation(LinearLayout.VERTICAL); item.setGravity(Gravity.CENTER);
        int c=activeTab.equals(tab)?BLUE:MUTED;
        TextView i=text(icon,22,c,true); i.setGravity(Gravity.CENTER); item.addView(i);
        TextView t=text(label,11,c,activeTab.equals(tab)); t.setGravity(Gravity.CENTER); item.addView(t);
        item.setOnClickListener(v->{activeTab=tab;render();});
        nav.addView(item,new LinearLayout.LayoutParams(0,-1,1));
    }

    private void showActivityDialog(JSONObject existing) {
        final boolean edit=existing!=null;
        LinearLayout form=new LinearLayout(this); form.setOrientation(LinearLayout.VERTICAL); form.setPadding(dp(8),dp(4),dp(8),0);
        EditText title=field("Nama kegiatan"); EditText note=field("Catatan (opsional)");
        EditText date=field("Tanggal (YYYY-MM-DD)"); date.setText(edit?existing.optString("date"):selectedDate.toString());
        EditText time=field("Jam (HH:MM)"); time.setText(edit?existing.optString("time"):"08:00");
        CheckBox alarm=new CheckBox(this); alarm.setText("Aktifkan notifikasi dan alarm"); alarm.setTextColor(INK); alarm.setButtonTintList(android.content.res.ColorStateList.valueOf(BLUE)); alarm.setChecked(!edit||existing.optBoolean("alarm",true));
        if(edit){title.setText(existing.optString("title"));note.setText(existing.optString("note"));}
        form.addView(title); form.addView(note); form.addView(date); form.addView(time); form.addView(alarm);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(edit?"Ubah kegiatan":"Kegiatan baru").setView(form)
            .setNegativeButton("Batal",null).setPositiveButton("Simpan",null).create();
        dialog.setOnShowListener(x->dialog.getButton(-1).setOnClickListener(v->{
            String t=title.getText().toString().trim(), d=date.getText().toString().trim(), tm=time.getText().toString().trim();
            try { LocalDate.parse(d); LocalTime.parse(tm); } catch(Exception ex){toast("Periksa format tanggal dan jam.");return;}
            if(t.isEmpty()){title.setError("Nama kegiatan wajib diisi");return;}
            JSONObject o=edit?existing:new JSONObject();
            try{o.put("id",edit?existing.optInt("id"):nextId());o.put("title",t);o.put("note",note.getText().toString().trim());o.put("date",d);o.put("time",tm);o.put("alarm",alarm.isChecked());if(!edit)o.put("done",false);}catch(Exception ignored){}
            if(!edit)activities.add(o);
            saveData(); schedule(o); selectedDate=LocalDate.parse(d); dialog.dismiss(); render();
        }));
        dialog.show();
    }

    private void activityOptions(JSONObject o) {
        String[] opts=o.optBoolean("done",false)?new String[]{"Tandai belum selesai","Ubah kegiatan","Hapus"}:new String[]{"Tandai selesai","Ubah kegiatan","Hapus"};
        new AlertDialog.Builder(this).setTitle(o.optString("title")).setItems(opts,(d,which)->{
            if(which==0){try{o.put("done",!o.optBoolean("done",false));}catch(Exception ignored){}saveData();render();}
            if(which==1)showActivityDialog(o);
            if(which==2)new AlertDialog.Builder(this).setMessage("Hapus kegiatan ini?").setNegativeButton("Batal",null).setPositiveButton("Hapus",(dd,w)->{cancelAlarm(o);activities.remove(o);saveData();render();}).show();
        }).show();
    }

    private void showSavingDialog() {
        LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(dp(8),0,dp(8),0);
        EditText label=field("Keterangan (mis. uang saku disisihkan)");EditText amount=field("Jumlah rupiah");
        EditText date=field("Tanggal (YYYY-MM-DD)");date.setText(LocalDate.now().toString());
        form.addView(label);form.addView(amount);form.addView(date);
        new AlertDialog.Builder(this).setTitle("Catat tabungan").setView(form).setNegativeButton("Batal",null).setPositiveButton("Simpan",(d,w)->{
            try{long a=Long.parseLong(amount.getText().toString().trim());LocalDate.parse(date.getText().toString().trim());if(a<=0)throw new Exception();
                JSONObject o=new JSONObject();o.put("label",label.getText().toString().trim().isEmpty()?"Tabungan harian":label.getText().toString().trim());o.put("amount",a);o.put("date",date.getText().toString().trim());savings.add(o);saveData();render();
            }catch(Exception e){toast("Masukkan jumlah dan tanggal yang valid.");}
        }).show();
    }

    private void showGoalDialog() {
        LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(dp(8),0,dp(8),0);
        EditText title=field("Nama target");EditText desc=field("Deskripsi (opsional)");EditText target=field("Total langkah / target angka");
        form.addView(title);form.addView(desc);form.addView(target);
        new AlertDialog.Builder(this).setTitle("Target baru").setView(form).setNegativeButton("Batal",null).setPositiveButton("Simpan",(d,w)->{
            try{String t=title.getText().toString().trim();int n=Integer.parseInt(target.getText().toString().trim());if(t.isEmpty()||n<=0)throw new Exception();
                JSONObject g=new JSONObject();g.put("title",t);g.put("desc",desc.getText().toString().trim());g.put("target",n);g.put("progress",0);goals.add(g);saveData();render();
            }catch(Exception e){toast("Isi nama target dan angka target yang valid.");}
        }).show();
    }
    private void goalOptions(JSONObject g) {
        String[] opts={"Tambah progres","Ubah target","Hapus target"};
        new AlertDialog.Builder(this).setTitle(g.optString("title")).setItems(opts,(d,w)->{
            if(w==0){EditText input=field("Tambahan progres");new AlertDialog.Builder(this).setTitle("Tambah progres").setView(input).setNegativeButton("Batal",null).setPositiveButton("Simpan",(dd,ww)->{
                try{int n=Integer.parseInt(input.getText().toString().trim());if(n<0)throw new Exception();g.put("progress",Math.min(g.optInt("target",1),g.optInt("progress",0)+n));saveData();render();}catch(Exception e){toast("Masukkan angka progres yang valid.");}
            }).show();}
            else if(w==1){showEditGoal(g);}
            else new AlertDialog.Builder(this).setMessage("Hapus target ini?").setNegativeButton("Batal",null).setPositiveButton("Hapus",(dd,ww)->{goals.remove(g);saveData();render();}).show();
        }).show();
    }
    private void showEditGoal(JSONObject g) {
        LinearLayout f=new LinearLayout(this);f.setOrientation(LinearLayout.VERTICAL);f.setPadding(dp(8),0,dp(8),0);
        EditText t=field("Nama target"),d=field("Deskripsi"),n=field("Target angka");t.setText(g.optString("title"));d.setText(g.optString("desc"));n.setText(""+g.optInt("target",1));f.addView(t);f.addView(d);f.addView(n);
        new AlertDialog.Builder(this).setTitle("Ubah target").setView(f).setNegativeButton("Batal",null).setPositiveButton("Simpan",(x,y)->{try{int target=Integer.parseInt(n.getText().toString());if(t.getText().toString().trim().isEmpty()||target<=0)throw new Exception();g.put("title",t.getText().toString().trim());g.put("desc",d.getText().toString().trim());g.put("target",target);g.put("progress",Math.min(target,g.optInt("progress",0)));saveData();render();}catch(Exception e){toast("Periksa nama dan target.");}}).show();
    }

    private void pickDate() {
        DatePickerDialog d=new DatePickerDialog(this,(v,y,m,day)->{selectedDate=LocalDate.of(y,m+1,day);activeTab="agenda";render();},selectedDate.getYear(),selectedDate.getMonthValue()-1,selectedDate.getDayOfMonth());d.show();
    }

    private void schedule(JSONObject o) {
        cancelAlarm(o);
        if(!o.optBoolean("alarm",true))return;
        try {
            LocalDate date=LocalDate.parse(o.getString("date"));LocalTime time=LocalTime.parse(o.getString("time"));
            long at=LocalDateTime.of(date,time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            if(at<System.currentTimeMillis())return;
            AlarmManager am=(AlarmManager)getSystemService(ALARM_SERVICE);
            if(Build.VERSION.SDK_INT>=31 && !am.canScheduleExactAlarms()) {
                try { Intent i=new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:"+getPackageName())); startActivity(i); } catch(Exception ignored){}
            }
            Intent intent=new Intent(this,ReminderReceiver.class);intent.putExtra("title",o.optString("title","Pengingat kegiatan"));
            String note=o.optString("note","Waktunya menjalankan kegiatanmu.");if(note.isEmpty())note="Waktunya menjalankan kegiatanmu.";intent.putExtra("note",note);intent.putExtra("reminderId",o.optInt("id",1));
            PendingIntent pi=PendingIntent.getBroadcast(this,o.optInt("id",1),intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            if(Build.VERSION.SDK_INT>=23)am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pi);else am.setExact(AlarmManager.RTC_WAKEUP,at,pi);
        } catch(Exception e){toast("Kegiatan tersimpan, tetapi alarm belum bisa dijadwalkan.");}
    }
    private void cancelAlarm(JSONObject o) {
        try{Intent i=new Intent(this,ReminderReceiver.class);PendingIntent pi=PendingIntent.getBroadcast(this,o.optInt("id",1),i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);((AlarmManager)getSystemService(ALARM_SERVICE)).cancel(pi);}catch(Exception ignored){}
    }
    private void rescheduleUpcoming() {
        boolean needs=prefs.getBoolean("needs_reschedule",false);
        if(needs){for(JSONObject o:activities)schedule(o);prefs.edit().putBoolean("needs_reschedule",false).apply();}
    }
    private int nextId(){int max=0;for(JSONObject o:activities)max=Math.max(max,o.optInt("id",0));return max+1;}

    private void card(LinearLayout parent,int color){LinearLayout c=panel();c.setBackground(round(color,20));c.setPadding(dp(17),dp(17),dp(17),dp(17));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,0,0,dp(14));parent.addView(c,p);}
    private LinearLayout panel(){LinearLayout p=new LinearLayout(this);p.setOrientation(LinearLayout.VERTICAL);p.setPadding(dp(14),dp(13),dp(14),dp(13));p.setBackground(round(0xFFFFFFFF,18));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,dp(10));p.setElevation(dp(1));return p;}
    private LinearLayout emptyPanel(String title,String hint){LinearLayout p=panel();p.setGravity(Gravity.CENTER);p.setPadding(dp(20),dp(24),dp(20),dp(24));p.addView(text("○",28,BLUE,true));TextView t=text(title,15,INK,true);t.setGravity(Gravity.CENTER);p.addView(t);TextView h=text(hint,12,MUTED,false);h.setGravity(Gravity.CENTER);p.addView(h);return p;}
    private TextView text(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private EditText field(String hint){EditText e=new EditText(this);e.setSingleLine(true);e.setTextSize(14);e.setHint(hint);e.setTextColor(INK);e.setHintTextColor(MUTED);e.setPadding(dp(10),dp(8),dp(10),dp(8));return e;}
    private Button solidButton(String label,int color){Button b=new Button(this);b.setText(label);b.setTextColor(0xFFFFFFFF);b.setAllCaps(false);b.setTextSize(14);b.setBackground(round(color,14));return b;}
    private Button softButton(String label,int bg,int fg){Button b=new Button(this);b.setText(label);b.setTextColor(fg);b.setAllCaps(false);b.setTextSize(13);b.setBackground(round(bg,14));return b;}
    private GradientDrawable round(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private View space(int h){View v=new View(this);v.setLayoutParams(new LinearLayout.LayoutParams(1,dp(h)));return v;}
    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
    private String rupiah(long value){return NumberFormat.getCurrencyInstance(new Locale("id","ID")).format(value).replace(",00","");}
    private void toast(String msg){Toast.makeText(this,msg,Toast.LENGTH_LONG).show();}
}
