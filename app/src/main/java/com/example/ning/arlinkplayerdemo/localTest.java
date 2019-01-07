package com.example.ning.arlinkplayerdemo;

import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;

/**
 * Created by 郑梦晨 on 2018/11/28.
 */
public class localTest {

    public localTest(FiFoUtlis streamFiFo) {
        int count = 0;
        int len = 0;
        FileInputStream fis = null;
        try {
            String video = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "720p.h264";
            fis = new FileInputStream(new File(video));
        } catch (Exception e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        try {
            while (true) {
                byte[] buf = null;
                if (count % 30 == 0)
                    buf = new byte[200000];
                else
                    buf = new byte[10760];
                len = fis.read(buf);
                if (len <= 0)
                    break;

                int left = 0, lll = 0;
                while (left < len) {
                    lll = streamFiFo.FiFoWrite(buf, len);
                    left += lll;
                    Thread.sleep(5);
                }
                count++;
                Thread.sleep(33);
            }
        } catch (Exception e) {
        }
    }
}
