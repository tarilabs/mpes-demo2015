package net.tarilabs.mpes.demo2015;

import org.apache.camel.builder.RouteBuilder;

public class MyRoutes extends RouteBuilder {

	@Override
	public void configure() throws Exception {
		from("xbeeapi://?baud=9600&tty=COM15")
			.routeId("route-xbeeapi")
			.to("seda:xbeeAsyncBuffer");
	
		from("seda:xbeeAsyncBuffer")
			.routeId("route-xbeeAsyncBuffer")
			.to("bean:ruleengine");
			
		from("direct:sentence")
			.routeId("route-sentence")
			.log("${body}")
			.to("seda:twitterAsync");
		
		from("seda:twitterAsync")
			.routeId("route-twitterAsync")
			.setBody(simple("[TEST] ${body}"))
			.to("twitter://timeline/user?"
					+ "consumerKey=" + System.getProperty("twitter.consumerKey")
					+ "&consumerSecret=" + System.getProperty("twitter.consumerSecret")
					+ "&accessToken=" + System.getProperty("twitter.accessToken")
					+ "&accessTokenSecret=" + System.getProperty("twitter.accessTokenSecret"));
			
	}

}
