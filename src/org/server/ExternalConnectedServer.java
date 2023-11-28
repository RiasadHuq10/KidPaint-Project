package org.server;

import org.gui.ChatArea;
import org.KidPaint;
import org.gui.UI;
import org.gui.UserNameInput;

import java.io.*;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.Socket;

public class ExternalConnectedServer {
    private static ExternalConnectedServer instance = null;
    private boolean isIntialised = false;
    private Socket externalSocket = null;
    private DataInputStream in = null;
    private DataOutputStream out = null;


    public static ExternalConnectedServer getInstance() {
        if (instance == null) {
            instance = new ExternalConnectedServer();
        }
        return instance;
    }

    public static void kill(){
        try {
            System.out.println("killing socket now");
            if (getInstance().externalSocket != null) getInstance().externalSocket.close();
        } catch (IOException e) {
            System.out.println("found you exception");
            throw new RuntimeException(e);
        }
        instance = null;
    }

    public void initialiseServer(String[] serverDetails) {
        String username = serverDetails[0];
        String ip = serverDetails[1];
        int port = Integer.parseInt(serverDetails[2]);
        System.out.println("Connected server's port is " + port);
        InetAddress server;
        try {
            server = InetAddress.getByName(ip);
            externalSocket = new Socket(server.getHostAddress(), port);
            in = new DataInputStream(externalSocket.getInputStream());
            out = new DataOutputStream(externalSocket.getOutputStream());
        } catch (Exception e) {
            System.out.println("This is an immediate exception " + e.getMessage());

            KidPaint.serverNotFound();
            return;
            //Run ServerDead protocol
        }


        this.externalSocket = externalSocket;
        listenToExternalServer();
        this.isIntialised = true;

        ChatArea.getInstance().addBannerAlert("Joining " + username + "'s studio ");

    }

    private ExternalConnectedServer() {

    }

