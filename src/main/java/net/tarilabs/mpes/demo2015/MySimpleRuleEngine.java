package net.tarilabs.mpes.demo2015;

import org.apache.camel.Exchange;
import org.kie.api.KieBase;
import org.kie.api.KieBaseConfiguration;
import org.kie.api.KieServices;
import org.kie.api.builder.Message;
import org.kie.api.builder.Results;
import org.kie.api.conf.EventProcessingOption;
import org.kie.api.definition.KiePackage;
import org.kie.api.definition.rule.Rule;
import org.kie.api.event.rule.DefaultRuleRuntimeEventListener;
import org.kie.api.event.rule.ObjectDeletedEvent;
import org.kie.api.event.rule.ObjectInsertedEvent;
import org.kie.api.event.rule.ObjectUpdatedEvent;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.KieSessionConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum MySimpleRuleEngine {
	INSTANCE;
	
	private final Logger LOG = LoggerFactory.getLogger(MySimpleRuleEngine.class);
	private KieSession session;
	private Thread reThread;
	
	MySimpleRuleEngine() { 
    	KieServices kieServices = KieServices.Factory.get();
        
		KieContainer kContainer = kieServices.getKieClasspathContainer();
        Results verifyResults = kContainer.verify();
        for (Message m : verifyResults.getMessages()) {
        	LOG.info("{}", m);
        }
        
        LOG.info("Creating kieBase with STREAM option");
        KieBaseConfiguration kieBaseConf = kieServices.newKieBaseConfiguration();
		kieBaseConf.setOption( EventProcessingOption.STREAM );
        KieBase kieBase = kContainer.newKieBase(kieBaseConf);
        
        LOG.info("There should be rules: ");
        for ( KiePackage kp : kieBase.getKiePackages() ) {
        	for (Rule rule : kp.getRules()) {
        		LOG.info("kp " + kp + " rule " + rule.getName());
        	}
        }

        LOG.info("Creating kieSession");
        KieSessionConfiguration config = kieServices.newKieSessionConfiguration();
//		config.setOption( ClockTypeOption.get("pseudo") );
        session = kieBase.newKieSession(config, null);
//        SessionPseudoClock clock = session.getSessionClock();
        session.addEventListener(new DefaultRuleRuntimeEventListener() {@Override
			public void objectDeleted(ObjectDeletedEvent event) {
				LOG.debug("{} {} {}", new Object[]{event.getClass().getSimpleName(), "x", event.getOldObject()});
			}
			@Override
			public void objectInserted(ObjectInsertedEvent event) {
				LOG.debug("{} {} {}", new Object[]{event.getClass().getSimpleName(), event.getObject(), "x"});
			}

			@Override
			public void objectUpdated(ObjectUpdatedEvent event) {
				LOG.debug("{} {} {}", new Object[]{event.getClass().getSimpleName(), event.getObject(), event.getOldObject()});
			}
        });
        
        
        LOG.info("Populating globals");
        Logger globalLOG = LoggerFactory.getLogger("RE");
        session.setGlobal("LOG", globalLOG);
                
        LOG.info("Setup completed, now fireUntilHalt()");
        Runnable taskRE = () -> { session.fireUntilHalt(); };
        reThread = new Thread(null, taskRE, "thread-RE");
        reThread.start();
	}

    public static MySimpleRuleEngine getInstance() {
        return INSTANCE;
    }
    
    public void process(Exchange exchange) {
    	Object body = exchange.getIn().getBody();
    	LOG.trace("About to insert: {}", body);
		session.insert(body);
    }
}
