import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.StaleProxyException;

public class Main {
    public static void main(String[] args) {
        Runtime rt = Runtime.instance();
        Profile p = new ProfileImpl();
        p.setParameter(Profile.GUI, "true");

        AgentContainer mainContainer = rt.createMainContainer(p);

        try {
            // Запуск вычислителей
            mainContainer.createNewAgent("calc1", "CalculatorAgent", null).start();
            mainContainer.createNewAgent("calc2", "CalculatorAgent", null).start();
            mainContainer.createNewAgent("calc3", "CalculatorAgent", null).start();

            // Запуск координаторов
            mainContainer.createNewAgent("coordinator1", "CoordinatorAgent", null).start();
            mainContainer.createNewAgent("coordinator2", "CoordinatorAgent", null).start();

            // Запуск клиентов-заказчиков
            mainContainer.createNewAgent("client1", "ClientAgent", null).start();
            mainContainer.createNewAgent("client2", "ClientAgent", null).start();

            System.out.println("All agents started successfully!");
        } catch (StaleProxyException e) {
            e.printStackTrace();
        }
    }
}
