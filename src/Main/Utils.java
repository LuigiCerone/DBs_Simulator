package Main;

import Model.Tool;

import java.util.ArrayList;
import java.util.Random;

public class Utils {
    Random random = new Random();

    public ArrayList<String> createFakeData(int COUNT) {
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
    public Tool createFakeRobot(String recipeOID, String stepOID) {
        String toolOID = getRandomHexString(6);

        return new Tool(toolOID, recipeOID, stepOID);
    }

    public String getRandomHexString(int numchars) {
        Random r = new Random();
        StringBuffer sb = new StringBuffer();
        while (sb.length() < numchars) {
            sb.append(Integer.toHexString(r.nextInt()));
        }

        return sb.toString().substring(0, numchars).toUpperCase();
    }

}
