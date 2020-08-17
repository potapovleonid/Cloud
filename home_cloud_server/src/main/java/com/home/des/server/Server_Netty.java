package com.home.des.server;

import com.home.des.common.ConnectionSettings;
import com.home.des.common.FileMessage;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

import java.nio.file.Files;

public class Server_Netty {

    public void run() throws Exception{
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap server = new ServerBootstrap();
            server.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline().addLast(
                            new ObjectDecoder((int) (FileMessage.SIZE_BYTE_BUFFER * 1.05), ClassResolvers.cacheDisabled(null)),
                            new ObjectEncoder(),
                            new MyHandler()
                            );
                        }
                    });
            ChannelFuture future = server.bind(ConnectionSettings.PORT).sync();

            if (!Files.exists(ConnectionSettings.destination_server_files)){
                Files.createDirectory(ConnectionSettings.destination_server_files);
            }
            System.out.println("Server started");

            future.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }

    }

    public static void main(String[] args) throws Exception {
        new Server_Netty().run();
    }
}
