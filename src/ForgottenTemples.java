
import javax.microedition.midlet.MIDlet;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import java.io.IOException;

public class ForgottenTemples extends MIDlet {

    private Display display;
    private LoadingCanvas loadingCanvas;
    private boolean destroyed = false;

    public void startApp() {
        if (destroyed) {
            return;
        }

        display = Display.getDisplay(this);

        if (loadingCanvas == null) {
            loadingCanvas = new LoadingCanvas();
        }

        display.setCurrent(loadingCanvas);
        loadingCanvas.beginLoading();
    }

    public void pauseApp() {
    }

    public void destroyApp(boolean unconditional) {
        destroyed = true;

        if (loadingCanvas != null) {
            loadingCanvas.stopLoading();
        }

        notifyDestroyed();
    }

    private class LoadingCanvas extends Canvas implements Runnable {

        private Image logo;
        private Thread loadingThread;
        private boolean loading;
        private boolean finished;
        private long startTime;
        private int animationPosition;

        LoadingCanvas() {
            setFullScreenMode(true);

            try {
                logo = Image.createImage("/temple_logo.png");
            } catch (IOException e) {
                logo = null;
            }
        }

        synchronized void beginLoading() {
            if (loadingThread != null || finished) {
                return;
            }

            loading = true;
            startTime = System.currentTimeMillis();

            loadingThread = new Thread(this);
            loadingThread.start();
        }

        synchronized void stopLoading() {
            loading = false;

            if (loadingThread != null) {
                loadingThread.interrupt();
                loadingThread = null;
            }
        }

        public void run() {
            Thread thisThread = Thread.currentThread();

            try {
                while (loading && !destroyed) {
                    long elapsed =
                        System.currentTimeMillis() - startTime;

                    animationPosition =
                        (int) ((elapsed / 35) % 100);

                    repaint();

                    if (elapsed >= 3500) {
                        break;
                    }

                    try {
                        Thread.sleep(35);
                    } catch (InterruptedException e) {
                        return;
                    }
                }

                if (!loading || destroyed) {
                    return;
                }

                finished = true;
                loading = false;
                animationPosition = 100;
                repaint();

                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    return;
                }

                if (!destroyed && display != null) {
                    display.setCurrent(new MenuCanvas());
                }
            } finally {
                synchronized (this) {
                    if (loadingThread == thisThread) {
                        loadingThread = null;
                    }
                }
            }
        }

        protected void paint(Graphics g) {
            int w = getWidth();
            int h = getHeight();

            g.setColor(12, 18, 25);
            g.fillRect(0, 0, w, h);

            if (logo != null) {
                int x = (w - logo.getWidth()) / 2;
                int y = (h - logo.getHeight()) / 2;

                g.drawImage(
                    logo,
                    x,
                    y,
                    Graphics.TOP | Graphics.LEFT
                );
            } else {
                g.setColor(220, 176, 83);
                g.drawString(
                    "FORGOTTEN TEMPLES",
                    w / 2,
                    h / 3,
                    Graphics.HCENTER | Graphics.TOP
                );

                g.setColor(230, 230, 230);
                g.drawString(
                    "Image missing",
                    w / 2,
                    h / 3 + 24,
                    Graphics.HCENTER | Graphics.TOP
                );
            }

            drawLoadingBar(g, w, h);
        }