    public void sendChatMessage(String message) {
        String username = UserNameInput.getUserName();
        try {

            out.writeInt(0);//type
            out.writeInt(username.length());
            out.write(username.getBytes(), 0, username.length());
            out.writeInt(message.length());
            out.write(message.getBytes(), 0, message.length());
            out.flush();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    public void sendPixelMessage(int color, int x, int y,  int penType,int penSize) {

        try {

            out.writeInt(1); // type
            out.writeInt(color);
            out.writeInt(x);
            out.writeInt(y);
            out.writeInt(penType);
            out.writeInt(penSize);
            out.flush();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    public void sendBucketMessage(int x, int y, int color) {

        try {

            out.writeInt(2); // type
            out.writeInt(color);
            out.writeInt(x);
            out.writeInt(y);
            out.flush();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    public void sendFileMessage(File file) {
        String username = UserNameInput.getUserName();
        try {
            FileInputStream in = new FileInputStream(file);
            byte[] buffer = new byte[1024];

            out.writeInt(4); // type

            out.writeInt(username.length());
            out.write(username.getBytes(), 0, username.length());

            out.writeInt(file.getName().length());
            out.write(file.getName().getBytes());

            long size = file.length();
            out.writeLong(size);

            System.out.printf("Uploading file %s (%d)", file.getName(), size);
            while(size > 0) {
                int len = in.read(buffer, 0, buffer.length);
                out.write(buffer, 0, len);
                size -= len;
                System.out.print(".");
            }

        } catch (Exception e) {
            System.err.println("Unable upload file " + file);
            e.printStackTrace();
        }
        System.out.println("\nUpload completed.");
    }





    public void sendImageMessage(File file) {
        String username = UserNameInput.getUserName();
        try {
            FileInputStream in = new FileInputStream(file);
            byte[] buffer = new byte[1024];

            out.writeInt(5); // type

            out.writeInt(username.length());
            out.write(username.getBytes(), 0, username.length());

            out.writeInt(file.getName().length());
            out.write(file.getName().getBytes());

            long size = file.length();
            out.writeLong(size);

            System.out.printf("Uploading image %s (%d)", file.getName(), size);
            while(size > 0) {
                int len = in.read(buffer, 0, buffer.length);
                out.write(buffer, 0, len);
                size -= len;
                System.out.print(".");
            }
            System.out.println("\nUpload completed.");
        } catch (Exception e) {
            System.err.println("Unable upload file " + file);
            e.printStackTrace();
        }

    }







    private void listenToExternalServer() {
        System.out.println("Listening to external server:");
        Thread t = new Thread(() -> {
            System.out.println(externalSocket.getInetAddress().getHostName());
            while (true) {
                try {
                    int code = in.readInt();
                    switch (code) {
                        case 0:
                            int usernameLength = in.readInt();
                            byte[] b = new byte[usernameLength];
                            in.read(b, 0, usernameLength);
                            String incomingUsername = new String(b, 0, usernameLength);
                            int messageLength = in.readInt();
                            b = new byte[messageLength];
                            in.read(b, 0, messageLength);
                            String incomingMessage = new String(b, 0, messageLength);
                            ChatArea.getInstance().addMessage(incomingMessage, incomingUsername);
                            break;
                        case 1:
                            int pixelColor = in.readInt();
                            int pixelX = in.readInt();
                            int pixelY = in.readInt();
                            int penType = in.readInt();
                            int penSize = in.readInt();
                            UI.getInstance().paintPixel(pixelColor, pixelX, pixelY,penType,penSize);
                            break;
                        case 2:
                            int bucketColor = in.readInt();
                            int bucketX = in.readInt();
                            int bucketY = in.readInt();
                            UI.getInstance().paintArea(bucketColor, bucketX, bucketY);
                            break;
                        case 3:
                            File file = readCompleteFileFromServer();
                            int[][][] tempData = new int [1][1][1];
                            int[] tempBlockSize = new int [1];
                            boolean[] success = new boolean[1];
                            UI.loadDrawingFromFileIntoDataArray(file, tempData, tempBlockSize, success);
                            if (success[0]){
                                 UI.getInstance().loadDrawingLocally(tempData[0], tempBlockSize[0]);
                            }
                            break;
                        case 4:
                            int usernameLength1File = in.readInt();
                            byte[] bb = new byte[usernameLength1File];
                            in.read(bb, 0, usernameLength1File);
                            String incomingUsername1 = new String(bb, 0, usernameLength1File);

                            byte[] buffer = new byte[1024];
                            int fileNameSize = in.readInt();

                            in.read(buffer,0,fileNameSize);
                            String fileName= new String(buffer,0,fileNameSize);
                            while (new File(fileName).exists()){
                                fileName = "_" + fileName;
                            }
                            System.out.println("Downloading File from server "+ fileName);
                            File outputFile = new File(fileName);
                            FileOutputStream outFile = new FileOutputStream(outputFile);

                            long fileSize = in.readLong();



                            while (fileSize>0)
                             {
                                int len=in.read(buffer,0, buffer.length);
                                outFile.write(buffer,0,len);
                                fileSize=fileSize-len;
                            }
                            outFile.flush();
                            outFile.close();

                            ChatArea.getInstance().addFileMessage(outputFile, incomingUsername1);

                            break;
                        case 5:
                            int usernameLength1Image = in.readInt();
                            byte[] bbb = new byte[usernameLength1Image];
                            in.read(bbb, 0, usernameLength1Image);
                            String incomingUsername2 = new String(bbb, 0, usernameLength1Image);

                            byte[] buffers = new byte[1024];
                            int imageNameSize = in.readInt();

                            in.read(buffers,0,imageNameSize);
                            String imageName= new String(buffers,0,imageNameSize);
                            while (new File(imageName).exists()){
                                imageName = "_" + imageName;
                            }
                            System.out.println("Downloading Image");
                            File outputImage = new File(imageName);
                            FileOutputStream outImage = new FileOutputStream(outputImage);

                            long imageSize = in.readLong();


                            while (imageSize>0)
                            {
                                int len=in.read(buffers,0, buffers.length);
                                outImage.write(buffers,0,len);
                                imageSize=imageSize-len;
                            }
                            outImage.flush();
                            outImage.close();
                            System.out.println("Download complete");

                            ChatArea.getInstance().addImageMessage(outputImage, incomingUsername2);

                    }

                } catch (SocketException e) {
//                    Server is dead
                    //initiate save and find new connection protocol.
                    System.out.println("Server down");
                    KidPaint.restart();
                    return;
                } catch (IOException e){
                    e.printStackTrace();
                }
            }

        });
        t.start();
    }

    //TODO

    /**
     * This method reads an entire file from the server's datainputstream
     * YOu can decide how to implement it
     * Make sure your implementation works with the sender in ImternalServer.java
     *
     * The sender of the file you wanna read is {{{ handleLoadFile in InternalServer.java }}}
     * @return
     */
    private File readCompleteFileFromServer() {
        try {
            int usernameLength1File = in.readInt();
            byte[] bb = new byte[usernameLength1File];
            in.read(bb, 0, usernameLength1File);
            String incomingUsername1 = new String(bb, 0, usernameLength1File);

            byte[] buffer = new byte[1024];
            int fileNameSize = in.readInt();

            in.read(buffer, 0, fileNameSize);
            String fileName = new String(buffer, 0, fileNameSize);
            while (new File(fileName).exists()){
                fileName = "_" + fileName;
            }
            System.out.println("Downloading File from server " + fileName);
            File outputFile = new File(fileName);
            FileOutputStream outFile = new FileOutputStream(outputFile);

            long fileSize = in.readLong();


            while (fileSize > 0) {
                int len = in.read(buffer, 0, buffer.length);
                outFile.write(buffer, 0, len);
                fileSize = fileSize - len;
            }
            outFile.flush();
            outFile.close();
            System.out.println("File read complete");
            return outputFile;
        }
        catch (Exception e){
            System.out.println("readCompleteFileFromServer()");
            return null;
        }

    }


    /**
     * This one sends a loadfile to the server
     * The reciepient of this is {{{  handleLoadFile in InternalServer.java  }}} message code = 3
     * @param loadFile
     */
    public void sendLoadFileToServer(File loadFile){

        //TODO
        //send file to server with code 3
        //send your username
        //send file
        File file=loadFile;
        String username = UserNameInput.getUserName();
        try {
            FileInputStream in = new FileInputStream(file);
            byte[] buffer = new byte[1024];

            out.writeInt(3); // type

            out.writeInt(username.length());
            out.write(username.getBytes(), 0, username.length());

            out.writeInt(file.getName().length());
            out.write(file.getName().getBytes());

            long size = file.length();
            out.writeLong(size);

            System.out.printf("Uploading file %s (%d)", file.getName(), size);
            while(size > 0) {
                int len = in.read(buffer, 0, buffer.length);
                out.write(buffer, 0, len);
                size -= len;
                System.out.print(".");
            }

        } catch (Exception e) {
            System.err.println("Unable upload file " + file);
            e.printStackTrace();
        }
        System.out.println("\nUpload completed.");


    }


}


