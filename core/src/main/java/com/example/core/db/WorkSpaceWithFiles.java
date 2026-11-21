package com.example.core.db;

import androidx.room3.Embedded;
import androidx.room3.Relation;

import java.util.List;

public class WorkSpaceWithFiles {
    @Embedded public WorkSpaceEntity workSpace;
    @Relation(
            parentColumns = {"workSpaceId"},
            entityColumns = {"workSpaceOwnerId"}
    ) public List<FileEntity> fileEntities;

    public WorkSpaceWithFiles() {
    }
}
