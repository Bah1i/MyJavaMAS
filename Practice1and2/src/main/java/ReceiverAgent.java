import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.util.Logger;

public class ReceiverAgent extends Agent {
    private Logger logger = Logger.getMyLogger(getClass().getName());

    @Override
    protected void setup() {
        logger.info("Agent " + getLocalName() + " " + getAID().getName() + " is ready.");

        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    // Выводим информацию в логгер
                    logger.info("[" + getLocalName() + "] get message:");
                    logger.info("Act: " + ACLMessage.getPerformative(msg.getPerformative()));
                    logger.info("From: " + msg.getSender().getLocalName());
                    logger.info("Content: " + msg.getContent());
                } else {
                    block();
                }
            }
        });
    }
}