        private void drawLoadingBar(Graphics g, int w, int h) {
            int barWidth = w - 48;

            if (barWidth > 200) {
                barWidth = 200;
            }

            if (barWidth < 40) {
                barWidth = w - 16;
            }

            int barHeight = 7;
            int barX = (w - barWidth) / 2;
            int barY = h - 39;

            g.setColor(20, 24, 25);
            g.fillRect(
                barX - 2,
                barY - 2,
                barWidth + 4,
                barHeight + 4
            );

            g.setColor(105, 91, 62);
            g.drawRect(barX, barY, barWidth, barHeight);

            int segmentWidth = barWidth / 4;

            if (segmentWidth < 8) {
                segmentWidth = 8;
            }

            int travel = barWidth - segmentWidth;
            int movingX =
                barX + (animationPosition * travel / 100);

            g.setColor(224, 171, 69);
            g.fillRect(
                movingX,
                barY + 1,
                segmentWidth,
                barHeight - 1
            );

            g.setColor(255, 240, 195);

            String label = finished
                ? "READY"
                : "ENTERING THE TEMPLES";

            g.drawString(
                label,
                w / 2,
                barY - 20,
                Graphics.HCENTER | Graphics.TOP
            );
        }
    }

    private class MenuCanvas extends Canvas {

        private int selected = 0;

        private final String[] options = {
            "START ADVENTURE",
            "HOW TO PLAY",
            "SETTINGS"
        };

        MenuCanvas() {
            setFullScreenMode(true);
        }

        protected void paint(Graphics g) {
            int w = getWidth();
            int h = getHeight();

            g.setColor(12, 18, 25);
            g.fillRect(0, 0, w, h);

            g.setColor(219, 171, 74);
            g.drawString(
                "FORGOTTEN TEMPLES",
                w / 2,
                25,
                Graphics.HCENTER | Graphics.TOP
            );

            g.setColor(174, 151, 102);
            g.drawLine(20, 49, w - 20, 49);

            int firstY = h / 2 - 32;

            for (int i = 0; i < options.length; i++) {
                int y = firstY + i * 34;

                if (i == selected) {
                    g.setColor(112, 76, 35);
                    g.fillRect(15, y - 4, w - 30, 27);

                    g.setColor(255, 224, 148);
                } else {
                    g.setColor(220, 220, 220);
                }

                g.drawString(
                    options[i],
                    w / 2,
                    y,
                    Graphics.HCENTER | Graphics.TOP
                );
            }

            g.setColor(185, 185, 185);
            g.drawString(
                "UP/DOWN: SELECT",
                w / 2,
                h - 43,
                Graphics.HCENTER | Graphics.TOP
            );

            g.drawString(
                "5 OR FIRE: CONFIRM",
                w / 2,
                h - 27,
                Graphics.HCENTER | Graphics.TOP
            );
        }

        protected void keyPressed(int keyCode) {
            int action = getGameAction(keyCode);

            if (action == UP || keyCode == KEY_NUM2) {
                selected--;

                if (selected < 0) {
                    selected = options.length - 1;
                }

                repaint();
                return;
            }

            if (action == DOWN || keyCode == KEY_NUM8) {
                selected++;

                if (selected >= options.length) {
                    selected = 0;
                }

                repaint();
                return;
            }

            if (action == FIRE
                    || keyCode == KEY_NUM5
                    || keyCode == KEY_NUM0) {
                openSelectedOption();
            }
        }

        private void openSelectedOption() {
            if (selected == 0) {
                display.setCurrent(new MessageCanvas(
                    "ADVENTURE",
                    "The first temple",
                    "The playable level",
                    "comes next."
                ));
            } else if (selected == 1) {
                display.setCurrent(new MessageCanvas(
                    "HOW TO PLAY",
                    "Use the direction",
                    "keys to explore.",
                    "More instructions",
                    "will follow."
                ));
            } else {
                display.setCurrent(new MessageCanvas(
                    "SETTINGS",
                    "Game settings",
                    "will be added",
                    "in a later build."
                ));
            }
        }
    }

    private class MessageCanvas extends Canvas {

        private String[] lines;

        MessageCanvas(String a, String b, String c, String d) {
            lines = new String[] {a, b, c, d};
            setFullScreenMode(true);
        }

        MessageCanvas(
                String a,
                String b,
                String c,
                String d,
                String e) {
            lines = new String[] {a, b, c, d, e};
            setFullScreenMode(true);
        }

        protected void paint(Graphics g) {
            int w = getWidth();
            int h = getHeight();

            g.setColor(12, 18, 25);
            g.fillRect(0, 0, w, h);

            g.setColor(224, 176, 79);
            g.drawString(
                lines[0],
                w / 2,
                h / 3,
                Graphics.HCENTER | Graphics.TOP
            );

            g.setColor(235, 235, 235);

            for (int i = 1; i < lines.length; i++) {
                g.drawString(
                    lines[i],
                    w / 2,
                    h / 3 + 30 + (i - 1) * 22,
                    Graphics.HCENTER | Graphics.TOP
                );
            }

            g.setColor(200, 200, 200);
            g.drawString(
                "Press 5 or FIRE to return",
                w / 2,
                h - 30,
                Graphics.HCENTER | Graphics.TOP
            );
        }

        protected void keyPressed(int keyCode) {
            int action = getGameAction(keyCode);

            if (action == FIRE
                    || keyCode == KEY_NUM5
                    || keyCode == KEY_NUM0
                    || action == LEFT
                    || action == RIGHT) {
                display.setCurrent(new MenuCanvas());
            }
        }
    }
}
