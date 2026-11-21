package com.example.pdfmaker.viewmodels;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.core.db.FileEntity;
import com.example.core.db.WorkSpaceEntity;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class LogsActivityViewModel extends ViewModel {

    private MutableLiveData<List<WorkSpaceEntity>> workSpaces = new MutableLiveData<>();
    private MutableLiveData<List<FileEntity>> pdfsList = new MutableLiveData<>();
    private MutableLiveData<List<FileEntity>> texList = new MutableLiveData<>();
    private MutableLiveData<List<FileEntity>> logsList = new MutableLiveData<>();

    @Inject
    public LogsActivityViewModel() {
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

    public void setPdfsList(MutableLiveData<List<FileEntity>> pdfsList) {
        this.pdfsList = pdfsList;
    }

    public MutableLiveData<List<FileEntity>> getTexList() {
        return texList;
    }

    public void setTexList(MutableLiveData<List<FileEntity>> texList) {
        this.texList = texList;
    }

    public MutableLiveData<List<WorkSpaceEntity>> getWorkSpaces() {
        return workSpaces;
    }

    public void setWorkSpaces(MutableLiveData<List<WorkSpaceEntity>> workSpaces) {
        this.workSpaces = workSpaces;
    }
}
