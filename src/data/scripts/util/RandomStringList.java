package data.scripts.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RandomStringList {
    
    public static List<String> generateRandomStrings(int listSize, int stringLength) {
        Random random = new Random();
        List<String> randomStrings = new ArrayList<>();
        
        for (int i = 0; i < listSize; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < stringLength; j++) {
                char randomChar = (char) ('a' + random.nextInt(26)); 
                sb.append(randomChar);
            }
            randomStrings.add(sb.toString());
        }
        
        return randomStrings;
    }
}