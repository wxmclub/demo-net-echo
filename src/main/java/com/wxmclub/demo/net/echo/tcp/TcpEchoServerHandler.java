/*
MIT License

Copyright (c) 2018 wxmclub@gmail.com

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/
package com.wxmclub.demo.net.echo.tcp;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author wxmclub@gmail.com
 * @version 1.0
 * @date 2018-02-14
 */
@ChannelHandler.Sharable
public class TcpEchoServerHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private InetSocketAddress serverSocketAddress;

    /**
     * key:客户端的地址, value: 创建的对应的服务端的连接
     */
    private final Map<SocketAddress, Channel> clientToServerChannels = new ConcurrentHashMap<>();
    /**
     * key:服务端连接地址, value: 客户端的channel
     */
    private final Map<SocketAddress, Channel> serverToClientChannels = new ConcurrentHashMap<>();

    public TcpEchoServerHandler(InetSocketAddress serverSocketAddress) {
        this.serverSocketAddress = serverSocketAddress;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        Channel serverChannel = clientToServerChannels.get(ctx.channel().remoteAddress());
        if (serverChannel == null || !serverChannel.isActive()) {
            // 客户端第一次建立连接时，需要初始化代理打洞节点的连接
            synchronized (clientToServerChannels) {
                serverChannel = clientToServerChannels.get(ctx.channel().remoteAddress());
                if (serverChannel == null || !serverChannel.isActive()) {
                    serverChannel = this.createChannel();
                    clientToServerChannels.put(ctx.channel().remoteAddress(), serverChannel);
                    serverToClientChannels.put(serverChannel.remoteAddress(), ctx.channel());
                }
            }
        }
        System.out.println("forward echo msg: from client [" + ctx.channel().remoteAddress()
                + "] to server [" + this.serverSocketAddress + "], length=" + msg.readableBytes());
        // 向服务端转发消息
        serverChannel.writeAndFlush(msg.copy());
    }

    /**
     * 创建连接
     */
    private Channel createChannel() throws InterruptedException {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(new NioEventLoopGroup())
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast("handler", new TcpEchoClientHandler());
                    }
                });
        return bootstrap.connect(this.serverSocketAddress).sync().channel().closeFuture().channel();
    }

    /**
     * 服务端消息处理
     */
    private class TcpEchoClientHandler extends SimpleChannelInboundHandler<ByteBuf> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
            Channel clientChannel = serverToClientChannels.get(ctx.channel().remoteAddress());
            if (clientChannel == null) {
                throw new RuntimeException("forward echo msg error, from server [" + serverSocketAddress + "] to client is null!");
            } else {
                System.out.println("forward echo msg: from server [" + serverSocketAddress
                        + "] to client [" + clientChannel.remoteAddress() + "], length=" + msg.readableBytes());
                clientChannel.writeAndFlush(msg.copy());
            }
        }

    }

}
