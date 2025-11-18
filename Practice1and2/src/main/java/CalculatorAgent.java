import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.util.Logger;

public class CalculatorAgent extends Agent {
    private Logger logger = Logger.getMyLogger(getClass().getName());

    @Override
    protected void setup() {
        logger.info("Calculator agent " + getLocalName() + " is ready.");

        // Регистрируемся в DF как сервис "sum-service"
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());

        ServiceDescription sd = new ServiceDescription();
        sd.setType("sum-service");
        sd.setName("JADE-sum");
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
            logger.info(getLocalName() + " registered in DF as sum-service");
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        // Поведение обработки сообщений
        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    if (msg.getPerformative() == ACLMessage.REQUEST &&
                            "sum".equals(msg.getLanguage())) {

                        // Разбираем диапазон
                        String[] parts = msg.getContent().split(",");
                        int a = Integer.parseInt(parts[0].trim());
                        int b = Integer.parseInt(parts[1].trim());

                        // Считаем сумму
                        int sum = 0;
                        for (int i = a; i <= b; i++) sum += i;

                        logger.info("[" + getLocalName() + "] computed sum(" + a + "," + b + ") = " + sum);

                        // Отправляем CONFIRM назад
                        ACLMessage reply = msg.createReply();
                        reply.setPerformative(ACLMessage.CONFIRM);
                        reply.setLanguage("sum");
                        reply.setContent(String.valueOf(sum));
                        send(reply);
                    } else {
                        block();
                    }
                } else {
                    block();
                }
            }
        });
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        logger.info("Calculator agent " + getLocalName() + " terminated.");
    }
}
