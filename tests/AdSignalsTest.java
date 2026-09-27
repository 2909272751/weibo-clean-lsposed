package io.github.weiboclean;
public final class AdSignalsTest {
 static int checks;
 static void check(boolean b,String name){checks++;if(!b)throw new AssertionError(name);}
 public static void main(String[] args){
  check(AdSignals.promotion("1","campaign"),"explicit campaign");
  check(AdSignals.promotion(" 2 ","42"),"numeric type");
  check(!AdSignals.promotion("0","42"),"ordinary promotion");
  check(!AdSignals.promotion("-1","42"),"invalid type");
  check(!AdSignals.promotion("unknown","42"),"unknown type retained");
  check(!AdSignals.promotion(null,"42"),"missing type");
  check(!AdSignals.promotion("1",null),"missing campaign");
  check(!AdSignals.promotion("1"," "),"empty campaign");
  check(!AdSignals.promotion("1","0"),"zero campaign");
  java.util.Set<String> ads=new java.util.HashSet<>(java.util.Arrays.asList(Config.AD_KEYS));
  for(String key:Config.KEYS)check(Config.defaultOn(key)==ads.contains(key),"default "+key);
  check(!Config.defaultOn("future_unknown"),"unknown feature stays off");
  System.out.println("PASS "+checks+" ad classification and defaults checks");
 }
}
