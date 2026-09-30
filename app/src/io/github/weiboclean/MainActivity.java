package io.github.weiboclean;

import android.app.*;
import android.content.*;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.json.*;

public final class MainActivity extends Activity {
 private LinearLayout root,body,tabs;
 private ScrollView contentScroll;
 private final Handler main=new Handler(Looper.getMainLooper());
 private int page=0,ink,muted,bg,surface,accent=0xff087e70;
 private boolean resumed=false;
 private String lastReport="";
 private final Runnable refresh=new Runnable(){public void run(){if(!resumed)return;if(page==2){String r=report();if(!r.equals(lastReport)){lastReport=r;int y=contentScroll==null?0:contentScroll.getScrollY();render();contentScroll.post(()->contentScroll.scrollTo(0,y));}}main.postDelayed(this,1500);}};
 protected void onCreate(Bundle b){super.onCreate(b);page=b==null?0:b.getInt("page",0);Config.grant(this);render();}
 protected void onSaveInstanceState(Bundle b){super.onSaveInstanceState(b);b.putInt("page",page);}
 protected void onResume(){super.onResume();resumed=true;Config.grant(this);render();main.post(refresh);}
 protected void onPause(){super.onPause();resumed=false;main.removeCallbacks(refresh);}
 private int d(int n){return (int)(getResources().getDisplayMetrics().density*n+.5f);}
 private TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setLineSpacing(d(3),1);return t;}
 private GradientDrawable shape(int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(d(radius));return g;}
 private LinearLayout box(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);l.setPadding(d(18),d(12),d(18),d(12));l.setBackground(shape(surface,18));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,d(8),0,d(8));body.addView(l,p);return l;}
 private void section(String s){TextView t=text(s,14,accent);t.setTypeface(null,Typeface.BOLD);t.setPadding(d(3),d(18),0,d(3));body.addView(t);}
 private Button button(String label,Runnable action){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextColor(accent);b.setOnClickListener(v->action.run());return b;}
 private void render(){
  boolean dark=(getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES;
  bg=dark?0xff101818:0xfff3f7f6;surface=dark?0xff1d2828:Color.WHITE;ink=dark?0xffe7efed:0xff172d29;muted=dark?0xffa6bab5:0xff637771;
  getWindow().setStatusBarColor(bg);getWindow().setNavigationBarColor(bg);
  getWindow().getDecorView().setSystemUiVisibility(dark?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
  root=new LinearLayout(this);root.setOrientation(1);root.setPadding(d(20),d(16),d(20),d(8));root.setBackgroundColor(bg);setContentView(root);
  TextView title=text("微博清简",29,ink);title.setTypeface(null,Typeface.BOLD);root.addView(title);root.addView(text("独立模块 · 0.3.0 实验版",13,muted));
  tabs=new LinearLayout(this);tabs.setPadding(0,d(14),0,d(8));String[] names={"广告净化","页面精简","适配诊断"};
  for(int i=0;i<3;i++){final int target=i;TextView tab=text(names[i],15,i==page?Color.WHITE:muted);tab.setGravity(Gravity.CENTER);tab.setPadding(0,d(12),0,d(12));tab.setBackground(shape(i==page?accent:surface,14));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.setMargins(i==0?0:d(5),0,0,0);tabs.addView(tab,lp);tab.setOnClickListener(v->{page=target;render();});}root.addView(tabs);
  ScrollView scroll=new ScrollView(this);contentScroll=scroll;scroll.setFillViewport(true);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));body=new LinearLayout(this);body.setOrientation(1);scroll.addView(body);
  if(page==2){diagnostics();return;}
  LinearLayout intro=box();intro.addView(text(page==0?"让内容回到前面":"留下你常用的入口",22,ink));intro.addView(text(page==0?"在 LSPosed 启用本模块并勾选微博。首次使用后，到适配诊断查看实际命中。":"每项独立控制，默认保留全部入口。首页和「我」始终保留。",14,muted));
  if(page==0){section("广告拦截");for(int i=0;i<4;i++)toggle(i);for(String key:new String[]{"flow_ads","carousel_ads","mine_ads","mine_vip_ads","video_preroll_ads","video_overlay_ads","redpacket","push_notify"})toggle(key);
   section("内容与用户过滤");toggle("keywords");ruleEditor("keyword_rules","关键词规则","每行一个关键词，也可用 | 分隔。按文字包含匹配，不执行正则，避免复杂表达式拖慢滑动。");
   toggle("users");ruleEditor("user_rules","用户规则","每行一个用户名或 UID，精确匹配。不会取关、拉黑或删除微博。");
   toggle("comment_filter");toggle("comment_location");ruleEditor("location_rules","评论地区规则","每行一个地区，匹配评论公开显示的来源地区；未显示地区的评论保留。");
  }
  else{section("活动与卡片");toggle(4);toggle(5);section("底部导航");for(int i=6;i<10;i++)toggle(i);
   section("信息流细节");for(String k:new String[]{"decor_background","decor_avatar","decor_keywords","follow_recommend","feed_trends","feed_headers"})toggle(k);
   section("我的页面");for(String k:Enhancements.CARD_KEYS)if(!k.equals("mine_ads"))toggle(k);
   section("图片与隐私");for(String k:new String[]{"original_images","clipboard_guard","copy_clean"})toggle(k);
  }
  LinearLayout note=box();note.addView(text("修改后重启微博生效",16,ink));note.addView(text("如果同时使用其他微博模块，请只让一个模块处理同一功能，避免重复过滤。",13,muted));note.addView(button("重启微博",this::restart));note.addView(button("打开微博",this::launch));
  TextView footer=text("当前以 16.9.0 Play 版做验证；其他版本逐项检测，未识别的功能自动跳过。",12,muted);footer.setPadding(d(4),d(10),d(4),d(20));body.addView(footer);
 }
 private void toggle(int i){LinearLayout card=box();Switch s=new Switch(this);s.setText(Config.TITLES[i]);s.setTextSize(16);s.setTextColor(ink);s.setPadding(0,d(6),0,d(8));s.setChecked(Config.prefs(this).getBoolean(Config.KEYS[i],Config.defaultOn(Config.KEYS[i])));card.addView(s);card.addView(text(Config.DETAILS[i],12,muted));s.setOnCheckedChangeListener((b,value)->{Config.prefs(this).edit().putBoolean(Config.KEYS[i],value).putLong("revision",System.currentTimeMillis()).apply();Config.grant(this);});}
 private void toggle(String key){for(int i=0;i<Config.KEYS.length;i++)if(Config.KEYS[i].equals(key)){toggle(i);return;}}
 private void ruleEditor(String key,String title,String help){
  LinearLayout row=box();String saved=Config.prefs(this).getString(key,"");row.addView(text(title+" · "+TextRules.parse(saved).size()+" 项",15,ink));row.addView(text(help,12,muted));
  row.addView(button("编辑"+title,()->{
   EditText edit=new EditText(this);edit.setTextColor(Color.BLACK);edit.setGravity(Gravity.TOP);edit.setMinLines(4);edit.setMaxLines(8);edit.setPadding(d(20),d(12),d(20),d(12));edit.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);edit.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(TextRules.MAX_INPUT)});edit.setText(Config.prefs(this).getString(key,""));
   AlertDialog dialog=new AlertDialog.Builder(this).setTitle(title).setMessage(help+"\n每组最多 100 项，每项最多 100 字。保存后重启微博。").setView(edit).setNegativeButton("取消",null).setPositiveButton("保存",null).create();
   dialog.setOnShowListener(v->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(b->{try{String value=edit.getText().toString();TextRules.parse(value);Config.prefs(this).edit().putString(key,value).putLong("revision",System.currentTimeMillis()).apply();dialog.dismiss();render();}catch(IllegalArgumentException e){edit.setError(e.getMessage());}}));dialog.show();
  }));
 }
 private String report(){return getSharedPreferences("diagnostics",0).getString("json","");}
 private void diagnostics(){
  String raw=report();lastReport=raw;LinearLayout head=box();
  if(raw.isEmpty()){head.addView(text("等待微博首次运行",22,ink));head.addView(text("启用模块并打开微博后，这里会显示每项检测结果。不会把尚未检测的项目算作成功。",14,muted));head.addView(button("打开微博",this::launch));}
  else try{
   JSONObject report=new JSONObject(raw);int progress=report.optInt("progress"),total=report.optInt("total",Config.KEYS.length);
   head.addView(text(progress==total?"兼容检查已完成":"正在读取功能入口",22,ink));
   ProgressBar bar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);bar.setMax(total);bar.setProgress(progress);head.addView(bar,new LinearLayout.LayoutParams(-1,d(18)));head.addView(text(progress+" / "+total+" 项 · 微博 "+report.optString("version"),13,muted));
   head.addView(text("检测耗时 "+report.optLong("installMs")+" ms · "+report.optInt("hooks")+" 个挂接入口",12,muted));
   head.addView(text("读取来源："+report.optString("source"),12,muted));
   if(report.optLong("revision")!=Config.prefs(this).getLong("revision",0))head.addView(text("设置已修改，重启微博后再核对结果。",14,0xffbd7020));
   head.addView(text("记录时间："+new java.text.SimpleDateFormat("MM-dd HH:mm:ss",java.util.Locale.CHINA).format(new java.util.Date(report.optLong("time"))),12,muted));
   JSONArray rows=report.optJSONArray("rows");int observed=0,ready=0,missing=0,disabled=0,errors=0;
   if(rows!=null)for(int i=0;i<rows.length();i++){JSONObject r=rows.getJSONObject(i);String state=r.optString("state");if(state.equals("disabled"))disabled++;else if(state.equals("missing"))missing++;else if(state.equals("error"))errors++;else if(r.optLong("hits")>0)observed++;else if(state.equals("hooked"))ready++;}
   head.addView(text("已命中 "+observed+" · 待触发 "+ready+" · 未适配 "+missing+" · 异常 "+errors+" · 关闭 "+disabled,13,ink));
   section("逐项结果");
   if(rows!=null)for(int i=0;i<rows.length();i++){JSONObject r=rows.getJSONObject(i);String state=r.optString("state");long hits=r.optLong("hits");String status=state.equals("disabled")?"已关闭":state.equals("missing")?"未适配":state.equals("error")?"运行异常":hits>0?"已命中 "+hits+" 次":state.equals("hooked")?"已挂接 · 待触发":"检测中";
    LinearLayout row=box();row.addView(text(Config.title(r.optString("key")),16,ink));row.addView(text(status,14,state.equals("error")||state.equals("missing")?0xffbd7020:accent));row.addView(text(r.optString("detail"),12,muted));}
  }catch(Exception e){head.addView(text("诊断记录无法读取，请重启微博重新生成。",15,ink));}
  LinearLayout actions=box();actions.addView(text("挂接成功表示入口已找到；命中表示规则被调用，最终效果仍需结合页面确认。",13,muted));actions.addView(button("重启微博并重新检查",this::restart));actions.addView(button("复制诊断日志",()->{((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("微博清简诊断",report()));Toast.makeText(this,"已复制；不包含微博内容或账号凭据",Toast.LENGTH_SHORT).show();}));
  section("配置与功能对照");LinearLayout roadmap=box();roadmap.addView(text("广告相关规则默认开启，其他规则默认关闭；已保存的开关按你的选择保留。",14,ink));
  roadmap.addView(button("恢复默认设置",()->new AlertDialog.Builder(this).setTitle("恢复默认设置？").setMessage("清空本模块的过滤规则并恢复默认开关，不改微博账号数据。重启微博后生效。").setNegativeButton("取消",null).setPositiveButton("恢复默认",(dialog,which)->{Config.prefs(this).edit().clear().putLong("revision",System.currentTimeMillis()).apply();render();}).show()));
  roadmap.addView(button("导出设置",()->SettingsBackup.export(this)));roadmap.addView(button("导入设置",()->SettingsBackup.importFile(this)));
  roadmap.addView(button("查看猪手功能对照",()->{try{int id=getResources().getIdentifier("feature_inventory","raw",getPackageName());java.io.InputStream in=getResources().openRawResource(id);java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1)out.write(b,0,n);in.close();new AlertDialog.Builder(this).setTitle("功能对照 · 不等同于实测通过").setMessage(out.toString("UTF-8")).setPositiveButton("关闭",null).show();}catch(Exception e){Toast.makeText(this,"功能对照文件暂不可用",Toast.LENGTH_LONG).show();}}));
 }
 protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(result==RESULT_OK&&data!=null&&data.getData()!=null)SettingsBackup.handle(this,request,data.getData(),this::render);}
 private void launch(){try{Intent intent=getPackageManager().getLaunchIntentForPackage(Config.HOST);if(intent==null)throw new IllegalStateException();startActivity(intent);}catch(Exception e){new AlertDialog.Builder(this).setMessage("未找到微博，请先安装微博。").setPositiveButton("知道了",null).show();}}
 private void restart(){
  ProgressDialog p=new ProgressDialog(this);p.setMessage("正在重启微博…");p.setCancelable(false);p.show();
  new Thread(()->{String error=null;try{
   java.lang.Process process=new ProcessBuilder("su","-c","am force-stop --user 0 com.sina.weibo; am start --user 0 -n com.sina.weibo/com.sina.weibo.SplashActivity").redirectErrorStream(true).start();
   if(!process.waitFor(10,java.util.concurrent.TimeUnit.SECONDS)){process.destroy();throw new IllegalStateException("Root 操作超时");}if(process.exitValue()!=0)throw new IllegalStateException("未获得 Root 权限");
  }catch(Exception e){error=e instanceof java.io.IOException?"当前环境未向模块开放 Root 命令":e.getMessage();}final String problem=error;main.post(()->{if(isFinishing())return;p.dismiss();if(problem!=null)new AlertDialog.Builder(this).setTitle("需要手动重启微博").setMessage(problem+"。打开系统设置，点「强行停止」，然后重新打开微博。模块开关已经保存。").setPositiveButton("打开系统设置",(dialog,which)->startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,android.net.Uri.parse("package:"+Config.HOST)))).setNegativeButton("稍后",null).show();});},"WeiboClean-restart").start();
 }
}

