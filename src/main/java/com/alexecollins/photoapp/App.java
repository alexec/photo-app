package com.alexecollins.photoapp;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;

/**
 * Hello world!
 */
public class App {
    private static class ImagePanel extends JPanel {
        private BufferedImage originalImage;

        @Override
        public void paintComponent(Graphics g) {

            final float scale = Math.min(
                    (float) getHeight() / originalImage.getHeight(),
                    (float) getWidth() / originalImage.getWidth()
            );

            Graphics2D g2 = (Graphics2D)g;
            int newW = (int)(originalImage.getWidth() * scale);
            int newH = (int)(originalImage.getHeight() * scale);
            int x = (getWidth() - newW) / 2;
            int y = (getHeight() - newH) / 2;


            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(originalImage, x, y, newW, newH, null);
        }
    }

    private static int index;
    private static File[] images;
    private static final ImagePanel imagePanel = new ImagePanel();
    private static final JLabel marksLabel = new JLabel("");

    public static void main(String[] args) throws IOException {

        images = new File(args[0]).listFiles();
        index = 0;

        System.out.println("images=" + Arrays.toString(images));

        final JFrame frame = new JFrame();

        frame.setExtendedState(Frame.MAXIMIZED_BOTH);
        frame.setUndecorated(true);

        updateImage(1);

        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(imagePanel, BorderLayout.CENTER);

        frame.getContentPane().add(marksLabel, BorderLayout.SOUTH);

        frame.requestFocus();
        frame.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent keyEvent) {
                switch (keyEvent.getKeyCode()) {
                    case KeyEvent.VK_1:
                    case KeyEvent.VK_2:
                    case KeyEvent.VK_3:
                    case KeyEvent.VK_4:
                        toggleMark(keyEvent.getKeyChar());
                        updateLabel();
                        break;
                    case KeyEvent.VK_LEFT:
                        updateImage(-1);
                        break;
                    case KeyEvent.VK_RIGHT:
                        updateImage(1);
                        break;
                    case KeyEvent.VK_ESCAPE:
                        System.exit(0);
                }
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        frame.repaint();
                    }
                });
            }
        });

        frame.setVisible(true);


        GraphicsDevice device = GraphicsEnvironment
                .getLocalGraphicsEnvironment().getScreenDevices()[0];

        device.setFullScreenWindow(frame);
    }

    private static void updateImage(int direction) {
        imagePanel.originalImage = nextImage(direction);
        updateLabel();

    }

    private static void updateLabel() {
        marksLabel.setText(image() +" " + String.valueOf(getMarks(image())));
    }

    private static File image() {
        final File image = images[index];
        System.out.println(image);
        return image;
    }

    private static Set<Character> toggleMark(char mark) {
        final File image = image();

        final Set<Character> imageMarks = getMarks(image);
        final File markerFile = markerFile(image, mark);
        if (imageMarks.contains(mark)) {
            System.out.println("removing mark " + mark);
            markerFile.delete();
        } else {
            System.out.println("adding mark " + mark);
            try {
                final File dir = markerFile.getParentFile();
                if (!dir.exists()) {
                    dir.mkdir();
                }
                final Process exec = Runtime.getRuntime().exec("/bin/ln -s ../" + image.getName() + " " + markerFile.getCanonicalPath());
                if (exec.waitFor() != 0) {
                    throw new RuntimeException(new Scanner(exec.getErrorStream()).useDelimiter("\\A").next());
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return imageMarks;
    }

    private static Set<Character> getMarks(File image) {
        final Set<Character> marks = new TreeSet<Character>();
        for (char c : new char[] {'1','2','3','4'}) {
            if (markerFile(image, c).exists()) {
                marks.add(c);
            }
        }
         return marks;
    }

    private static File markerFile(File image, char c) {
        return new File(image.getParentFile(), c + "/" + image.getName());
    }

    private static BufferedImage nextImage(int direction) {
        BufferedImage image = null;
        index += direction      ;
        while (index < images.length) {
            System.out.println("index=" + index);
            try {
                image = ImageIO.read(image());
                if (image != null) {
                    break;
                }
            } catch (IOException ignored) {
            }
            index += direction  ;
        }
        return image;
    }
}
