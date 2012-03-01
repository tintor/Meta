package tintor.devel.compiler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.generic.BranchHandle;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.InstructionConstants;
import org.apache.bcel.generic.InstructionFactory;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.Type;

import tintor.devel.compiler.scanner.Scanner;

public class Main {
	public static void main(final String[] args) throws Exception {
		//Grammar.trace = true;
		final InputStream input = new FileInputStream(new File(bin, "gcd.txt"));
		System.out.println(compile(input, "tintor.devel.compiler.Memory").getMethod("main").invoke(null));
	}

	static Class<?> compile(final InputStream input, final String className) {
		return builder.build(className, parser.parse(input));
	}

	static File bin = new File("bin" + File.separator
			+ Main.class.getPackage().getName().replace('.', File.separatorChar));

	static Parser parser = new Parser();
	static Builder builder = new Builder();
}

class Parser {
	private final Grammar grammar = new Grammar(new File(Main.bin, "grammar.txt")) {
		@Override
		public Matcher.Result format(final String name, final int position, final Matcher.Result p) {
			// TODO Auto-generated method stub
			return super.format(name, position, p);
		}

		@Override
		public Matcher getGeneric(final String name) {
			assert name.length() == 1;
			return new Matcher() {
				@Override
				public Result match(final int position) {
					final XNode[] a = tokens.get();
					return position < a.length && a[position].name == name ? new Result(position + 1) : null;
				}
			};
		}
	};

	private static final ThreadLocal<XNode[]> tokens = new ThreadLocal<XNode[]>();

	public XNode parse(final InputStream input) {
		final Scanner sc = new Scanner(input);
		sc.symbols("if", "def");
		sc.operators("+-*{}<>=,()");
		try {
			sc.scan();
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
		tokens.set(sc.tokens.toArray(new XNode[sc.tokens.size()]));

		final XNode parseTree = (XNode) grammar.parse();
		tokens.set(null);

		System.out.println(parseTree);
		return parseTree;
	}
}

class Builder {
	final Map<String, MethodBuilder> functions = new HashMap<String, MethodBuilder>();

	public Class<?> build(final String className, final XNode parseTree) {
		final ClassGen cg = new ClassGen(className, "java.lang.Object", null, Constants.ACC_PUBLIC
				| Constants.ACC_SUPER, null);

		final MethodBuilder builder = new MethodBuilder(cg, "main");
		functions.put("main", builder);
		builder.codeTree(parseTree);
		builder.finish();

		functions.clear();
		return loadClass(cg.getJavaClass());
	}

