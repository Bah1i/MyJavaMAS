import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.util.Logger;

public class RouteRequesterAgent extends Agent {
    private Logger logger = Logger.getMyLogger(getClass().getName());

    @Override
    protected void setup() {
        logger.info("Route requester started.");

        addBehaviour(new TickerBehaviour(this, 4000) {
            @Override
            protected void onTick() {
                String start = getLocalName();
                String target = "D";

                logger.info("Requesting path to: " + target);

                ACLMessage req = new ACLMessage(ACLMessage.REQUEST);
                req.setLanguage("route");
                req.addReceiver(new AID("A", AID.ISLOCALNAME));  // стартуем от A
                req.setContent(target + ";");
                send(req);
            }
        });
    }
}
