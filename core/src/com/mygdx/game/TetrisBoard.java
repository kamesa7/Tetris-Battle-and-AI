package com.mygdx.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

class TetrisBoard implements Cloneable {
	public static final int WIDTH = 10;
	public static final int HEIGHT = 22;
	public static final int VISIBLE_HEIGHT = 20;

	public static final int LEVELUPCNT = 20;// lines
	// public static final int TICKTIME = 20;// ms

	public Mino[][] board = new Mino[HEIGHT][WIDTH];

	final int viewX, viewY;

	int score = 0;
	int level = 0;
	int clearlines = 0;

	int REN = -1;
	int BackToBack = -1;

	private boolean gameOver = false;
	private boolean pileable = false;
	private boolean holdable = true;
	private boolean playable = true;

	private int gametick = 0;
	// private long lastSystick = System.currentTimeMillis();

	int DROPTICK;
	int PILETICK;
	int STICKTICK;
	int NEXTTICK;
	int FINALSTOPTICK;
	int POSTPONE;

	Controler player;
	TetrisBoard enemy;

	static final Texture background = new Texture("back.png");;
	static final Texture ghost = new Texture("ghost.png");;

	private static final ArrayList<Mino> Package = new ArrayList<Mino>(
			Arrays.asList(Mino.I, Mino.O, Mino.S, Mino.Z, Mino.J, Mino.L, Mino.T));
	public ArrayList<Mino> nexts = new ArrayList<Mino>();
	private ArrayList<Pile> piles = new ArrayList<Pile>();
	private Tetrimino mino;
	private Mino hold = null;
	private boolean win = false;
	private int cheat = 0;

	public TetrisBoard(final int viewX, final int viewY, Controler player) {
		this.viewX = viewX;
		this.viewY = viewY;
		this.player = player;

		player.init(this);

		levelup();
		push();
	}

	private void levelup() {

		DROPTICK = Math.max(0, 20 - level);
		PILETICK = Math.max(100, 300 - level * 10);
		STICKTICK = Math.max(20, DROPTICK * 2);
		NEXTTICK = Math.max(2, 5 - level / 2);
		POSTPONE = Math.max(2, 6 - level / 3);
		FINALSTOPTICK = Math.max(100, 300 - level * 10);

		level++;
	}

	public void setEnemy(TetrisBoard enemy) {
		if (this.enemy == enemy)
			return;
		this.enemy = enemy;
		this.enemy.setEnemy(this);
	}

	public void draw(SpriteBatch batch) {
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
		for (Pile pile : piles) {
			Texture img;
			if (getGametick() - pile.birthtick < PILETICK / 2)
				img = Mino.Obstacle.img;
			else if (getGametick() - pile.birthtick < PILETICK)
				img = Mino.O.img;
			else
				img = Mino.Z.img;
			for (int y = 0; y < pile.count; y++)
				batch.draw(img, viewX + -2 * 12, viewY + (cnt + y) * 12, 12, 12);
			cnt += pile.count;
		}
		// hold
		if (hold != null)
			for (int y = 0; y < hold.shape[0].length; y++)
				for (int x = 0; x < hold.shape[0][y].length; x++)
					if (hold.shape[0][y][x] == 1)
						batch.draw(hold.img, viewX + (-5 + x) * 13, viewY + (HEIGHT - y) * 13, 13, 13);
	}

	public void hold() {
		if (!isHoldable())
			return;
		setHoldable(false);

		if (hold == null) {
			hold = getMino().type;
			push();
		} else {
			Mino next = hold;
			hold = getMino().type;
			setMino(new Tetrimino(this, next));
		}
	}

	private void push() {
		if (nexts.size() < 7) {
			Collections.shuffle(Package);
			for (int i = 0; i < Package.size(); i++) {
				nexts.add(Package.get(i));
			}
		}
		Tetrimino result = getMino();

		setMino(new Tetrimino(this, nexts.get(0)));
		nexts.remove(0);

		player.refresh(this, result);
	}

	public void tick() {
		if (player == null) {
			tick(Control.Wait);
		} else {
			tick(player.control(this));
		}
	}

	public void tick(Control control) {
		addGametick();

		if (getMino().onGround) {
			if (isPlayable()) {
				ground(getMino());
			} else {
				if (isPileable()) {
					if (piles.size() > 0 && getGametick() - piles.get(0).birthtick > PILETICK) {
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
					} else {
						setPlayable(true);
						setPileable(false);
						push();
					}
				} else {
					setPlayable(true);
					push();
				}
			}
		} else {
			getMino().tick(this, control);
		}
	}

