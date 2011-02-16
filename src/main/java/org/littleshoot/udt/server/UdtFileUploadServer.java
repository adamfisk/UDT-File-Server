package org.littleshoot.udt.server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.barchart.udt.net.NetServerSocketUDT;

public class UdtFileUploadServer {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final ExecutorService readPool = Executors.newCachedThreadPool();
    
    //private final ServerSocketChannel acceptorChannel;

    //private final ServerSocket acceptorSocket;

    //private final AbstractSelector selector;

    //private SelectionKey acceptorKey;

    private final ServerSocket serverSocket;

    public UdtFileUploadServer() {
        try {
            this.serverSocket = new NetServerSocketUDT();
            final SocketAddress serverAddress = 
                new InetSocketAddress("127.0.0.1", 7777);
            serverSocket.bind(serverAddress);
        } catch (final IOException e) {
            throw new RuntimeException("Could not launch server", e);
        }
        //System.setProperty("java.library.path", ".");
        /*
        final SelectorProvider provider = SelectorProviderUDT.DATAGRAM;
        try {
            this.acceptorChannel = provider.openServerSocketChannel();
            this.acceptorChannel.configureBlocking(false);
            this.acceptorSocket = acceptorChannel.socket();
            final InetSocketAddress acceptorAddress = 
                new InetSocketAddress("localhost", 12345);
            acceptorSocket.bind(acceptorAddress);
            
            selector = provider.openSelector();
            acceptorKey = 
                acceptorChannel.register(selector, SelectionKey.OP_ACCEPT);
            
            select();
        } catch (final IOException e) {
            throw new RuntimeException("Could not launch server", e);
        }
        */
    }
    
    public void start() {
        log.info("About to accept...");
        while (true) {
            final Socket sock;
            try {
                sock = serverSocket.accept();
            } catch (final IOException e) {
                log.warn("Exception on accept", e);
                continue;
            }
            copyFile(sock);
        }

        /*
        try {
            final SocketChannel connectorChannel = acceptorChannel.accept();
        } catch (final IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        */
    }

    private void copyFile(final Socket sock) {
        final Runnable runner = new Runnable() {
            public void run() {
                InputStream is = null;
                OutputStream os = null;
                try {
                    is = sock.getInputStream();
                    final byte[] bytes = new byte[1024];
                    final int bytesRead = is.read(bytes);
                    int index = 0;
                    boolean found = false;
                    for (final byte b : bytes) {
                        index++;
                        if (b == '\n') {
                            found = true;
                            break;
                        }
                    }
                    if (index < 2) {
                        // First bytes was a LF?
                        sock.close();
                        return;
                    }
                    if (!found) {
                        
                    }
                    final String fileName = new String(bytes, 0, index).trim();
                    //final BufferedReader br = 
                    //    new BufferedReader(new InputStreamReader(is));
                    //final String fileName = br.readLine();
                    final File file = new File(fileName);
                    os = new FileOutputStream(file);
                    final int len = bytesRead - index;
                    if (len > 0) {
                        os.write(bytes, index, len);
                    }
                    IOUtils.copy(is, os);
                } catch (final IOException e) {
                    log.info("Exception reading file...", e);
                } finally {
                    IOUtils.closeQuietly(is);
                    IOUtils.closeQuietly(os);
                    IOUtils.closeQuietly(sock);
                }
            }
        };
        log.info("Executing copy...");
        readPool.execute(runner);
    }

}