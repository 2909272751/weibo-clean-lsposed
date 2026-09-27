package io.github.weiboclean;
import java.util.*;
public final class TextRulesTest {
 private static int checks;
 static void check(boolean ok,String name){checks++;if(!ok)throw new AssertionError(name);}
 static void rejects(String input){try{TextRules.parse(input);throw new AssertionError("limit accepted");}catch(IllegalArgumentException expected){checks++;}}
 public static void main(String[] args){
  List<String> rules=TextRules.parse("音乐\n Java |java\r\n \n");
  check(rules.size()==2,"deduplicate blank and mixed separators");
  check(TextRules.contains(rules,"测试JAVA代码"),"case-independent literal match");
  check(!TextRules.contains(rules,"普通内容"),"unrelated post preserved");
  check(!TextRules.contains(rules,null),"missing text preserved");
  check(TextRules.exact(TextRules.parse("张三\n123456"),"123456"),"UID exact match");
  check(!TextRules.exact(TextRules.parse("张三"),"张三丰"),"similar username preserved");
  check(TextRules.contains(TextRules.parse("(a+)+$"),"literal (a+)+$"),"regex metacharacters treated literally");
  check(!TextRules.contains(TextRules.parse("(a+)+$"),"aaaaaaaa"),"no regex execution");
  check(TextRules.parse(null).isEmpty(),"missing configuration safe");
  check(TextRules.cleanCopy("[cp]你好[/cp] https://example.org").equals("你好 https://example.org"),"copy keeps remaining content");
  rejects(String.join("",Collections.nCopies(101,"a")));
  StringBuilder many=new StringBuilder();for(int i=0;i<101;i++)many.append(i).append('\n');rejects(many.toString());
  rejects(String.join("",Collections.nCopies(10001,"a")));
  check(TextRules.parse("a|a|a").size()==1,"duplicates do not consume term limit");
  System.out.println("PASS "+checks+" text rule checks");
 }
}
