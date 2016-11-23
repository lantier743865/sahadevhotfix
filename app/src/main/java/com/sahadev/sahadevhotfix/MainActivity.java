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

    private TextView mSampleText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            String unzipRAWFile = unzipRAWFile(this);
            loadClass(unzipRAWFile);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void loadClass(String apkPath) throws ClassNotFoundException {
        ClassLoader classLoader = getClassLoader();

        File file = new File(apkPath);

        DexClassLoader dexClassLoader = new DexClassLoader(apkPath, file.getParent() + "/optimizedDirectory/", "", classLoader);
        Class<?> aClass = dexClassLoader.loadClass("ClassStudent");
        Log.i(TAG, "ClassStudent = " + aClass);
    }

    /**
     * 解压原始的APK文件
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

            Log.i(TAG, "文件解压完毕，路径地址为：" + apkFilePath);
        } else {
            Log.i(TAG, "文件已存在，无需解压");
        }

        return apkFilePath;
    }
}
