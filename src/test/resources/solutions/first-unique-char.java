import java.util.*;
public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String s = sc.next();
        int[] count = new int[26];
        for (char c : s.toCharArray()) count[c - 'a']++;
        for (char c : s.toCharArray()) {
            if (count[c - 'a'] == 1) {
                System.out.println(c);
                return;
            }
        }
        System.out.println(-1);
    }
}
