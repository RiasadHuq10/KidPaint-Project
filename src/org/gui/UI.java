package org.gui;

import org.server.ExternalConnectedServer;
import org.server.InternalServer;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;
import java.util.Scanner;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;

enum PaintMode {Pixel, Area, Shape};

public class UI extends JFrame {
    private JTextField msgField;
    private JPanel toolPanel;
    private ChatArea chatArea;
    private JPanel penSizeSlider;
    private JPanel pnlColorPicker;
    private JPanel paintPanel;
    private JToggleButton tglPen;
    private JToggleButton tglBucket;
    JToggleButton tglShape;
    private JPanel shapeSelector;
    private JButton loadButton;
    private JButton saveButton;
    private JPanel sendTextArea;
    private JLabel sendTextPrompt;
    protected String username = " .. ";
    private static UI instance;
    private int selectedColor = -543230;    //golden

    private int penSize = 1;
    private int shapeType = 0; //0 is circle, 1 square, 2 star //TODO SHAPES ARE COMPLETELY SEPARATE FROM PEN: on mouse click, if paintMode is Shape, draw only one instance of shape using shape size
    private int shapeSize = 3; //min 3 max 30

    int[][] data = new int[50][50];            // pixel color data array
    int blockSize = 3;
    PaintMode paintMode = PaintMode.Pixel;

    /**
     * get the instance of UI. Singleton design pattern.
     *
     * @return
     */
    public static UI getInstance() {
        if (instance == null)
            instance = new UI();

        return instance;
    }

    public static UI restart(){
        instance = new UI();
        return instance;
    }

