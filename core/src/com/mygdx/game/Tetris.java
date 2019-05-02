package com.mygdx.game;

import java.util.Date;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;

public class Tetris extends ApplicationAdapter {
	FreeTypeFontGenerator fontGenerator;
BitmapFont bitmapFont;

	TetrisBoard tetris1;
	TetrisBoard tetris2;

	SpriteBatch batch;

	TetrisThread thread;

	 Controler controler1 = new Player();
//	Controler controler1 = new CPAI(true, false);
	Controler controler2 = new CPAI(false, true);

	public final static String DATE = new Date().toString().replace(":", "");

	@Override
	public void create() {
		System.out.println(DATE);

		batch = new SpriteBatch();

        FileHandle file = Gdx.files.local("font/SNslit.ttf");
        fontGenerator = new FreeTypeFontGenerator(file);
        FreeTypeFontGenerator.FreeTypeFontParameter param = new FreeTypeFontGenerator.FreeTypeFontParameter();
        param.size = 20;
        param.color= Color.BLACK;
        bitmapFont = fontGenerator.generateFont(param);

		tetris1 = new TetrisBoard(80, 10, controler1);
		tetris2 = new TetrisBoard(400, 10, controler2);

		thread = new TetrisThread();
//		thread.start();
	}

	@Override
	public void render() {
		if (Gdx.input.isKeyJustPressed(Keys.Q))
			controler1 = new Player();
		if (Gdx.input.isKeyJustPressed(Keys.W))
			controler1 = new CPAI(true, false);
		if (Gdx.input.isKeyJustPressed(Keys.E))
			controler1 = new CPAI(false, true);
		if (Gdx.input.isKeyJustPressed(Keys.P))
			controler2.save();
		if (Gdx.input.isKeyJustPressed(Keys.T)) {
			thread.setRun(false);
			thread.interrupt();
			thread = new TetrisThread();
			tetris1 = new TetrisBoard(80, 10, controler1);
			tetris2 = new TetrisBoard(400, 10, controler2);
		}
		if (Gdx.input.isKeyJustPressed(Keys.SPACE)) {
			if (thread.isAlive()) {
				thread.setRun(false);
			} else {
				thread = new TetrisThread();
				thread.start();
			}
		}
		if(Gdx.input.isKeyJustPressed(Keys.ENTER)){
			tetris1 = new TetrisBoard(80, 10, controler1);
			tetris2 = new TetrisBoard(400, 10, controler2);
		}

		if (!thread.isAlive()) {
			thread.process();
		}

		Gdx.gl.glClearColor(0.6f, 0.6f, 0.8f, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		batch.begin();
		try {
			tetris1.draw(batch,bitmapFont);
			tetris2.draw(batch,bitmapFont);
		} catch (Exception e) {
			System.err.println("Something Drawing Error");
		}
		batch.end();
	}

	@Override
	public void dispose() {
		batch.dispose();
	}

	class TetrisThread extends Thread {
		private boolean run;

		@Override
		public void run() {
			setRun(true);
			while (isRun()) {
				process();
			}
		}

		private void process() {
			if(tetris1.isGameover() || tetris2.isGameover())
				return;

			if (tetris1.enemy == null)
				tetris1.setEnemy(tetris2);
			if (tetris2.enemy == null)
				tetris2.setEnemy(tetris1);

			tetris1.tick();
			if (tetris1.isGameover()) {
				tetris2.setGameOver(!tetris1.isWin());
				return;
			}
			tetris2.tick();
			if (tetris2.isGameover()) {
				tetris1.setGameOver(!tetris2.isWin());
				return;
			}
		}

		public boolean isRun() {
			return run;
		}

		public void setRun(boolean run) {
			this.run = run;
		}
	}
}
