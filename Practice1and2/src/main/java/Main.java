import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.StaleProxyException;

public class Main {
    public static void main(String[] args) {
        // Initialize the JADE runtime
        Runtime rt = Runtime.instance();
        Profile p = new ProfileImpl();
        p.setParameter(Profile.GUI, "true");

        AgentContainer mainContainer = rt.createMainContainer(p);

        try {
            mainContainer.createNewAgent("Alice", "HelloAgent", null).start();
            System.out.println("Agent Alice (HelloAgent) is worked");
        } catch (StaleProxyException e) {
            System.out.println("Error: Alisa ne rodilas: " + e.getMessage());
            e.printStackTrace();
        }

        try {
            mainContainer.createNewAgent("bob", "SampleAgent", null).start();
            System.out.println("Agent bob (SampleAgent) is worked");
        } catch (StaleProxyException e) {
            System.out.println("Error: Bob ne rodilsa: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("JADE container is worked!");

        try {
            // Вычислители
            mainContainer.createNewAgent("calc1", "CalculatorAgent", null).start();
            mainContainer.createNewAgent("calc2", "CalculatorAgent", null).start();
            mainContainer.createNewAgent("calc3", "CalculatorAgent", null).start();

            // Координатор
            mainContainer.createNewAgent("coordinator", "CoordinatorAgent", null).start();

            System.out.println("Agents started: coordinator, calc1, calc2, calc3");
        } catch (StaleProxyException e) {
            e.printStackTrace();
        }
    }
}