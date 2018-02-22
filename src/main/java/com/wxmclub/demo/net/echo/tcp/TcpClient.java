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
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;

import java.net.InetSocketAddress;

/**
 * @author wxmclub@gmail.com
 * @version 1.0
 * @date 2018-02-14
 */
public class TcpClient {

    private InetSocketAddress serverSocketAddress;

    public TcpClient(InetSocketAddress serverSocketAddress) {
        this.serverSocketAddress = serverSocketAddress;
    }

    public Channel run() throws Exception {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(new NioEventLoopGroup())
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast("decoder", new ByteArrayDecoder());
                        pipeline.addLast("encoder", new ByteArrayEncoder());
                        pipeline.addLast("handler", new TcpClientHandler());
                    }
                });
        return bootstrap.connect(serverSocketAddress).sync().channel().closeFuture().channel();
    }

    public class TcpClientHandler extends SimpleChannelInboundHandler<byte[]> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, byte[] msg) throws Exception {
            System.out.println("client接收到服务器返回的消息:" + new String(msg));
        }

    }

    public static void main(String[] args) throws Exception {
        // 直接向Server端发送消息
        //InetSocketAddress serverSocketAddress = new InetSocketAddress(TcpServer.HOST, TcpServer.PORT);
        // 通过Echo转发消息
        InetSocketAddress serverSocketAddress = new InetSocketAddress(TcpServer.HOST, TcpEchoServer.PORT);

        TcpClient client = new TcpClient(serverSocketAddress);
        Channel channel = client.run();

        Thread.sleep(500L);
        System.out.println("--------");

        channel.writeAndFlush("测试1".getBytes());
        Thread.sleep(500L);
        System.out.println("--------");

        channel.writeAndFlush("测试2".getBytes());
        Thread.sleep(500L);
        System.out.println("--------");
    }

}
