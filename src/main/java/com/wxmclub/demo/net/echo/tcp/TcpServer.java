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

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;

/**
 * @author wxmclub@gmail.com
 * @version 1.0
 * @date 2018-02-14
 */
public class TcpServer {

    public static final String HOST = "127.0.0.1";
    public static final int PORT = 9999;

    private String host;
    private int port;

    public TcpServer(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void run() throws Exception {
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(new NioEventLoopGroup())
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast("decoder", new ByteArrayDecoder());
                        pipeline.addLast("encoder", new ByteArrayEncoder());
                        pipeline.addLast("handler", new TcpServerHandler());
                    }
                });
        bootstrap.bind(host, port).sync();
        System.out.println("TCP 服务已启动");
    }

    public class TcpServerHandler extends SimpleChannelInboundHandler<byte[]> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, byte[] msg) {
            String message = new String(msg);
            System.out.println("服务端收到消息：" + message);

            // 响应内容
            String backMsg = "知道了(" + message + ")";
            System.out.println("回传的消息：" + backMsg);
            ctx.writeAndFlush((backMsg).getBytes());
        }

    }

    public static void main(String[] args) throws Exception {
        TcpServer server = new TcpServer(HOST, PORT);
        server.run();
    }

}
