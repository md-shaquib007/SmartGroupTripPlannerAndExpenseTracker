package com.tripsync.util;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class LocalDateAdapter extends TypeAdapter<LocalDate> {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    @Override
    public void write(JsonWriter out, LocalDate value) throws IOException {
        out.value(value == null ? null : value.format(FORMAT));
    }

    @Override
    public LocalDate read(JsonReader in) throws IOException {
        String s = in.nextString();
        return s == null || s.isBlank() ? null : LocalDate.parse(s, FORMAT);
    }
}
