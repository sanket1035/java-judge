import java.util.*;
public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String s = sc.next().toLowerCase();
        int i = 0, j = s.length() - 1;
        boolean ok = true;
        while (i < j) {
            if (s.charAt(i++) != s.charAt(j--)) { ok = false; break; }
        }
        System.out.println(ok ? "YES" : "NO");
    }
}
