package com.sasieczno.homeheat.manager.repository.impl;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import java.io.IOException;
import java.time.LocalTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LocalTimeDeserializer extends JsonDeserializer<LocalTime> {

    private final Pattern timePattern = Pattern.compile("(\\d{1,2})(:(\\d{1,2})){0,1}(:(\\d{1,2})){0,1}");

    @Override
    public LocalTime deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
        String text = jsonParser.getText();
        Matcher m = timePattern.matcher(text);
        int hour = 0;
        int minute = 0;
        int sec = 0;
        if (m.matches()) {
            try {
                hour = Integer.parseInt(m.group(1));
                if (hour < 0 || hour > 23) {
                    throw new InvalidFormatException(jsonParser, "Invalid hour in time", text, LocalTime.class);
                }
            } catch (NumberFormatException e) {
                throw new InvalidFormatException(jsonParser, "Invalid hour in time: " + e.getMessage(), text, LocalTime.class);
            }
            if (m.group(3) != null) {
                try {
                    minute = Integer.parseInt(m.group(3));
                    if (minute < 0 || minute > 59) {
                        throw new InvalidFormatException(jsonParser, "Invalid minute in time", text, LocalTime.class);
                    }
                } catch (NumberFormatException e) {
                    throw new InvalidFormatException(jsonParser, "Invalid minute in time: " + e.getMessage(), text, LocalTime.class);
                }
                if (m.group(5) != null) {
                    try {
                        sec = Integer.parseInt(m.group(5));
                        if (sec < 0 || sec > 59) {
                            throw new InvalidFormatException(jsonParser, "Invalid second in time", text, LocalTime.class);
                        }
                    } catch (NumberFormatException e) {
                        throw new InvalidFormatException(jsonParser, "Invalid second in time: " + e.getMessage(), text, LocalTime.class);
                    }
                }
            }
        } else {
            throw new InvalidFormatException(jsonParser, "Invalid time format", text, LocalTime.class);
        }
        return LocalTime.of(hour, minute, sec);
    }
}
