package com.zk.map.app;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.Poi;
import com.baidu.mapapi.animation.Animation;
import com.baidu.mapapi.animation.ScaleAnimation;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapBaseIndoorMapInfo;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.blankj.utilcode.util.ToastUtils;
import com.zk.map.app.service.LocationService;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private LocationService locationService;
    private TextView LocationResult;
    private Button startLocation;
    private String permissionInfo;
    private MapView mapView;
    private BaiduMap mBaiduMap;
    private Button switchFloor;
    private String floor_info;

    // 保存当前楼层的信息。
    private ArrayList<String> floors;
    private String floor_id;

    private static final String TAG = "MainActivity";

    private final int SDK_PERMISSION_REQUEST = 127;

    private TextView mTextMessage;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    LocationResult.setText(R.string.title_home);
                    return true;
                case R.id.navigation_dashboard:
                    mTextMessage.setText(R.string.title_dashboard);
                    return true;
                case R.id.navigation_notifications:
                    LocationResult.setText(R.string.title_notifications);
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LocationResult = (TextView) findViewById(R.id.message);
        startLocation = (Button) findViewById(R.id.startLocation);
        mapView = (MapView) findViewById(R.id.map_view);
        switchFloor = (Button) findViewById(R.id.switch_floor);
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        switchFloor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (floors != null && floors.size() > 0) {

                    if (!TextUtils.isEmpty(floor_info)) {
                        int i =  floors.indexOf(floor_info);
                        i++;
                        if (i > 0 && i < floors.size()) {
                            //切换楼层
                            floor_info = floors.get(i);

                        } else {
                            floor_info = floors.get(0);
                        }


                    } else {
                        floor_info = floors.get(0);
                    }

                    ToastUtils.showShort("当前楼层是：" + floor_info);

                    // 切换楼层信息
                    //strID 通过 mMapBaseIndoorMapInfo.getID()方法获得
                    MapBaseIndoorMapInfo.SwitchFloorError switchFloorError = mBaiduMap.switchBaseIndoorMapFloor(floor_info, floor_id);

                    Log.d(TAG, "onClick: switchFloorError==" + switchFloorError);

                    return;


                }
                ToastUtils.showShort("切换楼层错误。");
            }
        });


        mBaiduMap = mapView.getMap();
        baiduMapSetting();


        getPermission();
    }


    private void baiduMapSetting() {
        MapStatus.Builder builder = new MapStatus.Builder();
//        builder.zoom(18.0f);
        mBaiduMap.setMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));

        mBaiduMap.setMyLocationEnabled(true);


//        mCurrentMode = LocationMode.FOLLOWING;//定位跟随态
//        mCurrentMode = LocationMode.NORMAL;   //默认为 LocationMode.NORMAL 普通态
//        mCurrentMode = LocationMode.COMPASS;  //定位罗盘态
        mBaiduMap.setMyLocationConfiguration(new MyLocationConfiguration(MyLocationConfiguration.LocationMode.FOLLOWING,
                true, BitmapDescriptorFactory.fromResource(R.drawable.icon_start), 0xAAFFFF88, 0xAA00FF00));
        //自定义精度圈填充颜色
//        accuracyCircleFillColor = 0xAAFFFF88;
        //自定义精度圈边框颜色
//        accuracyCircleStrokeColor = 0xAA00FF00;
//        mBaiduMap.setMyLocationConfiguration(mLocationConfiguration)





        marker(0, 0);
        mapType();
        inDoorMapInfo();

    }

    private void marker(double lat, double lon) {

        if (lat == 0 || lon == 0)
            return;

        LatLng latLng = new LatLng(lat, lon);
        //构建Marker图标
        BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.icon_start);

        //创建marker
        MarkerOptions ooA = new MarkerOptions().position(latLng).icon(bitmapDescriptor);
        //添加marker
        Marker marker = (Marker) (mBaiduMap.addOverlay(ooA));
        startSingleScaleAnimation(marker);

