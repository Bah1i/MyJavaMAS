import jade.core.Agent;
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
    private ThreadedBehaviourFactory tbf;

    @Override
    protected void setup() {
        logger.info("Calculator " + getLocalName() + " ready.");

        tbf = new ThreadedBehaviourFactory();

        try {
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());

            ServiceDescription sd = new ServiceDescription();
            sd.setType("calculator");
            sd.setName("sum-calculator");
            dfd.addServices(sd);

            DFService.register(this, dfd);
            logger.info(getLocalName() + ": registered in DF as calculator");
        } catch (FIPAException e) {
            logger.severe(getLocalName() + ": DF registration error: " + e.getMessage());
        }

        // Основное поведение — ожидание сообщений
        addBehaviour(new WaitBehaviour());
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
            logger.info(getLocalName() + ": deregistered from DF");
        } catch (FIPAException e) {
            logger.severe(getLocalName() + ": DF deregistration error: " + e.getMessage());
        }
    }

    private class WaitBehaviour extends jade.core.behaviours.CyclicBehaviour {
        @Override
        public void action() {
            ACLMessage msg = myAgent.receive();
            if (msg != null) {
                if (msg.getPerformative() == ACLMessage.REQUEST &&
                        "sum".equals(msg.getLanguage())) {

                    String content = msg.getContent();
                    try {
                        String[] parts = content.split(",");
                        int a = Integer.parseInt(parts[0].trim());
                        int b = Integer.parseInt(parts[1].trim());

                        // создаём параллельное вычисление
                        OneShotBehaviour calcBehaviour = new CalcOneShotBehaviour(msg, a, b);
                        myAgent.addBehaviour(tbf.wrap(calcBehaviour));

                        logger.info(getLocalName() + ": started calculation [" + a + ";" + b + "] for " + msg.getSender().getLocalName());

                    } catch (Exception e) {
                        ACLMessage reply = msg.createReply();
                        reply.setPerformative(ACLMessage.DISCONFIRM);
                        reply.setLanguage("sum");
                        reply.setContent("Invalid range");
                        myAgent.send(reply);
                    }
                } else {
                    block();
                }
            } else {
                block();
            }
        }
    }

    private class CalcOneShotBehaviour extends OneShotBehaviour {

        private final ACLMessage request;
        private final int start, end;

        public CalcOneShotBehaviour(ACLMessage request, int start, int end) {
            this.request = request;
            this.start = start;
            this.end = end;
        }

        @Override
        public void action() {
            int result = 0;
            for (int i = start; i <= end; i++) {
                result += i;
            }

            // Отправка ответа координатору
            ACLMessage reply = request.createReply();
            reply.setPerformative(ACLMessage.CONFIRM);
            reply.setLanguage("sum");
            reply.setContent(String.valueOf(result));
            myAgent.send(reply);

            logger.info(getLocalName() + ": calculation done [" + start + ";" + end + "] = " + result
                    + ". Sent reply to " + request.getSender().getLocalName());
        }
    }
}
