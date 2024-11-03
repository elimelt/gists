package datastructures;
import java.util.*;

public class SegmentTree {
    private int[] tree;
    private int n;

    public SegmentTree(int n) {
        this.n = n;
        tree = new int[4 * n]; // 4n is enough to store the segment tree
    }

    public void build(int[] a) {
        build(a, 1, 0, n - 1);
    }

    private void build(int[] a, int v, int tl, int tr) {
        if (tl == tr) {
            tree[v] = a[tl];
        } else {
            int tm = (tl + tr) / 2;
            build(a, v * 2, tl, tm);
            build(a, v * 2 + 1, tm + 1, tr);
            tree[v] = tree[v * 2] + tree[v * 2 + 1];
        }
    }

    public int sum(int l, int r) {
        return sum(1, 0, n - 1, l, r);
    }

    private int sum(int v, int tl, int tr, int l, int r) {
        if (l > r) {
            return 0;
        }
        if (l == tl && r == tr) {
            return tree[v];
        }
        int tm = (tl + tr) / 2;
        return sum(v * 2, tl, tm, l, Math.min(r, tm)) +
                sum(v * 2 + 1, tm + 1, tr, Math.max(l, tm + 1), r);
    }

    public void update(int pos, int new_val) {
        update(1, 0, n - 1, pos, new_val);
    }

    private void update(int v, int tl, int tr, int pos, int new_val) {
        if (tl == tr) {
            tree[v] = new_val;
        } else {
            int tm = (tl + tr) / 2;
            if (pos <= tm) {
                update(v * 2, tl, tm, pos, new_val);
            } else {
                update(v * 2 + 1, tm + 1, tr, pos, new_val);
            }
            tree[v] = tree[v * 2] + tree[v * 2 + 1];
        }
    }

    public static void main(String[] args) {
        int[] a = { 1, 3, 5, 7, 9, 11 };
        SegmentTree st = new SegmentTree(a.length);
        st.build(a);
        System.out.println(st.sum(1, 3)); // Should print 15 (3 + 5 + 7)
        st.update(1, 10);
        System.out.println(st.sum(1, 3)); // Should print 22 (10 + 5 + 7)
    }
}
