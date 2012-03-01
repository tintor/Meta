package tintor.devel.minesweeper;

import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.peer.RobotPeer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import sun.awt.ComponentFactory;

public class Minesweeper {
	static final int delay = 0;

	public static void main(String[] args) throws Exception {
		final Process process = Runtime.getRuntime().exec("winmine");
		init();
		type(KeyEvent.VK_ALT);
		type(KeyEvent.VK_G);
		type(KeyEvent.VK_E);
		// type(KeyEvent.VK_C);
		// type(KeyEvent.VK_TAB);
		// type(KeyEvent.VK_TAB);
		// type(KeyEvent.VK_TAB);
		// type(KeyEvent.VK_TAB);
		// type(KeyEvent.VK_4);
		// type(KeyEvent.VK_0);
		// type(KeyEvent.VK_ENTER);
		// type(KeyEvent.VK_F2);ge
		findMap();

		while (state != null)
			state = think();
	}

	static State think() {
		switch (state) {
		case Start:
			try {
				openRandom();
			} catch (DeadException e) {
				reset();
				return State.Start;
			}
			if (empties == 0 && unknowns > 0) return State.Start;
			return State.Trivial;
		case Trivial:
			if (trivial()) return State.Trivial;
			refresh();
			if (unknowns == 0) return null;
			if (trivial()) return State.Trivial;

			System.out.println("no more ideas! guessing :(");
			try {
				openRandom();
			} catch (DeadException e) {
				reset();
				return State.Start;
			}
			return State.Trivial;
		}
		throw new RuntimeException();
	}

	enum State {
		Start, Trivial;
	}

	static State state = State.Start;

	static class DeadException extends RuntimeException {
	}

	static boolean trivial() {
		for (Point p : numbers) {
			int u = around(p.x, p.y, '#');
			if (u == 0) continue;

			int f = around(p.x, p.y, '!');
			int d = map[p.x][p.y] - '0';

			// safe flags
			if (d == u + f) {
				for (int i = p.x - 1; i <= p.x + 1; i++)
					for (int j = p.y - 1; j <= p.y + 1; j++)
						if (isValid(i, j) && map[i][j] == '#') flagCell(i, j);
				refreshFast(p.x, p.y, 3);
				return true;
			}

			// safe cells
			if (d == f) {
				for (int i = p.x - 1; i <= p.x + 1; i++)
					for (int j = p.y - 1; j <= p.y + 1; j++)
						if (isValid(i, j) && map[i][j] == '#') openCell(i, j);
				refreshFast(p.x, p.y, 3);
				return true;
			}
		}
		return false;
	}

	static int around(int x, int y, char c) {
		int a = 0;
		for (int i = x - 1; i <= x + 1; i++)
			for (int j = y - 1; j <= y + 1; j++)
				if (isValid(i, j) && map[i][j] == c) a++;
		return a;
	}

	static void openRandom() {
		List<Point> list = new ArrayList<Point>();
		for (int y = 0; y < ycells; y++) {
			for (int x = 0; x < xcells; x++)
				if (map[x][y] == '#') list.add(new Point(x, y));
		}
		Point a = list.get(random.nextInt(list.size()));
		openCell(a.x, a.y);
		refresh();
	}

	static final int Gray = 0xC0C0C0, White = 0xFFFFFF, Dark = 0x808080, Red = 0xFF0000, Black = 0x000000;
	static RobotPeer robot;
	static final Random random = new Random();
	static DisplayMode display;

	static Point board;

	static int xcells, ycells;
	static char[][] map;
	static int flags, unknowns, empties;

	static Map<String, Character> code = new HashMap<String, Character>();

