package sc.fiji.bdvpg.command;

import java.util.ArrayList;

public class CommandHelper {

    /**
     * Convert a comma separated list of indexes into an arraylist of integer
     *
     * For instance 1,2,5-7,10-12,14 returns an ArrayList containing
     * [1,2,5,6,7,10,11,12,14]
     *
     * Invalid format are ignored and an error message is displayed
     *
     * @param expression
     * @return list of indexes in ArrayList
     */

    static public ArrayList<Integer> commaSeparatedListToArray(String expression) {
        String[] splitIndexes = expression.split(",");
        ArrayList<Integer> arrayOfIndexes = new ArrayList<>();
        for (String str : splitIndexes) {
            str.trim();
            if (str.contains(":")) {
                // Array of source, like 2:5 = 2,3,4,5
                String[] boundIndex = str.split(":");
                if (boundIndex.length==2) {
                    try {
                        int binf = Integer.valueOf(boundIndex[0].trim());
                        int bsup = Integer.valueOf(boundIndex[1].trim());
                        for (int index = binf; index <= bsup; index++) {
                            arrayOfIndexes.add(index);
                        }
                    } catch (NumberFormatException e) {
                        //LOGGER.warning("Number format problem with expression:"+str+" - Expression ignored");
                    }
                } else {
                    //LOGGER.warning("Cannot parse expression "+str+" to pattern 'begin-end' (2-5) for instance, omitted");
                }
            } else {
                // Single source
                try {
                    int index = Integer.valueOf(str.trim());
                    arrayOfIndexes.add(index);
                } catch (NumberFormatException e) {
                    //LOGGER.warning("Number format problem with expression:"+str+" - Expression ignored");
                }
            }
        }
        return arrayOfIndexes;
    }

}
