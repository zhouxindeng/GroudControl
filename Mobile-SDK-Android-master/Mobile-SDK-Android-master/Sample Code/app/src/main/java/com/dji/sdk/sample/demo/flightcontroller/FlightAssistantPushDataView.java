package com.dji.sdk.sample.demo.flightcontroller;

import static com.google.android.gms.internal.zzahn.runOnUiThread;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.dji.sdk.sample.R;
import com.dji.sdk.sample.internal.controller.DJIDemoApplication;
import com.dji.sdk.sample.internal.controller.DJISampleApplication;
import com.dji.sdk.sample.internal.controller.LocationUpdateEvent;
import com.dji.sdk.sample.internal.controller.MainActivity;
import com.dji.sdk.sample.internal.controller.MySQLDatabaseHelper;
import com.dji.sdk.sample.internal.utils.ModuleVerificationUtil;
import com.dji.sdk.sample.internal.view.BaseThreeBtnView;

import org.jetbrains.annotations.NotNull;

import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.GPSSignalLevel;
import dji.common.flightcontroller.ObstacleDetectionSector;
import dji.common.flightcontroller.VisionDetectionState;
import dji.common.logics.warningstatuslogic.WarningStatusItem;
import dji.keysdk.DiagnosticsKey;
import dji.keysdk.KeyManager;
import dji.keysdk.callback.GetCallback;
import dji.sdk.base.BaseProduct;
import dji.sdk.flightcontroller.FlightAssistant;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;



/**
 * Class that retrieves the push data for Intelligent Flight Assistant
 */
public class FlightAssistantPushDataView extends BaseThreeBtnView {

    public FlightAssistantPushDataView(Context context) {
        super(context);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (ModuleVerificationUtil.isFlightControllerAvailable()) {
            FlightController flightController =
                ((Aircraft) DJISampleApplication.getProductInstance()).getFlightController();

            FlightAssistant intelligentFlightAssistant = flightController.getFlightAssistant();

            StringBuilder stringBuilder = new StringBuilder();

            if (intelligentFlightAssistant != null) {

                intelligentFlightAssistant.setVisionDetectionStateUpdatedCallback(new VisionDetectionState.Callback() {
                    @Override
                    public void onUpdate(@NonNull VisionDetectionState visionDetectionState) {
                        ObstacleDetectionSector[] visionDetectionSectorArray =
                            visionDetectionState.getDetectionSectors();

                        for (ObstacleDetectionSector visionDetectionSector : visionDetectionSectorArray) {

                            visionDetectionSector.getObstacleDistanceInMeters();
                            visionDetectionSector.getWarningLevel();

                            stringBuilder.append("Obstacle distance: ")
                                        .append(visionDetectionSector.getObstacleDistanceInMeters())
                                        .append("\n");
                            stringBuilder.append("Distance warning: ")
                                        .append(visionDetectionSector.getWarningLevel())
                                        .append("\n");
                        }

                        stringBuilder.append("WarningLevel: ")
                                    .append(visionDetectionState.getSystemWarning().name())
                                    .append("\n");
                        stringBuilder.append("Sensor state: ")
                                    .append(visionDetectionState.isSensorBeingUsed())
                                    .append("\n");
                    }
                });
            }

            // 这里添加飞行控制器状态的回调，获取更多飞行参数
            flightController.setStateCallback(new FlightControllerState.Callback() {
                @Override
                public void onUpdate(FlightControllerState state) {
                    // 获取飞行器的高度
                    float altitude = state.getAircraftLocation().getAltitude();

                    // 获取飞行器的速度
                    float velocityX = state.getVelocityX();
                    float velocityY = state.getVelocityY();
                    float velocityZ = state.getVelocityZ();
                    //获取飞行器的经纬度
                    double latitude = state.getAircraftLocation().getLatitude();
                    double longitude = state.getAircraftLocation().getLongitude();
                    //DJISampleApplication.getEventBus().post(new LocationUpdateEvent(latitude, longitude));
                    // 获取GPS信号等级
                    GPSSignalLevel gpsSignalLevel = state.getGPSSignalLevel();

                    // 获取姿态角度（俯仰、滚转、偏航）
                    double pitch = state.getAttitude().pitch;
                    double roll = state.getAttitude().roll;
                    double yaw = state.getAttitude().yaw;
                    //写入到MySQL数据库
//                    MySQLDatabaseHelper.getConn();

                    // test
                    MySQLDatabaseHelper.testConnect();
                    /*MySQLDatabaseHelper.insertDroneData(
                            longitude, latitude, velocityX, velocityY, velocityZ,
                            altitude, pitch, roll, yaw
                    );
*/
                    // 将这些飞行参数追加到StringBuilder中
                    stringBuilder.setLength(0); // 清空 StringBuilder 内容
                    stringBuilder.append("Longitude: ").append(longitude).append("°,Latitude: ").append(latitude).append("°\n");
                    stringBuilder.append("Altitude: ").append(altitude).append(" m\n");
                    stringBuilder.append("Velocity: ").append("X, ").append(velocityX).append(" m/s ").append("Y, ")
                            .append(velocityY).append(" m/s ").append("Z, ").append(velocityZ).append(" m/s\n");
                    stringBuilder.append("GPS Signal Level: ").append(gpsSignalLevel.name()).append("\n");
                    stringBuilder.append("Attitude: Pitch ").append(pitch).append("°, Roll ").append(roll)
                            .append("°, Yaw ").append(yaw).append("°\n");

                    // 更新UI或者日志
                    runOnUiThread(() -> changeDescription(stringBuilder.toString()));
                    //post(() -> changeDescription(stringBuilder.toString()));
                    Log.d("Flight Params", stringBuilder.toString());
                }
            });

            // Listen System status
            KeyManager.getInstance().getValue(DiagnosticsKey.create(DiagnosticsKey.SYSTEM_STATUS), new GetCallback() {
                @Override
                public void onSuccess(@NonNull @NotNull Object o) {
                    WarningStatusItem warningStatusItem = (WarningStatusItem) o;
                    stringBuilder.append("warning level: ")
                            .append(warningStatusItem.getWarningLevel())
                            .append("\n");
                    stringBuilder.append("warning message: ")
                            .append(warningStatusItem.getMessage())
                            .append("\n");
                }

                @Override
                public void onFailure(@NonNull @NotNull DJIError djiError) {

                }
            });

            changeDescription(stringBuilder.toString());
        } else {
            Log.i(DJISampleApplication.TAG, "onAttachedToWindow FC NOT Available");
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (ModuleVerificationUtil.isFlightControllerAvailable()) {
            FlightAssistant intelligentFlightAssistant = ((Aircraft) DJISampleApplication.getProductInstance()).getFlightController().getFlightAssistant();
            if(intelligentFlightAssistant != null) {
                intelligentFlightAssistant.setVisionDetectionStateUpdatedCallback(null);
            }
        }
    }

    @Override
    protected int getDescriptionResourceId() {
        return R.string.intelligent_flight_assistant_description;
    }

    @Override
    protected void handleRightBtnClick() {

    }

    @Override
    protected void handleMiddleBtnClick() {

    }

    @Override
    protected int getMiddleBtnTextResourceId() {
        return DISABLE;
    }

    @Override
    protected int getRightBtnTextResourceId() {
        return DISABLE;
    }

    @Override
    protected int getLeftBtnTextResourceId() {
        return DISABLE;
    }

    @Override
    protected void handleLeftBtnClick() {
    }

    @Override
    public int getDescription() {
        return R.string.flight_controller_listview_intelligent_flight_assistant;
    }
}
