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
public class ClassificationAgent2 extends Agent {

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

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
        }
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
                String csvData = msg.getContent();
                csvData = csvData.substring(1, csvData.length());
                ACLMessage reply = msg.createReply();
                RConnection connection = null;
                try {
                    connection = new RConnection();
                } catch (RserveException ex) {
                    Logger.getLogger(ClassificationAgent2.class.getName()).log(Level.SEVERE, null, ex);
                }
                int columnNumber = getColumnSize(csvData, "=");
                System.out.println(columnNumber);

                String title = "";
                for (int i = 0; i < columnNumber; i++) {
                    String element = "<-c(";
                    element += csvData.substring(getCharacterPosition(csvData, i + 1, "\\[") + 1, getCharacterPosition(csvData, i + 1, "\\]"));
                    element = element.replace(" ", "");
                    element = element.replace("，", ",");
                    element += ")";
                    if (i == 0) {
                        title = csvData.substring(0, getCharacterPosition(csvData, i + 1, "="));
                        title = title.replace(" ", "");
                    } else {
                        title = csvData.substring(getCharacterPosition(csvData, i, "\\]") + 3, getCharacterPosition(csvData, i + 1, "="));
                        title = title.replace(" ", "");
                    }
                    System.out.println(title + element);
                    try {
                        connection.voidEval(title + element);
                    } catch (RserveException ex) {
                        Logger.getLogger(ClassificationAgent2.class.getName()).log(Level.SEVERE, null, ex);
                    }

                }

                try {
                    connection.voidEval("trainingSet<-data.frame(fixedacidity,volatileacidity,citricacid,residualsugar,chlorides,freesulfurdioxide,totalsulfurdioxide,density,pH,sulphates,alcohol,quality)");

                } catch (RserveException ex) {
                    Logger.getLogger(ClassificationAgent2.class.getName()).log(Level.SEVERE, null, ex);
                }

                try {
                    connection.eval("trainingSet2 <- trainingSet[,-12]  #Remove lable column\n"
                            + "kmeans_result <- kmeans(trainingSet2,7) #Run kmeans \n"
                            + "confusiontTable<-table(trainingSet$quality,kmeans_result$cluster)#View comparison of clustering results and observations\n"
                            + "\n"
                            + "png('F:/R project/plot.png',width=300*3,height=1*600,res=72*1)\n"
                            + "plot(trainingSet2[c('fixedacidity','volatileacidity')],col=kmeans_result$cluster)#The data set has 5 dimensions, and only the first two dimensions are used for the plot\n"
                            + "points(kmeans_result$centers[,c('fixedacidity','volatileacidity')],col=1:7,pch=10,cex=3)#Show cluster centers\n"
                            + "\n"
                            + "dev.off()");
                } catch (RserveException ex) {
                    Logger.getLogger(ClassificationAgent2.class.getName()).log(Level.SEVERE, null, ex);
                }
                ShowKmeansImage showImage = new ShowKmeansImage();
                System.out.println("Performance measures:\n  The Recall (completeness) : ");
                try {
                    connection.eval("recall <- (diag(confusiontTable) / colSums(confusiontTable))");
                } catch (RserveException ex) {
                    Logger.getLogger(ClassificationAgent2.class.getName()).log(Level.SEVERE, null, ex);
                }

                try {
                    connection.eval("write.table(recall,file ='F:/R project/result2.txt' ,sep =',')");
                } catch (RserveException ex) {
                    Logger.getLogger(ClassificationAgent2.class.getName()).log(Level.SEVERE, null, ex);
                }
                try {
                    readTxt("F:/R project/result2.txt");
                } catch (IOException ex) {
                    Logger.getLogger(ClassificationAgent2.class.getName()).log(Level.SEVERE, null, ex);
                }
                System.out.println("  The F1 score : ");
                try {
                    REXP rexpAccuracy = connection.eval("precision <- diag(confusiontTable) / rowSums(confusiontTable)\n"
                            + "f1_score<-2*((recall*precision)/(recall+precision)) ");
                } catch (RserveException ex) {
                    Logger.getLogger(ClassificationAgent2.class.getName()).log(Level.SEVERE, null, ex);
                }

                try {
                    connection.eval("write.table(f1_score,file ='F:/R project/result2.txt' ,sep =',')");
                } catch (RserveException ex) {
                    Logger.getLogger(ClassificationAgent2.class.getName()).log(Level.SEVERE, null, ex);
                }
                try {
                    readTxt("F:/R project/result2.txt");
                } catch (IOException ex) {
                    Logger.getLogger(ClassificationAgent2.class.getName()).log(Level.SEVERE, null, ex);
                }

//                System.out.print(rawData);
                reply.setPerformative(ACLMessage.INFORM);
                System.out.println("Data has been proccessed via " + getAID().getName());
                System.out.println();

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

        public int getCharacterPosition(String string, int counter, String target) {

            Matcher slashMatcher = Pattern.compile(target).matcher(string);
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

        public HashMap<String, ArrayList<Double>> readXls(String filepath) {
            File csv = new File(filepath);
            csv.setReadable(true);
            csv.setWritable(true);
            BufferedReader br = null;
            try {
                br = new BufferedReader(new FileReader(csv));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            String line = "";
            String everyLine = "";
            HashMap<String, ArrayList<Double>> allString = new HashMap<String, ArrayList<Double>>();
            String[] titles = new String[12];
            int index = 0;
            try {
                while ((line = br.readLine()) != null) {

                    everyLine = line;
                    if (index == 0) {
                        String firstLine = everyLine.substring(1, everyLine.length() - 1);
                        titles = firstLine.split("\";\"");
                        for (int i = 0; i < titles.length; i++) {
                            allString.put(titles[i], new ArrayList<Double>());
                        }
                    } else {
                        String[] dataLine = everyLine.split(";");
                        for (int i = 0; i < dataLine.length; i++) {
                            allString.get(titles[i]).add(Double.valueOf(dataLine[i]));
                        }
                    }
                    index += 1;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return allString;

        }

        public double accuracyCalculating(String trainingData, String testData) {
            double accuracy = 0;
            return accuracy;
        }

    }
}
