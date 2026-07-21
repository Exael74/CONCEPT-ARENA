package com.conceptarena.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;

/**
 * C7: real route test beyond contextLoads — asserts the YAML route table parsed into the expected
 * RouteDefinitions (ids + Path predicates), so a typo/removal in the gateway routing config fails a
 * test instead of only surfacing as a 404 at runtime. Uses the default profile's http/ws URIs.
 */
@SpringBootTest
class GatewayRoutesTest {

    @Autowired private RouteDefinitionLocator routeDefinitionLocator;

    @Test
    void allExpectedRoutesArePresentWithTheirPathPredicates() {
        List<RouteDefinition> routes = routeDefinitionLocator.getRouteDefinitions().collectList().block();
        assertThat(routes).isNotNull();

        // Every backend the gateway fronts must have a route id.
        assertThat(routes).extracting(RouteDefinition::getId).contains(
            "auth-service", "room-service-rest", "concept-bank-service", "game-engine-service-rest",
            "room-service-ws", "room-service-stomp", "game-engine-service-ws", "game-engine-service-stomp",
            "voice-signaling-service-ws");

        // Spot-check that the auth route actually carries a Path predicate for /api/auth.
        RouteDefinition auth = routes.stream().filter(r -> r.getId().equals("auth-service")).findFirst().orElseThrow();
        assertThat(auth.getPredicates()).anyMatch(p ->
            p.getName().equals("Path") && p.getArgs().values().stream().anyMatch(v -> v.contains("/api/auth")));
    }
}
