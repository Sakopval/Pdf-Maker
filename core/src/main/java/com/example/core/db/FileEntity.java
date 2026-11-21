package com.example.core.db;

import androidx.room3.Entity;
import androidx.room3.PrimaryKey;

@Entity
public class FileEntity {
    @PrimaryKey (autoGenerate = true)
    private long fileId;
    private String type;
    private long size;
    private long date;
    private String name;
    private long lastModified;
    private String absolutePath;

    private long workSpaceOwnerId;

    public FileEntity(String absolutePath, long date,
                      long fileId, long lastModified, String name, long size, String type, long workSpaceOwnerId) {
        this.absolutePath = absolutePath;
        this.date = date;
        this.fileId = fileId;
        this.lastModified = lastModified;
        this.name = name;
        this.size = size;
        this.type = type;
        this.workSpaceOwnerId = workSpaceOwnerId;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public long getFileId() {
        return fileId;
    }

    public void setFileId(long fileId) {
        this.fileId = fileId;
    }

    public long getWorkSpaceOwnerId() {
        return workSpaceOwnerId;
    }

    public void setWorkSpaceOwnerId(long workSpaceOwnerId) {
        this.workSpaceOwnerId = workSpaceOwnerId;
    }
}
