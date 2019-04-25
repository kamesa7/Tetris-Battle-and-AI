package com.mygdx.game;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

enum BlockState {
	BLOCK, HOLE, LID, SKY, OPEN
}

public class Evaluater {

	public double evaluate(TetrisBoard basetetris, Tetrimino mino) {
		TetrisBoard tetris;
		try {
			tetris = basetetris.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return 0;
		}
		tetris.setMino(mino);
		mino.tick(tetris, Control.HardDrop);
		int prescore = tetris.score;

		tetris.tick(Control.Wait);

		Mino[][] board = tetris.board;
		int upscore = tetris.score - prescore;
		@SuppressWarnings("unused")
		int block = 0;
		int hole = 0;
		int lid = 0;
		int highest = 0;
		int indoor = 0;
		BlockState evboard[][] = new BlockState[TetrisBoard.HEIGHT][TetrisBoard.WIDTH];
		fillfield(board, evboard, Tetrimino.startX, TetrisBoard.HEIGHT - 1);
		for (int x = 0; x < TetrisBoard.WIDTH; x++)
			for (int y = TetrisBoard.HEIGHT - 1; y >= 0; --y) {
				if (evboard[y][x] == BlockState.OPEN)
					evboard[y][x] = BlockState.SKY;
				else
					break;
			}

		for (int y = 0; y < TetrisBoard.HEIGHT; y++)
			for (int x = 0; x < TetrisBoard.WIDTH; x++)
				if (board[y][x] != null) {
					evboard[y][x] = BlockState.BLOCK;
					highest = y;
				} else if (evboard[y][x] == null)
					evboard[y][x] = BlockState.HOLE;

		for (int y = 0; y < TetrisBoard.HEIGHT; y++) {
			for (int x = 0; x < TetrisBoard.WIDTH; x++) {
				if (evboard[y][x] == BlockState.HOLE) {
					int ck = y + 1;
					while (TetrisBoard.isInner(x, ck) && evboard[ck][x] == BlockState.BLOCK)
						evboard[ck++][x] = BlockState.LID;
				}
			}
		}

		for (int y = 0; y < TetrisBoard.HEIGHT; y++) {
			for (int x = 0; x < TetrisBoard.WIDTH; x++) {
				switch (evboard[y][x]) {
				case BLOCK:
					block++;
					break;
				case SKY:
					break;
				case HOLE:
					hole++;
					break;
				case LID:
					block++;
					lid++;
					break;
				case OPEN:
					indoor++;
					break;
				default:
					break;
				}
			}
		}

		double highvar = 0;
		int highave = 0;
		for (int x = 0; x < TetrisBoard.WIDTH; x++) {
			for (int y = highest; y >= 0; y--) {
				if (evboard[y][x] != BlockState.SKY) {
					highvar += y * y;
					highave += y;
					break;
				}
			}
		}
		highvar /= TetrisBoard.WIDTH;
		highave /= TetrisBoard.WIDTH;
		highvar = Math.sqrt(highvar - (highave * highave));

		double ret = 0;
		int i = 0;
		double[] data = new double[9];
		data[i++] = upscore;
		// data[i++] = block;
		data[i++] = hole;
		data[i++] = lid;
		data[i++] = indoor;
		data[i++] = highest;
		data[i++] = highvar;
		data[i++] = mino.posY;
		// data[i++] = tetris.getPileSum();
		data[i++] = tetris.REN + 1;
		data[i++] = tetris.BackToBack + 1;
		// System.out.println(Arrays.toString(data));
		forward(data);
		ret = getResult();
		return ret;
	}

	private void fillfield(Mino[][] board, BlockState[][] hole, int x, int y) {
		if (TetrisBoard.isInner(x, y) && board[y][x] == null && hole[y][x] == null) {
			hole[y][x] = BlockState.OPEN;
			fillfield(board, hole, x - 1, y);
			fillfield(board, hole, x + 1, y);
			fillfield(board, hole, x, y - 1);
		}
	}

	public static final int layerN = 3;
	public static final int[] unitN = { 9, 9, 1 };
	public double[][] unit;
	public double[][][] w;
	public static final double wMax = 1.0;
	double[][] delta;
	int outLayer;
	double err;

	public static final double MUTATE = 0.01;
	private int score = -1;

