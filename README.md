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

>**Note:** 在阅读本节之前最好先了解一下类加载器的双亲委派模型、DexClassLoader的使用以及反射的知识点。

###编写基本的Java文件并编译为.class文件
首先我们在一个工程目录下开始创建并编写我们的Java文件，你可能会选择各种IDE来做这件事，但我在这里劝你不要这么做，因为有坑在等你。等把基本流程搞清楚可以再选择更进阶的方法。这里我们可以选择文本编辑器比如EditPlus来对Java文件进行编辑。

新建一个Java文件，并命名为：com.sahadev.bean.ClassStudent.java，并在java文件内键入以下代码

```java
public class com.sahadev.bean.ClassStudent {
	private String name;

	public com.sahadev.bean.ClassStudent() {

	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName(){
		return this.name + ".Mr";	
	}
}
```

> **Note:** 这里要注意，不要对类添加包名，因为在后期对class文件处理时会遇到问题，具体问题会稍后说明。上面的getName方法在返回时对this.name属性添加了一段字符串，这里请注意，后面会用到。

在文件创建好之后，对Java文件进行编译：
![](https://code.csdn.net/u011064099/sahadevhotfix/blob/master/blogResource/20161123174417.png)

###将.class文件转为.dex文件
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
dx --dex --output=user.dex com.sahadev.bean.ClassStudent.class
```

这里我为了防止出错，提前在当前目录下新建好了user.dex文件。上述命令依赖编译.class文件的JDK版本，如果使用的是JDK8编译的class会提示以下问题：
```java
PARSE ERROR:
unsupported class file version 52.0
...while parsing com.sahadev.bean.ClassStudent.class
1 error; aborting
```

这里的52.0意味着class文件不被支持，需要使用JDK8以下的版本进行编译，但是dx所需的环境还是需要为JDK8的，这里我编译class文件使用的是JDK7,请注意。

上面我们提到了为什么先不要在ClassStudent中使用包名，因为在执行dx的时候会报以下异常，这是因为以下第二项条件没有通过，该代码位于com.android.dx.cf.direct.DirectClassFile文件内：
```java
	String thisClassName = thisClass.getClassType().getClassName();
	if(!(filePath.endsWith(".class") && filePath.startsWith(thisClassName) && (filePath.length()==(thisClassName.length()+6)))){
		throw new ParseException("class name (" + thisClassName + ") does not match path (" + filePath + ")");
	}
```

运行截图如下所示：
![](https://code.csdn.net/u011064099/sahadevhotfix/blob/master/blogResource/20161123160811.png)

好了，到此为止我们的目录应该如下：
![](https://code.csdn.net/u011064099/sahadevhotfix/blob/master/blogResource/20161123181510.png)

###写入dex到本地磁盘
接下来将生成好的user.dex文件放入Android工程的res\raw文件夹下：
![](https://code.csdn.net/u011064099/sahadevhotfix/blob/master/blogResource/20161123181909.png)

在系统启动时将其写入到磁盘，这里不再贴出具体的写入代码，项目的MainActivity中包含了此部分代码。

###加载dex中的类并测试
在写入完毕之后使用DexClassLoader对其进行加载。DexClassLoader的构造方法需要4个参数，这里对这4个参数进行简要说明：

- String dexPath:dex文件的绝对路径。在这里我将其放入了应用的cache文件夹下。
- String optimizedDirectory:优化后的dex文件存放路径。DexClassLoader在构造完毕之后会对原有的dex文件优化并生成一个新的dex文件，在这里我选择的是.../cache/optimizedDirectory/目录。此外，API文档对该目录有严格的说明：**Do not cache optimized classes on external storage.**出于安全考虑，请不要将优化后的dex文件放入外部存储器中。
- String libraryPath:dex文件所需要的库文件路径。这里没有依赖，使用空字符串代替。
- ClassLoader parent:双亲委派模型中提到的父类加载器。这里我们使用默认的加载器，通过getClassLoader()方法获得。

在解释完毕DexClassLoader的构造参数之后，我们开始对刚刚的dex文件进行加载：
```java
DexClassLoader dexClassLoader = new DexClassLoader(apkPath, file.getParent() + "/optimizedDirectory/", "", classLoader);
```

接来下开始load我们刚刚写入在dex文件中的ClassStudent类：
```
Class<?> aClass = dexClassLoader.loadClass("com.sahadev.bean.ClassStudent");
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
            Class<?> aClass = dexClassLoader.loadClass("com.sahadev.bean.ClassStudent");
            mLog.i(TAG, "com.sahadev.bean.ClassStudent = " + aClass);

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

###二、类的加载机制简要介绍
一个类在被加载到内存之前要经过加载、验证、准备等过程。经过这些过程之后，虚拟机才会从方法区将代表类的运行时数据结构转换为内存中的Class。

我们这节内容的重点在于一个类是如何被加载的，所以我们从类的加载入口开始。

类的加载是由虚拟机触发的，类的加载入口位于ClassLoader的loadClassInternal()方法：
```java
    // This method is invoked by the virtual machine to load a class.
    private Class<?> loadClassInternal(String name)
        throws ClassNotFoundException
    {
        // For backward compatibility, explicitly lock on 'this' when
        // the current class loader is not parallel capable.
        if (parallelLockMap == null) {
            synchronized (this) {
                 return loadClass(name);
            }
        } else {
            return loadClass(name);
        }
    }
```

这段方法还有段注释说明：这个方法由虚拟机调用用来加载一个类。我们看到这个类的内部最后调用了loadClass()方法。那我们进入loadClass()方法看看：
```java
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return loadClass(name, false);
    }
```
loadClass()方法方法内部调用了loadClass()的重载方法：
```java
    protected Class<?> loadClass(String name, boolean resolve)
        throws ClassNotFoundException
    {
        synchronized (getClassLoadingLock(name)) {
            // First, check if the class has already been loaded
            Class<?> c = findLoadedClass(name);
            if (c == null) {
                long t0 = System.nanoTime();
                try {
                    if (parent != null) {
                        c = parent.loadClass(name, false);
                    } else {
                        c = findBootstrapClassOrNull(name);
                    }
                } catch (ClassNotFoundException e) {
                    // ClassNotFoundException thrown if class not found
                    // from the non-null parent class loader
                }

                if (c == null) {
                    // If still not found, then invoke findClass in order
                    // to find the class.
                    long t1 = System.nanoTime();
                    c = findClass(name);

                    // this is the defining class loader; record the stats
                    sun.misc.PerfCounter.getParentDelegationTime().addTime(t1 - t0);
                    sun.misc.PerfCounter.getFindClassTime().addElapsedTimeFrom(t1);
                    sun.misc.PerfCounter.getFindClasses().increment();
                }
            }
            if (resolve) {
                resolveClass(c);
            }
            return c;
        }
    }
```
loadClass()方法大概做了以下工作：

- 首先查找该类是否已经被加载.
- 如果该ClassLoader有父加载器，那么调用父加载器的loadClass()方法.
- 如果没有父加载器，则调用findBootstrapClassOrNull()方法进行加载，该方法会使用引导类加载器进行加载。普通类是不会被该加载器加载到的，所以这里一般返回null.
- 如果前面的步骤都没找到，那调用自身的findClass()方法进行查找。

好，ClassLoader的findClass()方法是个空方法，所以这个过程一般是由子加载器实现的。Java的加载器这么设计是有一定的渊源的，感兴趣的读者可以自行查找书籍了解。
```java
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        throw new ClassNotFoundException(name);
    }
```

在Android中，ClassLoader的直接子类是BaseDexClassLoader，我们看一下BaseDexClassLoader的findClass()实现：
```java
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        Class clazz = pathList.findClass(name);

        if (clazz == null) {
            throw new ClassNotFoundException(name);
        }

        return clazz;
    }
```

> **Tips:** 有需要虚拟机以及类加载器全套代码的，请使用以下命令克隆:
> git clone https://android.googlesource.com/platform/dalvik-snapshot
> 相关代码位于项目的ics-mr1分支上。

看到这里我们可以知道，Android中类的查找是通过这个pathList进行查找的，而pathList又是个什么鬼呢？

在BaseDexClassLoader中声明了以下变量：
```java
    /** structured lists of path elements */
    private final DexPathList pathList;
```

所以我们可以看看DexPathList的findClass()方法做了什么：
```java
    public Class findClass(String name) {
        for (Element element : dexElements) {
            DexFile dex = element.dexFile;

            if (dex != null) {
                Class clazz = dex.loadClassBinaryName(name, definingContext);
                if (clazz != null) {
                    return clazz;
                }
            }
        }

        return null;
    }
```

这里通过遍历dexElements中的Element对象进行查找，最终走的是DexFile的loadClassBinaryName()方法：
```java
    public Class loadClassBinaryName(String name, ClassLoader loader) {
        return defineClass(name, loader, mCookie);
    }

    private native static Class defineClass(String name, ClassLoader loader, int cookie);
```

到此为止，我们就将一个类真正的加载过程梳理完了。

##三、Class文件的替换
在上一节了解了基本的类加载原理之后，我们这一节开始对工程内部的类实行替换。

> **Tips:** 本章主要依赖文章[http://blog.csdn.net/vurtne_ye/article/details/39666381](http://blog.csdn.net/vurtne_ye/article/details/39666381)中的未实现代码实现，实现思路也源自该文章，在阅读本文之前可以先行了解。

这一节我们主要实现的流程有：

- 在工程内创建相同的ClassStudent类，但在调用getName()方法返回字符串时会稍有区别，用于结果验证
- 使用DexClassLoader加载外部的user.dex
- 将DexClassLoader中的dexElements放在PathClassLoader的dexElements之前
- 验证替换结果

###创建工程内的ClassStudent
我们在第一节中演示了如何加载外部的Class，为了起到热修复效果，那么我们需要在工程内有一个被替换的类，被替换的ClassStudent类内容如下：
```java
package com.sahadev.bean;

/**
 * Created by shangbin on 2016/11/24.
 * Email: sahadev@foxmail.com
 */

public class ClassStudent {
    private String name;

    public ClassStudent() {

    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName(){
        return this.name + ".Miss";
    }

}
```
外部的ClassStudent类的内容如下：
```java
package com.sahadev.bean;

/**
 * Created by shangbin on 2016/11/24.
 * Email: sahadev@foxmail.com
 */

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

这两个类除了在getName()方法返回之处有差别之外，其它地方一模一样，不过这足可以让我们说明情况。

> **我们这里要实现的目的：** 我们默认调用getName()方法返回的是**"xxxx.Miss"**，如果热修复成功，那么再使用该方法的话，返回的则会是**"xxxx.Mr"**。

###对含有包名的类再次编译
因为第一节中专门声明了不可以对类声明包名，但是这样在Android工程中无法引用到该类，所以把不能声明包名的问题解决了一下。

不能声明包名的主要原因是在编译Java文件时，没有正确的使用命令。对含有包名的Java文件应当使用以下命令：
```
javac -d ./ ClassStudent.java
```

经过上面命令编译后的.class文件便可以顺利通过dx工具的转换。

我们还是按照第一节的步骤将转换后的user.dex文件放入工程中并写入本地磁盘，以便稍后使用。

####替换工程内的类文件
在开始之前还是再回顾一下实现思路：类在使用之前必须要经过加载器的加载才能够使用，在加载类时会调用自身的findClass()方法进行查找。然而在Android中类的查找使用的是BaseDexClassLoader，BaseDexClassLoader对findClass()方法进行了重写：
```java
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        Class clazz = pathList.findClass(name);

        if (clazz == null) {
            throw new ClassNotFoundException(name);
        }

        return clazz;
    }
```

pathList是类DexPathList的实例，这里pathList.findClass的实现如下：
```java
    public Class findClass(String name) {
        for (Element element : dexElements) {
            DexFile dex = element.dexFile;

            if (dex != null) {
                Class clazz = dex.loadClassBinaryName(name, definingContext);
                if (clazz != null) {
                    return clazz;
                }
            }
        }

        return null;
    }
```

由此我们可以得知类的查找是通过遍历dexElements来进行查找的。所以为了实现替换效果，我们需要将DexClassLoader中的Element对象放到dexElements数组的第0个位置，这样才能在BaseDexClassLoader查找类时先找到DexClassLoader所用的user.dex中的类。

> **Tips:** 如果对上面这段内容看不懂的，没关系，可以移步到本系列课程的第二节了解一下类加载的具体流程。

类的加载是从上而下加载的，所以就算是DexClassLoader加载了外部的类，但是在系统使用类的时候还是会先在ClassLoader中查找，如果找不到则会在BaseDexClassLoader中查找，如果再找不到，就会进入PathClassLoader中查找，最后才会使用DexClassLoader进行查找，所以按照这个流程外部类是无法正常发挥作用的。所以我们的目的就是在查找工程内的类之前，先让加载器去外部的dex中查找。

好了，再次梳理了思路之后，我们接下来对思路进行实践。

下面的方法是我们主要的注入方法：

```java
    public String inject(String apkPath) {
        boolean hasBaseDexClassLoader = true;

        File file = new File(apkPath);
        try {
            Class.forName("dalvik.system.BaseDexClassLoader");
        } catch (ClassNotFoundException e) {
            hasBaseDexClassLoader = false;
        }
        if (hasBaseDexClassLoader) {
            PathClassLoader pathClassLoader = (PathClassLoader) getClassLoader();
            DexClassLoader dexClassLoader = new DexClassLoader(apkPath, file.getParent() + "/optimizedDirectory/", "", pathClassLoader);
            try {
                Object dexElements = combineArray(getDexElements(getPathList(pathClassLoader)), getDexElements(getPathList(dexClassLoader)));
                Object pathList = getPathList(pathClassLoader);
                setField(pathList, pathList.getClass(), "dexElements", dexElements);
                return "SUCCESS";
            } catch (Throwable e) {
                e.printStackTrace();
                return android.util.Log.getStackTraceString(e);
            }
        }
        return "SUCCESS";
    }
 ```

 > **Tips:** 这段代码原封不动采用于[http://blog.csdn.net/vurtne_ye/article/details/39666381](http://blog.csdn.net/vurtne_ye/article/details/39666381)文章中最后的实现代码，但是该文章并没有给出具体的注入细节。我们接下里的过程就是对没有给全的细节进行补充与讲解。

这段代码的核心在于将DexClassLoader中的dexElements与PathClassLoader中的dexElements进行合并，然后将合并后的dexElements替换原先的dexElements。最后我们在使用ClassStudent类的时候便可以直接使用外部的ClassStudent，而不会再加载默认的ClassStudent类。

首先我们通过classLoader获取各自的pathList对象：
```java
    public Object getPathList(BaseDexClassLoader classLoader) {
        Class<? extends BaseDexClassLoader> aClass = classLoader.getClass();

        Class<?> superclass = aClass.getSuperclass();
        try {

            Field pathListField = superclass.getDeclaredField("pathList");
            pathListField.setAccessible(true);
            Object object = pathListField.get(classLoader);

            return object;
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }
```

在使用以上反射的时候要注意，pathList属性属于基类BaseDexClassLoader。所以如果直接获取DexClassLoader或者PathClassLoader的pathList属性的话，会得到null。

其次是获取pathList对应的dexElements，这里要注意dexElements是个数组对象：
```java
    public Object getDexElements(Object object) {
        if (object == null)
            return null;

        Class<?> aClass = object.getClass();
        try {
            Field dexElements = aClass.getDeclaredField("dexElements");
            dexElements.setAccessible(true);
            return dexElements.get(object);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;

    }
```

接下来我们将两个数组对象合并成为一个：
```java
    public Object combineArray(Object object, Object object2) {
        Class<?> aClass = Array.get(object, 0).getClass();

        Object obj = Array.newInstance(aClass, 2);

        Array.set(obj, 0, Array.get(object2, 0));
        Array.set(obj, 1, Array.get(object, 0));

        return obj;
    }
```
上面这段代码我们根据数组对象的类型创建了一个新的大小为2的新数组，并将两个数组的第一个元素取出，将代表外部dex的dexElement放在了第0个位置。这样便可以确保在查找类时优先从外部的dex中查找。

最后将原先的dexElements覆盖：
```java
    public void setField(Object pathList, Class aClass, String fieldName, Object fieldValue) {

        try {
            Field declaredField = aClass.getDeclaredField(fieldName);
            declaredField.setAccessible(true);
            declaredField.set(pathList, fieldValue);

        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

    }
```

###验证替换结果
好，我们做完以上的工作之后，写一段代码来进行验证：
```java
    /**
     * 验证替换类后的效果
     */
    private void demonstrationRawMode() {
        ClassStudent classStudent = new ClassStudent();
        classStudent.setName("Lavon");
        mLog.i(TAG, classStudent.getName());
    }
```

如果我们没有替换成功的话，那么这里默认使用的是内部的ClassStudent，getName()返回的会是**Lavon.Miss**。
如果我们替换成功的话，那么这里默认使用的是外部的ClassStudent，getName()返回的则会是**Lavon.Mr**。

我们实际运行看下效果：
![这里写图片描述](http://img.blog.csdn.net/20161127092544502)

这说明我们已经完成了基本的热修复。有任何疑问欢迎留言。

本节课程主要分为3块：

- 1.[一步步手动实现热修复(一)-dex文件的生成与加载](http://blog.csdn.net/sahadev_/article/details/53318251)
- 2.[一步步手动实现热修复(二)-类的加载机制简要介绍](http://blog.csdn.net/sahadev_/article/details/53334911)
- 3.[一步步手动实现热修复(三)-Class文件的替换](http://blog.csdn.net/sahadev_/article/details/53363052)

本节示例所用到的任何资源都已开源，项目中包含工程中所用到代码、示例图片、说明文档。项目地址为：
[https://code.csdn.net/u011064099/sahadevhotfix/tree/master](https://code.csdn.net/u011064099/sahadevhotfix/tree/master)

-----------------
我建了一个QQ群，欢迎对学习有兴趣的同学加入。我们可以一起探讨、深究、掌握那些我们会用到的技术，让自己不至于太落伍。
![这里写图片描述](http://img.blog.csdn.net/20161127095233746)