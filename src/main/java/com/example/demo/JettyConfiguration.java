package com.example.demo;

import org.eclipse.jetty.http3.api.Session;
import org.eclipse.jetty.http3.server.RawHTTP3ServerConnectionFactory;
import org.eclipse.jetty.quic.server.QuicServerConnector;
import org.eclipse.jetty.quic.server.ServerQuicConfiguration;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ssl.DefaultSslBundleRegistry;
import org.springframework.boot.web.embedded.jetty.JettyServerCustomizer;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Paths;

import static org.slf4j.LoggerFactory.getLogger;

@Configuration
public class JettyConfiguration implements WebServerFactoryCustomizer<JettyServletWebServerFactory> {
    private static final Logger log = getLogger(JettyConfiguration.class);
    @Autowired
    private DefaultSslBundleRegistry defaultSslBundleRegistry;

    @Value("${server.port}")
    private Integer serverPort;

    @Value("${server.jetty.connection-idle-timeout}")
    private Integer idleTimeout;

    @Override
    public void customize(JettyServletWebServerFactory factory) {

        var jettyServerCustomizer = new JettyServerCustomizer() {
            @Override
            public void customize(Server server) {
                var keyStore = defaultSslBundleRegistry.getBundle("server").getStores().getKeyStore();
                var trustore = defaultSslBundleRegistry.getBundle("client").getStores().getTrustStore();

                SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
                sslContextFactory.setTrustStore(trustore);
                sslContextFactory.setKeyStore(keyStore);
                sslContextFactory.setKeyStorePassword(""); // Must be set for Jetty

                HttpConfiguration httpConfig = new HttpConfiguration();
                httpConfig.addCustomizer(new SecureRequestCustomizer());
                httpConfig.setIdleTimeout(idleTimeout);

                ServerQuicConfiguration quicConfig = new ServerQuicConfiguration(sslContextFactory, // Must be set for Jetty
                        Paths.get(System.getProperty("java.io.tmpdir")));
//                HttpConnectionFactory connectionFactory = new HttpConnectionFactory(httpConfig);
                Session.Server.Listener sessionListener = new Session.Server.Listener() {
                    @Override
                    public void onAccept(Session session) {
                        log.info("onAccept: " + session);
                    }
                };
                RawHTTP3ServerConnectionFactory connectionFactory = new RawHTTP3ServerConnectionFactory(quicConfig, sessionListener);
                QuicServerConnector connector = new QuicServerConnector(server, quicConfig, connectionFactory);

                connector.setPort(serverPort);
                server.addConnector(connector);
            }
        };

        factory.addServerCustomizers(jettyServerCustomizer);
    }
}
