public class GreetingService {
    private final NameProvider nameProvider;

    public GreetingService(NameProvider nameProvider) {
        this.nameProvider = nameProvider;
    }

    public String greet() {
        return "Hello, " + nameProvider.getName();
    }
}