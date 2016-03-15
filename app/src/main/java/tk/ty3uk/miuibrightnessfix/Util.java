package tk.ty3uk.miuibrightnessfix;

import java.util.Arrays;

/**
 * Created by Ty3uK on 14.03.2016.
 */
public class Util {
    public static String IntArrayToString(int[] input) {
        String result = Arrays.toString(input);
        return result.substring(1, result.length() - 1);
    }

    public static int[] StringToIntArray(String input) throws NumberFormatException{
        String[] stringArray = input.split(",");
        int[] result = new int[stringArray.length];

        for(int i = 0; i < stringArray.length; i++)
            try {
                result[i] = Integer.parseInt(stringArray[i].trim());
            } catch (NumberFormatException nfe) {
                throw nfe;
            };

        return result;
    }
}
