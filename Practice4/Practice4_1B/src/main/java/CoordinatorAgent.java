import jade.core.AID;
import jade.core.Agent;
import jade.util.Logger;

import java.util.ArrayList;
import java.util.List;

public class CoordinatorAgent extends Agent {

    private final Logger logger = Logger.getMyLogger(CoordinatorAgent.class.getName());
    private final List<AID> workers = new ArrayList<>();

    @Override
    protected void setup() {
        logger.info("Координатор " + getLocalName() + " приступил к руководству");

        // поведение, обновляющее список работников через DF
        addBehaviour(new DFUpdateBehaviour(this, workers, 5000));

        // главная FSM-логика координатора
        addBehaviour(new CoordinatorFSMBehaviour(this, workers));
    }
}
