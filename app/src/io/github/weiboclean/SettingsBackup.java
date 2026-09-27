package io.github.weiboclean;

import android.app.*;
import android.content.*;
import android.net.Uri;
import java.io.*;
import java.nio.charset.StandardCharsets;
import org.json.*;

final class SettingsBackup {
 private static final int EXPORT=210,IMPORT=211;
 static void export(Activity a){Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/json").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE,"weiboclean-settings.json");open(a,i,EXPORT);}
 static void importFile(Activity a){open(a,new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("application/json").addCategory(Intent.CATEGORY_OPENABLE),IMPORT);}
 private static void open(Activity a,Intent intent,int request){try{a.startActivityForResult(intent,request);}catch(ActivityNotFoundException e){new AlertDialog.Builder(a).setTitle("文件选择器不可用").setMessage("系统没有可用的文件选择器，请启用系统文件应用后重试。当前设置未修改。").setPositiveButton("知道了",null).show();}}
 static JSONObject encode(Context c)throws JSONException{JSONObject j=new JSONObject();j.put("format","weiboclean-settings-1");JSONObject values=new JSONObject();for(String k:Config.KEYS)values.put(k,Config.prefs(c).getBoolean(k,Config.defaultOn(k)));for(String k:Config.TEXT_KEYS)values.put(k,Config.prefs(c).getString(k,""));j.put("settings",values);return j;}
 static JSONObject validate(String raw)throws JSONException{
  if(raw.length()>65536)throw new IllegalArgumentException("配置文件超过 64 KB");JSONObject j=new JSONObject(raw);
  if(!"weiboclean-settings-1".equals(j.optString("format")))throw new IllegalArgumentException("不是微博清简配置文件");JSONObject values=j.getJSONObject("settings");
  for(String k:Config.KEYS)if(values.has(k)&&!(values.get(k) instanceof Boolean))throw new IllegalArgumentException("开关格式错误："+k);
  for(String k:Config.TEXT_KEYS)if(values.has(k)){if(!(values.get(k) instanceof String))throw new IllegalArgumentException("规则格式错误");TextRules.parse(values.getString(k));}
  return values;
 }
 static void handle(Activity a,int request,Uri uri,Runnable refresh){
  if(request!=EXPORT&&request!=IMPORT)return;ProgressDialog dialog=new ProgressDialog(a);dialog.setMessage(request==EXPORT?"正在导出设置…":"正在读取并校验配置…");dialog.setCancelable(false);dialog.show();
  new Thread(()->{String error=null;try{
   if(request==EXPORT){byte[] bytes=encode(a).toString(2).getBytes(StandardCharsets.UTF_8);try(OutputStream out=a.getContentResolver().openOutputStream(uri,"wt")){if(out==null)throw new IOException();out.write(bytes);}}
   else{ByteArrayOutputStream buffer=new ByteArrayOutputStream();try(InputStream in=a.getContentResolver().openInputStream(uri)){if(in==null)throw new IOException();byte[] bytes=new byte[4096];int n;while((n=in.read(bytes))!=-1){if(buffer.size()+n>65536)throw new IllegalArgumentException("配置文件超过 64 KB");buffer.write(bytes,0,n);}}
    JSONObject values=validate(new String(buffer.toByteArray(),StandardCharsets.UTF_8));android.content.SharedPreferences.Editor e=Config.prefs(a).edit();for(String k:Config.KEYS)if(values.has(k))e.putBoolean(k,values.getBoolean(k));for(String k:Config.TEXT_KEYS)if(values.has(k))e.putString(k,values.getString(k));if(!e.putLong("revision",System.currentTimeMillis()).commit())throw new IOException();
   }
  }catch(Exception e){error=e instanceof IllegalArgumentException?e.getMessage():"文件无法读取或写入，请检查格式与存储权限";}
   final String issue=error;a.runOnUiThread(()->{if(a.isFinishing())return;dialog.dismiss();refresh.run();new AlertDialog.Builder(a).setTitle(issue==null?"已完成":"操作失败").setMessage(issue!=null?issue:request==EXPORT?"设置已导出。包含你的过滤词，不包含账号凭据或诊断记录。":"设置已导入，重启微博后生效。").setPositiveButton("知道了",null).show();});
  },"WeiboClean-settings-file").start();
 }
}
