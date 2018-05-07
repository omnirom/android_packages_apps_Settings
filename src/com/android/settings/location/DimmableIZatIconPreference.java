/* Copyright (c) 2016, The Linux Foundation. All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
      copyright notice, this list of conditions and the following
      disclaimer in the documentation and/or other materials provided
      with the distribution.
    * Neither the name of The Linux Foundation nor the names of its
      contributors may be used to endorse or promote products derived
      from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. */

package com.android.settings.location;

import android.content.Context;
import android.graphics.drawable.Drawable;
import androidx.preference.PreferenceViewHolder;
import com.android.settingslib.location.InjectedSetting;
import android.util.Log;
import com.android.settingslib.widget.apppreference.AppPreference;
import com.android.settings.widget.RestrictedAppPreference;
import dalvik.system.DexClassLoader;
import java.lang.ClassNotFoundException;
import java.lang.ExceptionInInitializerError;
import java.lang.IllegalAccessException;
import java.lang.IllegalArgumentException;
import java.lang.LinkageError;
import java.lang.NoSuchFieldException;
import java.lang.NoSuchMethodException;
import java.lang.NullPointerException;
import java.lang.SecurityException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class DimmableIZatIconPreference {
    private static final String TAG = "DimmableIZatIconPreference";
    private static Class mXtProxyClz;
    private static Class mNotifierClz;
    private static Method mGetXtProxyMethod;
    private static Method mGetConsentMethod;
    private static Method mShowIzatMethod;
    private static String mIzatPackage;
    private static DexClassLoader mLoader;

    private static void load(Context context) {
        if (mLoader == null) {
            try {
                if (mXtProxyClz == null || mNotifierClz == null) {
                    mLoader = new DexClassLoader("/system/framework/izat.xt.srv.jar",
                                                 context.getFilesDir().getAbsolutePath(),
                                                 null,
                                                 ClassLoader.getSystemClassLoader());
                    mXtProxyClz = Class.forName("com.qti.izat.XTProxy",
                                                true,
                                                mLoader);
                    mNotifierClz = Class.forName("com.qti.izat.XTProxy$Notifier",
                                                 true,
                                                 mLoader);
                    mIzatPackage = (String)mXtProxyClz.getField("IZAT_XT_PACKAGE").get(null);
                    mGetXtProxyMethod = mXtProxyClz.getMethod("getXTProxy",
                                                              Context.class,
                                                              mNotifierClz);
                    mGetConsentMethod = mXtProxyClz.getMethod("getUserConsent");
                    mShowIzatMethod = mXtProxyClz.getMethod("showIzat",
                                                            Context.class,
                                                            String.class);
                }
            } catch (NoSuchMethodException | NullPointerException | SecurityException |
                     NoSuchFieldException | LinkageError | IllegalAccessException |
                     ClassNotFoundException e) {
                mXtProxyClz = null;
                mNotifierClz = null;
                mIzatPackage = null;
                mGetXtProxyMethod = null;
                mGetConsentMethod = null;
                mShowIzatMethod = null;
                e.printStackTrace();
            }
        }
    }

    static boolean showIzat(Context context, String packageName) {
        load(context);
        boolean show = true;
        try {
            if (mShowIzatMethod != null) {
                show = (Boolean)mShowIzatMethod.invoke(null, context, packageName);
            }
        } catch (IllegalAccessException | IllegalArgumentException |
                 InvocationTargetException | ExceptionInInitializerError e) {
            e.printStackTrace();
        }
        return show;
    }

    private static boolean isIzatPackage(Context context, InjectedSetting info) {
        return (mIzatPackage != null && mIzatPackage.equals(info.packageName));
    }

    private static final int ICON_ALPHA_ENABLED = 255;
    private static final int ICON_ALPHA_DISABLED = 102;

    private static void dimIcon(AppPreference pref, boolean dimmed) {
        Drawable icon = pref.getIcon();
        if (icon != null) {
            icon.mutate().setAlpha(dimmed ? ICON_ALPHA_DISABLED : ICON_ALPHA_ENABLED);
            pref.setIcon(icon);
        }
    }

    private static class IZatAppPreference extends AppPreference {
        private boolean mChecked;
        private IZatAppPreference(Context context) {
            super(context);
            Object notifier = Proxy.newProxyInstance(mLoader,
                                                     new Class[] { mNotifierClz },
                                                     new InvocationHandler() {
                @Override   
                public Object invoke(Object proxy, Method method, Object[] args)
                    throws Throwable {
                    if (method.getName().equals("userConsentNotify") &&
                        args[0] != null && args[0] instanceof Boolean) {
                        boolean consent = (Boolean)args[0];
                        if (mChecked != consent) {
                            mChecked = consent;
                            dimIcon(IZatAppPreference.this, !isEnabled() || !mChecked);
                        }
                    }
                    return null;
                }});

            try {
                Object xt = mGetXtProxyMethod.invoke(null, context, notifier);
                mChecked = (Boolean)mGetConsentMethod.invoke(xt);
            } catch (IllegalAccessException | IllegalArgumentException |
                     InvocationTargetException | ExceptionInInitializerError e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onBindViewHolder(PreferenceViewHolder view) {
            super.onBindViewHolder(view);
            DimmableIZatIconPreference.dimIcon(this, !isEnabled() || !mChecked);
        }
    }

    private static class IZatRestrictedAppPreference extends RestrictedAppPreference {
        private boolean mChecked;
        private IZatRestrictedAppPreference(Context context, String userRestriction) {
            super(context, userRestriction);
            Object notifier = Proxy.newProxyInstance(mLoader,
                                                     new Class[] { mNotifierClz },
                                                     new InvocationHandler() {
                @Override   
                public Object invoke(Object proxy, Method method, Object[] args)
                    throws Throwable {
                    if (method.getName().equals("userConsentNotify") &&
                        args[0] != null && args[0] instanceof Boolean) {
                        boolean consent = (Boolean)args[0];
                        if (mChecked != consent) {
                            mChecked = consent;
                            dimIcon(IZatRestrictedAppPreference.this, !isEnabled() || !mChecked);
                        }
                    }
                    return null;
                }});

            try {
                Object xt = mGetXtProxyMethod.invoke(null, context, notifier);
                mChecked = (Boolean)mGetConsentMethod.invoke(xt);
            } catch (IllegalAccessException | IllegalArgumentException |
                     InvocationTargetException | ExceptionInInitializerError e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onBindViewHolder(PreferenceViewHolder view) {
            super.onBindViewHolder(view);
            DimmableIZatIconPreference.dimIcon(this, !isEnabled() || !mChecked);
        }
    }

    static AppPreference getAppPreference(Context context, InjectedSetting info) {
        return isIzatPackage(context, info) ?
                new IZatAppPreference(context) :
                new AppPreference(context);
    }

    static RestrictedAppPreference getRestrictedAppPreference(Context context, InjectedSetting info) {
        return isIzatPackage(context, info) ?
                new IZatRestrictedAppPreference(context, info.userRestriction) :
                new RestrictedAppPreference(context, info.userRestriction);
    }
}
