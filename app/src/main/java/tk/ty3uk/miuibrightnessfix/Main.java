package tk.ty3uk.miuibrightnessfix;

import android.app.AndroidAppHelper;
import android.content.Context;
import android.content.res.XModuleResources;
import android.content.res.XResources;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import android.hardware.SensorEvent;
import android.os.Build;
import android.os.SystemClock;

import java.lang.reflect.Method;

public class Main implements IXposedHookZygoteInit, IXposedHookLoadPackage {
    @Override
    public void initZygote(IXposedHookZygoteInit.StartupParam startupParam) throws Throwable {
        XModuleResources modRes = XModuleResources.createInstance(startupParam.modulePath, null);

        XSharedPreferences pref = new XSharedPreferences("tk.ty3uk.miuibrightnessfix", "levels");
        pref.makeWorldReadable();

        if (pref.contains("autoBrightnessLevels") && pref.contains("autoBrightnessLcdBacklightValues")) {
            try {
                XResources.setSystemWideReplacement("android", "array", "config_autoBrightnessLevels", Util.StringToIntArray(pref.getString("autoBrightnessLevels", "")));
                XResources.setSystemWideReplacement("android", "array", "config_autoBrightnessLcdBacklightValues", Util.StringToIntArray(pref.getString("autoBrightnessLcdBacklightValues", "")));
            } catch (NumberFormatException nfe) {
                nfe.printStackTrace();
            }
        } else {
            XResources.setSystemWideReplacement("android", "array", "config_autoBrightnessLevels", modRes.fwd(R.array.config_autoBrightnessLevels));
            XResources.setSystemWideReplacement("android", "array", "config_autoBrightnessLcdBacklightValues", modRes.fwd(R.array.config_autoBrightnessLcdBacklightValues));
        }
    }

    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals("android"))
            return;

        final Class<?> AutomaticBrightnessController = XposedHelpers.findClass("com.android.server.display.AutomaticBrightnessController", lpparam.classLoader);
        final Class<?> AutomaticBrightnessController$1 = XposedHelpers.findClass("com.android.server.display.AutomaticBrightnessController$1", lpparam.classLoader);
        final Class<?> DisplayPowerController = XposedHelpers.findClass("com.android.server.display.DisplayPowerController", lpparam.classLoader);

        final long BRIGHTENING_LIGHT_DEBOUNCE = 0x07D0;
        final long DARKENING_LIGHT_DEBOUNCE = 0x0FA0;

        final int LOW_DIMMING_PROTECTION_THRESHOLD = 1;

        try {
            XposedHelpers.findAndHookMethod(AutomaticBrightnessController$1, "onSensorChanged", SensorEvent.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    long mPrevLogTime;
                    float mPrevLogLux;

                    try {
                        mPrevLogTime = XposedHelpers.getLongField(param.thisObject, "mPrevLogTime");
                        mPrevLogLux = XposedHelpers.getFloatField(param.thisObject, "mPrevLogLux");

                        Object this$0 = XposedHelpers.getObjectField(param.thisObject, "this$0");
                        SensorEvent paramSensorEvent = (SensorEvent) param.args[0];

                        boolean mLightSensorEnabled = (boolean) XposedHelpers.callStaticMethod(AutomaticBrightnessController, "access$200", this$0);

                        if (mLightSensorEnabled) {
                            long time = SystemClock.uptimeMillis();
                            float lux = paramSensorEvent.values[0];

                            XposedHelpers.callStaticMethod(AutomaticBrightnessController, "access$302", lux);
                            XposedHelpers.callStaticMethod(AutomaticBrightnessController, "access$400", this$0, time, lux);

                            mPrevLogTime = XposedHelpers.getLongField(param.thisObject, "mPrevLogTime");
                            mPrevLogLux = XposedHelpers.getFloatField(param.thisObject, "mPrevLogLux");

                            if ((time - mPrevLogTime >= 500L) || (1.2F * mPrevLogLux <= lux) || (lux * 1.2F <= mPrevLogLux)) {
                                //XposedBridge.log("time: " + time + " | mPrevLogTime: " + mPrevLogTime);

                                XposedHelpers.setLongField(param.thisObject, "mPrevLogTime", time);
                                XposedHelpers.setFloatField(param.thisObject, "mPrevLogLux", lux);
                            }
                        }
                    } catch (NoSuchFieldError e) {
                        //XposedBridge.log("No such fields: mPrevLogTime, mPrevLogLux");
                    }
                }
            });
        } catch (NoSuchMethodError e) {
            XposedBridge.log("No such method: onSensorChanged");
        }

        try {
            XposedHelpers.findAndHookMethod(AutomaticBrightnessController, "nextAmbientLightBrighteningTransition", long.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Object mAmbientLightRingBuffer = XposedHelpers.getObjectField(param.thisObject, "mAmbientLightRingBuffer");

                    int i = (int) XposedHelpers.callMethod(mAmbientLightRingBuffer, "size");
                    long l = (long) param.args[0];

                    float lux = 0f, mBrighteningLuxThreshold;

                    for (int j = i - 1; ; j--) {
                        try {
                            lux = (float) XposedHelpers.callMethod(mAmbientLightRingBuffer, "getLux", j);
                        } catch(ArrayIndexOutOfBoundsException e) {
                            //e.printStackTrace();
                        }
                        mBrighteningLuxThreshold = (float) XposedHelpers.getObjectField(param.thisObject, "mBrighteningLuxThreshold");

                        if ((j < 0) || (lux <= mBrighteningLuxThreshold)) {
                            //XposedBridge.log("nextAmbientLightBrighteningTransition result: " + (BRIGHTENING_LIGHT_DEBOUNCE + l));
                            param.setResult(BRIGHTENING_LIGHT_DEBOUNCE + l);
                            return;
                        }

                        l = (long) XposedHelpers.callMethod(mAmbientLightRingBuffer, "getTime", j);
                    }
                }
            });
        } catch (NoSuchMethodError e) {
            XposedBridge.log("No such method: nextAmbientLightBrighteningTransition");
        }

        try {
            XposedHelpers.findAndHookMethod(AutomaticBrightnessController, "nextAmbientLightDarkeningTransition", long.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Object mAmbientLightRingBuffer = XposedHelpers.getObjectField(param.thisObject, "mAmbientLightRingBuffer");

                    int i = (int) XposedHelpers.callMethod(mAmbientLightRingBuffer, "size");
                    long l = (long) param.args[0];

                    float lux = 0f, mDarkeningLuxThreshold;

                    for (int j = i - 1; ; j--) {
                        try {
                            lux = (float) XposedHelpers.callMethod(mAmbientLightRingBuffer, "getLux", j);
                        } catch (ArrayIndexOutOfBoundsException e) {
                            //e.printStackTrace();
                        }
                        mDarkeningLuxThreshold = (float) XposedHelpers.getObjectField(param.thisObject, "mDarkeningLuxThreshold");

                        if ((j < 0) || (lux >= mDarkeningLuxThreshold)) {
                            //XposedBridge.log("nextAmbientLightDarkeningTransition result: " + (DARKENING_LIGHT_DEBOUNCE + l));
                            param.setResult(DARKENING_LIGHT_DEBOUNCE + l);
                            return;
                        }

                        l = (long) XposedHelpers.callMethod(mAmbientLightRingBuffer, "getTime", j);
                    }
                }
            });
        } catch (NoSuchMethodError e) {
            XposedBridge.log("No such method: nextAmbientLightDarkeningTransition");
        }

        try {
            XposedHelpers.findAndHookMethod(DisplayPowerController, "protectedMinimumBrightness", XC_MethodReplacement.returnConstant(LOW_DIMMING_PROTECTION_THRESHOLD));
        } catch (NoSuchMethodError e) {
            XposedBridge.log("No such method: protectedMinimumBrightness");
        }
    }
}