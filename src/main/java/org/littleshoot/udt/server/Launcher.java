package org.littleshoot.udt.server;

public class Launcher {

    public static void main(final String[] args) {
        final UdtFileUploadServer server = new UdtFileUploadServer();
        server.start();
    }
}
