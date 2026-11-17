import java.util.*;
public class Main {
    public static void main(String[] args) {
        String s = new Scanner(System.in).next();
        int[] count = new int[26];
        for (char c : s.toCharArray()) count[c - 'a']++;
        StringJoiner out = new StringJoiner(" ");
        for (int i = 0; i < 26; i++) {
            if (count[i] > 0) out.add((char) ('a' + i) + "=" + count[i]);
        }
        System.out.println(out);
    }
}
