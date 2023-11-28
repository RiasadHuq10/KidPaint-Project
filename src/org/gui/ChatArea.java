package org.gui;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Date;

public class ChatArea extends JPanel {
    private static ChatArea instance;
    private int lastMessagePosition = 0;

    private JScrollBar scrollBar = null;

    public static ChatArea getInstance() {
        if (instance == null) {
            instance = new ChatArea();
        }
        return instance;
    }

    public static void clean(){
        ChatArea.getInstance().removeAll();
        ChatArea.getInstance().lastMessagePosition = 0;
    }

    public void setScrollBar(JScrollBar sb){
        scrollBar = sb;
    }

    private void scrollToButton(){
        if (scrollBar == null) return;
        scrollBar.setValue(scrollBar.getMaximum());
    }

    private ChatArea() {
        setLayout(null);
    }

    public void addMessage(String content, String sender) {

        ChatMessage message = new ChatMessage(content, sender);
        message.setBounds(message.getChatXPosition(), lastMessagePosition + 20, message.getChatWidth(), message.getHeight());
        lastMessagePosition = lastMessagePosition + 20 + message.getHeight();
        setPreferredSize(new Dimension(0, lastMessagePosition));
        SwingUtilities.invokeLater(() -> {
            super.add(message);
            SwingUtilities.invokeLater(this::scrollToButton);
            revalidate();
            repaint();
        });


    }

    public void addFileMessage(File outputFile, String sender) {
        ChatFile message = new ChatFile(outputFile, sender);
        message.setBounds(message.getChatXPosition(), lastMessagePosition + 20, message.getChatWidth(), message.getHeight());
        lastMessagePosition = lastMessagePosition + 20 + message.getHeight();
        setPreferredSize(new Dimension(0, lastMessagePosition));
        SwingUtilities.invokeLater(() -> {
            super.add(message);
            SwingUtilities.invokeLater(this::scrollToButton);
            revalidate();
            repaint();
        });
    }

    public void addImageMessage(File outputFile, String sender) {

        ChatImage message = new ChatImage(outputFile, sender);
        message.setBounds(message.getChatXPosition(), lastMessagePosition + 20, message.getChatWidth(), message.getHeight());
        lastMessagePosition = lastMessagePosition + 20 + message.getHeight();
        setPreferredSize(new Dimension(0, lastMessagePosition));
        SwingUtilities.invokeLater(() -> {
            super.add(message);
            SwingUtilities.invokeLater(this::scrollToButton);
            revalidate();
            repaint();
        });

    }

    public void addBannerAlert(String content){
        BannerAlert alert = new BannerAlert(content);
        alert.setBounds(0, lastMessagePosition + 20, 300, alert.getHeight());
        lastMessagePosition = lastMessagePosition + 20 + alert.getHeight();
        SwingUtilities.invokeLater(() -> {
            super.add(alert);
            SwingUtilities.invokeLater(this::scrollToButton);
            revalidate();
            repaint();
        });

    }

    static class ChatItem extends JPanel{
        private final String sender;
        private final boolean isMyMessage;
        private final Color messageColor;
        private final Color textColor;

        private final Date timestamp;

        private final JTextField header;

        private final JTextField footer;

        private final JPanel panel;


        private final int xPosition;
        private final int width = 250;;

        protected int getChatXPosition(){
            return this.xPosition;
        }

        protected int getChatWidth(){
            return this.width;
        }


        protected ChatItem(String sender){
            this.sender = sender;
            this.isMyMessage = UserNameInput.getUserName().equals(sender);
            this.messageColor = this.isMyMessage ? Color.blue : Color.LIGHT_GRAY;
            this.textColor = this.isMyMessage ? Color.white : Color.black;
            this.xPosition = isMyMessage ? 30 : 5;
            this.timestamp = new Date();


            this.setLayout(new BorderLayout());

            panel = new JPanel(new BorderLayout());

            header = new JTextField(this.sender);
            header.setBackground(messageColor);
            header.setForeground(textColor);
            header.setFont(header.getFont().deriveFont(Font.BOLD, 15.0F));
            header.setEditable(false);
            header.setBorder(new EmptyBorder(0, 0, 0, 0));

            footer = new JTextField(this.timestamp.toString());
            footer.setBackground(messageColor);
            footer.setForeground(textColor);
            footer.setHorizontalAlignment(SwingConstants.RIGHT);
            footer.setFont(footer.getFont().deriveFont(Font.ITALIC, 9.0F));
            footer.setEditable(false);
            footer.setBorder(new EmptyBorder(0, 0, 0, 0));


            panel.add(header, BorderLayout.NORTH);
            panel.add(footer, BorderLayout.SOUTH);
            panel.setBorder(new EmptyBorder(10, 5, 2, 5));
            panel.setBackground(messageColor);



            if (isMyMessage)
                add(panel, BorderLayout.EAST);
            else
                add(panel, BorderLayout.WEST);
            add(new JPanel(), BorderLayout.CENTER);

        }

    }


    static class ChatMessage extends ChatItem {
        private final String content;


        /**
         * Message constructor.
         *
         * @param content -- message content
         * @param sender  -- username of the message sender.
         */
        public ChatMessage(String content, String sender) {
            super(sender);
            this.content = content;


            JTextArea inner = new JTextArea(this.content, 0, 10);
            inner.setSize(new Dimension(200, inner.getPreferredSize().height));
            inner.setEditable(false);
            inner.setLineWrap(true);
            inner.setWrapStyleWord(true);
            inner.setBackground(super.messageColor);
            inner.setForeground(super.textColor);
            inner.setFont(inner.getFont().deriveFont(Font.PLAIN, 12.0F));
            inner.setBorder(new EmptyBorder(0, 0, 8, 0));


            super.panel.add(inner, BorderLayout.CENTER);

            setSize(new Dimension(200, super.panel.getPreferredSize().height));
        }


    }

