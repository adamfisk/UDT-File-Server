package org.littleshoot.udt.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.barchart.udt.net.NetServerSocketUDT;

public class UdtFileUploadServer {

    private static int count;
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final ExecutorService readPool = Executors.newCachedThreadPool();
    
    //private final ServerSocketChannel acceptorChannel;

    //private final ServerSocket acceptorSocket;

    //private final AbstractSelector selector;

    //private SelectionKey acceptorKey;

    private final ServerSocket serverSocket;
    protected long start;

    public UdtFileUploadServer() {
        final Properties props = new Properties();
        boolean useUdt = true;
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(new File("udt.properties"));
            props.load(fis);
            final String udt = props.getProperty("udt", "true").trim();
            if (udt.equalsIgnoreCase("false")) {
                useUdt = false;
            }
        } catch (final IOException e) {
        }
        finally {
            IOUtils.closeQuietly(fis);
        }
        try {
            if (useUdt) {
                this.serverSocket = new NetServerSocketUDT();
            }
            else {
                this.serverSocket = new ServerSocket();
            }
            final SocketAddress serverAddress = 
                new InetSocketAddress(getLocalHost(), 7777);
            log.info("Server address is: {}", serverAddress);
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
            time();
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
                    int nameIndex = 0;
                    int lengthIndex = 0;
                    boolean foundFirst = false;
                    //boolean foundSecond = false;
                    for (final byte b : bytes) {
                        if (!foundFirst) {
                            nameIndex++;
                        }
                        lengthIndex++;
                        if (b == '\n') {
                            if (foundFirst) {
                                break;
                            }
                            foundFirst = true;
                        }
                    }
                    if (nameIndex < 2) {
                        // First bytes was a LF?
                        sock.close();
                        return;
                    }
                    if (!foundFirst) {
                        
                    }
                    System.out.println("Name Index: "+nameIndex);
                    System.out.println("Length index: "+lengthIndex);
                    final String fileName = new String(bytes, 0, nameIndex).trim();
                    final String lengthString = new String(bytes, nameIndex, lengthIndex).trim();
                    final long length = Long.parseLong(lengthString);
                    System.out.println("Length is: "+length);
                    
                    //final BufferedReader br = 
                    //    new BufferedReader(new InputStreamReader(is));
                    //final String fileName = br.readLine();
                    final File file = new File(fileName);
                    os = new FileOutputStream(file);
                    final int len = bytesRead - nameIndex;
                    if (len > 0) {
                        os.write(bytes, nameIndex, len);
                    }
                    start = System.currentTimeMillis();
                    copy(is, os, length);
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
    
    /**
     * Copy bytes from a large (over 2GB) <code>InputStream</code> to an
     * <code>OutputStream</code>.
     * <p>
     * This method buffers the input internally, so there is no need to use a
     * <code>BufferedInputStream</code>.
     * 
     * @param input  the <code>InputStream</code> to read from
     * @param output  the <code>OutputStream</code> to write to
     * @param length 
     * @return the number of bytes copied
     * @throws NullPointerException if the input or output is null
     * @throws IOException if an I/O error occurs
     * @since Commons IO 1.3
     */
    /*
    private static long copy(InputStream input, OutputStream output)
            throws IOException {
        byte[] buffer = new byte[1024 * 4];
        count = 0;
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }
    */
    
    private long copy(final InputStream input, final OutputStream output, 
        final long length) throws IOException {

        final int DEFAULT_BUFFER_SIZE = 1024 * 4;
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        // long count = 0;
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
            if (count == length) {
                break;
            }
            // System.out.println("Bytes written: "+count);
        }
        final long end = System.currentTimeMillis();
        System.out.println("TOTAL TIME: " + (end - start) / 1000 + " seconds");
        return count;
    }
    
    private void time() {
        final TimerTask tt = new TimerTask() {
            
            @Override
            public void run() {
                final long cur = System.currentTimeMillis();
                final long secs = (cur - start)/1000;
                System.out.println("TRANSFERRED: "+count/1024+" SPEED: "+(count/1024)/secs + "KB/s");
            }
        };
        final Timer t = new Timer();
        t.schedule(tt, 2000, 2000);
    }

    /**
     * Many Linux systems typically return 127.0.0.1 as the localhost address
     * instead of the address assigned on the local network. It has to do with
     * how localhost is defined in /etc/hosts. This method creates a quick
     * UDP socket and gets the local address for the socket on Linux systems
     * to get around the problem. This can also happen on OSX in newer
     * versions of the OS.
     * 
     * @return The local network address in a cross-platform manner.
     * @throws UnknownHostException If the host is considered unknown for 
     * any reason.
     */
    private InetAddress getLocalHost() throws UnknownHostException {
        final InetAddress is = InetAddress.getLocalHost();
        if (!is.isLoopbackAddress()) {
            return is;
        }

        return getLocalHostViaUdp();
    }

    private InetAddress getLocalHostViaUdp() throws UnknownHostException {
        final InetSocketAddress sa = new InetSocketAddress("www.google.com", 80);

        DatagramSocket sock = null;
        try {
            sock = new DatagramSocket();
            sock.connect(sa);
            final InetAddress address = sock.getLocalAddress();
            return address;
        } catch (final SocketException e) {
            log.warn("Exception getting address", e);
            return InetAddress.getLocalHost();
        } finally {
            if (sock != null) {
                sock.close();
            }
        }
    }
}
