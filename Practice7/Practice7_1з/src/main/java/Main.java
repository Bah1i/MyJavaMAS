import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;

public class Main {
    public static void main(String[] args) {
        try {
            Runtime runtime = Runtime.instance();

            // --- Главный контейнер ---
            Profile mainProfile = new ProfileImpl();
            mainProfile.setParameter(Profile.GUI, "true"); // включаем JADE GUI
            AgentContainer mainContainer = runtime.createMainContainer(mainProfile);
            System.out.println("Main container started with GUI");

            // --- Дополнительный контейнер для агентов ---
            Profile agentProfile = new ProfileImpl();
            agentProfile.setParameter(Profile.MAIN_HOST, "localhost"); // подключение к главному контейнеру
            AgentContainer agentContainer = runtime.createAgentContainer(agentProfile);
            System.out.println("Agent container created");

            // --- Создаём агентов в агентном контейнере ---
            AgentController coord1 = agentContainer.createNewAgent("coordinator1", "CoordinatorAgent", null);
            AgentController coord2 = agentContainer.createNewAgent("coordinator2", "CoordinatorAgent", null);
            AgentController calc1 = agentContainer.createNewAgent("calculator1", "CalculatorAgent", null);
            AgentController calc2 = agentContainer.createNewAgent("calculator2", "CalculatorAgent", null);
            AgentController customer = agentContainer.createNewAgent("customer", "CustomerAgent", null);

            // --- Запуск агентов ---
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
