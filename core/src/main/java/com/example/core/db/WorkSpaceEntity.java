package com.example.core.db;

import androidx.room3.Entity;
import androidx.room3.PrimaryKey;

@Entity
public class WorkSpaceEntity {
    @PrimaryKey(autoGenerate = true)
    private long workSpaceId;
    private String name;
    private String absolutePath;
    private int filesNumber;
    private long lastModified;


    public WorkSpaceEntity(long workSpaceId, String name, long lastModified, int filesNumber, String absolutePath) {
        this.workSpaceId = workSpaceId;
        this.name = name;
        this.lastModified = lastModified;
        this.filesNumber = filesNumber;
        this.absolutePath = absolutePath;
    }

    public int getFilesNumber() {
        return filesNumber;
    }

    public void setFilesNumber(int filesNumber) {
        this.filesNumber = filesNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAbsolutePath() {
        return absolutePath;
    }

    public void setAbsolutePath(String absolutePath) {
        this.absolutePath = absolutePath;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    public long getWorkSpaceId() {
        return workSpaceId;
    }

    public void setWorkSpaceId(long workSpaceId) {
        this.workSpaceId = workSpaceId;
    }
}
