package com.example.ebicompany.nettyclient;

import android.widget.EditText;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.CharsetUtil;


public class WebSocketClientHandler extends SimpleChannelInboundHandler<Object> {
    private final WebSocketClientHandshaker handshaker;
    private ChannelPromise handshakeFuture;
    private EditText txtRecievedMessages;

    public WebSocketClientHandler(WebSocketClientHandshaker handshaker, EditText txtRecievedMessages) {
        this.handshaker = handshaker;
        this.txtRecievedMessages = txtRecievedMessages;
    }

    public ChannelFuture handshakeFuture() {
        return handshakeFuture;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        handshakeFuture = ctx.newPromise();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        handshaker.handshake(ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        txtRecievedMessages.post(new Runnable() {
            @Override
            public void run() {
                (txtRecievedMessages).setText("WebSocket Client disconnected!" + "\n");
            }
        });
        //System.out.println("WebSocket Client disconnected!");
    }

    public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        Channel ch = ctx.channel();
        if (!handshaker.isHandshakeComplete()) {
            handshaker.finishHandshake(ch, (FullHttpResponse) msg);
            //System.out.println("WebSocket Client connected!");
            txtRecievedMessages.post(new Runnable() {
                @Override
                public void run() {
                    (txtRecievedMessages).setText("WebSocket Client connected" + "\n");
                }
            });
            handshakeFuture.setSuccess();
            return;
        }

        if (msg instanceof FullHttpResponse) {
            FullHttpResponse response = (FullHttpResponse) msg;
            throw new IllegalStateException(
                    "Unexpected FullHttpResponse (getStatus=" + response.getStatus() +
                            ", content=" + response.content().toString(CharsetUtil.UTF_8) + ')');
        }

        WebSocketFrame frame = (WebSocketFrame) msg;
        if (frame instanceof TextWebSocketFrame) {
            final TextWebSocketFrame textFrame = (TextWebSocketFrame) frame;
            //System.out.println("WebSocket Client received message: " + textFrame.text());
            txtRecievedMessages.post(new Runnable() {
                @Override
                public void run() {
                    (txtRecievedMessages).setText("WebSocket Client received message: " + textFrame.text() + "\n");
                }
            });
        } else if (frame instanceof PongWebSocketFrame) {
            //System.out.println("WebSocket Client received pong");
            txtRecievedMessages.post(new Runnable() {
                @Override
                public void run() {
                    (txtRecievedMessages).setText("WebSocket Client received pong" + "\n");
                }
            });
        } else if (frame instanceof CloseWebSocketFrame) {
            //System.out.println("WebSocket Client received closing");
            txtRecievedMessages.post(new Runnable() {
                @Override
                public void run() {
                    (txtRecievedMessages).setText("WebSocket Client received closing" + "\n");
                }
            });
            ch.close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        if (!handshakeFuture.isDone()) {
            handshakeFuture.setFailure(cause);
        }
        ctx.close();
    }
}