	private static Class<?> loadClass(final JavaClass javaClass) {
		try {
			javaClass.dump("bin" + File.separator + javaClass.getClassName().replace('.', File.separatorChar)
					+ ".class");
			return Class.forName(javaClass.getClassName());
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	class MethodBuilder {
		public final int argumentCount;
		private final ClassGen cg;
		private final MethodGen mg;
		private final InstructionList il;
		private final InstructionFactory factory;
		private final Map<String, Integer> variables = new HashMap<String, Integer>();

		public MethodBuilder(final ClassGen cg, final String name, final String... argumentNames) {
			this.cg = cg;
			argumentCount = argumentNames.length;

			final Type[] types = new Type[argumentNames.length];
			for (int i = 0; i < argumentNames.length; i++) {
				types[i] = Type.INT;
				variables.put(argumentNames[i], variables.size());
			}

			mg = new MethodGen(Constants.ACC_STATIC | Constants.ACC_PUBLIC, Type.INT, types, argumentNames, name,
					cg.getClassName(), new InstructionList(), cg.getConstantPool());

			il = mg.getInstructionList();
			factory = new InstructionFactory(cg);
		}

		private void codeBinOp(final String op, final XNode expr) {
			codeTree(expr);
			if (op == "plus")
				il.append(InstructionConstants.IADD);
			else if (op == "minus")
				il.append(InstructionConstants.ISUB);
			else if (op == "star")
				il.append(InstructionConstants.IMUL);
			else if (op == "less" || op == "greater" || op == "equals") {
				short opcode = -1;
				if (op == "less") opcode = Constants.IF_ICMPLT;
				if (op == "greater") opcode = Constants.IF_ICMPGT;
				if (op == "equals") opcode = Constants.IF_ICMPEQ;

				final BranchHandle a = il.append(InstructionFactory.createBranchInstruction(opcode, null));

				il.append(factory.createConstant(0));
				final BranchHandle c = il.append(InstructionFactory.createBranchInstruction(Constants.GOTO,
						null));

				a.setTarget(il.append(factory.createConstant(1)));
				c.setTarget(il.append(InstructionConstants.NOP));
			} else
				throw new RuntimeException("unknown op " + op);
		}

		public void codeTree(final XNode tree) {
			if (tree.name.startsWith("expr")) {
				assert tree.size() % 2 == 1;
				codeTree(tree.node(0));
				for (int i = 1; i < tree.size(); i += 2)
					codeBinOp(tree.node(i).name, tree.node(i + 1));
			} else if (tree.name == "if") {
				assert tree.size() == 3;
				codeIf(tree.node(0), tree.node(1), tree.node(2));
			} else if (tree.name == "unary") {
				assert tree.size() == 2 && tree.node(0).name == "minus";
				codeTree(tree.node(1));
				il.append(InstructionConstants.INEG);
			} else if (tree.name == "int") {
				assert tree.size() == 1;
				final int value = Integer.parseInt(tree.text(0));
				il.append(factory.createConstant(value));
			} else if (tree.name == "code") {
				for (int i = 0; i < tree.size(); i++)
					codeTree(tree.node(i));
			} else if (tree.name == "assign") {
				assert tree.size() == 2;
				assert tree.node(0).name == "name" || tree.node(0).name == "func";

				if (tree.node(0).name == "name") {
					codeTree(tree.node(1));
					final String var = tree.node(0).text(0);
					if (!variables.containsKey(var)) variables.put(var, variables.size());
					il.append(InstructionFactory.createStore(Type.INT, variables.get(var)));
				} else if (tree.node(0).name == "func") {
					final XNode func = tree.node(0);
					final XNode name = func.node(0);
					assert name.name == "name";
					if (functions.containsKey(name.text(0)))
						throw new RuntimeException("function '" + name.text(0) + "' already exists");

					final String[] args = new String[func.size() - 1];
					for (int i = 0; i < func.size() - 1; i++) {
						assert func.node(i + 1).name == "name";
						args[i] = func.node(i + 1).text(0);
					}

					final MethodBuilder builder = new MethodBuilder(cg, name.text(0), args);
					functions.put(name.text(0), builder);
					builder.codeTree(tree.node(1));
					builder.finish();
				}
			} else if (tree.name == "func") {
				assert tree.size() > 1;
				assert tree.node(0).name == "name";

				final String className = cg.getClassName();
				final String funcName = tree.node(0).text(0);

				final Type[] args = new Type[tree.size() - 1];
				for (int i = 0; i < tree.size() - 1; i++) {
					codeTree(tree.node(i + 1));
					args[i] = Type.INT;
				}

				il.append(factory.createInvoke(className, funcName, Type.INT, args, Constants.INVOKESTATIC));

			} else if (tree.name == "name") {
				assert tree.size() == 1;
				final String var = tree.text(0);
				if (!variables.containsKey(var))
					throw new RuntimeException("compile eror: var " + var + " is undefined");
				il.append(InstructionFactory.createLoad(Type.INT, variables.get(var)));
			} else
				throw new RuntimeException("unknown " + tree.name);
		}

		private void codeIf(final XNode cond, final XNode first, final XNode second) {
			codeTree(cond);
			final BranchHandle a = il.append(InstructionFactory.createBranchInstruction(Constants.IFEQ, null));

			codeTree(first);
			final BranchHandle c = il.append(InstructionFactory.createBranchInstruction(Constants.GOTO, null));
			a.setTarget(il.append(InstructionConstants.NOP));

			codeTree(second);
			c.setTarget(il.append(InstructionConstants.NOP));
		}

		public void finish() {
			il.append(InstructionConstants.IRETURN);

			mg.setMaxStack();
			mg.setMaxLocals();
			cg.addMethod(mg.getMethod());
			il.dispose();
		}
	}
}