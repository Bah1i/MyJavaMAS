import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;

public class Main {
    public static void main(String[] args) {
        Runtime runtime = Runtime.instance();
        Profile profile = new ProfileImpl();
        profile.setParameter(Profile.GUI, "true"); // включает JADE GUI
        AgentContainer container = runtime.createMainContainer(profile);

        try {
            // создаём координаторов
            AgentController coord1 = container.createNewAgent("coordinator1",
                    "CoordinatorAgent", null);
            AgentController coord2 = container.createNewAgent("coordinator2",
                    "CoordinatorAgent", null);

            // создаём калькуляторов
            AgentController calc1 = container.createNewAgent("calculator1",
                    "CalculatorAgent", null);
            AgentController calc2 = container.createNewAgent("calculator2",
                    "CalculatorAgent", null);

            // создаём заказчика
            AgentController customer = container.createNewAgent("customer",
                    "CustomerAgent", null);

            // запускаем всех агентов
            coord1.start();
            coord2.start();
            calc1.start();
            calc2.start();
            customer.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
