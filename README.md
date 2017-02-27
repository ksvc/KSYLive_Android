# KSYLive_Android
## 一. 功能介绍
KSYLive库融合了Android 播放和推流SDK。旨在提供业内一流的移动直播SDK。具体功能：
* [x]  美颜
* [x] 美声
* [x] 连麦互动
* [x] 动态贴纸  
* [x] 录屏  
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

## 四、下载集成

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
    compile 'com.ksyun.media:libksylive-java:2.0.4'
    compile 'com.ksyun.media:libksylive-armv7a:2.0.4'

    # Other ABIs: optional
    compile 'com.ksyun.media:libksylive-arm64:2.0.4'
    compile 'com.ksyun.media:libksylive-x86:2.0.4'
}
```

* clone [github库](https://github.com/ksvc/KSYLive_Android)，使用github库中的demo和lib库。

* 或者从oschina镜像下载，国内访问速度更快：https://git.oschina.net/ksvc/KSYLive_Android

## FAQ

已知的问题可参见[FAQ](https://github.com/ksvc/KSYLive_Android/wiki/FAQ)

## 反馈与建议
- 主页：[金山云](http://v.ksyun.com)
- 邮箱：<zengfanping@kingsoft.com>
- QQ讨论群：574179720
- Issues: <https://github.com/ksvc/KSYLive_Android/issues>

<a href="http://www.ksyun.com/"><img src="http://www.ksyun.com/assets/img/static/logo.png" border="0" alt="金山云计算" /></a>
