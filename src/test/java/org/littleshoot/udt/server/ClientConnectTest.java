package org.littleshoot.udt.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import com.barchart.udt.net.NetSocketUDT;

import junit.framework.TestCase;

public class ClientConnectTest extends TestCase {

    @Test public void testClientConnect() throws Exception {
        final Socket clientSocket = new NetSocketUDT();
        
        final SocketAddress serverAddress = 
            new InetSocketAddress("127.0.0.1", 7777);
        clientSocket.connect(serverAddress);
        
        final File f = new File("pom.xml");
        FileInputStream is = new FileInputStream(f);
        OutputStream os = clientSocket.getOutputStream();
        os.write("pomTest.xml\n".getBytes("UTF-8"));
        IOUtils.copy(is, os);
    }
}
