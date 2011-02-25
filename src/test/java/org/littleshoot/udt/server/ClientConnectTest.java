package org.littleshoot.udt.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Timer;
import java.util.TimerTask;

import junit.framework.TestCase;

import org.junit.Test;

import com.barchart.udt.net.NetSocketUDT;

public class ClientConnectTest extends TestCase {

    private final long start = System.currentTimeMillis();
    private int count = 0;

    @Test public void testClientConnect() throws Exception {
        final Socket clientSocket = new NetSocketUDT();
        //final Socket clientSocket = new Socket();
        
        final SocketAddress serverAddress = 
            new InetSocketAddress("75.101.156.158", 7777);
        clientSocket.connect(serverAddress);
        
        System.out.println("Connected!!");
        //final File f = new File("pom.xml");
        final File f = new File("LittleShootPlugin.tgz");
        FileInputStream is = new FileInputStream(f);
        OutputStream os = clientSocket.getOutputStream();
        time();
        os.write("LittleShootPlugin.tgz.test\n".getBytes("UTF-8"));
        //IOUtils.copy(is, os);
        copy(is, os);
        Thread.sleep(100 * 1000);
    }
    
    private long copy(final InputStream input, final OutputStream output)
            throws IOException {
        
        final int DEFAULT_BUFFER_SIZE = 1024 * 4;
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        //long count = 0;
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            this.count += n;
            //System.out.println("Bytes written: "+count);
        }
        final long end = System.currentTimeMillis();
        System.out.println("TOTAL TIME: "+(end-start)/1000 + " seconds");
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
}
