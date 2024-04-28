package gitlet;

import java.util.Iterator;
import java.util.NoSuchElementException;

/** This tree stores all commit hash code */
//public class CommitTree implements Iterable<Commit> {
//    private Commit commit0 = null;
//    private Commit head;
//    public CommitTree(Commit commit0) {
//        this.commit0 = commit0;
//        head = commit0;
//    }
//
////    public void addCommit(Commit nextCommit, Commit parentCommit) {
////        if (commit0 != null) {
////            commit0.addNextCommit(nextCommit, parentCommit);
////        } else {
////            System.out.println("The gitlet does not exit.");
////        }
////    }
//
//    @Override
//    public Iterator<Commit> iterator() {
//        return new commitIterator();
//    }

//    private class commitIterator implements Iterator<Commit> {
//        public Commit cur = head;
//
//        public boolean hasNext() {
//            return cur.parent != null;
//        }
//
//        public Commit next() {
//            if (!hasNext()) {
//                throw new NoSuchElementException("Out of range.");
//            }
//            Commit returnCommit = cur;
//            cur = cur.parent;
//            return returnCommit;
//        }
//    }
//
//    public static void main(String[] args) {
//        Commit c0 = new Commit();
//        CommitTree newTree = new CommitTree(c0);
//    }
//}
