package org;


import org.gui.ChatArea;
import org.gui.StudioSelectionPopup;
import org.gui.UI;
import org.gui.UserNameInput;
import org.server.ExternalConnectedServer;
import org.server.InternalServer;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.*;
import java.util.Base64;
import java.util.concurrent.ThreadLocalRandom;

public class KidPaint {
    static int ROWS = 50;
    static int COLUMNS = 50;
    static int BLOCK_SIZE = 16;
    static int WIDTH = 1330;
    static int HEIGHT = 900;

    public static void main(String[] args) {
        UI ui = UI.getInstance();            // get the instance of UI


        ui.setData(new int[ROWS][COLUMNS], BLOCK_SIZE);    // set the data array and block size. comment this statement to use the default data array and block size.
        ui.setSize(new Dimension(WIDTH, HEIGHT));
        ui.setVisible(true);                // set the ui

        UserNameInput userNameInput = UserNameInput.getInstance();
        StudioSelectionPopup studioSelectionPopup = StudioSelectionPopup.getInstance();

        userNameInput.setVisibleAfterClose(studioSelectionPopup);
        userNameInput.setVisible(true);  // launch the username dialog box

    }

    public static void restart() {
        //Kill everybody worth killing
        StudioSelectionPopup.kill(); //kill ssp and sf

        if (InternalServer.isRunning()){
            InternalServer.getInstance().kill();
        }
        if (ExternalConnectedServer.getInstance() != null){
            ExternalConnectedServer.getInstance().kill();
        }

        // ask to save
        int result = JOptionPane.showConfirmDialog(UI.getInstance(), "Do you want to save drawing?", "Studio Offline",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);


        if (result == JOptionPane.YES_OPTION) {
            JFileChooser fileChooser = new JFileChooser();
            FileNameExtensionFilter fileFiler = new FileNameExtensionFilter(
                    "KidPaint Files", "kpt");
            fileChooser.setFileFilter(fileFiler);
            int option = fileChooser.showSaveDialog(UI.getInstance());
            if (option == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                if (!selectedFile.getAbsolutePath().contains(".")) {
                    selectedFile = new File(selectedFile.getAbsolutePath() + ".kpt");
                }
                UI.getInstance().saveDrawingToFile(selectedFile);
            }
        }




        //rebuild UI

        UI ui = UI.getInstance();
        ui.setData(new int[ROWS][COLUMNS], BLOCK_SIZE);
        ui.setSize(new Dimension(WIDTH, HEIGHT));
        ChatArea.clean();
        ui.revalidate();
        ui.repaint();



        StudioSelectionPopup studioSelectionPopup = StudioSelectionPopup.getInstance();
        studioSelectionPopup.setVisible(true);





    }

    public static void serverNotFound() {
        JOptionPane.showMessageDialog(
                null,
                "Studio is no longer online!",
                "Error",
                JOptionPane.WARNING_MESSAGE
        );
        //Kill everybody worth killing
        StudioSelectionPopup.kill(); //kill ssp and sf

        if (InternalServer.isRunning()){
            InternalServer.getInstance().kill();
        }
        if (ExternalConnectedServer.getInstance() != null){
            ExternalConnectedServer.getInstance().kill();
        }

        System.out.println("killed everything");



        //rebuild UI

        UI ui = UI.getInstance();
        ui.setData(new int[ROWS][COLUMNS], BLOCK_SIZE);
        ui.setSize(new Dimension(WIDTH, HEIGHT));
        ChatArea.clean();
        ui.revalidate();
        ui.repaint();



        StudioSelectionPopup studioSelectionPopup = StudioSelectionPopup.getInstance();
        studioSelectionPopup.setVisible(true);





    }

    public static int getFreeUDPPort(){
        System.out.println("getting free UDP ports");
        int min = 15000;
        int max = 16000;

        for (int port = min; port <= max; port++){
            System.out.println("Checking UDP port " + port);
            try (DatagramSocket testSocket = new DatagramSocket(port)){
                System.out.println("Found free UDP port " + port);
                return port;
            } catch (ConnectException e){
            } catch (IOException e){
            }
        }
        System.out.println("No free port found");
        System.exit(1);
        return 0;
    }

    public static int getFreeTCPPort(){
        System.out.println("getting free TCP ports");
        int min = 11000;
        int max = 52000;

        for (int i = 0; i < 5000; i++){
            int port = ThreadLocalRandom.current().nextInt(min, max);
            System.out.println("Checking TCP port " + port);
            try (ServerSocket testSocket = new ServerSocket(port)){
                System.out.println("Found free TCP port " + port);
                return port;
            } catch (ConnectException e){
            } catch (IOException e){
            }
        }
        System.out.println("No free port found");
        System.exit(1);
        return 0;
    }

    public static String encode(String IP, String port) {
        // Concatenate IP and port with a separator
        String data = IP + ":" + port;

        // Encode the concatenated string using Base64
        String encodedData = Base64.getEncoder().encodeToString(data.getBytes());

        // Add the custom prefix "kpt://"
        return "kpt://" + encodedData;
    }

    // 2nd function: Decode the encoded string
    public static String[] decode(String encodedString) {
        // Remove the custom prefix "kpt://"
        String encodedData = encodedString.replace("kpt://", "");

        // Decode the Base64-encoded string
        byte[] decodedBytes = Base64.getDecoder().decode(encodedData);

        // Convert the decoded bytes back to a string
        String decodedString = new String(decodedBytes);

        // Split the decoded string into IP and port using the separator ":"
        String[] decodedArray = decodedString.split(":", 2);

        // Ensure that both IP and port are present
        if (decodedArray.length == 2) {
            return decodedArray;
        } else {
            // Handle invalid input
            throw new IllegalArgumentException("Invalid encoded string");
        }
    }
}
