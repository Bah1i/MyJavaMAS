import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.ThreadedBehaviourFactory;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.util.Logger;

import java.util.Arrays;
import java.util.Comparator;

public class CalculatorAgent extends Agent {

    private final Logger logger = Logger.getMyLogger(CalculatorAgent.class.getName());
    private final ThreadedBehaviourFactory tbf = new ThreadedBehaviourFactory();

    @Override
    protected void setup() {
        logger.info(getLocalName() + " started as calculator");

        // Регистрация как калькулятор
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

        // --- Выбор координатора ---
        addBehaviour(new LeaderElectionBehaviour());

        // --- Обработка задач от координатора ---
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

    // --- Внутреннее поведение для выбора координатора ---
    private class LeaderElectionBehaviour extends OneShotBehaviour {
        @Override
        public void action() {
            try {
                DFAgentDescription template = new DFAgentDescription();
                ServiceDescription sd = new ServiceDescription();
                sd.setType("calculator");
                template.addServices(sd);

                DFAgentDescription[] calculators = DFService.search(myAgent, template);

                if (calculators.length == 0) {
                    logger.warning(getLocalName() + ": no calculators found for leader election");
                    return;
                }

                // Выбираем координатора по наименьшему имени
                DFAgentDescription leader = Arrays.stream(calculators)
                        .min(Comparator.comparing(d -> d.getName().getLocalName()))
                        .orElse(calculators[0]);

                if (leader.getName().equals(myAgent.getAID())) {
                    // Этот агент выбран координатором
                    logger.info(getLocalName() + ": elected as coordinator");

                    // Регистрируемся в DF как координатор
                    DFAgentDescription dfd = new DFAgentDescription();
                    dfd.setName(getAID());
                    ServiceDescription sdc = new ServiceDescription();
                    sdc.setType("coordinator");
                    sdc.setName("sum-coordinator");
                    dfd.addServices(sdc);

                    DFService.register(myAgent, dfd);
                    logger.info(getLocalName() + ": registered in DF as coordinator");
                } else {
                    logger.info(getLocalName() + ": not elected as coordinator. Leader is "
                            + leader.getName().getLocalName());
                }

            } catch (FIPAException e) {
                logger.severe(getLocalName() + ": DF search error in leader election: " + e.getMessage());
            }
        }
    }
}
