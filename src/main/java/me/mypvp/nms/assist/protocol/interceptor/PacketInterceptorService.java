package me.mypvp.nms.assist.protocol.interceptor;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.minecraft.network.protocol.Packet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.*;

@SuppressWarnings("unused")
public class PacketInterceptorService {

  private static final Logger LOGGER = LogManager.getLogger();

  public static PacketInterceptorService create(Plugin plugin) {
    PacketInterceptorService service = new PacketInterceptorService(plugin);
    service.registerInjector();
    return service;
  }

  private final Map<Class<Packet<?>>, List<PacketInterceptorHandle<?>>> interceptors = new HashMap<>();
  private final Plugin plugin;
  private final String handlerName;

  private PacketInterceptorService(Plugin plugin) {
    this.plugin = plugin;
    this.handlerName = plugin.getName() + "_interceptor";
  }

  public void register(Object interceptorHolder) {
    for (Method method : interceptorHolder.getClass().getMethods()) {
      PacketInterceptor annotation = method.getDeclaredAnnotation(PacketInterceptor.class);
      if (annotation == null) {
        continue;
      }

      if (method.getParameterCount() != 1) {
        throw new IllegalArgumentException("The " + method.getName() + " must only expect one parameter");
      }

      Class<?> parameter = method.getParameterTypes()[0];
      if (parameter != InterceptorContext.class) {
        throw new IllegalArgumentException("The first parameter must be a InterceptorContext");
      }

      try {
        MethodHandle methodHandle = MethodHandles.lookup().unreflect(method);
        PacketInterceptorHandle<Packet<?>> handle = createInterceptorHandle(annotation.value(),
            interceptorHolder, annotation, methodHandle);
        var interceptors = this.interceptors.computeIfAbsent(handle.getPacketClass(),
            packetClass -> new ArrayList<>());
        interceptors.add(handle);
        interceptors.sort(Comparator.comparing(interceptor -> interceptor.annotation.priority()));
      } catch (IllegalAccessException e) {
        throw new IllegalArgumentException("Could not access method", e);
      }
    }
  }

  @SuppressWarnings("unused")
  public void unregister(Object interceptors) {
    for (List<PacketInterceptorHandle<?>> interceptorHandles : this.interceptors.values()) {
      interceptorHandles.removeIf(interceptor -> interceptor.getInterceptorHolder() == interceptors);
    }
  }

  @SuppressWarnings("unchecked")
  private <T extends Packet<?>> PacketInterceptorHandle<T> createInterceptorHandle(Class<?> packetClass,
      Object interceptorHolder, PacketInterceptor annotation, MethodHandle methodHandle) {
    return new PacketInterceptorHandle<>((Class<T>) packetClass, interceptorHolder, annotation, methodHandle);
  }

  private void registerInjector() {
    Bukkit.getPluginManager().registerEvents(new ListenerAdapter(), plugin);
  }

  private void inject(Player player) {
    CraftPlayer craftPlayer = (CraftPlayer) player;
    Channel channel = craftPlayer.getHandle().connection.connection.channel;
    ChannelHandler channelHandler = new ChannelHandler(craftPlayer);
    channel.pipeline().addBefore("packet_handler", handlerName, channelHandler);
  }

  private void uninject(Player player) {
    CraftPlayer craftPlayer = (CraftPlayer) player;
    Channel channel = craftPlayer.getHandle().connection.connection.channel;
    if (channel.pipeline().get(handlerName) != null) {
      channel.pipeline().remove(handlerName);
    }
  }

  public static class PacketInterceptorHandle<T extends Packet<?>> implements Comparable<PacketInterceptorHandle<?>> {

    private final Class<T> packetClass;
    private final Object interceptorHolder;
    private final PacketInterceptor annotation;
    private final MethodHandle method;

    PacketInterceptorHandle(Class<T> packetClass, Object interceptors, PacketInterceptor annotation,
                            MethodHandle method) {
      this.packetClass = packetClass;
      this.interceptorHolder = interceptors;
      this.annotation = annotation;
      this.method = method;
    }

    public Class<T> getPacketClass() {
      return packetClass;
    }

    public Object getInterceptorHolder() {
      return interceptorHolder;
    }

    public void invoke(InterceptorContext<T> ctx) throws Throwable {
      method.invoke(interceptorHolder, ctx);
    }

    @Override
    public int compareTo(@NotNull PacketInterceptorService.PacketInterceptorHandle<?> o) {
      return Byte.compare(annotation.priority(), o.annotation.priority());
    }

  }

  public class ListenerAdapter implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
      inject(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
      uninject(event.getPlayer());
    }

  }

  public class ChannelHandler extends ChannelDuplexHandler {

    private final Player player;

    public ChannelHandler(Player player) {
      this.player = player;
    }

    @Override
    public void channelRead(@NotNull ChannelHandlerContext ctx, @NotNull Object msg) throws Exception {
      msg = handleReadWrite(ctx, msg);
      if (msg != null)
        super.channelRead(ctx, msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
      msg = handleReadWrite(ctx, msg);
      if (msg != null)
        super.write(ctx, msg, promise);
    }

    private Object handleReadWrite(ChannelHandlerContext ctx, Object msg) {
      if (msg instanceof Packet<?> packet) {
        try {
          InterceptorContext<?> interceptorContext = invokeInterceptors(packet, ctx);
          if (interceptorContext != null) {
            if (interceptorContext.rejected()) {
              return null;
            }
            return interceptorContext.packet();
          }
        } catch (Exception e) {
          LOGGER.error("An exception got thrown by an interceptor", e);
        }
      }
      return msg;
    }

    @SuppressWarnings("unchecked")
    private <T extends Packet<?>> @Nullable InterceptorContext<T> invokeInterceptors(
        T packet, ChannelHandlerContext nettyContext) {
      List<PacketInterceptorHandle<?>> interceptors = PacketInterceptorService.this.interceptors.get(packet.getClass());
      if (interceptors == null) {
        return null;
      }
      InterceptorContext<T> ctx = new InterceptorContext<>(player, nettyContext, packet);
      for (PacketInterceptorHandle<?> interceptor : interceptors) {
        try {
          ((PacketInterceptorHandle<T>) interceptor).invoke(ctx);
          if (ctx.rejected()) {
            return ctx;
          }
        } catch (Throwable e) {
          LOGGER.error("An error occurred while invoking packet interceptor", e);
        }
      }
      return ctx;
    }

  }

}
