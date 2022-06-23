package tl.develoveper.lambda.shipping;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariDataSource;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import tl.develoveper.lambda.shipping.dao.ShippingDao;
import tl.develoveper.lambda.shipping.entity.Consignment;
import tl.develoveper.lambda.shipping.entity.ConsignmentCheckIn;
import tl.develoveper.lambda.shipping.entity.ConsignmentItem;
import tl.develoveper.lambda.shipping.service.ShippingService;

import java.util.HashMap;
import java.util.Map;

import static org.hibernate.cfg.AvailableSettings.*;

/**
 * Handler for requests to Lambda function.
 */
public class App implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final SessionFactory sessionFactory = createSessionFactory();

    //    private String getPageContents(String address) throws IOException {
//        URL url = new URL(address);
//        try (BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()))) {
//            return br.lines().collect(Collectors.joining(System.lineSeparator()));
//        }
//    }
    private static SessionFactory createSessionFactory() {
        Map<String, String> settings = new HashMap<>();
        settings.put(URL, System.getenv("DB_URL"));
        settings.put(DIALECT, "org.hibernate.dialect.PostgreSQLDialect");
        settings.put(DEFAULT_SCHEMA, "shipping");
        settings.put(DRIVER, "org.postgresql.Driver");
        settings.put(USER, System.getenv("DB_USER"));
        settings.put(PASS, System.getenv("DB_PASSWORD"));
        settings.put("hibernate.hikari.connectionTimeout", "20000");
        settings.put("hibernate.hikari.minimumIdle", "1");
        settings.put("hibernate.hikari.maximumPoolSize", "2");
        settings.put("hibernate.hikari.idleTimeout", "30000");

// commented out as we only need them on first use
        settings.put(HBM2DDL_AUTO, "create-only");
        settings.put(HBM2DDL_DATABASE_ACTION, "create");

        StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                .applySettings(settings)
                .build();

        return new MetadataSources(registry)
                .addAnnotatedClass(Consignment.class)
                .addAnnotatedClass(ConsignmentItem.class)
                .addAnnotatedClass(ConsignmentCheckIn.class)
                .buildMetadata()
                .buildSessionFactory();
    }

    private static <T> T fromJson(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private static <T> String toJson(T object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }


    private void flushConnectionPool() {
        ConnectionProvider connectionProvider = sessionFactory.getSessionFactoryOptions()
                .getServiceRegistry()
                .getService(ConnectionProvider.class);
        HikariDataSource hikariDataSource = connectionProvider.unwrap(HikariDataSource.class);
        hikariDataSource.getHikariPoolMXBean().softEvictConnections();
    }

    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent input, final Context context) {
        try {
            ShippingService service = new ShippingService(sessionFactory, new ShippingDao());
            return routeRequest(input, service);
        } finally {
            flushConnectionPool();
        }
    }

    public APIGatewayProxyResponseEvent routeRequest(final APIGatewayProxyRequestEvent input, final ShippingService service) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("X-Custom-Header", "application/json");

        Object result = "OK";
//        ShippingService service = new ShippingService();

        switch (input.getResource()) {
            case "/consignment":
                result = service.createConsignment(
                        fromJson(input.getBody(), Consignment.class));
                break;
            case "/consignment/{id}":
                result = service.view(input.getPathParameters().get("id"));
                break;
            case "/consignment/{id}/item":
                service.addItem(input.getPathParameters().get("id"),
                        fromJson(input.getBody(), ConsignmentItem.class));
                break;
            case "/consignment/{id}/check-in":
                service.checkIn(input.getPathParameters().get("id"),
                        fromJson(input.getBody(), ConsignmentCheckIn.class));
                break;
        }

        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent()
                .withStatusCode(200)
                .withBody(toJson(result))
                .withHeaders(headers);
        return response;
//        try {
//            final String pageContents = this.getPageContents("https://checkip.amazonaws.com");
//            String output = String.format("{ \"message\": \"hello world\", \"location\": \"%s\" }", pageContents);
//
//            return response
//                    .withStatusCode(200)
//                    .withBody(output);
//        } catch (IOException e) {
//            return response
//                    .withBody("{}")
//                    .withStatusCode(500);
//        }
    }
}