    /**
     * private constructor. To create an instance of UI, call UI.getInstance() instead.
     */
    private UI() {
        setTitle("KidPaint");

        JPanel basePanel = new JPanel();
        getContentPane().add(basePanel, BorderLayout.CENTER);
        basePanel.setLayout(new BorderLayout(0, 0));

        paintPanel = new JPanel() {

            // refresh the paint panel
            @Override
            public void paint(Graphics g) {
                super.paint(g);

                Graphics2D g2 = (Graphics2D) g; // Graphics2D provides the setRenderingHints method

                // enable anti-aliasing
                RenderingHints rh = new RenderingHints(
                        RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHints(rh);

                // clear the paint panel using black
                g2.setColor(Color.black);
                g2.fillRect(0, 0, this.getWidth(), this.getHeight());

                // draw and fill circles with the specific colors stored in the data array
                for (int x = 0; x < data.length; x++) {
                    for (int y = 0; y < data[0].length; y++) {
                        g2.setColor(new Color(data[x][y]));
                        g2.fillArc(blockSize * x, blockSize * y, blockSize, blockSize, 0, 360);
                        g2.setColor(Color.darkGray);
                        g2.drawArc(blockSize * x, blockSize * y, blockSize, blockSize, 0, 360);
                    }
                }
            }
        };

        paintPanel.addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    changeAllColorToSelectedColor(e.getX() / blockSize, e.getY() / blockSize, selectedColor);
                }
                if (e.getButton() == MouseEvent.BUTTON1 && paintMode == PaintMode.Shape) {
                	if (InternalServer.isRunning()) {
                        paintPixel(e.getX() / blockSize, e.getY() / blockSize, shapeType, 4);
                        InternalServer.getInstance().sendMyPixelMessage(selectedColor, e.getX() / blockSize, e.getY() / blockSize, shapeType, 4);
                    } else {
                        ExternalConnectedServer.getInstance().sendPixelMessage(selectedColor, e.getX() / blockSize, e.getY() / blockSize, shapeType, 4);
                    }
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }

            @Override
            public void mousePressed(MouseEvent e) {
            }

            // handle the mouse-up event of the paint panel
            @Override
            public void mouseReleased(MouseEvent e) {
                if (paintMode == PaintMode.Area && e.getX() >= 0 && e.getY() >= 0)
                    if (InternalServer.isRunning()) {
                        paintArea(e.getX() / blockSize, e.getY() / blockSize);
                        InternalServer.getInstance().sendMyBucketMessage(selectedColor, e.getX() / blockSize, e.getY() / blockSize);
                    } else {
                        ExternalConnectedServer.getInstance().sendBucketMessage(e.getX() / blockSize, e.getY() / blockSize, selectedColor);
                    }
            }
        });

        paintPanel.addMouseMotionListener(new MouseMotionListener() {

            @Override
            public void mouseDragged(MouseEvent e) {
                if (paintMode == PaintMode.Pixel && e.getX() >= 0 && e.getY() >= 0)
                    if (InternalServer.isRunning()) {
                        paintPixel(e.getX() / blockSize, e.getY() / blockSize, 0, penSize);
                        InternalServer.getInstance().sendMyPixelMessage(selectedColor, e.getX() / blockSize, e.getY() / blockSize, 0, penSize);
                    } else {
                        ExternalConnectedServer.getInstance().sendPixelMessage(selectedColor, e.getX() / blockSize, e.getY() / blockSize, 0, penSize);
                    }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
            }

        });


        paintPanel.setPreferredSize(new Dimension(data.length * blockSize, data[0].length * blockSize));

        JScrollPane scrollPaneLeft = new JScrollPane(paintPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        basePanel.add(scrollPaneLeft, BorderLayout.CENTER);

        //TOOLPANEL STARTED

        toolPanel = new JPanel();
        basePanel.add(toolPanel, BorderLayout.NORTH);
        toolPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));


        pnlColorPicker = new JPanel();
        pnlColorPicker.setPreferredSize(new Dimension(24, 24));
        pnlColorPicker.setBackground(new Color(selectedColor));
        pnlColorPicker.setBorder(new LineBorder(new Color(0, 0, 0)));

        // show the color picker
        pnlColorPicker.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }

            @Override
            public void mousePressed(MouseEvent e) {
            }


            @Override
            public void mouseReleased(MouseEvent e) {
                ColorPicker picker = ColorPicker.getInstance(UI.instance);
                Point location = pnlColorPicker.getLocationOnScreen();
                location.y += pnlColorPicker.getHeight();
                picker.setLocation(location);
                picker.setVisible(true);
            }

        });

        toolPanel.add(pnlColorPicker);

        //Pen

        tglPen = new JToggleButton("Pen");
        tglPen.setSelected(true);
        toolPanel.add(tglPen);
        //Pen Size Slider
        penSizeSlider = new JPanel(new FlowLayout());
        penSizeSlider.add(new JLabel("Pen Size"));
        JSlider penSizeSliderTrack = new JSlider(1, 10, 1);
        penSizeSliderTrack.setPaintTicks(true);
        penSizeSliderTrack.setMajorTickSpacing(1);
        penSizeSliderTrack.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                penSize = penSizeSliderTrack.getValue();
            }
        });
        penSizeSlider.add(penSizeSliderTrack);
        toolPanel.add(penSizeSlider);


        //Bucket

        tglBucket = new JToggleButton("Bucket");
        toolPanel.add(tglBucket);


        //Shape

        tglShape = new JToggleButton("Shape");
        toolPanel.add(tglShape);
        //ShapeSelector
        shapeSelector = new JPanel(new FlowLayout());
        JComboBox<String> shapeDropDown = new JComboBox<>(new String[]{"Circle", "Square", "Star"});
        shapeDropDown.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                shapeType = shapeDropDown.getSelectedIndex();
            }
        });
        shapeDropDown.setSelectedIndex(0);
        shapeSelector.add(shapeDropDown);
        shapeSelector.setVisible(false);
        toolPanel.add(shapeSelector);

        // change the paint mode to PIXEL mode
        tglPen.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                tglPen.setSelected(true);
                penSizeSlider.setVisible(true);
                tglShape.setSelected(false);
                shapeSelector.setVisible(false);
                tglBucket.setSelected(false);
                paintMode = PaintMode.Pixel;
            }
        });

        tglShape.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                tglPen.setSelected(false);
                penSizeSlider.setVisible(false);
                tglShape.setSelected(true);
                shapeSelector.setVisible(true);
                tglBucket.setSelected(false);
                paintMode = PaintMode.Shape;
            }
        });

        // change the paint mode to AREA mode
        tglBucket.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                tglPen.setSelected(false);
                penSizeSlider.setVisible(false);
                tglShape.setSelected(false);
                shapeSelector.setVisible(false);
                tglBucket.setSelected(true);
                paintMode = PaintMode.Area;
            }
        });


        loadButton = new JButton("Load File");
        loadButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                FileNameExtensionFilter fileFiler = new FileNameExtensionFilter(
                        "KidPaint Files", "kpt");
                fileChooser.setFileFilter(fileFiler);
                int option = fileChooser.showOpenDialog(UI.this);
                if (option == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    if (!selectedFile.getAbsolutePath().contains(".")) {
                        selectedFile = new File(selectedFile.getAbsolutePath() + ".kpt");
                    }
                    if (InternalServer.isRunning()) {
                        try {
                            InternalServer.getInstance().loadMyFile(selectedFile);
                        } catch (FileNotFoundException ex) {
                            //This exception will probably never occur because the file is gotten from JFileChooser which is safe.
                            throw new RuntimeException(ex);
                        }

                    } else {
                        ExternalConnectedServer.getInstance().sendLoadFileToServer(selectedFile);
                    }
                    JOptionPane.showMessageDialog(UI.this,
                            "Loading file: " + selectedFile.getAbsolutePath());
                } else {
                    JOptionPane.showMessageDialog(UI.this,
                            "Load operation canceled");
                }
            }
        });
        toolPanel.add(loadButton);

        saveButton = new JButton("Save to file");
        saveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                FileNameExtensionFilter fileFiler = new FileNameExtensionFilter(
                        "KidPaint Files", "kpt");
                fileChooser.setFileFilter(fileFiler);
                int option = fileChooser.showSaveDialog(UI.this);
                if (option == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    if (!selectedFile.getAbsolutePath().contains(".")) {
                        selectedFile = new File(selectedFile.getAbsolutePath() + ".kpt");
                    }
                    saveDrawingToFile(selectedFile);
                } else {
                    JOptionPane.showMessageDialog(UI.this,
                            "Save command canceled");
                }
            }
        });

        toolPanel.add(saveButton);






        //TOOLPANEL FINISHED

        //MSGPANEL STARTED


        JPanel msgPanel = new JPanel();

        getContentPane().add(msgPanel, BorderLayout.EAST);

        msgPanel.setLayout(new BorderLayout(0, 0));

