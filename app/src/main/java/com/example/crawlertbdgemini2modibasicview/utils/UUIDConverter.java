package com.example.crawlertbdgemini2modibasicview.utils;

import androidx.room.TypeConverter;

import java.util.UUID;

public class UUIDConverter {
    @TypeConverter
    public static UUID fromString(String uuidString) {
        return uuidString == null ? null : UUID.fromString(uuidString);
    }

    @TypeConverter
    public static String uuidToString(UUID uuid) {
        return uuid == null ? null : uuid.toString();
    }
}