    static class ChatFile extends ChatItem {
        private final File file;


        public ChatFile(File file, String sender) {
            super(sender);
            this.file = file;

            JPanel inner = new JPanel(new FlowLayout());
            JLabel label = new JLabel(file.getName());
            label.setPreferredSize(new Dimension(150, 30));
            inner.add(label);
            JButton saveButton = new JButton("Save");
            saveButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    downloadFile();
                }
            });
            inner.add(saveButton);


            super.header.setBorder(new EmptyBorder(0, 0, 10, 0));
            super.footer.setBorder(new EmptyBorder(10, 0, 0, 0));

            super.panel.add(inner, BorderLayout.CENTER);

            setSize(new Dimension(250, super.panel.getPreferredSize().height));


        }

        private void downloadFile() {
            try {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setName("Save File");
                int option = fileChooser.showSaveDialog(ChatArea.getInstance());
                if (option == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    if (!selectedFile.getAbsolutePath().contains(".")){
                        selectedFile = new File(selectedFile.getAbsolutePath() + "." + this.file.getName().split("\\.")[1]);
                    }

                    Files.copy(this.file.toPath(), selectedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    JOptionPane.showMessageDialog(ChatArea.getInstance(),
                            "File downloaded to " + selectedFile.getCanonicalPath());
                } else {
                    return;
                }
            } catch (IOException e) {
                System.out.println("Download file failed");
            }
        }
    }

    static class ChatImage extends ChatItem {
        private final File file;


        public ChatImage(File file, String sender) {
            super(sender);
            this.file = file;


            JLabel inner = fetchImageFromFile(file);

            JPanel footer = new JPanel(new GridLayout(0, 1));

            if (!super.isMyMessage) {
                JPanel saveButtonHolder = new JPanel(new FlowLayout(FlowLayout.LEFT));
                saveButtonHolder.setBackground(super.messageColor);
                JButton saveButton = new JButton("Save");
                saveButtonHolder.add(saveButton);
                saveButton.setPreferredSize(new Dimension(80, 30));
                saveButton.setHorizontalAlignment(SwingConstants.CENTER);
                saveButton.setFont(saveButton.getFont().deriveFont(Font.BOLD, 10.0F));
                saveButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        downloadImage();
                    }
                });
                footer.add(saveButtonHolder);
            }


            JTextField timestamp = new JTextField(super.timestamp.toString());
            timestamp.setBackground(super.messageColor);
            timestamp.setForeground(super.textColor);
            timestamp.setHorizontalAlignment(SwingConstants.RIGHT);
            timestamp.setFont(footer.getFont().deriveFont(Font.ITALIC, 9.0F));
            timestamp.setEditable(false);
            footer.add(timestamp);

            footer.setBackground(super.messageColor);
            footer.setForeground(super.textColor);
            footer.setBorder(new EmptyBorder(10, 0, 0, 0));

            super.panel.add(inner, BorderLayout.CENTER);
            super.panel.add(footer, BorderLayout.SOUTH);

            setSize(new Dimension(250, super.panel.getPreferredSize().height));
        }


        private JLabel fetchImageFromFile(File imageFile) {
            try {
                // Read the image file into a BufferedImage
                BufferedImage originalImage = ImageIO.read(imageFile);

                // Calculate the desired width and heisght based on the available space
                int maxWidth = 230;
                int maxHeight = 800;
                int originalWidth = originalImage.getWidth();
                int originalHeight = originalImage.getHeight();
                double widthRatio = (double) maxWidth / originalWidth;
                double heightRatio = (double) maxHeight / originalHeight;
                double scaleFactor = Math.min(widthRatio, heightRatio);
                int newWidth = (int) (originalWidth * scaleFactor);
                int newHeight = (int) (originalHeight * scaleFactor);

                Image scaledImage = originalImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);

                ImageIcon imageIcon = new ImageIcon(scaledImage);

                JLabel imageLabel = new JLabel(imageIcon);
                return imageLabel;


            } catch (IOException e) {
                e.printStackTrace();
                JLabel errorMsg = new JLabel("**Image failed to load**");
                errorMsg.setFont(errorMsg.getFont().deriveFont(Font.ITALIC, 12.0F));
                return errorMsg;
            }
        }

        private void downloadImage() {
            try {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setName("Save Image");
                FileNameExtensionFilter imageFilter = new FileNameExtensionFilter(
                        "Image Files", "jpg", "jpeg", "png", "gif", "bmp", "webp");
                fileChooser.setFileFilter(imageFilter);
                int option = fileChooser.showSaveDialog(ChatArea.getInstance());
                if (option == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    if (!selectedFile.getAbsolutePath().contains(".")){
                        selectedFile = new File(selectedFile.getAbsolutePath() + ".jpeg");
                    }

                    Files.copy(this.file.toPath(), selectedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    JOptionPane.showMessageDialog(ChatArea.getInstance(),
                            "Image downloaded to " + selectedFile.getCanonicalPath());
                } else {
                    return;
                }
            } catch (IOException e) {
                System.out.println("Download image failed");
            }
        }


    }

    static class BannerAlert extends JPanel{


        public BannerAlert(String content) {
            JLabel label = new JLabel(content);
            add(label);
            setBackground(Color.yellow);
            setBorder(new EmptyBorder(10, 0,10,0));
            setSize(new Dimension(300, getPreferredSize().height));
        }
    }
}



