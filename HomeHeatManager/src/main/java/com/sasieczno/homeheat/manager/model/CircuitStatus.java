package com.sasieczno.homeheat.manager.model;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.IOException;

@NoArgsConstructor
@Getter
@Setter
public class CircuitStatus {
    private int circuitIndex;
    private CircuitMode circuitStatus;
    private boolean heatingOn;
    private String circuitName;
    private double circuitTemperature;

    public static class CircuitStatusDeserializer extends JsonDeserializer<CircuitMode> {

        @Override
        public CircuitMode deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
            String value = p.getText();
            try {
                return CircuitMode.valueOf(value);
            } catch (IllegalArgumentException e) {
                throw new IOException("Unexpected CircuitMode value: " + value, e);
            }
        }
    }
}