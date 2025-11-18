import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.util.Logger;

import java.util.*;

public class CoordinatorAgent extends Agent {
    private Logger logger = Logger.getMyLogger(getClass().getName());
    private List<AID> workers = new ArrayList<>();
    private Map<String, Integer> results = new HashMap<>();
    private ACLMessage originalRequest;

    @Override
    protected void setup() {
        logger.info("Coordinator agent " + getLocalName() + " is ready.");

        // Периодическое обновление списка агентов каждые 5 секунд
        addBehaviour(new TickerBehaviour(this, 5000) {
            @Override
            protected void onTick() {
                updateWorkers();
            }
        });

        // Основное поведение обработки сообщений
        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    if (msg.getPerformative() == ACLMessage.REQUEST && "sum".equals(msg.getLanguage())) {
                        originalRequest = msg;
                        results.clear();

                        String[] parts = msg.getContent().split(",");
                        int a = Integer.parseInt(parts[0].trim());
                        int b = Integer.parseInt(parts[1].trim());

                        if (workers.isEmpty()) {
                            logger.warning("No workers available!");
                            ACLMessage reply = msg.createReply();
                            reply.setPerformative(ACLMessage.FAILURE);
                            reply.setContent("No workers available");
                            send(reply);
                            return;
                        }

                        int totalRange = b - a + 1;
                        int chunk = totalRange / workers.size();
                        int remainder = totalRange % workers.size();

                        int start = a;
                        for (int i = 0; i < workers.size(); i++) {
                            int end = start + chunk - 1;
                            if (i < remainder) end++;

                            ACLMessage req = new ACLMessage(ACLMessage.REQUEST);
                            req.setLanguage("sum");
                            req.setContent(start + "," + end);
                            req.addReceiver(workers.get(i));
                            send(req);

                            logger.info("[" + getLocalName() + "] sent task " + start + "," + end + " to " + workers.get(i).getLocalName());
                            start = end + 1;
                        }
                    }
                    else if (msg.getPerformative() == ACLMessage.CONFIRM && "sum".equals(msg.getLanguage())) {
                        results.put(msg.getSender().getLocalName(), Integer.parseInt(msg.getContent()));
                        if (results.size() == workers.size()) {
                            int total = results.values().stream().mapToInt(Integer::intValue).sum();
                            logger.info("[" + getLocalName() + "] Final result = " + total);

                            ACLMessage reply = originalRequest.createReply();
                            reply.setPerformative(ACLMessage.CONFIRM);
                            reply.setLanguage("sum");
                            reply.setContent(String.valueOf(total));
                            send(reply);

                            results.clear();
                        }
                    } else {
                        block();
                    }
                } else {
                    block();
                }
            }
        });
    }

    private void updateWorkers() {
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("sum-service");
        template.addServices(sd);

        try {
            DFAgentDescription[] result = DFService.search(this, template);
            workers.clear();
            for (DFAgentDescription desc : result) {
                workers.add(desc.getName());
            }

            if (workers.isEmpty()) {
                logger.warning("[" + getLocalName() + "] No workers found in DF.");
            } else {
                StringBuilder sb = new StringBuilder("[" + getLocalName() + "] Updated worker list: ");
                for (AID worker : workers) {
                    sb.append(worker.getLocalName()).append(" ");
                }
                logger.info(sb.toString().trim());
            }

        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }
}
