package server.alikindoger.com;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

public class WebSocketFrameHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
        if (frame instanceof TextWebSocketFrame) {
            // pasa al gameLogicHandler
            ctx.fireChannelRead(frame.retain());
        } 
        // Ignora otros tipos de frames (p.ej., CloseWebSocketFrame, PingWebSocketFrame)
    }

    // Manejo de errores
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        System.err.println("Error en WebSocketFrameHandler:");
        cause.printStackTrace();
        ctx.close();
    }
}