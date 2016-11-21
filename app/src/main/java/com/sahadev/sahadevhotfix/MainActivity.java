package com.sahadev.sahadevhotfix;

import android.content.Context;
import android.content.res.Resources;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import dalvik.system.DexClassLoader;
import dalvik.system.PathClassLoader;

public class MainActivity extends AppCompatActivity {
    private String TAG = getClass().getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            String unzipRAWFile = unzipRAWFile(this);
            loadClass(unzipRAWFile);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void loadClass(String apkPath) throws ClassNotFoundException {
        ClassLoader classLoader = getClassLoader();

        File file = new File(apkPath);

        Class.forName("android.support.v4.app.FragmentActivity");
        DexClassLoader dexClassLoader = new DexClassLoader(apkPath, file.getParent(), apkPath, classLoader);
        Class<?> aClass = dexClassLoader.loadClass("com.kongfuzi.student.app.HomeActivity");
        Log.i(TAG, "com.kongfuzi.student.app.HomeActivity = " + aClass);
    }

    /**
     * 解压原始的APK文件
     *
     * @param context
     * @return
     * @throws IOException
     */
    private String unzipRAWFile(Context context) throws IOException {

        String apkFilePath;
        Resources resources = context.getResources();
        InputStream inputStream = resources.openRawResource(R.raw.yikaojiuguo_release);

        File externalCacheDir = context.getExternalCacheDir();

        File file = new File(externalCacheDir, resources.getResourceEntryName(R.raw.yikaojiuguo_release) + ".apk");

        apkFilePath = file.getAbsolutePath();
        if (!file.exists()) {
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(file));

            byte[] buffer = new byte[4 * 1024];
            int size = 0;
            while ((size = inputStream.read(buffer)) != -1) {
                bufferedOutputStream.write(buffer, 0, size);
                bufferedOutputStream.flush();
            }
            inputStream.close();
            bufferedOutputStream.close();
        }

        return apkFilePath;
    }
}
