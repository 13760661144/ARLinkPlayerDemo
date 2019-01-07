package com.example.ning.arlinkplayerdemo;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

import static com.example.ning.arlinkplayerdemo.MainActivity.PIXEL_HEIGHT;
import static com.example.ning.arlinkplayerdemo.MainActivity.PIXEL_WIDTH;

/**
 * Created by 郑梦晨 on 2019/1/2.
 */
public class PlayerThread extends Thread {

    private Surface surface;
    private MediaCodec decoder = null;
    private MainActivity mMainActivity;
    int mFrameIndex = 0;
    public PlayerThread(Surface surface,MainActivity mainActivity) {
        this.surface = surface;
        this.mMainActivity=mainActivity;
    }

    @Override
    public void run() {
        try {
            decoder = MediaCodec.createDecoderByType("video/avc");
        } catch (IOException e) {
            e.printStackTrace();
        }
        MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", PIXEL_WIDTH, PIXEL_HEIGHT);

        mediaFormat.setInteger(MediaFormat.KEY_MAX_WIDTH, PIXEL_WIDTH);
        mediaFormat.setInteger(MediaFormat.KEY_HEIGHT, PIXEL_HEIGHT);
        mediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, PIXEL_WIDTH * PIXEL_HEIGHT);
        mediaFormat.setInteger(MediaFormat.KEY_PUSH_BLANK_BUFFERS_ON_STOP, 1);

        if (surface == null)
            Log.e("DecodeActivity", "The Surface is NULL! ");

        else
            Log.e("DecodeActivity", "The Surface is OK ");

        decoder.configure(mediaFormat, surface, null, 0);
        decoder.start();
        ByteBuffer buffer = null;
        try {
            while (true) {
                if (mMainActivity.hasFrameBuffer()) {

                    buffer = mMainActivity.dequeueFrameBuffer();
                    if (buffer != null) {

                        offerDecoderBuffer(buffer);
                        buffer = null;
                    }
                } else {
                    Thread.sleep(5);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void offerDecoderBuffer(ByteBuffer byteBuffer) {
        try {
            ByteBuffer[] inputBuffers = decoder.getInputBuffers();
            int inputBufferIndex = decoder.dequeueInputBuffer(-1);

            if (inputBufferIndex >= 0) {
                ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                long timestamp = mFrameIndex++ * (1000000 / 30);
                inputBuffer.rewind();
                inputBuffer.put(byteBuffer);
                decoder.queueInputBuffer(inputBufferIndex, 0, byteBuffer.capacity(), timestamp, 0);
            }

            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            int outputBufferIndex = decoder.dequeueOutputBuffer(bufferInfo, 0);

            while (outputBufferIndex >= 0) {
                decoder.releaseOutputBuffer(outputBufferIndex, true);
                outputBufferIndex = decoder.dequeueOutputBuffer(bufferInfo, 0);
            }

        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
