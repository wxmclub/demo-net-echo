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
package com.wxmclub.demo.net.echo.udp;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

/**
 * UDP打洞左客户端
 *
 * @author wxmclub@gmail.com
 * @version 1.0
 * @date 2018-02-13
 */
public class UdpLeftClient {

    public static final int PORT = 7778;

    private String host;
    private int port;
    private InetSocketAddress serverAddress;

    public UdpLeftClient(String host, int port, InetSocketAddress serverAddress) {
        this.host = host;
        this.port = port;
        this.serverAddress = serverAddress;
    }

    public void run() {
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(new NioEventLoopGroup())
                    .channel(NioDatagramChannel.class)
                    .option(ChannelOption.SO_BROADCAST, true)
                    .handler(new UdpClientHandler());
            bootstrap.bind(host, port).sync().channel().closeFuture().await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private class UdpClientHandler extends SimpleChannelInboundHandler<DatagramPacket> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) {
            ByteBuf buf = packet.copy().content();
            ByteBuffer buffer = ByteBuffer.allocate(buf.readableBytes());
            buf.readBytes(buffer);

            String msg = new String(buffer.array());
            System.out.println("接收到的信息:" + msg);

            // 接收到消息后回传消息
            String backMsg = "我知道了(" + msg + ")";
            System.out.println("回传的消息：" + backMsg);
            ctx.writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer(backMsg.getBytes()), serverAddress));
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            System.out.println("左客户端向服务器发送自己的地址: " + ctx.channel().localAddress());
            ctx.writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer("L".getBytes()), serverAddress));
            super.channelActive(ctx);
        }

    }

    public static void main(String[] args) {
        UdpLeftClient client = new UdpLeftClient(UdpEchoServer.HOST, PORT,
                new InetSocketAddress(UdpEchoServer.HOST, UdpEchoServer.PORT));
        client.run();
    }

}
