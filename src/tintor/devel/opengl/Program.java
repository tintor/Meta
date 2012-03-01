package tintor.devel.opengl;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import javax.media.opengl.GL;

public class Program {
	private final GL gl;
	private final int program;

	public Program(final GL gl) {
		this.gl = gl;
		program = gl.glCreateProgram();
	}

	public void attachVertexShader(final File source) {
		attachShader(GL.GL_VERTEX_SHADER, read(source));
	}

	public void attachFragmentShader(final File source) {
		attachShader(GL.GL_FRAGMENT_SHADER, read(source));
	}

	private String read(final File file) {
		final StringBuilder builder = new StringBuilder();
		try {
			final Reader reader = new FileReader(file);
			final char[] buffer = new char[1024];
			while (true) {
				final int len = reader.read(buffer);
				if (len <= 0) break;
				builder.append(buffer, 0, len);
			}
		}
		catch (final IOException e) {
			throw new RuntimeException(e);
		}
		return builder.toString();
	}

	private void attachShader(final int type, final String source) {
		final int shader = gl.glCreateShader(type);
		gl.glShaderSource(shader, 1, new String[] { source }, new int[] { source.length() }, 0);
		gl.glCompileShader(shader);
		gl.glAttachShader(program, shader);

		final int[] length = new int[1];
		final byte[] buffer = new byte[1024];
		gl.glGetInfoLogARB(shader, buffer.length, length, 0, buffer, 0);
		if (length[0] > 0) System.out.println(source + ": " + new String(buffer, 0, length[0]));

		gl.glDeleteShader(shader);
	}

	public void link() {
		gl.glLinkProgram(program);
		gl.glValidateProgram(program);

		final int[] length = new int[1];
		final byte[] buffer = new byte[1024];
		gl.glGetInfoLogARB(program, buffer.length, length, 0, buffer, 0);
		if (length[0] > 0) System.out.println(new String(buffer, 0, length[0]));
	}

	public void use() {
		gl.glUseProgram(program);
	}

	public void delete() {
		gl.glDeleteProgram(program);
	}

	public int uniform(final String name) {
		return gl.glGetUniformLocation(program, name);
	}

	public int attrib(final String name) {
		return gl.glGetAttribLocation(program, name);
	}
}