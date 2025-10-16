package server.alikindoger.com;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;

public class GameChannelInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        
        // 1. Manejo de HTTP: Necesario para el 'handshake' inicial de WebSocket
        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(new HttpObjectAggregator(65536)); // Máximo tamaño de mensaje

        // 2. Manejo de WebSocket: Realiza el upgrade de HTTP a WebSocket
        // El '/ws' es la ruta donde los clientes se conectarán (ws://servidor:8080/ws)
        pipeline.addLast(new WebSocketServerProtocolHandler("/ws")); 
        
        // 3. Handler de Mensajes de WebSocket: Convierte el Frame de WebSocket a texto/bytes
        pipeline.addLast(new WebSocketFrameHandler()); 	

        // 4. Lógica de Juego: Nuestro handler personalizado donde se procesan los comandos (MOVE, CHAT, etc.)
        pipeline.addLast(new GameLogicHandler()); 
    }
}