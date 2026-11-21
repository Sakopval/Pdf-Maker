package com.example.core.modules;

import android.content.Context;

import androidx.room3.Room;
import androidx.sqlite.driver.AndroidSQLiteDriver;

import com.example.core.db.AppDataBase;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public class RoomDB {
    @Provides
    @Singleton
    public AppDataBase provideDB(@ApplicationContext Context context){
        return Room.databaseBuilder(context, AppDataBase.class, "pdfMakerDB").
                setDriver(new AndroidSQLiteDriver()).build();
    }
}
