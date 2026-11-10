package cl.germain.studymanager.security;

import org.apache.catalina.connector.Connector;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.RemoteIpValve;
import org.apache.catalina.valves.ValveBase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.tomcat.autoconfigure.TomcatServerProperties;
import org.springframework.boot.tomcat.autoconfigure.TomcatWebServerFactoryCustomizer;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.mock.env.MockEnvironment;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class TrustedProxyConfigurationTest {

    @Test
    void directConnectionsDoNotInstallAForwardingValve() throws Exception {
        var factory = factory(environment(false));

        assertThat(factory.getEngineValves()).noneMatch(RemoteIpValve.class::isInstance);
    }

    @Test
    void cloudDetectionCannotEnableForwardingWithoutTheProxyProfile() throws Exception {
        var env = environment(false).withProperty("spring.main.cloud-platform", "kubernetes");

        assertThat(env.getProperty("server.forward-headers-strategy")).isEqualTo("none");
        assertThat(factory(env).getEngineValves()).noneMatch(RemoteIpValve.class::isInstance);
    }

    @Test
    void proxyProfileWithoutExplicitIpsTrustsNobodyIncludingLoopback() throws Exception {
        var valve = valve(environment(true));

        assertThat(valve.getInternalProxies()).isNullOrEmpty();
        assertThat(valve.getTrustedProxies()).isNullOrEmpty();
        for (String peer : new String[]{"127.0.0.1", "::1", "10.20.0.10", "192.168.1.2", "203.0.113.9"}) {
            assertThat(remoteAddressDuringRequest(valve, peer, "198.51.100.77")).isEqualTo(peer);
        }
    }

    @Test
    void onlyExplicitProxyCanSupplyTheClientAddress() throws Exception {
        var env = environment(true).withProperty("APP_PROXY_INTERNAL_PROXIES", "^10[.]20[.]0[.]10$");
        var valve = valve(env);

        assertThat(remoteAddressDuringRequest(valve, "10.20.0.10", "198.51.100.77"))
                .isEqualTo("198.51.100.77");
        assertThat(remoteAddressDuringRequest(valve, "10.20.0.11", "198.51.100.77"))
                .isEqualTo("10.20.0.11");
        assertThat(remoteAddressDuringRequest(valve, "203.0.113.9", "198.51.100.77"))
                .isEqualTo("203.0.113.9");
    }

    @Test
    void trustedProxyWithoutHeaderKeepsItsOwnAddress() throws Exception {
        var valve = valve(environment(true)
                .withProperty("APP_PROXY_INTERNAL_PROXIES", "^10[.]20[.]0[.]10$"));

        assertThat(remoteAddressDuringRequest(valve, "10.20.0.10", null)).isEqualTo("10.20.0.10");
    }

    @Test
    void forwardedChainStopsAtFirstUntrustedHopFromTheRight() throws Exception {
        var valve = valve(environment(true)
                .withProperty("APP_PROXY_INTERNAL_PROXIES", "^10[.]20[.]0[.]10$"));

        assertThat(remoteAddressDuringRequest(valve, "10.20.0.10", "198.51.100.77, 203.0.113.9"))
                .isEqualTo("203.0.113.9");
    }

    private MockEnvironment environment(boolean proxy) throws Exception {
        var env = new MockEnvironment();
        env.getPropertySources().addLast(new ResourcePropertySource("classpath:application.properties"));
        if (proxy) {
            env.getPropertySources().addFirst(new ResourcePropertySource("classpath:application-proxy.properties"));
        }
        return env;
    }

    private TomcatServletWebServerFactory factory(MockEnvironment env) {
        var server = Binder.get(env).bind("server", ServerProperties.class).orElseGet(ServerProperties::new);
        var tomcat = Binder.get(env).bind("server.tomcat", TomcatServerProperties.class)
                .orElseGet(TomcatServerProperties::new);
        var factory = new TomcatServletWebServerFactory();
        new TomcatWebServerFactoryCustomizer(env, server, tomcat, new WebProperties()).customize(factory);
        return factory;
    }

    private RemoteIpValve valve(MockEnvironment env) {
        return factory(env).getEngineValves().stream().filter(RemoteIpValve.class::isInstance)
                .map(RemoteIpValve.class::cast).findFirst().orElseThrow();
    }

    // Real Tomcat request/valve: MockMvc alone does not execute the container's valve pipeline.
    private String remoteAddressDuringRequest(RemoteIpValve valve, String peer, String forwardedFor)
            throws Exception {
        var coyote = new org.apache.coyote.Request();
        coyote.scheme().setString("http");
        coyote.serverName().setString("localhost");
        coyote.setServerPort(8080);
        if (forwardedFor != null) {
            coyote.getMimeHeaders().addValue("X-Forwarded-For").setString(forwardedFor);
        }
        var request = new Request(new Connector(), coyote);
        request.setRemoteAddr(peer);
        request.setRemoteHost(peer);
        var observed = new AtomicReference<String>();
        valve.setNext(new ValveBase() {
            @Override
            public void invoke(Request request, Response response) {
                observed.set(request.getRemoteAddr());
            }
        });
        valve.invoke(request, new Response(new org.apache.coyote.Response()));
        return observed.get();
    }
}
