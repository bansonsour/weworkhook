package org.gallonyin.weworkhk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Created by gallonyin on 2018/6/13.
 */

public class WeXinHook {
    private static final String TAG = "WeXinKKKKK";

    private ClassLoader classLoader;

    private float salt = 0.00005f;
    private float defLa = 23.008083f;
    private float defLo = 113.347741f;
    private float latitude = 0;
    private float longtitude = 0;
    private boolean isOpen = true;

    public void start(ClassLoader classLoader) {
        this.classLoader = classLoader;

        hkStart();
    }

    private void hkStart() {
        Log.e(TAG, "hkStart()");
        XposedHelpers.findAndHookMethod("android.app.Application", classLoader, "attach", Context.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Log.e(TAG, "===========afterHookedMethod()======");
                Context context = (Context) param.args[0];

                SharedPreferences sp = context.getSharedPreferences("hkWeWork", Context.MODE_PRIVATE);
                initReceiver(context, sp);
                latitude = sp.getFloat("GPSLatitude", defLa);
                longtitude = sp.getFloat("GPSLongitude", defLo);
                hkGPS(classLoader);

            }
        });
    }

    private void initReceiver(Context context, final SharedPreferences sp) {
        BroadcastReceiver receiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                Log.e(TAG, "onReceive: " + intent.getAction());
                String action = intent.getAction();
                if (action == null) return;
                String data;
                switch (action) {
                    case "weworkdk_gps":
                        data = intent.getStringExtra("data");
                        Log.e(TAG, "============onReceive=====: " + data);
                        String[] split = data.split("#");
                        if (split.length != 2) return;
                        float la = Float.parseFloat(split[0]);
                        float lo = Float.parseFloat(split[1]);
                        sp.edit().putFloat("GPSLatitude", la)
                                .putFloat("GPSLongitude", lo)
                                .apply();
                        break;
                    case "weworkdk_open":
                        isOpen = intent.getBooleanExtra("open", true);
                        Log.e(TAG, "===========initReceiver() isOpen======" + isOpen);
                        break;
                }
            }
        };

        IntentFilter filter = new IntentFilter("weworkdk_gps");
        filter.addAction("weworkdk_pic");
        filter.addAction("weworkdk_open");
        context.registerReceiver(receiver, filter);
    }

    private float saltedLa(float f) {
        if (f > 0) {
            return (float) (f + 0.002082f + salt * (1 - (Math.random() * 2)));
        }
        return f;
    }

    private float saltedLo(float f) {
        if (f > 0) {
            return (float) (f + -0.005203f + salt * (1 - (Math.random() * 2)));
        }
        return f;
    }

    private void hkGPS(final ClassLoader classLoader) {
        //// 反classes11.dex
        Log.d(TAG, "=====hkGPS======: " + latitude + "#" + longtitude);
        //com/tencent/map/geolocation/sapp/TencentLocationListener
        try {
            final Class tencentLocationClazz = classLoader.loadClass("com.tencent.map.geolocation.sapp.TencentLocation");

            XposedHelpers.findAndHookMethod(XposedHelpers.findClass("com.tencent.map.geolocation.sapp.TencentLocationListener", classLoader),
                    "onLocationChanged", tencentLocationClazz, int.class, String.class, new XC_MethodHook() {

                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            Log.e(TAG, "Hook->TencentLocationListener->onLocationChanged():beforeHookedMethod:" + param.getClass());
                            if (param != null && param.args != null && param.args.length > 0) {
                                for (int i = 0; i < param.args.length; i++) {
                                    Log.e(TAG, "Hook onLocationChanged(beforeHookedMethod) -> param.args[i]:" +
                                            param.args[i].getClass() + "::::" + param.args[i]);
                                }

                                try {
                                    final Class txLocationClazz = param.args[0].getClass();
                                    //Object aMloc = XposedHelpers.newInstance(txLocationClazz);
                                    for (Method method : txLocationClazz.getDeclaredMethods()) {

                                        Log.e(TAG,"Hook->TxLocation-->method====" + method.getName());

                                        if (method.getName().equals("setLocationType") && !Modifier.isAbstract(method.getModifiers())) {
                                            Log.e(TAG,"Hook->TxLocation-->setLocationType");
                                            method.invoke( param.args[0], 2);
                                        }

                                        if (method.getName().equals("setLatitude") && !Modifier.isAbstract(method.getModifiers())) {
                                            Log.e(TAG,"Hook->TxLocation-->setLatitude" + latitude);
                                            method.invoke( param.args[0], latitude);
                                        }
                                        if (method.getName().equals("setLongitude") && !Modifier.isAbstract(method.getModifiers())) {
                                            Log.e(TAG,"Hook->TxLocation-->setLongitude" + longtitude);
                                            method.invoke( param.args[0], longtitude);
                                        }

                                        if (method.getName().equals("getLatitude") && !Modifier.isAbstract(method.getModifiers())) {
                                            Log.e(TAG,"Hook->TxLocation-->getLatitude::::" +  method.invoke( param.args[0]));
                                        }
                                        if (method.getName().equals("getLongitude") && !Modifier.isAbstract(method.getModifiers())) {
                                            Log.e(TAG,"Hook->TxLocation-->getLongitude::::" + method.invoke( param.args[0]));
                                        }


                                    }

                                    for (Field field : txLocationClazz.getDeclaredFields()) {
                                        Log.e(TAG,"Hook->TxLocation-->field====" + field.getName());
                                    }

                                    Method getLatitude = txLocationClazz.getDeclaredMethod("getLatitude");
                                    Method getLongitude = txLocationClazz.getDeclaredMethod("getLongitude");
                                    if (getLatitude != null && getLongitude != null) {
                                        XposedBridge.hookMethod(getLatitude, new XC_MethodHook() {
                                            @Override
                                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                                try {
                                                    Log.e(TAG, "Hook ----->>>>>>>>>>>>>>>>>getLatitude(beforeHookedMethod) " + param);
                                                    param.setResult((double)latitude);
                                                } catch (Throwable e) {
                                                    e.printStackTrace();
                                                }

                                            }
                                        });

                                        XposedBridge.hookMethod(getLongitude, new XC_MethodHook() {
                                            @Override
                                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                                try {
                                                    Log.e(TAG, "Hook ----->>>>>>>>>>>>>>>>>getLongitude(beforeHookedMethod) " + param);
                                                    param.setResult((double)longtitude);
                                                } catch (Throwable e) {
                                                    e.printStackTrace();
                                                }

                                            }
                                        });
                                    }

                                    param.args[0] =  param.args[0];

                                } catch (Throwable e) {
                                    e.printStackTrace();
                                }

                            }

                        }

                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            Log.e(TAG, "Hook->TencentLocationListener->onLocationChanged():afterHookedMethod:" + param.getClass());
                            if (param != null && param.args != null && param.args.length > 0) {
                                for (int i = 0; i < param.args.length; i++) {
                                    Log.e(TAG, "Hook onLocationChanged(afterHookedMethod) -> param.args[i]:"
                                            + param.args[i].getClass() + "::::" + param.args[i]);
                                }
                            }
                        }
                    });

            Log.d(TAG, "========================================================================");
            final Class requestClazz = classLoader.loadClass("com.tencent.map.geolocation.sapp.TencentLocationRequest");
            final Class listenerClazz = classLoader.loadClass("com.tencent.map.geolocation.sapp.TencentLocationListener");
            XposedHelpers.findAndHookMethod(XposedHelpers.findClass("com.tencent.map.geolocation.sapp.TencentLocationManager", classLoader),
                    "requestLocationUpdates", requestClazz, listenerClazz, new XC_MethodHook() {

                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            Log.d(TAG, "Hook->TencentLocationManager->requestLocationUpdates():beforeHookedMethod:" + param.getClass());
                            if (param != null && param.args != null && param.args.length > 0) {
                                for (int i = 0; i < param.args.length; i++) {
                                    Log.d(TAG, "Hook requestLocationUpdates(beforeHookedMethod) -> param.args[i]:" +
                                            param.args[i].getClass() + "::::" + param.args[i]);
                                }

                            }

                        }

                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            Log.d(TAG, "Hook->TencentLocationManager->requestLocationUpdates():afterHookedMethod:" + param.getClass());
                            if (param != null && param.args != null && param.args.length > 0) {
                                for (int i = 0; i < param.args.length; i++) {
                                    Log.d(TAG, "Hook requestLocationUpdates(afterHookedMethod) -> param.args[i]:"
                                            + param.args[i].getClass() + "::::" + param.args[i]);
                                }
                            }
                        }
                    });


        } catch (ClassNotFoundException e) {
            Log.e(TAG, "Hook->ClassNotFoundException" );
            e.printStackTrace();
        } catch (Throwable t) {
            System.out.println("Hook->Throwable" + t.getMessage());
            t.printStackTrace();
        }
    }

}
