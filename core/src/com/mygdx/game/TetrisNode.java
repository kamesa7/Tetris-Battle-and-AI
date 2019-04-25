package com.mygdx.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.PriorityQueue;

class TetrisNode {
	private ArrayList<Node> path = new ArrayList<Node>();
	private static final int searchLimit = 1000;

	public TetrisNode(TetrisBoard target, Evaluater evaluater) throws CloneNotSupportedException {
		TetrisBoard tetris = target.clone();
		ArrayList<Candidate> candidate = new ArrayList<Candidate>();
		for (int y = -2; y < TetrisBoard.HEIGHT + 2; y++) {
			for (int x = -2; x < TetrisBoard.WIDTH + 2; x++) {
				for (int state = 0; state < 4; state++) {
					if (tetris.getMino().isStopable(tetris.board, state, x, y)) {
						candidate.add(new Candidate(tetris, new Tetrimino(tetris, tetris.getMino().type, x, y, state),
								evaluater));
					}
				}
			}
		}
		Collections.sort(candidate, new Comparator<Candidate>() {
			@Override
			public int compare(Candidate o1, Candidate o2) {
				//System.out.println(o1.getScore()+" "+o2.getScore());
				if (o1.getScore() == o2.getScore())
					return 0;
				else if (o2.getScore() > o1.getScore())
					return 1;
				else
					return -1;
			}
		});
		candidate = (ArrayList<Candidate>) candidate.subList(0, candidate.size());
		for(int i=0;i<candidate.size();i++){
			candidate.get(i).nextCandidate();
		}
		Collections.sort(candidate, new Comparator<Candidate>() {
			@Override
			public int compare(Candidate o1, Candidate o2) {
				//System.out.println(o1.getScore()+" "+o2.getScore());
				if (o1.getScore() == o2.getScore())
					return 0;
				else if (o2.getScore() > o1.getScore())
					return 1;
				else
					return -1;
			}
		});
		// System.out.println("candidate: "+ candidate.size());
		for (int i = 0; i < candidate.size(); i++) {
			path = bestFirstSearch(new Node(null, tetris, tetris.getMino(), Control.Wait), candidate.get(i).getMino());
			if (path.isEmpty())
				continue;
			// System.out.println(candidate.get(i).getScore());
			break;
		}
	}

	public ArrayList<Node> getBestControl() {
		return path;
	}

	private ArrayList<Node> bestFirstSearch(Node start, final Tetrimino goal) throws CloneNotSupportedException {
		// System.out.println("start:" + start);
		// System.out.println("goal :" + goal);
		ArrayList<Node> ret = new ArrayList<Node>();
		// ArrayList<Node> closed = new ArrayList<Node>();
		PriorityQueue<Node> open = new PriorityQueue<Node>(searchLimit, new Comparator<Node>() {
			public int compare(Node o1, Node o2) {
				return o1.getCost(goal) - o2.getCost(goal);
			}
		});
		int count = 0;
		// closed.add(start);
		open.add(start);
		while (!open.isEmpty()) {
			Node node = open.poll();
			// System.out.println(node);
			if (node.getMino().equals(goal)) {
				// System.out.println("goal : " + goal);
				Node tmp = node;
				while (tmp.parent != null) {
					ret.add(tmp);
					tmp = tmp.parent;
				}
				Collections.reverse(ret);
				// ret.remove(0);
				return ret;
			} else if (count++ > searchLimit) {
				// System.err.println("out of limit");
				return ret;
			}
			for (Node exNode : expandNode(node)) {
				// System.out.println(exNode);
				if (exNode.isValid())
					open.add(exNode);
			}
		}
		// System.err.println("impossible to go");
		return ret;
	}

	private ArrayList<Node> expandNode(Node node) throws CloneNotSupportedException {
		ArrayList<Node> ret = new ArrayList<Node>();
		ArrayList<Control> checkCTR = new ArrayList<Control>(Arrays.asList(Control.Drop, Control.Lslide, Control.Rslide,
				Control.Lrotate, Control.Rrotate, Control.Wait));
		// Arrays.asList(Control.values()));
		TetrisBoard currentTetris = node.getTetris().clone();
		currentTetris.addGametick();
		for (Control control : checkCTR) {
			Node tmp = new Node(node, currentTetris, node.getMino().clone(), control);
			if (tmp.isValid())
				ret.add(tmp);
		}
		return ret;
	}

	class Candidate {
		private final Tetrimino mino;
		private double score;
		private TetrisBoard tetris;
		private Evaluater evaluater;

		public Candidate(TetrisBoard tetris, Tetrimino mino, Evaluater evaluater) {
			this.tetris = tetris;
			this.mino = mino;
			this.evaluater = evaluater;
			this.score = evaluater.evaluate(tetris, mino);
		}

		public void nextCandidate() throws CloneNotSupportedException{
			TetrisBoard nextetris = tetris.clone();
			nextetris.setMino(mino);
			mino.tick(nextetris, Control.HardDrop);
			nextetris.tick(Control.Wait);

			ArrayList<Candidate> candidate = new ArrayList<Candidate>();
			for (int y = -2; y < TetrisBoard.HEIGHT + 2; y++) {
				for (int x = -2; x < TetrisBoard.WIDTH + 2; x++) {
					for (int state = 0; state < 4; state++) {
						if (nextetris.getMino().isStopable(nextetris.board, state, x, y)) {
							candidate.add(new Candidate(nextetris, new Tetrimino(nextetris, nextetris.getMino().type, x, y, state),
									evaluater));
						}
					}
				}
			}

			Collections.sort(candidate, new Comparator<Candidate>() {
				@Override
				public int compare(Candidate o1, Candidate o2) {
					//System.out.println(o1.getScore()+" "+o2.getScore());
					if (o1.getScore() == o2.getScore())
						return 0;
					else if (o2.getScore() > o1.getScore())
						return 1;
					else
						return -1;
				}
			});
			this.score = candidate.get(0).getScore();
		}

		public double getScore() {
			return score;
		}



		public Tetrimino getMino() {
			return mino;
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

			if (parent == null) {
				return;
			}

			switch (control) {
			case HardDrop:
			case Hold:
				valid = false;
				return;
			default:
				break;
			}

			mino.tick(tetris, control);

			if (mino.onGround) {
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
			int cost = moveCost(control);
			int stcost = Math.abs(goal.state - getMino().state) >= 3 ? 1 : Math.abs(goal.state - getMino().state);
			if (l1 == 0)
				return 0;
			else
				return l1 * 10 + stcost * 10 + cost;
		}

		private int moveCost(Control move) {
			switch (move) {
			case Drop:
				return 10;
			case Lrotate:
			case Rrotate:
				return 10;
			case Lslide:
			case Rslide:
				return 3;
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
