package com.alexecollins.photoapp;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.*;

/**
 * Hello world!
 */
public class App {
    private static class Tuple2<T1, T2> {
        final T1 _1;
        final T2 _2;

        private Tuple2(T1 t1, T2 t2) {
            _1 = t1;
            _2 = t2;
        }
    }

    private static class LruCache<A, B> extends LinkedHashMap<A, B> {
        private final int maxEntries;

        public LruCache(final int maxEntries) {
            super(maxEntries + 1, 1.0f, true);
            this.maxEntries = maxEntries;
        }

        @Override
        protected boolean removeEldestEntry(final Map.Entry<A, B> eldest) {
            return super.size() > maxEntries;
        }
    }

    private static class ImagePanel extends JPanel {
        private BufferedImage image;

        @Override
        public void paintComponent(Graphics g) {

            final float scale = Math.min(
                    (float) getHeight() / image.getHeight(),
                    (float) getWidth() / image.getWidth()
            );

            Graphics2D g2 = (Graphics2D) g;
            int newW = (int) (image.getWidth() * scale);
            int newH = (int) (image.getHeight() * scale);
            int x = (getWidth() - newW) / 2;
            int y = (getHeight() - newH) / 2;


            //g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
             //       RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(image, x, y, newW, newH, null);
        }
    }

    private static int index;
    private static File[] files;
    private static LruCache<File, BufferedImage> images = new LruCache<File, BufferedImage>(2);
    private static final ImagePanel imagePanel = new ImagePanel();
    private static final JLabel marksLabel = new JLabel("");

    public static void main(String[] args) throws IOException {

        files = new File(args[0]).listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                return s.endsWith(".JPG");
            }
        });
        index = 0;

        System.out.println("images=" + Arrays.toString(files));

        final JFrame frame = new JFrame();
        frame.setBackground(Color.BLACK);

        frame.setExtendedState(Frame.MAXIMIZED_BOTH);
        frame.setUndecorated(true);

        updateImage(0);

        frame.getContentPane().setBackground(Color.BLACK);
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(imagePanel, BorderLayout.CENTER);
        frame.getContentPane().add(marksLabel, BorderLayout.SOUTH);

        marksLabel.setForeground(Color.GRAY);
        marksLabel.setHorizontalAlignment(SwingConstants.CENTER);

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
                    case KeyEvent.VK_R:
                        imagePanel.image = rotate(imagePanel.image, Math.PI / 2);
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

    private static void updateImage(final int direction) {
        index += direction;
        index = index % files.length;
        final File file = files[index];
        if (!images.containsKey(file)) {
            try {
                images.put(file, ImageIO.read(file));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        imagePanel.image = images.get(file);
        updateLabel();

        new Thread(new Runnable() {
            @Override
            public void run() {
                prePopulateNextImage(direction);
            }
        }).start();
    }

    private static void prePopulateNextImage(int direction) {
        int i = (index + direction) % files.length;

        final File file = files[i];

        if (images.containsKey(file)) {
            return;
        }

        try {
            images.put(file, ImageIO.read(file));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void updateLabel() {
        marksLabel.setText(image().getName() + " " + String.valueOf(getMarks(image())));
    }

    private static File image() {
        return files[index];
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
        for (char c : new char[]{'1', '2', '3', '4'}) {
            if (markerFile(image, c).exists()) {
                marks.add(c);
            }
        }
        return marks;
    }

    private static File markerFile(File image, char c) {
        return new File(image.getParentFile(), c + "/" + image.getName());
    }

    public static BufferedImage rotate(BufferedImage image, double angle) {
        double sin = Math.abs(Math.sin(angle)), cos = Math.abs(Math.cos(angle));
        int w = image.getWidth(), h = image.getHeight();
        int neww = (int) Math.floor(w * cos + h * sin), newh = (int) Math.floor(h * cos + w * sin);
        GraphicsConfiguration gc = getDefaultConfiguration();
        BufferedImage result = gc.createCompatibleImage(neww, newh, Transparency.TRANSLUCENT);
        Graphics2D g = result.createGraphics();
        g.translate((neww - w) / 2, (newh - h) / 2);
        g.rotate(angle, w / 2, h / 2);
        g.drawRenderedImage(image, null);
        g.dispose();
        return result;
    }



    public static GraphicsConfiguration getDefaultConfiguration() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();
        return gd.getDefaultConfiguration();
    }
}
