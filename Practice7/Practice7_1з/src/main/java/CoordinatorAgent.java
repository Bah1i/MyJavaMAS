import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.ContractNetResponder;
import jade.util.Logger;

import java.util.ArrayList;
import java.util.List;

public class CoordinatorAgent extends Agent {

    private final Logger logger = Logger.getMyLogger(CoordinatorAgent.class.getName());
    private final List<AID> workers = new ArrayList<>();
    private boolean shutdownRequested = false; // kill-switch

    @Override
    protected void setup() {
        logger.info(getLocalName() + ": Coordinator started");

        // Register in DF
        registerInDF();

        // Periodically update workers list
        addBehaviour(new DFUpdateBehaviour(this, workers, 3000));

        // ContractNetResponder
        MessageTemplate mt = MessageTemplate.and(
                MessageTemplate.MatchPerformative(ACLMessage.CFP),
                MessageTemplate.MatchLanguage("sum")
        );

        addBehaviour(new ContractNetResponder(this, mt) {
            @Override
            protected ACLMessage handleCfp(ACLMessage cfp) {
                logger.info(getLocalName() + ": Received CFP from " + cfp.getSender().getLocalName()
                        + ", content: " + cfp.getContent());
                ACLMessage reply = cfp.createReply();
                reply.setLanguage("sum");

                if (shutdownRequested) {
                    reply.setPerformative(ACLMessage.REFUSE);
                    reply.setContent("Coordinator shutting down");
                    logger.info(getLocalName() + ": REFUSE, shutdown in progress");
                } else if (workers.isEmpty()) {
                    reply.setPerformative(ACLMessage.REFUSE);
                    reply.setContent("No available workers");
                    logger.info(getLocalName() + ": REFUSE, no workers available");
                } else {
                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContent("Ready to compute");
                    logger.info(getLocalName() + ": PROPOSE sent");
                }
                return reply;
            }

            @Override
            protected ACLMessage handleAcceptProposal(ACLMessage cfp, ACLMessage propose, ACLMessage accept) {
                logger.info(getLocalName() + ": Proposal accepted, executing task " + cfp.getContent());
                long total = 0L;
                try {
                    String[] parts = cfp.getContent().split(",");
                    int a = Integer.parseInt(parts[0].trim());
                    int b = Integer.parseInt(parts[1].trim());
                    total = distributedSum(a, b);
                } catch (Exception e) {
                    logger.severe(getLocalName() + ": Error parsing range " + cfp.getContent());
                }

                ACLMessage inform = accept.createReply();
                inform.setPerformative(ACLMessage.INFORM);
                inform.setLanguage("sum");
                inform.setContent(String.valueOf(total));
                logger.info(getLocalName() + ": Task done, result = " + total);
                return inform;
            }

            @Override
            protected void handleRejectProposal(ACLMessage cfp, ACLMessage propose, ACLMessage reject) {
                logger.info(getLocalName() + ": Proposal rejected by " + reject.getSender().getLocalName());
            }
        });
    }

    private long distributedSum(int a, int b) {
        if (workers.isEmpty()) return 0L;

        int n = workers.size();
        int totalCount = b - a + 1;
        int base = totalCount / n;
        int remainder = totalCount % n;
        int currentStart = a;
        long total = 0L;

        logger.info(getLocalName() + ": Splitting range [" + a + ";" + b + "] into " + n + " parts");

        for (int i = 0; i < n; i++) {
            int length = base + (i < remainder ? 1 : 0);
            int subA = currentStart;
            int subB = currentStart + length - 1;
            currentStart = subB + 1;

            ACLMessage req = new ACLMessage(ACLMessage.REQUEST);
            req.setLanguage("sum");
            req.setContent(subA + "," + subB);
            req.addReceiver(workers.get(i));
            send(req);

            logger.info(getLocalName() + ": Sent task [" + subA + ";" + subB + "] to " + workers.get(i).getLocalName());
        }

        int pending = n;
        MessageTemplate mt = MessageTemplate.and(
                MessageTemplate.MatchPerformative(ACLMessage.CONFIRM),
                MessageTemplate.MatchLanguage("sum")
        );

        while (pending > 0) {
            ACLMessage msg = blockingReceive(mt);
            if (msg != null) {
                try {
                    long part = Long.parseLong(msg.getContent().trim());
                    total += part;
                    pending--;
                    logger.info(getLocalName() + ": Received " + part + " from " + msg.getSender().getLocalName()
                            + ", remaining " + pending);
                } catch (Exception e) {
                    logger.warning(getLocalName() + ": Failed to parse reply " + msg.getContent());
                }
            }
        }

        return total;
    }

    /** Kill-switch: вызывается извне для корректного завершения работы координатора */
    public void requestShutdown() {
        shutdownRequested = true;
        logger.info(getLocalName() + ": Shutdown requested, deregistering from DF");
        try {
            DFService.deregister(this);
            logger.info(getLocalName() + ": Deregistered from DF");
        } catch (FIPAException e) {
            logger.severe(getLocalName() + ": DF deregistration error: " + e.getMessage());
        }
        doDelete(); // завершение агента
    }

    private void registerInDF() {
        try {
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());
            ServiceDescription sd = new ServiceDescription();
            sd.setType("coordinator");
            sd.setName("sum-coordinator");
            dfd.addServices(sd);
            DFService.register(this, dfd);
            logger.info(getLocalName() + ": Registered in DF as coordinator");
        } catch (FIPAException e) {
            logger.severe(getLocalName() + ": DF registration error: " + e.getMessage());
        }
    }

    @Override
    protected void takeDown() {
        if (!shutdownRequested) {
            try {
                DFService.deregister(this);
                logger.info(getLocalName() + ": Deregistered from DF at takeDown");
            } catch (FIPAException e) {
                logger.severe(getLocalName() + ": DF deregistration error: " + e.getMessage());
            }
        }
        logger.info(getLocalName() + ": Coordinator terminated");
    }
}
