import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.util.Logger;

import java.util.List;
import java.util.ArrayList;

public class DFUpdateBehaviour extends TickerBehaviour {

    private final List<AID> workers;
    private final Logger logger = Logger.getMyLogger(DFUpdateBehaviour.class.getName());

    public DFUpdateBehaviour(Agent a, List<AID> workers, long periodMillis) {
        super(a, periodMillis);
        this.workers = workers;
    }

    @Override
    protected void onTick() {
        try {
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("calculator");
            template.addServices(sd);

            DFAgentDescription[] result = DFService.search(myAgent, template);

            // build new list
            List<AID> newList = new ArrayList<>();
            for (DFAgentDescription dfd : result) {
                newList.add(dfd.getName());
            }

            synchronized (workers) {
                workers.clear();
                workers.addAll(newList);
            }

            logger.info(myAgent.getLocalName() + ": DF update, found " + newList.size() + " calculators");

        } catch (FIPAException e) {
            logger.warning("DFUpdateBehaviour: DF search failed: " + e.getMessage());
        }
    }
}
