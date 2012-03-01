package tintor.devel.rmi;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class Server implements Hello {
	public String sayHello() {
		return "Hello, world!";
	}

	public static void main(final String args[]) throws Exception {
		final Hello stub = (Hello) UnicastRemoteObject.exportObject(new Server(), 0);

		final Registry registry = LocateRegistry.getRegistry();
		registry.bind("Hello", stub);

		System.err.println("Server ready");
	}
}