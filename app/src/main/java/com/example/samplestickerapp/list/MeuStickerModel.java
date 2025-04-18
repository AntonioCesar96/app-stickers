/*
 * Copyright (c) WhatsApp Inc. and its affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.example.samplestickerapp.list;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

class MeuStickerModel implements Parcelable {
    public String imageFileName;
    public List<String> emojis;
    public String accessibilityText;
    public long size;

    MeuStickerModel(String imageFileName, List<String> emojis, String accessibilityText, long size) {
        this.imageFileName = imageFileName;
        this.emojis = emojis;
        this.accessibilityText = accessibilityText;
        this.size = size;
    }

    private MeuStickerModel(Parcel in) {
        imageFileName = in.readString();
        emojis = in.createStringArrayList();
        accessibilityText = in.readString();
        size = in.readLong();
    }

    public static final Creator<MeuStickerModel> CREATOR = new Creator<MeuStickerModel>() {
        @Override
        public MeuStickerModel createFromParcel(Parcel in) {
            return new MeuStickerModel(in);
        }

        @Override
        public MeuStickerModel[] newArray(int size) {
            return new MeuStickerModel[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(imageFileName);
        dest.writeStringList(emojis);
        dest.writeString(accessibilityText);
        dest.writeLong(size);
    }
}
