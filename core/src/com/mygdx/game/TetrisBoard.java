package com.mygdx.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

class TetrisBoard implements Cloneable {
	public static final int WIDTH = 10;
	public static final int HEIGHT = 22;
	public static final int VISIBLE_HEIGHT = HEIGHT - 2;

	public static final int LEVELUPCNT = 20;// lines

	public static final int MAX_LINES = 1000;// lines

	public Mino[][] board = new Mino[HEIGHT][WIDTH];

	final int viewX, viewY;

	int score = 0;
	int level = 0;
	int clearlines = 0;

	int REN = -1;
	int BackToBack = -1;

	int CntRen = 0;
	int CntTetris = 0;
	int CntTspin = 0;

	private boolean gameOver = false;
	private boolean pileable = false;
	private boolean holdable = true;
	private boolean playable = true;

	private int gametick = 0;

	int DROPTICK;
	int PILETICK;
	int STICKTICK;
	// int NEXTTICK;
	int FINALSTOPTICK;
	int POSTPONE;

	Controler controler;
	TetrisBoard enemy;

	static final Texture background = new Texture("back.png");;
	static final Texture ghost = new Texture("ghost.png");;

	private static final ArrayList<Mino> Package = new ArrayList<Mino>(
			Arrays.asList(Mino.I, Mino.O, Mino.S, Mino.Z, Mino.J, Mino.L, Mino.T));
	public ArrayList<Mino> nexts = new ArrayList<Mino>();
	private ArrayList<Pile> piles = new ArrayList<Pile>();
	private ArrayList<Effect> effects = new ArrayList<Effect>();
	private Tetrimino mino;
	private Mino hold = null;
	private boolean win = false;
	private int cheat = 0;

	public int ClonedPileSum;
	private boolean original;

	/**
	 * 初期化 ミノプッシュ
	 *
	 * @param viewX
	 * @param viewY
	 * @param player
	 *            コントローラー
	 */
	public TetrisBoard(final int viewX, final int viewY, Controler player) {
		this.viewX = viewX;
		this.viewY = viewY;
		this.controler = player;

		this.original = true;

		player.init(this);

		levelup();
		push();
	}

	/**
	 * 現在のレベルでパラメータ設定、レベルを上げる
	 */
	private void levelup() {

		DROPTICK = Math.max(0, 20 - level);
		PILETICK = Math.max(10, 500 - level * 20);
		STICKTICK = Math.max(10, DROPTICK * 2);
		// NEXTTICK = Math.max(1, 5 - level / 2);
		POSTPONE = Math.max(2, 6 - level / 3);
		FINALSTOPTICK = Math.max(50, 300 - level * 10);

		level++;
	}

	public void setEnemy(TetrisBoard enemy) {
		if (this.enemy == enemy)
			return;
		this.enemy = enemy;
		this.enemy.setEnemy(this);
	}

	public void draw(SpriteBatch batch, BitmapFont bitmapFont) throws Exception {
		// board
		for (int y = 0; y < VISIBLE_HEIGHT; y++) {
			for (int x = 0; x < WIDTH; x++) {
				if (board[y][x] == null) {
					batch.draw(background, viewX + x * 16, viewY + y * 16);
				} else {
					batch.draw(board[y][x].img, viewX + x * 16, viewY + y * 16);
				}
			}
		}
		// mino
		getMino().draw(this, batch);
		// next
		for (int i = 0; i < 6; i++) {
			Mino type = nexts.get(i);
			for (int y = 0; y < type.shape[0].length; y++) {
				for (int x = 0; x < type.shape[0][y].length; x++) {
					if (type.shape[0][y][x] == 1) {
						batch.draw(type.img, viewX + (WIDTH + 5 + x) * 12, viewY + (HEIGHT + 2 - (i * 3) - y) * 12, 12,
								12);
					}
				}
			}
		}
		// pile
		int cnt = 0;
		for (int i = 0; i < piles.size(); i++) {
			Pile pile = piles.get(i);
			Texture img;
			if (getGametick() - pile.birthtick < PILETICK / 2)
				img = Mino.Obstacle.img;
			else if (getGametick() - pile.birthtick < PILETICK)
				img = Mino.O.img;
			else
				img = Mino.Z.img;
			for (int y = 0; y < pile.count; y++)
				batch.draw(img, viewX + -2 * 12, viewY + (cnt + y) * 12, 14, 14);
			cnt += pile.count;
		}
		// hold
		if (hold != null) {
			Mino hold = this.hold;
			for (int y = 0; y < hold.shape[0].length; y++)
				for (int x = 0; x < hold.shape[0][y].length; x++)
					if (hold.shape[0][y][x] == 1)
						batch.draw(hold.img, viewX + (-5 + x) * 13, viewY + (HEIGHT - y) * 13, 13, 13);
		}
		// score
		bitmapFont.draw(batch, "Lv: " + level + "  Line: " + clearlines + "\r\nScore: " + score + " ", viewX - 30,
				viewY + 19 * 20);
		// effect
		for (int i = 0; i < effects.size(); i++) {
			Effect effect = effects.get(i);
			bitmapFont.draw(batch, effect.getDescription(), viewX + effect.posX * 16,
					viewY + effect.posY * 16 + (getGametick() - effect.getBirthtick()) * 4);
			if (getGametick() - effect.getBirthtick() > Gdx.graphics.getHeight()) {
				effects.remove(i--);
			}
		}
	}

