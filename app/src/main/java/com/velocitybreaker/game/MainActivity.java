package com.velocitybreaker.game;

import android.app.Activity;
import android.os.Bundle;
import android.view.WindowManager;

public class MainActivity extends Activity {

    private RunnerGameView gameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        );

        gameView = new RunnerGameView(this);
        setContentView(gameView);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (gameView != null) {
            gameView.resumeGame();
        }
    }

    @Override
    protected void onPause() {
        if (gameView != null) {
            gameView.pauseGame();
        }

        super.onPause();
    }
}