	static {
		code.put("{8421504=31, 12632256=225}", ' ');
		code.put("{255=40, 8421504=31, 12632256=185}", '1');
		code.put("{32768=65, 8421504=31, 12632256=160}", '2');
		code.put("{8421504=31, 12632256=163, 16711680=62}", '3');
		code.put("{128=56, 8421504=31, 12632256=169}", '4');
		code.put("{8388608=70, 8421504=31, 12632256=155}", '5');
		code.put("{32896=72, 8421504=31, 12632256=153}", '6');

		code.put("{8421504=54, 12632256=148, 16777215=54}", '#'); // unknown
		code.put("{0=77, 8421504=31, 12632256=144, 16777215=4}", '*'); // mine
		// code.put("{0=77, 8421504=31, 16711680=144, 16777215=4}", '@'); // exploded mine
		code.put("{0=22, 8421504=54, 12632256=109, 16711680=17, 16777215=54}", '!'); // flag
	}

	static void print() {
		System.out.println("flags = " + flags);
		System.out.println("unknowns = " + unknowns);
		for (int y = 0; y < ycells; y++) {
			for (int x = 0; x < xcells; x++)
				System.out.print(map[x][y]);
			System.out.println();
		}
	}

	static void reset() {
		type(KeyEvent.VK_F2);
		refresh();
	}

	static final List<Point> numbers = new ArrayList<Point>();

	static boolean[][] bad = new boolean[40][40];
	static Map<Object, Map<Integer, Character>> pixel = new HashMap<Object, Map<Integer, Character>>();
	static int goodpixels = 256;

	static void read(int x, int y) {
		if (getCellColor(x, y, 15, 15) == Red) {
			// exploded mine
			throw new DeadException();
		}

		String s = histogram(x, y).toString();
		if (!code.containsKey(s)) throw new RuntimeException("unknown cell at " + x + "," + y + "  " + s);
		char c = code.get(s);

		if (('1' <= c && c <= '8') || c == '!' || c == ' ') for (int i = 0; i < 16; i++)
			for (int j = 0; j < 16; j++)
				if (!bad[i][j]) {
					Object key = Arrays.asList(i, j);
					int color = getCellColor(x, y, i, j);
					if (pixel.containsKey(key)) {
						Map<Integer, Character> m = pixel.get(key);
						if (m.containsKey(color)) {
							if (m.get(color) != c) {
								bad[i][j] = true;
								goodpixels--;
							}
						} else {
							m.put(color, c);
						}
					} else {
						Map<Integer, Character> m = new HashMap<Integer, Character>();
						m.put(color, c);
						pixel.put(key, m);
					}
				}
		System.out.println(goodpixels);
		for (int i = 0; i < 16; i++)
			for (int j = 0; j < 16; j++) {
				if (!bad[i][j]) {
					System.out.println(i + "," + j + " " + pixel.get(Arrays.asList(i, j)));
					i = j = 16;
				}
			}

		if (c == '*') throw new DeadException();
		if (c == '!') flags++;
		if (c == '#') unknowns++;
		if (c == ' ') empties++;
		if ('1' <= c && c <= '8') numbers.add(new Point(x, y));
		map[x][y] = c;

	}

	static void refreshFast(int ix, int iy, int p) {
		numbers.clear();
		flags = unknowns = empties = 0;
		for (int x = ix - p; x <= ix + p; x++)
			for (int y = iy - p; y <= iy + p; y++)
				if (isValid(x, y)) read(x, y);
	}

	static void refresh() {
		numbers.clear();
		flags = unknowns = empties = 0;
		for (int x = 0; x < xcells; x++)
			for (int y = 0; y < ycells; y++)
				read(x, y);
	}

	static Map<Integer, Integer> histogram(int x, int y) {
		validate(x, y);
		Map<Integer, Integer> map = new HashMap<Integer, Integer>();
		for (int dx = 0; dx < 16; dx++)
			for (int dy = 0; dy < 16; dy++) {
				int c = getCellColor(x, y, dx, dy);
				if (!map.containsKey(c))
					map.put(c, 1);
				else
					map.put(c, map.get(c) + 1);
			}
		return new TreeMap<Integer, Integer>(map);
	}

	static int getCellColor(int x, int y, int dx, int dy) {
		validate(x, y);
		if (dx < 0 || dy < 0 || dx >= 16 || dy >= 16) throw new RuntimeException();
		return getColor(board.x + x * 16 + dx, board.y + y * 16 + dy);
	}

