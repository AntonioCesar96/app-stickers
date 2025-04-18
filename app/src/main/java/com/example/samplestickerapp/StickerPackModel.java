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

class StickerPackModel{
    final String identifier;
    final String name;
    final String publisher;
    @SerializedName("tray_image_file") final String trayImageFile;
    @SerializedName("publisher_email") final String publisherEmail;
    @SerializedName("publisher_website") final String publisherWebsite;
    @SerializedName("privacy_policy_website") final String privacyPolicyWebsite;
    @SerializedName("license_agreement_website") final String licenseAgreementWebsite;
    @SerializedName("image_data_version") final String imageDataVersion;
    @SerializedName("avoid_cache") final boolean avoidCache;
    @SerializedName("animated_sticker_pack") final boolean animatedStickerPack;
    private List<StickerModel> stickers;

    StickerPackModel(String identifier, String name, String publisher, String trayImageFile, String publisherEmail,
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

    void setStickers(List<StickerModel> stickers) {
        this.stickers = stickers;
    }
}
