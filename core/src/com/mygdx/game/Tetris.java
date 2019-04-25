package com.mygdx.game;

import java.util.Date;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class Tetris extends ApplicationAdapter {
	TetrisBoard tetris1;
	TetrisBoard tetris2;

	SpriteBatch batch;

	int speed = 1;

	Controler controler1 = new CPAI(true);
	Controler controler2 = new CPAI(false);

	public final static String DATE = new Date().toString().replace(":", "");

	@Override
	public void create() {
		System.out.println(DATE);

		batch = new SpriteBatch();

		tetris1 = new TetrisBoard(80, 10, controler1);
		tetris2 = new TetrisBoard(400, 10, controler2);

	}

	@Override
	public void render() {
		if (Gdx.input.isKeyJustPressed(Keys.NUM_1))
			speed = 1;
		if (Gdx.input.isKeyJustPressed(Keys.NUM_2))
			speed = 5;
		if (Gdx.input.isKeyJustPressed(Keys.NUM_3))
			speed = 10;
		if (Gdx.input.isKeyJustPressed(Keys.NUM_4))
			speed = 50;
		if (Gdx.input.isKeyJustPressed(Keys.NUM_5))
			speed = 100;
		if (Gdx.input.isKeyJustPressed(Keys.NUM_6))
			speed = 500;
		if (Gdx.input.isKeyJustPressed(Keys.NUM_7))
			speed = 1000;
		if (Gdx.input.isKeyJustPressed(Keys.NUM_8))
			speed = 5000;
		if (Gdx.input.isKeyJustPressed(Keys.NUM_9))
			speed = 10000;
		if (Gdx.input.isKeyJustPressed(Keys.NUM_0))
			speed = 0;
		if (Gdx.input.isKeyJustPressed(Keys.Q))
			controler1 = new Player();
		if (Gdx.input.isKeyJustPressed(Keys.W))
			controler1 = new CPAI(true);
		if (Gdx.input.isKeyJustPressed(Keys.E))
			controler1 = new CPAI(false);

		for (int i = 0; i < speed; i++) {
			if (tetris1.enemy == null)
				tetris1.setEnemy(tetris2);
			if (tetris2.enemy == null)
				tetris2.setEnemy(tetris1);

			if (tetris1.isGameover()) {
				tetris2.setGameOver(!tetris1.isWin());

				tetris1 = new TetrisBoard(80, 10, controler1);
				tetris2 = new TetrisBoard(400, 10, controler2);

			} else if (tetris2.isGameover()) {
				tetris1.setGameOver(!tetris2.isWin());

				tetris1 = new TetrisBoard(80, 10, controler1);
				tetris2 = new TetrisBoard(400, 10, controler2);

			} else {
				tetris1.tick();
				tetris2.tick();
			}
		}

		Gdx.gl.glClearColor(1, 1, 1, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		batch.begin();
		tetris1.draw(batch);
		tetris2.draw(batch);
		batch.end();
	}

	@Override
	public void dispose() {
		batch.dispose();
	}

}
