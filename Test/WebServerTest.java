import com.company.WebServer;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.Socket;

import static org.junit.Assert.assertEquals;

public class WebServerTest {
    WebServer webServer;
    File root;
    File fileHTML;
    File fileCSS;
    File fileJPG;
    File fileTXT;

    @Before
    public void setup() {
        webServer = new WebServer();
        root = new  File("TestSite");
        fileHTML = new File(root,"typefile.html");
        fileCSS = new File(root,"typefile.css");
        fileJPG = new File(root,"typefile.jpeg");
        fileTXT = new File(root,"typefile.txt");
    }

    @Test
    public void getStatusRunning() {
        webServer.setServerState(1);
        assertEquals(1,webServer.getServerState());
    }

    @Test
    public void getStatusMaintenance() {
        webServer.setServerState(2);
        assertEquals(2,webServer.getServerState());
    }

    @Test
    public void getStatusClosed() {
        webServer.setServerState(3);
        assertEquals(3,webServer.getServerState());
    }

    @Test
    public void getSocketClient() {
        Socket sock = new Socket();
        webServer.setClientSocket(sock);
        assertEquals(sock,webServer.getClientSocket());
    }

    @Test
    public void testHTML() {
        assertEquals("text/html",webServer.getFilePath(fileHTML));
    }

    @Test
    public void testCSS() {
        assertEquals("text/css",webServer.getFilePath(fileCSS));
    }

    @Test
    public void testTXT() {
        assertEquals("text/html",webServer.getFilePath(fileTXT));
    }

    @Test
    public void testPortUnder1024() throws IOException {
        assertEquals(false,webServer.setPortNumber(1023));
    }

    @Test
    public void testPortAboveMax() throws IOException {
        assertEquals(false,webServer.setPortNumber(65536));
    }
}
