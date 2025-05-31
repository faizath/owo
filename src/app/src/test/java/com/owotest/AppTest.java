import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class AppTest {
    @Test
    void testGreet() {
        // Manual stub implementation of NameProvider
        NameProvider stubProvider = new NameProvider() {
            @Override
            public String getName() {
                return "Alice";
            }
        };

        GreetingService service = new GreetingService(stubProvider);
        String result = service.greet();

        assertEquals("Hello, Alice", result);
    }
}