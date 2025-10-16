package server.alikindoger.com;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class GameServer {
    private final int port;

    public GameServer(int port) {
        this.port = port;
    }

    public void run() throws Exception {
        //boss acepta nuevas conexiones
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        //worker maneja el tráfico de datos para las conexiones aceptadas
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class) // Usamos canales TCP NIO (Non-blocking I/O)
             .childHandler(new GameChannelInitializer()); // Define el pipeline de handlers
             
            // Vincula el servidor al puerto e inicia la escucha
            ChannelFuture f = b.bind(port).sync(); 
            System.out.println("Servidor de Juego iniciado en el puerto " + port);

            // Espera a que el socket del servidor se cierre
            f.channel().closeFuture().sync();
        } finally {
            // Cierra ambos EventLoopGroups de forma elegante (shutdownGracefully)
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
    
    public static void main(String[] args) throws Exception {
        new GameServer(8080).run();
    }
}