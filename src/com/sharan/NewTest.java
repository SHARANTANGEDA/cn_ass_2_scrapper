///* BeginGroupMembers */
///* f20170241@hyderabad.bits-pilani.ac.in Tangeda Sai Sharan */
///* f20170104@hyderabad.bits-pilani.ac.in Kanduri Ajith Krishna */
///* f20170128@hyderabad.bits-pilani.ac.in Muppa Manish */
///* f20170023@hyderabad.bits-pilani.ac.in Adapa Sai Vamsi */
///* EndGroupMembers */
//package com.sharan;
//
//import java.io.*;
//import java.net.InetSocketAddress;
//import java.net.Socket;
//import java.net.URL;
//import java.net.UnknownHostException;
//import java.util.Arrays;
//import java.util.Base64;
//
//class SocketHTTPClient2 {
//    public Socket socket;
//    public String hostName;
//    public int proxyServerPort;
//    public String proxyServer;
//    public String proxyCredUsername;
//    public String proxyCredPassword;
//
//    public SocketHTTPClient2(String hostName, int proxyServerPort, String proxyServer, String proxyCredUsername,
//                            String proxyCredPassword) {
//        this.hostName = hostName;
//        this.proxyServerPort = proxyServerPort;
//        this.proxyServer = proxyServer;
//        this.proxyCredUsername = proxyCredUsername;
//        this.proxyCredPassword = proxyCredPassword;
//    }
//
//    public String readResponse(InputStream in) {
//        try {
//            byte[] reply = new byte[4096];
//            byte[] header = new byte[4096];
//            int replyLen = 0;
//            int headerLen = 0;
//            int newlinesSeen = 0;
//            boolean headerDone = false;
//            int bytesRead = 0;
//            while (newlinesSeen < 2) {
//                int i = in.read();
//                bytesRead+=i;
//                if (i < 0) {
//                    throw new IOException("Unexpected EOF from remote server");
//                }
//                if (i == '\n') {
//                    if (newlinesSeen != 0) {
//                        String h = new String(header, 0, headerLen);
//                        String[] split = h.split(": ");
//                    }
//                    headerDone = true;
//                    ++newlinesSeen;
//                    headerLen = 0;
//                } else if (i != '\r') {
//                    newlinesSeen = 0;
//                    if (!headerDone && replyLen < reply.length) {
//                        reply[replyLen++] = (byte) i;
//                    } else if (headerLen < reply.length) {
//                        header[headerLen++] = (byte) i;
//                    }
//                }
//            }
//            System.out.println("Bytes Read: "+bytesRead);
//            String replyStr;
//            try {
//                replyStr = new String(reply, 0, replyLen, "ASCII7");
//            } catch (UnsupportedEncodingException ignored) {
//                replyStr = new String(reply, 0, replyLen);
//            }
//            return replyStr;
//        }catch (IOException e) {
//            e.printStackTrace();
//        }
//        return null;
//    }
//
//    public void initializeConnection() {
////        InetSocketAddress address = new InetSocketAddress(proxyServer, proxyServerPort);
////        Proxy proxy = new Proxy(Proxy.Type.SOCKS, address);
//        System.out.println("Going to socket");
//        socket = new Socket();
//        try {
//            socket.connect(new InetSocketAddress(proxyServer, proxyServerPort));
//            System.out.println("Connected");
//            String authString = proxyCredUsername + ":" + proxyCredPassword;
//            String encoding = Base64.getEncoder().encodeToString(authString.getBytes());
//            StringBuilder msg = new StringBuilder();
//            msg.append("CONNECT ").append(hostName).append(':').append(443).append(" HTTP/1.1\r\n");
//            msg.append("Proxy-Connection: keep-alive\r\n").append("Connection: keep-alive\r\n");
//            msg.append("Proxy-Authorization: Basic ").append(encoding).append("\r\n").append("\r\n");
//            byte[] bytes;
//            try {
//                bytes = msg.toString().getBytes("ASCII7");
//            } catch (UnsupportedEncodingException ignored) {
//                bytes = msg.toString().getBytes();
//            }
//            socket.getOutputStream().write(bytes);
//            socket.getOutputStream().flush();
//            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//            String inputLine;
//            while (!(inputLine = in.readLine()).equals(""))
//                System.out.println(inputLine);
////            in.close();
//            System.out.println("out");
////            String response1 = readResponse(socket.getInputStream());
////            if (!response1.startsWith("HTTP/1.0 200") && !response1.startsWith("HTTP/1.1 200")) {
////                throw new IOException("Unable to tunnel. Proxy returns \"" + response1 + "\"");
////            }
////            System.out.println(response1);
//            msg.setLength(0);
//            msg.append("CONNECT ").append(hostName).append(':').append(443).append(" HTTP/1.1\r\n");
//            msg.append("Proxy-Connection: keep-alive\r\n").append("Connection: keep-alive\r\n");
//            msg.append("Proxy-Authorization: Basic ").append(encoding).append("\r\n").append("\r\n");
//            try {
//                bytes = msg.toString().getBytes("ASCII7");
//            } catch (UnsupportedEncodingException ignored) {
//                bytes = msg.toString().getBytes();
//            }
//            socket.getOutputStream().write(bytes);
//            socket.getOutputStream().flush();
//            String response1 = readResponse(socket.getInputStream());
//            if (!response1.startsWith("HTTP/1.0 200") && !response1.startsWith("HTTP/1.1 200")) {
//                throw new IOException("Unable to tunnel. Proxy returns \"" + response1 + "\"");
//            }
//            System.out.println(response1);
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    public void testConnection() {
//
//        try {
//            URL url = new URL(this.hostName);
//            socket.connect(new InetSocketAddress(url.getHost(), url.getPort()));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//    }
//    public static void main(String[] args) {
//
//        String hostName = "www.martinbroadhurst.com";
//        int portNumber = 80;
//
//        try {
//            Socket socket = new Socket(hostName, portNumber);
//            PrintWriter out =
//                    new PrintWriter(socket.getOutputStream(), true);
//            BufferedReader in =
//                    new BufferedReader(
//                            new InputStreamReader(socket.getInputStream()));
//            out.println("GET / HTTP/1.1\nHost: www.martinbroadhurst.com\n\n");
//            String inputLine;
//            while ((inputLine = in.readLine()) != null) {
//                System.out.println(inputLine);
//            }
//        } catch (UnknownHostException e) {
//            System.err.println("Don't know about host " + hostName);
//            System.exit(1);
//        } catch (IOException e) {
//            System.err.println("Couldn't get I/O for the connection to " +
//                    hostName);
//            System.exit(1);
//        }
//    }
//}
//
//class NewTest {
//    public static void main(String[] args) {
//        System.out.println(Arrays.toString(args));
//        String hostName = args[0];
//        String proxyIp = args[1];
//        int proxyPort = Integer.parseInt(args[2]);
//        String userName = args[3];
//        String password = args[4];
//        String htmlFileName = args[5];
//        String pngFileName = args[6];
//        System.out.println("Hello");
//        SocketHTTPClient2 SocketHTTPClient2 = new SocketHTTPClient2(hostName, proxyPort, proxyIp, userName, password);
//        SocketHTTPClient2.initializeConnection();
//
//    }
//
//}
