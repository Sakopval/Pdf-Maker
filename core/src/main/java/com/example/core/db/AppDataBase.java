package com.example.core.db;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room3.Database;
import androidx.room3.RoomDatabase;

import com.example.core.db.FileEntity;
import com.example.core.db.WorkSpaceDao;
import com.example.core.db.WorkSpaceEntity;

import kotlin.Unit;
import kotlin.coroutines.Continuation;

@Database(
        entities = {FileEntity.class, WorkSpaceEntity.class, PkgEntity.class},
        version = 1,
        exportSchema = false
)
public abstract class AppDataBase extends RoomDatabase {
    public abstract WorkSpaceDao workSpaceDao();
}
