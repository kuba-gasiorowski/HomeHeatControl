package com.sasieczno.homeheat.manager.repository.impl;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.math.BigDecimal;

public class DoubleSerializer extends JsonSerializer<Double> {
    @Override
    public void serialize(Double value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        BigDecimal bigDecimal = BigDecimal.valueOf(value);
        gen.writeNumber(bigDecimal.toPlainString());
    }
}
