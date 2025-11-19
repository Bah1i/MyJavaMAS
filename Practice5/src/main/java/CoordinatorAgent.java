import jade.core.AID;
import jade.core.Agent;
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

    @Override
    protected void setup() {
        logger.info("Coordinator " + getLocalName() + " started");

        try {
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());

            ServiceDescription sd = new ServiceDescription();
            sd.setType("coordinator");
            sd.setName("sum-coordinator");

            dfd.addServices(sd);

            DFService.register(this, dfd);
            logger.info(getLocalName() + " registered in DF as coordinator");
        } catch (FIPAException e) {
            logger.severe(getLocalName() + ": DF registration error: " + e.getMessage());
        }

        addBehaviour(new DFUpdateBehaviour(this, workers, 1000));

        MessageTemplate mt = MessageTemplate.and(
                MessageTemplate.MatchPerformative(ACLMessage.CFP),
                MessageTemplate.MatchLanguage("sum")
        );

        addBehaviour(new ContractNetResponder(this, mt) {
            @Override
            protected ACLMessage handleCfp(ACLMessage cfp) {
                ACLMessage reply = cfp.createReply();
                reply.setLanguage("sum");

                if (workers.isEmpty()) {
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
                logger.info(getLocalName() + ": proposal accepted, executing task " + cfp.getContent());
                long total = 0L;
                try {
                    String[] parts = cfp.getContent().split(",");
                    int a = Integer.parseInt(parts[0].trim());
                    int b = Integer.parseInt(parts[1].trim());
                    total = distributedSum(a, b);
                } catch (Exception e) {
                    logger.severe(getLocalName() + ": error parsing range " + cfp.getContent());
                }

                ACLMessage inform = accept.createReply();
                inform.setPerformative(ACLMessage.INFORM);
                inform.setLanguage("sum");
                inform.setContent(String.valueOf(total));
                logger.info(getLocalName() + ": task done, result " + total);
                return inform;
            }

            @Override
            protected void handleRejectProposal(ACLMessage cfp, ACLMessage propose, ACLMessage reject) {
                logger.info(getLocalName() + ": proposal rejected by " + reject.getSender().getLocalName());
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
            logger.info(getLocalName() + ": sent task [" + subA + ";" + subB + "] to " + workers.get(i).getLocalName());
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
                    logger.info(getLocalName() + ": received " + part + " from " + msg.getSender().getLocalName()
                            + ", remaining " + pending);
                } catch (Exception e) {
                    logger.warning(getLocalName() + ": failed to parse reply " + msg.getContent());
                }
            }
        }

        return total;
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
}
