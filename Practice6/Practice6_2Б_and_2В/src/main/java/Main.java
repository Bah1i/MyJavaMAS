import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;

public class Main {
    public static void main(String[] args) {
        try {
            Runtime runtime = Runtime.instance();

            // Главный контейнер
            Profile mainProfile = new ProfileImpl();
            mainProfile.setParameter(Profile.GUI, "true");
            AgentContainer mainContainer = runtime.createMainContainer(mainProfile);
            System.out.println("Main container started");

            // Контейнер для агентов графа
            Profile graphProfile = new ProfileImpl();
            graphProfile.setParameter(Profile.MAIN_HOST, "localhost");
            AgentContainer graphContainer = runtime.createAgentContainer(graphProfile);
            System.out.println("Graph container started");

            graphContainer.createNewAgent("A", "NodeAgent", new Object[]{"B","C"}).start();
            graphContainer.createNewAgent("B", "NodeAgent", new Object[]{"A","D"}).start();
            graphContainer.createNewAgent("C", "NodeAgent", new Object[]{"A","D"}).start();
            graphContainer.createNewAgent("D", "NodeAgent", new Object[]{"B","C"}).start();

            // Агент, который делает запрос пути
            graphContainer.createNewAgent("client", "RouteRequesterAgent", null).start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
