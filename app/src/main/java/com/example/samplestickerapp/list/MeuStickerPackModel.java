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

public class MeuStickerPackModel implements Parcelable {
    public String identifier;
    public String name;
    public String publisher;
    public String trayImageFile;
    public String publisherEmail;
    public String publisherWebsite;
    public String privacyPolicyWebsite;
    public String licenseAgreementWebsite;
    public String imageDataVersion;
    public boolean avoidCache;
    public boolean animatedStickerPack;

    public  List<MeuStickerModel> stickers;
    public long totalSize;

    MeuStickerPackModel(String identifier, String name, String publisher, String trayImageFile, String publisherEmail,
                        String publisherWebsite, String privacyPolicyWebsite, String licenseAgreementWebsite,
                        String imageDataVersion, boolean avoidCache, boolean animatedStickerPack) {
        this.identifier = identifier;
        this.name = name;
        this.publisher = publisher;
        this.trayImageFile = trayImageFile;
        this.publisherEmail = publisherEmail;
        this.publisherWebsite = publisherWebsite;
        this.privacyPolicyWebsite = privacyPolicyWebsite;
        this.licenseAgreementWebsite = licenseAgreementWebsite;
        this.imageDataVersion = imageDataVersion;
        this.avoidCache = avoidCache;
        this.animatedStickerPack = animatedStickerPack;
    }

    private MeuStickerPackModel(Parcel in) {
        identifier = in.readString();
        name = in.readString();
        publisher = in.readString();
        trayImageFile = in.readString();
        publisherEmail = in.readString();
        publisherWebsite = in.readString();
        privacyPolicyWebsite = in.readString();
        licenseAgreementWebsite = in.readString();
        stickers = in.createTypedArrayList(MeuStickerModel.CREATOR);
        totalSize = in.readLong();
        imageDataVersion = in.readString();
        avoidCache = in.readByte() != 0;
        animatedStickerPack = in.readByte() != 0;
    }

    public static final Creator<MeuStickerPackModel> CREATOR = new Creator<MeuStickerPackModel>() {
        @Override
        public MeuStickerPackModel createFromParcel(Parcel in) {
            return new MeuStickerPackModel(in);
        }

        @Override
        public MeuStickerPackModel[] newArray(int size) {
            return new MeuStickerPackModel[size];
        }
    };

    void setStickers(List<MeuStickerModel> stickers) {
        this.stickers = stickers;
        totalSize = 0;
        for (MeuStickerModel sticker : stickers) {
            totalSize += sticker.size;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(identifier);
        dest.writeString(name);
        dest.writeString(publisher);
        dest.writeString(trayImageFile);
        dest.writeString(publisherEmail);
        dest.writeString(publisherWebsite);
        dest.writeString(privacyPolicyWebsite);
        dest.writeString(licenseAgreementWebsite);
        dest.writeTypedList(stickers);
        dest.writeLong(totalSize);
        dest.writeString(imageDataVersion);
        dest.writeByte((byte) (avoidCache ? 1 : 0));
        dest.writeByte((byte) (animatedStickerPack ? 1 : 0));

    }
}
