package com.example.pdfmaker.utils;

import com.example.core.db.AppDataBase;
import com.example.core.db.FileEntity;
import com.example.core.db.WorkSpaceDao;
import com.example.core.db.WorkSpaceEntity;
import com.example.core.db.WorkSpaceWithFiles;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class WorkSpaceUtils {

    @Inject
    public WorkSpaceUtils() {
    }
    public boolean workSpaceExists(WorkSpaceEntity workSpace, WorkSpaceDao workSpaceDao){
        List<WorkSpaceEntity> workSpaceEntities = workSpaceDao.getAllWorkSpaces();
        return workSpaceEntities.contains(workSpace);
    }

    public boolean fileEntityExists(FileEntity file, WorkSpaceDao workSpaceDao){
        for (WorkSpaceWithFiles workSpaceWithFiles : workSpaceDao.getWorkSpaces()){
            if (workSpaceWithFiles.fileEntities.contains(file)) {
                return true;
            }
        }
        return false;
    }
}
