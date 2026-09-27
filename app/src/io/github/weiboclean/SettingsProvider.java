package io.github.weiboclean;

import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.*;

/** Only the module and scoped host may read settings or submit diagnostics. No account data. */
public final class SettingsProvider extends ContentProvider {
 public boolean onCreate(){return true;}
 private boolean allowed(){
  if(Binder.getCallingUid()==android.os.Process.myUid())return true;
  String[] packages=getContext().getPackageManager().getPackagesForUid(Binder.getCallingUid());
  if(packages!=null)for(String p:packages)if(Config.HOST.equals(p))return true;
  return false;
 }
 public Bundle call(String method,String arg,Bundle extras){
  if(!allowed())throw new SecurityException("Caller outside module scope");
  if("settings".equals(method))return Config.bundle(getContext());
  if("report".equals(method)&&extras!=null){
   String report=extras.getString("json","");
   if(report.length()>32000)throw new IllegalArgumentException("Report too large");
   getContext().getSharedPreferences("diagnostics",0).edit().putString("json",report).apply();
   return Bundle.EMPTY;
  }
  throw new IllegalArgumentException("Unknown operation");
 }
 public Cursor query(Uri u,String[]p,String s,String[]a,String o){return null;}
 public String getType(Uri u){return "application/json";}
 public Uri insert(Uri u,ContentValues v){throw new UnsupportedOperationException();}
 public int delete(Uri u,String s,String[]a){throw new UnsupportedOperationException();}
 public int update(Uri u,ContentValues v,String s,String[]a){throw new UnsupportedOperationException();}
}
