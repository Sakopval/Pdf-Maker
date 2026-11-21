package com.example.pdfmaker.utils;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.documentfile.provider.DocumentFile;

import com.example.core.FileTools;
import com.example.core.db.AppDataBase;
import com.example.core.db.FileEntity;
import com.example.core.db.WorkSpaceDao;
import com.example.core.db.WorkSpaceEntity;
import com.example.pdfmaker.R;
import com.example.core.MainActivityViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class
ImportWorkSpace {
    private Context context;
    private final Handler handler;
    private final ThreadPoolExecutor poolExecutor;
    private final AppDataBase dataBase;
    private final File filesDir;
    private final FileTools fileTools;
    private WorkSpaceDao workSpaceDao;
    private MaterialAlertDialogBuilder dialogBuilder;
    MainActivityViewModel viewModel;

    @Inject
    public ImportWorkSpace(@ApplicationContext Context context, Handler handler, AppDataBase dataBase, ThreadPoolExecutor poolExecutor,
                           FileTools fileTools) {
        this.context = context;
        this.filesDir = context.getExternalFilesDir(null);
        this.handler = handler;
        this.poolExecutor = poolExecutor;
        this.fileTools = fileTools;
        this.dataBase = dataBase;
        this.workSpaceDao = dataBase.workSpaceDao();
    }

    public void importWorkSpace(Context activityContext, DocumentFile documentFile, MainActivityViewModel viewModel, RelativeLayout loadingLay) {
        this.viewModel = viewModel;
        if (fileTools.checkDocTree(documentFile).equals(fileTools.TYPE_VALID)) {
            poolExecutor.execute(() -> {
                String name;
                if (dataBase.workSpaceDao().workSpaceExists(documentFile.getName())) {
                    name = documentFile.getName() + fileTools.generateName();
                } else {
                    name = documentFile.getName();
                }
                handler.post(() -> {
                    loadingLay.setVisibility(View.VISIBLE);
                    loadingLay.findViewById(R.id.loading).setVisibility(View.VISIBLE);
                    loadingLay.findViewById(R.id.state_text).setVisibility(View.VISIBLE);
                    ((TextView) loadingLay.findViewById(R.id.state_text)).setText("Importing " + documentFile.getName());
                });
                File copyTo = new File(filesDir, name);
                if (!copyTo.exists()){
                    copyTo.mkdirs();
                }
                try {
                    Thread.sleep(3000);
                    fileTools.copyRecursive(documentFile, copyTo);
                    addToDB(documentFile, name);
                    handler.post(() -> {
                        ((TextView) loadingLay.findViewById(R.id.state_text)).setText("Imported " + documentFile.getName() + " Successfuly");
                    });
                    Thread.sleep(3000);
                    handler.post(() -> {
                        loadingLay.setVisibility(View.GONE);
                    });
                } catch (Exception e) {
                    Log.e("importWorkSpace", "importWorkSpaceMethod", e);
                    handler.post(() -> {
                        ((TextView) loadingLay.findViewById(R.id.state_text)).setText("Importing " + documentFile.getName() + " Unsuccessful");
                    });
                    try {
                        Thread.sleep(3000);
                    } catch (Exception ex) {
                        Log.e("sleepingThread", "", ex);
                    }
                    handler.post(() -> {
                        loadingLay.setVisibility(View.GONE);
                    });
                }
            });
        }
    }

    private void addToDB(DocumentFile documentFile, String name) throws Exception {
        long workSpaceID;
        File copied;
        List<FileEntity> fileEntities = new ArrayList<>();
        copied = new File(filesDir, name);
        Log.d("importWorkSpace", "copied folder path: ", new Throwable(copied.getAbsolutePath()));
        WorkSpaceEntity importedWorkSpace = new WorkSpaceEntity(0,
                copied.getName(), documentFile.lastModified(), 0,
                copied.getAbsolutePath());
        workSpaceID = workSpaceDao.insertWorkSpace(importedWorkSpace);
        Log.d("importWorkSpace", "workspace doesn't exist ", new Throwable("work space is" + workSpaceDao
                .getWorkSpaceByName(importedWorkSpace.getName())));

        if (copied.listFiles() != null) {
            for (File file : copied.listFiles()) {
                addFileToDB(file, workSpaceID, fileEntities);
            }
            workSpaceDao.insertWorkSpaceWithFiles(workSpaceDao.getWorkSpaceById(workSpaceID), fileEntities);
            //update workspace
            WorkSpaceEntity editedWorkSpace = workSpaceDao.getWorkSpaceById(workSpaceID);
            editedWorkSpace.setFilesNumber(fileEntities.size());
            editedWorkSpace.setLastModified(System.currentTimeMillis());
            workSpaceDao.updateWorkSpace(editedWorkSpace);
        }
        //update view model variables
        viewModel.setPdfsList(workSpaceDao.getAllPdfs());
        viewModel.setTexList(workSpaceDao.getAllTex());
        viewModel.setWorkSpaces(workSpaceDao.getAllWorkSpaces());
    }

    private void addFileToDB(File file,long workSpaceID, List<FileEntity> fileEntities){
        if (file.isDirectory()){
            for (File p:
                file.listFiles() ) {
                addFileToDB(p,workSpaceID,fileEntities);
            }
        }else {
            if (file.getName().contains(".")){
                FileEntity fileEntity = new FileEntity(file.getAbsolutePath(), System.currentTimeMillis(),
                        0, file.lastModified(), file.getName(),
                        file.length(), file.getName().substring(file.getName().lastIndexOf(".")), workSpaceID);
                fileEntities.add(fileEntity);
                Log.d("importWorkSpace", "created file entity with name: ", new Throwable(fileEntity.getName()));
            }
        }
    }
}
