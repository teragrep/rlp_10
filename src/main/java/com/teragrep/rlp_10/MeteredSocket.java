package com.teragrep.rlp_10;

import com.codahale.metrics.Timer;
import com.teragrep.net_01.channel.socket.Socket;
import com.teragrep.net_01.channel.socket.TransportInfo;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class MeteredSocket implements Socket {
    private final Socket origin;
    private final Metrics metrics;

    public MeteredSocket(final Socket origin, final Metrics metrics){
        this.origin = origin;
        this.metrics = metrics;
    }

    @Override
    public long read(final ByteBuffer[] dsts) throws IOException {
        try(final Timer.Context transmitTimer = metrics.transmitLatency().time()){
            return origin.read(dsts);
        }
    }

    @Override
    public long write(final ByteBuffer[] dsts) throws IOException {
        return origin.write(dsts);
    }

    @Override
    public TransportInfo getTransportInfo() {
        return origin.getTransportInfo();
    }

    @Override
    public void close() throws IOException {
        origin.close();
    }

    @Override
    public SocketChannel socketChannel() {
        return origin.socketChannel();
    }
}
