package com.sasieczno.homeheat.manager.repository.impl;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.LocalTime;

public class LocalTimeSerializer extends JsonSerializer<LocalTime> {

    @Override
    public void serialize(LocalTime localTime, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        if (localTime == null)
            jsonGenerator.writeNull();
        else {
            StringBuilder sb = new StringBuilder().append(localTime.getHour());
            sb.append(':').append(String.format("%02d", localTime.getMinute()));
            sb.append(':').append(String.format("%02d", localTime.getSecond()));
            jsonGenerator.writeString(sb.toString());
        }

    }
}