	/**
	 * ホールド可能ならホールド、ホールド不可に設定
	 */
	public void hold() {
		if (!isHoldable())
			return;
		setHoldable(false);

		if (hold == null) {
			hold = getMino().type;
			/// copy of push
			if (nexts.size() < 7) {
				Collections.shuffle(Package);
				for (int i = 0; i < Package.size(); i++) {
					nexts.add(Package.get(i));
				}
			}
			setMino(new Tetrimino(this, nexts.get(0)));
			nexts.remove(0);
			/// end of push copy
		} else {
			Mino next = hold;
			hold = getMino().type;
			setMino(new Tetrimino(this, next));
		}
	}

	/**
	 * 新しくミノセット、プレイヤーに更新信号を送る
	 */
	public void push() {
		Tetrimino result = getMino();
		if (nexts.size() < 7) {
			Collections.shuffle(Package);
			for (int i = 0; i < Package.size(); i++) {
				nexts.add(Package.get(i));
			}
		}
		setMino(new Tetrimino(this, nexts.get(0)));
		nexts.remove(0);

		if (controler != null)
			controler.refresh(this, result);
	}

	public void tick() {
		assert !isGameover() : this;
		addGametick();

		if (cheat > 0) {
			if (Math.random() < 0.001) {
				attack(4);
			}
		}

		if (getMino().isOnGround()) { // 最小2tick 最大getPileSum()tick
			if (isPlayable()) {// 接地最初
				ground();
				if (clearlines >= MAX_LINES) {
					setGameOver(true);
					return;
				}
				setPlayable(false);
			} else {
				if (isPileable() && piles.size() > 0 && getGametick() - piles.get(0).birthtick > PILETICK) {// 迫り上がり可
					Pile pile = piles.get(0);
					for (int y = HEIGHT - 1; y - 1 >= 0; y--) {
						for (int x = 0; x < WIDTH; x++) {
							board[y][x] = board[y - 1][x];
						}
					}
					for (int x = 0; x < WIDTH; x++) {
						if (x == pile.holeX)
							board[0][x] = null;
						else
							board[0][x] = Mino.Obstacle;
					}
					pile.decrement();
				} else {// 迫り上がり不可：最終的に通る分岐
					setPlayable(true);
					setPileable(false);
					setHoldable(true);
					push();
				}
			}
		} else {
			if (controler == null) {
				getMino().tick(this, Control.Wait);
			} else {
				getMino().tick(this, controler.control(this));
			}
		}
	}