//        sendTextArea = new JPanel();
//        sendTextArea.setLayout(new BorderLayout(0, 0));
//        sendTextPrompt = new JLabel(":::");
//        sendTextArea.add(sendTextPrompt, BorderLayout.WEST);
//
//        msgField = new JTextField();    // text field for inputting message
//        sendTextArea.add(msgField, BorderLayout.CENTER);
//        msgPanel.add(sendTextArea, BorderLayout.SOUTH);

        sendTextArea = new JPanel();
        sendTextArea.setLayout(new BorderLayout(0, 0));
        sendTextPrompt = new JLabel(":::");
        sendTextArea.add(sendTextPrompt, BorderLayout.WEST);

        JTextField msgField = new JTextField();    // text field for inputting message
        sendTextArea.add(msgField, BorderLayout.CENTER);

        JButton sendButton = new JButton("Send");
        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e)  {
                onSendChatMessage(msgField.getText());
                msgField.setText("");
            }
        });
        sendTextArea.add(sendButton, BorderLayout.EAST);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton sharePhoto = new JButton("Share photo");
        sharePhoto.addActionListener((ActionEvent e) -> {
            onSendChatImage();
        });
        JButton shareFile = new JButton("Share file");
        shareFile.addActionListener((ActionEvent e) -> {
            onSendChatFile();
        });
        top.add(sharePhoto);
        top.add(shareFile);

        sendTextArea.add(top, BorderLayout.NORTH);
        sendTextArea.setBorder(new EmptyBorder(0, 0, 10, 0));

        msgPanel.add(sendTextArea, BorderLayout.SOUTH);


        // handle key-input event of the message field
        msgField.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyPressed(KeyEvent e) {
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == 10) {        // if the user press ENTER
                    onSendChatMessage(msgField.getText());
                    msgField.setText("");
                }
            }

        });

        chatArea = ChatArea.getInstance();

        JScrollPane scrollPaneRight = new JScrollPane(chatArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPaneRight.setPreferredSize(new Dimension(300, this.getHeight()));
        chatArea.setScrollBar(scrollPaneRight.getVerticalScrollBar());
        msgPanel.add(scrollPaneRight, BorderLayout.CENTER);

        this.setSize(new Dimension(800, 600));
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
    }

    /**
     * it will be invoked if the user selected the specific color through the color picker
     *
     * @param colorValue - the selected color
     */
    public void selectColor(int colorValue) {
        SwingUtilities.invokeLater(() -> {
            selectedColor = colorValue;
            pnlColorPicker.setBackground(new Color(colorValue));
        });
    }

    /**
     * it will be invoked if the user inputted text in the message field
     *
     * @param text - user inputted text
     */
    private void onSendChatMessage(String text) {
        if (text.isEmpty()) return;


        if (InternalServer.isRunning()) {
            ChatArea.getInstance().addMessage(text, UserNameInput.getUserName());
            InternalServer.getInstance().sendMyChatMessage(text);
        } else {
            ExternalConnectedServer.getInstance().sendChatMessage(text);
        }

    }

    private void onSendChatImage() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setName("Send a photo");
        FileNameExtensionFilter fileFiler = new FileNameExtensionFilter(
                "Image Files", new String[] {"jpg", "jpeg", "png", "gif", "bmp"});
        fileChooser.setFileFilter(fileFiler);
        int option = fileChooser.showOpenDialog(ChatArea.getInstance());
        if (option == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();

            if (InternalServer.isRunning()) {
                ChatArea.getInstance().addImageMessage(selectedFile, UserNameInput.getUserName());
                InternalServer.getInstance().sendMyImageMessage(selectedFile);
            } else {
                ExternalConnectedServer.getInstance().sendImageMessage(selectedFile);
            }


        } else {
            System.out.println("Sending photo cancelled");
            return;
        }

    }


    private void onSendChatFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setName("Send a file");
        int option = fileChooser.showOpenDialog(ChatArea.getInstance());
        if (option == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();

            if (InternalServer.isRunning()) {
                ChatArea.getInstance().addFileMessage(selectedFile, UserNameInput.getUserName());
                InternalServer.getInstance().sendMyFileMessage(selectedFile);
            } else {
                ExternalConnectedServer.getInstance().sendFileMessage(selectedFile);
            }

        } else {
            System.out.println("Sending file cancelled");
            return;
        }

    }

    /**
     * change the color of a specific pixel
     *
     * @param col, row - the position of the selected pixel
     */

    public void paintPixel(int col, int row, int pentype, int pensize) {
        paintPixel(selectedColor, col, row, pentype, pensize);
    }

    public void paintPixel(int color, int col, int row, int pentype, int pensize) {

        switch (pentype) {
            case 0:// circle
                paintCircle(color, col, row, pentype, pensize);
                break;

            case 1:// square
                paintSquare(color, col, row, pentype, pensize);
                break;

            case 2:// star
                paintStar(color, col, row, pentype, pensize);
                break;
            default:
                paintPixel(color, col, row);

        }

    }

    public void paintPixel(int col, int row) {
        paintPixel(selectedColor, col, row);
    }

    public void paintPixel(int color, int col, int row) {
        if (col >= data.length || row >= data[0].length || row < 0 || col < 0)
            return;

        data[col][row] = color;
        paintPanel.repaint(col * blockSize, row * blockSize, blockSize, blockSize);
    }

    public void paintCircle(int color, int col, int row, int pentype, int pensize) {
        int x = 0;
        int y = pensize - 1;
        int d = 3 - 2 * (pensize - 1);
        while (x <= y) {
            for (int i = col - x; i <= col + x; i++) {
                paintPixel(color, i, row + y);
                paintPixel(color, i, row - y);

            }
            for (int i = col - y; i <= col + y; i++) {
                paintPixel(color, i, row + x);
                paintPixel(color, i, row - x);

            }

            if (d < 0) {
                d = d + 4 * x + 6;
            } else {
                d = d + 4 * (x - y) + 10;
                y--;
            }
            x++;
        }
    }

    public void paintSquare(int color, int col, int row, int pentype, int pensize) {
        for (int i = 0; i < pensize; i++) {
            for (int j = 0; j < pensize; j++) {
                paintPixel(color, col - i, row - j);
            }
        }
        for (int i = 0; i < pensize; i++) {
            for (int j = 0; j < pensize; j++) {
                paintPixel(color, col + i, row - j);
            }
        }
        for (int i = 0; i < pensize; i++) {
            for (int j = 0; j < pensize; j++) {
                paintPixel(color, col - i, row + j);
            }
        }
        for (int i = 0; i < pensize; i++) {
            for (int j = 0; j < pensize; j++) {
                paintPixel(color, col + i, row + j);
            }
        }
    }

    public void paintStar(int color, int col, int row, int pentype, int pensize) {
        for (int i = 0; i < pensize; i++) {

            paintPixel(color, col, row - i);

        }
        for (int i = 0; i < pensize; i++) {

            paintPixel(color, col - i, row);

        }
        for (int i = 0; i < pensize; i++) {

            paintPixel(color, col + i, row);

        }
        for (int i = 0; i < pensize; i++) {

            paintPixel(color, col - i, row + i);

        }
        for (int i = 0; i < pensize; i++) {

            paintPixel(color, col + i, row + i);

        }
    }

    /**
     * change the color of a specific area
     *
     * @param col, row - the position of the selected pixel
     * @return a list of modified pixels
     */
    public void paintArea(int col, int row) {
        paintArea(selectedColor, col, row);
    }

    public List paintArea(int color, int col, int row) {
        LinkedList<Point> filledPixels = new LinkedList<Point>();

        if (col >= data.length || row >= data[0].length) return filledPixels;

        int oriColor = data[col][row];
        LinkedList<Point> buffer = new LinkedList<Point>();

        if (oriColor != color) {
            buffer.add(new Point(col, row));

            while (!buffer.isEmpty()) {
                Point p = buffer.removeFirst();
                int x = p.x;
                int y = p.y;

                if (data[x][y] != oriColor) continue;

                data[x][y] = color;
                filledPixels.add(p);

                if (x > 0 && data[x - 1][y] == oriColor) buffer.add(new Point(x - 1, y));
                if (x < data.length - 1 && data[x + 1][y] == oriColor) buffer.add(new Point(x + 1, y));
                if (y > 0 && data[x][y - 1] == oriColor) buffer.add(new Point(x, y - 1));
                if (y < data[0].length - 1 && data[x][y + 1] == oriColor) buffer.add(new Point(x, y + 1));
            }
            paintPanel.repaint();
        }
        return filledPixels;
    }

    /**
     * set pixel data and block size
     *
     * @param data
     * @param blockSize
     */
    public void setData(int[][] data, int blockSize) {
        this.data = data;
        this.blockSize = blockSize;
        paintPanel.setPreferredSize(new Dimension(data.length * blockSize, data[0].length * blockSize));
        paintPanel.repaint();
    }

    public void loadusername(String username) {
        this.username = username;
        sendTextPrompt.setText(username + ": ");
    }

    public static void loadDrawingFromFileIntoDataArray(File file, int[][][] outputDataArray, int[] outputBlockSize, boolean[] success) {
        success[0] = false;
        try (Scanner in = new Scanner(file)) {
            int rows = Integer.parseInt(in.next());
            int cols = Integer.parseInt(in.next());
            outputDataArray[0] = new int[rows][cols];
            outputBlockSize[0] = Integer.parseInt(in.next());
            in.nextLine();


            for (int i = 0; in.hasNextLine(); i++) {
                String[] row = in.nextLine().split(" ");
                for (int j = 0; j < row.length; j++)
                    outputDataArray[0][j][i] = Integer.parseInt(row[j]);

            }

        } catch (FileNotFoundException e) {
            if (InternalServer.isRunning()) {
                JOptionPane.showMessageDialog(UI.getInstance(),
                        "File not found!");
            } else {
                System.out.println(e.getMessage());
            }
        }
        success[0] = true;

    }

    public void loadDrawingLocally(int[][] newData, int newBlockSize) {
        data = newData;
        blockSize = newBlockSize;
        paintPanel.repaint();
    }


    public void saveDrawingToFile(File selectedFile) {


        try (PrintWriter writer = new PrintWriter(selectedFile)) {
            writer.write(String.valueOf(data.length));
            writer.write(" ");
            writer.write(String.valueOf(data[0].length));
            writer.write(" ");
            writer.write(String.valueOf(blockSize));
            writer.println();

            for (int col = 0; col < data.length; col++) {
                for (int row = 0; row < data[col].length; row++) {
                    writer.write(String.valueOf(data[row][col]));
                    writer.write(" ");
                }
                writer.println();
            }


            writer.flush();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(UI.this,
                    "Error saving file: " + ex.getMessage());
        }


    }

    public void changeAllColorToSelectedColor(int col, int row, int color) {
        int origColor = data[col][row];
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[i].length; j++) {
                if (data[i][j] == origColor) {
                    if (InternalServer.isRunning()) {
                        paintPixel(color, i, j, 0, 1); // pentype = 0, pensize = 1
                        InternalServer.getInstance().sendMyPixelMessage(color, i, j, 0, 1);// pentype 0, pensize 1
                    } else {
                        ExternalConnectedServer.getInstance().sendPixelMessage(color, i, j, 0, 1);// pentype 0, pensize 1
                    }
                }
            }
        }
    }

    public void internalServerSetup(){
        String code = InternalServer.getInvitationCode();
        JButton inviteCodeButton = new JButton("Invite Others");
        inviteCodeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JTextArea textArea = new JTextArea(code);
                textArea.setEditable(false);
                JScrollPane scrollPane = new JScrollPane(textArea);
                JOptionPane.showMessageDialog(null, scrollPane, "Share this invitation code", JOptionPane.INFORMATION_MESSAGE);
                JOptionPane.setRootFrame(UI.getInstance());
            }
        });

        toolPanel.add(inviteCodeButton);
        toolPanel.revalidate();
        toolPanel.repaint();

    }


}
