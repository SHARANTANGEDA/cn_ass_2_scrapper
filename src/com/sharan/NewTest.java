/* BeginGroupMembers */
/* f20170241@hyderabad.bits-pilani.ac.in Tangeda Sai Sharan */
/* f20170104@hyderabad.bits-pilani.ac.in Kanduri Ajith Krishna */
/* f20170128@hyderabad.bits-pilani.ac.in Muppa Manish */
/* f20170023@hyderabad.bits-pilani.ac.in Adapa Sai Vamsi */
/* EndGroupMembers */
package com.sharan;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.util.Arrays;
import java.util.Base64;

class SocketHTTP {
    public Socket socket;
    public String hostName;
    public int proxyServerPort;
    public String proxyServer;
    public String proxyCredUsername;
    public String proxyCredPassword;
    public String htmlFileName;
    public String logoFileName;
    public String authEncoding;
    public SocketHTTP(String hostName, int proxyServerPort, String proxyServer, String proxyCredUsername,
                            String proxyCredPassword, String htmlFileName, String logoFileName) {
        this.hostName = hostName;
        this.proxyServerPort = proxyServerPort;
        this.proxyServer = proxyServer;
        this.proxyCredUsername = proxyCredUsername;
        this.proxyCredPassword = proxyCredPassword;
        this.htmlFileName = htmlFileName;
        this.logoFileName = logoFileName;
    }

