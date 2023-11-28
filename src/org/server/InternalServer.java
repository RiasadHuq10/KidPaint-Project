package org.server;

import org.gui.ChatArea;
import org.KidPaint;
import org.gui.UI;
import org.gui.UserNameInput;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class InternalServer {
    private static InternalServer instance = null;
    private final ArrayList<Socket> subscribers;
    int port = KidPaint.getFreeTCPPort();;

    ServerSocket internalServerSocket;

    public static InternalServer getInstance() {
        if (instance == null) {
            instance = new InternalServer();
        }

        return instance;
    }
    public static void kill(){
        try {
            getInstance().internalServerSocket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        instance = null;
    }

    public static boolean isRunning() {
        return (instance != null);
    }

    private InternalServer() {
        System.out.println("Internal Server started");
        subscribers = new ArrayList<>();
        ServerFinder.launchServerResponder(port);


        try {

            internalServerSocket = new ServerSocket(port);

            System.out.println("Internal server socket listening at port : " + port);
            Thread runner = new Thread(() -> {
                run();
            });
            runner.start();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    public static String getInvitationCode() {
        try {

            String myPublicIp = "";

            String[] publicIpCheckers = new String[]{
                    "https://checkip.amazonaws.com/",
                    "https://ipv4.icanhazip.com/",
                    "http://myexternalip.com/raw",
                    "http://ipecho.net/plain"
            };

            for (String urlString : publicIpCheckers) {
                URL url = new URL(urlString);
                try (BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()))) {
                    myPublicIp = (br.readLine());
                    System.out.println(myPublicIp);
                    break;
                } catch (IOException e) {
                    System.out.println("Failed to read my public ip from " + urlString);
                }
            }

            if (myPublicIp.isEmpty()) {
                System.out.println("Generating Invitation Code failed");
            }

            return KidPaint.encode(myPublicIp, String.valueOf(InternalServer.getInstance().port));

        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void run() {
        while (true) {

            try {
                Socket clientSocket = internalServerSocket.accept();
                synchronized (subscribers) {
                    subscribers.add(clientSocket);
                }

                Thread t = new Thread(() -> {
                    try {
                        sendStateToNewJoiner(clientSocket);
                        serve(clientSocket);
                    } catch (IOException ex) {

                    }
                    synchronized (subscribers) {
                        subscribers.remove(clientSocket);
                    }

                });
                t.start();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

        }
    }


    private void serve(Socket clientSocket) throws IOException {
        System.out.println("Internal Server is now serving " + clientSocket.getInetAddress());
        DataInputStream in = new DataInputStream(clientSocket.getInputStream());


        while (true) {
            int type = in.readInt();// type represents the message type
            System.out.println("Got message of type " + type + " from" + clientSocket.getInetAddress());

            switch (type) {
                case 0:

                    handleChatMessage(in);
                    //save chat locally
                    break;
                case 1:
                    handlePixelMessage(in);
                    break;
                case 2:
                    handleBucketMessage(in);
                    break;
                case 3:
                    //a message indicating a load comman. This message will be of the form, (int messageCode = 4) (long sizeOfFile) + (bytes)
                    handleLoadFile(in);

                case 4:
                    handleFileMessage(in);
                    break;
                case 5:
                    handleImageMessage(in);
                    break;

                default:

            }

        }
    }

    private void handleChatMessage(DataInputStream in) throws IOException {
        byte[] userNameBuffer = new byte[1024];
        int userNameLen = in.readInt();
        in.read(userNameBuffer, 0, userNameLen);

        byte[] chatBuffer = new byte[1024];
        int chatLen = in.readInt();
        in.read(chatBuffer, 0, chatLen);

        ChatArea.getInstance().addMessage(new String(chatBuffer, 0, chatLen), new String(userNameBuffer, 0, userNameLen));
        synchronized (subscribers) {
            System.out.println("" + subscribers.size() + "Subscribers");
            for (int i = 0; i < subscribers.size(); i++) {
                System.out.println("Sending data to sub: " + subscribers.get(i));
                try {
                    Socket s = subscribers.get(i);
                    DataOutputStream out = new DataOutputStream(s.getOutputStream());
                    out.writeInt(0);//type
                    out.writeInt(userNameLen);
                    out.write(userNameBuffer, 0, userNameLen);
                    out.writeInt(chatLen);
                    out.write(chatBuffer, 0, chatLen);
                    out.flush();
                } catch (IOException ex) {
                    System.out.println("Client already disconnected");
                }

            }
        }
    }


    private void handlePixelMessage(DataInputStream in) throws IOException {
        int color = in.readInt();
        int x = in.readInt();
        int y = in.readInt();
        int pentype = in.readInt();
        int pensize = in.readInt();

        UI.getInstance().paintPixel(color, x, y, pentype, pensize);
        synchronized (subscribers) {
            for (int i = 0; i < subscribers.size(); i++) {
                try {
                    Socket s = subscribers.get(i);
                    DataOutputStream out = new DataOutputStream(s.getOutputStream());
                    out.writeInt(1); // type
                    out.writeInt(color);
                    out.writeInt(x);
                    out.writeInt(y);
                    out.writeInt(pentype);
                    out.writeInt(pensize);
                    out.flush();
                } catch (IOException ex) {
                    System.out.println("Client already disconnected");
                    ex.printStackTrace();
                }

            }
        }
    }

    private void handleBucketMessage(DataInputStream in) throws IOException {
        int color = in.readInt();
        int x = in.readInt();
        int y = in.readInt();
        UI.getInstance().paintArea(color, x, y);

        synchronized (subscribers) {
            for (int i = 0; i < subscribers.size(); i++) {
                try {
                    Socket s = subscribers.get(i);
                    DataOutputStream out = new DataOutputStream(s.getOutputStream());
                    out.writeInt(2); // type
                    out.writeInt(color);
                    out.writeInt(x);
                    out.writeInt(y);
                    out.flush();
                } catch (IOException ex) {
                    System.out.println("Client already disconnected");
                    ex.printStackTrace();
                }

            }
        }
    }

    private void handleFileMessage(DataInputStream in) throws IOException {

        byte[] userNameBuffer = new byte[1024];
        int userNameLen = in.readInt();
        in.read(userNameBuffer, 0, userNameLen);

        byte[] buffer = new byte[1024];
        int fileNameSize = in.readInt();

        in.read(buffer, 0, fileNameSize);
        String fileName = new String(buffer, 0, fileNameSize);
        System.out.println("Downloading File"+ fileName);


        long fileSize = in.readLong();

        File outputFile = new File(fileName);
        FileOutputStream out = new FileOutputStream(outputFile);

        while (fileSize > 0) {
            int len = in.read(buffer, 0, buffer.length);
            out.write(buffer, 0, len);
            fileSize = fileSize - len;
        }
        System.out.println("Download complete");
        out.close();


        ChatArea.getInstance().addFileMessage(outputFile, new String(userNameBuffer,0,userNameLen));

        File f = new File(fileName);

        synchronized (subscribers) {

            for (int i = 0; i < subscribers.size(); i++) {
                try {
                    FileInputStream insend = new FileInputStream(f);
                    Socket s = subscribers.get(i);
                    DataOutputStream outsend = new DataOutputStream(s.getOutputStream());
                    outsend.writeInt(4); // type
                    outsend.writeInt(userNameLen); //usernamelen
                    outsend.write(userNameBuffer, 0, userNameLen);
                    outsend.writeInt(f.getName().length()); //fileName
                    outsend.write(f.getName().getBytes());

                    long size = f.length();
                    outsend.writeLong(size);

                    System.out.printf("Uploading file %s (%d)", f.getName(), size);
                    while (size > 0) {
                        int len = insend.read(buffer, 0, buffer.length);
                        outsend.write(buffer, 0, len);
                        size -= len;

                    }
                    outsend.flush();
                    insend.close();
                } catch (IOException ex) {
                    System.out.println("Client already disconnected");
                    ex.printStackTrace();
                }

            }
        }
    }


    private void handleImageMessage(DataInputStream in) throws IOException {

        byte[] userNameBuffer = new byte[1024];
        int userNameLen = in.readInt();
        in.read(userNameBuffer, 0, userNameLen);
        String username = new String(userNameBuffer, 0, userNameLen);

        byte[] buffer = new byte[1024];
        int imageNameSize = in.readInt();

        in.read(buffer, 0, imageNameSize);
        String imageName = new String(buffer, 0, imageNameSize);
        System.out.println("Downloading Image");


        long imageSize = in.readLong();

        File outputImage = new File(imageName);
        FileOutputStream out = new FileOutputStream(outputImage);

        while (imageSize > 0) {
            int len = in.read(buffer, 0, buffer.length);
            out.write(buffer, 0, len);
            imageSize = imageSize - len;
        }
        System.out.println("Download complete");
        out.close();

        ChatArea.getInstance().addImageMessage(outputImage, username);


        //UI.getInstance().paintArea(color, x, y);

        File f = new File(imageName);

        synchronized (subscribers) {

            for (int i = 0; i < subscribers.size(); i++) {
                try {
                    FileInputStream insend = new FileInputStream(f);
                    Socket s = subscribers.get(i);
                    DataOutputStream outsend = new DataOutputStream(s.getOutputStream());
                    outsend.writeInt(5); // type
                    outsend.writeInt(userNameLen); //usernamelen
                    outsend.write(userNameBuffer, 0, userNameLen); //username
                    outsend.writeInt(f.getName().length()); //filename
                    outsend.write(f.getName().getBytes()); //filename

                    long size = f.length();
                    outsend.writeLong(size);

                    System.out.printf("Uploading image %s (%d)", f.getName(), size);
                    while (size > 0) {
                        int len = insend.read(buffer, 0, buffer.length);
                        outsend.write(buffer, 0, len);
                        size -= len;
                    }
                    outsend.flush();
                    insend.close();
                } catch (IOException ex) {
                    System.out.println("Client already disconnected");
                    ex.printStackTrace();
                }

            }
        }
    }


    public void sendMyChatMessage(String chatMessage) {
        try {
            byte[] username = UserNameInput.getUserName().getBytes();
            int userNameLen = username.length;
            byte[] chatBuffer = chatMessage.getBytes();
            int chatLen = chatBuffer.length;

            synchronized (subscribers) {
                for (int i = 0; i < subscribers.size(); i++) {
                    System.out.println("Sending data to sub: " + subscribers.get(i));
                    try {
                        Socket s = subscribers.get(i);
                        DataOutputStream out = new DataOutputStream(s.getOutputStream());
                        out.writeInt(0);//type
                        out.writeInt(userNameLen);
                        out.write(username, 0, userNameLen);
                        out.writeInt(chatLen);
                        out.write(chatBuffer, 0, chatLen);
                        out.flush();
                    } catch (IOException ex) {
                        System.out.println("Client already disconnected");
                        ex.printStackTrace();
                    }

                }
            }
        } catch (Exception e) {
            System.out.println("Error here handle my ownmessage");
            e.printStackTrace();
        }
    }

    public void sendMyPixelMessage(int color, int x, int y, int penType, int penSize) {
        try {
            synchronized (subscribers) {
                for (int i = 0; i < subscribers.size(); i++) {
                    try {
                        Socket s = subscribers.get(i);
                        DataOutputStream out = new DataOutputStream(s.getOutputStream());
                        out.writeInt(1); // type
                        out.writeInt(color);
                        out.writeInt(x);
                        out.writeInt(y);
                        out.writeInt(penType);
                        out.writeInt(penSize);
                        out.flush();
                    } catch (IOException ex) {
                        System.out.println("Client already disconnected");
                        ex.printStackTrace();
                    }

                }
            }
        } catch (Exception e) {
            System.out.println("Error here handlemyownmessage");
            e.printStackTrace();
        }
    }

    public void sendMyBucketMessage(int color, int x, int y) {
        try {
            synchronized (subscribers) {
                for (int i = 0; i < subscribers.size(); i++) {
                    try {
                        Socket s = subscribers.get(i);
                        DataOutputStream out = new DataOutputStream(s.getOutputStream());
                        out.writeInt(2); // type
                        out.writeInt(color);
                        out.writeInt(x);
                        out.writeInt(y);
                        out.flush();
                    } catch (IOException ex) {
                        System.out.println("Client already disconnected");
                        ex.printStackTrace();
                    }

                }
            }
        } catch (Exception e) {
            System.out.println("Error here handlemyownmessage");
            e.printStackTrace();
        }
    }

    // TODO

    /**
     * send load file to clients
     * implemnt thge methods of sending your message
     */

    private void handleLoadFile(DataInputStream in) throws IOException {
        byte[] userNameBuffer = new byte[1024];
        int userNameLen = in.readInt();
        in.read(userNameBuffer, 0, userNameLen);

        byte[] buffer = new byte[1024];
        int fileNameSize = in.readInt();

        in.read(buffer, 0, fileNameSize);
        String fileName = new String(buffer, 0, fileNameSize);
        System.out.println("Downloading File");


        long fileSize = in.readLong();

        File file = new File(fileName);
        FileOutputStream out = new FileOutputStream(file);

        while (fileSize > 0) {
            int len = in.read(buffer, 0, buffer.length);
            out.write(buffer, 0, len);
            fileSize = fileSize - len;
        }

        //loaded file into File


        int[][][] output = new int[2][100][100];

        int[][][] outputDataArray = new int[1][][];
        boolean[] success = new boolean[1];
        int[] outputBlockSize = new int[1];
        UI.loadDrawingFromFileIntoDataArray(file, outputDataArray, outputBlockSize, success);
        if (!success[0]) {
            System.out.println("Loading failed at server: unable to properly load the file sent by the client");
            return;
        }
        //update the server's own data array
        UI.getInstance().loadDrawingLocally(outputDataArray[0], outputBlockSize[0]);

//        send this new data to all subscribers
        synchronized (subscribers) {
            System.out.println("" + subscribers.size() + "Subscribers");
            for (int i = 0; i < subscribers.size(); i++) {
                try {
                    FileInputStream insend = new FileInputStream(file);
                    Socket s = subscribers.get(i);
                    DataOutputStream outsend = new DataOutputStream(s.getOutputStream());
                    outsend.writeInt(3); // type
                    outsend.writeInt(userNameLen); //usernamelen
                    outsend.write(userNameBuffer, 0, userNameLen);
                    outsend.writeInt(file.getName().length()); //fileName
                    outsend.write(file.getName().getBytes());

                    long size = file.length();
                    outsend.writeLong(size);

                    System.out.printf("Sending loaded file to all subscribers %s (%d)", file.getName(), size);
                    while (size > 0) {
                        int len = insend.read(buffer, 0, buffer.length);
                        outsend.write(buffer, 0, len);
                        size -= len;

                    }
                    outsend.flush();
                    insend.close();
                } catch (IOException ex) {
                    System.out.println("Client already disconnected");
                    ex.printStackTrace();
                }

            }
        }

    }


    private void sendStateToNewJoiner(Socket clientSocket) {
        try {
            /**
             * send a message code 3
             */

            File f = new File("lateststate.kpt");
            UI.getInstance().saveDrawingToFile(f);


            String Username = UserNameInput.getUserName();
            byte userNameBuffer[] = Username.getBytes();

            FileInputStream inL = new FileInputStream(f);
            Socket s = clientSocket;
            DataOutputStream outsend = new DataOutputStream(s.getOutputStream());
            outsend.writeInt(3); // type
            outsend.writeInt(userNameBuffer.length); //usernamelen
            outsend.write(userNameBuffer, 0, userNameBuffer.length);

            outsend.writeInt(f.getName().length()); //filename
            outsend.write(f.getName().getBytes()); //filename

            long size = f.length();
            outsend.writeLong(size);

            byte[] buffer = new byte[1024];
            System.out.printf("Sending state to new joiner %s (%d)", f.getName(), size);
            while (size > 0) {
                int len = inL.read(buffer, 0, buffer.length);
                outsend.write(buffer, 0, len);
                size -= len;
            }
            outsend.flush();
            inL.close();

        } catch (Exception e) {

        }

    }

    public void loadMyFile(File file) throws FileNotFoundException {
        //Load file locally

        int[][][] outputDataArray = new int[1][1][1];
        boolean[] success = new boolean[1];
        int[] outputBlockSize = new int[1];
        UI.loadDrawingFromFileIntoDataArray(file, outputDataArray, outputBlockSize, success);
        if (!success[0]) {
            System.out.println("Loading failed at server: unable to properly load the file sent by the client");
            return;
        }
        //update the server's own data array
        UI.getInstance().loadDrawingLocally(outputDataArray[0], outputBlockSize[0]);

        //send file to subscribers
        try {
            /**
             *
             * //
             *         //TODO (File is already given as parameter. No need to read anything)
             *         //read file from datainputstream into file object
             * //        in.read(userNameBuffer, 0, userNameLen);
             *
             *
             */

            String Username = UserNameInput.getUserName();
            byte userNameBuffer[] = Username.getBytes();


            synchronized (subscribers) {
                System.out.println("" + subscribers.size() + "Subscribers");
                for (int i = 0; i < subscribers.size(); i++) {
                    try {
                        FileInputStream insend = new FileInputStream(file);
                        Socket s = subscribers.get(i);
                        DataOutputStream outsend = new DataOutputStream(s.getOutputStream());
                        outsend.writeInt(3); // type
                        outsend.writeInt(userNameBuffer.length); //usernamelen
                        outsend.write(userNameBuffer, 0, userNameBuffer.length);
                        outsend.writeInt(file.getName().length()); //fileName
                        outsend.write(file.getName().getBytes());

                        long size = file.length();
                        outsend.writeLong(size);
                        byte[] buffer = new byte[1024];
                        System.out.printf("Sending loaded file to all subscribers %s (%d)", file.getName(), size);
                        while (size > 0) {
                            int len = insend.read(buffer, 0, buffer.length);
                            outsend.write(buffer, 0, len);
                            size -= len;

                        }
                        outsend.flush();
                        insend.close();
                    } catch (IOException ex) {
                        System.out.println("Load my file error Internal server");
                        ex.printStackTrace();
                    }

                }
            }
        } catch (Exception e) {
            System.out.println("Load my file error Internal server");
        }

    }

    public void sendMyImageMessage(File file) {
        String username = UserNameInput.getUserName();
        synchronized (subscribers) {

            for (int i = 0; i < subscribers.size(); i++) {
                try {
                    FileInputStream insend = new FileInputStream(file);
                    Socket s = subscribers.get(i);
                    DataOutputStream outsend = new DataOutputStream(s.getOutputStream());
                    byte[] buffer = new byte[1024];
                    outsend.writeInt(5); // type
                    outsend.writeInt(username.length()); //usernamelen
                    outsend.write(username.getBytes(), 0, username.length()); //username
                    outsend.writeInt(file.getName().length()); //filename
                    outsend.write(file.getName().getBytes()); //filename

                    long size = file.length();
                    outsend.writeLong(size);

                    System.out.printf("Uploading image %s (%d)", file.getName(), size);
                    while (size > 0) {
                        int len = insend.read(buffer, 0, buffer.length);
                        outsend.write(buffer, 0, len);
                        size -= len;
                    }
                    outsend.flush();
                    insend.close();
                } catch (IOException ex) {
                    System.out.println("Client already disconnected");
                    ex.printStackTrace();
                }

            }
        }
    }

    public void sendMyFileMessage(File file) {
        String username = UserNameInput.getUserName();
            synchronized (subscribers) {
                for (int i = 0; i < subscribers.size(); i++) {
                    try {
                        Socket s = subscribers.get(i);
                        DataOutputStream out = new DataOutputStream(s.getOutputStream());
                        FileInputStream in = new FileInputStream(file);
                        byte[] buffer = new byte[1024];

                        out.writeInt(4); // type

                        out.writeInt(username.length());
                        out.write(username.getBytes(), 0, username.length());

                        out.writeInt(file.getName().length());
                        out.write(file.getName().getBytes());

                        long size = file.length();
                        out.writeLong(size);

                        while (size > 0) {
                            int len = in.read(buffer, 0, buffer.length);
                            out.write(buffer, 0, len);
                            size -= len;
                            System.out.print(".");
                        }
                        out.flush();
                        in.close();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

    }

}

