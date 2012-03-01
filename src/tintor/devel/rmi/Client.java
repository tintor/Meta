package tintor.devel.rmi;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Client {
	public static void main(final String[] args) throws Exception {
		final String host = args.length < 1 ? null : args[0];
		final Registry registry = LocateRegistry.getRegistry(host);
		final Hello stub = (Hello) registry.lookup("Hello");
		final String response = stub.sayHello();
		System.out.println("response: " + response);
	}
}