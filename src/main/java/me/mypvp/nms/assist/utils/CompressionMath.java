package me.mypvp.nms.assist.utils;

public class CompressionMath {

  public static short compressMovePosition(double from, double to) {
    return (short) ((to - from) * 4096);
  }

  public static byte compressAngle(float value) {
    return (byte) (int) (value * 256F / 360F);
  }

  public static int compressPosition(double position) {
    return (int) Math.floor(position * 32D);
  }

}
