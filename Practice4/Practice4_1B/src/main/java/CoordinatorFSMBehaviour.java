import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.FSMBehaviour;
import jade.lang.acl.ACLMessage;
import jade.util.Logger;

import java.util.List;

public class CoordinatorFSMBehaviour extends FSMBehaviour {

    private static final String STATE_WAIT = "WAIT";
    private static final String STATE_COMPUTE = "COMPUTE";

    private final Logger logger = Logger.getMyLogger(CoordinatorFSMBehaviour.class.getName());
    private final List<AID> workers;

    private ACLMessage originalRequest;
    private int pendingResponses;
    private int totalSum;

    public CoordinatorFSMBehaviour(Agent a, List<AID> workers) {
        super(a);
        this.workers = workers;

        Behaviour waitState = createWaitState();
        Behaviour computeState = createComputeState();

        registerFirstState(waitState, STATE_WAIT);
        registerState(computeState, STATE_COMPUTE);

        registerTransition(STATE_WAIT, STATE_COMPUTE, 1);

        registerDefaultTransition(STATE_COMPUTE, STATE_WAIT);
    }

    private Behaviour createWaitState() {
        return new Behaviour(myAgent) {

            private boolean hasTask = false;

            @Override
            public void onStart() {
                hasTask = false;
            }

            @Override
            public void action() {
                ACLMessage msg = myAgent.receive();
                if (msg == null) {
                    block();
                    return;
                }

                if (msg.getPerformative() == ACLMessage.REQUEST &&
                        "sum".equals(msg.getLanguage())) {

                    if (workers.isEmpty()) {
                        ACLMessage reply = msg.createReply();
                        reply.setPerformative(ACLMessage.DISCONFIRM);
                        reply.setLanguage("sum");
                        reply.setContent("No available workers for computation");
                        myAgent.send(reply);
                        logger.info(myAgent.getLocalName() + ": request from "
                                + msg.getSender().getLocalName() + " rejected — no workers available");
                        return;
                    }

                    originalRequest = msg;
                    logger.info("Coordinator " + myAgent.getLocalName()
                            + ": received request from " + msg.getSender().getLocalName()
                            + ", content: " + msg.getContent()
                            + ". Starting task distribution.");


                    try {
                        String[] parts = msg.getContent().split(",");
                        int a = Integer.parseInt(parts[0].trim());
                        int b = Integer.parseInt(parts[1].trim());
                        startDistributedSum(a, b);
                        hasTask = true;
                    } catch (Exception e) {
                        logger.warning("Coordinator: invalid range: " + msg.getContent());
                        ACLMessage reply = msg.createReply();
                        reply.setPerformative(ACLMessage.DISCONFIRM);
                        reply.setLanguage("sum");
                        reply.setContent("invalid range");
                        myAgent.send(reply);
                    }
                } else {
                    logger.info("Coordinator " + myAgent.getLocalName()
                            + ": received irrelevant message. Ignoring.");

                }
            }

            @Override
            public boolean done() {
                return hasTask;
            }

            @Override
            public int onEnd() {
                return hasTask ? 1 : 0;
            }
        };
    }

    private Behaviour createComputeState() {
        return new Behaviour(myAgent) {

            private boolean finished = false;

            @Override
            public void onStart() {
                finished = false;
            }

            @Override
            public void action() {
                boolean hadMessages = false;

                ACLMessage msg;
                while ((msg = myAgent.receive()) != null) {
                    hadMessages = true;

                    if (msg.getPerformative() == ACLMessage.REQUEST &&
                            "sum".equals(msg.getLanguage())) {

                        ACLMessage refuse = msg.createReply();
                        refuse.setPerformative(ACLMessage.REFUSE);
                        refuse.setLanguage("sum");
                        refuse.setContent("Coordinator busy bow");
                        myAgent.send(refuse);

                        logger.info("Coordinator " + myAgent.getLocalName()
                                + ": busy, sent REFUSE to " + msg.getSender().getLocalName());
                    }
                    else if (msg.getPerformative() == ACLMessage.CONFIRM &&
                            "sum".equals(msg.getLanguage())) {

                        handleWorkerReply(msg);
                    } else {
                        logger.info("Coordinator " + myAgent.getLocalName()
                                + ": received unexpected message during computation. Ignoring.");

                    }

                    if (finished) {
                        break;
                    }
                }

                if (!hadMessages && !finished) {
                    block(100);
                }
            }

            @Override
            public boolean done() {
                return finished;
            }

            @Override
            public int onEnd() {
                originalRequest = null;
                totalSum = 0;
                pendingResponses = 0;
                finished = false;
                return 0;
            }

            private void handleWorkerReply(ACLMessage reply) {
                try {
                    int partSum = Integer.parseInt(reply.getContent().trim());
                    logger.info("Coordinator " + myAgent.getLocalName()
                            + ": get answer from:  " + reply.getSender().getLocalName()
                            + " = " + partSum);

                    totalSum += partSum;
                    pendingResponses--;

                    if (pendingResponses == 0 && originalRequest != null) {
                        ACLMessage finalReply = originalRequest.createReply();
                        finalReply.setPerformative(ACLMessage.CONFIRM);
                        finalReply.setLanguage("sum");
                        finalReply.setContent(String.valueOf(totalSum));

                        myAgent.send(finalReply);
                        logger.info("coordinator " + myAgent.getLocalName()
                                + ": control sum: " + totalSum
                                + " take result " + originalRequest.getSender().getLocalName());

                        finished = true;
                    }
                } catch (Exception e) {
                    logger.warning("Coordinator: failed to parse worker reply: "
                            + reply.getContent());

                }
            }
        };
    }

    private void startDistributedSum(int a, int b) {
        int n = workers.size();
        int totalCount = b - a + 1;
        int base = totalCount / n;
        int remainder = totalCount % n;

        int currentStart = a;

        logger.info("Coordinator " + myAgent.getLocalName()
                + ": splitting [" + a + ";" + b + "] into " + n + " parts");


        for (int i = 0; i < n; i++) {
            int length = base + (i < remainder ? 1 : 0);
            int subA = currentStart;
            int subB = currentStart + length - 1;
            currentStart = subB + 1;

            ACLMessage req = new ACLMessage(ACLMessage.REQUEST);
            req.setLanguage("sum");
            req.setContent(subA + "," + subB);
            req.addReceiver(workers.get(i));

            myAgent.send(req);
            logger.info("Coordinator " + myAgent.getLocalName()
                    + ": assigned range [" + subA + ";" + subB + "] to "
                    + workers.get(i).getLocalName());

        }

        pendingResponses = n;
        totalSum = 0;
    }
}
