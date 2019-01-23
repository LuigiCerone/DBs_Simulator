package Threads;

import Model.DatabaseRawData;
import Model.Tool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Random;

public class RawDataInsertThread implements Runnable {
    private int MAX_DELAY = 10000; // 10 sec.

    // Each char is 1 byte in SQL, so 10MB = 10 millions of chars.
    // 8MB instead of 10MB because 8MB is the max default value allowed (without changing max_allowed_packet).
//    private int SIZE = 8000000;
    private int SIZE = 2000000;

    private ArrayList<Tool> tools;
    private DatabaseRawData databaseRawData;

    public RawDataInsertThread(ArrayList<Tool> tools) {
        this.tools = tools;
        this.databaseRawData = new DatabaseRawData();
    }

    @Override
    public void run() {

        while (true) {
            Random random = new Random();

            int index = random.nextInt((tools.size() - 1) + 1);
            Tool currTool = tools.get(index);

            int whichField = random.nextInt(3);
            String oid = null;
            String nameTranslation = null;
            if (whichField == 0) {
                // This means we need to insert in raw_data a equip OID translation.
                oid = currTool.getEquipOID();
                nameTranslation = "equipName" + currTool.getEquipOID();
            } else if (whichField == 1) {
                // This means we need to insert in raw_data a recipe OID translation.
                oid = currTool.getRecipeOID();
                nameTranslation = "recipeName" + currTool.getRecipeOID();
            } else if (whichField == 2) {
                // This means we need to insert in raw_data a step OID translation.
                oid = currTool.getStepOID();
                nameTranslation = "stepName" + currTool.getStepOID();
            }

            Connection connection = databaseRawData.getConnection();

            // Add analytics.
            store(oid, nameTranslation, whichField, connection);

            try {
                Thread.sleep(random.nextInt(MAX_DELAY - 1) + 1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean store(String oid, String nameTranslation, int type, Connection connection) {
        PreparedStatement stmt = null;

        String query = "INSERT INTO analytics (id, oid, nameTranslation, type, fakeData) " +
                "VALUE (null, ?,?,?,?);";

        char[] fakeData = new char[SIZE];
        String fakeString = new String(fakeData);

        boolean done = false;
        try {


            stmt = connection.prepareStatement(query);
            stmt.setString(1, oid);
            stmt.setString(2, nameTranslation);
            stmt.setInt(3, type);
            stmt.setString(4, fakeString);

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
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return done;
    }
}
