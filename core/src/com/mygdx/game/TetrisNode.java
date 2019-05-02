package com.mygdx.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.PriorityQueue;

class TetrisNode {
	private ArrayList<Node> path = new ArrayList<Node>();
	private static final int searchLimit = 200;

	/**
	 * 新規ノード どこに置いてもゲームオーバーの時バグる
	 *
	 * @param target
	 *            新規ミノが出た直後のもの
	 * @param evaluater
	 * @throws CloneNotSupportedException
	 */
	public TetrisNode(TetrisBoard target, Evaluater evaluater) throws CloneNotSupportedException {
		assert target.isGameover() == false : "InvalidTarget";
		assert target.getMino() != null : "NoMinoTarget";

		if (target.getMino().isOnGround()) // ゲームオーバー予定
			return;
		// 1
		TetrisBoard tetrisNormal = target.clone();
		Tetrimino normal = tetrisNormal.getMino();
		// 2
		TetrisBoard tetrisHold = target.clone();
		tetrisHold.hold();
		Tetrimino hold = tetrisHold.getMino();

		boolean holdable = !hold.isOnGround();

		assert tetrisHold.isGameover() == false : tetrisHold;
		assert normal.hashCode() != hold.hashCode() : normal + " " + hold;

		// add all candidate
		ArrayList<Candidate> candidate = new ArrayList<Candidate>();
		for (int y = -3; y < TetrisBoard.HEIGHT; y++) {
			for (int x = -3; x < TetrisBoard.WIDTH + 3; x++) {
				for (int state = 0; state < 4; state++) {
					if (normal.isStopable(tetrisNormal.board, state, x, y)) {
						candidate.add(new Candidate(tetrisNormal, new Tetrimino(tetrisNormal, normal.type, x, y, state),
								evaluater));
					}
					if (holdable && hold.isStopable(tetrisHold.board, state, x, y)) {
						candidate.add(new Candidate(tetrisHold, new Tetrimino(tetrisHold, hold.type, x, y, state),
								evaluater));
					}
				}
			}
		}
		assert candidate.size() > 0 : "no candidate";
		Collections.sort(candidate);
		// System.out.println("candidate1: " + candidate.size());

		candidate = new ArrayList<Candidate>(candidate.subList(0, candidate.size() / 3));
		for (int i = 0; i < candidate.size(); i++) {
			candidate.get(i).nextCandidate();
		}
		Collections.sort(candidate);
		// System.out.println("candidate2: " + candidate.size());

		for (int i = 0; i < candidate.size(); i++) {
			Candidate cand = candidate.get(i);
			assert cand.getMino().isOnGround() == false : "InvalidMino";
			if (cand.getMino().type == target.getMino().type) {
				path = bestFirstSearch(new Node(null, target, target.getMino(), Control.Wait), cand.getMino());
				if (path.isEmpty())
					continue;
				return;
			} else {
				path = bestFirstSearch(new Node(null, target, hold, Control.Wait), cand.getMino());
				if (path.isEmpty())
					continue;
				if (cand.getMino().type != target.getMino().type)
					path.add(0, new Node(null, target, target.getMino(), Control.Hold));
				return;
			}
		}
		path.clear();
	}

	public ArrayList<Node> getBestControl() {
		return path;
	}

	/**
	 * 最良優先探索する
	 *
	 * @param start
	 *            現在状況ノード
	 * @param goal
	 *            ゴール地ミノ（非接地）
	 * @return ノードリスト（到達不可で空リスト）
	 * @throws CloneNotSupportedException
	 */
	private ArrayList<Node> bestFirstSearch(Node start, final Tetrimino goal) throws CloneNotSupportedException {
		assert start.getMino().isOnGround() == false : start.getMino();
		assert goal.isOnGround() == false : goal;
		// System.out.println("start:" + start);
		// System.out.println("goal :" + goal);

		PriorityQueue<Node> open = new PriorityQueue<Node>(searchLimit, new Comparator<Node>() {
			public int compare(Node o1, Node o2) {
				return o1.getCost(goal) - o2.getCost(goal);
			}
		});
		int count = 0;
		open.add(start);
		while (!open.isEmpty()) {
			Node node = open.poll();
			if (node.getMino().equals(goal)) {
				ArrayList<Node> ret = new ArrayList<Node>();
				Node tmp = node;
				while (tmp.parent != null) {
					ret.add(tmp);
					tmp = tmp.parent;
				}
				Collections.reverse(ret);
				return ret;
			} else if (count++ > searchLimit) {
				// System.err.println("limit exceed");
				// System.out.println("start:" + start);
				// System.out.println("goal :" + goal);
				// TetrisBoard.printBoard(start.tetris.board,goal);
				return new ArrayList<Node>();
			}
			for (Node exNode : expandNode(node)) {
				assert exNode.isValid() : exNode;
				open.add(exNode);
			}
		}
		return new ArrayList<Node>();
	}

