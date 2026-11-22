package com.example.core;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.core.db.FileEntity;
import com.example.core.db.FontEntity;
import com.example.core.db.PkgEntity;
import com.example.core.db.WorkSpaceEntity;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public final class MainActivityViewModel extends ViewModel {
    private MutableLiveData<Boolean> compiled;
    private MutableLiveData<List<WorkSpaceEntity>> workSpaces = new MutableLiveData<>();
    private MutableLiveData<List<FileEntity>> pdfsList = new MutableLiveData<>();
    private MutableLiveData<List<FileEntity>> texList = new MutableLiveData<>();
    private MutableLiveData<List<FileEntity>> logsList = new MutableLiveData<>();

    private MutableLiveData<List<FileEntity>> allFiles = new MutableLiveData<>();

    private MutableLiveData<Boolean> kpathsea_depug = new MutableLiveData<>(false);

    private MutableLiveData<List<PkgEntity>> pkgs = new MutableLiveData<>(new ArrayList<>());
    private MutableLiveData<List<FontEntity>> fonts = new MutableLiveData<>(new ArrayList<>());




    @Inject
    public MainActivityViewModel() {
        compiled = new MutableLiveData<>(true);
    }


    public void setCompiled(MutableLiveData<Boolean> compiled) {
        this.compiled = compiled;
    }

    public MutableLiveData<Boolean> getCompiled() {
        return compiled;
    }

    public MutableLiveData<List<WorkSpaceEntity>> getWorkSpaces() {
        return workSpaces;
    }

    public void setWorkSpaces(List<WorkSpaceEntity> workSpaces) {
        this.workSpaces.postValue(workSpaces);
    }

    public MutableLiveData<List<FileEntity>> getLogsList() {
        return logsList;
    }

    public void setLogsList(List<FileEntity> logsList) {
        this.logsList.postValue(logsList);
    }

    public MutableLiveData<List<FileEntity>> getPdfsList() {
        return pdfsList;
    }

    public void setPdfsList(List<FileEntity> pdfsList) {
        this.pdfsList.postValue(pdfsList);
    }

    public MutableLiveData<List<FileEntity>> getTexList() {
        return texList;
    }

    public void setTexList(List<FileEntity> texList) {
        this.texList.postValue(texList);
    }

    public MutableLiveData<List<FileEntity>> getAllFiles() {
        return allFiles;
    }

    public MutableLiveData<Boolean> getKpathsea_depug() {
        return kpathsea_depug;
    }

    public MutableLiveData<List<PkgEntity>> getPkgs() {
        return pkgs;
    }

    public MutableLiveData<List<FontEntity>> getFonts() {
        return fonts;
    }
}
