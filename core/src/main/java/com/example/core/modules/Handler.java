package com.example.core.modules;

import android.content.Context;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public class Handler {
    @Provides
    @Singleton
    android.os.Handler provideHandler(@ApplicationContext Context context){
        return new android.os.Handler(context.getMainLooper());
    }
}
