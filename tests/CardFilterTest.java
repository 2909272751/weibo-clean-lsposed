package io.github.weiboclean;
import org.json.*;
public final class CardFilterTest {
 static void check(boolean pass,String name){if(!pass)throw new AssertionError(name);}
 public static void main(String[] args)throws Exception{
  JSONObject source=new JSONObject("{\"since_id\":\"keep-cursor\",\"cards\":[{\"title\":\"谈谈广告行业\",\"mblog\":{\"mblogtype\":0}},{\"is_ad\":1},{\"card_group\":[{\"title\":\"普通内容\"},{\"ad_tag\":{\"text\":\"广告\"}}]},null,7]}");
  JSONObject copy=new JSONObject(source.toString());
  check(CardFilter.clean(copy,0)==2,"nested explicit ads removed");
  check(copy.getJSONArray("cards").length()==4,"organic and unknown data retained");
  check(copy.getJSONArray("cards").getJSONObject(1).getJSONArray("card_group").length()==1,"nested group retained");
  check(copy.getString("since_id").equals("keep-cursor"),"pagination untouched");
  check(source.getJSONArray("cards").length()==5,"caller source untouched");
  check(CardFilter.clean(copy,0)==0,"idempotent");
  check(!CardFilter.advert(new JSONObject("{\"text\":\"广告\",\"ad_tag\":{\"text\":\"推荐\"},\"promotion\":{}}")),"no text-based false positive");
  check(CardFilter.clean(new JSONObject("{\"cards\":\"unexpected\",\"card_group\":null}"),0)==0,"unexpected schema retained");
  check(CardFilter.clean(new JSONObject("{\"cards\":[{\"is_ad\":true}]}"),6)==0,"depth bound");
  check(CardFilter.advert(new JSONObject("{\"is_ad_card\":1}")),"typed card ad marker");
  check(!CardFilter.advert(new JSONObject("{\"is_ad_card\":0,\"title\":\"普通推荐\"}")),"normal typed card retained");
  check(!CardFilter.advert(new JSONObject("{\"is_ad_card\":2}")),"unknown card marker retained");
  System.out.println("PASS 12 card filtering checks");
 }
}
