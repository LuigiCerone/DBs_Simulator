package Threads;

import Model.DatabaseRawData;
import Model.Tool;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Random;

public class RawDataInsertThread implements Runnable {
    private int MAX_DELAY = 10000; // 10 sec.

    private ArrayList<Tool> tools;
    private DatabaseRawData databaseRawData;

    public RawDataInsertThread(ArrayList<Tool> tools) {
        this.tools = tools;
        this.databaseRawData = new DatabaseRawData();
    }

    @Override
    public void run() {
        String threadName = Thread.currentThread().getName();
        Random random = new Random();

        int index = random.nextInt((tools.size() - 1) + 1);
        Tool currTool = tools.get(index);

        Connection connection = databaseRawData.getConnection();

        // Add analytics.


        try {
            Thread.sleep(random.nextInt(MAX_DELAY - 1) + 1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
