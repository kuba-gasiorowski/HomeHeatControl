package com.sasieczno.homeheat.manager.service.impl;

import com.sasieczno.homeheat.manager.config.AppConfig;
import com.sasieczno.homeheat.manager.model.HeatingData;
import com.sasieczno.homeheat.manager.model.HeatingPeriod;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;

public class ControllerStatusServiceImplTests {

    @Test
    public void test_decodeManagementMessage_0() {
        ControllerStatusServiceImpl obj = new ControllerStatusServiceImpl(new AppConfig());
        // timestamp: 1606295646.886288 -> 2020-11-25 10:14:06.886
//        byte[] timestampbuf = new byte[]{(byte)0xf1, (byte)0xb8, (byte)0xb8, (byte)0x17, (byte)0x88, (byte)0xef, (byte)0xd7, (byte)0x41};
        byte[] timestampbuf = new byte[]{(byte)0x41, (byte)0xd7, (byte)0xef, (byte)0x88, (byte)0x17, (byte)0xb8, (byte)0xb8, (byte)0xf1};
        System.arraycopy(timestampbuf,
                0, obj.buf, 0, 8);

        // period = night
        obj.buf[8] = 0x01;
        HeatingData hs = obj.decodeManagementMessage(9);
        Assertions.assertEquals(1606295646886L, hs.getLastMessageTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        Assertions.assertEquals(HeatingPeriod.NIGHT, hs.getHeatingPeriod());
    }

    @Test
    public void test_decodeManagementMessage_1() {
        ControllerStatusServiceImpl obj = new ControllerStatusServiceImpl(new AppConfig());
        byte[] doublebuf = new byte[]{(byte)'A', (byte)0xd8, (byte)0x89, (byte)0x96, (byte)0x9b, (byte)',', (byte)'V', (byte)0xb7};
        System.arraycopy(doublebuf, 0, obj.buf, 0, 8);
        obj.buf[8] = 0x01;
        doublebuf = new byte[]{(byte)'?', (byte)0xf0, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)'@', (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00};
        System.arraycopy(doublebuf, 0, obj.buf, 9, 16);
        HeatingData hs = obj.decodeManagementMessage(25);
        System.out.println("HeatingData: lmt=" + hs.getLastMessageTime() + ", hp=" + hs.getHeatingPeriod()
        + ", et=" + hs.getExternalTemperature() + ", aet=" + hs.getAverageExternalTemperature());

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
