package assignment1;

import jade.core.AID;
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
import java.io.InputStream;
import java.util.*;
import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;

//  C:/Users/38079/OneDrive/桌面/rawData1.xls
//  C:/Users/38079/OneDrive/桌面/winequality-white.csv
/**
 * The whole project is coded by both of us, Junwei Gong and Jianeng Li
 */
public class DataAgent extends Agent {

    String path;
    // The list of known classifaction agents
    private AID[] classifactionAgent;
    private ArrayList<String> headsOfData = new ArrayList<>();

    /**
     * Put seller agent initializations here
     */
    @Override
    protected void setup() {
        // Printout a welcome message
        System.out.println("Hallo! Data-agent opened" + getAID().getName() + ".");

        // Get the path of the data to classificate as a start-up argument
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            path = (String) args[0];
            System.out.println("Target data is from" + path);

            // Add a TickerBehaviour that schedules a request to seller agents every minute
            addBehaviour(new TickerBehaviour(this, 5000) {
                @Override
                protected void onTick() {
                    System.out.println("Try to find an agent to process data");
                    // Update the list of seller agents
                    DFAgentDescription template = new DFAgentDescription();
                    ServiceDescription sd = new ServiceDescription();
                    sd.setType("data-transforing");
                    template.addServices(sd);
                    try {
                        DFAgentDescription[] result = DFService.search(myAgent, template);
                        System.out.println("Found the following classification agents:");
                        classifactionAgent = new AID[result.length];
                        for (int i = 0; i < result.length; ++i) {
                            classifactionAgent[i] = result[i].getName();
                            System.out.println(classifactionAgent[i].getName());
                        }
                    } catch (FIPAException fe) {
                    }
                    // Perform the request
                    myAgent.addBehaviour(new RequestPerformer());
                }
            });
        } else {
            // Make the agent terminate
            System.out.println("No path specified");
            doDelete();
        }
    }

    public String readExcel(String filepath) {
        String streamingData = "";
        try {
            File xls = new File(filepath); // CSV file path
            // Create input stream and read Excel
            InputStream is = new FileInputStream(xls);
            // Workbook class provided by jxl
            Workbook wb = Workbook.getWorkbook(is);
            // the number of pages of Excel
            int sheet_size = wb.getNumberOfSheets();
            for (int index = 1; index < sheet_size; index++) {
                List<List> outerList = new ArrayList<>();
                // every page creates a Sheet objective
                Sheet sheet = wb.getSheet(index);
                // sheet.getColumns()return the number of columns 
                int columnNumber = 0;
                for (int j = 0; j < sheet.getColumns(); j++) {
                    if (!sheet.getCell(j, 0).getContents().isEmpty()) {
                        columnNumber += 1;
                    } else {
                        break;
                    }
                }

                for (int j = 0; j < columnNumber; j++) {
                    List innerList = new ArrayList();
                    // sheet.getRows() return the number of lines
                    headsOfData.add(sheet.getCell(j, 0).getContents());

                    for (int i = 0; i < sheet.getRows(); i++) {
                        String cellinfo = sheet.getCell(j, i).getContents();
                        if (cellinfo.isEmpty()) {
                            break;
                        }

                        if (j == columnNumber - 1) {
                            if (i == 0) {
                                innerList.add(" UNS");
                            } else {
                                innerList.add("'" + cellinfo + "'");
                            }
                        } else {
                            innerList.add(cellinfo);
                        }

                    }
                    outerList.add(innerList);
                }
                streamingData += listToString(outerList, ',') + "\n" + "\n";
            }
        } catch (FileNotFoundException e) {
        } catch (BiffException | IOException e) {
        }
        return streamingData;
    }

    public static String listToString(List list, char separator) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i == list.size() - 1) {
                sb.append(list.get(i));
            } else {
                sb.append(list.get(i));
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    public HashMap<String, ArrayList<Double>> readCsv(String filepath) {
        File csv = new File(filepath); // CSV file path 
        csv.setReadable(true);//set readable
        csv.setWritable(true);//set writeable
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
        for (int i = 0; i < titles.length; i++) {
            headsOfData.add(titles[i]);
        }

        return allString;

    }

    @Override
    protected void takeDown() {
        // Printout a dismissal message
        System.out.println("Data-agent " + getAID().getName() + " terminating.");
    }

    public class RequestPerformer extends Behaviour {

        private String xlsDataString;
        private AID targetAlgorithm;
        private String targetAlgorithmName;

        private int repliesCnt = 0;
        private MessageTemplate mt;
        private int step = 0;

        @Override
        public void action() {
            switch (step) {
                case 0:
                    // Send the cfp to all agents
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                    for (int i = 0; i < classifactionAgent.length; ++i) {
                        cfp.addReceiver(classifactionAgent[i]);
                    }

                    if (path.substring(path.length() - 3).equals("xls")) {
                        xlsDataString = readExcel(path);
                        cfp.setContent("knn");
                    } else {
                        xlsDataString = readCsv(path).toString();
                        cfp.setContent("kmeans");
                    }

                    cfp.setConversationId("data-transfor");
                    cfp.setReplyWith("cfp" + System.currentTimeMillis()); // Unique value
                    myAgent.send(cfp);
                    // Prepare the template to get proposals
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("data-transfor"),
                            MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
                    step = 1;
                    break;
                case 1:
                    // Receive all proposals/refusals from other agents
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        // Reply received
                        if (reply.getPerformative() == ACLMessage.PROPOSE) {
                            if (targetAlgorithm == null) {
                                targetAlgorithmName = (reply.getContent());
                                targetAlgorithm = reply.getSender();
                            }
                        }
                        repliesCnt++;
                        if (repliesCnt >= classifactionAgent.length) {
                            // We received all replies
                            step = 2;
                        }
                    } else {
                        block();
                    }
                    break;
                case 2:
                    ACLMessage targetData = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                    targetData.addReceiver(targetAlgorithm);
                    targetData.setContent(xlsDataString);//xlsDataString
                    targetData.setConversationId("data-transfor");
                    targetData.setReplyWith("Data was transforred at" + System.currentTimeMillis());
                    myAgent.send(targetData);
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("data-transfor"),
                            MessageTemplate.MatchInReplyTo(targetData.getReplyWith()));
                    step = 3;
                    break;
                case 3:
                    reply = myAgent.receive(mt);
                    if (reply != null) {
                        if (reply.getPerformative() == ACLMessage.INFORM) {
                            System.out.println(reply.getSender().getName() + " successfully processed data: ");

                            myAgent.doDelete();
                        } else {
                            System.out.println("Attempt failed: ..");
                        }
                        step = 4;
                    } else {
                        block();
                    }
                    break;
            }
        }

        @Override
        public boolean done() {
            if (step == 2 && targetAlgorithm == null) {
                System.out.println("Attempt failed: no available classification agent");
            }
            return ((step == 2 && targetAlgorithm == null) || step == 4);
        }
    }
}
