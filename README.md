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
![](https://code.csdn.net/u011064099/sahadevhotfix/blob/master/blogResource/20161123174417.png)

好，现在我们使用class文件生成对应的dex文件。生成dex文件所需要的工具为dx，dx工具位于sdk的build-tools文件夹内，如下图所示：
![](https://code.csdn.net/u011064099/sahadevhotfix/blob/master/blogResource/20161123175306.png)

> **Tips:** 为了方便使用，建议将dx的路径添加到环境变量中。如果对dx工具不熟悉的，可以在终端中输入dx --help以获取帮助。

dx工具的基本用法是：
```
dx --dex [--output=<file>] [<file>.class | <file>.{zip,jar,apk} | <directory>]
```

> **Tips:** 刚开始自己摸索的时候，就没有仔细看命令，导致后面两个参数的顺序颠倒了，搞出了一些让人疑惑难解的问题，最后又不得不去找dx工具的源码调试，最后才发现自己的问题在哪。如果有对dx工具感兴趣的，可以对dx的包进行反编译或者获取dx的相关源代码进行了解。dx.lib文件位于dx.bat的下级目录lib文件夹中，可以使用JD-GUI工具对其进行查看或导出。如果需要获取源代码的，请使用以下命令进行克隆：
> 
> git clone https://android.googlesource.com/platform/dalvik
> 

我们使用以下命令生成dex文件：
```
dx --dex --output=user.dex ClassStudent.class
```

这里我为了防止出错，提前在当前目录下新建好了user.dex文件。上述命令依赖编译.class文件的JDK版本，如果使用的是JDK8编译的class会提示以下问题：
```java
PARSE ERROR:
unsupported class file version 52.0
...while parsing ClassStudent.class
1 error; aborting
```

这里的52.0意味着class文件不被支持，需要使用JDK8以下的版本进行编译，但是dx所需的环境还是需要为JDK8的，这里我编译class文件使用的是JDK7,请注意。

运行截图如下所示：
![](https://code.csdn.net/u011064099/sahadevhotfix/blob/master/blogResource/20161123160811.png)

好了，到此为止我们的目录应该如下：
![](https://code.csdn.net/u011064099/sahadevhotfix/blob/master/blogResource/20161123181510.png)

接下来将生成好的user.dex文件放入Android工程的res\raw文件夹下：
![](https://code.csdn.net/u011064099/sahadevhotfix/blob/master/blogResource/20161123181909.png)

在系统启动时将其写入到磁盘，这里不再贴出具体的写入代码，项目的MainActivity中包含了此部分代码。

在写入完毕之后使用DexClassLoader对其进行加载。DexClassLoader的构造方法需要4个参数，这里对这4个参数进行简要说明：

- String dexPath:dex文件的绝对路径。在这里我将其放入了应用的cache文件夹下。
- String optimizedDirectory:优化后的dex文件存放路径。DexClassLoader在构造完毕之后会对原有的dex文件优化并生成一个新的dex文件，在这里我选择的是.../cache/optimizedDirectory/目录。此外，API文档对该目录有严格的说明：**Do not cache optimized classes on external storage.**出于安全考虑，请不要将优化后的dex文件放入外部存储器中。
- String libraryPath:dex文件所需要的库文件路径。这里没有依赖，使用空字符串代替。
- ClassLoader parent:双亲委派原则中提到的父类加载器。这里我们使用默认的加载器，通过getClassLoader()方法获得。

在解释完毕DexClassLoader的构造参数之后，我们开始对刚刚的dex文件进行加载：
```java
DexClassLoader dexClassLoader = new DexClassLoader(apkPath, file.getParent() + "/optimizedDirectory/", "", classLoader);
```

接来下开始load我们刚刚写入在dex文件中的ClassStudent类：
```
Class<?> aClass = dexClassLoader.loadClass("ClassStudent");
```

然后我们对其进行初始化，并调用相关的get/set方法对其进行验证，在这里我传给ClassStudent对象一个字符串，然后调用它的get方法获取在方法内合并后的字符串：
```
	Object instance = aClass.newInstance();
	Method method = aClass.getMethod("setName", String.class);
	method.invoke(instance, "Sahadev");
			
	Method getNameMethod = aClass.getMethod("getName");
	Object invoke = getNameMethod.invoke(instance););
```

最后我们实现的代码可能是这样的：
```java
    /**
     * 加载指定路径的dex
     *
     * @param apkPath
     */
    private void loadClass(String apkPath) {
        ClassLoader classLoader = getClassLoader();

        File file = new File(apkPath);

        try {

            DexClassLoader dexClassLoader = new DexClassLoader(apkPath, file.getParent() + "/optimizedDirectory/", "", classLoader);
            Class<?> aClass = dexClassLoader.loadClass("ClassStudent");
            mLog.i(TAG, "ClassStudent = " + aClass);

            Object instance = aClass.newInstance();
            Method method = aClass.getMethod("setName", String.class);
            method.invoke(instance, "Sahadev");

            Method getNameMethod = aClass.getMethod("getName");
            Object invoke = getNameMethod.invoke(instance);

            mLog.i(TAG, "invoke result = " + invoke);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
```

最后附上我们的运行截图：
![](https://code.csdn.net/u011064099/sahadevhotfix/blob/master/blogResource/194172438045771671.jpg)