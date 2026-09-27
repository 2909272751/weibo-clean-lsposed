"""Extract menu labels only from a local reference APK analysis; no reference code is shipped."""
from pathlib import Path
import re

root=Path(__file__).resolve().parents[1]
reference=root.parent/'weibo-helper-assessment/module-253/readable/p037'
groups={
 '净化主菜单':['AlertDialogC0835.java'],
 '信息流细节':['C0827.java'],
 '发现页面':['C0828.java','C0829.java'],
 '个人中心':['C0830.java'],
 '搜索与热搜':['C0831.java','C0832.java'],
 '详情页面':['C0833.java'],
 '视频页面':['C0834.java'],
 '加强功能':['AlertDialogC0906.java'],
 '高级功能':['AlertDialogC0894.java'],
 '其他设置':['AlertDialogC0876.java'],
}
implemented={
 '去除信息流广告':'新版列表已实测命中，普通内容保留',
 '轮播卡片':'只过滤带广告角标的轮播项，开关对照通过；普通话题保留',
 '前5s视频广告':'已补广告片段列表规则；未遇到实际插播样本，不按时长裁剪正片',
 '视频广告':'已补独立广告片段、横幅及商品浮层规则；样本待验证',
 '广告视频':'已补独立广告片段规则；普通视频播放通过，广告样本待验证',
 '视频背景广告':'已补部分广告浮层字段；未覆盖所有背景样式',
 '开屏广告':'已有规则，需广告样本验证',
 '主界面底部Tab':'已实测隐藏与恢复',
 '信息流的广告':'新版列表已实测命中，普通内容保留',
 '浏览红包处理':'隐藏已实测；自动领取未实现',
 '头像装饰':'已实测移除头像圈；其他装饰待覆盖',
 '右上角装饰':'入口已命中，完整样式对照待验证',
 '关键词广告超链接':'已补独立规则，待实测',
 '默认原图':'原图替换入口已命中，完整看图待验证',
 '禁止读取粘贴板':'已补独立规则，待实测',
 '复制评论去除[cp]':'已补纯文本标记清理，待实测',
 '根据内容屏蔽':'已补文字包含匹配；正则未实现',
 '根据用户名屏蔽':'已补用户名/UID精确匹配，待实测',
 '开启屏蔽':'已拆分微博/评论开关，待实测',
 '地址过滤评论开关':'已补独立规则，待实测',
 '根据地址过滤评论':'已补地区编辑入口，待实测',
 '我的钱包':'已补独立卡片规则，网格入口未实现',
 '任务中心':'已补卡片规则，待当前页面验证',
 '创作者中心':'已补卡片规则，网格入口未实现',
 '让红包飞':'已补卡片规则，待当前页面验证',
 '活动节日广告卡片':'已补卡片规则，待当前页面验证',
 '图片广告卡片':'已补卡片规则，待当前页面验证',
 '为你推荐':'已补卡片规则，待当前页面验证',
 '关注页直播中用户':'顶部推荐区整体隐藏规则；细分未实现',
 '禁止点赞后增加内容':'部分覆盖关注推荐，其他新增内容待适配',
}
sections=[];total=0
for title,files in groups.items():
 labels=[]
 for name in files:
  s=(reference/name).read_text(encoding='utf-8')
  for m in re.finditer(r'new C0725\(([^;]*?)\)',s):
   text=re.findall(r'"((?:[^"\\]|\\.)*)"',m.group(1))
   if not text: continue
   label=text[1] if m.group(1).lstrip().startswith('"') and len(text)>1 else text[0]
   if not re.search('[\u4e00-\u9fff]',label) or len(label)>40 or label in labels:continue
   if label in ['首页','发现页','消息页','搜索页','详情页','其它','聊天相关']:continue
   labels.append(label)
  if name=='AlertDialogC0906.java':labels+=['自定义来源','自定义位置']
 total+=len(labels);sections.append((title,labels))
summary=f'参考：微博猪手 2.5.3（343）APK 中的设置菜单，共整理 {total} 个菜单项目（含子选项，不等于独立功能数）。\n未复制原模块实现；已补规则不代表已实测或完整等价。\n配置本地导出/导入已补，文件选择器实测待完成；WebDAV同步未实现。0.3.0 共 34 个规则开关；本轮新增 5 个广告开关，11 个明确广告项默认开启。非广告选项保持原设置。个人中心会员促销另有独立开关，开关对照通过。\n\n'
plain=summary+'\n\n'.join(title+'\n'+'\n'.join(label+'：'+implemented.get(label,'待补齐/待适配') for label in labels) for title,labels in sections)
raw=root/'app/res/raw';raw.mkdir(exist_ok=True)
(raw/'feature_inventory.txt').write_text(plain,encoding='utf-8')
doc='# 猪手功能对照与补齐账本\n\n'+summary+'\n'
for title,labels in sections:
 doc+='## '+title+'\n\n| 项目 | 当前进度 |\n|---|---|\n'
 doc+='\n'.join('| '+label+' | '+implemented.get(label,'待补齐/待适配')+' |' for label in labels)+'\n\n'
doc+='自动签到、领取、发博和删除操作默认不执行；未完成项目不伪装为成功。实测证据另见验收记录。\n'
(root/'功能对照.md').write_text(doc,encoding='utf-8')
print('Menu entries:',total)
