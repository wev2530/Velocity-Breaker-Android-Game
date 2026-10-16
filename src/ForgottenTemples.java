
import javax.microedition.midlet.MIDlet;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import java.io.IOException;

public class ForgottenTemples extends MIDlet {

    private Display display;
    private LoadingCanvas loadingCanvas;
    private boolean destroyed;

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
        private Thread worker;
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
            if (worker != null || finished) {
                return;
            }

            loading = true;
            startTime = System.currentTimeMillis();
            worker = new Thread(this);
            worker.start();
        }

        synchronized void stopLoading() {
            loading = false;

            if (worker != null) {
                worker.interrupt();
                worker = null;
            }
        }

        public void run() {
            Thread current = Thread.currentThread();

            try {
                while (loading && !destroyed) {
                    long elapsed =
                        System.currentTimeMillis() - startTime;

                    animationPosition =
                        (int) ((elapsed / 35) % 100);

                    repaint();

                    if (elapsed >= 2200) {
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
                repaint();

                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    return;
                }

                if (!destroyed && display != null) {
                    display.setCurrent(new MenuCanvas());
                }
            } finally {
                synchronized (this) {
                    if (worker == current) {
                        worker = null;
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
                int y = (h - 90 - logo.getHeight()) / 2;

                if (x < 0) {
                    x = 0;
                }

                if (y < 8) {
                    y = 8;
                }

                g.drawImage(
                    logo,
                    x,
                    y,
                    Graphics.TOP | Graphics.LEFT
                );
            } else {
                g.setColor(224, 176, 79);
                g.drawString(
                    "FORGOTTEN TEMPLES",
                    w / 2,
                    h / 3,
                    Graphics.HCENTER | Graphics.TOP
                );
            }

            int barWidth = w - 48;
            if (barWidth > 200) {
                barWidth = 200;
            }
            if (barWidth < 40) {
                barWidth = w - 16;
            }

            int bx = (w - barWidth) / 2;
            int by = h - 39;

            g.setColor(80, 72, 55);
            g.drawRect(bx, by, barWidth, 7);

            int segment = barWidth / 4;
            if (segment < 8) {
                segment = 8;
            }

            int travel = barWidth - segment;
            int sx = bx + animationPosition * travel / 100;

            g.setColor(224, 171, 69);
            g.fillRect(sx, by + 1, segment, 6);

            g.setColor(255, 240, 195);
            g.drawString(
                finished ? "READY" : "ENTERING THE TEMPLES",
                w / 2,
                by - 20,
                Graphics.HCENTER | Graphics.TOP
            );
        }
    }

    private class MenuCanvas extends Canvas {

        private int selected;
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

            g.setColor(224, 176, 79);
            g.drawString(
                "FORGOTTEN TEMPLES",
                w / 2,
                28,
                Graphics.HCENTER | Graphics.TOP
            );

            g.setColor(120, 100, 65);
            g.drawLine(18, 52, w - 18, 52);

            int firstY = h / 2 - 30;

            for (int i = 0; i < options.length; i++) {
                int y = firstY + i * 34;

                if (selected == i) {
                    g.setColor(112, 76, 35);
                    g.fillRect(12, y - 4, w - 24, 26);
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
                "2/8: SELECT",
                w / 2,
                h - 48,
                Graphics.HCENTER | Graphics.TOP
            );

            g.drawString(
                "5: CONFIRM",
                w / 2,
                h - 29,
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
            } else if (action == DOWN || keyCode == KEY_NUM8) {
                selected++;
                if (selected >= options.length) {
                    selected = 0;
                }
                repaint();
            } else if (action == FIRE
                    || keyCode == KEY_NUM5
                    || keyCode == KEY_NUM0) {
                if (selected == 0) {
                    display.setCurrent(new GameCanvas(1));
                } else if (selected == 1) {
                    display.setCurrent(new MessageCanvas(
                        "HOW TO PLAY",
                        "LEFT/RIGHT: MOVE",
                        "UP/DOWN: CLIMB",
                        "5: JUMP",
                        "Find the key and exit."
                    ));
                } else {
                    display.setCurrent(new MessageCanvas(
                        "SETTINGS",
                        "Settings will be",
                        "added later.",
                        "Press 5 to return."
                    ));
                }
            }
        }
    }

    private class GameCanvas extends Canvas implements Runnable {

        private static final int TILE = 16;
        private static final int MAP_W = 64;
        private static final int MAP_H = 18;

        private static final int AIR = 0;
        private static final int STONE = 1;
        private static final int DIAMOND = 2;
        private static final int KEY = 3;
        private static final int EXIT = 4;
        private static final int VINE = 5;
        private static final int CRACK = 6;
        private static final int ROCK = 7;
        private static final int TRAP = 8;

        private static final int PLAYER_W = 12;
        private static final int PLAYER_H = 15;

        private int[][] map;
        private int mission;
        private int playerX;
        private int playerY;
        private int velocityX;
        private int velocityY;
        private int cameraX;
        private int diamonds;
        private int totalDiamonds;
        private boolean hasKey;
        private boolean onGround;
        private boolean climbing;
        private boolean dead;
        private boolean complete;
        private boolean leftHeld;
        private boolean rightHeld;
        private boolean upHeld;
        private boolean downHeld;
        private boolean jumpRequested;
        private boolean running;
        private Thread gameThread;
        private long messageUntil;
        private String message = "";
        private int frame;

        GameCanvas(int stage) {
            mission = stage;

            if (mission < 1) {
                mission = 1;
            }
            if (mission > 8) {
                mission = 8;
            }

            setFullScreenMode(true);
            createLevel();
            startGameThread();
        }

        private void startGameThread() {
            if (gameThread != null) {
                return;
            }

            running = true;
            gameThread = new Thread(this);
            gameThread.start();
        }

        public void run() {
            Thread current = Thread.currentThread();

            try {
                while (running && !destroyed) {
                    updateGame();
                    repaint();

                    try {
                        Thread.sleep(45);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            } finally {
                synchronized (this) {
                    if (gameThread == current) {
                        gameThread = null;
                    }
                }
            }
        }

        private void createLevel() {
            map = new int[MAP_H][MAP_W];

            diamonds = 0;
            totalDiamonds = 0;
            hasKey = false;
            dead = false;
            complete = false;
            climbing = false;
            velocityX = 0;
            velocityY = 0;
            cameraX = 0;
            leftHeld = false;
            rightHeld = false;
            upHeld = false;
            downHeld = false;
            jumpRequested = false;

            for (int x = 0; x < MAP_W; x++) {
                map[16][x] = STONE;
                map[17][x] = STONE;
            }

            // Raised stone platforms.
            platform(5, 13, 10);
            platform(13, 11, 7);
            platform(23, 13, 9);
            platform(34, 10, 8);
            platform(45, 12, 8);
            platform(54, 9, 7);

            // Climbable green vines.
            vineColumn(8, 9, 13);
            vineColumn(17, 7, 11);
            vineColumn(28, 8, 13);
            vineColumn(38, 6, 10);
            vineColumn(49, 8, 12);
            vineColumn(57, 5, 9);

            // Cracked walls can be climbed.
            crackColumn(20, 8, 13);
            crackColumn(32, 7, 12);
            crackColumn(43, 8, 13);
            crackColumn(52, 6, 11);

            // Treasure placed along the route.
            putDiamond(4, 14);
            putDiamond(7, 12);
            putDiamond(10, 12);
            putDiamond(14, 10);
            putDiamond(18, 10);
            putDiamond(24, 12);
            putDiamond(27, 12);
            putDiamond(30, 12);
            putDiamond(35, 9);
            putDiamond(39, 9);
            putDiamond(46, 11);
            putDiamond(50, 11);
            putDiamond(55, 8);
            putDiamond(59, 14);

            // The key is required to open the exit.
            map[10][21] = KEY;

            // Hazards on the floor.
            map[15][12] = TRAP;
            map[15][33] = TRAP;
            map[15][44] = TRAP;

            // Pushable-looking rocks for future interaction.
            map[15][25] = ROCK;
            map[15][40] = ROCK;

            // Locked exit near the end of the level.
            map[15][62] = EXIT;

            // Later stages get extra hazards.
            if (mission >= 2) {
                map[15][18] = TRAP;
                map[15][48] = TRAP;
            }
            if (mission >= 3) {
                map[15][29] = TRAP;
                map[15][56] = TRAP;
            }
            if (mission >= 4) {
                map[15][36] = TRAP;
                map[15][60] = TRAP;
            }

            playerX = 24;
            playerY = 16 * TILE - PLAYER_H;

            // Each stage keeps the same route for now, but
            // adds hazards as a first difficulty progression.
            if (mission >= 5) {
                putDiamond(16, 6);
                putDiamond(41, 5);
                putDiamond(58, 4);
            }

            totalDiamonds = countDiamonds();
        }

        private void platform(int x, int row, int length) {
            for (int i = 0; i < length; i++) {
                int px = x + i;
                if (px >= 0 && px < MAP_W) {
                    map[row][px] = STONE;
                }
            }
        }

        private void vineColumn(int x, int top, int bottom) {
            for (int y = top; y <= bottom; y++) {
                if (x >= 0 && x < MAP_W
                        && y >= 0 && y < MAP_H
                        && map[y][x] == AIR) {
                    map[y][x] = VINE;
                }
            }
        }

        private void crackColumn(int x, int top, int bottom) {
            for (int y = top; y <= bottom; y++) {
                if (x >= 0 && x < MAP_W
                        && y >= 0 && y < MAP_H
                        && map[y][x] == AIR) {
                    map[y][x] = CRACK;
                }
            }
        }

        private void putDiamond(int x, int y) {
            if (x >= 0 && x < MAP_W && y >= 0 && y < MAP_H) {
                map[y][x] = DIAMOND;
            }
        }

        private int countDiamonds() {
            int count = 0;

            for (int y = 0; y < MAP_H; y++) {
                for (int x = 0; x < MAP_W; x++) {
                    if (map[y][x] == DIAMOND) {
                        count++;
                    }
                }
            }

            return count;
        }

        private boolean solidAt(int px, int py) {
            if (px < 0 || px >= MAP_W * TILE) {
                return true;
            }

            if (py < 0) {
                return false;
            }

            if (py >= MAP_H * TILE) {
                return true;
            }

            int tx = px / TILE;
            int ty = py / TILE;
            int tile = map[ty][tx];

            if (tile == STONE || tile == ROCK) {
                return true;
            }

            if (tile == CRACK && !climbing) {
                return true;
            }

            return false;
        }

        private boolean touchingClimbable() {
            int left = playerX / TILE;
            int right = (playerX + PLAYER_W - 1) / TILE;
            int top = playerY / TILE;
            int bottom = (playerY + PLAYER_H - 1) / TILE;

            for (int y = top; y <= bottom; y++) {
                for (int x = left - 1; x <= right + 1; x++) {
                    if (x < 0 || x >= MAP_W
                            || y < 0 || y >= MAP_H) {
                        continue;
                    }

                    int tile = map[y][x];

                    if (tile == VINE || tile == CRACK) {
                        int tileX = x * TILE;

                        if (playerX + PLAYER_W >= tileX - 3
                                && playerX <= tileX + TILE + 3) {
                            return true;
                        }
                    }
                }
            }

            return false;
        }

        private void updateGame() {
            if (dead || complete) {
                return;
            }

            frame++;

            if (upHeld && touchingClimbable()) {
                climbing = true;
            }

            if (!touchingClimbable()) {
                climbing = false;
            }

            if (climbing) {
                velocityY = 0;

                if (upHeld) {
                    moveVertical(-2);
                } else if (downHeld) {
                    moveVertical(2);
                }

                if (leftHeld) {
                    moveHorizontal(-2);
                } else if (rightHeld) {
                    moveHorizontal(2);
                }
            } else {
                if (leftHeld) {
                    velocityX = -2;
                } else if (rightHeld) {
                    velocityX = 2;
                } else {
                    velocityX = 0;
                }

                moveHorizontal(velocityX);

                if (jumpRequested && onGround) {
                    velocityY = -7;
                    onGround = false;
                }

                jumpRequested = false;

                if (velocityY < 5) {
                    velocityY++;
                }

                moveVertical(velocityY);
            }

            collectTile();
            checkHazard();
            checkExit();
            updateCamera();
        }

        private void moveHorizontal(int amount) {
            if (amount == 0) {
                return;
            }

            int nextX = playerX + amount;

            if (amount > 0) {
                int edge = nextX + PLAYER_W - 1;

                if (solidAt(edge, playerY)
                        || solidAt(edge, playerY + PLAYER_H - 1)) {
                    nextX = (edge / TILE) * TILE - PLAYER_W;
                }
            } else {
                if (solidAt(nextX, playerY)
                        || solidAt(nextX, playerY + PLAYER_H - 1)) {
                    nextX = (nextX / TILE + 1) * TILE;
                }
            }

            if (nextX < 0) {
                nextX = 0;
            }

            int maxX = MAP_W * TILE - PLAYER_W;
            if (nextX > maxX) {
                nextX = maxX;
            }

            playerX = nextX;
        }

        private void moveVertical(int amount) {
            if (amount == 0) {
                return;
            }

            int nextY = playerY + amount;
            onGround = false;

            if (amount > 0) {
                if (solidAt(playerX, nextY + PLAYER_H - 1)
                        || solidAt(playerX + PLAYER_W - 1,
                            nextY + PLAYER_H - 1)) {
                    int foot = nextY + PLAYER_H - 1;
                    nextY = (foot / TILE) * TILE - PLAYER_H;
                    velocityY = 0;
                    onGround = true;
                }
            } else {
                if (solidAt(playerX, nextY)
                        || solidAt(playerX + PLAYER_W - 1, nextY)) {
                    nextY = (nextY / TILE + 1) * TILE;
                    velocityY = 0;
                }
            }

            if (nextY < 0) {
                nextY = 0;
                velocityY = 0;
            }

            playerY = nextY;
        }

        private void collectTile() {
            int left = playerX / TILE;
            int right = (playerX + PLAYER_W - 1) / TILE;
            int top = playerY / TILE;
            int bottom = (playerY + PLAYER_H - 1) / TILE;

            for (int y = top; y <= bottom; y++) {
                for (int x = left; x <= right; x++) {
                    if (x < 0 || x >= MAP_W
                            || y < 0 || y >= MAP_H) {
                        continue;
                    }

                    if (map[y][x] == DIAMOND) {
                        map[y][x] = AIR;
                        diamonds++;
                        showMessage("DIAMOND FOUND!");
                    } else if (map[y][x] == KEY) {
                        map[y][x] = AIR;
                        hasKey = true;
                        showMessage("TEMPLE KEY FOUND!");
                    }
                }
            }
        }

        private void checkHazard() {
            int left = playerX / TILE;
            int right = (playerX + PLAYER_W - 1) / TILE;
            int top = playerY / TILE;
            int bottom = (playerY + PLAYER_H - 1) / TILE;

            for (int y = top; y <= bottom; y++) {
                for (int x = left; x <= right; x++) {
                    if (x >= 0 && x < MAP_W
                            && y >= 0 && y < MAP_H
                            && map[y][x] == TRAP) {
                        dead = true;
                        showMessage("TRAP! PRESS 5 TO RETRY");
                        return;
                    }
                }
            }

            if (playerY > MAP_H * TILE) {
                dead = true;
                showMessage("FELL! PRESS 5 TO RETRY");
            }
        }

        private void checkExit() {
            int tx = (playerX + PLAYER_W / 2) / TILE;
            int ty = (playerY + PLAYER_H / 2) / TILE;

            if (tx < 0 || tx >= MAP_W || ty < 0 || ty >= MAP_H) {
                return;
            }

            if (map[ty][tx] == EXIT) {
                if (hasKey) {
                    complete = true;
                    showMessage("TEMPLE CLEARED! PRESS 5");
                } else {
                    showMessage("FIND THE KEY FIRST!");
                }
            }
        }

        private void showMessage(String text) {
            message = text;
            messageUntil = System.currentTimeMillis() + 1600;
        }

        private void updateCamera() {
            int w = getWidth();
            int desired = playerX - w / 2;

            if (desired < 0) {
                desired = 0;
            }

            int max = MAP_W * TILE - w;
            if (desired > max) {
                desired = max;
            }

            cameraX = desired;
        }

        protected void keyPressed(int keyCode) {
            int action = getGameAction(keyCode);

            if (dead) {
                if (action == FIRE
                        || keyCode == KEY_NUM5
                        || keyCode == KEY_NUM0) {
                    createLevel();
                }
                return;
            }

            if (complete) {
                if (action == FIRE
                        || keyCode == KEY_NUM5
                        || keyCode == KEY_NUM0) {
                    if (mission < 8) {
                        display.setCurrent(new GameCanvas(mission + 1));
                    } else {
                        display.setCurrent(new MessageCanvas(
                            "CAMPAIGN COMPLETE",
                            "You explored all",
                            "eight temple stages.",
                            "More adventures",
                            "can be added next."
                        ));
                    }
                }
                return;
            }

            if (action == LEFT || keyCode == KEY_NUM4) {
                leftHeld = true;
            } else if (action == RIGHT || keyCode == KEY_NUM6) {
                rightHeld = true;
            } else if (action == UP || keyCode == KEY_NUM2) {
                upHeld = true;
            } else if (action == DOWN || keyCode == KEY_NUM8) {
                downHeld = true;
            } else if (action == FIRE
                    || keyCode == KEY_NUM5
                    || keyCode == KEY_NUM0) {
                jumpRequested = true;
            } else if (keyCode == KEY_NUM1) {
                running = false;
                display.setCurrent(new MenuCanvas());
            }
        }

        protected void keyReleased(int keyCode) {
            int action = getGameAction(keyCode);

            if (action == LEFT || keyCode == KEY_NUM4) {
                leftHeld = false;
            } else if (action == RIGHT || keyCode == KEY_NUM6) {
                rightHeld = false;
            } else if (action == UP || keyCode == KEY_NUM2) {
                upHeld = false;
            } else if (action == DOWN || keyCode == KEY_NUM8) {
                downHeld = false;
            }
        }

        protected void paint(Graphics g) {
            int w = getWidth();
            int h = getHeight();

            g.setColor(14, 20, 27);
            g.fillRect(0, 0, w, h);

            int viewTop = 24;
            int worldHeight = h - viewTop;

            // Draw distant temple background.
            g.setColor(23, 34, 39);
            g.fillRect(0, viewTop, w, worldHeight);

            g.setColor(34, 49, 49);

            for (int i = 0; i < 6; i++) {
                int bx = (i * 83 - cameraX / 3) % (w + 80);
                if (bx < -40) {
                    bx += w + 80;
                }

                g.fillRect(bx, viewTop + 30, 18, worldHeight - 30);
                g.fillRect(bx - 8, viewTop + 25, 34, 7);
            }

            int firstTile = cameraX / TILE;
            int lastTile = (cameraX + w) / TILE + 1;

            if (lastTile >= MAP_W) {
                lastTile = MAP_W - 1;
            }

            for (int y = 0; y < MAP_H; y++) {
                int screenY = viewTop + y * TILE;

                if (screenY < viewTop - TILE
                        || screenY > h) {
                    continue;
                }

                for (int x = firstTile; x <= lastTile; x++) {
                    int tile = map[y][x];
                    int screenX = x * TILE - cameraX;

                    drawTile(g, tile, screenX, screenY);
                }
            }

            drawPlayer(g, viewTop);

            // HUD.
            g.setColor(8, 13, 18);
            g.fillRect(0, 0, w, viewTop);

            g.setColor(224, 176, 79);
            g.drawString(
                "MISSION " + mission + "/8",
                5,
                5,
                Graphics.TOP | Graphics.LEFT
            );

            g.setColor(225, 235, 235);
            g.drawString(
                "D:" + diamonds + "/" + totalDiamonds,
                w - 5,
                5,
                Graphics.TOP | Graphics.RIGHT
            );

            if (hasKey) {
                g.setColor(255, 220, 90);
                g.drawString(
                    "KEY",
                    w / 2,
                    5,
                    Graphics.TOP | Graphics.HCENTER
                );
            }

            if (message.length() > 0
                    && System.currentTimeMillis() < messageUntil) {
                g.setColor(5, 9, 12);
                g.fillRect(4, h - 24, w - 8, 20);

                g.setColor(255, 224, 148);
                g.drawString(
                    message,
                    w / 2,
                    h - 21,
                    Graphics.TOP | Graphics.HCENTER
                );
            } else if (dead || complete) {
                g.setColor(255, 224, 148);
                g.drawString(
                    message,
                    w / 2,
                    h - 24,
                    Graphics.TOP | Graphics.HCENTER
                );
            }
        }

        private void drawTile(Graphics g, int tile, int x, int y) {
            if (tile == AIR) {
                return;
            }

            if (tile == STONE) {
                g.setColor(92, 91, 79);
                g.fillRect(x, y, TILE, TILE);

                g.setColor(123, 117, 94);
                g.drawLine(x, y, x + TILE - 1, y);
                g.drawLine(x + 3, y + 7, x + 10, y + 7);

                g.setColor(59, 62, 58);
                g.drawLine(x, y + TILE - 1, x + TILE - 1, y + TILE - 1);
            } else if (tile == DIAMOND) {
                g.setColor(76, 220, 236);
                int[] dx = {x + 8, x + 13, x + 8, x + 3};
                int[] dy = {y + 2, y + 8, y + 14, y + 8};
                g.fillTriangle(dx[0], dy[0], dx[1], dy[1], dx[2], dy[2]);
                g.fillTriangle(dx[0], dy[0], dx[2], dy[2], dx[3], dy[3]);

                g.setColor(205, 250, 255);
                g.drawLine(x + 8, y + 3, x + 8, y + 13);
            } else if (tile == KEY) {
                g.setColor(255, 212, 75);
                g.drawArc(x + 3, y + 3, 8, 8, 0, 360);
                g.drawLine(x + 10, y + 10, x + 14, y + 14);
                g.drawLine(x + 12, y + 12, x + 12, y + 14);
            } else if (tile == EXIT) {
                g.setColor(90, 56, 30);
                g.fillRect(x + 2, y - 8, TILE - 4, TILE + 8);

                g.setColor(222, 171, 71);
                g.drawRect(x + 2, y - 8, TILE - 4, TILE + 8);

                g.setColor(35, 28, 23);
                g.fillRect(x + 5, y - 4, TILE - 10, TILE + 4);

                g.setColor(255, 217, 93);
                g.fillRect(x + 10, y + 6, 2, 2);
            } else if (tile == VINE) {
                g.setColor(36, 112, 48);
                g.drawLine(x + 7, y, x + 7, y + TILE);

                g.setColor(68, 173, 63);
                g.drawLine(x + 7, y + 4, x + 3, y + 7);
                g.drawLine(x + 7, y + 10, x + 11, y + 13);
                g.drawLine(x + 7, y + 14, x + 3, y + 16);
            } else if (tile == CRACK) {
                g.setColor(104, 103, 89);
                g.fillRect(x, y, TILE, TILE);

                g.setColor(40, 43, 40);
                g.drawLine(x + 5, y, x + 8, y + 5);
                g.drawLine(x + 8, y + 5, x + 4, y + 9);
                g.drawLine(x + 4, y + 9, x + 10, y + 13);
                g.drawLine(x + 10, y + 13, x + 8, y + 16);

                g.setColor(145, 136, 111);
                g.drawLine(x, y, x + TILE - 1, y);
            } else if (tile == ROCK) {
                g.setColor(78, 80, 76);
                g.fillRect(x + 1, y + 1, TILE - 2, TILE - 2);

                g.setColor(130, 130, 116);
                g.drawLine(x + 3, y + 4, x + 10, y + 3);
                g.drawLine(x + 10, y + 3, x + 13, y + 8);
            } else if (tile == TRAP) {
                g.setColor(83, 47, 36);
                g.fillRect(x, y + 10, TILE, 6);

                g.setColor(219, 79, 48);
                g.fillTriangle(x, y + 11, x + 5, y + 2, x + 9, y + 11);
                g.fillTriangle(x + 7, y + 11, x + 12, y + 2, x + 16, y + 11);
            }
        }

        private void drawPlayer(Graphics g, int viewTop) {
            int x = playerX - cameraX;
            int y = viewTop + playerY;

            // Shadow.
            g.setColor(6, 9, 10);
            g.fillRect(x - 1, y + PLAYER_H - 1, PLAYER_W + 2, 2);

            // Body.
            g.setColor(48, 91, 147);
            g.fillRect(x + 2, y + 5, 8, 8);

            // Head.
            g.setColor(219, 173, 125);
            g.fillRect(x + 3, y + 1, 7, 6);

            // Hair.
            g.setColor(47, 35, 26);
            g.fillRect(x + 3, y, 7, 2);

            // Arms and legs animate while moving.
            g.setColor(219, 173, 125);

            if (climbing) {
                int armOffset = (frame / 3) % 2;
                g.drawLine(x + 3, y + 6, x + armOffset, y + 2);
                g.drawLine(x + 9, y + 6, x + 11, y + 2 + armOffset);
                g.drawLine(x + 4, y + 12, x + 2, y + 15);
                g.drawLine(x + 8, y + 12, x + 10, y + 15);
            } else {
                g.drawLine(x + 3, y + 7, x, y + 10);
                g.drawLine(x + 9, y + 7, x + 11, y + 10);
                g.drawLine(x + 4, y + 12, x + 2, y + 15);
                g.drawLine(x + 8, y + 12, x + 10, y + 15);
            }

            g.setColor(255, 255, 255);
            g.fillRect(x + 7, y + 3, 1, 1);
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
                "Press 5 to return",
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
