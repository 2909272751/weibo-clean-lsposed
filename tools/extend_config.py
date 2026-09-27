from pathlib import Path
root=Path(__file__).resolve().parents[1]
path=root/'app/src/io/github/weiboclean/Config.java'
s=path.read_text(encoding='utf-8')
if '"mine_recommend"' in s:
    raise SystemExit('Config already extended; no changes made')
features=[
 ('keywords','按关键词过滤微博','一行一个词，匹配正文；仅影响已适配列表，不删除内容'),
 ('users','按用户过滤微博','用户名或 UID，一行一项，精确匹配'),
 ('comment_filter','评论内容与用户过滤','使用同一组关键词、用户规则；未支持的评论列表会跳过'),
 ('comment_location','按地区过滤评论','匹配评论显示的发布地区；一行一个地区'),
 ('decor_background','隐藏博文右上角装饰','移除博文背景装饰，保留正文与图片'),
 ('decor_avatar','隐藏头像装饰','隐藏头像圈与扩展装饰，保留头像'),
 ('decor_keywords','移除关键词推广链接','移除正文关键词结构，不改普通网页链接'),
 ('follow_recommend','隐藏关注推荐卡片','移除博文附带的推荐关注模块'),
 ('feed_trends','隐藏信息流趋势推荐','移除信息流 trends 推荐项'),
 ('feed_headers','隐藏首页顶部推荐区','移除信息流 headers 区，可能包含直播和普通推荐'),
 ('original_images','大图优先读取原图','大图地址可用时替换为原图，可能增加流量；不修改水印'),
 ('clipboard_guard','禁止微博读取剪贴板','包含主动粘贴也会被阻止，需要粘贴时请关闭并重启'),
 ('copy_clean','复制时清理评论标记','复制文本时移除 [cp] 与 [/cp]，保留其余文字'),
 ('mine_ads','隐藏我的页面广告卡片','按已识别卡片标识过滤图片和活动广告'),
 ('mine_wallet','隐藏我的钱包卡片','仅隐藏独立钱包卡片；顶部网格入口可能仍保留'),
 ('mine_tasks','隐藏我的任务卡片','隐藏任务中心和红包活动卡片'),
 ('mine_creator','隐藏创作者中心卡片','仅隐藏独立卡片，不影响账号功能'),
 ('mine_recommend','隐藏我的页面用户推荐','过滤明确标识的用户推荐卡片'),
]
import json,re
for name,index in [('KEYS',0),('TITLES',1),('DETAILS',2)]:
 pattern=r'(static final String\[\] '+name+r'=\{)(.*?)(\};)'
 m=re.search(pattern,s)
 if 'keywords' not in re.search(r'static final String\[\] KEYS=\{(.*?)\};',s).group(1) or name!='KEYS':
  vals=','.join(json.dumps(row[index],ensure_ascii=False) for row in features)
  s=s[:m.start()]+m.group(1)+m.group(2)+','+vals+m.group(3)+s[m.end():]
path.write_text(s,encoding='utf-8')