	private void ground(Tetrimino mino) {
		if (!mino.placeTo(board)) {
			setGameOver(false);
			return;
		}
//		if(clearlines>=40) {
//			setGameOver(true);
//			return;
//		}

		int cnt = clear(board);
		for (int k = 0; k < cnt; k++) {
			score += 5;
			clearlines++;
			if (clearlines % LEVELUPCNT == 0) {
				levelup();
			}
		}

		if (cnt > 0) {
			if (mino.isTspin || cnt == 4) {
				if (++BackToBack > 0) {
					// System.out.println(" BackToBack " + BackToBack);
				}
			} else {
				BackToBack = -1;
			}
			if (++REN > 0) {
				// System.out.println(" REN " + REN);
			}
			setPileable(false);
		} else {
			REN = -1;
			setPileable(true);
		}

		int line = 0;
		line += attackBuff();
		if (isPerfect(board)) {
//			System.out.println("Perfect-Clear");
			line += 5;
		}
		line += attackMino(cnt, mino);

		// if (mino.isTspin) {
		// if (cnt == 3) {
		// // System.out.println("Tspin-Triple");
		// } else if (mino.isTspinMini) {
		// // System.out.println("Tspin-Mini " + cnt);
		// } else if (cnt == 2) {
		// // System.out.println("Tspin-Double");
		// } else if (cnt == 1) {
		// // System.out.println("Tspin-Single");
		// } else {
		// // System.out.println("Tspin");
		// }
		// } else {
		// if (cnt == 4) {
		// // System.out.println("Tetris");
		// } else if (cnt == 3) {
		// // System.out.println("Triple");
		// } else if (cnt == 2) {
		// // System.out.println("Double");
		// } else if (cnt == 1) {
		// // System.out.println("Single");
		// }
		// }
		// if (line > 0)
		// System.out.println(this);

		attack(line);

		setPlayable(false);
		setHoldable(true);
	}

	private void attack(int line) {
		if (line == 0)
			return;

		score += (line + 1) * line * 10;

		int cnt = line;
		while (cnt > 0 && !piles.isEmpty()) {
			piles.get(0).decrement();
			cnt--;
		}

		if (enemy != null)
			enemy.pileUp(line);
	}

	private void pileUp(int line) {
		if (line == 0)
			return;
		new Pile(line);
	}

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

	private int attackBuff() {
		int line = 0;
		if (BackToBack > 0)
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
		return line;
	}

	private static int attackMino(int clear, Tetrimino mino) {
		if (mino.isTspin) {
			if (clear == 3) {
				return 6;
			} else if (mino.isTspinMini) {
				return 0;
			} else if (clear == 2) {
				return 4;
			} else if (clear == 1) {
				return 2;
			}
		} else {
			if (clear == 4) {
				return 4;
			} else if (clear == 3) {
				return 2;
			} else if (clear == 2) {
				return 1;
			} else if (clear == 1) {
				return 0;
			}
		}
		return 0;
	}

	@Override
	public String toString() {
		return "Level:" + level + " Score:" + score + " Line:" + clearlines;
	}

	private boolean isHoldable() {
		return holdable && !getMino().onGround;
	}

	private void setHoldable(boolean holdable) {
		this.holdable = holdable;
	}

	public int getGametick() {
		return gametick;
	}

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

	public void setGameOver(boolean win) {
		if (!win && cheat>0) {
			board = new Mino[HEIGHT][WIDTH];
			cheat--;
			push();
			return;
		}

		setPlayable(false);
		this.gameOver = true;
		this.win = win;
		score += Math.sqrt(getGametick());
		player.gameset(this);
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
		ret.board = cloneBoard(board);
		ret.nexts = new ArrayList<Mino>(nexts);
		ret.piles = new ArrayList<Pile>(piles);
		ret.enemy = null;
		if (getMino() != null) {
			ret.setMino(getMino().clone());
		}
		return ret;
	}

	public static Mino[][] cloneBoard(Mino[][] baseboard) {
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

	public void setMino(Tetrimino mino) {
		this.mino = mino;
	}

	public void setCheat(int chaet) {
		this.cheat = chaet;
	}
}