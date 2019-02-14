package com.zk.map.app;

import android.app.Application;

import team.zhuoke.sdk.ZKBase;

public class MapApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        ZKBase.init(this);


//        SuITjygZLWqSDXNOznQt5x2dRegws5Kt
    }
}
