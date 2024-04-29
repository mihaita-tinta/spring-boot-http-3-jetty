package com.example.demo;

import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.http.MetaData;
import org.eclipse.jetty.http3.api.Session;
import org.eclipse.jetty.http3.api.Stream;
import org.eclipse.jetty.http3.client.HTTP3Client;
import org.eclipse.jetty.http3.frames.HeadersFrame;
import org.eclipse.jetty.quic.client.ClientQuicConfiguration;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ssl.DefaultSslBundleRegistry;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
properties = {"server.port=443",
//"logging.level.root=TRACE"
})
class Http3DemoApplicationTests {

	@Autowired
	private DefaultSslBundleRegistry defaultSslBundleRegistry;

	@Test
	void contextLoads() throws Exception {

		var keyStore = defaultSslBundleRegistry.getBundle("server").getStores().getKeyStore();
		var trustore = defaultSslBundleRegistry.getBundle("client").getStores().getTrustStore();

		SslContextFactory.Client sslContextFactory = new SslContextFactory.Client();
		sslContextFactory.setTrustStore(trustore);
		sslContextFactory.setKeyStore(keyStore);
		HTTP3Client http3Client = new HTTP3Client(new ClientQuicConfiguration(sslContextFactory, Files.createTempDirectory("junit")));

// Configure HTTP3Client, for example:
		http3Client.getHTTP3Configuration().setStreamIdleTimeout(15000);

// Start HTTP3Client.
		http3Client.start();

		SocketAddress serverAddress = new InetSocketAddress("localhost", 443);
		CompletableFuture<Session.Client> sessionCF = http3Client.connect(serverAddress, new Session.Client.Listener() {});
		Session.Client session = sessionCF.get();

// Configure the request headers.
		HttpFields requestHeaders = HttpFields.build()
				.put(HttpHeader.USER_AGENT, "Jetty HTTP3Client 12.0.8");

// The request metadata with method, URI and headers.
		MetaData.Request request = new MetaData.Request("GET", HttpURI.from("http://localhost:443/counter"), HttpVersion.HTTP_3, requestHeaders);

// The HTTP/3 HEADERS frame, with endStream=true
// to signal that this request has no content.
		HeadersFrame headersFrame = new HeadersFrame(request, true);

// Open a Stream by sending the HEADERS frame.
		// Open a Stream by sending the HEADERS frame.
		session.newRequest(headersFrame, new Stream.Client.Listener()
		{
			@Override
			public void onResponse(Stream.Client stream, HeadersFrame frame)
			{
				MetaData metaData = frame.getMetaData();
				MetaData.Response response = (MetaData.Response)metaData;
				System.getLogger("http3").log(System.Logger.Level.INFO, "Received response {0}", response);
			}

			@Override
			public void onDataAvailable(Stream.Client stream)
			{
				// Read a chunk of the content.
				Stream.Data data = stream.readData();
				if (data == null)
				{
					// No data available now, demand to be called back.
					stream.demand();
				}
				else
				{
					// Process the content.
//					process(data.getByteBuffer());

					// Notify the implementation that the content has been consumed.
					data.release();

					if (!data.isLast())
					{
						// Demand to be called back.
						stream.demand();
					}
				}
			}
		});
		// Stop HTTP3Client.
		http3Client.stop();
	}

}
