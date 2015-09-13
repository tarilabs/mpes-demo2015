package net.tarilabs.mpes.demo2015;

import static org.apache.camel.builder.Builder.body;
import static org.junit.Assert.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.main.Main;
import org.drools.core.time.SessionPseudoClock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.kie.api.runtime.KieSession;

import com.rapplogic.xbee.api.XBeeAddress64;
import com.rapplogic.xbee.api.zigbee.ZNetRxIoSampleResponse;
import com.rapplogic.xbee.util.IIntArrayInputStream;

public class AllRulesTest {
	private static final long JUNIT_TIMEOUT = 30_000L;
	private PseudoRuleEngine reInstance;

	@Before
	public void init() throws Exception {
		reInstance = new PseudoRuleEngine();
	}
	
	@After
	public void shutdown() throws Exception {
		reInstance.getSession().dispose();
	}
	
	@Test(timeout=JUNIT_TIMEOUT)
	public void testFilter01() throws IOException {
		ZNetRxIoSampleResponse r = makeAnalogResponse(30);
		
		//bad
		ZNetRxIoSampleResponse r2 = new ZNetRxIoSampleResponse();
		r2.setRemoteAddress64(new XBeeAddress64(0x00,0x13,0xa2,0x00,0x40,0x68,0xe0,0x99));
		r2.setAnalog1(30);

		getKsession().insert(r);
		getKsession().insert(r2);
		getKsession().fireAllRules();
		
		assertNotNull("I was expecting to still find r in the working memory.", getKsession().getFactHandle(r));
		assertNull("I was expecting that r2 would have been retracted.", getKsession().getFactHandle(r2));
	}
	
	@Test(timeout=JUNIT_TIMEOUT)
	public void testFilter02() throws IOException {

		ZNetRxIoSampleResponse r = makeAnalogResponse(30);
		
		//bad, as it does not contains any analog reading.
		ZNetRxIoSampleResponse r2 = new ZNetRxIoSampleResponse();
		r2.setRemoteAddress64(new XBeeAddress64(0x00,0x13,0xa2,0x00,0x40,0x68,0xe0,0x95));

		getKsession().insert(r);
		getKsession().insert(r2);
		getKsession().fireAllRules();
		
		assertNotNull("I was expecting to still find r in the working memory.", getKsession().getFactHandle(r));
		assertNull("I was expecting that r2 would have been retracted.", getKsession().getFactHandle(r2));
	}
	
	@Test(timeout=JUNIT_TIMEOUT)
	public void testExpiration() throws IOException {
		ZNetRxIoSampleResponse r = makeAnalogResponse(30);

		getKsession().insert(r);
		getKsession().fireAllRules();
		assertNotNull("I was expecting to still find r in the working memory.", getKsession().getFactHandle(r));
		
		SessionPseudoClock clock = getKsession().getSessionClock();
		clock.advanceTime(120, TimeUnit.MINUTES);
		getKsession().fireAllRules();
		
		assertNull("I was expect r retracted for expiration.", getKsession().getFactHandle(r));
	}
	
	/**
	 * There cannot be a test for Detect Undocked 
	 * @throws IOException
	 */
	@Test(timeout=JUNIT_TIMEOUT)
	public void testDetectUndockedIntegration() throws IOException {
		getKsession().fireAllRules();
		
		assertEquals("Still no sensor reading in the working mem, not yet enough sensor reading to tell the status", 0, countUnDocked());
		
		advanceTime(10, TimeUnit.SECONDS);
		getKsession().insert(makeAnalogResponse(1023));
		getKsession().fireAllRules();
		assertEquals("Not yet enough sensor reading to tell the status", 0, countUnDocked());
		
		advanceTime(1, TimeUnit.SECONDS);
		getKsession().insert(makeAnalogResponse(181));
		getKsession().fireAllRules();
		assertEquals("Not yet enough sensor reading to tell the status", 0, countUnDocked());
		
		advanceTime(1, TimeUnit.SECONDS);
		getKsession().insert(makeAnalogResponse(181));
		getKsession().fireAllRules();
		assertEquals("Not yet enough sensor reading to tell the status", 0, countUnDocked());
		
		advanceTime(1, TimeUnit.SECONDS);
		getKsession().insert(makeAnalogResponse(181));
		getKsession().fireAllRules();
		assertEquals("Should be detected as UNDOCKED now.", 1, countUnDocked());
	}
	
