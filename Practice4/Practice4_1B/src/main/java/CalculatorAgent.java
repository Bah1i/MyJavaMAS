import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.util.Logger;

public class CalculatorAgent extends Agent {

    private final Logger logger = Logger.getMyLogger(CalculatorAgent.class.getName());

    @Override
    protected void setup() {
        logger.info("Калькулятор " + getLocalName() + " вышел на смену");

        try {
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());

            ServiceDescription sd = new ServiceDescription();
            sd.setType("calculator");
            sd.setName("sum-calculator");

            dfd.addServices(sd);

            DFService.register(this, dfd);
            logger.info(getLocalName() + ": зарегистрировался в DF как calculator");
        } catch (FIPAException e) {
            logger.severe(getLocalName() + ": ошибка регистрации в DF: " + e.getMessage());
        }

        addBehaviour(new CalculatorFSMBehaviour(this));
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
            logger.info(getLocalName() + ": снялся с регистрации в DF");
        } catch (FIPAException e) {
            logger.severe(getLocalName() + ": ошибка снятия регистрации в DF: " + e.getMessage());
        }
    }
}
