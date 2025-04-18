/*
 * Copyright (c) WhatsApp Inc. and its affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.example.samplestickerapp;

import com.google.gson.annotations.SerializedName;

import java.util.List;

class ContentsModel {
    @SerializedName("android_play_store_link")
    public String androidPlayStoreLink;
    @SerializedName("ios_app_store_link")
    public String iosAppStoreLink;
    @SerializedName("sticker_packs")
    public List<StickerPackModel> stickerPacks;
}
