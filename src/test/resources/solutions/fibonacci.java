import java.util.*;
public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        long a = 0, b = 1;
        StringJoiner out = new StringJoiner(" ");
        for (int i = 0; i < n; i++) {
            out.add(String.valueOf(a));
            long next = a + b;
            a = b;
            b = next;
        }
        System.out.println(out);
    }
}
