package com.example.core.modules;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public final class ThreadPool {

    @Provides
    @Singleton
    static ThreadPoolExecutor provideThreadPool(){
        return new ThreadPoolExecutor(4,8,10,
                TimeUnit.SECONDS, new ArrayBlockingQueue<>(5),new ThreadPoolExecutor.DiscardPolicy());
    }
}
