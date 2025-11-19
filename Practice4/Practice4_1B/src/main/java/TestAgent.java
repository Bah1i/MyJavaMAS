import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.ACLMessage;

public class TestAgent extends Agent {

    @Override
    protected void setup() {
        System.out.println("TestAgent started");

        addBehaviour(new Behaviour() {
            boolean done = false;

            @Override
            public void action() {
                try { Thread.sleep(2000); } catch (Exception ignored) {}

                ACLMessage req = new ACLMessage(ACLMessage.REQUEST);
                req.setLanguage("sum");
                req.setContent("1,100000000");
                req.addReceiver(new jade.core.AID("coordinator", jade.core.AID.ISLOCALNAME));
                send(req);

                System.out.println("TestAgent → Sent test REQUEST to coordinator");

                ACLMessage reply = blockingReceive(5000);

                if (reply == null) {
                    System.out.println("Test FAILED: no reply");
                    done = true;
                    return;
                }

                long expected = 1000L * 1001L / 2L;
                long actual = Long.parseLong(reply.getContent());

                if (actual == expected)
                    System.out.println("TEST PASSED ✔ Result = " + actual);
                else
                    System.out.println("TEST FAILED ❌ expected=" + expected + " got=" + actual);

                done = true;
            }

            @Override
            public boolean done() {
                return done;
            }
        });
    }
}
