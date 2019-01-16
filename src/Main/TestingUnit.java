package Main;

import Model.DatabaseFabData;
import Model.Tool;
import Threads.FabDataInsertThread;
import Threads.RawDataInsertThread;
import Threads.ShutDownThread;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TestingUnit extends JFrame {
    ArrayList<Tool> tools;

    public static int totalRequestsNumber = 0;
    static long startTime = 0;
    static boolean userWants = true;
    private DatabaseFabData databaseFabData = new DatabaseFabData();
    private Utils utils = new Utils();


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

        JLabel robotNumberLabel = new JLabel("Tools number: ");
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
                System.out.println("Number of robots: " + robotNumber.getText() + ", with pause size: " + pauseSize.getText());
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

                System.exit(0);
//                userWants = false;
            }
        });
    }

    private void run(String toolNumber, String pauseSize) {
        long p1;
        try {
            p1 = Long.parseLong(pauseSize);
        } catch (NumberFormatException e) {
            p1 = 1000;
        }
        final long pause = p1;
//        int TOOLS_NUMBER = 1;

        int t;
        try {
            t = Integer.parseInt(toolNumber);
        } catch (Exception e) {
            t = 1000;
        }

        final int TOOLS_NUMBER = t;

        // Create an array of fake Robots.
        tools = new ArrayList<Tool>(TOOLS_NUMBER);

        int RECIPES_NUMBER = TOOLS_NUMBER / 10;
        int STEP_NUMBER = RECIPES_NUMBER / 10;
        ArrayList<String> recipes = utils.createFakeData(RECIPES_NUMBER);
        ArrayList<String> steps = utils.createFakeData(STEP_NUMBER);

        Random random = new Random();
        for (int i = 0; i < TOOLS_NUMBER; i++) {
            tools.add(utils.createFakeRobot(recipes.get(random.nextInt(recipes.size())), steps.get(random.nextInt(steps.size()))));
        }

        System.out.println("Array of fake robots has been created.");
        startTime = System.currentTimeMillis();

        Runtime.getRuntime().addShutdownHook(new ShutDownThread(startTime, this));


        ExecutorService executor = Executors.newFixedThreadPool(2);

        // Fab data threads.
        executor.submit(new Runnable() {
            public void run() {
                // Pool for the threads that will store data into fab_data.
                while (userWants) {
                    System.out.println("FabDataInsertThread started");

                    ExecutorService pool = Executors.newFixedThreadPool(4);
                    DatabaseFabData databaseFabData = new DatabaseFabData();

//                    System.out.println(pause);

                    Random random = new Random();
                    int index = random.nextInt((TOOLS_NUMBER - 1) + 1);
                    Tool currTool = tools.get(index);

                    Connection connection = databaseFabData.getConnection();

                    Runnable worker = new FabDataInsertThread(currTool, connection);
//                    worker.run();
                    pool.execute(worker); //calling execute method of ExecutorService

                    totalRequestsNumber++;

                    try {
                        Thread.sleep(pause);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        // Raw data thread.
        Runnable rawDataThread = new RawDataInsertThread(tools);
        System.out.println("RawDataInsertThread started");

        executor.submit(rawDataThread);


    }

    public int getTotalRequestsNumber() {
        return totalRequestsNumber;
    }
}
