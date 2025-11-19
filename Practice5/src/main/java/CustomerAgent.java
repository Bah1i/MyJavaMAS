import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.proto.ContractNetInitiator;
import jade.util.Logger;

import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;

public class CustomerAgent extends Agent {

    private final Logger logger = Logger.getMyLogger(CustomerAgent.class.getName());
    private int taskCounter = 1;

    @Override
    protected void setup() {
        logger.info("Customer " + getLocalName() + " started");

        addBehaviour(new TickerBehaviour(this, 10000) {
            @Override
            protected void onTick() {
                try {
                    DFAgentDescription template = new DFAgentDescription();
                    ServiceDescription sd = new ServiceDescription();
                    sd.setType("coordinator");
                    template.addServices(sd);

                    DFAgentDescription[] result = DFService.search(myAgent, template);

                    if (result.length == 0) {
                        logger.warning(getLocalName() + ": no coordinators found in DF");
                        return;
                    }

                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                    cfp.setLanguage("sum");
                    int rangeEnd = taskCounter * 100000;
                    cfp.setContent("1," + rangeEnd);
                    cfp.setConversationId("sum-contract");
                    cfp.setReplyByDate(new Date(System.currentTimeMillis() + 15000));

                    for (DFAgentDescription dfd : result) {
                        cfp.addReceiver(dfd.getName());
                    }

                    logger.info(getLocalName() + ": sending CFP for task 1," + rangeEnd);

                    addBehaviour(new ContractNetInitiator(myAgent, cfp) {

                        @Override
                        protected void handlePropose(ACLMessage propose, Vector acceptances) {
                            logger.info(getLocalName() + ": received PROPOSE from " + propose.getSender().getLocalName());
                        }

                        @Override
                        protected void handleRefuse(ACLMessage refuse) {
                            logger.info(getLocalName() + ": received REFUSE from " + refuse.getSender().getLocalName()
                                    + " text: " + refuse.getContent());
                        }

                        @Override
                        @SuppressWarnings("rawtypes")
                        protected void handleAllResponses(Vector responses, Vector acceptances) {
                            int bestIndex = -1;
                            int i = 0;
                            Enumeration e = responses.elements();
                            while (e.hasMoreElements()) {
                                ACLMessage msg = (ACLMessage) e.nextElement();
                                ACLMessage reply = msg.createReply();
                                reply.setLanguage("sum");
                                acceptances.add(reply);

                                if (msg.getPerformative() == ACLMessage.PROPOSE && bestIndex == -1) {
                                    bestIndex = i;
                                }
                                i++;
                            }

                            if (bestIndex == -1) {
                                logger.info(getLocalName() + ": no coordinator proposed, all REJECT_PROPOSAL sent");
                                for (int j = 0; j < acceptances.size(); j++) {
                                    ACLMessage acc = (ACLMessage) acceptances.get(j);
                                    acc.setPerformative(ACLMessage.REJECT_PROPOSAL);
                                }
                                return;
                            }

                            for (int j = 0; j < acceptances.size(); j++) {
                                ACLMessage acc = (ACLMessage) acceptances.get(j);
                                if (j == bestIndex) {
                                    acc.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                                    logger.info(getLocalName() + ": ACCEPT_PROPOSAL to coordinator "
                                            + ((ACLMessage) responses.get(j)).getSender().getLocalName());
                                } else {
                                    acc.setPerformative(ACLMessage.REJECT_PROPOSAL);
                                }
                            }
                        }

                        @Override
                        protected void handleInform(ACLMessage inform) {
                            logger.info(getLocalName() + ": received INFORM from " + inform.getSender().getLocalName()
                                    + " result = " + inform.getContent());
                        }

                        @Override
                        protected void handleFailure(ACLMessage failure) {
                            logger.warning(getLocalName() + ": FAILURE from " + failure.getSender().getLocalName());
                        }
                    });

                    taskCounter++; // увеличиваем диапазон для следующей задачи
                } catch (FIPAException fe) {
                    logger.severe("DF search error: " + fe.getMessage());
                }
            }
        });
    }
}
