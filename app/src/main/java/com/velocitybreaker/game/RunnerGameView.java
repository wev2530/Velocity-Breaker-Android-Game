package com.velocitybreaker.game;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class RunnerGameView extends View {

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random random = new Random();
    private final ArrayList<Item> items = new ArrayList<>();
    private final SharedPreferences prefs;

    private int lane = 1;
    private int coins;
    private int diamonds;
    private int best;
    private int score;

    private float distanceTravelled;
    private float speed = 420f;
    private float spawnTimer;
    private float jumpTimer;
    private float shieldTimer;

    private float lastX;
    private float lastY;
    private float downX;
    private float downY;
    private float lastFrameTime;

    private boolean gameOver;
    private boolean paused = true;
    private boolean saveMeUsed;
    private boolean running = true;

    private int runCoins;
    private int runDiamonds;

    private final int roadColor = Color.rgb(40, 43, 49);
    private final int red = Color.rgb(245, 55, 65);
    private final int gold = Color.rgb(255, 205, 35);
    private final int cyan = Color.rgb(30, 220, 255);

    private static class Item {
        float y;
        int lane;
        int type;
        boolean collected;

        Item(float y, int lane, int type) {
            this.y = y;
            this.lane = lane;
            this.type = type;
        }
    }

    public RunnerGameView(Context context) {
        super(context);

        prefs = context.getSharedPreferences(
            "velocity_breaker_data", Context.MODE_PRIVATE
        );

        coins = prefs.getInt("coins", 0);
        diamonds = prefs.getInt("diamonds", 0);
        best = prefs.getInt("best", 0);

        setFocusable(true);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    public void resumeGame() {
        paused = false;
        lastFrameTime = 0;
        invalidate();
    }

    public void pauseGame() {
        paused = true;
    }

    private float w() {
        return getWidth();
    }

    private float h() {
        return getHeight();
    }

    private float roadLeft() {
        return w() * 0.12f;
    }

    private float roadRight() {
        return w() * 0.88f;
    }

    private float laneWidth() {
        return (roadRight() - roadLeft()) / 3f;
    }

    private float laneX(int number) {
        return roadLeft() + laneWidth() * (number + 0.5f);
    }

    private void color(int c) {
        p.setColor(c);
        p.setStyle(Paint.Style.FILL);
    }

    private void text(Canvas c, String value, float x, float y,
                      float size, int color) {
        p.setColor(color);
        p.setTextSize(size);
        p.setTypeface(android.graphics.Typeface.create(
            "sans-serif-condensed", android.graphics.Typeface.BOLD
        ));
        p.setTextAlign(Paint.Align.LEFT);
        c.drawText(value, x, y, p);
    }

    private void centered(Canvas c, String value, float x, float y,
                          float size, int color) {
        p.setColor(color);
        p.setTextSize(size);
        p.setTypeface(android.graphics.Typeface.create(
            "sans-serif-condensed", android.graphics.Typeface.BOLD
        ));
        p.setTextAlign(Paint.Align.CENTER);
        c.drawText(value, x, y, p);
        p.setTextAlign(Paint.Align.LEFT);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float now = android.os.SystemClock.uptimeMillis();
        float dt = lastFrameTime == 0 ? 0 :
            Math.min((now - lastFrameTime) / 1000f, 0.04f);
        lastFrameTime = now;

        if (!paused && !gameOver) {
            updateGame(dt);
        }

        drawWorld(canvas);
        drawHUD(canvas);

        if (gameOver) {
            drawGameOver(canvas);
        }

        if (!paused && !gameOver) {
            postInvalidateDelayed(16);
        }
    }

    private void updateGame(float dt) {
        distanceTravelled += speed * dt;
        score = (int) (distanceTravelled / 18f);
        speed = Math.min(850f, 420f + score * 0.45f);

        if (jumpTimer > 0) jumpTimer -= dt;
        if (shieldTimer > 0) shieldTimer -= dt;

        spawnTimer -= dt;

        if (spawnTimer <= 0) {
            spawnItems();
            spawnTimer = Math.max(0.48f, 0.95f - score / 2500f);
        }

        float playerY = h() * 0.77f;
        float hitRange = Math.max(30f, h() * 0.045f);

        Iterator<Item> iterator = items.iterator();

        while (iterator.hasNext()) {
            Item item = iterator.next();
            item.y += speed * dt;

            if (item.y > h() + 80) {
                iterator.remove();
                continue;
            }

            if (item.collected || item.lane != lane) {
                continue;
            }

            if (Math.abs(item.y - playerY) < hitRange) {
                if (item.type == 0) {
                    runCoins++;
                    coins++;
                    item.collected = true;
                    saveProgress();
                } else if (item.type == 1) {
                    runDiamonds++;
                    diamonds++;
                    item.collected = true;
                    saveProgress();
                } else if (item.type == 2) {
                    if (jumpTimer <= 0.15f && shieldTimer <= 0) {
                        endRun();
                    }
                }
            }
        }

        if (score > best) {
            best = score;
            saveProgress();
        }
    }

    private void spawnItems() {
        int obstacleLane = random.nextInt(3);

        items.add(new Item(-50, obstacleLane, 2));

        int rewardLane = random.nextInt(3);
        if (rewardLane == obstacleLane) {
            rewardLane = (rewardLane + 1 + random.nextInt(2)) % 3;
        }

        int rewardType = random.nextInt(5) == 0 ? 1 : 0;
        items.add(new Item(-100, rewardLane, rewardType));

        if (random.nextBoolean()) {
            int extraLane = random.nextInt(3);
            if (extraLane != obstacleLane && extraLane != rewardLane) {
                items.add(new Item(-150, extraLane, 0));
            }
        }
    }

    private void saveProgress() {
        prefs.edit()
            .putInt("coins", coins)
            .putInt("diamonds", diamonds)
            .putInt("best", best)
            .apply();
    }

    private void endRun() {
        gameOver = true;
        saveProgress();
    }

    private void resetRun() {
        items.clear();
        lane = 1;
        score = 0;
        distanceTravelled = 0;
        speed = 420f;
        spawnTimer = 0.3f;
        jumpTimer = 0;
        shieldTimer = 0;
        runCoins = 0;
        runDiamonds = 0;
        saveMeUsed = false;
        gameOver = false;
        lastFrameTime = 0;
        invalidate();
    }

    private void drawWorld(Canvas c) {
        float width = w();
        float height = h();

        c.drawColor(Color.rgb(13, 25, 34));

        color(Color.rgb(15, 75, 59));
        c.drawRect(0, 0, roadLeft(), height, p);
        c.drawRect(roadRight(), 0, width, height, p);

        color(roadColor);
        c.drawRect(roadLeft(), 0, roadRight(), height, p);

        color(Color.rgb(70, 76, 83));
        c.drawRect(roadLeft(), 0, roadLeft() + 7, height, p);
        c.drawRect(roadRight() - 7, 0, roadRight(), height, p);

        float dashHeight = height * 0.055f;
        float gap = dashHeight * 0.8f;
        float offset = (distanceTravelled % (dashHeight + gap));

        color(Color.rgb(185, 195, 205));
        for (int i = -1; i < 15; i++) {
            float y = i * (dashHeight + gap) + offset;
            for (int separator = 1; separator <= 2; separator++) {
                float x = roadLeft() + laneWidth() * separator;
                c.drawRoundRect(
                    x - 3, y, x + 3, y + dashHeight,
                    4, 4, p
                );
            }
        }

        for (Item item : items) {
            if (item.collected) continue;

            float x = laneX(item.lane);
            float y = item.y;
            float scale = 0.75f + 0.25f * (y / Math.max(1, height));

            if (item.type == 0) {
                color(gold);
                c.drawCircle(x, y, 14 * scale, p);
                color(Color.rgb(255, 244, 155));
                c.drawCircle(x, y, 8 * scale, p);
                centered(c, "$", x, y + 5 * scale, 13 * scale,
                    Color.rgb(115, 75, 0));
            } else if (item.type == 1) {
                color(cyan);
                PathHelper.drawDiamond(c, p, x, y, 19 * scale);
                color(Color.WHITE);
                c.drawCircle(x - 4 * scale, y - 4 * scale,
                    3 * scale, p);
            } else {
                float size = 24 * scale;

                color(Color.rgb(110, 30, 35));
                c.drawRoundRect(
                    x - size, y - size, x + size, y + size,
                    5, 5, p
                );

                color(red);
                c.drawRoundRect(
                    x - size + 5, y - size + 5,
                    x + size - 5, y + size - 5,
                    4, 4, p
                );

                color(Color.rgb(255, 145, 145));
                c.drawRect(x - 4, y - size + 8,
                    x + 4, y + size - 8, p);
            }
        }

        drawPlayer(c);
    }

    private void drawPlayer(Canvas c) {
        float x = laneX(lane);
        float y = h() * 0.77f;

        if (jumpTimer > 0) {
            y -= (float) Math.sin(
                Math.min(1, jumpTimer / 0.65f) * Math.PI
            ) * h() * 0.12f;
        }

        if (shieldTimer > 0) {
            p.setColor(Color.argb(100, 30, 220, 255));
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(5);
            c.drawCircle(x, y - 15, 43, p);
            p.setStyle(Paint.Style.FILL);
        }

        color(Color.argb(90, 0, 0, 0));
        c.drawOval(x - 27, y + 22, x + 27, y + 38, p);

        color(Color.rgb(30, 30, 38));
        c.drawRoundRect(x - 22, y - 4, x - 5, y + 32,
            7, 7, p);
        c.drawRoundRect(x + 5, y - 4, x + 22, y + 32,
            7, 7, p);

        color(Color.rgb(235, 55, 65));
        c.drawRoundRect(x - 25, y - 40, x + 25, y + 8,
            10, 10, p);

        color(Color.rgb(255, 210, 165));
        c.drawCircle(x, y - 52, 18, p);

        color(Color.rgb(35, 35, 45));
        c.drawArc(x - 19, y - 71, x + 19, y - 36,
            180, 180, true, p);

        color(Color.WHITE);
        c.drawCircle(x - 6, y - 53, 3, p);
        c.drawCircle(x + 6, y - 53, 3, p);

        if (shieldTimer > 0) {
            centered(c, "SHIELD", x, y - 90, 17, cyan);
        }
    }

    private void drawHUD(Canvas c) {
        float pad = w() * 0.045f;
        float size = Math.max(17, w() * 0.045f);

        text(c, "VELOCITY BREAKER", pad, h() * 0.055f,
            size * 1.15f, Color.WHITE);

        text(c, "SCORE  " + score, pad, h() * 0.095f,
            size, gold);

        text(c, "BEST  " + best, pad, h() * 0.13f,
            size * 0.85f, Color.LTGRAY);

        p.setTextAlign(Paint.Align.RIGHT);
        text(c, "COINS " + coins, w() - pad, h() * 0.075f,
            size, gold);
        p.setTextAlign(Paint.Align.LEFT);

        p.setTextAlign(Paint.Align.RIGHT);
        text(c, "DIAMONDS " + diamonds, w() - pad,
            h() * 0.11f, size * 0.82f, cyan);
        p.setTextAlign(Paint.Align.LEFT);
    }

    private void drawGameOver(Canvas c) {
        p.setColor(Color.argb(205, 5, 10, 18));
        c.drawRect(0, 0, w(), h(), p);

        centered(c, "RUN OVER", w() / 2f, h() * 0.32f,
            w() * 0.105f, red);

        centered(c, "Score: " + score, w() / 2f,
            h() * 0.40f, w() * 0.06f, Color.WHITE);

        centered(c, "Coins collected: " + runCoins,
            w() / 2f, h() * 0.45f, w() * 0.045f, gold);

        centered(c, "Diamonds collected: " + runDiamonds,
            w() / 2f, h() * 0.49f, w() * 0.045f, cyan);

        float buttonLeft = w() * 0.18f;
        float buttonRight = w() * 0.82f;
        float buttonTop = h() * 0.56f;
        float buttonBottom = h() * 0.64f;

        color(saveMeUsed ? Color.GRAY : Color.rgb(25, 145, 100));
        c.drawRoundRect(buttonLeft, buttonTop, buttonRight,
            buttonBottom, 18, 18, p);

        centered(c, saveMeUsed ? "SAVE ME USED" : "SAVE ME  -  FREE",
            w() / 2f, h() * 0.61f, w() * 0.052f,
            Color.WHITE);

        float restartTop = h() * 0.69f;
        float restartBottom = h() * 0.77f;

        color(Color.rgb(190, 38, 50));
        c.drawRoundRect(buttonLeft, restartTop, buttonRight,
            restartBottom, 18, 18, p);

        centered(c, "PLAY AGAIN", w() / 2f,
            h() * 0.74f, w() * 0.06f, Color.WHITE);

        centered(c, "Swipe left/right to change lanes",
            w() / 2f, h() * 0.86f, w() * 0.038f,
            Color.LTGRAY);

        centered(c, "Swipe up to jump over obstacles",
            w() / 2f, h() * 0.90f, w() * 0.038f,
            Color.LTGRAY);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            downX = x;
            downY = y;
            lastX = x;
            lastY = y;
            return true;
        }

        if (event.getAction() == MotionEvent.ACTION_UP) {
            float dx = x - downX;
            float dy = y - downY;

            if (gameOver) {
                if (y > h() * 0.55f && y < h() * 0.65f) {
                    if (!saveMeUsed) {
                        saveMeUsed = true;
                        gameOver = false;
                        shieldTimer = 1.8f;

                        Iterator<Item> iterator = items.iterator();
                        while (iterator.hasNext()) {
                            Item item = iterator.next();
                            if (item.type == 2 &&
                                item.lane == lane &&
                                Math.abs(item.y - h() * 0.77f)
                                    < h() * 0.18f) {
                                iterator.remove();
                            }
                        }

                        lastFrameTime = 0;
                        invalidate();
                    }
                } else if (y > h() * 0.67f && y < h() * 0.80f) {
                    resetRun();
                } else {
                    resetRun();
                }

                return true;
            }

            if (Math.abs(dx) > 45 && Math.abs(dx) > Math.abs(dy)) {
                if (dx < 0) {
                    lane = Math.max(0, lane - 1);
                } else {
                    lane = Math.min(2, lane + 1);
                }
            } else if (dy < -45 && Math.abs(dy) > Math.abs(dx)) {
                jumpTimer = 0.65f;
            }

            invalidate();
            return true;
        }

        return true;
    }

    private static class PathHelper {
        static void drawDiamond(Canvas c, Paint p,
                                float x, float y, float size) {
            android.graphics.Path path = new android.graphics.Path();
            path.moveTo(x, y - size);
            path.lineTo(x + size * 0.72f, y);
            path.lineTo(x, y + size);
            path.lineTo(x - size * 0.72f, y);
            path.close();
            p.setStyle(Paint.Style.FILL);
            c.drawPath(path, p);
        }
    }
}
