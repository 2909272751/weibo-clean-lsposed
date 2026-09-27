package io.github.weiboclean;
import org.json.*;
public final class BackupTest {
 private static int checks;
 static void check(boolean ok,String name){checks++;if(!ok)throw new AssertionError(name);}
 static void rejects(String raw)throws Exception{try{SettingsBackup.validate(raw);throw new AssertionError("invalid backup accepted");}catch(JSONException|IllegalArgumentException expected){checks++;}}
 public static void main(String[] args)throws Exception{
  JSONObject valid=SettingsBackup.validate("{\"format\":\"weiboclean-settings-1\",\"settings\":{\"feed\":false,\"keyword_rules\":\"音乐\\nabc\",\"unknown_future_option\":42}}");
  check(!valid.getBoolean("feed"),"boolean retained");check(valid.getString("keyword_rules").contains("音乐"),"Unicode rules preserved");
  check(SettingsBackup.validate("{\"format\":\"weiboclean-settings-1\",\"settings\":{}}").length()==0,"partial older backup allowed");
  rejects("{}");rejects("{\"format\":\"other\",\"settings\":{}}");
  rejects("{\"format\":\"weiboclean-settings-1\",\"settings\":{\"feed\":\"true\"}}");
  rejects("{\"format\":\"weiboclean-settings-1\",\"settings\":{\"keyword_rules\":null}}");
  rejects("not JSON");
  check(Config.KEYS.length==Config.TITLES.length&&Config.KEYS.length==Config.DETAILS.length,"settings labels aligned");
  check(new java.util.HashSet<>(java.util.Arrays.asList(Config.KEYS)).size()==Config.KEYS.length,"settings keys unique");
  System.out.println("PASS "+checks+" backup and configuration checks");
 }
}