	/**
	 * ボードのミノをボードに置く、置けなかったらゲームオーバー 揃ったラインを消し攻撃、スコア加算 pileableもセット
	 *
	 * @param mino
	 */
	public void ground() {
		//place
		assert mino.isOnGround() : mino;
		if (!mino.placeTo(board)) {
			setGameOver(false);
			return;
		}

		// clear
		int cnt = clear(board);
		for (int k = 0; k < cnt; k++) {
			clearlines++;
			if (clearlines % LEVELUPCNT == 0) {
				levelup();
			}
		}

		//gameover check
		for (int y = HEIGHT - 1; y >= VISIBLE_HEIGHT; y--) {
			for (int x = 0; x < WIDTH; x++) {
				if (board[y][x] != null) {
					setGameOver(false);
					return;
				}
			}
		}

		// REN BackToBack Pile check
		if (cnt > 0) {
			setPileable(false);
			REN++;
			if (REN > 0) {
				CntRen++;
				new Effect("REN " + REN);
			}

			if (mino.isTspin || cnt == 4) {
				BackToBack++;
			} else {
				BackToBack = -1;
			}
		} else {
			setPileable(true);
			REN = -1;
		}

		//line counts
		int line = 0;

		if (mino.isTspin) {
			CntTspin++;
			score += 1000;
			if (cnt == 3) {
				new Effect("T-Spin-Triple");
				line+=6;
			}else if (mino.isTspinMini) {
				new Effect("T-Spin-Mini");
				line+=0;
			}else if (cnt == 1) {
				new Effect("T-Spin-Single");
				line += 2;
			}else if (cnt == 2) {
				new Effect("T-Spin-Double");
				line+=4;
			}
		} else {
			if (cnt == 1) {
				// new Effect("single");
			line+=0;
			}else if (cnt == 2) {
				new Effect("double");
				line+=1;
			}else if (cnt == 3) {
				new Effect("triple");
				line+=2;
			}else if (cnt == 4) {
				CntTetris++;
				score += 1000;
				new Effect("TETRIS");
				line+=4;
			}
		}

		if (BackToBack > 0 && cnt > 0)
			line++;
		if (REN <= 0) {

		} else if (REN <= 2) {
			line++;
		} else if (REN <= 4) {
			line += 2;
		} else if (REN <= 6) {
			line += 3;
		} else if (REN <= 9) {
			line += 4;
		} else {
			line += 5;
		}

		if (isPerfect(board)) {
			line += 5;
			new Effect("ALL-CLEAR");
		}

		score += line * line * 10;
		attack(line);
	}

	/**
	 * 溜まりから引く、まだ残っていれば残りで敵に攻撃
	 *
	 * @param line
	 */
	private void attack(int line) {
		if (line == 0)
			return;
		int cnt = line;
		while (cnt > 0 && !piles.isEmpty()) {
			piles.get(0).decrement();
			cnt--;
		}

		if (enemy != null)
			enemy.pileUp(cnt);
	}

	/**
	 * new Pile(line)
	 *
	 * @param line
	 */
	private void pileUp(int line) {
		if (line <= 0)
			return;
		new Pile(line);
	}

	/**
	 *
	 * @param board
	 * @return 消したラインの数
	 */
	private static int clear(Mino[][] board) {
		int cnt = 0;
		for (int y = 0; y < HEIGHT; y++) {
			boolean isFull = true;
			for (int x = 0; x < WIDTH; x++) {
				if (board[y][x] == null) {
					isFull = false;
					break;
				}
			}
			if (isFull) {
				cnt++;
				for (int cy = y-- + 1; cy < HEIGHT; cy++) {
					for (int cx = 0; cx < WIDTH; cx++) {
						board[cy - 1][cx] = board[cy][cx];
					}
				}
			}
		}
		return cnt;
	}

	private static boolean isPerfect(Mino[][] board) {
		for (int x = 0; x < WIDTH; x++) {
			if (board[0][x] != null) {
				return false;
			}
		}
		return true;
	}

	private boolean isHoldable() {
		return holdable && !getMino().isOnGround();
	}

	private void setHoldable(boolean holdable) {
		this.holdable = holdable;
	}

	public int getGametick() {
		return gametick;
	}

	/**
	 * インクリメントするだけ
	 */
	public void addGametick() {
		this.gametick++;
	}

	public boolean isPlayable() {
		return playable;
	}

	private void setPlayable(boolean playable) {
		this.playable = playable;
	}

	private boolean isPileable() {
		return pileable;
	}

	private void setPileable(boolean pileable) {
		this.pileable = pileable;
	}

