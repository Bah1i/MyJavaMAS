import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.util.Logger;

public class SampleAgent extends Agent {
    private Logger logger = Logger.getMyLogger(getClass().getName());

    @Override
    protected void setup() {
        System.out.println("Hello! Agent " + getLocalName() + " "  + getAID().getName() + " is ready.");

        // Add cyclic behavior for receiving messages
        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    logger.info("=== Message received by agent " + getLocalName() + " ===");
                    logger.info("Performative: " + ACLMessage.getPerformative(msg.getPerformative()));
                    logger.info("Sender: " + msg.getSender().getLocalName());
                    logger.info("Content: " + msg.getContent());
                } else {
                    block();
                }
            }
        });
    }
}