	static boolean isValid(int x, int y) {
		return x >= 0 && y >= 0 && x < xcells && y < ycells;
	}

	static void validate(int x, int y) {
		if (x < 0 || y < 0 || x >= xcells || y >= ycells) throw new RuntimeException();
	}

	static void openCell(int x, int y) {
		validate(x, y);
		robot.mouseMove(board.x + 7 + x * 16, board.y + 7 + y * 16);
		robot.mousePress(InputEvent.BUTTON1_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_MASK);
	}

	static void flagCell(int x, int y) {
		validate(x, y);
		robot.mouseMove(board.x + 7 + x * 16, board.y + 7 + y * 16);
		robot.mousePress(InputEvent.BUTTON3_MASK);
		robot.mouseRelease(InputEvent.BUTTON3_MASK);
	}

	static void init() throws Exception {
		GraphicsDevice screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		if (toolkit instanceof ComponentFactory)
			robot = ((ComponentFactory) toolkit).createRobot(null, screen);

		Thread.currentThread().sleep(200);
		// robot.setAutoDelay(delay);
		display = MouseInfo.getPointerInfo().getDevice().getDisplayMode();
	}

	static void findMap() {
		board = findAnyCell();
		Point b = new Point(board.x, board.y);
		while (isCellAt(board.x - 16, board.y))
			board.x -= 16;
		while (isCellAt(board.x, board.y - 16))
			board.y -= 16;

		while (isCellAt(b.x + 16, b.y))
			b.x += 16;
		while (isCellAt(b.x, b.y + 16))
			b.y += 16;

		xcells = (b.x - board.x) / 16 + 1;
		ycells = (b.y - board.y) / 16 + 1;
		map = new char[xcells][ycells];
		refresh();
	}

	static void type(int key) {
		robot.keyPress(key);
		robot.keyRelease(key);
	}

	static Point findAnyCell() {
		for (int i = 0; i < 10000; i++) {
			int x = random.nextInt(display.getWidth()), y = random.nextInt(display.getHeight());
			int c = getColor(x, y);
			if (c == Gray) {
				int a = 0;
				while (a < 12 && x > 0 && getColor(x - 1, y) == Gray) {
					x--;
					a++;
				}
				if (a >= 12) continue;

				int b = 0;
				while (b < 12 && y > 0 && getColor(x, y - 1) == Gray) {
					y--;
					b++;
				}
				if (b >= 12) continue;

				x = x - 2;
				y = y - 2;
				if (isCellAt(x, y)) {
					return new Point(x, y);
				}
			}
		}
		throw new RuntimeException("cell not found");
	}

	static boolean isCellAt(int x, int y) {
		if (getColor(x + 15, y) != Gray) return false;
		if (getColor(x + 14, y + 1) != Gray) return false;
		if (getColor(x + 15, y + 1) != Dark) return false;
		if (getColor(x, y + 14) != White) return false;
		if (getColor(x + 1, y + 14) != Gray) return false;
		if (getColor(x, y + 15) != Gray) return false;

		for (int i = 2; i < 16; i++)
			if (getColor(x + i, y + 14) != Dark) return false;

		for (int i = 1; i < 16; i++)
			if (getColor(x + i, y + 15) != Dark) return false;

		for (int j = 2; j <= 13; j++) {
			if (getColor(x, y + j) != White) return false;
			if (getColor(x + 1, y + j) != White) return false;
			for (int i = 2; i < 14; i++)
				if (getColor(x + i, y + j) != Gray) return false;
			if (getColor(x + 14, y + j) != Dark) return false;
			if (getColor(x + 15, y + j) != Dark) return false;
		}

		for (int i = 0; i < 15; i++)
			if (getColor(x + i, y) != White) return false;

		for (int i = 0; i < 14; i++)
			if (getColor(x + i, y + 1) != White) return false;

		return true;
	}

	static int getColor(int x, int y) {
		return robot.getRGBPixel(x, y) & 0x00FFFFFF;
	}
}