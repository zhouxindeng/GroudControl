package com.dji.sdk.sample.demo.missionoperator;

import static com.google.android.gms.internal.zzahn.runOnUiThread;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.CameraUpdate;
import com.amap.api.maps2d.CameraUpdateFactory;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.Marker;
import com.amap.api.maps2d.model.MarkerOptions;
import com.dji.sdk.sample.R;
import com.dji.sdk.sample.demo.missionmanager.MissionBaseView;
import com.dji.sdk.sample.internal.controller.DJISampleApplication;
import com.dji.sdk.sample.internal.controller.LocationUpdateEvent;
import com.dji.sdk.sample.internal.controller.MainActivity;
import com.dji.sdk.sample.internal.utils.ToastUtils;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.simulator.InitializationData;
import dji.common.gimbal.CapabilityKey;
import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointAction;
import dji.common.mission.waypoint.WaypointActionType;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionDownloadEvent;
import dji.common.mission.waypoint.WaypointMissionExecutionEvent;
import dji.common.mission.waypoint.WaypointMissionFinishedAction;
import dji.common.mission.waypoint.WaypointMissionFlightPathMode;
import dji.common.mission.waypoint.WaypointMissionGotoWaypointMode;
import dji.common.mission.waypoint.WaypointMissionHeadingMode;
import dji.common.mission.waypoint.WaypointMissionState;
import dji.common.mission.waypoint.WaypointMissionUploadEvent;
import dji.common.mission.waypoint.WaypointTurnMode;
import dji.common.model.LocationCoordinate2D;
import dji.common.util.CommonCallbacks;
import dji.common.util.DJIParamMinMaxCapability;
import dji.keysdk.FlightControllerKey;
import dji.keysdk.KeyManager;
import dji.sdk.base.BaseProduct;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.gimbal.Gimbal;
import dji.sdk.mission.MissionControl;
import dji.sdk.mission.waypoint.WaypointMissionOperator;
import dji.sdk.mission.waypoint.WaypointMissionOperatorListener;
import dji.sdk.products.Aircraft;

import static dji.keysdk.FlightControllerKey.HOME_LOCATION_LATITUDE;
import static dji.keysdk.FlightControllerKey.HOME_LOCATION_LONGITUDE;

public class WaypointMissionOperatorView extends MissionBaseView implements AMap.OnMapClickListener {

    private static final double BASE_LATITUDE = 22;
    private static final double BASE_LONGITUDE = 113;
    private static final int REFRESH_FREQ = 10;
    private static final int SATELLITE_COUNT = 10;
    private static final int MAX_HEIGHT = 500;
    private static final int MAX_RADIUS = 500;
    private static final double ONE_METER_OFFSET = 0.00000899322;
    private static final double HORIZONTAL_DISTANCE = 8;
    private static final double VERTICAL_DISTANCE = 8;
    private static final int WAYPOINT_COUNT = 4;

    private WaypointMissionOperator waypointMissionOperator = null;
    private FlightController flightController = null;
    private WaypointMission mission = null;
    private WaypointMissionOperatorListener listener;
    private float calculateTotalTime = 0.0f;
    double latitude;
    double longitude;
    float waypoint_alt=3.0f;//航点的指定高度
    private List<Waypoint> waypoint_list = new ArrayList<>();//航点的信息
    Marker fly_mark=null;
    private Boolean isadd=false;
    private final Map<Integer, Marker> markers = new ConcurrentHashMap<>();
    MapView mapView;
    AMap aMap;
    public static WaypointMission.Builder waypointMissionBuilder;
    private int pointIndex=0;

    public WaypointMissionOperatorView(Context context) {
        super(context);
    }