	/**
	 * チートモードで負けならボードをクリアしてプッシュするだけ プレイ不可にしコントローラーに終了信号
	 *
	 * @param win
	 *            boolwin
	 */
	public void setGameOver(boolean win) {
		if (controler == null) {
			this.gameOver = true;
			this.win = win;
		} else if (cheat > 0) {
			board = new Mino[HEIGHT][WIDTH];
			cheat--;
			piles.clear();
			controler.gameset(this);
			push();
		} else {
			setPlayable(false);
			this.gameOver = true;
			this.win = win;
			score += Math.sqrt(getGametick());
//			if (this.win)
//				score *= 2;
//			score /= (clearlines + 1);
			controler.gameset(this);
		}
	}

	public boolean isGameover() {
		return gameOver;
	}

	public boolean isWin() {
		return win;
	}

	@Override
	protected TetrisBoard clone() throws CloneNotSupportedException {
		TetrisBoard ret = (TetrisBoard) super.clone();
		ret.original = false;
		ret.board = cloneBoard(board);
		ret.nexts = new ArrayList<Mino>(nexts);
		ret.piles = new ArrayList<Pile>();
		ret.effects = new ArrayList<Effect>();
		ret.enemy = null;
		ret.controler = null;
		if (getMino() != null) {
			ret.setMino(getMino().clone());
		}
		if (original) {
			ret.ClonedPileSum = getPileSum();
		}
		return ret;
	}

	private static Mino[][] cloneBoard(Mino[][] baseboard) {
		Mino[][] retboard = new Mino[baseboard.length][];
		for (int i = 0; i < baseboard.length; i++)
			retboard[i] = baseboard[i].clone();
		return retboard;
	}

	public static boolean isInner(int x, int y) {
		if (0 <= x && x < WIDTH && 0 <= y && y < HEIGHT)
			return true;
		else
			return false;
	}

	public static void printBoard(Mino[][] board, Tetrimino mino) {
		for (int y = HEIGHT - 1; y >= 0; --y) {
			String line = "";
			for (int x = 0; x < WIDTH; x++) {
				if (board[y][x] != null) {
					line += "■";
				} else {
					int inX = x - mino.posX;
					int inY = mino.posY - y;
					if (0 <= inY && inY < mino.type.shape[mino.state].length && 0 <= inX
							&& inX < mino.type.shape[mino.state][inY].length
							&& mino.type.shape[mino.state][inY][inX] == 1) {
						line += "★";
					} else {
						line += "□";
					}
				}
			}
			System.out.println(line);
		}
	}

	@Override
	public String toString() {
		return "Level:" + level + " Score:" + score + " Line:" + clearlines + " Tetris:" + CntTetris + " Ren:" + CntRen
				+ " Tspin:" + CntTspin;
	}

	class Pile {
		int count;
		int holeX;
		int birthtick;

		public Pile(int count) {
			if (count <= 0)
				throw new Error("Invalid Pile Count");
			this.count = count;
			this.birthtick = getGametick();
			if (!piles.isEmpty()) {
				this.holeX = piles.get(piles.size() - 1).holeX;
			} else {
				this.holeX = (int) (Math.random() * WIDTH);
			}
			piles.add(this);
		}

		public void decrement() {
			count--;
			if (count <= 0) {
				piles.remove(this);
			}
		}
	}

	class Effect {
		private final int birthtick;
		private final String description;
		private final int posX, posY;

		public Effect(String description) {
			this.birthtick = getGametick();
			this.description = description;
			posX = mino.posX - description.length() / 2;
			posY = mino.posY;
			effects.add(this);
		}

		public int getBirthtick() {
			return birthtick;
		}

		public String getDescription() {
			return description;
		}
	}

	/**
	 *
	 * @return 溜まっているライン数
	 */
	public int getPileSum() {
		int ret = 0;
		for (Pile pile : piles) {
			ret += pile.count;
		}
		return ret;
	}

	public Tetrimino getMino() {
		return mino;
	}

	/**
	 * フィールドミノ代入
	 *
	 * @param mino
	 *            ミノ
	 */
	public void setMino(Tetrimino mino) {
		this.mino = mino;
	}

	public void setCheat(int chaet) {
		this.cheat = chaet;
	}
}