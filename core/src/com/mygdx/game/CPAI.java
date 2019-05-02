package com.mygdx.game;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

import com.mygdx.game.TetrisNode.Node;

class CPAI extends Controler {

	public static final int POPULATION = 200;
	public static final double ELITE = 0.05;
	private static final int TEACHER_CHEAT = 0;

	private ArrayList<Node> controlPlan = new ArrayList<Node>();
	private ArrayList<Evaluater> evaluaters = new ArrayList<Evaluater>();
	private Evaluater evaluater;
	private int ctrIndex = 0;
	private int evaIndex = 0;
	private int generation = 1;
	private boolean teacher;

	public CPAI(boolean teacher, boolean loadsave) {
		this.teacher = teacher;
		if (teacher) {
			if (loadsave) {
				try {
					evaluater = new Evaluater(new File("gene.csv"));
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				evaluater = new SimpleEvaluater();
			}
		} else {
			if (loadsave) {
				try {
					evaluater = new Evaluater(new File("gene.csv"));
					for(int i=0;i<5;i++)
						evaluaters.add(evaluater);
					while (evaluaters.size() < POPULATION) {
						evaluaters.add(new Evaluater(evaluater, evaluater));
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				evaluater = new Evaluater();
				evaluaters.add(evaluater);
				while (evaluaters.size() < POPULATION) {
					evaluaters.add(new Evaluater());
				}
			}
		}
	}

	@Override
	public Control control(TetrisBoard tetris) {
		try {
			if (!tetris.isPlayable()) {
				return Control.Wait;
			}
			if (controlPlan.isEmpty()) {
				return Control.Wait;
			}
			if (ctrIndex >= controlPlan.size()) {
				return Control.HardDrop;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return Control.Wait;
		}

		return controlPlan.get(ctrIndex++).control;
	}

	@Override
	public void init(TetrisBoard tetris) {
		if (teacher)
			tetris.setCheat(TEACHER_CHEAT);
	}

	@Override
	public void refresh(TetrisBoard tetris, Tetrimino result) {
		// if (!controlPlan.isEmpty() && !teacher) {
		// System.out.println("dest: " + controlPlan.get(controlPlan.size() -
		// 1).toString());
		// System.out.println("result: " + result + " : " +
		// tetris.getGametick());
		// }
		// System.out.println();
		if (controlPlan.size() - ctrIndex != 0 && !teacher) {
			System.err.println("control err " + (controlPlan.size() - ctrIndex));
			System.out.println("dest: " + controlPlan.get(controlPlan.size() - 1).toString());
			for (int i = 0; i < controlPlan.size(); i++) {
				if (i == ctrIndex) {
					System.out.println(" : : : : : ");
					System.out.println("result:" + tetris.getMino());
					System.out.println(" : : : : : ");
				}
				System.out.println(controlPlan.get(i));
			}
		}

		try {
			TetrisNode tetrisNode = new TetrisNode(tetris, evaluater);
			this.controlPlan = tetrisNode.getBestControl();
			this.ctrIndex = 0;
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void gameset(TetrisBoard tetris) {

		this.ctrIndex = 0;
		this.controlPlan.clear();

		if (teacher)
			return;

		System.out.println("student" + evaIndex + ": " + tetris.isWin() + "  " + tetris.score + "	" + tetris);
		evaluater.setScore(tetris);

		if (evaIndex >= evaluaters.size()) {
			Collections.sort(evaluaters, new Comparator<Evaluater>() {
				@Override
				public int compare(Evaluater o1, Evaluater o2) {
					return o2.getScore() - o1.getScore();
				}
			});
			 tetris.enemy.controler.appyEvaluater(evaluaters.get(0));
			int ave = 0;
			for (Evaluater eva : evaluaters) {
				ave += eva.getScore();
			}
			ave /= evaluaters.size();

			ArrayList<Evaluater> elite = new ArrayList<Evaluater>(
					evaluaters.subList(0, (int) (evaluaters.size() * ELITE)));

			System.out.println("GENERATION: [" + (generation) + "]  AVE: " + ave);
			System.out.println("NEXT GENERATION: [" + (++generation) + "]");
			System.out.println(Arrays.toString(elite.toArray()));

			try {
				File file = new File("log/" + Tetris.DATE + ".csv");
				boolean first = false;
				if (!file.exists()) {
					System.out.println(file.getAbsolutePath());
					file.createNewFile();
					first = true;
				}
				if (file.exists() && file.isFile() && file.canWrite()) {
					BufferedWriter bf = new BufferedWriter(new FileWriter(file, true));
					if (first) {
						bf.write("\"ave\",\"max\"");
						bf.newLine();
					}
					bf.write(ave + "," + elite.get(0).getScore());
					bf.newLine();
					bf.close();
				} else {
					System.out.println("ファイルに書き込めません");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

			evaluaters = new ArrayList<Evaluater>(elite);

			while (evaluaters.size() < POPULATION) {
				int p1 = (int) (Math.random() * elite.size());
				int p2 = (int) (Math.random() * elite.size());
				evaluaters.add(new Evaluater(elite.get(p1), elite.get(p2)));
			}

			evaIndex = 0;
		}

		evaluater = evaluaters.get(evaIndex);
		evaIndex++;

	}

	@Override
	public void save() {
		try {
			System.out.println("save: " + evaluaters.get(0));
			evaluaters.get(0).writeGene();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	@Override
	public void appyEvaluater(Evaluater evaluater) {
		this.evaluater = evaluater;
	}

}