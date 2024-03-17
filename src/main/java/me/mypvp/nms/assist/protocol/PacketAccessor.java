package me.mypvp.nms.assist.protocol;

import net.minecraft.network.protocol.Packet;
import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

@SuppressWarnings("unused")
public class PacketAccessor<T extends Packet<?>> {

  private static final Objenesis objenesis = new ObjenesisStd();

  private static void checkPacketIsRecord(Class<?> packetClass) {
    if (packetClass.isRecord()) {
      throw new IllegalArgumentException("Could not create a packet container of a record packet");
    }
  }

  public static <T extends Packet<?>> PacketAccessor<T> create(Class<T> packetClass) {
    checkPacketIsRecord(packetClass);
    return new PacketAccessor<>(objenesis.newInstance(packetClass));
  }

  public static <T extends Packet<?>> PacketAccessor<T> of(T packet) {
    checkPacketIsRecord(packet.getClass());
    return new PacketAccessor<>(packet);
  }

  private final T packet;
  private final PacketInfo<T> packetInfo;

  @SuppressWarnings("unchecked")
  public PacketAccessor(T packet) {
    this.packet = packet;
    this.packetInfo = PacketInfo.create((Class<T>) packet.getClass());
  }

  public PacketAccessor<T> write(int index, Object obj) {
    Field field = packetInfo.getFields()[index];
    try {
      field.setAccessible(true);
      field.set(packet, obj);
    } catch (IllegalAccessException e) {
      throw new IllegalStateException("Could not access field " + field, e);
    }
    return this;
  }

  public <U> U read(int index, Class<U> type) {
    Field field = packetInfo.getFields()[index];
    try {
      return type.cast(field.get(packet));
    } catch (IllegalAccessException e) {
      throw new IllegalStateException("Could not get value of field " + field, e);
    }
  }

  public boolean readBoolean(int index) {
    return read(index, boolean.class);
  }

  public byte readByte(int index) {
    return read(index, byte.class);
  }

  public short readShort(int index) {
    return read(index, short.class);
  }

  public int readInt(int index) {
    return read(index, int.class);
  }

  public long readLong(int index) {
    return read(index, long.class);
  }

  public float readFloat(int index){
    return read(index, float.class);
  }

  public double readDouble(int index) {
    return read(index, double.class);
  }

  public String readString(int index) {
    return read(index, String.class);
  }

  public T get() {
    return this.packet;
  }

  public static class PacketInfo<T extends Packet<?>> {

    private static final Map<Class<?>, PacketInfo<?>> PACKET_INFOS = new HashMap<>();

    @SuppressWarnings("unchecked")
    private static <T extends Packet<?>> PacketInfo<T> create(Class<T> packetClass)  {
      return (PacketInfo<T>) PACKET_INFOS.computeIfAbsent(packetClass,
          pCls -> new PacketInfo<>((Class<T>) pCls));
    }

    private final Field[] fields;

    public PacketInfo(Class<T> packetClass) {
      List<Field> discoveredFields = new ArrayList<>();
      for (Field field : packetClass.getDeclaredFields()) {
        if (Modifier.isStatic(field.getModifiers())) {
          continue;
        }
        field.setAccessible(true);
        discoveredFields.add(field);
      }
      this.fields = discoveredFields.toArray(Field[]::new);
    }

    public Field[] getFields() {
      return fields;
    }

  }

}
