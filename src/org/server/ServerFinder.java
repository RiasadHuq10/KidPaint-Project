package org.server;

import org.KidPaint;
import org.gui.UserNameInput;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;

public class ServerFinder {
    private static ServerFinder instance = null;
    DatagramSocket socket;
    int port = KidPaint.getFreeUDPPort();
    ArrayList<String[]> servers;
    DatagramSocket responderSocket;
    static int[] serverFinderPortList = new int[]{22222,33333,44444};

    public static ServerFinder getInstance() {
        if (instance == null) {
            instance = new ServerFinder();
        }
        return instance;
    }
    public static void kill(){
        ServerFinder sf = ServerFinder.getInstance();
        sf.socket.close();
        if (sf.responderSocket != null) sf.responderSocket.close();
        instance = null;
    }


    private ServerFinder() {
        servers = new ArrayList<>();


        try {
            socket = new DatagramSocket(port);

        } catch (SocketException e) {
            System.out.println(port);
            throw new RuntimeException(e);
        }

        sendServerFindRequest();
    }



    private void sendServerFindRequest() {
        try {
            InetAddress broadcastAddress = InetAddress.getByName("255.255.255.255");
            String str = "Servers please respond!";
            for (int port : ServerFinder.serverFinderPortList) {
                DatagramPacket packet = new DatagramPacket(str.getBytes(), str.length(), broadcastAddress, port);
                socket.send(packet);
            }
        } catch (IOException e) {
            System.out.println("Server finding failed");
            e.printStackTrace();
        }

        //wait for response from servers
        Thread t = new Thread(() -> {
            while (true) {
                DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
                try {
                    socket.receive(packet);
                } catch (SocketException e) {
                    System.out.println(e);
                    return;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                byte[] data = packet.getData();
                String str = new String(data, 0, packet.getLength());
                if (str.startsWith("Server:")) {
                    //Server:~<username>~<server port>
                    System.out.println(str);
                    String[] serverData = str.split("~");
                    String username = serverData[1];
                    String[] server= new String[3];
                    server[0]=username;
                    server[1]= packet.getAddress().getHostName();
                    server[2] = serverData[2];
                    servers.add(server);
                }
            }
        });
        t.start();

    }

    public static void launchServerResponder(int myServerPort){
        DatagramSocket responderSocket = null;

        for (int i = 0; i < ServerFinder.serverFinderPortList.length; i++){
            int port = ServerFinder.serverFinderPortList[i];
            try {
                responderSocket = new DatagramSocket(port);
                System.out.println("Setting up server responder at port" + port);
                break;
            } catch (SocketException e){
            }
            if (i == ServerFinder.serverFinderPortList.length - 1){
                System.out.println("Maximum Kidpaint servers active");
                System.out.println(" ( could be that kidpaint ports are in use by other system processes )");
                System.exit(1);
            }
        }




        DatagramSocket finalResponderSocket = responderSocket;
        Thread t = new Thread(() -> {
            while (true) {
                DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
                try {
                    finalResponderSocket.receive(packet);
                } catch (SocketException e) {
                    System.out.println(e);
                    return;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                byte[] data = packet.getData();
                String str = new String(data, 0, packet.getLength());
                InetAddress srcAddr = packet.getAddress();
                int srcPort = packet.getPort();
                if (str.equals("Servers please respond!")) {
                    if (InternalServer.isRunning()){
                        String myUsername = UserNameInput.getUserName();
                        String strr = "Server:~"+myUsername + "~" + myServerPort;
                        packet = new DatagramPacket(strr.getBytes(), strr.length(), srcAddr, srcPort);
                        try {
                            finalResponderSocket.send(packet);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }

                }
            }

        });
        t.start();
        ServerFinder.getInstance().responderSocket = responderSocket;
    }

    public static ArrayList<String[]> getServers(){
        try {
            Thread.sleep(500);
        } catch (InterruptedException e){

        }
        ArrayList<String[]> servers = ServerFinder.getInstance().servers;
        return servers;
    }


}