	/**
	 * テトリスボードクローンの後addTickしてから探索地を広げる
	 *
	 * @param node
	 *            起点
	 * @return 有効ノードリスト
	 * @throws CloneNotSupportedException
	 */
	private ArrayList<Node> expandNode(Node node) throws CloneNotSupportedException {
		ArrayList<Node> ret = new ArrayList<Node>();
		ArrayList<Control> checkCTR = new ArrayList<Control>(Arrays.asList(Control.Drop, Control.Lslide, Control.Rslide,
				Control.Lrotate, Control.Rrotate/* , Control.Wait */));
		// Arrays.asList(Control.values()));
		TetrisBoard currentTetris = node.getTetris().clone();
		assert currentTetris.isGameover() == false : currentTetris;
		assert node.getMino().isOnGround() == false : node.getMino();
		currentTetris.addGametick();
		for (Control control : checkCTR) {
			Node tmp = new Node(node, currentTetris, node.getMino().clone(), control);
			if (tmp.isValid())
				ret.add(tmp);
		}
		return ret;
	}

	class Candidate implements Comparable<Candidate> {
		private final Tetrimino mino;
		private double score;
		private TetrisBoard tetris;
		private Evaluater evaluater;

		/**
		 * 候補
		 *
		 * @param tetris
		 *            ミノ誕生時ボード
		 * @param mino
		 *            ゴール地点にいるミノ、非接地
		 * @param evaluater
		 */
		public Candidate(TetrisBoard tetris, Tetrimino mino, Evaluater evaluater) {

			assert mino.isOnGround() == false : mino;
			assert mino.isStopable(tetris.board, mino.state, mino.posX, mino.posY) : mino;

			this.tetris = tetris;
			this.mino = mino;
			this.evaluater = evaluater;
			this.score = evaluater.evaluate(tetris, mino);

			assert mino.isOnGround() == false : mino;
		}

		/**
		 * 次のミノを探索、最良スコアを加算
		 *
		 * @throws CloneNotSupportedException
		 */
		public void nextCandidate() throws CloneNotSupportedException {
			TetrisBoard nextetris = tetris.clone();
			assert nextetris.isGameover() == false : nextetris;
			assert mino.isOnGround() == false : mino;
			Tetrimino mino = this.mino.clone();
			mino.setOnGround();
			nextetris.setMino(mino);
			nextetris.ground();
			if (nextetris.isGameover())
				return;
			nextetris.push();

			assert nextetris.getMino() != mino : "SameMinoErr";

			ArrayList<Candidate> candidate = new ArrayList<Candidate>();
			for (int y = -2; y < TetrisBoard.HEIGHT + 2; y++) {
				for (int x = -2; x < TetrisBoard.WIDTH + 2; x++) {
					for (int state = 0; state < 4; state++) {
						if (nextetris.getMino().isStopable(nextetris.board, state, x, y)) {
							candidate.add(new Candidate(nextetris,
									new Tetrimino(nextetris, nextetris.getMino().type, x, y, state), evaluater));
						}
					}
				}
			}
			if (candidate.isEmpty())
				return;

			Collections.sort(candidate);
			this.score += candidate.get(0).getScore();
		}

		public double getScore() {
			return score;
		}

		public Tetrimino getMino() {
			return mino;
		}

		@Override
		public int compareTo(Candidate o) {
			// System.out.println(o1.getScore()+" "+o2.getScore());
			if (getScore() == o.getScore())
				return 0;
			else if (getScore() < o.getScore())
				return 1;
			else
				return -1;
		}
	}

	class Node {
		final Control control;
		final Node parent;
		private boolean valid = true;
		private TetrisBoard tetris;
		private Tetrimino mino;

		public Node(Node parent, TetrisBoard tetris, Tetrimino mino, Control control) {
			this.parent = parent;
			this.tetris = tetris;
			this.mino = mino;
			this.control = control;

			assert tetris.isGameover() == false : "InvalidNode";
			assert !mino.isOnGround() : "InvalidMino";

			if (parent == null) {
				return;
			}

			mino.tick(tetris, control);

			if (mino.isOnGround() || !mino.isChanged()) {
				valid = false;
				return;
			}
		}

		public boolean isValid() {
			return this.valid;
		}

		public TetrisBoard getTetris() {
			return tetris;
		}

		public Tetrimino getMino() {
			return mino;
		}

		public int getCost(Tetrimino goal) {
			int l1 = Math.abs(getMino().posX - goal.posX) + Math.abs(getMino().posY - goal.posY);
			int mcost = moveCost(control);
			int stcost;
			if ((getMino().state + 2) % 4 == goal.state) {
				stcost = 2;
			} else if (goal.state == getMino().state) {
				stcost = 0;
			} else {
				stcost = 1;
			}
			if (l1 == 0)
				return 0;
			else
				return l1 * 10 + stcost * 5 + mcost;
		}

		private int moveCost(Control move) {
			switch (move) {
			case Drop:
				return 10;
			case Lrotate:
			case Rrotate:
				return 9;
			case Lslide:
			case Rslide:
				return 8;
			case Wait:
				return 0;
			case HardDrop:
			case Hold:
			default:
				return 30;
			}
		}

		@Override
		public String toString() {
			return getMino().toString() + " : " + control + " : " + tetris.getGametick();
		}
	}
}
