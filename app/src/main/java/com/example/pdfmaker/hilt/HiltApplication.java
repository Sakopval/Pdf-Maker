package com.example.pdfmaker.hilt;

import android.app.Application;

import com.rk.App;
import com.rk.application.XedApplication;

import javax.inject.Inject;

import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
public class HiltApplication extends App {
    @Inject Bar bar;

    @Override
    public void onCreate() {
        super.onCreate();
    }
}
