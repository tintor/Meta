package tintor.devel;

import java.io.File;

public class Path {
	public static File get(final Class<?> clazz, final String filename) {
		return new File("bin/" + clazz.getPackage().getName().replace('.', '/'), filename);
	}
}