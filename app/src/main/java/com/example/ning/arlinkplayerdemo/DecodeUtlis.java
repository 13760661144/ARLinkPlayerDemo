package com.example.ning.arlinkplayerdemo;

import android.util.Log;

import java.nio.ByteBuffer;

/**
 * Created by 郑梦晨 on 2018/11/28.
 */
public class DecodeUtlis {

    public int flag_1 = 0;


    private int frame_d_size = 0;
    private int count_flag = 0;

    private Boolean isIframe = false;

    public DecodeUtlis() {
    }
    private static DecodeUtlis single = new DecodeUtlis();

    public static DecodeUtlis getInstance() {
        return single;
    }

    public void searchFrame(FiFoUtlis streamFiFo, FrameBufferListener frameBufferListener) {
        int i = 0;
        int llen = streamFiFo.getActualSize();
        for (i = 0; i < llen; i++) {
            if(count_flag == 1) {
                frame_d_size++;
            }
            if(!(streamFiFo.getBuffer()[(streamFiFo.getFront() + i) % streamFiFo.FIFO_SIZE] == (byte)0x00 && i + 4 < streamFiFo.getActualSize()
                    && streamFiFo.getBuffer()[(streamFiFo.getFront() + i + 1) % streamFiFo.FIFO_SIZE] == (byte)0x00 ))
                continue;
            if(!(streamFiFo.getBuffer()[(streamFiFo.getFront() + i + 2) % streamFiFo.FIFO_SIZE] == (byte)0x00 &&
                    streamFiFo.getBuffer()[(streamFiFo.getFront() + i + 3) % streamFiFo.FIFO_SIZE] == (byte)0x01) )
                continue;
            if(!(streamFiFo.getBuffer()[(streamFiFo.getFront() + i + 4) % streamFiFo.FIFO_SIZE] == (byte)0x67 ||
                    streamFiFo.getBuffer()[(streamFiFo.getFront() + i + 4) % streamFiFo.FIFO_SIZE] == (byte)0x41) )
                continue;
            count_flag++;
            if (count_flag == 1) {
                if(streamFiFo.getBuffer()[(streamFiFo.getFront() + i + 4) % streamFiFo.FIFO_SIZE] == (byte)0x67) {
                    isIframe = true;
                }
                flag_1 = i;
                i += 64;
                frame_d_size += 64;


            } else if (count_flag == 2) {
                byte []dataa = null;
                ByteBuffer byteBuffer = null;

                if (isIframe == true) {
                    dataa = new byte[frame_d_size + flag_1];
                    streamFiFo.FiFoRead(dataa, frame_d_size + flag_1);

                    if (dataa[flag_1 + 9] == (byte)0x03 && dataa[flag_1 + 10] == (byte)0xC0 && dataa[flag_1 + 11] == (byte)0x11) {
                        byteBuffer = ByteBuffer.wrap(dataa, flag_1, frame_d_size);

                        byte[] sps = {
                                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x01, (byte)0x67, (byte)0x4D, (byte)0x00, (byte)0x28, (byte)0xF4,
                                (byte)0x03, (byte)0xC0, (byte)0x11, (byte)0x3F, (byte)0x2E, (byte)0x02, (byte)0x20, (byte)0x00, (byte)0x00,
                                (byte)0x03, (byte)0x00, (byte)0x20, (byte)0x00, (byte)0x00, (byte)0x07, (byte)0x81,  (byte)0xE3, (byte)0x06,
                                (byte)0x54};

                        byte []bdata = new byte[frame_d_size + 14];
                        System.arraycopy(sps, 0, bdata, 0, sps.length);
                        System.arraycopy(dataa, flag_1 + 14, bdata, sps.length, dataa.length - flag_1 - 14);

                        byteBuffer = ByteBuffer.wrap(bdata, 0, frame_d_size + 14);
                        Log.e("ArtosynPlayer", "Resolution: 1080P30");

                    } else if (dataa[flag_1 + 9] == (byte)0x02 && dataa[flag_1 + 10] == (byte)0x80 && dataa[flag_1 + 11] == (byte)0x2D){
                        byte[] sps = {
                                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x01, (byte)0x67, (byte)0x4D, (byte)0x00, (byte)0x28, (byte)0xF4,
                                (byte)0x02, (byte)0x80, (byte)0x2D, (byte)0xD8, (byte)0x08, (byte)0x80, (byte)0x00, (byte)0x00, (byte)0x03,
                                (byte)0x00, (byte)0x80, (byte)0x00, (byte)0x00, (byte)0x1E, (byte)0x07,  (byte)0x8C, (byte)0x19, (byte)0x50};

                        byte []bdata = new byte[frame_d_size + 14];
                        System.arraycopy(sps, 0, bdata, 0, sps.length);
                        System.arraycopy(dataa, flag_1 + 13, bdata, sps.length, dataa.length - flag_1 - 13);

                        byteBuffer = ByteBuffer.wrap(bdata, 0, frame_d_size + 14);

                    } else {
                        byteBuffer = ByteBuffer.wrap(dataa, flag_1, frame_d_size);
                    }


                } else {
                    dataa = new byte[frame_d_size + flag_1];
                    streamFiFo.FiFoRead(dataa, frame_d_size + flag_1);
                    byteBuffer = ByteBuffer.wrap(dataa, flag_1, frame_d_size);
                }

                frameBufferListener.isPushFrameBuffer(byteBuffer);
                flag_1 = 0;
                count_flag = 0;
                frame_d_size = 0;
                isIframe = false;
                break;
            }
        }
        flag_1 = 0;
        count_flag = 0;
        frame_d_size = 0;
        isIframe = false;

    }
    interface FrameBufferListener {
        void isPushFrameBuffer(ByteBuffer byteBuffer);
    }
}
