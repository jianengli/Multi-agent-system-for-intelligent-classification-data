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
 * Main class used to create a tea seller agent
 */
public class ClassificationAgent3 extends Agent {

    /**
     * Put seller agent initializations here
     */
    @Override
    protected void setup() {
        // Create the catalogue
//        catalogue = new Hashtable();

//        // Create and show the GUI 
//        myGui = new TeaSellerGui(this);
//        myGui.showGui();
        // Register the data-transforing service in the yellow pages
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
        // Add the behaviour serving queries from buyer agents
        addBehaviour(new OfferRequestsServer());

        // Add the behaviour serving purchase orders from buyer agents
        addBehaviour(new PurchaseOrdersServer());
    }

    public static HashMap<String, ArrayList<Double>> readTransforedData(String data) {

        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(data));
        } catch (FileNotFoundException e) {
        }
        String line = "";
        String everyLine = "";
        HashMap<String, ArrayList<Double>> allString = new HashMap<>();
        String[] titles = new String[12];
        int index = 0;
        try {
            while ((line = br.readLine()) != null) // 读取到的内容给line变量
            {
                everyLine = line;
//                System.out.println(everyLine);
                if (index == 0) {
                    String firstLine = everyLine.substring(1, everyLine.length() - 1);
                    titles = firstLine.split("\";\"");
                    for (String title : titles) {
                        allString.put(title, new ArrayList<>());
                    }
                } else {
                    String[] dataLine = everyLine.split(";");
                    for (int i = 0; i < dataLine.length; i++) {
                        allString.get(titles[i]).add(Double.valueOf(dataLine[i]));
                    }
                }
                index += 1;
            }
//            System.out.println("csv表格中所有行数：" + allString.size());
        } catch (IOException e) {
        }
        return allString;
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

    /**
     * Inner class OfferRequestsServer. This is the behavior used by Tea-seller
     * agents to serve incoming requests for offer from buyer agents.
     */
    public class OfferRequestsServer extends CyclicBehaviour {

        /**
         * This is a method in OfferRequestsServer class used by Tea-seller
         * agents to serve incoming requests for offer from buyer agents. If the
         * requested tea is in the local catalogue the seller agent replies with
         * a PROPOSE message specifying the price. Otherwise a REFUSE message is
         * sent back.
         */
        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                // CFP Message received. Process it
                String requiredAlgorithm = msg.getContent();
                ACLMessage reply = msg.createReply();

//                Integer price = (Integer) catalogue.get(title);
                // Get the name of the classagent to classificate as a start-up argument
                String algorithm = "";
                if (getAID() != null) {
                    algorithm = getAID().getName().substring(0, getAID().getName().indexOf("@"));
                }
                if (algorithm.equals(requiredAlgorithm)) {
                    // The requested tea is available for sale. Reply with the price
                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContent(algorithm);
                } else {
                    // The requested tea is NOT available for sale.
                    reply.setPerformative(ACLMessage.REFUSE);
                    reply.setContent("not-available");
                }
                myAgent.send(reply);
            } else {
                block();
            }
        }
    }  // End of inner class OfferRequestsServer

    /**
     * Inner class PurchaseOrdersServer. This is the behavior used by Tea-seller
     * agents to serve incoming offer acceptances (i.e. purchase orders) from
     * buyer agents.
     */
    public class PurchaseOrdersServer extends CyclicBehaviour {

        /**
         * This is a method in PurchaseOrdersServer class used by Tea-seller
         * agents to serve incoming offer acceptances (i.e. purchase orders)
         * from buyer agents. The seller agent removes the purchased tea from
         * its catalogue and replies with an INFORM message to notify the buyer
         * that the purchase has been successfully completed.
         */
        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                // ACCEPT_PROPOSAL Message received. Process it
                String rawData = msg.getContent();
                ACLMessage reply = msg.createReply();
                //创建对象
                RConnection connection = null;
                try {
                    connection = new RConnection();

                } catch (RserveException ex) {
                    Logger.getLogger(ClassificationAgent3.class.getName()).log(Level.SEVERE, null, ex);
                }

                int vectorNamePosition = 0;
                int vectorEndPosition = 0;
                String createDataFrame = "<-data.frame(";

//                        String initTrainingSet="trainingSet<-data.frame(";
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
                        Logger.getLogger(ClassificationAgent3.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                createDataFrame += ")";
                try {

                    connection.eval("trainingSet" + createDataFrame);

                } catch (RserveException ex) {
                    Logger.getLogger(ClassificationAgent3.class.getName()).log(Level.SEVERE, null, ex);
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
                        Logger.getLogger(ClassificationAgent3.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

                try {
                    connection.eval("library(caret)\n"
                            + "library(\"klaR\")\n"
                            + "x = trainingSet[,-6]\n"
                            + "y = trainingSet$UNS\n"
                            + "model = train(x,y,'nb',trControl=trainControl(method='cv',number=10))\n"
                            + "model\n"
                            + "predict(model$finalModel,x)\n"
                            + "confusiontTable<-table(predict(model$finalModel,x)$class,y)\n"
                            + "accuracy<-sum(diag(confusiontTable))/sum(confusiontTable)\n"
                            + "precision <- diag(confusiontTable) / rowSums(confusiontTable)\n"
                            + "trainingSet <- NaiveBayes(trainingSet$UNS ~ ., data = trainingSet)\n"
                            + "png('F:/R project/plot_nb.png',width=300*3,height=1*600,res=72*1)\n"
                            + "plot(trainingSet)\n"
                            + "dev.off()");
                    ShowNBImage showImage = new ShowNBImage();

                    System.out.println("Performance measures:\n  The Confusion Matrix : ");
                    connection.eval("write.table(print(confusiontTable),file ='F:/R project/result3.txt' ,sep =',')");
                    readTxt("F:/R project/result3.txt");
                    System.out.println("  The Accuracy : ");
                    REXP rexpAccuracy = connection.eval("accuracy");
                    System.out.println(rexpAccuracy.asDouble());
                    System.out.println("  The Presicion : ");
                    connection.eval("write.table(print(precision),file ='F:/R project/result3.txt' ,sep =',')");
                    readTxt("F:/R project/result3.txt");

                } catch (RserveException | IOException | REXPMismatchException ex) {
                    Logger.getLogger(ClassificationAgent3.class.getName()).log(Level.SEVERE, null, ex);
                }

//                System.out.println("the mean of given vector is="+mean);
//                System.out.print(rawData);
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
            //这里是获取"/"符号的位置
            Matcher slashMatcher = Pattern.compile(target).matcher(data);
            int mIdx = 0;
            while (slashMatcher.find()) {
                mIdx++;
                //当"/"符号第三次出现的位置
                if (mIdx == counter) {
                    break;
                }
            }
            return slashMatcher.start();
        }

        public int getColumnSize(String string, String target) {
            //这里是获取"/"符号的位置
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

    }  // End of inner class OfferRequestsServer
}
