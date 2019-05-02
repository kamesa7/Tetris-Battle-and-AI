package com.mygdx.game;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class Tetrimino implements Cloneable {
	public static final int startX = TetrisBoard.WIDTH/2-2;
	public static final int startY = TetrisBoard.HEIGHT-1;

	public int posX, posY;
	public int state;
	public Mino type;

	private boolean onGround = false;
	public boolean isTspin = false;
	public boolean isTspinMini = false;

	private boolean spined = false;
	private boolean changed = false;

	@SuppressWarnings("unused")
	private final int birthTick;


	private int lastdrop;
	private int lasttouch;
	private int postponement = 0;
	private int stickwait = 0;

	/**
	 * 詳細設定されたミノ
	 * @param tetris
	 * @param mino
	 * @param posX
	 * @param posY
	 * @param state
	 */
	public Tetrimino(TetrisBoard tetris, Mino mino, int posX, int posY, int state) {
		this.posX = posX;
		this.posY = posY;
		this.type = mino;
		this.state = state;

		birthTick = tetris.getGametick();
		if (isCollide(tetris.board, state, posX, posY)) {
			setOnGround();
		}
	}

	/**
	 * 通常の新規ミノ
	 * @param tetris
	 * @param mino
	 */
	public Tetrimino(TetrisBoard tetris, Mino mino) {
		this.posX = startX;
		this.posY = startY;
		if (type == Mino.I) posY++;
		this.type = mino;
		this.state = 0;

		birthTick = tetris.getGametick();
		if (isCollide(tetris.board, state, posX, posY)) {
			setOnGround();
		}

	}

	/**
	 * 指定されたコントロールを行う
	 * changed = falseの後コントロール
	 * コントロール後にドロップするならドロップ
	 * @param tetris
	 * @param control
	 */
	public void tick(TetrisBoard tetris, Control control) {
		assert !isOnGround() : this;

		changed = false;

		control(tetris, control);

		if (tetris.getGametick() - lastdrop >= tetris.DROPTICK) {
			drop(tetris);
		}
	}

	/**
	 * 指定環境で衝突するか
	 * @param board　ボード
	 * @param state　状態
	 * @param posX　ｘ
	 * @param posY　ｙ
	 * @return　boolean
	 */
	public boolean isCollide(Mino[][] board, int state, int posX, int posY) {
		for (int y = 0; y < type.shape[state].length; y++) {
			for (int x = 0; x < type.shape[state][y].length; x++) {
				if (type.shape[state][y][x] == 1 && !isInner(posX + x, posY - y)) {
					return true;
				} else if (type.shape[state][y][x] == 1 && isInner(posX + x, posY - y)
						&& board[posY - y][posX + x] != null) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 指定環境で停止可能か
	 * @param board
	 * @param state
	 * @param posX
	 * @param posY
	 * @return
	 */
	public boolean isStopable(Mino[][] board, int state, int posX, int posY) {
		if (!isCollide(board, state, posX, posY) && isCollide(board, state, posX, posY - 1))
			return true;
		else
			return false;
	}

	/**
	 * ボード参照用
	 * @param x
	 * @param y
	 * @return
	 */
	private boolean isInner(int x, int y) {
		return TetrisBoard.isInner(x, y);
	}

	/**
	 * onGroundなら表示しない
	 * @param tetris
	 * @param batch
	 */
	public void draw(TetrisBoard tetris, SpriteBatch batch) {
		if (isOnGround())
			return;

		int ghostY = posY;
		while (!isCollide(tetris.board, state, posX, --ghostY))
			;
		++ghostY;
		for (int y = 0; y < type.shape[state].length; y++) {
			for (int x = 0; x < type.shape[state][y].length; x++) {
				if (type.shape[state][y][x] == 1) {
					batch.draw(TetrisBoard.ghost, tetris.viewX + (posX + x) * 16, tetris.viewY + (ghostY - y) * 16);
					batch.draw(type.img, tetris.viewX + (posX + x) * 16, tetris.viewY + (posY - y) * 16);
				}
			}
		}
	}

	private final int[][] SRS = new int[][] { { 0, 0 }, { 0, -1 }, { 1, 0 }, { 1, -1 },  { 0, -2 }, { 1, -2 },{ 0, 1 },
			{ 0, 2 } };

	/**
	 * 回転を試し、できたらchangedかつspined
	 * @param tetris
	 * @param rightRotate
	 */
	private void rotate(TetrisBoard tetris, boolean rightRotate) {
		int nextstate = state;
		boolean isOk = false;
		if (rightRotate) {
			nextstate++;
			nextstate %= type.shape.length;
		} else {
			nextstate += type.shape.length - 1;
			nextstate %= type.shape.length;
		}

		if (rightRotate)
			for (int index = 0; index < SRS.length; index++) {
				if (!isCollide(tetris.board, nextstate, posX + SRS[index][0] * -1, posY + SRS[index][1])) {
					move(SRS[index][0] * -1, SRS[index][1]);
					isOk = true;
					break;
				}
				if (!isCollide(tetris.board, nextstate, posX + SRS[index][0], posY + SRS[index][1])) {
					move(SRS[index][0], SRS[index][1]);
					isOk = true;
					break;
				}
			}
		else
			for (int index = 0; index < SRS.length; index++) {
				if (!isCollide(tetris.board, nextstate, posX + SRS[index][0], posY + SRS[index][1])) {
					move(SRS[index][0], SRS[index][1]);
					isOk = true;
					break;
				}
				if (!isCollide(tetris.board, nextstate, posX + SRS[index][0] * -1, posY + SRS[index][1])) {
					move(SRS[index][0] * -1, SRS[index][1]);
					isOk = true;
					break;
				}
			}

		if (isOk) {
			changed = true;
			spined = true;
			state = nextstate;
			postponement += tetris.POSTPONE;
		}
	}

	/**
	 * スライドを試す、できるならmoveへ
	 * @param tetris
	 * @param rightSlide
	 */
	private void slide(TetrisBoard tetris, boolean rightSlide) {
		if (rightSlide) {
			if (!isCollide(tetris.board, state, posX + 1, posY)) {
				move(1, 0);
				postponement += tetris.POSTPONE;
			}
		} else {
			if (!isCollide(tetris.board, state, posX - 1, posY)) {
				move(-1, 0);
				postponement += tetris.POSTPONE;
			}
		}
	}

	/**
	 * ハードドロップ、moveの後stickへ
	 * @param tetris
	 */
	private void hardDrop(TetrisBoard tetris) {
		int ground = posY;
		while (!isStopable(tetris.board, state, posX, ground))
			ground--;
		move(0, ground - posY);
		setOnGround();
		spinCheck(tetris);
	}

	/**
	 * 落ちる、下がれない場合条件を満たせば固定
	 * @param tetris
	 */
	private void drop(TetrisBoard tetris) {
		if (isStopable(tetris.board, state, posX, posY)) {
			if (stickwait == 0) {
				lasttouch = tetris.getGametick();
			} else if (tetris.getGametick() - lasttouch > tetris.STICKTICK + postponement) {
				spinCheck(tetris);
				setOnGround();
			} else if (tetris.getGametick() - lasttouch > tetris.FINALSTOPTICK) {
				spinCheck(tetris);
				setOnGround();
			}
			stickwait++;
		} else {
			move(0, -1);
			stickwait = 0;
			lastdrop = tetris.getGametick();
		}
	}

	private void hold(TetrisBoard tetris) {
		tetris.hold();
	}

	/**
	 * 指定差分動く、spined=falseかつchanged=true
	 * @param x
	 * @param y
	 */
	private void move(int x, int y) {
		if (x == 0 && y == 0)
			return;
		posX += x;
		posY += y;
		spined = false;
		changed = true;
	}

	/**
	 * isTspin isTspinMiniをセット
	 * @param tetris
	 */
	private void spinCheck(TetrisBoard tetris) {
		if (type == Mino.T && spined) {
			int cnt = 0;
			boolean miniable = false;
			for (int y = 0; y < type.shape[state].length; y++) {
				for (int x = 0; x < type.shape[state][y].length; x++) {
					if (type.shape[state][y][x] == 3) {
						if (isInner(posX + x, posY - y) && tetris.board[posY - y][posX + x] != null)
							cnt++;
						else if (!isInner(posX + x, posY - y))
							cnt++;
					}
					if (type.shape[state][y][x] == 2
							&& (!isInner(posX + x, posY - y) || tetris.board[posY - y][posX + x] != null)) {
						miniable = true;
					}
				}
			}
			if (cnt >= 3) {
				isTspin = true;
				if (miniable)
					isTspinMini = true;
			}
		}
	}

	private void doNothing(TetrisBoard tetris) {

	}

	public boolean isOnGround() {
		return onGround;
	}

	public void setOnGround() {
		this.onGround = true;
	}

	/**
	 * 場所、回転、形が同じなら等しい
	 */
	public boolean equals(Object obj) {
		if (obj instanceof Tetrimino) {
			Tetrimino mino = (Tetrimino) obj;
			if (mino.posX == this.posX && mino.posY == this.posY && mino.state == this.state
					&& mino.type == this.type) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString() {
		return " : " + type + " : " + posX + ", " + posY + " r:" + state;
	}

	private static final int SLIDE_FIRST = 9;
	private static final int SLIDE_NEXT = 6;
	private static final int DOWN = 1;

	private boolean Lp = false;
	private boolean Rp = false;
	private int Lt = 0;
	private int Rt = 0;
	private int Dt = 0;

	/**
	 * ルールに則ってコントロール
	 * @param tetris
	 * @param control
	 */
	private void control(TetrisBoard tetris, Control control) {

		int now = tetris.getGametick();
		switch (control) {
		case Drop:
			if (now - Dt >= DOWN) {
				Dt = now;
				drop(tetris);
			}
			break;
		case HardDrop:
			hardDrop(tetris);
			break;
		case Hold:
			hold(tetris);
			break;
		case Lrotate:
			rotate(tetris, false);
			break;
		case Lslide:
			if (!Lp) {
				slide(tetris, false);
				Lp = true;
				Lt = now;
			} else if (now - Lt >= SLIDE_FIRST) {
				slide(tetris, false);
				Lt = now - (SLIDE_FIRST - SLIDE_NEXT);
			}
			break;
		case Rrotate:
			rotate(tetris, true);
			break;
		case Rslide:
			if (!Rp) {
				slide(tetris, true);
				Rp = true;
				Rt = now;
			} else if (now - Rt >= SLIDE_FIRST) {
				slide(tetris, true);
				Rt = now - (SLIDE_FIRST - SLIDE_NEXT);
			}
			break;
		case Wait:
			doNothing(tetris);
			break;
		default:
			System.err.println("unknown control");
			break;
		}

		if (control != Control.Lslide)
			Lp = false;
		if (control != Control.Rslide)
			Rp = false;
	}

	@Override
	protected Tetrimino clone() throws CloneNotSupportedException {
		Tetrimino ret = (Tetrimino) super.clone();
		return ret;
	}

	public boolean placeTo(Mino[][] board) {
		for (int y = 0; y < type.shape[state].length; y++) {
			for (int x = 0; x < type.shape[state][y].length; x++) {
				if (type.shape[state][y][x] == 1) {
					if (board[posY - y][posX + x] != null)
						return false;
					else
						board[posY - y][posX + x] = type;
				}
			}
		}
		return true;
	}

	public boolean isChanged() {
		return changed;
	}
}
