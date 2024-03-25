package me.mypvp.nms.assist.protocol.interceptor;

import io.netty.channel.ChannelHandlerContext;
import me.mypvp.nms.assist.protocol.PacketAccessor;
import net.minecraft.network.protocol.Packet;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class InterceptorContext<T extends Packet<?>> {

  private final Player player;
  private final ChannelHandlerContext nettyContext;
  private T packet;
  private boolean reject;

  public InterceptorContext(@NotNull Player player, @NotNull ChannelHandlerContext nettyContext, @NotNull T packet) {
    this.player = player;
    this.nettyContext = nettyContext;
    this.packet = packet;
  }

  public @NotNull T packet() {
    return packet;
  }

  public @NotNull Player player() {
    return player;
  }

  public @NotNull ChannelHandlerContext nettyContext() {
    return nettyContext;
  }

  public @NotNull PacketAccessor<T> accessor() {
    return PacketAccessor.of(packet);
  }

  public void replace(@NotNull T packet) {
    this.packet = packet;
  }

  public void reject() {
    this.reject = true;
  }

  public boolean rejected() {
    return this.reject;
  }

  @Deprecated
  public @NotNull Player getPlayer() {
    return player;
  }

  @Deprecated
  public @NotNull ChannelHandlerContext getNettyContext() {
    return nettyContext;
  }

}
