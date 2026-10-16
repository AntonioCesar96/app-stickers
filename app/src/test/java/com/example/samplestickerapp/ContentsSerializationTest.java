package com.example.samplestickerapp;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;

import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import static org.junit.Assert.*;

public class ContentsSerializationTest {
    @Test public void contentsRetainTheSchemaReadByTheProvider() {
        StickerPackModel pack = new StickerPackModel("pack-id", "Pack name", "Publisher", "icone.png",
                "", "", "", "", "1", false, true);
        pack.setStickers(Arrays.asList(new StickerModel("01_test.webp", Arrays.asList("test"), "Example")));
        ContentsModel contents = new ContentsModel();
        contents.androidPlayStoreLink = "";
        contents.iosAppStoreLink = "";
        contents.stickerPacks = Arrays.asList(pack);
        JsonObject root = JsonParser.parseString(new Gson().toJson(contents)).getAsJsonObject();
        JsonObject serializedPack = root.getAsJsonArray("sticker_packs").get(0).getAsJsonObject();
        assertEquals("pack-id", serializedPack.get("identifier").getAsString());
        assertEquals("Pack name", serializedPack.get("name").getAsString());
        assertEquals("Publisher", serializedPack.get("publisher").getAsString());
        JsonObject sticker = serializedPack.getAsJsonArray("stickers").get(0).getAsJsonObject();
        assertEquals("01_test.webp", sticker.get("image_file").getAsString());
        assertEquals("test", sticker.getAsJsonArray("emojis").get(0).getAsString());
        assertEquals("Example", sticker.get("accessibility_text").getAsString());
    }

    @Test public void everyPersistedFieldHasAnExplicitNameProtectedByGsonShrinkRules() {
        for (Class<?> model : new Class<?>[]{ContentsModel.class, StickerPackModel.class, StickerModel.class}) {
            for (Field field : model.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) continue;
                assertNotNull(model.getSimpleName() + "." + field.getName() + " needs a stable JSON name",
                        field.getAnnotation(SerializedName.class));
            }
        }
    }
}
