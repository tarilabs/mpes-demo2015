package net.tarilabs.mpes.demo2015;

import java.io.PrintStream;
import org.apache.camel.main.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Demo {
	final static Logger LOG = LoggerFactory.getLogger(Demo.class);

	private static void setupCtx(Main main) throws Exception {
		main.bind("ruleengine", MySimpleRuleEngine.getInstance());
		main.addRouteBuilder(new MyRoutes());
	}

	public static void main(String[] args) throws Exception {
		Main main = MyMain.getInstance();
		setupCtx(main);

		System.out.println("Starting Camel. Use ctrl + c to terminate the JVM.\n");
		main.run();
	}

	public static void start(String[] args) throws Exception {
		LOG.info("start");
		System.setOut(new PrintStream(System.out) {
			public void print(String s) {
				LOG.info(s);
			}
		});
		System.setErr(new PrintStream(System.err) {
			public void print(String s) {
				LOG.error(s);
			}
		});
		System.out.println("console output start");

		Main main = MyMain.getInstance();
		setupCtx(main);

		LOG.info("Starting Camel. Use ctrl + c to terminate the JVM.\n");
		main.run();
	}

	public static void stop(String[] args) throws Exception {
		LOG.info("stopping");
		Main instance = MyMain.getInstance();

		instance.shutdown();
		LOG.info("shutdown() returned.");

		while (!instance.isStopped()) {
			LOG.info("waiting for camel to stop...");
			Thread.sleep(1000);
		}
		LOG.info("Camel stopped.");
		LOG.info("stop() shutdown sequence completed.");
	}
}
