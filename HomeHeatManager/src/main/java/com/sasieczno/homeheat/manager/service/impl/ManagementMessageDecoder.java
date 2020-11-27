package com.sasieczno.homeheat.manager.service.impl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public enum ManagementMessageDecoder {
    MESSAGE_TIMESTAMP(8, Double.class),
    HEATING_PERIOD(1, Byte.class),
    EXTERNAL_TEMPERATURE(8, Double.class),
    AVG_EXTERNAL_TEMPERATURE(8, Double.class),
    CIRCUIT_INDEX(1, Byte.class),
    CIRCUIT_TEMPERATURE(8, Double.class),
    CIRCUIT_STATUS(1, Boolean.class);
    private int fieldLength;
    private Class fieldType;
    ManagementMessageDecoder(int fieldLength, Class fieldType) {
        this.fieldLength = fieldLength;
        this.fieldType = fieldType;
    }

    public int getFieldLength() {
        return fieldLength;
    }

    public Class getFieldType() {
        return fieldType;
    }

    public <T> T decodeValue(byte[] buffer, int offset) {
        if (fieldType.equals(Double.class)) {
            return (T) Double.valueOf(ByteBuffer.wrap(buffer, offset, fieldLength).order(ByteOrder.LITTLE_ENDIAN).getDouble());
        } else if (fieldType.equals(Long.class)) {
            return (T) Long.valueOf(ByteBuffer.wrap(buffer, offset, fieldLength).order(ByteOrder.LITTLE_ENDIAN).getLong());
        } else if (fieldType.equals(Byte.class)) {
            return (T) Integer.valueOf(buffer[offset]);
        } else if (fieldType.equals(Boolean.class)) {
            if (buffer[offset] == 0)
                return (T) Boolean.FALSE;
            else
                return (T) Boolean.TRUE;
        } else if (fieldType.equals(String.class)) {
            return (T) new String(buffer, offset, fieldLength);
        }
        throw new ClassCastException("Unsupported data type: " + fieldType.getCanonicalName());
    }
}
