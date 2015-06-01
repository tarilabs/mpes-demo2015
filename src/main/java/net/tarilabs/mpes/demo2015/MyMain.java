package net.tarilabs.mpes.demo2015;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContextNameStrategy;
import org.apache.camel.main.Main;


public class MyMain extends Main {

	private MyMain() {
		super();
	}
	
	@Override
	protected CamelContext createContext() {
		LOG.info("createContext() with NameStrategy");
		CamelContext ret = super.createContext();
		ret.setNameStrategy(new DefaultCamelContextNameStrategy("camel-mpes-demo2015"));
		return ret;
	}
	
	public static Main getInstance() {
        if (instance == null) {
        	synchronized (MyMain.class) {
        		if (instance == null) {
        			instance = new MyMain();
        		}
			}
        }
        return instance;
    }

}
