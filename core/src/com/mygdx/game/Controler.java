package com.mygdx.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Input.Keys;

enum Control {
	Rslide, Lslide, Rrotate, Lrotate, HardDrop, Drop, Hold, Wait
}

public class Controler {
	public Control control(TetrisBoard tetrisBoard) {
		return Control.Wait;
	}

	public void refresh(TetrisBoard tetrisBoard, Tetrimino result) {

	}

	public void gameset(TetrisBoard tetrisBoard) {
		System.out.println(tetrisBoard);
	}

	public void appyEvaluater(Evaluater evaluater) {

	}

	public void init(TetrisBoard tetrisBoard) {
		System.out.println(tetrisBoard);
	}

	public void save() {

	}

}

class Player extends Controler {

	@Override
	public Control control(TetrisBoard tetrisBoard) {
		if (Gdx.input.isKeyJustPressed(Input.Keys.CONTROL_LEFT))
			return (Control.Hold);

		if (Gdx.input.isKeyJustPressed(Keys.UP))
			return (Control.HardDrop);
		if (Gdx.input.isKeyJustPressed(Keys.X))
			return (Control.Rrotate);
		if (Gdx.input.isKeyJustPressed(Keys.Z))
			return (Control.Lrotate);

		if (Gdx.input.isKeyPressed(Keys.LEFT) && Gdx.input.isKeyPressed(Keys.RIGHT)) {

		} else {
			if (Gdx.input.isKeyPressed(Keys.RIGHT))
				return (Control.Rslide);
			if (Gdx.input.isKeyPressed(Keys.LEFT))
				return (Control.Lslide);
		}
		if (Gdx.input.isKeyPressed(Keys.DOWN))
			return (Control.Drop);

		return super.control(tetrisBoard);
	}
}
