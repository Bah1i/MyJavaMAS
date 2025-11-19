import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.FSMBehaviour;
import jade.lang.acl.ACLMessage;

public class CalculatorFSM extends Agent {

    private static final String STATE_WAIT = "WAIT";
    private static final String STATE_COMPUTE = "COMPUTE";

    private ACLMessage currentTask;
    private int a, b, current, result;

    @Override
    protected void setup() {
        FSMBehaviour fsm = new FSMBehaviour(this);

        fsm.registerFirstState(new WaitState(), STATE_WAIT);
        fsm.registerState(new ComputeState(), STATE_COMPUTE);

        fsm.registerTransition(STATE_WAIT, STATE_COMPUTE, 1);
        fsm.registerDefaultTransition(STATE_COMPUTE, STATE_WAIT);

        addBehaviour(fsm);
        System.out.println(getLocalName() + " → Calculator started");
    }

    // ---------- WAIT ----------
    private class WaitState extends Behaviour {
        private boolean received = false;

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

                try {
                    String[] parts = msg.getContent().split(",");
                    a = Integer.parseInt(parts[0].trim());
                    b = Integer.parseInt(parts[1].trim());
                    current = a;
                    result = 0;
                    System.out.println(getLocalName() + " → Received task: " + msg.getContent());
                    received = true;
                } catch (Exception e) {
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.DISCONFIRM);
                    reply.setLanguage("sum");
                    reply.setContent("Invalid range format");
                    myAgent.send(reply);
                }
            } else {
                System.out.println(getLocalName() + " → Received unrelated message. Ignoring.");
            }
        }

        @Override
        public boolean done() {
            return received;
        }

        @Override
        public int onEnd() {
            return received ? 1 : 0; // переход в COMPUTE
        }
    }

    // ---------- COMPUTE ----------
    private class ComputeState extends Behaviour {
        private boolean finished = false;

        @Override
        public void action() {
            // Обработка входящих REQUEST от других клиентов/координатора во время вычисления
            ACLMessage msg;
            while ((msg = myAgent.receive()) != null) {
                if (msg.getPerformative() == ACLMessage.REQUEST &&
                        "sum".equals(msg.getLanguage())) {
                    ACLMessage refuse = msg.createReply();
                    refuse.setPerformative(ACLMessage.REFUSE);
                    refuse.setLanguage("sum");
                    refuse.setContent(getLocalName() + " is busy computing");
                    myAgent.send(refuse);
                    System.out.println(getLocalName() + " → Busy. Sent REFUSE to " + msg.getSender().getLocalName());
                }
            }

            // Поэтапное вычисление, чтобы можно было увидеть REFUSE
            int stepsPerTick = 10_000;
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

                System.out.println(getLocalName() + " → Computation finished. Result = " + result);
                finished = true;
            } else {
                block(50);
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
            return 0; // возврат в WAIT
        }
    }
}
