package de.prokyo.network.server;

import de.prokyo.network.common.pipeline.PacketDecoder;
import de.prokyo.network.common.pipeline.PacketEncoder;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import lombok.RequiredArgsConstructor;

/**
 * Class for initializing socket channels of clients.
 */
@RequiredArgsConstructor
public class ClientChannelInitializer extends ChannelInitializer<SocketChannel> {

    private final ProkyoServer prokyoServer;

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ClientConnection connection = new ClientConnection(ch);

        ch.pipeline()
                .addLast("idleStateHandler", new IdleStateHandler(0, 25, 0))
                .addLast("timeout", new ReadTimeoutHandler(30))
                .addLast("frame-decoder", new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4))
                .addLast("prokyoDecoder", new PacketDecoder())
                .addLast("frame-prepender", new LengthFieldPrepender(4))
                .addLast("prokyoEncoder", new PacketEncoder())
                .addLast("prokyoPacketHandler", new ProkyoDuplexHandler(this.prokyoServer, connection));

        ch.attr(ClientConnection.ATTRIBUTE_KEY).set(connection);
    }

}