//        //构建MarkerOption，用于在地图上添加Marker
//        OverlayOptions option = new MarkerOptions()
//                .position(latLng)
//                .icon(bitmap);
////在地图上添加Marker，并显示
//        mBaiduMap.addOverlay(option);

    }

    private void mapType() {
        //显示卫星图层
//        mBaiduMap.setMapType(BaiduMap.MAP_TYPE_SATELLITE);
    }

    private void inDoorMapInfo() {
        //打开室内图，默认为关闭状态
        mBaiduMap.setIndoorEnable(true);
        mBaiduMap.setOnBaseIndoorMapListener(new BaiduMap.OnBaseIndoorMapListener() {

            /**
             * 地图进入室内图模式回调函数
             *
             * @param in 是否进入室内图模式
             * @param mapBaseIndoorMapInfo 室内图信息
             */
            @Override
            public void onBaseIndoorMapMode(boolean in, MapBaseIndoorMapInfo mapBaseIndoorMapInfo) {
                if (in) {
                    // 进入室内图
                    // 通过获取回调参数 mapBaseIndoorMapInfo 便可获取室内图信息，包含楼层信息，室内ID等


                    floors = mapBaseIndoorMapInfo.getFloors();
                    floor_id = mapBaseIndoorMapInfo.getID();

                    Log.d(TAG, "onBaseIndoorMapMode:  当前的楼层信息：" + floors);

//                    floor_info = floors.get(0);

//                    // 切换楼层信息
////strID 通过 mMapBaseIndoorMapInfo.getID()方法获得
//                    SwitchFloorError switchFloorError = mBaiduMap.switchBaseIndoorMapFloor(strFloor, strID);

                } else {
                    // 移除室内图
                }
            }
        });

        //实现楼层间地图切换,展示不同楼层的室内图
//        MapBaseIndoorMapInfo.SwitchFloorError switchFloorError = mBaiduMap.switchBaseIndoorMapFloor(strFloor, floorID);
        //上面代码中，strFloor表示室内图楼层,格式为F1,B1… strID 表示室内图ID；返回值switchFloorError 用于标识楼层切换错误信息， 具体如下：
        //切换楼层成功    SWITCH_OK
        // 切换楼层, 室内ID信息错误   FLOOR_INFO_ERROR,
        //楼层溢出  FLOOR_OVERLFLOW,
        //切换楼层室内ID与当前聚焦室内ID不匹配  FOCUSED_ID_ERROR,
        //切换楼层失败    SWITCH_ERROR
    }


    private Animation getScaleAnimation() {
        //创建缩放动画
        ScaleAnimation mScale = new ScaleAnimation(1f, 2f, 1f);
        //设置动画执行时间
        mScale.setDuration(2000);
        //动画重复模式
        mScale.setRepeatMode(Animation.RepeatMode.RESTART);
        //动画重复次数
        mScale.setRepeatCount(1);
        //设置缩放动画监听
        mScale.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart() {
            }
            @Override
            public void onAnimationEnd() {
            }
            @Override
            public void onAnimationCancel() {
            }
            @Override
            public void onAnimationRepeat() {
            }
        });
        return mScale;
    }
    /**
     * 开启单边缩放动画 X或Y方向
     */
    public void startSingleScaleAnimation(Marker marker) {
        //marker设置动画
        marker.setAnimation(getScaleAnimation());
        //开启marker动画
        marker.startAnimation();
    }


    @TargetApi(23)
    private void getPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ArrayList<String> permissions = new ArrayList<String>();
            /***
             * 定位权限为必须权限，用户如果禁止，则每次进入都会申请
             */
            // 定位精确位置
            if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
            if(checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            }
            /*
             * 读写权限和电话状态权限非必要权限(建议授予)只会申请一次，用户同意或者禁止，只会弹一次
             */
            // 读写权限
            if (addPermission(permissions, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                permissionInfo += "Manifest.permission.WRITE_EXTERNAL_STORAGE Deny \n";
            }
            // 读取电话状态权限
            if (addPermission(permissions, Manifest.permission.READ_PHONE_STATE)) {
                permissionInfo += "Manifest.permission.READ_PHONE_STATE Deny \n";
            }

            if (permissions.size() > 0) {
                requestPermissions(permissions.toArray(new String[permissions.size()]), SDK_PERMISSION_REQUEST);
            }
        }
    }

    @TargetApi(23)
    private boolean addPermission(ArrayList<String> permissionsList, String permission) {
        if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) { // 如果应用没有获得对应权限,则添加到列表中,准备批量申请
            if (shouldShowRequestPermissionRationale(permission)){
                return true;
            }else{
                permissionsList.add(permission);
                return false;
            }

        }else{
            return true;
        }
    }

    @TargetApi(23)
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // TODO Auto-generated method stub
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    }


    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时必须调用mMapView. onResume ()
        mapView.onResume();
    }
    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时必须调用mMapView. onPause ()
        mapView.onPause();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        //在activity执行onDestroy时必须调用mMapView.onDestroy()
        mapView.onDestroy();
    }

    /***
     * Stop location service
     */
    @Override
    protected void onStop() {
        // TODO Auto-generated method stub
        locationService.unregisterListener(mListener); //注销掉监听
        locationService.stop(); //停止定位服务
        super.onStop();
    }

    @Override
    protected void onStart() {
        // TODO Auto-generated method stub
        super.onStart();
        // -----------location config ------------
        locationService = ((MapApplication) getApplication()).locationService;
        //获取locationservice实例，建议应用中只初始化1个location实例，然后使用，可以参考其他示例的activity，都是通过此种方式获取locationservice实例的
        locationService.registerListener(mListener);
        //注册监听
        int type = getIntent().getIntExtra("from", 0);
        if (type == 0) {
            locationService.setLocationOption(locationService.getDefaultLocationClientOption());
        } else if (type == 1) {
            locationService.setLocationOption(locationService.getOption());
        }
        startLocation.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (startLocation.getText().toString().equals(getString(R.string.startlocation))) {
                    locationService.start();// 定位SDK
                    // start之后会默认发起一次定位请求，开发者无须判断isstart并主动调用request
                    startLocation.setText(getString(R.string.stoplocation));
                } else {
                    locationService.stop();
                    startLocation.setText(getString(R.string.startlocation));
                }
            }
        });
    }


    /*****
     *
     * 定位结果回调，重写onReceiveLocation方法，可以直接拷贝如下代码到自己工程中修改
     *
     */
    private BDAbstractLocationListener mListener = new BDAbstractLocationListener() {

        @Override
        public void onReceiveLocation(BDLocation location) {
            // TODO Auto-generated method stub
            if (null != location && location.getLocType() != BDLocation.TypeServerError) {
                StringBuffer sb = new StringBuffer(256);
                sb.append("time : ");
                /**
                 * 时间也可以使用systemClock.elapsedRealtime()方法 获取的是自从开机以来，每次回调的时间；
                 * location.getTime() 是指服务端出本次结果的时间，如果位置不发生变化，则时间不变
                 */
                sb.append(location.getTime());
                sb.append("\nlocType : ");// 定位类型
                sb.append(location.getLocType());
                sb.append("\nlocType description : ");// *****对应的定位类型说明*****
                sb.append(location.getLocTypeDescription());
                sb.append("\nlatitude : ");// 纬度
                sb.append(location.getLatitude());
                sb.append("\nlontitude : ");// 经度
                sb.append(location.getLongitude());
                sb.append("\nradius : ");// 半径
                sb.append(location.getRadius());
                sb.append("\nCountryCode : ");// 国家码
                sb.append(location.getCountryCode());
                sb.append("\nCountry : ");// 国家名称
                sb.append(location.getCountry());
                sb.append("\ncitycode : ");// 城市编码
                sb.append(location.getCityCode());
                sb.append("\ncity : ");// 城市
                sb.append(location.getCity());
                sb.append("\nDistrict : ");// 区
                sb.append(location.getDistrict());
                sb.append("\nStreet : ");// 街道
                sb.append(location.getStreet());
                sb.append("\naddr : ");// 地址信息
                sb.append(location.getAddrStr());
                sb.append("\nUserIndoorState: ");// *****返回用户室内外判断结果*****
                sb.append(location.getUserIndoorState());
                sb.append("\nDirection(not all devices have value): ");
                sb.append(location.getDirection());// 方向
                sb.append("\nlocationdescribe: ");
                sb.append(location.getLocationDescribe());// 位置语义化信息
                sb.append("\nPoi: ");// POI信息
                if (location.getPoiList() != null && !location.getPoiList().isEmpty()) {
                    for (int i = 0; i < location.getPoiList().size(); i++) {
                        Poi poi = (Poi) location.getPoiList().get(i);
                        sb.append(poi.getName() + ";");
                    }
                }
                if (location.getLocType() == BDLocation.TypeGpsLocation) {// GPS定位结果
                    sb.append("\nspeed : ");
                    sb.append(location.getSpeed());// 速度 单位：km/h
                    sb.append("\nsatellite : ");
                    sb.append(location.getSatelliteNumber());// 卫星数目
                    sb.append("\nheight : ");
                    sb.append(location.getAltitude());// 海拔高度 单位：米
                    sb.append("\ngps status : ");
                    sb.append(location.getGpsAccuracyStatus());// *****gps质量判断*****
                    sb.append("\ndescribe : ");
                    sb.append("gps定位成功");
                } else if (location.getLocType() == BDLocation.TypeNetWorkLocation) {// 网络定位结果
                    // 运营商信息
                    if (location.hasAltitude()) {// *****如果有海拔高度*****
                        sb.append("\nheight : ");
                        sb.append(location.getAltitude());// 单位：米
                    }
                    sb.append("\noperationers : ");// 运营商信息
                    sb.append(location.getOperators());
                    sb.append("\ndescribe : ");
                    sb.append("网络定位成功");
                } else if (location.getLocType() == BDLocation.TypeOffLineLocation) {// 离线定位结果
                    sb.append("\ndescribe : ");
                    sb.append("离线定位成功，离线定位结果也是有效的");
                } else if (location.getLocType() == BDLocation.TypeServerError) {
                    sb.append("\ndescribe : ");
                    sb.append("服务端网络定位失败，可以反馈IMEI号和大体定位时间到loc-bugs@baidu.com，会有人追查原因");
                } else if (location.getLocType() == BDLocation.TypeNetWorkException) {
                    sb.append("\ndescribe : ");
                    sb.append("网络不同导致定位失败，请检查网络是否通畅");
                } else if (location.getLocType() == BDLocation.TypeCriteriaException) {
                    sb.append("\ndescribe : ");
                    sb.append("无法获取有效定位依据导致定位失败，一般是由于手机的原因，处于飞行模式下一般会造成这种结果，可以试着重启手机");
                }
                logMsg(sb.toString());




                //西单大悦城
//                116.379299,39.916967
                double lat = 116.379299;
                double lon = 39.916967;

                lat = location.getLatitude();
                lon = location.getLongitude();


                marker(lat, lon);




                MyLocationData locData = new MyLocationData.Builder()
                        .accuracy(location.getRadius())
                        // 此处设置开发者获取到的方向信息，顺时针0-360
                        .direction(location.getDirection()).latitude(location.getLatitude())
                        .longitude(location.getLongitude()).build();
                mBaiduMap.setMyLocationData(locData);
            }
        }

    };

    /**
     * 显示请求字符串
     *
     * @param str
     */
    public void logMsg(String str) {
        final String s = str;
        try {
            if (LocationResult != null){
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        LocationResult.post(new Runnable() {
                            @Override
                            public void run() {
                                LocationResult.setText(s);
                            }
                        });

                    }
                }).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
