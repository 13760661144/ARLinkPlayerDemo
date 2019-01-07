package com.example.ning.arlinkplayerdemo;

import android.util.Log;

/**
 * Created by 郑梦晨 on 2018/11/28.
 */
public class FiFoUtlis {

    private static Object lockObject = new Object();
    public static int FIFO_SIZE = 1024*1024*4;

    public byte[] getBuffer() {
        return buffer;
    }



    private byte[] buffer = new byte[FIFO_SIZE];

    public int getFront() {
        return front;
    }

    private int front = 0;

    public int getRear() {
        return rear;
    }

    private int rear = 0;

    private boolean isEmpty = true;
    private boolean isFull = false;

    /**
     * 写视频流
     * @param data
     * @param length
     * @return
     */
    public int FiFoWrite(byte[] data, int length) {
        synchronized (lockObject) {
            int count = 0;
            int bufSize = getActualSize();

            if (length < 1 || isFull) {
                isFull = true;
                return 0;
            }

            if (FIFO_SIZE - bufSize > length) {
                count = length;
                isFull = false;
            } else {
                count = FIFO_SIZE - bufSize;
                isFull = true;
            }

            if (isEmpty)
                isEmpty = false;

            if (rear >= front) {
                if (FIFO_SIZE - rear >= count) {
                    System.arraycopy(data, 0, buffer, rear, count);
                    rear = rear + count >= FIFO_SIZE ? 0 : rear + count;
                } else {
                    System.arraycopy(data, 0, buffer, rear, FIFO_SIZE - rear);
                    System.arraycopy(data, FIFO_SIZE - rear, buffer, 0, count - (FIFO_SIZE - rear));
                    rear = rear + count - FIFO_SIZE;
                }

            } else {
                System.arraycopy(data, 0, buffer, rear, count);
                rear = rear + count >= FIFO_SIZE ? 0 : rear + count;
            }
            return count;
        }
    }


    /**
     * 读视频流
     * @param data
     * @param length
     * @return
     */
    public int FiFoRead(byte[] data, int length) {
        synchronized (lockObject) {
            int count = 0;
            int bufSize = getActualSize();

            if (length < 1 || isEmpty) {
                //isEmpty = true;
                return 0;
            }

            if (bufSize > length) {
                count = length;
                isEmpty = false;
            } else {
                count = bufSize;

                isEmpty = true;
            }

            if (isFull)
                isFull = false;

            if (rear > front) {
                System.arraycopy(buffer, front, data, 0, count);
                front = front + count;
            } else {
                if (count > FIFO_SIZE - front) {
                    System.arraycopy(buffer, front, data, 0, FIFO_SIZE - front);
                    System.arraycopy(buffer, 0, data, FIFO_SIZE - front, count - (FIFO_SIZE - front));
                } else {
                    System.arraycopy(buffer, front, data, 0, count);
                }
                front = (front + count) >= FIFO_SIZE ? (front + count - FIFO_SIZE) : (front + count);
            }
            return count;
        }

    }



    public int FiFoCopy(byte[] data, int length) {
        synchronized (lockObject) {
            int count = 0;
            int bufSize = getActualSize();

            if (length < 1 || isEmpty) {
                isEmpty = true;
                return 0;
            }

            if (bufSize > length) {
                count = length;
                isEmpty = false;
            } else {
                count = bufSize;
            }

            if (rear > front) {
                System.arraycopy(buffer, front, data, 0, count);
            } else {
                if (count > FIFO_SIZE - front) {
                    System.arraycopy(buffer, front, data, 0, FIFO_SIZE - front);
                    System.arraycopy(buffer, 0, data, FIFO_SIZE - front, count - (FIFO_SIZE - front));
                } else {
                    System.arraycopy(buffer, front, data, 0, count - front);
                }

            }
            return count;
        }
    }

    /**
     * 获取当前缓存是否已满
     * @return
     */
    public int getActualSize() {
        if (isEmpty == true) {
            return 0;
        } else {
            if (rear >= front)
                return (rear - front);
            else
                return (FIFO_SIZE - (front - rear));
        }
    }
}
