Hello SahadevHotFix

#一步步手动实现热修复

热修复技术自从QQ空间团队搞出来之后便渐渐趋于成熟。

我们这个系列主要介绍如何一步步手动实现基本的热修复功能，无需使用第三方框架。

在开始学习之前，需要对基本的热修复技术有些了解，以下文章可以帮助到你：

- [安卓App热补丁动态修复技术介绍](https://mp.weixin.qq.com/s?__biz=MzI1MTA1MzM2Nw==&mid=400118620&idx=1&sn=b4fdd5055731290eef12ad0d17f39d4a&scene=1&srcid=1106Imu9ZgwybID13e7y2nEi#wechat_redirect)
- [【腾讯Bugly干货分享】Android Patch 方案与持续交付](https://my.oschina.net/bugly/blog/727850)
- [Android dex分包方案](http://blog.csdn.net/vurtne_ye/article/details/39666381)
- [dodola/HotFix](https://github.com/dodola/HotFix)

##一、生成dex文件及加载
