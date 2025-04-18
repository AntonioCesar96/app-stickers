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

class StickerModel {
    @SerializedName("image_file")
    final String imageFileName;
    final List<String> emojis;
    @SerializedName("accessibility_text")
    final String accessibilityText;

    StickerModel(String imageFileName, List<String> emojis, String accessibilityText) {
        this.imageFileName = imageFileName;
        this.emojis = emojis;
        this.accessibilityText = accessibilityText;
    }
}
