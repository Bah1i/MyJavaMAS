import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.TickerBehaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.util.Logger;

import java.util.*;

public class ClientAgent extends Agent {

    private final Logger logger = Logger.getMyLogger(ClientAgent.class.getName());
    private final Random rand = new Random();

    private String[] coordinators = {"coordinator1", "coordinator2"};

    @Override
    protected void setup() {
        logger.info("Client " + getLocalName() + " ready to send requests.");

        // Отправка задачи каждые 2 секунды
        addBehaviour(new TickerBehaviour(this, 2000) {
            @Override
            protected void onTick() {

                // Генерация случайного диапазона
                int a = rand.nextInt(1000);
                int b = a + rand.nextInt(1000);

                // Эталонная сумма
                int expectedSum = (b - a + 1) * (a + b) / 2;

                // Отправляем задачу всем координаторам
                Set<String> awaiting = new HashSet<>();
                for (String coord : coordinators) {
                    ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                    msg.setLanguage("sum");
                    msg.setContent(a + "," + b);
                    msg.addReceiver(new AID(coord, AID.ISLOCALNAME));
                    send(msg);
                    logger.info(getLocalName() + ": sent task [" + a + ";" + b + "] to " + coord);
                    awaiting.add(coord);
                }

                // Поведение для отслеживания всех ответов
                addBehaviour(new CyclicBehaviour() {

                    @Override
                    public void action() {
                        ACLMessage reply = receive();
                        if (reply != null) {
                            String sender = reply.getSender().getLocalName();

                            if (reply.getPerformative() == ACLMessage.CONFIRM) {
                                int receivedSum = Integer.parseInt(reply.getContent().trim());
                                if (receivedSum == expectedSum) {
                                    logger.info(getLocalName() + ": received correct result " +
                                            receivedSum + " from " + sender);
                                } else {
                                    logger.warning(getLocalName() + ": received WRONG result " +
                                            receivedSum + " from " + sender +
                                            " (expected " + expectedSum + ")");
                                }
                                awaiting.remove(sender);
                            } else if (reply.getPerformative() == ACLMessage.REFUSE ||
                                    reply.getPerformative() == ACLMessage.DISCONFIRM) {
                                logger.warning(getLocalName() + ": request refused by " + sender +
                                        " (" + reply.getContent() + ")");
                                awaiting.remove(sender);
                            }

                            // Если получили ответы от всех координаторов — завершаем
                            if (awaiting.isEmpty()) {
                                stop();
                            }
                        } else {
                            block();
                        }
                    }
                });
            }
        });
    }
}
