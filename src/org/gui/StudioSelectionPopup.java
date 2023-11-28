package org.gui;

import org.KidPaint;
import org.server.ExternalConnectedServer;
import org.server.InternalServer;
import org.server.ServerFinder;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.net.BindException;
import java.util.ArrayList;
import java.util.List;

public class StudioSelectionPopup extends JDialog {
    private static StudioSelectionPopup instance = null;
    private final JFrame parent;
    private final JPanel serverListPanel;

    private boolean isActive = true;


    public static StudioSelectionPopup getInstance() {
        if (instance == null)
            instance = new StudioSelectionPopup(UI.getInstance());
        return instance;
    }

    public static void kill(){
        ServerFinder.kill();
        instance = null;
    }


    private StudioSelectionPopup(JFrame parent) {
        super(parent, true);
        this.parent = parent;

        ServerFinder.getInstance();

        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        serverListPanel = new JPanel();
        serverListPanel.setLayout(new BoxLayout(serverListPanel, BoxLayout.Y_AXIS));
        serverListPanel.setBorder(new EmptyBorder(15, 15, 5, 15));

        System.out.println("getting servers");
        for (String[] server : ServerFinder.getServers()) {
            String studioName = server[0]+ "'s studio";
            JButton button = new JButton(studioName);
            button.setAlignmentX(Component.CENTER_ALIGNMENT);
            button.setMaximumSize(new Dimension(Integer.MAX_VALUE, button.getPreferredSize().height));

            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    isActive = false;
                    dispose();
                    joinServer(server);
                }
            });
            serverListPanel.add(button);
        }

        JScrollPane scrollPane = new JScrollPane(serverListPanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        JPanel buttomBar = new JPanel(new BorderLayout(0, 3));

        JButton inviteCodeButton = new JButton("Use Invite Code");
        inviteCodeButton.setPreferredSize(new Dimension(130, 20));
        inviteCodeButton.addActionListener((ActionEvent e) -> {
            processInviteCode();
        });

        JPanel inviteCodeButtonContainer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        inviteCodeButtonContainer.add(inviteCodeButton);

        JButton startServerButton = new JButton("Start your own studio");
        startServerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                isActive = false;
                dispose();
                startServer();
            }
        });
        startServerButton.setBorder(new EmptyBorder(20, 5, 20, 5));

        buttomBar.add(inviteCodeButtonContainer, BorderLayout.NORTH);

        buttomBar.add(startServerButton, BorderLayout.CENTER);


        JPanel container = new JPanel(new BorderLayout());
        JLabel label = new JLabel("Choose a studio");
        label.setFont(label.getFont().deriveFont(Font.BOLD, 15.0F));
        label.setHorizontalAlignment(SwingConstants.CENTER);
        add(label, BorderLayout.NORTH);
        container.add(scrollPane, BorderLayout.CENTER);
        container.add(buttomBar, BorderLayout.SOUTH);
        container.setPreferredSize(new Dimension(300, 300));
        getContentPane().add(container);
        pack();
        setLocationRelativeTo(parent);

    }
    private void joinServer(String[] server) {
        //create new external server with correct credentials
        ExternalConnectedServer.getInstance().initialiseServer(server);
    }

    private void startServer() {
        ChatArea.getInstance().addBannerAlert("You've started your own studio!");
        InternalServer.getInstance();
        UI.getInstance().internalServerSetup();
    }

    private void processInviteCode() {

        JDialog dialog = new JDialog(this, true);

        dialog.setSize(300, 150);
        dialog.setLocationRelativeTo(parent);
        dialog.setLayout(new BorderLayout());

        JLabel label = new JLabel("Enter a valid invitation code");
        label.setFont(label.getFont().deriveFont(Font.BOLD, 15.0F));
        label.setHorizontalAlignment(SwingConstants.CENTER);
        dialog.add(label, BorderLayout.NORTH);

        JTextField inputField = new JTextField();
        inputField.setBorder(new EmptyBorder(0, 10, 0, 10));
        inputField.setHorizontalAlignment(SwingConstants.CENTER);
        dialog.add(inputField, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new BorderLayout());
        JLabel errorMsg = new JLabel("");
        errorMsg.setForeground(Color.RED);
        errorMsg.setHorizontalAlignment(SwingConstants.CENTER);

        JButton okButton = new JButton("Use Code");
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String[] decodedCode = KidPaint.decode(inputField.getText());
                    isActive = false;
                    dialog.dispose();
                    dispose();
                    joinServer(new String[]{"Remote User", decodedCode[0], decodedCode[1]});
            }
        });

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.dispose();
            }
        });



        buttonPanel.add(errorMsg, BorderLayout.NORTH);

        buttonPanel.add(okButton, BorderLayout.WEST);
        buttonPanel.add(cancelButton, BorderLayout.EAST);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.show();


    }

    private String[] useInviteCode(JDialog dialog, String code) {
        String decodedIp;
        String decodedPort = "12000";
        try {
            decodedIp = StudioSelectionPopup.decodeInvitationCode(code);
        } catch (Exception e) {
            return new String[0];
        }
        return new String[]{"Remote User", decodedIp, decodedPort};
    }

    public static String decodeInvitationCode(String encodedIp) throws Exception {

        if (encodedIp.length() < 8) throw new Exception("Invalid code");
        String decode = "";
        // Remove "kpt::" prefix
        for (int i = encodedIp.length() - 8; i < encodedIp.length(); i++)
            decode += encodedIp.charAt(i);


        StringBuilder ipString = new StringBuilder();

        for (int i = 0; i < decode.length(); i += 2) {
            String hexByte = decode.substring(i, i + 2);
            int d = Integer.parseInt(hexByte, 16);
            ipString.append(d);

            if (i < decode.length() - 2) {
                ipString.append(".");
            }
        }
        return ipString.toString();

    }


}