import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.util.Logger;

import java.util.Arrays;
import java.util.Comparator;

public class LeaderElectionBehaviour extends OneShotBehaviour {

    private final Logger logger = Logger.getMyLogger(LeaderElectionBehaviour.class.getName());

    @Override
    public void action() {
        try {
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("calculator");
            template.addServices(sd);

            DFAgentDescription[] calculators = DFService.search(myAgent, template);

            if (calculators.length == 0) {
                logger.warning(myAgent.getLocalName() + ": no calculators found in DF for leader election");
                return;
            }

            // Выбираем координатора по наименьшему имени
            DFAgentDescription leader = Arrays.stream(calculators)
                    .min(Comparator.comparing(d -> d.getName().getLocalName()))
                    .orElse(calculators[0]);

            if (leader.getName().equals(myAgent.getAID())) {
                // Этот агент выбран координатором
                logger.info(myAgent.getLocalName() + ": elected as coordinator");

                // Регистрируемся в DF как координатор
                DFAgentDescription dfd = new DFAgentDescription();
                dfd.setName(myAgent.getAID());
                ServiceDescription sdc = new ServiceDescription();
                sdc.setType("coordinator");
                sdc.setName("sum-coordinator");
                dfd.addServices(sdc);

                DFService.register(myAgent, dfd);
            } else {
                logger.info(myAgent.getLocalName() + ": not elected as coordinator, leader is "
                        + leader.getName().getLocalName());
            }

        } catch (FIPAException e) {
            logger.severe(myAgent.getLocalName() + ": DF search error in leader election: " + e.getMessage());
        }
    }
}
