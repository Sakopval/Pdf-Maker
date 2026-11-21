package com.example.core.db;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.room3.Dao;
import androidx.room3.Delete;
import androidx.room3.Insert;
import androidx.room3.OnConflictStrategy;
import androidx.room3.Query;
import androidx.room3.Transaction;
import androidx.room3.Update;

import com.example.core.ItemsData;
import com.example.core.db.WorkSpaceEntity;
import com.example.core.db.WorkSpaceWithFiles;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

@Dao
public interface WorkSpaceDao {

    @Transaction
    @Query("SELECT * FROM WorkSpaceEntity")
    List<WorkSpaceWithFiles> getWorkSpaces();

    @Delete
    void deleteWorkSpace(WorkSpaceEntity workSpace);
    @Delete
    void deleteFile(FileEntity file);
    @Delete
    void deletePkg(PkgEntity pkg);

    @Query("SELECT * FROM WorkSpaceEntity WHERE name = :name LIMIT 1")
    WorkSpaceEntity getWorkSpaceByName(String name);
    @Query("SELECT * FROM WorkSpaceEntity WHERE workSpaceId = :id LIMIT 1")
    WorkSpaceEntity getWorkSpaceById(long id);
    @Query("SELECT * FROM FileEntity WHERE name = :name LIMIT 1")
    FileEntity getFileByName(String name);
    @Query("SELECT * FROM PkgEntity WHERE name = :name LIMIT 1")
    PkgEntity getPkgByName(String name);

    @Query("SELECT EXISTS(SELECT 1 FROM WorkSpaceEntity WHERE name = :name LIMIT 1)")
    boolean workSpaceExists(String name);
    @Query("SELECT EXISTS(SELECT 1 FROM PkgEntity WHERE name = :name LIMIT 1)")
    boolean pkgExists(String name);
//    @Query("SELECT EXISTS(SELECT 1 FROM FileEntity WHERE name = :name LIMIT 1)")
//    boolean FileExists(String name);

    @Update
    void updateWorkSpace(WorkSpaceEntity workSpace);
    @Update
    void updateFileEntity(FileEntity file);
    @Update
    void updatePkgEntity(PkgEntity pkg);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertWorkSpace(WorkSpaceEntity workSpace);
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertFile(FileEntity file);
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertFiles(List<FileEntity> fileEntities);
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertPkg(PkgEntity pkg);

    @Query("SELECT * FROM PkgEntity")
    List<PkgEntity> getAllPkgs();
    @Query("SELECT * FROM FileEntity WHERE LOWER(type) = 'pdf' OR LOWER(type) = '.pdf'")
    List<FileEntity> getAllPdfs();
    @Query("SELECT * FROM FileEntity WHERE LOWER(type) = 'log' OR LOWER(type) = '.log'")
    List<FileEntity> getAllLogs();
    @Query("SELECT * FROM FileEntity WHERE LOWER(type) = 'tex' OR LOWER(type) = '.tex'")
    List<FileEntity> getAllTex();
    @Query("SELECT * FROM FileEntity WHERE workSpaceOwnerId = :id")
    List<FileEntity> getFilesForWorkSpace(long id);
    @Query("SELECT * FROM WorkSpaceEntity")
    List<WorkSpaceEntity> getAllWorkSpaces();
    @Query("SELECT * FROM FileEntity")
    List<FileEntity> getAllFiles();

    @Transaction
    default void insertWorkSpaceWithFiles(WorkSpaceEntity workSpace, List<FileEntity> fileEntities){
        long workSpaceId = insertWorkSpace(workSpace);
        if (fileEntities != null && !fileEntities.isEmpty()){
            for (FileEntity file : fileEntities){
                file.setWorkSpaceOwnerId(workSpaceId);
            }
            insertFiles(fileEntities);
        }
    }

    @Transaction
    default void deleteWorkSpaceWithFiles(WorkSpaceEntity workSpace){
        long workSpaceId = insertWorkSpace(workSpace);
        List<FileEntity> fileEntities = getAllFiles();
        if (fileEntities != null && !fileEntities.isEmpty()){
            for (FileEntity file : fileEntities){
                if (file.getWorkSpaceOwnerId() == workSpaceId){
                    deleteFile(file);
                }
            }
            deleteWorkSpace(workSpace);
        }
    }

    @Transaction
    default List<FileEntity> getOnlyImportant(){
        List<FileEntity> all = new ArrayList<>();
        all.addAll(getAllLogs());
        all.addAll(getAllPdfs());
        all.addAll(getAllTex());
        return all;
    }

    default boolean FileExists(String name, long wsId){
        for (FileEntity file : getAllFiles()){
            if (file.getName().equals(name) && file.getWorkSpaceOwnerId() == wsId){
                return true;
            }
        }
        return false;
    }

    @Transaction
    default void renameWorkSpaceFiles(String newName, long workSpaceID, String newWsPath){
        WorkSpaceEntity workSpace = getWorkSpaceById(workSpaceID);
        for (FileEntity file : getFilesForWorkSpace(workSpaceID)){
            String oldPath = file.getAbsolutePath();
            String newPath = newWsPath+oldPath.replace(workSpace.getAbsolutePath(), "");
            file.setAbsolutePath(newPath);
            updateFileEntity(file);
        }
    }

    default FileEntity getFileEntity(String path, String name){
        FileEntity file = new FileEntity("", 0,0,0,"not found", 0, "tex",0);
        for (FileEntity f : getAllFiles()){
            if (path.equals(f.getAbsolutePath()) && name.equals(f.getName())){
                file = f;
                break;
            }
        }
        return file;
    }

    @Transaction
    default List<ItemsData> getItemsData(String entityType, @Nullable List<FileEntity> customList){
        List<WorkSpaceEntity> workSpaceEntities = getAllWorkSpaces();
        List<ItemsData> itemsData = new ArrayList<>();
        List<FileEntity> fileEntities;
        switch (entityType) {
            case "tex":
                fileEntities = getAllTex();
                break;
            case "pdf":
                fileEntities = getAllPdfs();
                break;
            case "log":
                fileEntities = getAllLogs();
                break;
            case "all":
                fileEntities = getOnlyImportant();
                break;
            case "custom":
                fileEntities = customList;
                break;
            default:
                Log.e("WrongEntityType", entityType + " not known entity type");
                return null;
        }
        for (WorkSpaceEntity ws : workSpaceEntities){
            long id = ws.getWorkSpaceId();
            int firstSimilar = -1;
            boolean addedFirstOne = false;
            for (FileEntity f : fileEntities){
                if (f.getWorkSpaceOwnerId() == id && firstSimilar == -1){
                    int index = fileEntities.indexOf(f);
                    firstSimilar = index;
                    itemsData.add(new ItemsData(null, "header", ws.getName()));
                    addedFirstOne = true;
                }
                if (addedFirstOne && f.getWorkSpaceOwnerId() == id){
                    itemsData.add(new ItemsData(f, "file", null));
                }
            }
            firstSimilar = -1;
            addedFirstOne = false;
        }

        return itemsData;
    }

    @Transaction
    default void insertFileEntity(FileEntity file){
        insertFile(file);
        WorkSpaceEntity workSpace = getWorkSpaceById(file.getWorkSpaceOwnerId());
        workSpace.setFilesNumber(workSpace.getFilesNumber() + 1);
        workSpace.setLastModified(System.currentTimeMillis());
        updateWorkSpace(workSpace);
    }
}
