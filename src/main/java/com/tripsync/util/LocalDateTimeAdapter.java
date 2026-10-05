package com.tripsync.util;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LocalDateTimeAdapter extends TypeAdapter<LocalDateTime> {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Override
    public void write(JsonWriter out, LocalDateTime value) throws IOException {
        out.value(value == null ? null : value.format(FORMAT));
    }

    @Override
    public LocalDateTime read(JsonReader in) throws IOException {
        String s = in.nextString();
        return s == null || s.isBlank() ? null : LocalDateTime.parse(s, FORMAT);
    }
}
