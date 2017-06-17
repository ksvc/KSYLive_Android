# KSYLive_Android
## 阅读对象
本文档面向所有使用[金山云直播SDK][KSYLive_Android]的开发、测试人员等, 要求读者具有一定的Android编程开发经验，并且要求读者具备阅读[wiki][wiki]的习惯。

## 1. 概述

[金山云直播SDK][KSYLive_Android]是金山云提供的直播解决方案的一部分，完成了Android端音视频数据采集、处理、推流和播放的工作。

[金山云直播SDK][KSYLive_Android]**不限制**用户的推流、拉流地址。用户可以只使用[金山云直播SDK][KSYLive_Android]而不使用金山云的云服务。

[金山云直播SDK][KSYLive_Android]不收取任何授权使用费用，不含任何失效时间或者远程下发关闭的后门。同时[金山云直播SDK][KSYLive_Android]也不要求ak/sk等鉴权，没有任何用户标识信息。

[金山云直播SDK][KSYLive_Android]提供了业内一流的H.265编码、解码能力，H.265能力也是**免费使用**，欢迎集成试用。

[金山云直播SDK][KSYLive_Android]当前未提供开源代码，如果需要其他定制化开发功能，请通过[金山云商务渠道][ksyun]联系。

### 1.1 功能介绍
KSYLive库融合了Android 播放和推流SDK。旨在提供业内一流的移动直播SDK。具体功能：
* [x] [美颜](https://github.com/ksvc/KSYStreamer_Android/wiki/Video_Filter_Inner)
* [x] [滤镜](https://github.com/ksvc/KSYStreamer_Android/wiki/Video_Filter)
* [x] [美声](https://github.com/ksvc/KSYStreamer_Android/wiki/Audio_Filter)
* [x] [背景音乐功能](https://github.com/ksvc/KSYStreamer_Android/wiki/Audio_Mixer)
* [x] 多种动态贴纸  
* [x] [录屏](https://github.com/ksvc/KSYDiversityLive_Android/tree/master/KSYScreenStreamer)  
* [x] 秒开加速
* [x] 低延时直播
* [x] 弱网优化
* [x] 卡顿优化
* [x] 丰富的软硬编、软硬解支持
* [x] [悬浮窗推流](https://github.com/ksvc/KSYStreamer_Android/wiki/FloatingView)
* [x] [短视频编辑](https://github.com/ksvc/KSYMediaEditorKit_Android)

具体推流功能请见：
* [KSYStreamer_Android](https://github.com/ksvc/KSYStreamer_Android)

具体播放功能请见：
* [KSYMediaPlayer_Android](https://github.com/ksvc/KSYMediaPlayer_Android)

### 1.2 运行环境
- 最低支持版本为Android 4.0 (API level 15)
- 支持的cpu架构：[armv5](https://github.com/ksvc/KSYLive_Android/releases/tag/v2.2.6)，armv7, arm64, x86，


### 1.3 关于费用
金山云SDK保证，提供的[KSYLive Android直播SDK](https://github.com/ksvc/KSYLive_Android)可以用于商业应用，不会收取任何SDK使用费用。

但是基于[KSYLive Android直播SDK](https://github.com/ksvc/KSYLive_Android)的其他商业服务，会由特定供应商收取授权费用，大致包括：

1. 云存储
1. CDN分发
1. 动态贴纸
1. 连麦
1. 第三方美颜

## 2. 文档说明
* KSYMediaPlayer wiki <https://github.com/ksvc/KSYMediaPlayer_Android>，介绍了如何使用直播播放（观众侧）。
* KSYStreamer wiki <https://github.com/ksvc/KSYStreamer_Android>，介绍了如何使用直播推流（主播侧）。

## 3. 包大小说明
此说明以加入arm63-v8a X86 armeabi-v7a 三个体系结构为例进行说明

注明：
* lib 为项目动态库so的大小
* res 资源文件目录
* classes.dex dalvik 字节码 
* 项目总大小是 lib/res/classes.dex等未压缩的大小
* apk size是将项目总大小打包后的大小

|名称|库名称| lib | res | classes.dex | 项目总大小 | apk size | 
| :---: | :---:|:---:|:---:|:---:|:---:|:---:|
|播放SDK| libksyplayer.so|12.7M|1.5M|2.8M|17.3M|6.8M| 
|推流+播放SDK| libksystreamer.so|30.5M|1.5M|2.8M|35.5M|14.6M|
|直播融合库|libksylive.so|19.8M|1.6M|3.3M|25.1M|10.1M|


**这里可以看出，融合库比单纯推流和播放的apk size从14.6M减小了4.5M，到10.1M。（当然，如果不使用三个体系结构的话，并不会到10M大小。如果对apk size敏感，建议只使用armeabi-v7a库。）**
**推荐大家使用融合库！**

### 3.1 体系结构
当前[KSYLive_Android][KSYLive_Android]支持以下体系结构:
* armeabi
* armeabi-v7a
* arm64-v8a
* x86

为了节省apk size，如果没有特殊缘由，请只集成armeabi-v7a版本。
> 只集成armeabi-v7a版本，会导致ARMv5 ARMv6 设备不能运行。如果APP需要适配这两类设备，需要额外集成armebi版本。
> ARMv5 ARMv6 设备计算性能较差，金山云不保证该芯片设备上的直播体验。不推荐直播APP视频适配该两款芯片设备。

## 4、下载集成

* 推荐直接使用gradle方式集成：

``` gradle
# required
allprojects {
    repositories {
        jcenter()
    }
}

dependencies {
    # required, enough for most devices.
    compile 'com.ksyun.media:libksylive-java:2.3.3'
    compile 'com.ksyun.media:libksylive-armv7a:2.3.3'

    # Other ABIs: optional
    compile 'com.ksyun.media:libksylive-arm64:2.3.3'
    compile 'com.ksyun.media:libksylive-x86:2.3.3'
}
```

* clone [github库](https://github.com/ksvc/KSYLive_Android)，使用github库中的demo和lib库。

* 或者从oschina镜像下载，国内访问速度更快：https://git.oschina.net/ksvc/KSYLive_Android

## 5. FAQ

已知的问题可参见[FAQ](https://github.com/ksvc/KSYLive_Android/wiki/FAQ)

## 6. 反馈与建议
- 主页：[金山云](http://v.ksyun.com)
- 邮箱：<zengfanping@kingsoft.com>
- QQ讨论群：574179720
- Issues: <https://github.com/ksvc/KSYLive_Android/issues>

<a href="http://www.ksyun.com/"><img src="https://raw.githubusercontent.com/wiki/ksvc/KSYLive_Android/images/logo.png" border="0" alt="金山云计算" /></a>

[KSYLive_Android]:https://github.com/ksvc/KSYLive_Android/edit/master/README.md
[ksyun]:https://www.ksyun.com/about/aboutcontact
[wiki]:https://github.com/ksvc/KSYLive_Android/wiki
