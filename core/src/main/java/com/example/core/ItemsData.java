package com.example.core;

import com.example.core.db.FileEntity;

import javax.annotation.Nullable;

public class ItemsData {
    private String type;
    private FileEntity fileEntity;
    private String headerName;

    public ItemsData(@Nullable FileEntity fileEntity, String type, @Nullable String headerName) {
        this.fileEntity = fileEntity;
        this.type = type;
        this.headerName = headerName;
    }

    public FileEntity getFileEntity() {
        return fileEntity;
    }

    public String getType() {
        return type;
    }

    public String getHeaderName() {
        return headerName;
    }
}
