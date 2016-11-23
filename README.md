#一步步手动实现热修复

热修复技术自从QQ空间团队搞出来之后便渐渐趋于成熟。

我们这个系列主要介绍如何一步步手动实现基本的热修复功能，无需使用第三方框架。

在开始学习之前，需要对基本的热修复技术有些了解，以下文章可以帮助到你：

- [安卓App热补丁动态修复技术介绍](https://mp.weixin.qq.com/s?__biz=MzI1MTA1MzM2Nw==&mid=400118620&idx=1&sn=b4fdd5055731290eef12ad0d17f39d4a&scene=1&srcid=1106Imu9ZgwybID13e7y2nEi#wechat_redirect)
- [【腾讯Bugly干货分享】Android Patch 方案与持续交付](https://my.oschina.net/bugly/blog/727850)
- [Android dex分包方案](http://blog.csdn.net/vurtne_ye/article/details/39666381)
- [dodola/HotFix](https://github.com/dodola/HotFix)

##一、dex文件的生成与加载
我们在这部分主要做的流程有：

- 1.编写基本的Java文件并编译为.class文件。
- 2.将.class文件转为.dex文件。
- 3.将转好的dex文件放入创建好的Android工程内并在启动时将其写入本地。
- 4.加载解压后的.dex文件中的类，并调用其方法进行测试。

>**Note:** 在阅读本节之前最好先了解一下类加载器的双亲委派原则、DexClassLoader的使用以及反射的知识点。
###编写基本的Java文件并编译为.class文件
首先我们在一个工程目录下开始创建并编写我们的Java文件，你可能会选择各种IDE来做这件事，但我在这里劝你不要这么做，因为有坑在等你。等把基本流程搞清楚可以再选择更进阶的方法。这里我们可以选择文本编辑器比如EditPlus来对Java文件进行编辑。

新建一个Java文件，并命名为：ClassStudent.java，并在java文件内键入以下代码：
```java
public class ClassStudent {
	private String name;

	public ClassStudent() {

	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName(){
		return this.name + ".Mr";	
	}
}
```

> **Note:** 这里要注意，不要对类添加包名，因为在后期对class文件处理时会遇到问题，具体问题会稍后说明。

在文件创建好之后，对Java文件进行编译：

