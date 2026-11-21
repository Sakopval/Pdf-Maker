package com.example.pdfmaker.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.concurrent.ThreadPoolExecutor;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class SharedFiles {
    private final Context context;
    private final File cacheFolder;
    private final ThreadPoolExecutor poolExecutor;
    private final Handler handler;

    @Inject
    public SharedFiles(@ApplicationContext Context context, ThreadPoolExecutor poolExecutor, Handler handler) {
        this.context = context;
        this.poolExecutor = poolExecutor;
        cacheFolder = context.getCacheDir();
        this.handler = handler;
    }

    public void sharePdf(File pdf){
        poolExecutor.execute(()->{
            //make pdf cache folder
            File pdfCacheFolder = new File(cacheFolder, "shared_pdfs");
            if (!pdfCacheFolder.exists()){
                pdfCacheFolder.mkdirs();
            }
            //create the pdf in cache
            File pdfToShare = new File(pdfCacheFolder, "pdf_shared.pdf");
            try (FileOutputStream outputStream = new FileOutputStream(pdfToShare)) {
                try (FileInputStream inputStream = new FileInputStream(pdf)) {
                    byte[] bytes = new byte[8192];
                    int read;
                    while ((read = inputStream.read(bytes)) != -1){
                        outputStream.write(bytes, 0, read);
                    }
                    inputStream.close();
                    outputStream.flush();
                    outputStream.close();
                } catch (Exception e) {
                    Log.e(this.getClass().getName(), "error at copying this pdf to share", e);;
                }
                Uri uri = FileProvider.getUriForFile(context, context.getPackageName()+".fileprovider", pdfToShare);
                Intent share = new Intent();
                share.setDataAndType(uri, context.getContentResolver().getType(uri));
                share.setAction(Intent.ACTION_VIEW);
                share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION + Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(share);
            } catch (Exception e) {
                Log.e(this.getClass().getName(), "error at sharing this pdf", e);
            }
        });
    }


}
