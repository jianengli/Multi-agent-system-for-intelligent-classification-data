package assignment1;

import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

/**
 * The whole project is coded by both of us, Junwei Gong and Jianeng Li
 */
public class ClassificationAgent extends Agent {


    @Override
    protected void setup() {

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("data-transforing");
        sd.setName("JADE-data-transforing");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
        }
        addBehaviour(new OfferRequestsServer());

        addBehaviour(new PurchaseOrdersServer());
    }

    /**
     * Put agent clean-up operations here
     */
    @Override
    protected void takeDown() {
        // Deregister from the yellow pages
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
        }

        // Printout a dismissal message
        System.out.println("Class-agent " + getAID().getName() + " terminating.");
    }


    public class OfferRequestsServer extends CyclicBehaviour {


        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                // CFP Message received. Process it
                String requiredAlgorithm = msg.getContent();
                ACLMessage reply = msg.createReply();

                // Get the name of the classagent to classificate as a start-up argument
                String algorithm = "";
                if (getAID() != null) {
                    algorithm = getAID().getName().substring(0, getAID().getName().indexOf("@"));
                }
                if (algorithm.equals(requiredAlgorithm)) {
                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContent(algorithm);

                } else {
                    reply.setPerformative(ACLMessage.REFUSE);
                    reply.setContent("not-available");
                }
                myAgent.send(reply);
            } else {
                block();
            }
        }
    }  // End of inner class OfferRequestsServer

    public class PurchaseOrdersServer extends CyclicBehaviour {

        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                // ACCEPT_PROPOSAL Message received. Process it
                String rawData = msg.getContent();
                ACLMessage reply = msg.createReply();
                RConnection connection = null;
                try {
                    connection = new RConnection();

                } catch (RserveException ex) {
                    Logger.getLogger(ClassificationAgent.class.getName()).log(Level.SEVERE, null, ex);
                }

                int vectorNamePosition = 0;
                int vectorEndPosition = 0;
                String createDataFrame = "<-data.frame(";

                int vectorNumber = getColumnSize(rawData, "\\[");
                for (int i = 0; i < vectorNumber / 2; i++) {
                    vectorNamePosition = getCharacterPosition(rawData, i + 1, "\\[");
                    vectorEndPosition = getCharacterPosition(rawData, i + 1, "\\]");
                    String training_vector = rawData.substring(vectorNamePosition + 1, vectorNamePosition + 5).replaceAll("^,*|,*$", "") + "<-c("
                            + rawData.substring(vectorNamePosition + 6, vectorEndPosition) + "),";
                    training_vector = training_vector.replaceAll("^,*|,*$", "");
                    training_vector = training_vector.replace(" ", "").replace("very_low", "VeryLow");

                    createDataFrame += rawData.substring(vectorNamePosition + 1, vectorNamePosition + 5);
                    try {
                        connection.eval(training_vector);
                    } catch (RserveException ex) {
                        Logger.getLogger(ClassificationAgent.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                createDataFrame += ")";
                try {

                    connection.eval("trainingSet" + createDataFrame);

                } catch (RserveException ex) {
                    Logger.getLogger(ClassificationAgent.class.getName()).log(Level.SEVERE, null, ex);
                }

                for (int i = vectorNumber / 2; i < vectorNumber; i++) {
                    vectorNamePosition = getCharacterPosition(rawData, i + 1, "\\[");
                    vectorEndPosition = getCharacterPosition(rawData, i + 1, "\\]");
                    String test_vector = rawData.substring(vectorNamePosition + 1, vectorNamePosition + 5).replaceAll("^,*|,*$", "") + "<-c("
                            + rawData.substring(vectorNamePosition + 6, vectorEndPosition) + "),";
                    test_vector = test_vector.replaceAll("^,*|,*$", "");
                    test_vector = test_vector.replace(" ", "");
                    try {
                        connection.eval(test_vector);
                    } catch (RserveException ex) {
                        Logger.getLogger(ClassificationAgent.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

                try {
                    connection.eval("testSet" + createDataFrame);
                    connection.voidEval("normalize<-function(x){\n"
                            + "  num<-x-min(x)\n"
                            + "  denum<-max(x)-min(x)\n"
                            + "  return (num/denum)\n"
                            + "}\n"
                            + "\n"
                            + "trainingSet_norm<-as.data.frame(lapply(trainingSet[1:5],normalize))\n"
                            + "trainingSet_norm$UNS<-trainingSet$UNS\n"
                            + "testSet_norm<-as.data.frame(lapply(testSet[1:5],normalize))\n"
                            + "testSet_norm$UNS<-testSet$UNS\n"
                            + "#Store in train and test\n"
                            + "trainingDataNorm<-trainingSet_norm[,1:5]\n"
                            + "testData_norm<-testSet_norm[,1:5]\n"
                            + "trainingLabelsNorm<-trainingSet_norm[,6]\n"
                            + "testLabelsNorm<-testSet_norm[,6]\n"
                            + "library(class)\n"
                            + "#Build the classifier\n"
                            + "data_pred_norm<-knn(train=trainingDataNorm , test = testData_norm, cl=trainingLabelsNorm, k=3)\n"
                            + "#Confusiontable\n"
                            + "confusiontTableNorm<-table(data_pred_norm,testLabelsNorm)\n"
                            + "accuracyNorm<-sum(diag(confusiontTableNorm))/sum(confusiontTableNorm)");
                    System.out.println("Performance measures:\n  The Confusion Matrix : ");
                    connection.eval("write.table(print(confusiontTableNorm),file ='F:/R project/result1.txt' ,sep =',')");
                    readTxt("F:/R project/result1.txt");
                    System.out.println("  The Accuracy : ");
                    REXP rexpAccuracy = connection.eval("accuracyNorm");
                    System.out.println(rexpAccuracy.asDouble());
                    System.out.println("  The Presicion : ");
                    connection.eval("precision <- diag(confusiontTableNorm) / rowSums(confusiontTableNorm)");
                    connection.eval("write.table(print(precision),file ='F:/R project/result1.txt' ,sep =',')");
                    readTxt("F:/R project/result1.txt");

                } catch (RserveException | IOException | REXPMismatchException ex) {
                    Logger.getLogger(ClassificationAgent.class.getName()).log(Level.SEVERE, null, ex);
                }

                System.out.println();

                reply.setPerformative(ACLMessage.INFORM);
                System.out.println("Data has been proccessed via " + getAID().getName());
                connection.close();

                myAgent.send(reply);
            } else {
                block();
            }
        }

        public void readTxt(String filePath) throws FileNotFoundException, IOException {
            FileInputStream fin = new FileInputStream(filePath);
            InputStreamReader reader = new InputStreamReader(fin);
            try (BufferedReader buffReader = new BufferedReader(reader)) {
                String strTmp = "";
                while ((strTmp = buffReader.readLine()) != null) {
                    System.out.println(strTmp);
                }
            }
        }

        public int getCharacterPosition(String data, int counter, String target) {
            Matcher slashMatcher = Pattern.compile(target).matcher(data);
            int mIdx = 0;
            while (slashMatcher.find()) {
                mIdx++;
                if (mIdx == counter) {
                    break;
                }
            }
            return slashMatcher.start();
        }

        public int getColumnSize(String string, String target) {
            Matcher slashMatcher = Pattern.compile(target).matcher(string);
            int mIdx = 0;
            while (slashMatcher.find()) {
                mIdx++;

            }
            return mIdx;
        }

        public double accuracyCalculating(String trainingData, String testData) {
            double accuracy = 0;
            return accuracy;
        }

    } 
}
