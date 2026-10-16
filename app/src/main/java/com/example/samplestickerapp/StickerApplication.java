/*
 * Copyright (c) WhatsApp Inc. and its affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.example.samplestickerapp;

import android.app.Application;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.facebook.drawee.backends.pipeline.Fresco;

public class StickerApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Fresco.initialize(this);
        FilesHelper.setContext(this);
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            private int started;
            private boolean returning;

            @Override public void onActivityStarted(Activity activity) {
                if (started++ == 0) returning = true;
            }

            @Override public void onActivityResumed(Activity activity) {
                if (!returning) return;
                returning = false;
                if (activity instanceof CropVideoActivity || activity instanceof SplashActivity) return;
                CompressionJob pending = CompressionJob.pending(StickerApplication.this);
                if (pending != null) {
                    try {
                        activity.startActivity(pending.screen(activity).addFlags(
                                Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
                    } catch (Exception e) { android.util.Log.w("CompressionJob", "Recuperação do editor", e); }
                }
            }

            @Override public void onActivityStopped(Activity activity) { started = Math.max(0, started - 1); }
            @Override public void onActivityCreated(Activity activity, Bundle state) { }
            @Override public void onActivityPaused(Activity activity) { }
            @Override public void onActivitySaveInstanceState(Activity activity, Bundle state) { }
            @Override public void onActivityDestroyed(Activity activity) { }
        });
    }
}
