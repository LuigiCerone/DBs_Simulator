
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TestingUnit extends JFrame {
    ArrayList<Tool> tools;
    Random random = new Random();
    public static int totalRequestsNumber = 0;
    static long startTime = 0;
    static boolean userWants = true;
    private Database database = new Database();


    public static void main(String[] args) {
        final TestingUnit testingUnit = new TestingUnit();

        JButton STARTButton = new JButton("Start");
        JButton STOPButton = new JButton("Stop");


        final JTextField robotNumber = new JTextField(6);
        final JTextField pauseSize = new JTextField(6);

        final JTextArea stats = new JTextArea();
        stats.setEditable(false);


        JFrame frame = new JFrame("Testing unit");
//        frame.setSize(400, 400);
        frame.setResizable(false);

        Container frameContentPane = frame.getContentPane();
        frameContentPane.setLayout(new BoxLayout(frameContentPane, BoxLayout.Y_AXIS));

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new FlowLayout());
        buttonsPanel.add(STARTButton);
        buttonsPanel.add(STOPButton);
        frameContentPane.add(buttonsPanel);

        // ==================================================================0

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.Y_AXIS));

        JLabel robotNumberLabel = new JLabel("Robots number: ");
        inputPanel.add(robotNumberLabel);
        inputPanel.add(robotNumber);

        JLabel pauseSizeLabel = new JLabel("Pause size: ");
        inputPanel.add(pauseSizeLabel);
        inputPanel.add(pauseSize);
        frameContentPane.add(inputPanel);

        // ==================================================================

        JPanel logPanel = new JPanel();
        logPanel.setLayout(new FlowLayout());
        JLabel infoLabel = new JLabel("Info: ");
        logPanel.add(infoLabel);
        logPanel.add(stats);
        frameContentPane.add(logPanel);

        // ==================================================================

//        frame.setContentPane(new StartTest().panelMain);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);

        STARTButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println(robotNumber.getText() + " " + pauseSize.getText());
                userWants = true;

                totalRequestsNumber = 0;
                new Thread() {
                    public void run() {
                        testingUnit.run(robotNumber.getText(), pauseSize.getText());
                    }
                }.start();
            }
        });

        STOPButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                long endTime = System.currentTimeMillis();
//                stats.setText("AAA");
                System.out.println("Requests: " + totalRequestsNumber);

                stats.setText(" " + totalRequestsNumber + " requests made in " + (endTime - startTime) + " ms.");

//                System.exit(0);
                userWants = false;
            }
        });
    }

    private void run(String toolNumber, String pauseSize) {

//        int TOOLS_NUMBER = 1;
        int TOOLS_NUMBER;
        try {
            TOOLS_NUMBER = Integer.parseInt(toolNumber);
        } catch (Exception e) {
            TOOLS_NUMBER = 1000;
        }
        // Create an array of fake Robots.
        tools = new ArrayList<Tool>(TOOLS_NUMBER);

        int RECIPES_NUMBER = TOOLS_NUMBER / 10;
        int STEP_NUMBER = RECIPES_NUMBER / 10;
        ArrayList<String> recipes = createFakeData(RECIPES_NUMBER);
        ArrayList<String> steps = createFakeData(STEP_NUMBER);

        for (int i = 0; i < TOOLS_NUMBER; i++) {
            tools.add(createFakeRobot(recipes.get(random.nextInt(recipes.size())), steps.get(random.nextInt(steps.size()))));
        }

        System.out.println("Array has been created.");
        startTime = System.currentTimeMillis();
//        Runtime.getRuntime().addShutdownHook(new shutDownHook(startProgram));

        ExecutorService executor = Executors.newFixedThreadPool(4);//creating a pool of 4 threads

        while (userWants) {

            int index = random.nextInt((TOOLS_NUMBER - 1) + 1);
            Tool currTool = tools.get(index);

            Runnable worker = new SenderThread(currTool);
            executor.execute(worker); //calling execute method of ExecutorService

//            sendData(currRobot);
            totalRequestsNumber++;
            long pause;
            try {
                pause = Long.parseLong(pauseSize);
            } catch (NumberFormatException e) {
                pause = 1000;
            }
            try {
                Thread.sleep(pause);
//                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private ArrayList<String> createFakeData(int COUNT) {
        int minBound = COUNT - 10;
        if (minBound < 0) minBound = 0;
        int length = random.nextInt(((COUNT + 10) - minBound) + minBound);
        ArrayList<String> array = new ArrayList<>(length);

        for (int i = 0; i < length; i++) {
            array.add(getRandomHexString(6));
        }
        return array;
    }

    // Create a new tool with random data.
    private Tool createFakeRobot(String recipeOID, String stepOID) {
        String toolOID = getRandomHexString(6);

        return new Tool(toolOID, recipeOID, stepOID);
    }

    private String getRandomHexString(int numchars) {
        Random r = new Random();
        StringBuffer sb = new StringBuffer();
        while (sb.length() < numchars) {
            sb.append(Integer.toHexString(r.nextInt()));
        }

        return sb.toString().substring(0, numchars).toUpperCase();
    }

    // Thread that create a JSON payload and sent it with a POST requests to our system.
    public class SenderThread implements Runnable {
        Tool tool;

        public SenderThread(Tool tool) {
            this.tool = tool;
        }

        @Override
        public void run() {
//            long start = System.currentTimeMillis();
            try {

                Event event = new Event(this.tool, "holdType", this.tool.isOnHold(),
                        new Date(System.currentTimeMillis()));

                boolean oldValue = this.tool.isOnHold();
                event.setHoldFlag(!oldValue);

                // Uncomment to see the data.
                // System.out.println(jsonObject.toString());

                // Now we create a POST request with jsonObject as data.
                store(event);

            } catch (Exception e) {
                e.printStackTrace();
            }

//            long end = System.currentTimeMillis();
//            System.out.println("One single request took : " + (end - start) + "ms.");

        }

        private boolean store(Event event) {
            Connection conn = null;
            PreparedStatement stmt = null;

            String query = "INSERT INTO event (id, equip, recipe, step, holdtype, holdflag, datetime) " +
                    "VALUE (null, ?,?,?,?,?,?);";
            boolean done = false;
            try {
                conn = database.getConnection();

                stmt = conn.prepareStatement(query);
                stmt.setString(1, event.getTool().getEquipOID());
                stmt.setString(2, event.getTool().getRecipeOID());
                stmt.setString(3, event.getTool().getStepOID());
                stmt.setString(4, event.getHoldType());
                stmt.setBoolean(5, event.isHoldFlag());
                stmt.setDate(6, event.getDateTime());

                int rows = stmt.executeUpdate();
                if (rows > 0) {
                    done = true;
                } else done = false;
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                if (stmt != null) {
                    try {
                        stmt.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
                if (conn != null) {
                    try {
                        conn.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
            return done;
        }
    }

    // Thread used for display statistics when the Virtual Machine exit() is called.
//    public class shutDownHook extends Thread {
//        long startTime;
//
//        public shutDownHook(long startTime) {
//            this.startTime = startTime;
//        }
//
//        @Override
//        public void run() {
//            long endTime = System.currentTimeMillis();
//            System.out.println("I've run for : " + (endTime - startTime) + "ms and I've done :" + totalRequestsNumber + " requests.");
//        }
//    }
}
