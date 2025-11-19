import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.ThreadedBehaviourFactory;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.util.Logger;

public class CalculatorAgent extends Agent {

    private final Logger logger = Logger.getMyLogger(CalculatorAgent.class.getName());
    private final ThreadedBehaviourFactory tbf = new ThreadedBehaviourFactory();

    @Override
    protected void setup() {
        logger.info("Calculator " + getLocalName() + " is ready");

        try {
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());

            ServiceDescription sd = new ServiceDescription();
            sd.setType("calculator");
            sd.setName("sum-calculator");

            dfd.addServices(sd);

            DFService.register(this, dfd);
            logger.info(getLocalName() + " registered in DF as calculator");
        } catch (FIPAException e) {
            logger.severe(getLocalName() + ": DF registration error: " + e.getMessage());
        }

        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                ACLMessage msg = receive();
                if (msg == null) {
                    block();
                    return;
                }

                if (msg.getPerformative() == ACLMessage.REQUEST &&
                        "sum".equals(msg.getLanguage())) {

                    String content = msg.getContent();
                    logger.info(getLocalName() + ": received task " + content);

                    addBehaviour(tbf.wrap(new OneShotBehaviour(myAgent) {
                        @Override
                        public void action() {
                            try {
                                String[] parts = content.split(",");
                                int a = Integer.parseInt(parts[0].trim());
                                int b = Integer.parseInt(parts[1].trim());

                                long result = 0;
                                for (int i = a; i <= b; i++) {
                                    result += i;
                                }

                                ACLMessage reply = msg.createReply();
                                reply.setPerformative(ACLMessage.CONFIRM);
                                reply.setLanguage("sum");
                                reply.setContent(String.valueOf(result));

                                myAgent.send(reply);
                                logger.info(getLocalName() + ": calculation done, sent result to " + msg.getSender().getLocalName());
                            } catch (Exception e) {
                                logger.warning(getLocalName() + ": error computing task " + content);
                            }
                        }
                    }));
                } else {
                    logger.info(getLocalName() + ": irrelevant message, ignoring");
                }
            }
        });
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
            logger.info(getLocalName() + ": deregistered from DF");
        } catch (FIPAException e) {
            logger.severe(getLocalName() + ": DF deregistration error: " + e.getMessage());
        }
        tbf.interrupt();
    }
}