    @Override
    public void onClick(View view) {
        waypointMissionOperator = getWaypointMissionOperator();
        switch(view.getId()) {
            case R.id.btn_simulator:
                startSimulator();
                break;
            case R.id.btn_set_maximum_altitude:
                if (null != getFlightController()) {
                    flightController.setMaxFlightHeight(MAX_HEIGHT, new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            ToastUtils.setResultToToast(djiError == null ? "The maximum height is set to 500m!" : djiError.getDescription());
                        }
                    });
                }
                break;
            case R.id.btn_set_maximum_radius:
                if (null != getFlightController()) {
                    flightController.setMaxFlightRadius(MAX_RADIUS, new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            ToastUtils.setResultToToast(djiError == null ? "The maximum radius is set to 500m!" : djiError.getDescription());
                        }
                    });
                }
                break;
            case R.id.back: {
                update_fly_marker();
                move_cam();
                break;
            }
            case R.id.addpoint: {
                isadd = !isadd;
                if (isadd) {
                    waypoint_add.setText("退出添加");
                } else {
                    waypoint_add.setText("添加锚点");
                }
                break;
            }
            case R.id.clearpoint: {

                runOnUiThread(
                        new Runnable() {
                            @Override
                            public void run() {
                                if (aMap != null) {
                                    aMap.clear();
                                } else {
                                    Log.e("MapError", "aMap is null");
                                }
                            }
                        }
                );
                update_fly_marker();
                move_cam();
                waypoint_list.clear();
                waypointMissionBuilder.waypointList(waypoint_list);
                pointIndex = 0;
                break;
            }
            case R.id.btn_load:
                mission=waypoint_load();
                DJIError djiError = waypointMissionOperator.loadMission(mission);
                if (djiError == null) {
                    ToastUtils.setResultToToast("Mission is loaded successfully, estimated execution time is " + calculateTotalTime + " seconds.");
                } else {
                    ToastUtils.setResultToToast(djiError.getDescription());
                }
                break;
            case R.id.btn_upload:
                waypointMissionOperator.uploadMission(new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (djiError == null) {
                            ToastUtils.setResultToToast( "任务已经上传完成!" );
                        } else {
                            ToastUtils.setResultToToast( "任务上传失败: " + djiError.getDescription() + "  请重新设置." );
                            getWaypointMissionOperator().retryUploadMission( null );
                        }
                    }
                });
                break;
            case R.id.btn_start:
                if (null != mission) {
                    waypointMissionOperator.startMission(new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            ToastUtils.setResultToToast(djiError != null ? djiError.getDescription(): "start success");
                        }
                    });
                } else {
                    ToastUtils.setResultToToast("Wait for mission to be uploaded");
                }
                break;
            case R.id.btn_stop:
                waypointMissionOperator.stopMission(new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        ToastUtils.setResultToToast(djiError != null ? "" : djiError.getDescription());
                    }
                });
                break;
            case R.id.btn_pause:
                waypointMissionOperator.pauseMission(new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        ToastUtils.setResultToToast(djiError == null ? "The mission has been paused" : djiError.getDescription());
                    }
                });
                break;
            case R.id.btn_resume:
                waypointMissionOperator.resumeMission(new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        ToastUtils.setResultToToast(djiError == null ? "The mission has been resumed" : djiError.getDescription());
                    }
                });
                break;
            case R.id.btn_download:
                if (WaypointMissionState.EXECUTING.equals(waypointMissionOperator.getCurrentState()) || WaypointMissionState.EXECUTION_PAUSED.equals(waypointMissionOperator.getCurrentState())) {
                    waypointMissionOperator.downloadMission(new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            ToastUtils.setResultToToast(djiError != null ? "" : djiError.getDescription());
                        }
                    });
                } else {
                    ToastUtils.setResultToToast("Mission can be downloaded when the mission state is EXECUTING or EXECUTION_PAUSED!");
                }
                break;
            case R.id.btn_usingPython:
                executePythonScript();
                break;
            default:
                break;
        }
    }

    private void executePythonScript() {
        // 实例化 usingPython 类，并调用 runPythonScript 方法
        UsingPython scriptExecutor = new UsingPython();
        scriptExecutor.runPythonScript();
    }


    private void update_fly_marker(){
        // 更新地图位置
        LatLng currentLocation = new LatLng(homeLatitude, homeLongitude);
        //aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 18));
        //在地图上对无人机标点
        MarkerOptions markerOption = new MarkerOptions();
        markerOption.position(currentLocation);
        //markerOption.title("西安市").snippet("西安市：34.341568, 108.940174");
        //markerOption.draggable(true);//设置Marker可拖动
        markerOption.icon(BitmapDescriptorFactory.fromResource(R.drawable.aircraft));
        // 将Marker设置为贴地显示，可以双指下拉地图查看效果
        //markerOption.setFlat(true);//设置marker平贴地图效果
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(fly_mark!=null)
                    fly_mark.remove();
                fly_mark=aMap.addMarker(markerOption);
            }
        });
    }


    private WaypointMission waypoint_load() {
        if (waypointMissionBuilder == null) {
            waypointMissionBuilder = new WaypointMission.Builder()
                    .finishedAction(WaypointMissionFinishedAction.NO_ACTION)
                    .headingMode(WaypointMissionHeadingMode.AUTO)
                    .autoFlightSpeed(3.0f)
                    .maxFlightSpeed(3.0f)
                    .flightPathMode(WaypointMissionFlightPathMode.NORMAL);//根据设定点的顺序飞行
        } else {
            waypointMissionBuilder.finishedAction(WaypointMissionFinishedAction.NO_ACTION)
                    .headingMode(WaypointMissionHeadingMode.AUTO)
                    .autoFlightSpeed(3.0f)
                    .maxFlightSpeed(3.0f)
                    .flightPathMode(WaypointMissionFlightPathMode.NORMAL);
        }
        if ((waypointMissionBuilder.getWaypointList().size()) > 0) {
            for (int i = 0; i < waypointMissionBuilder.getWaypointList().size(); i++) {
                waypointMissionBuilder.getWaypointList().get(i).altitude = 25.0f;
            }
        }

        calculateTotalTime = waypointMissionBuilder.calculateTotalTime();
        return waypointMissionBuilder.build();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        BaseProduct product = DJISampleApplication.getProductInstance();

        if (product == null || !product.isConnected()) {
            ToastUtils.setResultToToast("Disconnect");
            return;
        } else {
            if (product instanceof Aircraft) {
                flightController = ((Aircraft) product).getFlightController();
            }
            if (flightController != null) {
                flightController.setStateCallback(new FlightControllerState.Callback() {
                    @Override
                    public void onUpdate(@NonNull FlightControllerState flightControllerState) {
                        homeLatitude = flightControllerState.getAircraftLocation().getLatitude();
                        homeLongitude = flightControllerState.getAircraftLocation().getLongitude();
                        flightState = flightControllerState.getFlightMode();
                        altitude = flightControllerState.getAircraftLocation().getAltitude();
                        velocityX = flightControllerState.getVelocityX();
                        velocityY = flightControllerState.getVelocityY();
                        velocityZ = flightControllerState.getVelocityZ();

                        if (flightControllerState.isLandingConfirmationNeeded()) {
                            flightController.confirmLanding(new CommonCallbacks.CompletionCallback() {
                                @Override
                                public void onResult(DJIError djiError) {
                                    ToastUtils.setResultToToast(djiError == null ? "confirmLanding OK" : djiError.getDescription());
                                }
                            });
                        }
                        updateWaypointMissionState();
                    }
                });
            }
        }
        mapView = (MapView) findViewById(R.id.map);
        mapView.onCreate(null);
//        if (aMap == null) {
//            aMap = mapView.getMap();
//        }
        initMapView();
        move_cam();
        waypointMissionOperator = MissionControl.getInstance().getWaypointMissionOperator();
        setUpListener();
    }

    private void move_cam() {
        LatLng latLng = new LatLng(homeLatitude, homeLongitude);
        float ZoomLevel = 18.0f;
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, ZoomLevel);
        aMap.moveCamera(cameraUpdate);
    }

    private void initMapView() {

        if (aMap == null) {
            aMap = mapView.getMap();
            aMap.setOnMapClickListener(this);// add the listener for click for amap object
            //aMap.setOnMarkerClickListener((AMap.OnMarkerClickListener) this);//去除单个标点
            //aMap.setInfoWindowAdapter((AMap.InfoWindowAdapter) this);
            //aMap.setOnInfoWindowClickListener((AMap.OnInfoWindowClickListener) this);//显示条可点击
            //aMap.setOnMarkerDragListener((AMap.OnMarkerDragListener) this);// 设置marker可拖拽事件监听器
            //aMap.setOnMapLoadedListener((AMap.OnMapLoadedListener) this);
        }
        //LatLng nuaa = new LatLng(32.036006,118.813250);
        //aMap.addMarker(new MarkerOptions().position(nuaa).title("Marker in nuaa"));
        //aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(nuaa, 18));
    }


    @Override
    protected void onDetachedFromWindow() {
        tearDownListener();
        if (flightController != null) {
            flightController.getSimulator().stop(null);
            flightController.setStateCallback(null);
        }
        super.onDetachedFromWindow();
    }


    private void updateWaypointMissionState(){
        if (waypointMissionOperator != null && waypointMissionOperator.getCurrentState() != null) {
            ToastUtils.setResultToText(FCPushInfoTV,
                    "latitude: "
                            + homeLatitude
                            + "\nlongitude: "
                            + homeLongitude
                            + "\n altitude: "
                            + altitude
                            + "\n vX: "
                            + velocityX
                            + "vY: "
                            + velocityY
                            + "vZ: "
                            + velocityZ
                            + "\nFlight state: "
                            + flightState.name()
                            + "\nMission state : "
                            + waypointMissionOperator.getCurrentState().getName());
        } else {
            ToastUtils.setResultToText(FCPushInfoTV,
                    "home point latitude: "
                            + homeLatitude
                            + "\nhome point longitude: "
                            + homeLongitude
                            + "\nFlight state: "
                            + flightState.name());
        }
    }

    private void setUpListener() {
        // Example of Listener
        listener = new WaypointMissionOperatorListener() {
            @Override
            public void onDownloadUpdate(@NonNull WaypointMissionDownloadEvent waypointMissionDownloadEvent) {
                // Example of Download Listener
                if (waypointMissionDownloadEvent.getProgress() != null
                        && waypointMissionDownloadEvent.getProgress().isSummaryDownloaded
                        && waypointMissionDownloadEvent.getProgress().downloadedWaypointIndex == (WAYPOINT_COUNT - 1)) {
                    ToastUtils.setResultToToast("Mission is downloaded successfully");
                }
                updateWaypointMissionState();
            }

            @Override
            public void onUploadUpdate(@NonNull WaypointMissionUploadEvent waypointMissionUploadEvent) {
                // Example of Upload Listener
                if (waypointMissionUploadEvent.getProgress() != null
                        && waypointMissionUploadEvent.getProgress().isSummaryUploaded
                        && waypointMissionUploadEvent.getProgress().uploadedWaypointIndex == (WAYPOINT_COUNT - 1)) {
                    ToastUtils.setResultToToast("Mission is uploaded successfully");
                }
                updateWaypointMissionState();
            }

            @Override
            public void onExecutionUpdate(@NonNull WaypointMissionExecutionEvent waypointMissionExecutionEvent) {
                // Example of Execution Listener
                Log.d("TAG",
                        (waypointMissionExecutionEvent.getPreviousState() == null
                                ? ""
                                : waypointMissionExecutionEvent.getPreviousState().getName())
                                + ", "
                                + waypointMissionExecutionEvent.getCurrentState().getName()
                                + (waypointMissionExecutionEvent.getProgress() == null
                                ? ""
                                : waypointMissionExecutionEvent.getProgress().targetWaypointIndex));
                updateWaypointMissionState();
            }

            @Override
            public void onExecutionStart() {
                ToastUtils.setResultToToast("Mission started");
                updateWaypointMissionState();
            }

            @Override
            public void onExecutionFinish(@Nullable DJIError djiError) {
                ToastUtils.setResultToToast("Mission finished");
                updateWaypointMissionState();
            }
        };

        if (waypointMissionOperator != null && listener != null) {
            // Example of adding listeners
            waypointMissionOperator.addListener(listener);
        }
    }

    private void tearDownListener() {
        if (waypointMissionOperator != null && listener != null) {
            // Example of removing listeners
            waypointMissionOperator.removeListener(listener);
        }
    }

    private int calculateTurnAngle() {
        return Math.round((float)Math.toDegrees(Math.atan(VERTICAL_DISTANCE/ HORIZONTAL_DISTANCE)));
    }

    private WaypointMissionOperator getWaypointMissionOperator() {
        if (null == waypointMissionOperator) {
            if (null != MissionControl.getInstance()) {
                return MissionControl.getInstance().getWaypointMissionOperator();
            }
        }
        return waypointMissionOperator;
    }

    private FlightController getFlightController() {
        if (null == flightController) {
            if (null != DJISampleApplication.getAircraftInstance()) {
                return DJISampleApplication.getAircraftInstance().getFlightController();
            }
            ToastUtils.setResultToToast("Product is disconnected!");
        }
        return flightController;
    }

    private void startSimulator() {
        if (null != getFlightController()) {
            flightController.getSimulator().start(InitializationData.createInstance(new LocationCoordinate2D(BASE_LATITUDE, BASE_LONGITUDE),REFRESH_FREQ, SATELLITE_COUNT), new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    ToastUtils.setResultToToast(djiError != null ?  djiError.getDescription():"Simulator started");
                }
            });
        }
    }

    @Override
    public int getDescription() {
        return R.string.component_listview_waypoint_mission_operator;
    }

    @Override
    public void onMapClick(LatLng latLng) {
        if (isadd) {
            markpoint_onmap(latLng);
            Waypoint waypoint = new Waypoint(latLng.latitude, latLng.longitude, waypoint_alt);
            if (waypointMissionBuilder == null) {
                waypointMissionBuilder = new WaypointMission.Builder();
                waypoint_list.add(waypoint);
                waypointMissionBuilder.waypointList(waypoint_list).waypointCount(waypoint_list.size());
            } else {
                waypoint_list.add(waypoint);
                waypointMissionBuilder.waypointList(waypoint_list).waypointCount(waypoint_list.size());
            }
        }
    }

    private void markpoint_onmap(LatLng latLng) {

        MarkerOptions mark_option = new MarkerOptions();
        mark_option.position(latLng);
        mark_option.draggable(true);
        mark_option.title("" + ++pointIndex);
        mark_option.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));


        Marker marker = aMap.addMarker(mark_option);
        marker.showInfoWindow();
        markers.put(markers.size(), marker);
    }

    @Subscribe
    public void onLocationUpdate(LocationUpdateEvent event) {
        double latitude = event.getLatitude();
        double longitude = event.getLongitude();

        // 更新地图位置
        LatLng currentLocation = new LatLng(latitude, longitude);
        aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 18));
        //在地图上对无人机标点
        MarkerOptions markerOption = new MarkerOptions();
        markerOption.position(currentLocation);
        //markerOption.title("西安市").snippet("西安市：34.341568, 108.940174");
        //markerOption.draggable(true);//设置Marker可拖动
        markerOption.icon(BitmapDescriptorFactory.fromResource(R.drawable.aircraft));
        // 将Marker设置为贴地显示，可以双指下拉地图查看效果
        //markerOption.setFlat(true);//设置marker平贴地图效果
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(fly_mark!=null)
                    fly_mark.remove();
                fly_mark=aMap.addMarker(markerOption);
            }
        });
    }

}
