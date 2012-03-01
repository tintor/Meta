package tintor.devel.deadlockgen;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Locale;

import tintor.devel.deadlockgen.util.Statistics;

public class Search {
	public static void main(final String[] args) {
		Locale.setDefault(Locale.US);
		final Map map = new Map(5, 5);
		map.load("#$$$$$ $ #$   $$  #$$#$  ");
		for (int i = 0; i < 30; i++)
			map.isDeadlock();
		Statistics.dump(new PrintWriter(new OutputStreamWriter(System.out)));
	}
}
