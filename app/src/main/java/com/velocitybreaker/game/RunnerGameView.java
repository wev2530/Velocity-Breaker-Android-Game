
package com.velocitybreaker.game;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class RunnerGameView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random random = new Random();
    private final ArrayList<WorldItem> items = new ArrayList<>();
    private final SharedPreferences prefs;

    private int lane = 1;
    private int coins;
    private int diamonds;
    private int best;
    private int score;
    private int runCoins;
    private int runDiamonds;

    private float speed = 0.34f;
    private float spawnTimer = 0.8f;
    private float jumpTimer;
    private float slideTimer;
    private float distance;
    private float lastFrame;
    private float downX;
    private float downY;

    private boolean gameOver;
    private boolean paused = true;

    private static final int COIN = 0;
    private static final int DIAMOND = 1;
    private static final int BLOCK = 2;
    private static final int OVERHEAD = 3;

    private static final int WHITE = Color.WHITE;
    private static final int GOLD = Color.rgb(255, 207, 54);
    private static final int CYAN = Color.rgb(58, 231, 255);
    private static final int RED = Color.rgb(255, 66, 78);

    private static class WorldItem {
        int lane;
        int type;
        float depth;
        boolean collected;

        WorldItem(int lane, int type, float depth) {
            this.lane = lane;
            this.type = type;
            this.depth = depth;
        }
    }

    public RunnerGameView(Context context) {
        super(context);

        prefs = context.getSharedPreferences(
                "velocity_breaker_data",
                Context.MODE_PRIVATE
        );

        coins = prefs.getInt("coins", 0);
        diamonds = prefs.getInt("diamonds", 0);
        best = prefs.getInt("best", 0);

        setFocusable(true);
    }

    public void resumeGame() {
        paused = false;
        lastFrame = 0;
        postInvalidateOnAnimation();
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

    private float horizon() {
        return h() * 0.27f;
    }

    private float roadBottomLeft() {
        return w() * 0.035f;
    }

    private float roadBottomRight() {
        return w() * 0.965f;
    }

    private float roadTopHalf() {
        return w() * 0.105f;
    }

    private float roadCenter() {
        return w() * 0.5f;
    }

    private float roadLeftAt(float depth) {
        return roadCenter()
                - roadTopHalf()
                - (roadCenter() - roadBottomLeft() - roadTopHalf())
                * depth;
    }

    private float roadRightAt(float depth) {
        return roadCenter()
                + roadTopHalf()
                + (roadBottomRight() - roadCenter() - roadTopHalf())
                * depth;
    }

    private float roadY(float depth) {
        return horizon() + (h() * 0.94f - horizon()) * depth;
    }

    private float laneX(int number, float depth) {
        float left = roadLeftAt(depth);
        float right = roadRightAt(depth);
        return left + (right - left) * (number + 0.5f) / 3f;
    }

    private float scaleAt(float depth) {
        return 0.16f + depth * 1.1f;
    }

    private void fill(int color) {
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(null);
        paint.setStrokeWidth(1);
    }

    private void polygon(Canvas c, int color, float... points) {
        Path path = new Path();
        path.moveTo(points[0], points[1]);

        for (int i = 2; i < points.length; i += 2) {
            path.lineTo(points[i], points[i + 1]);
        }

        path.close();
        fill(color);
        c.drawPath(path, paint);
    }

    private void text(Canvas c, String value, float x, float y,
                      float size, int color, Paint.Align align) {
        fill(color);
        paint.setTextSize(size);
        paint.setTypeface(Typeface.create(
                "sans-serif-condensed", Typeface.BOLD
        ));
        paint.setTextAlign(align);
        c.drawText(value, x, y, paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float now = android.os.SystemClock.uptimeMillis();
        float dt = lastFrame == 0
                ? 0
                : Math.min((now - lastFrame) / 1000f, 0.04f);
        lastFrame = now;

        if (!paused && !gameOver) {
            updateGame(dt);
        }

        drawWorld(canvas);
        drawItems(canvas);
        drawPlayer(canvas);
        drawHud(canvas);

        if (gameOver) {
            drawGameOver(canvas);
        }

        if (!paused && !gameOver) {
            postInvalidateOnAnimation();
        }
    }

    private void updateGame(float dt) {
        distance += dt * speed;
        score = (int) (distance * 100);

        speed = Math.min(0.72f, 0.34f + score / 16000f);

        if (jumpTimer > 0) {
            jumpTimer = Math.max(0, jumpTimer - dt);
        }

        if (slideTimer > 0) {
            slideTimer = Math.max(0, slideTimer - dt);
        }

        spawnTimer -= dt;

        if (spawnTimer <= 0) {
            spawnItems();
            spawnTimer = Math.max(0.48f, 1.0f - score / 12000f);
        }

        Iterator<WorldItem> iterator = items.iterator();

        while (iterator.hasNext()) {
            WorldItem item = iterator.next();
            item.depth += speed * dt;

            if (item.depth > 1.12f) {
                iterator.remove();
                continue;
            }

            if (item.collected || item.lane != lane) {
                continue;
            }

            if (item.depth >= 0.88f && item.depth <= 1.02f) {
                if (item.type == COIN) {
                    coins++;
                    runCoins++;
                    item.collected = true;
                    saveProgress();
                } else if (item.type == DIAMOND) {
                    diamonds++;
                    runDiamonds++;
                    item.collected = true;
                    saveProgress();
                } else if (item.type == BLOCK) {
                    if (jumpTimer <= 0.05f) {
                        endRun();
                    }
                } else if (item.type == OVERHEAD) {
                    if (slideTimer <= 0.05f) {
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
        int obstacleType = random.nextInt(4) == 0
                ? OVERHEAD : BLOCK;

        items.add(new WorldItem(
                obstacleLane, obstacleType, 0.02f
        ));

        int rewardLane = random.nextInt(3);

        if (rewardLane == obstacleLane) {
            rewardLane = (obstacleLane + 1 + random.nextInt(2)) % 3;
        }

        int rewardType = random.nextInt(6) == 0
                ? DIAMOND : COIN;

        items.add(new WorldItem(
                rewardLane, rewardType, 0.0f
        ));

        if (random.nextBoolean()) {
            int thirdLane = 3 - obstacleLane - rewardLane;
            items.add(new WorldItem(
                    thirdLane, COIN, -0.10f
            ));
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
        distance = 0;
        speed = 0.34f;
        spawnTimer = 0.6f;
        jumpTimer = 0;
        slideTimer = 0;
        runCoins = 0;
        runDiamonds = 0;
        gameOver = false;
        lastFrame = 0;
        paused = false;
        postInvalidateOnAnimation();
    }

    private void drawWorld(Canvas c) {
        float width = w();
        float height = h();
        float horizon = horizon();

        // Sky gradient
        fill(Color.rgb(9, 19, 45));
        paint.setShader(new android.graphics.LinearGradient(
                0, 0, 0, horizon,
                Color.rgb(12, 29, 65),
                Color.rgb(69, 104, 146),
                android.graphics.Shader.TileMode.CLAMP
        ));
        c.drawRect(0, 0, width, horizon + 2, paint);
        paint.setShader(null);

        // Distant glow
        fill(Color.rgb(122, 161, 194));
        c.drawCircle(width * 0.78f, horizon * 0.54f,
                width * 0.055f, paint);

        // Distant mountain silhouettes
        polygon(c, Color.rgb(29, 58, 75),
                0, horizon,
                width * 0.12f, horizon * 0.57f,
                width * 0.25f, horizon * 0.83f,
                width * 0.39f, horizon * 0.45f,
                width * 0.55f, horizon * 0.88f,
                width * 0.71f, horizon * 0.60f,
                width * 0.87f, horizon * 0.78f,
                width, horizon * 0.51f,
                width, horizon,
                0, horizon
        );

        // Ground
        fill(Color.rgb(19, 82, 65));
        c.drawRect(0, horizon, width, height, paint);

        // Road shoulders
        polygon(c, Color.rgb(106, 115, 122),
                roadCenter() - roadTopHalf() - width * 0.012f,
                horizon,
                roadCenter() + roadTopHalf() + width * 0.012f,
                horizon,
                roadBottomRight() + width * 0.018f,
                height,
                roadBottomLeft() - width * 0.018f,
                height
        );

        // Main road
        polygon(c, Color.rgb(37, 43, 55),
                roadCenter() - roadTopHalf(), horizon,
                roadCenter() + roadTopHalf(), horizon,
                roadBottomRight(), height,
                roadBottomLeft(), height
        );

        // Road edge highlights
        polygon(c, Color.rgb(194, 207, 218),
                roadCenter() - roadTopHalf(), horizon,
                roadCenter() - roadTopHalf() + width * 0.004f, horizon,
                roadBottomLeft() + width * 0.006f, height,
                roadBottomLeft(), height
        );

        polygon(c, Color.rgb(194, 207, 218),
                roadCenter() + roadTopHalf() - width * 0.004f, horizon,
                roadCenter() + roadTopHalf(), horizon,
                roadBottomRight(), height,
                roadBottomRight() - width * 0.006f, height
        );

        // Three-lane markings, shrinking toward the horizon
        float phase = (distance * 1.6f) % 0.16f;

        for (int divider = 1; divider <= 2; divider++) {
            for (int i = 0; i < 12; i++) {
                float depth = (i * 0.105f + phase) % 1f;

                if (depth < 0.035f) {
                    continue;
                }

                float y = roadY(depth);
                float nextY = roadY(Math.min(1f, depth + 0.045f));
                float x = roadLeftAt(depth)
                        + (roadRightAt(depth) - roadLeftAt(depth))
                        * divider / 3f;
                float thickness = 0.8f + depth * width * 0.008f;

                fill(Color.rgb(183, 198, 213));
                c.drawRoundRect(
                        x - thickness, y,
                        x + thickness, nextY,
                        thickness, thickness, paint
                );
            }
        }

        drawScenery(c);
    }

    private void drawScenery(Canvas c) {
        // Repeating stylized trees and roadside lights
        float phase = (distance * 1.7f) % 0.20f;

        for (int i = 0; i < 8; i++) {
            float depth = (i * 0.145f + phase) % 1f;
            float y = roadY(depth);
            float scale = scaleAt(depth);

            for (int side = 0; side < 2; side++) {
                float x = side == 0
                        ? roadLeftAt(depth) - w() * 0.10f * scale
                        : roadRightAt(depth) + w() * 0.10f * scale;

                float trunkW = w() * 0.009f * scale;
                float trunkH = h() * 0.085f * scale;

                fill(Color.rgb(83, 55, 39));
                c.drawRect(
                        x - trunkW, y - trunkH * 0.18f,
                        x + trunkW, y + trunkH, paint
                );

                fill(Color.rgb(17, 86, 66));
                c.drawCircle(x, y - trunkH * 0.75f,
                        w() * 0.035f * scale, paint);

                fill(Color.rgb(25, 113, 77));
                c.drawCircle(x - trunkW * 1.4f,
                        y - trunkH * 0.92f,
                        w() * 0.026f * scale, paint);

                fill(Color.rgb(40, 139, 91));
                c.drawCircle(x + trunkW * 1.2f,
                        y - trunkH * 0.92f,
                        w() * 0.023f * scale, paint);
            }
        }
    }

    private void drawItems(Canvas c) {
        for (WorldItem item : items) {
            if (item.collected || item.depth < 0) {
                continue;
            }

            float depth = Math.min(1.15f, item.depth);
            float x = laneX(item.lane, Math.min(1f, depth));
            float y = roadY(Math.min(1f, depth));
            float s = scaleAt(Math.min(1f, depth));

            // Ground shadow
            fill(Color.argb(80, 0, 0, 0));
            c.drawOval(
                    x - 23 * s, y - 3 * s,
                    x + 23 * s, y + 8 * s, paint
            );

            if (item.type == COIN) {
                fill(Color.rgb(191, 125, 18));
                c.drawCircle(x, y - 23 * s, 13 * s, paint);

                fill(GOLD);
                c.drawCircle(x, y - 23 * s, 10 * s, paint);

                fill(Color.rgb(255, 239, 151));
                c.drawCircle(x - 3 * s, y - 27 * s,
                        3 * s, paint);

                text(c, "V", x, y - 19 * s,
                        11 * s, Color.rgb(125, 76, 6),
                        Paint.Align.CENTER);

            } else if (item.type == DIAMOND) {
                float size = 17 * s;

                polygon(c, Color.rgb(27, 137, 208),
                        x, y - 46 * s,
                        x + size, y - 28 * s,
                        x, y - 9 * s,
                        x - size, y - 28 * s
                );

                polygon(c, CYAN,
                        x, y - 42 * s,
                        x + size * 0.6f, y - 28 * s,
                        x, y - 13 * s,
                        x - size * 0.6f, y - 28 * s
                );

                fill(WHITE);
                c.drawCircle(x - 4 * s, y - 34 * s,
                        2.4f * s, paint);

            } else if (item.type == BLOCK) {
                float size = 25 * s;
                float top = y - 52 * s;

                fill(Color.rgb(88, 27, 40));
                c.drawRoundRect(
                        x - size, top,
                        x + size, y,
                        5 * s, 5 * s, paint
                );

                polygon(c, Color.rgb(237, 58, 75),
                        x - size, top + 7 * s,
                        x + size, top + 7 * s,
                        x + size * 0.74f, y - 7 * s,
                        x - size * 0.74f, y - 7 * s
                );

                fill(Color.rgb(255, 150, 153));
                c.drawRect(
                        x - 3 * s, top + 11 * s,
                        x + 3 * s, y - 12 * s, paint
                );

            } else if (item.type == OVERHEAD) {
                float size = 30 * s;
                float top = y - 80 * s;

                fill(Color.rgb(66, 74, 91));
                c.drawRect(
                        x - size, top,
                        x + size, top + 13 * s, paint
                );

                fill(Color.rgb(255, 157, 53));
                c.drawRect(
                        x - size, top + 13 * s,
                        x + size, top + 20 * s, paint
                );

                text(c, "SLIDE", x, top + 10 * s,
                        8 * s, WHITE, Paint.Align.CENTER);
            }
        }
    }

    private void drawPlayer(Canvas c) {
        float x = laneX(lane, 1f);
        float baseY = h() * 0.83f;

        float jumpProgress = jumpTimer > 0
                ? jumpTimer / 0.68f : 0f;

        float jumpHeight = jumpTimer > 0
                ? (float) Math.sin(jumpProgress * Math.PI)
                    * h() * 0.15f
                : 0f;

        float y = baseY - jumpHeight;
        boolean sliding = slideTimer > 0;

        // Soft character shadow
        fill(Color.argb(90, 0, 0, 0));
        c.drawOval(
                x - w() * 0.052f, baseY - 5,
                x + w() * 0.052f, baseY + 12, paint
        );

        if (sliding) {
            drawSlidingPlayer(c, x, y);
            return;
        }

        float unit = Math.min(w(), h()) * 0.001f;
        float bodyW = w() * 0.058f;
        float bodyH = h() * 0.115f;
        float headR = w() * 0.027f;

        // Running animation
        float time = android.os.SystemClock.uptimeMillis() / 100f;
        float swing = (float) Math.sin(time) * 7f * unit;
        float legSwing = (float) Math.sin(time + Math.PI) * 9f * unit;

        // Legs
        fill(Color.rgb(24, 30, 43));

        c.drawRoundRect(
                x - bodyW * 0.47f, y - bodyH * 0.20f,
                x - bodyW * 0.06f + swing, y + bodyH * 0.34f,
                7 * unit, 7 * unit, paint
        );

        c.drawRoundRect(
                x + bodyW * 0.06f + legSwing, y - bodyH * 0.20f,
                x + bodyW * 0.47f, y + bodyH * 0.34f,
                7 * unit, 7 * unit, paint
        );

        // Boots
        fill(Color.rgb(8, 12, 23));
        c.drawRoundRect(
                x - bodyW * 0.55f + swing,
                y + bodyH * 0.22f,
                x - bodyW * 0.02f + swing,
                y + bodyH * 0.39f,
                5 * unit, 5 * unit, paint
        );

        c.drawRoundRect(
                x + bodyW * 0.02f + legSwing,
                y + bodyH * 0.22f,
                x + bodyW * 0.55f + legSwing,
                y + bodyH * 0.39f,
                5 * unit, 5 * unit, paint
        );

        // Arms
        fill(Color.rgb(31, 48, 74));
        c.drawRoundRect(
                x - bodyW * 0.68f, y - bodyH * 0.85f,
                x - bodyW * 0.39f, y - bodyH * 0.12f,
                8 * unit, 8 * unit, paint
        );

        c.drawRoundRect(
                x + bodyW * 0.39f, y - bodyH * 0.85f,
                x + bodyW * 0.68f, y - bodyH * 0.12f,
                8 * unit, 8 * unit, paint
        );

        // Jacket body with layered shading
        fill(Color.rgb(125, 30, 49));
        c.drawRoundRect(
                x - bodyW * 0.55f, y - bodyH,
                x + bodyW * 0.55f, y - bodyH * 0.13f,
                12 * unit, 12 * unit, paint
        );

        fill(Color.rgb(226, 49, 69));
        c.drawRoundRect(
                x - bodyW * 0.40f, y - bodyH * 0.95f,
                x + bodyW * 0.18f, y - bodyH * 0.18f,
                8 * unit, 8 * unit, paint
        );

        // Back emblem
        fill(Color.rgb(255, 207, 54));
        c.drawCircle(
                x, y - bodyH * 0.58f,
                bodyW * 0.12f, paint
        );

        // Neck
        fill(Color.rgb(205, 143, 113));
        c.drawRect(
                x - headR * 0.35f, y - bodyH - headR * 0.15f,
                x + headR * 0.35f, y - bodyH + headR * 0.18f,
                paint
        );

        // Head
        fill(Color.rgb(220, 164, 129));
        c.drawCircle(
                x, y - bodyH - headR * 0.70f,
                headR, paint
        );

        // Hair cap
        fill(Color.rgb(24, 27, 40));
        c.drawArc(
                x - headR * 1.05f,
                y - bodyH - headR * 1.75f,
                x + headR * 1.05f,
                y - bodyH - headR * 0.05f,
                180, 180, true, paint
        );

        // Hair tips at the back
        polygon(c, Color.rgb(24, 27, 40),
                x - headR * 0.92f, y - bodyH - headR * 0.70f,
                x - headR * 0.30f, y - bodyH - headR * 0.35f,
                x - headR * 0.55f, y - bodyH + headR * 0.30f,
                x - headR * 0.95f, y - bodyH + headR * 0.05f
        );

        polygon(c, Color.rgb(24, 27, 40),
                x + headR * 0.92f, y - bodyH - headR * 0.70f,
                x + headR * 0.30f, y - bodyH - headR * 0.35f,
                x + headR * 0.55f, y - bodyH + headR * 0.30f,
                x + headR * 0.95f, y - bodyH + headR * 0.05f
        );

        // Rim light around the character
        paint.setColor(Color.argb(110, 90, 219, 255));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(1.5f, unit * 2.2f));
        c.drawRoundRect(
                x - bodyW * 0.55f, y - bodyH,
                x + bodyW * 0.55f, y - bodyH * 0.13f,
                12 * unit, 12 * unit, paint
        );
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawSlidingPlayer(Canvas c, float x, float y) {
        float width = w() * 0.105f;
        float height = h() * 0.045f;

        // Low sliding silhouette
        fill(Color.rgb(20, 28, 43));
        c.drawRoundRect(
                x - width * 0.75f, y - height * 0.55f,
                x + width * 0.65f, y + height * 0.45f,
                height * 0.5f, height * 0.5f, paint
        );

        fill(Color.rgb(221, 48, 67));
        c.drawRoundRect(
                x - width * 0.40f, y - height * 0.75f,
                x + width * 0.38f, y + height * 0.12f,
                height * 0.3f, height * 0.3f, paint
        );

        fill(Color.rgb(220, 164, 129));
        c.drawCircle(
                x + width * 0.60f, y - height * 0.40f,
                height * 0.55f, paint
        );

        // Sliding energy trail
        fill(Color.argb(120, 58, 231, 255));
        c.drawRoundRect(
                x - width * 1.15f, y - height * 0.10f,
                x - width * 0.55f, y + height * 0.10f,
                height, height, paint
        );
    }

    private void drawHud(Canvas c) {
        float pad = w() * 0.045f;
        float size = Math.max(17, w() * 0.042f);

        text(c, "VELOCITY BREAKER",
                pad, h() * 0.055f, size * 1.1f,
                WHITE, Paint.Align.LEFT);

        text(c, "SCORE  " + score,
                pad, h() * 0.10f, size,
                GOLD, Paint.Align.LEFT);

        text(c, "BEST  " + best,
                pad, h() * 0.14f, size * 0.85f,
                Color.LTGRAY, Paint.Align.LEFT);

        text(c, "COINS " + coins,
                w() - pad, h() * 0.075f, size * 0.9f,
                GOLD, Paint.Align.RIGHT);

        text(c, "DIAMONDS " + diamonds,
                w() - pad, h() * 0.115f, size * 0.8f,
                CYAN, Paint.Align.RIGHT);

        // Control hint
        if (score < 120) {
            text(c, "SWIPE  LEFT / RIGHT",
                    w() * 0.5f, h() * 0.925f,
                    size * 0.7f, WHITE, Paint.Align.CENTER);
        }
    }

    private void drawGameOver(Canvas c) {
        fill(Color.argb(215, 5, 10, 22));
        c.drawRect(0, 0, w(), h(), paint);

        text(c, "RUN OVER",
                w() / 2f, h() * 0.33f,
                w() * 0.105f, RED, Paint.Align.CENTER);

        text(c, "Score: " + score,
                w() / 2f, h() * 0.41f,
                w() * 0.06f, WHITE, Paint.Align.CENTER);

        text(c, "Coins collected: " + runCoins,
                w() / 2f, h() * 0.47f,
                w() * 0.045f, GOLD, Paint.Align.CENTER);

        text(c, "Diamonds collected: " + runDiamonds,
                w() / 2f, h() * 0.52f,
                w() * 0.04f, CYAN, Paint.Align.CENTER);

        RectF button = new RectF(
                w() * 0.18f, h() * 0.61f,
                w() * 0.82f, h() * 0.71f
        );

        fill(Color.rgb(190, 38, 56));
        c.drawRoundRect(button, 20, 20, paint);

        text(c, "PLAY AGAIN",
                w() / 2f, h() * 0.675f,
                w() * 0.06f, WHITE, Paint.Align.CENTER);

        text(c, "Swipe up to jump",
                w() / 2f, h() * 0.81f,
                w() * 0.042f, Color.LTGRAY, Paint.Align.CENTER);

        text(c, "Swipe down to slide",
                w() / 2f, h() * 0.86f,
                w() * 0.042f, Color.LTGRAY, Paint.Align.CENTER);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            downX = x;
            downY = y;
            return true;
        }

        if (event.getAction() == MotionEvent.ACTION_UP) {
            float dx = x - downX;
            float dy = y - downY;

            if (gameOver) {
                resetRun();
                return true;
            }

            if (Math.abs(dx) > 42 && Math.abs(dx) > Math.abs(dy)) {
                if (dx < 0) {
                    lane = Math.max(0, lane - 1);
                } else {
                    lane = Math.min(2, lane + 1);
                }
            } else if (dy < -42 && Math.abs(dy) > Math.abs(dx)) {
                if (jumpTimer <= 0 && slideTimer <= 0) {
                    jumpTimer = 0.68f;
                }
            } else if (dy > 42 && Math.abs(dy) > Math.abs(dx)) {
                if (slideTimer <= 0 && jumpTimer <= 0) {
                    slideTimer = 0.65f;
                }
            }

            postInvalidateOnAnimation();
            return true;
        }

        return true;
    }
}
