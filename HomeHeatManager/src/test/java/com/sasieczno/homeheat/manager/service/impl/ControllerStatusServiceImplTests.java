package com.sasieczno.homeheat.manager.service.impl;

import com.sasieczno.homeheat.manager.model.HeatingData;
import com.sasieczno.homeheat.manager.model.HeatingPeriod;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ControllerStatusServiceImplTests {

    @Test
    public void test_decodeManagementMessage_0() {
        ControllerStatusServiceImpl obj = new ControllerStatusServiceImpl();
        // timestamp: 1606295646.886288 -> 2020-11-25 10:14:06.886
        byte[] timestampbuf = new byte[]{(byte)0xf1, (byte)0xb8, (byte)0xb8, (byte)0x17, (byte)0x88, (byte)0xef, (byte)0xd7, (byte)0x41};
        System.arraycopy(timestampbuf,
                0, obj.buf, 0, 8);

        // period = night
        obj.buf[8] = 0x01;
        HeatingData hs = obj.decodeManagementMessage(9);
        Assertions.assertEquals(1606295646886L, hs.getLastMessageTime().getTimeInMillis());
        Assertions.assertEquals(HeatingPeriod.NIGHT, hs.getHeatingPeriod());
    }

    public String bytesToHex(byte[] bytes) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < bytes.length; i++) {
            sb.append(Character.forDigit((bytes[i] >> 4) & 0x0F , 16));
            sb.append(Character.forDigit(bytes[i] & 0x0F , 16));
        }
        return sb.toString();
    }
}
