package me.mypvp.nms.assist.protocol.interceptor;

import net.minecraft.network.protocol.Packet;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PacketInterceptor {

  Class<? extends Packet<?>> value();
  byte priority() default Priority.NORMAL;

  @SuppressWarnings("unused")
  class Priority {

    public static final byte HIGHEST = 16;
    public static final byte HIGH = 8;
    public static final byte NORMAL = 0;
    public static final byte LOW = -8;
    public static final byte LOWEST = -16;
    public static final byte MONITOR = -64;

  }

}
