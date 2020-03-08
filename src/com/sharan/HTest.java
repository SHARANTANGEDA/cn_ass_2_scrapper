package com.sharan;//Summary:
//1. Connected to google through squid proxy (using HTTP CONNECT method)
//2. Got html code from google page (using HTTP GET method)
//3. Got image from google page (using HTTP GET method)

import java.io.*;
import java.net.Socket;


class HttpProxyH{

    //function to encode into base64
    static String base64encoding(String str){
        String x = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
        char[] char_set = x.toCharArray();
        int length = str.length();
        char[] ans = new char[100];
        int index, no_of_bits = 0, padding = 0, val = 0, counter = 0, tmp;
        int i, j, k = 0;
        for(i=0; i<length; i+=3){
            val = 0;counter = 0;no_of_bits = 0;
            for(j=i; j<length&&j<=i+2; j++){
                val = val<<8;
                val = val | str.charAt(j);
                counter++;
            }
            no_of_bits = counter*8;
            padding = no_of_bits%3;

            while(no_of_bits !=0){
                if(no_of_bits>=6){
                    tmp = no_of_bits - 6;
                    index = (val>>tmp) & 63;
                    no_of_bits -= 6;
                }
                else{
                    tmp = 6 - no_of_bits;
                    index = (val<<tmp) & 63;
                    no_of_bits = 0;
                }
                ans[k++] = char_set[index];
            }
        }
        for(i=1; i<=padding; ++i){
            ans[k++] = '=';
        }
        ans[k] = '\0';

        String result = "";
        for(i=0; i<k; ++i)
            result += ans[i];
        return result;
    }

    public static void main(String args[]) throws Exception{

        //command line arguments
        String site = args[0];
        String host = args[1];
        int port = Integer.parseInt(args[2]);
        String uname = args[3];
        String passwd = args[4];
        String html_file = args[5];
        String logo_file = args[6];

        //credentials for authorization
        String auth = base64encoding(uname+':'+passwd);


        //1. Connecting to www.google.com through squid
        Socket s = new Socket(host,port);
        PrintWriter w = new PrintWriter(s.getOutputStream());

        //connect request
        w.print("CONNECT "+site+":443 HTTP/1.0\r\nProxy-Connection: keep-alive\r\nHost:"+site+":443\r\nProxy-Authorization: Basic "+auth+"\r\n\r\n");
        w.flush();

        s.close();
        w.close();


        ////////////////////////////////////////////////////////////////////////
        //2. getting html text from google

        s = new Socket(host,port);
        w = new PrintWriter(s.getOutputStream());

        //get request for html file
        w.print("GET http://"+site+" HTTP/1.0\r\nHost: "+host+":"+port+"\r\nProxy-Authorization: Basic "+auth+"\r\n\r\n");
        w.flush();

        DataInputStream din = new DataInputStream(s.getInputStream());
        OutputStream opfile = new FileOutputStream(html_file);

        //loop to eliminate HTTP response headers at the beginning of the html code
        //we recognise the end of the HTTP response headers using "\r\n\r\n"
        int z,y;
        byte[] data = new byte[2048];
        boolean eoh = false;

        while((z = din.read(data)) != -1){
            y = 0;
            if(!eoh){
                String str = new String(data, 0, z);
                int index = str.indexOf("\r\n\r\n");
                if(index!=-1){
                    z = z - index - 4;
                    y = index+4;
                    eoh = true;
                }
                else z = 0;
            }
            opfile.write(data, y, z);
            opfile.flush();
        }

        s.close();
        din.close();
        w.close();
        opfile.close();

        //getting logo path from html file
        String img_path = "";
        File f = new File(html_file);
        FileReader fr = new FileReader(f);
        BufferedReader br = new BufferedReader(fr);
        int flag = 0, img = 0, c = 0;

        //finding "<img" in the html file
        //and adding all characters between "\ ".
        while((c=br.read())!=-1){
            if((char)c=='<'&&img==0) img=1;
            else if((char)c=='i'&&img==1) img=2;
            else if((char)c=='m'&&img==2) img=3;
            else if((char)c=='g'&&img==3){
                flag = 1;
            }
            else img=0;

            if((char)c=='\"'&&flag==1){
                c=br.read();
                if((char)c=='/'){
                    img_path+=(char)c;
                    flag = 2;
                }
                continue;
            }
            else if((char)c=='\"'&&flag==2){
                flag = 3;
            }
            if(flag==2){
                img_path += (char)c;
            }
            if(flag==3) break;
        }
        br.close();

        ////////////////////////////////////////////////////////////////////////
        //3. getting image from google


        s = new Socket(host,port);
        w = new PrintWriter(s.getOutputStream());


        //get request for image png file
        w.print("GET http://"+site+img_path+" HTTP/1.0\r\nHost: "+host+":"+port+"\r\nProxy-Authorization: Basic "+auth+"\r\n\r\n");
        w.flush();

        DataInputStream in = new DataInputStream(s.getInputStream());
        OutputStream dos = new FileOutputStream(logo_file);

        //loop to eliminate HTTP response headers at the beginning of the image
        //we recognise the end of the HTTP response headers using "\r\n\r\n"
        z = 0;
        y = 0;
        data = new byte[2048];
        eoh = false;
        while((z = in.read(data)) != -1){
            y = 0;
            if(!eoh){
                String str = new String(data, 0, z);
                int index = str.indexOf("\r\n\r\n");
                if(index!=-1){
                    z = z - index - 4;
                    y = index+4;
                    eoh = true;
                }
                else z = 0;
            }
            dos.write(data, y, z);
            dos.flush();
        }

        in.close();
        dos.close();
        s.close();
    }
}