import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.util.Logger;

import java.util.*;

public class NodeAgent extends Agent {
    private Logger logger = Logger.getMyLogger(getClass().getName());
    private List<String> neighbors = new ArrayList<>();

    @Override
    protected void setup() {
        logger.info("Node " + getLocalName() + " started.");

        // Получаем список соседей из аргументов
        Object[] args = getArguments();
        if (args != null) {
            for (Object arg : args) {
                neighbors.add(arg.toString());
            }
        }
        logger.info("[" + getLocalName() + "] Neighbors: " + neighbors);

        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    // --- Обработка запроса маршрута ---
                    if (msg.getPerformative() == ACLMessage.REQUEST && "route".equals(msg.getLanguage())) {
                        String[] parts = msg.getContent().split(";");
                        String target = parts[0];
                        List<String> path = new ArrayList<>();
                        if (parts.length > 1 && parts[1].length() > 0) {
                            path.addAll(Arrays.asList(parts[1].split(",")));
                        }

                        logger.info("[" + getLocalName() + "] Received route request to " + target + " with path " + path);

                        // Добавляем текущий узел в маршрут
                        path.add(getLocalName());

                        if (getLocalName().equals(target)) {
                            // Цель достигнута
                            ACLMessage reply = msg.createReply();
                            reply.setPerformative(ACLMessage.CONFIRM);
                            reply.setLanguage("route");
                            reply.setContent(String.join("\n", path));
                            send(reply);
                            logger.info("[" + getLocalName() + "] Found target " + target + ". Route = " + path);
                        } else {
                            // Пересылаем соседям, которых ещё не посещали
                            boolean forwarded = false;
                            for (String neigh : neighbors) {
                                if (!path.contains(neigh)) {
                                    ACLMessage fwd = new ACLMessage(ACLMessage.REQUEST);
                                    fwd.setLanguage("route");
                                    fwd.setContent(target + ";" + String.join(",", path));
                                    fwd.addReceiver(new AID(neigh, AID.ISLOCALNAME));
                                    send(fwd);
                                    logger.info("[" + getLocalName() + "] Forwarded request to " + neigh);
                                    forwarded = true;
                                }
                            }

                            if (!forwarded) {
                                // Некуда идти → возвращаем отказ
                                ACLMessage reply = msg.createReply();
                                reply.setPerformative(ACLMessage.DISCONFIRM);
                                reply.setLanguage("route");
                                reply.setContent("Target " + target + " not found");
                                send(reply);
                                logger.info("[" + getLocalName() + "] Cannot reach " + target);
                            }
                        }
                    }
                    // --- Обработка результата маршрута ---
                    else if ((msg.getPerformative() == ACLMessage.CONFIRM || msg.getPerformative() == ACLMessage.DISCONFIRM)
                            && "route".equals(msg.getLanguage())) {
                        // Выводим результат в консоль и не пересылаем дальше → цикл прекращается
                        System.out.println("[" + getLocalName() + "] Route result:\n" + msg.getContent());
                        logger.info("[" + getLocalName() + "] Route result received: " + msg.getPerformative());
                    }
                    else {
                        block();
                    }
                } else {
                    block();
                }
            }
        });
    }
}
