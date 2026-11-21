package com.example.pdfmaker.repositories;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.room3.Dao;

import com.example.core.db.AppDataBase;
import com.example.core.db.FileEntity;
import com.example.core.db.WorkSpaceDao;
import com.example.core.db.WorkSpaceEntity;
import com.example.core.db.WorkSpaceWithFiles;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class WorkSpaceRepo {

    private final AppDataBase dataBase;
    private final WorkSpaceDao dao;
    private LiveData<List<FileEntity>> pdfs;
    private LiveData<List<FileEntity>> texs;
    private LiveData<List<WorkSpaceWithFiles>> workSpaces;

    @Inject
    public WorkSpaceRepo(AppDataBase dataBase) {
        this.dataBase = dataBase;
        this.dao = dataBase.workSpaceDao();
        this.workSpaces = new MutableLiveData<>(dao.getWorkSpaces());
        this.pdfs = new MutableLiveData<>(dao.getAllPdfs());
        this.texs = new MutableLiveData<>(dao.getAllTex());
    }

}
