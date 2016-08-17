# KSYLive_Android
## 一. 功能介绍
KSYLive库融合了Android 播放和推流SDK。旨在提供业内一流的移动直播SDK。具体功能：
* [x]  美颜
* [x] 美声
* [x] 连麦互动
* [x] 秒开加速
* [x] 低延时直播
* [x] 弱网优化
* [x] 卡顿优化
* [x] 丰富的软硬编、软硬解支持

具体推流功能请见：
* [KSYStreamer_Android](https://github.com/ksvc/KSYStreamer_Android)

具体播放功能请见：
* [KSYMediaPlayer_Android](https://github.com/ksvc/KSYMediaPlayer_Android)

## 运行环境
- 最低支持版本为Android 4.0 (API level 15)
- 支持的cpu架构：armv7, arm64, x86

## 二. 文档说明
* KSYMediaPlayer wiki <https://github.com/ksvc/KSYMediaPlayer_Android>
* KSYStreamer wiki <https://github.com/ksvc/KSYStreamer_Android>

## 三. 包大小说明

以体系结构armeabi-v7a为例：

注明：lib 为项目动态库so的大小
      res 资源文件目录
      classes.dex dalvik 字节码 

|名称|库名称| lib | res | classes.dex | 项目总大小 | apk size | 
| :---: | :---:|:---:|:---:|:---:|:---:|:---:|
|KSYMediaPlayer动态库| libksyplayer.so|12.7M|1.5M|2.8M|17.3M|14.6M| 
|KSYStreamer动态库| libksystreamer.so|30.5M|1.5M|2.8M|35.5M|6.8M|
|直播融合库|libksylive.so|19.8M|1.6M|3.3M|25.1M|10.1M|

## 反馈与建议
- 主页：[金山云](http://v.ksyun.com)
- 邮箱：<zengfanping@kingsoft.com>
- Issues: <https://github.com/ksvc/KSYLive_Android/issues>
