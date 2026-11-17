import java.util.*;
public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        int[] a = new int[n];
        int k = 0;
        for (int i = 0; i < n; i++) {
            int x = sc.nextInt();
            if (x != 0) a[k++] = x;
        }
        StringJoiner out = new StringJoiner(" ");
        for (int x : a) out.add(String.valueOf(x));
        System.out.println(out);
    }
}
