/* BeginGroupMembers */
/* f20170241@hyderabad.bits-pilani.ac.in Tangeda Sai Sharan */
/* f20170104@hyderabad.bits-pilani.ac.in Kanduri Ajith Krishna */
/* f20170128@hyderabad.bits-pilani.ac.in Muppa Manish */
/* f20170023@hyderabad.bits-pilani.ac.in Adapa Sai Vamsi */
/* EndGroupMembers */

/* Brief description of program...*/
/* This code Download's HTML and Logo for google and HTML only for other sites */
/* Code also recognizes 302 errors and redirects to correct url automatically */

package com.sharan;
import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.Security;
import java.util.Arrays;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpProxyDownload {
    public Socket socket;
    public String hostName;
    public int proxyServerPort;
    public String proxyServer;
    public String proxyCredUsername;
    public String proxyCredPassword;
    public String htmlFileName;
    public String logoFileName;
    public String authEncoding;
    public static void main(String[] args) {
        System.out.println(Arrays.toString(args));
        String hostName = args[0];
        String proxyIp = args[1];
        int proxyPort = Integer.parseInt(args[2]);
        String userName = args[3];
        String password = args[4];
        String htmlFileName = args[5];
        String pngFileName = args[6];
        System.setProperty("com.sun.net.ssl.checkRevocation", "true");
        Security.setProperty("ocsp.enable", "true");
        HttpProxyDownload proxyDownload = new HttpProxyDownload(hostName, proxyPort, proxyIp, userName, password,
                htmlFileName, pngFileName);
        proxyDownload.initializeConnection();
        String htmlRes = proxyDownload.getHtml();
        if(htmlRes.equals("302")) {
            System.out.println("Captured 302 Error Redirecting to new url now....");
            proxyDownload.initializeConnection();
            htmlRes = proxyDownload.getHtml();
        }
        proxyDownload.parseImage(htmlRes);
        proxyDownload.closeConnection();
    }
    public HttpProxyDownload(String hostName, int proxyServerPort, String proxyServer, String proxyCredUsername,
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
    public void closeConnection() {
        try {
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
            socket.getOutputStream().write(bytes);
            socket.getOutputStream().flush();
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String inputLine;
            StringBuilder htmlRes = new StringBuilder();
            try {
                if(!(inputLine = in.readLine()).equals(null)) {
                    if(!inputLine.contains("200 OK")) {
                        String error = inputLine.substring(9,12);
                        if(error.equals("302")) {
                            String new_url = in.readLine();
                            new_url = new_url.substring(new_url.indexOf("https://")+8);
                            hostName = new_url;
                            while (!(inputLine = in.readLine()).equals(null)) {
                                if(inputLine.isEmpty()) {
                                    socket.close();
                                    return error;
                                }
                            }
                        }else {
                            System.out.println("RECEIVED A CONNECTION ERROR PLEASE RE-CHECK THE CODE");
                            throw new NullPointerException(inputLine);
                        }
                    }
                }
                while (!(inputLine = in.readLine()).equals(null)) {
                    if(inputLine.startsWith("<!doctype html")  || inputLine.startsWith("<!DOCTYPE html")) {
                        htmlRes.setLength(0);
                        htmlRes.append(inputLine);
                    }else if(inputLine.contains("<!doctype html")  || inputLine.contains("<!DOCTYPE html")) {
                        htmlRes.setLength(0);
                        if (inputLine.contains("<!doctype html")) {
                            int ind = inputLine.indexOf("<!doctype html");
                            htmlRes.append(inputLine.substring(ind));
                        }else {
                            int ind = inputLine.indexOf("<!DOCTYPE html");
                            htmlRes.append(inputLine.substring(ind));
                        }
                    }else {
                        htmlRes.append(inputLine);
                    }
                    if(inputLine.endsWith("</html>")) {
                        break;
                    }
                }
            }catch (NullPointerException nu) {
                System.out.println();
            }
            BufferedWriter fileWriter = new BufferedWriter(new FileWriter(htmlFileName));
            fileWriter.append(htmlRes);
            fileWriter.flush();
            fileWriter.close();
            System.out.println("######################## HTML OUTPUT IS FINISHED #########################");
            return htmlRes.toString();
        }catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void parseImage(String htmlCode) {
        Pattern pattern = Pattern.compile("<img .*? " +
                "src=\".*.png\" " +
                ".* id=\"hplogo\">");
        Matcher matcher = pattern.matcher(htmlCode);
        String path="";
        try {
            if (matcher.find()) {
                path = matcher.group(0);
            }
            if (path.length()==0) {
                throw new NullPointerException("Logo Not available");
            }
            int start_ind = path.indexOf("src=", 0);
            int end_ind = path.indexOf("\"", start_ind+5);
            path = path.substring(start_ind+5, end_ind);
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
            DataInputStream in2 = new DataInputStream(socket.getInputStream());
            OutputStream dos = new FileOutputStream(logoFileName);
            int count,offset;
            byte[] buffer = new byte[2048];
            boolean fl = false;
            while((count = in2.read(buffer)) != -1){
                offset = 0;
                if(!fl){
                    String str = new String(buffer, 0, count);
                    int index = str.indexOf("e\r\n\r\n");
                    if(index!=-1){
                        count = count - index - 5;
                        offset = index+5;
                        fl = true;
                    }
                    else count = 0;
                }
                dos.write(buffer, offset, count);
                dos.flush();
            }
            dos.close();
            System.out.println("######################## LOGO OUTPUT IS FINISHED #########################");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
