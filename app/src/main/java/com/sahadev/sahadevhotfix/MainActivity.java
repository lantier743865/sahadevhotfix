package com.sahadev.sahadevhotfix;

import android.content.Context;
import android.content.res.Resources;
import android.os.PersistableBundle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.sahadev.bean.ClassStudent;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import dalvik.system.BaseDexClassLoader;
import dalvik.system.DexClassLoader;
import dalvik.system.PathClassLoader;

public class MainActivity extends AppCompatActivity {
    private String TAG = getClass().getSimpleName();

    private TextView mSampleText;
    private Log mLog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSampleText = (TextView) findViewById(R.id.sample_text);
        mLog = new Log();

        String unzipRAWFile = unzipRAWFile(this);
        loadClass(unzipRAWFile);
        inject(unzipRAWFile);
        demonstrationRawMode();
    }

    /**
     * 验证替换类后的效果
     */
    private void demonstrationRawMode() {
        ClassStudent classStudent = new ClassStudent();
        classStudent.setName("Lavon");
        mLog.i(TAG, classStudent.getName());
    }

    public String inject(String apkPath) {
        boolean hasBaseDexClassLoader = true;

        File file = new File(apkPath);

        File optimizedDirectoryFile = new File(file.getParentFile(), "optimizedDirectory");

        if (!optimizedDirectoryFile.exists())
            optimizedDirectoryFile.mkdir();

        try {
            Class.forName("dalvik.system.BaseDexClassLoader");
        } catch (ClassNotFoundException e) {
            hasBaseDexClassLoader = false;
        }
        if (hasBaseDexClassLoader) {
            PathClassLoader pathClassLoader = (PathClassLoader) getClassLoader();
            DexClassLoader dexClassLoader = new DexClassLoader(apkPath, optimizedDirectoryFile.getAbsolutePath(), "", pathClassLoader);
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

    public Object combineArray(Object object, Object object2) {
        Class<?> aClass = Array.get(object, 0).getClass();

        Object obj = Array.newInstance(aClass, 2);

        Array.set(obj, 0, Array.get(object2, 0));
        Array.set(obj, 1, Array.get(object, 0));

        return obj;
    }

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

    /**
     * 加载指定路径的dex
     *
     * @param apkPath
     */
    private void loadClass(String apkPath) {
        //该方法内的以下代码需要注释
        if (true) {
            return;
        }

        ClassLoader classLoader = getClassLoader();

        File file = new File(apkPath);

        File optimizedDirectoryFile = new File(file.getParentFile(), "optimizedDirectory");

        if (!optimizedDirectoryFile.exists())
            optimizedDirectoryFile.mkdir();

        try {

            DexClassLoader dexClassLoader = new DexClassLoader(apkPath, optimizedDirectoryFile.getAbsolutePath(), "", classLoader);
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

    /**
     * 解压原始的dex文件
     *
     * @param context
     * @return
     * @throws IOException
     */
    private String unzipRAWFile(Context context) {

        String apkFilePath;
        Resources resources = context.getResources();
        InputStream inputStream = resources.openRawResource(R.raw.user);

        File externalCacheDir = context.getExternalCacheDir();

        File file = new File(externalCacheDir, resources.getResourceEntryName(R.raw.user) + ".dex");

        apkFilePath = file.getAbsolutePath();
        if (!file.exists()) {
            BufferedOutputStream bufferedOutputStream = null;
            FileOutputStream fileOutputStream = null;

            try {
                fileOutputStream = new FileOutputStream(file);
                bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            byte[] buffer = new byte[4 * 1024];
            int size;
            try {
                while ((size = inputStream.read(buffer)) != -1) {
                    bufferedOutputStream.write(buffer, 0, size);
                    bufferedOutputStream.flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (inputStream != null)
                    inputStream.close();
                if (bufferedOutputStream != null)
                    bufferedOutputStream.close();
                if (fileOutputStream != null)
                    fileOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            mLog.i(TAG, "文件解压完毕，路径地址为：" + apkFilePath);
        } else {
            mLog.i(TAG, "文件已存在，无需解压");
        }

        return apkFilePath;
    }

    private class Log {
        void i(String TAG, String content) {
            mSampleText.append(content + "\n\n");
        }
    }
}
