import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.FSMBehaviour;
import jade.lang.acl.ACLMessage;
import jade.util.Logger;

public class CalculatorFSMBehaviour extends FSMBehaviour {

    private static final String STATE_WAIT = "WAIT";
    private static final String STATE_COMPUTE = "COMPUTE";

    private final Logger logger = Logger.getMyLogger(CalculatorFSMBehaviour.class.getName());

    private ACLMessage currentTask;
    private int a;
    private int b;
    private int current;
    private int result;

    public CalculatorFSMBehaviour(Agent aAgent) {
        super(aAgent);

        Behaviour waitState = createWaitState();
        Behaviour computeState = createComputeState();

        registerFirstState(waitState, STATE_WAIT);
        registerState(computeState, STATE_COMPUTE);

        registerTransition(STATE_WAIT, STATE_COMPUTE, 1);
        registerDefaultTransition(STATE_COMPUTE, STATE_WAIT);
    }

    private Behaviour createWaitState() {
        return new Behaviour(myAgent) {

            private boolean received = false;

            @Override
            public void onStart() {
                received = false;
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

                    currentTask = msg;
                    String content = msg.getContent();

                    try {
                        String[] parts = content.split(",");
                        a = Integer.parseInt(parts[0].trim());
                        b = Integer.parseInt(parts[1].trim());
                        current = a;
                        result = 0;

                        logger.info(myAgent.getLocalName()
                                + ": I got a problem " + content
                                + ". Switching to the COMPUTE state");

                        received = true;
                    } catch (Exception e) {
                        logger.warning(myAgent.getLocalName()
                                + ": I couldn't solve the problem " + content);
                        ACLMessage reply = msg.createReply();
                        reply.setPerformative(ACLMessage.DISCONFIRM);
                        reply.setLanguage("sum");
                        reply.setContent("Invalid range");
                        myAgent.send(reply);
                    }
                } else {
                    logger.info(myAgent.getLocalName()
                            + ": irrelevant message in WAIT state. Ignoring.");

                }
            }

            @Override
            public boolean done() {
                return received;
            }

            @Override
            public int onEnd() {
                return received ? 1 : 0;
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
                // Пока есть входящие REQUEST (от клиента или координатора) — отсылаем REFUSE
                ACLMessage msg;
                while ((msg = myAgent.receive()) != null) {
                    if (msg.getPerformative() == ACLMessage.REQUEST &&
                            "sum".equals(msg.getLanguage())) {

                        ACLMessage refuse = msg.createReply();
                        refuse.setPerformative(ACLMessage.REFUSE);
                        refuse.setLanguage("sum");
                        refuse.setContent("Calculator" + myAgent.getLocalName() + " busy now");
                        myAgent.send(refuse);

                        logger.info(myAgent.getLocalName()
                                + ": busy, sent a REFUSE for "
                                + msg.getSender().getLocalName());
                    } else {
                        logger.info(myAgent.getLocalName()
                                + ": Strange value. Ignoring. ");
                    }
                }

                // Эмуляция поэтапного вычисления (чтобы можно было наблюдать REFUSE по ходу дела)
                int stepsPerTick = 10_000; // регулирует "длительность" вычисления
                int count = 0;
                while (current <= b && count < stepsPerTick) {
                    result += current;
                    current++;
                    count++;
                }

                if (current > b) {
                    ACLMessage reply = currentTask.createReply();
                    reply.setPerformative(ACLMessage.CONFIRM);
                    reply.setLanguage("sum");
                    reply.setContent(String.valueOf(result));
                    myAgent.send(reply);

                    logger.info(myAgent.getLocalName()
                            + ": the calculation is completed. The result = " + result
                            + ". A response has been sent to the coordinator");

                    finished = true;
                } else {
                    block(50); // даём другим поведениями шанс прийти и получить REFUSE
                }
            }

            @Override
            public boolean done() {
                return finished;
            }

            @Override
            public int onEnd() {
                currentTask = null;
                a = b = current = result = 0;
                finished = false;
                return 0;
            }
        };
    }
}
