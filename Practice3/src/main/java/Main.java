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

            // Новый координатор через DF
            mainContainer.createNewAgent("coordinatorDF", "CoordinatorAgentDF", null).start();

            // Узлы для маршрутизации
            mainContainer.createNewAgent("nodeA", "NodeAgent", new Object[]{"nodeB","nodeC"}).start();
            mainContainer.createNewAgent("nodeB", "NodeAgent", new Object[]{"nodeA","nodeD"}).start();
            mainContainer.createNewAgent("nodeC", "NodeAgent", new Object[]{"nodeA","nodeD"}).start();
            mainContainer.createNewAgent("nodeD", "NodeAgent", new Object[]{"nodeB","nodeC"}).start();

            System.out.println("All agents started successfully!");
        } catch (StaleProxyException e) {
            e.printStackTrace();
        }
    }
}
