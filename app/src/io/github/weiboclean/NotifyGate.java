package io.github.weiboclean;

import android.app.Notification;
import android.app.NotificationChannel;
import android.os.Bundle;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 推送通知广告闸门。
 *
 * <p>挂在 {@code NotificationManager} 上：{@code notify(...)} 是微博进程内所有通知的唯一出口
 * （厂商推送通道、自建长连接、轮询拉回来的推广最终都要调它），而它是平台类、不参与 R8 混淆，
 * 所以微博换版本改名的是它自己的类，这里不受影响。
 *
 * <p>判定顺序（先便宜后昂贵，命中即停）：渠道 id → 渠道名/描述 → 标题/正文/长文/副标题/
 * 附加文本/滚动文本/tag。
 *
 * <p><b>误伤是这里最贵的错误</b>：微博的私信、@我、评论、关注、点赞都是正常通知，词表刻意
 * 避开这些词；任何异常一律 fail-open 放行，行为与没装模块一致。
 *
 * <p><b>性能</b>：{@code notify} 按小时计，不是 {@code onDraw} 那种热路径；拦截体内只做
 * {@code String.indexOf} 和取已有对象，零反射、零逐条日志。
 */
final class NotifyGate {

 private NotifyGate(){}

 /** 判定结果；{@code suppress=false} 表示放行。 */
 static final class Decision{
  final boolean suppress;final String reason;
  private Decision(boolean suppress,String reason){this.suppress=suppress;this.reason=reason;}
  static final Decision KEEP=new Decision(false,null);
 }

 // 渠道 id 里的广告标记
 private static final String[] CHANNEL_TOKENS={"_ad","ad_","ads_","ad.",".ad","promo","marketing","advert","push_ad","gdt","gromore"};
 // 渠道名/描述里的广告字样
 private static final String[] CHANNEL_NAME_TOKENS={"广告","促销","推广","营销"};
 /**
  * 通知文案广告词。刻意避开微博正常通知用词：私信、@我、评论、关注、点赞、转发、涨粉、热门。
  * 「会员/特权/绿钻」这类是微博的付费推广话术，属于要拦的那一类。
  */
 private static final String[] TEXT_TOKENS={
  "广告","推广","营销","促销",
  "领券","领红包","优惠券","红包雨","抽奖","红包",
  "限时","折扣","优惠","秒杀","特价","免费领","免费领会员",
  "立即领取","点击领取","马上领","限时领取","领取奖励","领取礼包",
  "助力","邀请好友","拉新","邀请返","分享得","砍价","助力得",
  "福利","活动","特惠","超值","福利中心",
  "会员","特权","超级会员","绿钻","开通会员","会员促销",
 };

 private static final AtomicLong scanned=new AtomicLong(),suppressed=new AtomicLong(),
         channelsSeen=new AtomicLong(),channelsAd=new AtomicLong();

 static long scanned(){return scanned.get();}
 static long suppressed(){return suppressed.get();}
 static long channelsSeen(){return channelsSeen.get();}
 static long channelsAd(){return channelsAd.get();}
 static String stats(){return "已拦广告通知 "+suppressed.get()+" 条 / 共见到 "+scanned.get()
         +" 条；广告渠道 "+channelsAd.get()+"/"+channelsSeen.get();}

 /** 判定一条即将下发的通知。fail-open：任何异常都返回放行。 */
 static Decision evaluate(Notification n,String tag){
  if(n==null)return Decision.KEEP;
  try{
   scanned.incrementAndGet();
   String channelId=null;try{channelId=n.getChannelId();}catch(Throwable ignored){}
   Decision d=matchTokens(channelId,CHANNEL_TOKENS);
   if(d!=null)return d;
   // Notification 没有 getExtras()，extras 是 public 字段；取已有 Bundle，不分配。
   Bundle extras=null;try{extras=n.extras;}catch(Throwable ignored){}
   if(extras!=null){
    d=matchExtra(extras,Notification.EXTRA_TITLE);
    if(d==null)d=matchExtra(extras,Notification.EXTRA_TEXT);
    if(d==null)d=matchExtra(extras,Notification.EXTRA_BIG_TEXT);
    if(d==null)d=matchExtra(extras,Notification.EXTRA_SUB_TEXT);
    if(d==null)d=matchExtra(extras,Notification.EXTRA_INFO_TEXT);
   }
   if(d==null){CharSequence ticker=null;try{ticker=n.tickerText;}catch(Throwable ignored){}d=match(ticker);}
   if(d==null)d=match(tag);
   if(d==null)return Decision.KEEP;
   suppressed.incrementAndGet();
   return d;
  }catch(Throwable t){return Decision.KEEP;}
 }

 /** 渠道注册时判定一次：渠道 id/名/描述创建后固定，是比文案更强的信号。只观测不拦截。 */
 static Decision evaluateChannel(NotificationChannel ch){
  if(ch==null)return Decision.KEEP;
  try{
   channelsSeen.incrementAndGet();
   Decision d=matchTokens(ch.getId(),CHANNEL_TOKENS);
   if(d==null)d=match(ch.getName());
   if(d==null)d=match(ch.getDescription());
   if(d==null)return Decision.KEEP;
   channelsAd.incrementAndGet();
   return d;
  }catch(Throwable t){return Decision.KEEP;}
 }

 private static Decision matchExtra(Bundle extras,String key){
  CharSequence value;try{value=extras.getCharSequence(key);}catch(Throwable t){return null;}
  return match(value);
 }

 private static Decision match(CharSequence value){
  if(value==null)return null;
  Decision d=matchTokens(value,TEXT_TOKENS);
  return d!=null?d:matchTokens(value,CHANNEL_NAME_TOKENS);
 }

 /** 收 CharSequence：NotificationChannel.getName()/getDescription() 返回 CharSequence。 */
 private static Decision matchTokens(CharSequence haystack,String[] tokens){
  if(haystack==null)return null;
  // String.toString() 返回 this，不会产生新对象，所以这里没有额外分配。
  String s=haystack.toString();
  if(s.isEmpty())return null;
  for(int i=0;i<tokens.length;i++)if(s.indexOf(tokens[i])>=0)return new Decision(true,tokens[i]);
  return null;
 }

 // 没有真机广告通知时，用固定样本证明「判定函数本身」是对的，而不是只说「钩子装上了」。
 private static final String[][] SELF_TEST={
  {"热门话题：xxx 上了热搜","0"},
  {"@你 发表了新微博","0"},
  {"xxx 评论了你的微博","0"},
  {"你关注的 yyy 发布了新微博","0"},
  {"周年庆活动，限时 5 折开通会员","1"},
  {"点击领取 30 元红包，先到先得","1"},
  {"邀请好友助力得会员","1"},
  {"你有一条新私信","0"},
 };

 /** @return 形如 "self_test 6/8 passed [失败输入...]" 的自检结论。 */
 static String selfTest(){
  int passed=0;StringBuilder failed=new StringBuilder();
  for(String[] sample:SELF_TEST){
   boolean got=match(sample[0])!=null, want="1".equals(sample[1]);
   if(got==want)passed++;
   else{if(failed.length()>0)failed.append(" | ");failed.append(sample[0]).append("=>").append(got?"拦":"放");}
  }
  return "self_test "+passed+"/"+SELF_TEST.length+" passed"+(failed.length()==0?"":"["+failed+"]");
 }
}