	// initialize
	public Evaluater() {
		outLayer = layerN - 1;
		unit = new double[layerN][];
		delta = new double[layerN][];
		w = new double[layerN][][];
		for (int l = 0; l < layerN; l++) {
			int u = unitN[l];
			unit[l] = new double[u + (l == outLayer ? 0 : 1)];
			if (l < outLayer)
				unit[l][u] = 1.0;
			if (l > 0) {
				int v = unitN[l - 1] + 1;
				delta[l] = new double[u];
				w[l] = new double[u][v];
			}
		}
		for (int l = 1; l < layerN; l++) {
			for (int j = 0; j < unitN[l]; j++) {
				for (int i = 0; i < unitN[l - 1] + 1; i++) {
					w[l][j][i] = (Math.random() * 2 - 1) * wMax;
				}
			}
		}
	}

	// load
	public Evaluater(File file) throws NumberFormatException, IOException {
		if (file.exists() && file.isFile() && file.canWrite()) {
			System.out.println(file.getAbsolutePath());
			BufferedReader br = new BufferedReader(new FileReader(file));
			outLayer = layerN - 1;
			unit = new double[layerN][];
			delta = new double[layerN][];
			w = new double[layerN][][];
			for (int l = 0; l < layerN; l++) {
				int u = unitN[l];
				unit[l] = new double[u + (l == outLayer ? 0 : 1)];
				if (l < outLayer)
					unit[l][u] = 1.0;
				if (l > 0) {
					int v = unitN[l - 1] + 1;
					delta[l] = new double[u];
					w[l] = new double[u][v];
				}
			}
			for (int l = 1; l < layerN; l++) {
				for (int j = 0; j < unitN[l]; j++) {
					for (int i = 0; i < unitN[l - 1] + 1; i++) {
						w[l][j][i] = Double.longBitsToDouble(Long.parseLong(br.readLine()));
					}
				}
			}
			br.close();
		} else {
			System.out.println("ファイルを読み込めません");
		}
	}

	// clone
	public Evaluater(Evaluater target) {
		outLayer = layerN - 1;
		unit = new double[layerN][];
		delta = new double[layerN][];
		w = new double[layerN][][];
		for (int l = 0; l < layerN; l++) {
			int u = unitN[l];
			unit[l] = new double[u + (l == outLayer ? 0 : 1)];
			if (l < outLayer)
				unit[l][u] = 1.0;
			if (l > 0) {
				int v = unitN[l - 1] + 1;
				delta[l] = new double[u];
				w[l] = new double[u][v];
			}
		}
		for (int l = 1; l < layerN; l++) {
			for (int j = 0; j < unitN[l]; j++) {
				for (int i = 0; i < unitN[l - 1] + 1; i++) {
					w[l][j][i] = target.w[l][j][i];
				}
			}
		}
	}

	// child
	public Evaluater(Evaluater first, Evaluater second) {
		outLayer = layerN - 1;
		unit = new double[layerN][];
		delta = new double[layerN][];
		w = new double[layerN][][];
		for (int l = 0; l < layerN; l++) {
			int u = unitN[l];
			unit[l] = new double[u + (l == outLayer ? 0 : 1)];
			if (l < outLayer)
				unit[l][u] = 1.0;
			if (l > 0) {
				int v = unitN[l - 1] + 1;
				delta[l] = new double[u];
				w[l] = new double[u][v];
			}
		}
		for (int l = 1; l < layerN; l++) {
			for (int j = 0; j < unitN[l]; j++) {
				for (int i = 0; i < unitN[l - 1] + 1; i++) {
					double tw = (first.w[l][j][i] + second.w[l][j][i]) / 2;

					if (Math.random() < 0.5)
						tw = first.w[l][j][i];
					else
						tw = second.w[l][j][i];

					if (Math.random() < MUTATE)
						tw = (Math.random() * 2 - 1) * wMax;

					w[l][j][i] = tw;
				}
			}
		}
	}

	double sigmoid(double x) {
		return 1 / (1 + Math.pow(Math.E, -x));
	}

	void softmax(double[] x) {
		if (x.length == 1) {
			for (int i = 0; i < x.length; i++)
				x[i] = sigmoid(x[i]);
		} else {
			double s = 0.0;
			for (int i = 0; i < x.length; i++) {
				x[i] = Math.exp(x[i]);
				s += x[i];
			}
			for (int i = 0; i < x.length; i++)
				x[i] /= s;
		}
	}