	private long countUnDocked() {
		return getKsession().getObjects(o -> o.getClass().getSimpleName().equals("UnDockedEvt")).size();
	}

	/**
	 * There cannot be a test for Detect Docked 
	 * @throws IOException
	 */
	@Test(timeout=JUNIT_TIMEOUT)
	public void testDetectDockedIntegration() throws IOException {
		getKsession().fireAllRules();
		
		assertEquals("Still no sensor reading in the working mem,  not yet enough sensor reading to tell the status", 0, countDocked());
		
		advanceTime(10, TimeUnit.SECONDS);
		getKsession().insert(makeAnalogResponse(181));
		getKsession().fireAllRules();
		assertEquals("Not yet enough sensor reading to tell the status", 0, countDocked());
		
		advanceTime(1, TimeUnit.SECONDS);
		getKsession().insert(makeAnalogResponse(1023));
		getKsession().fireAllRules();
		assertEquals("Not yet enough sensor reading to tell the status", 0, countDocked());
		
		advanceTime(1, TimeUnit.SECONDS);
		getKsession().insert(makeAnalogResponse(1023));
		getKsession().fireAllRules();
		assertEquals("Not yet enough sensor reading to tell the status", 0, countDocked());
		
		advanceTime(1, TimeUnit.SECONDS);
		getKsession().insert(makeAnalogResponse(1023));
		getKsession().fireAllRules();
		assertEquals("Should be detected as DOCKED now.", 1, countDocked());
	}
	
	private Object countDocked() {
		return getKsession().getObjects(o -> o.getClass().getSimpleName().equals("DockedEvt")).size();
	}

	/**
	 * Testing only by single rule and manual override insert object into working memory.
	 * @throws Exception 
	 */
	@Test(timeout=JUNIT_TIMEOUT)
	public void testToothbrushSentence() throws Exception {
		getKsession().fireAllRules();
		
		Main main = MyMain.getInstance();
		main.bind("ruleengine", reInstance);
		main.addRouteBuilder(new RouteBuilder() {
			@Override
			public void configure() throws Exception {
				from("direct:sentence")
					.routeId("route-sentence")
					.log("${body}")
					.to("log:headers?showAll=true");
			}
		});
		main.start();
		NotifyBuilder camelNotify = new NotifyBuilder(main.getCamelContexts().get(0))
				.from("direct:sentence").whenAllDoneMatches(body().contains("I just used my toothbrush!"))
				.create();
		
		assertEquals("Still no sensor reading in the working mem,  not yet enough sensor reading to tell the status", countUnDocked(), 0);
		
		advanceTime(10, TimeUnit.SECONDS);
		getKsession().insert(makeAnalogResponse(1023));
		getKsession().fireAllRules();
		assertEquals("Not yet enough sensor reading to tell the status", countUnDocked(), 0);
		
		int i = 0;
		while (i < 30) {
			advanceTime(1, TimeUnit.SECONDS);
			getKsession().insert(makeAnalogResponse(181));
			getKsession().fireAllRules();
			i++;
		}
		
		i = 0;
		while (i < 3) {
			advanceTime(1, TimeUnit.SECONDS);
			getKsession().insert(makeAnalogResponse(1023));
			getKsession().fireAllRules();
			i++;
		}
		
		assertTrue("I was expecting Toothbrush sentence", camelNotify.matches());
		
	}
	
	
	private long advanceTime(int amount, TimeUnit unit) {
		SessionPseudoClock clock = getKsession().getSessionClock();
		return clock.advanceTime(amount, unit);
	}

	private KieSession getKsession() {
		return reInstance.getSession();
	}

	private ZNetRxIoSampleResponse makeAnalogResponse(int analog1) throws IOException {
		ZNetRxIoSampleResponse r = new ZNetRxIoSampleResponse();
		r.setRemoteAddress64(new XBeeAddress64(0x00,0x13,0xa2,0x00,0x40,0x68,0xe0,0x95));
		r.parse( new IIntArrayInputStream() {
			@Override
			public int read(String arg0) throws IOException {
				if (arg0.equals("ZNet RX IO Sample Size")) {
					return 1;
				}
				if (arg0.equals("ZNet RX IO Sample Analog Channel Mask")) {
					// this should map to analog[1]
					return 2;
				}
				return 0;
			}		
			@Override
			public int read() throws IOException {
				return 0;
			}
		});
		r.setAnalog1(analog1);
		return r;
	}
}