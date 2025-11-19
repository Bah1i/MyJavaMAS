import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.FSMBehaviour;
import jade.lang.acl.ACLMessage;

public class CoordinatorFSM extends Agent {

    private static final String STATE_WAIT = "WAIT";
    private static final String STATE_SEND = "SEND";
    private static final String STATE_WAIT_RESULT = "WAIT_RESULT";

    private static final String MSG_KEY = "CLIENT_MSG";

    private String[] workers = {"calc1", "calc2", "calc3"};
    private int currentIndex = 0;

    @Override
    protected void setup() {
        FSMBehaviour fsm = new FSMBehaviour(this);

        fsm.registerFirstState(new WaitState(), STATE_WAIT);
        fsm.registerState(new SendState(), STATE_SEND);
        fsm.registerState(new WaitResultState(), STATE_WAIT_RESULT);

        fsm.registerTransition(STATE_WAIT, STATE_WAIT, 0);
        fsm.registerTransition(STATE_WAIT, STATE_SEND, 1);

        fsm.registerTransition(STATE_SEND, STATE_WAIT_RESULT, 0);
        fsm.registerTransition(STATE_WAIT_RESULT, STATE_WAIT, 0);

        addBehaviour(fsm);
        System.out.println(getLocalName() + " → Coordinator started");
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

                getDataStore().put(MSG_KEY, msg);
                System.out.println("Coordinator → Received task from client");
                received = true;
            } else {
                System.out.println("Coordinator → Received unrelated message. Ignoring.");
            }
        }

        @Override
        public boolean done() {
            return received;
        }

        @Override
        public int onEnd() {
            return received ? 1 : 0; // переход в SEND
        }
    }

    // ---------- SEND ----------
    private class SendState extends Behaviour {

        private boolean sent = false;

        @Override
        public void action() {
            ACLMessage orig = (ACLMessage) getDataStore().get(MSG_KEY);

            String workerName = workers[currentIndex];
            currentIndex = (currentIndex + 1) % workers.length;

            ACLMessage task = new ACLMessage(ACLMessage.REQUEST);
            task.setLanguage("sum");
            task.setContent(orig.getContent());
            task.addReceiver(new AID(workerName, AID.ISLOCALNAME));

            myAgent.send(task);
            System.out.println("Coordinator → Sent task to " + workerName);
            sent = true;
        }

        @Override
        public boolean done() {
            return sent;
        }

        @Override
        public int onEnd() {
            return 0; // переход в WAIT_RESULT
        }
    }

    // ---------- WAIT_RESULT ----------
    private class WaitResultState extends Behaviour {
        private boolean received = false;

        @Override
        public void action() {
            ACLMessage msg = myAgent.receive();
            if (msg == null) {
                block();
                return;
            }

            ACLMessage orig = (ACLMessage) getDataStore().get(MSG_KEY);
            ACLMessage reply = orig.createReply();
            reply.setPerformative(msg.getPerformative());
            reply.setLanguage("sum");
            reply.setContent(msg.getContent());
            myAgent.send(reply);

            System.out.println("Coordinator → Returned result to client: " + msg.getContent());
            received = true;
        }

        @Override
        public boolean done() {
            return received;
        }

        @Override
        public int onEnd() {
            return 0; // переход обратно в WAIT
        }
    }
}