	public void forward(double[] d) {
		for (int j = 0; j < unitN[0]; j++)
			unit[0][j] = d[j];
		for (int l = 0; l < outLayer - 1; l++) {
			for (int j = 0; j < unitN[l + 1]; j++) {
				double s = 0.0;
				for (int i = 0; i < unitN[l] + 1; i++) {
					s += w[l + 1][j][i] * unit[l][i];
				}
				unit[l + 1][j] = sigmoid(s);
			}
		}
		for (int j = 0; j < unitN[outLayer]; j++) {
			unit[outLayer][j] = 0.0;
			for (int i = 0; i < unitN[outLayer - 1] + 1; i++) {
				unit[outLayer][j] += w[outLayer][j][i] * unit[outLayer - 1][i];
			}
		}
		softmax(unit[outLayer]);
	}

	void backPropagate(double[] d, double[] t) {
		for (int j = 0; j < unitN[outLayer]; j++) {
			double e = t[j] - unit[outLayer][j];
			delta[outLayer][j] = e;
			err += e * e;
		}
		for (int l = outLayer - 1; l > 0; l--) {
			for (int j = 0; j < unitN[l]; j++) {
				double df = unit[l][j] * (1.0 - unit[l][j]);
				double s = 0.0;
				for (int k = 0; k < unitN[l + 1]; k++) {
					s += delta[l + 1][k] * w[l + 1][k][j];
				}
				delta[l][j] = df * s;
			}
		}
	}

	void update(double rate) {
		for (int l = layerN - 1; l > 0; l--) {
			for (int j = 0; j < unitN[l]; j++) {
				for (int i = 0; i < unitN[l - 1] + 1; i++) {
					w[l][j][i] += rate * delta[l][j] * unit[l - 1][i];
				}
			}
		}
	}

	public void train(double[][] data, double[][] teach, int patN, int trainN, double learnRate) {
		for (int t = 0; t < trainN; t++) {
			err = 0.0;
			for (int p = 0; p < patN; p++) {
				forward(data[p]);
				backPropagate(data[p], teach[p]);
				update(learnRate);
			}
		}
	}

	public double getResult() {
		double max = 0.0;
		double[] out = unit[layerN - 1];
		for (int j = 0; j < out.length; j++) {
			if (out[j] > max) {
				max = out[j];
			}
		}
		return max;
	}

	public void setScore(int score) {
		this.score = score;
	}

	public int getScore() {
		if (score == -1)
			throw new Error("NotUsedEvaluater");
		return score;
	}

	@Override
	public String toString() {
		return String.valueOf(getScore());
	}

	public void writeGene() throws IOException {
		File file = new File("gene.csv");
		if (!file.exists()) {
			file.createNewFile();
		}
		if (file.exists() && file.isFile() && file.canWrite()) {
			System.out.println(file.getAbsolutePath());
			BufferedWriter bf = new BufferedWriter(new FileWriter(file));
			for (int l = 1; l < layerN; l++) {
				for (int j = 0; j < unitN[l]; j++) {
					for (int i = 0; i < unitN[l - 1] + 1; i++) {
						bf.write(Long.toString(Double.doubleToLongBits(w[l][j][i])));
						bf.newLine();
					}
				}
			}
			bf.close();
		} else {
			System.out.println("ファイルに書き込めません");
		}
	}
}

class SimpleEvaluater extends Evaluater {
	@Override
	public double evaluate(TetrisBoard basetetris, Tetrimino mino) {
		TetrisBoard tetris;
		try {
			tetris = basetetris.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return 0;
		}
		tetris.setMino(mino);
		mino.tick(tetris, Control.HardDrop);

		int prescore = tetris.score;
		tetris.tick(Control.Wait);
		int upscore = tetris.score - prescore;

		Mino[][] board = tetris.board;
		double ret = 0;
		for (int y = 0; y < TetrisBoard.HEIGHT; y++) {
			for (int x = 0; x < TetrisBoard.WIDTH; x++) {
				if (board[y][x] != null) {
					ret -= y * 3;
				}
			}
		}
		ret += upscore;
		return ret;
	}
}