    public String readResponse(InputStream in) {
        try {
            byte[] reply = new byte[4096];
            byte[] header = new byte[4096];
            int replyLen = 0;
            int headerLen = 0;
            int newlinesSeen = 0;
            boolean headerDone = false;
            int bytesRead = 0;
            while (newlinesSeen < 2) {
                int i = in.read();
                bytesRead+=i;
                if (i < 0) {
                    throw new IOException("Unexpected EOF from remote server");
                }
                if (i == '\n') {
                    if (newlinesSeen != 0) {
                        String h = new String(header, 0, headerLen);
                        String[] split = h.split(": ");
                    }
                    headerDone = true;
                    ++newlinesSeen;
                    headerLen = 0;
                } else if (i != '\r') {
                    newlinesSeen = 0;
                    if (!headerDone && replyLen < reply.length) {
                        reply[replyLen++] = (byte) i;
                    } else if (headerLen < reply.length) {
                        header[headerLen++] = (byte) i;
                    }
                }
            }
            System.out.println("Bytes Read: "+bytesRead);
            String replyStr, headStr;
            try {
                replyStr = new String(reply, 0, replyLen, "ASCII7");
                headStr = new String(header, 0, headerLen, "ASCII7");
            } catch (UnsupportedEncodingException ignored) {
                replyStr = new String(reply, 0, replyLen);
                headStr = new String(header, 0, headerLen, "ASCII7");
            }
            System.out.println("HEAD:"+headStr);
            return replyStr;
        }catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void initializeConnection() {
        System.out.println("Going to socket");
        socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(proxyServer, proxyServerPort));
            System.out.println("Connected");
            String authString = proxyCredUsername + ":" + proxyCredPassword;
            String encoding = Base64.getEncoder().encodeToString(authString.getBytes());
            authEncoding = encoding;
            StringBuilder msg = new StringBuilder();
            msg.append("CONNECT ");
            msg.append(hostName);
            msg.append(':');
            msg.append(443);
            msg.append(" HTTP/1.1\r\n");
            msg.append("Proxy-Connection: keep-alive\r\n");
            msg.append("Connection: keep-alive\r\n");
            msg.append("Proxy-Authorization: Basic ");
            msg.append(encoding);
            msg.append("\r\n");
            msg.append("\r\n");
            byte[] bytes;
            try {
                bytes = msg.toString().getBytes("ASCII7");
            } catch (UnsupportedEncodingException ignored) {
                bytes = msg.toString().getBytes();
            }
            socket.getOutputStream().write(bytes);
            socket.getOutputStream().flush();
            String response1 = readResponse(socket.getInputStream());
            if (!response1.startsWith("HTTP/1.0 200") && !response1.startsWith("HTTP/1.1 200")) {
                throw new IOException("Unable to tunnel. Proxy returns \"" + response1 + "\"");
            }

            SSLSocket s = (SSLSocket) ((SSLSocketFactory) SSLSocketFactory.getDefault())
                    .createSocket(socket, hostName, 443, true);
            s.setEnabledProtocols(new String[] {"TLSv1.3"});
            s.setEnabledCipherSuites(new String[] {"TLS_AES_128_GCM_SHA256"});
            s.setUseClientMode(true);
            s.addHandshakeCompletedListener(
                    new HandshakeCompletedListener()
                    {
                        public void handshakeCompleted( HandshakeCompletedEvent event )
                        {
                            System.out.println( "Handshake finished!" );
                            System.out.println(
                                    "\t CipherSuite:" + event.getCipherSuite() );
                            System.out.println(
                                    "\t SessionId " + event.getSession() );
                            System.out.println(
                                    "\t PeerHost " + event.getSession().getPeerHost() );
                        }
                    } );

            s.startHandshake();
            socket = s;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void closeConnection(SSLSocket s) {
        try {
            s.close();
            socket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getHtml() {
        try {
            StringBuilder msg = new StringBuilder();
            byte[] bytes;
            msg.setLength(0);
            msg.append("GET / ");
            msg.append(" HTTP/1.1\r\n");
            msg.append("HOST: ").append(hostName).append("\r\n");
            msg.append("Connection: keep-alive\r\n");
            msg.append("\r\n");
            try {
                bytes = msg.toString().getBytes("ASCII7");
            } catch (UnsupportedEncodingException ignored) {
                bytes = msg.toString().getBytes();
            }
            System.out.println("Yo");
            socket.getOutputStream().write(bytes);
            socket.getOutputStream().flush();
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String inputLine;
            StringBuilder htmlRes = new StringBuilder();
            try {
                while (!(inputLine = in.readLine()).equals(null)) {
                    System.out.println(inputLine);
                    if(inputLine.startsWith("<!doctype html>")  || inputLine.startsWith("<!DOCTYPE html>")) {
                        htmlRes.setLength(0);
                        System.out.println("THIS:"+inputLine);
                    }
                    htmlRes.append(inputLine);
                    if(inputLine.endsWith("</html>")) {
                        break;
                    }
                }
                System.out.println("out");
            }catch (NullPointerException nu) {
                System.out.println();
            }
            BufferedWriter fileWriter = new BufferedWriter(new FileWriter(htmlFileName));
            fileWriter.append(htmlRes);
            fileWriter.flush();
            fileWriter.close();
            return htmlRes.toString();
        }catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void parseImage(String htmlCode) {
        Document doc = Jsoup.parse(htmlCode);
        Element logo = doc.getElementById("hplogo");
        String path;
        try {
            path = logo.attr("src");
        }catch (NullPointerException nu) {
            System.out.println("################ LOGO COULD NOT BE EXTRACTED AS SITE IS NOT GOOGLE ##################");
            return;
        }

        System.out.println(path);
        byte[] bytes;
        try {
            StringBuilder msg = new StringBuilder();
            System.out.println("------------------------");
            msg.append("GET ").append(path);
            msg.append(" HTTP/1.1\r\n");
            msg.append("HOST: ").append(hostName).append("\r\n");
            msg.append("Connection: close\r\n");
            msg.append("\r\n");
            try {
                bytes = msg.toString().getBytes("ASCII7");
            } catch (UnsupportedEncodingException ignored) {
                bytes = msg.toString().getBytes();
            }
            socket.getOutputStream().write(bytes);
            socket.getOutputStream().flush();
//
//            InputStream inputStream = socket.getInputStream();
//            OutputStream os = new FileOutputStream(new File("./imag.png"));
//
//            byte[] buffer = new byte[8192];
//            int bytesRead;
//            ByteArrayOutputStream output = new ByteArrayOutputStream();
//            while ((bytesRead = inputStream.read(buffer)) != -1) {
//                output.write(buffer, 0, bytesRead);
//            }
//            os.write(buffer);
//            os.close();

            String inputLine;
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder byteStream = new StringBuilder();
            boolean flag=false;
            byte[] b = new byte[6000];
            try {
//                while (!(inputLine = in.readLine()).equals(null)){
////                    System.out.println(inputLine);
//                    byteStream.append(inputLine);
//                    System.out.println(inputLine);
////                    if (flag) {
////                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
////                        outputStream.write( b );
////                        outputStream.write( inputLine.getBytes(StandardCharsets.UTF_8) );
////                        b = outputStream.toByteArray();
////                    }
//                    if(inputLine.equals("Connection: close")) {
//                        byteStream.setLength(0);
//                        flag=true;
//                    }
//                }
                System.out.println("out");
                DataInputStream in2 = new DataInputStream(socket.getInputStream());
                OutputStream dos = new FileOutputStream("testtttt.png");
                int count,y;
                byte[] buffer = new byte[2048];
                boolean fl = false;
                while((count = in2.read(buffer)) != -1){
                    y = 0;
                    if(!fl){
                        String str = new String(buffer, 0, count);
                        int index = str.indexOf("e\r\n\r\n");
                        System.out.println("THIS::::"+str+":::::"+index);

                        if(index!=-1){
                            count = count - index - 5;
                            y = index+5;
                            fl = true;
                        }
                        else count = 0;
                    }
                    String str = new String(buffer, 0, count);
                    System.out.println("THIS IS HAPP:"+str);
                    dos.write(buffer, y, count);
                    dos.flush();
                }
//                while ((count = in2.read(buffer)) != -1)
//                {
//                    System.out.println(Arrays.toString(buffer));
//                    String str = new String(buffer, 0, count);
//                    System.out.println("THIS:"+str);
//                    int index = str.indexOf("Connection: close");
//
//                    if (str.contains("e\r\n\r\n")) {
//                        System.out.println("THIS IS THE ONE:"+str);
//
//                        fl = true;
//                    }
//                    if (fl) {
//                        dos.write(buffer, 0, count);
//                        dos.flush();
//                    }
//                }
                dos.close();

            }catch (NullPointerException nu) {
                System.out.println();
            }
//            OutputStream os = new FileOutputStream(new File("./gm.png"));
//            os.write(b);
//            os.close();
//            BufferedWriter fileWriter = new BufferedWriter(new FileWriter(logoFileName));
//            fileWriter.append(byteStream);
//            fileWriter.flush();
//            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void testConnection() {

        try {
            URL url = new URL(this.hostName);
            socket.connect(new InetSocketAddress(url.getHost(), url.getPort()));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}

class HttpProxy {
    public static void main(String[] args) {
        System.out.println(Arrays.toString(args));
        String hostName = args[0];
        String proxyIp = args[1];
        int proxyPort = Integer.parseInt(args[2]);
        String userName = args[3];
        String password = args[4];
        String htmlFileName = args[5];
        String pngFileName = args[6];
        System.out.println("Hello");
        System.setProperty("com.sun.net.ssl.checkRevocation", "true");
        Security.setProperty("ocsp.enable", "true");
        SocketHTTP socketHTTPClient = new SocketHTTP(hostName, proxyPort, proxyIp, userName, password,
                htmlFileName, pngFileName);
        socketHTTPClient.initializeConnection();
        String htmlRes = socketHTTPClient.getHtml();
        socketHTTPClient.parseImage(htmlRes);
    }

}
