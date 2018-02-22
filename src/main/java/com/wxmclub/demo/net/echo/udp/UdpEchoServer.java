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

/**
 * 打洞服务
 *
 * 启动顺序：UdpEchoServer，UdpLeftClient，UdpRightClient
 *
 * @author wxmclub@gmail.com
 * @version 1.0
 * @date 2018-02-13
 */
public class UdpEchoServer {

    public static final String HOST = "127.0.0.1";
    public static final int PORT = 4777;

    private String host;
    private int port;

    public UdpEchoServer(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void run() {
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(new NioEventLoopGroup())
                    .channel(NioDatagramChannel.class)
                    .option(ChannelOption.SO_BROADCAST, true)
                    .handler(new UdpEchoServerHandler());
            bootstrap.bind(host, port).sync().channel().closeFuture().await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private class UdpEchoServerHandler extends SimpleChannelInboundHandler<DatagramPacket> {

        private InetSocketAddress lAddr;
        private InetSocketAddress rAddr;

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) {
            ByteBuf buf = msg.copy().content();
            if (buf.readableBytes() == 1) {
                switch (buf.readByte()) {
                    case 'L':
                        // 左节点打洞消息
                        this.lAddr = msg.sender();
                        System.out.println("左节点打洞:" + this.lAddr);
                        return;
                    case 'R':
                        // 右节点打洞消息
                        this.rAddr = msg.sender();
                        System.out.println("右节点打洞:" + this.rAddr);
                        return;
                }
            }
            if (this.lAddr == null) {
                throw new RuntimeException("left address is null!");
            } else if (this.rAddr == null) {
                throw new RuntimeException("right address is null!");
            }

            InetSocketAddress toAddress;
            if (msg.sender().equals(this.lAddr)) {
                // 左节点发送的消息，向右节点发送
                toAddress = this.rAddr;
            } else if (msg.sender().equals(this.rAddr)) {
                // 右节点发送的消息，向左节点发送
                toAddress = this.lAddr;
            } else {
                // 未识别的节点
                throw new RuntimeException("unknow address:" + msg.sender());
            }
            System.out.println("forward echo msg: from [" + msg.sender() + "] to [" + toAddress + "], message length=" + buf.readableBytes());
            ctx.writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer(buf), toAddress));
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            System.out.println("服务已启动：" + ctx.channel().localAddress());
            ctx.fireChannelActive();
        }

    }

    public static void main(String[] args) {
        UdpEchoServer server = new UdpEchoServer(HOST, PORT);
        server.run();
    }

}
