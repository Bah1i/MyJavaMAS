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

        Object[] args = getArguments();
        if (args != null) {
            for (Object arg : args) neighbors.add(arg.toString());
        }

        logger.info("[" + getLocalName() + "] Neighbors: " + neighbors);

        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {

                    // === Запрос на поиск пути ===
                    if (msg.getPerformative() == ACLMessage.REQUEST &&
                            "route".equals(msg.getLanguage())) {

                        String[] parts = msg.getContent().split(";");
                        String target = parts[0];
                        List<String> path = new ArrayList<>();

                        if (parts.length > 1 && !parts[1].isEmpty())
                            path.addAll(Arrays.asList(parts[1].split(",")));

                        path.add(getLocalName());

                        logger.info("[" + getLocalName() + "] BFS step → path: " + path);

                        if (getLocalName().equals(target)) {
                            // --- Цель найдена ---
                            ACLMessage reply = msg.createReply();
                            reply.setPerformative(ACLMessage.CONFIRM);
                            reply.setLanguage("route");
                            reply.setContent(String.join("->", path));
                            send(reply);
                            logger.info("[" + getLocalName() + "] FOUND target! Path = " + path);
                            return;
                        }

                        // Пересылаем только непосещённым соседям (BFS)
                        boolean forwarded = false;
                        for (String neigh : neighbors) {
                            if (!path.contains(neigh)) {
                                ACLMessage fwd = new ACLMessage(ACLMessage.REQUEST);
                                fwd.setLanguage("route");
                                fwd.addReceiver(new AID(neigh, AID.ISLOCALNAME));
                                fwd.setContent(target + ";" + String.join(",", path));
                                send(fwd);
                                forwarded = true;
                            }
                        }

                        if (!forwarded) {
                            ACLMessage reply = msg.createReply();
                            reply.setPerformative(ACLMessage.DISCONFIRM);
                            reply.setLanguage("route");
                            reply.setContent("Dead end at " + getLocalName());
                            send(reply);
                        }
                    }

                    // === Получили результат поиска ===
                    else if ("route".equals(msg.getLanguage()) &&
                            (msg.getPerformative() == ACLMessage.CONFIRM ||
                                    msg.getPerformative() == ACLMessage.DISCONFIRM)) {

                        logger.info("[" + getLocalName() + "] Route result:\n" + msg.getContent());
                    }

                    else {
                        block();
                    }
                }
                else block();
            }
        });
    }
}
