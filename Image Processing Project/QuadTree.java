/* Name: Yiling Hou & Yang Wu
* File: QuadTree.java
* Desc:
*
* The QuadTree class. Represents the image. After a Boundary is split,
* the four children of it are the four new smaller Boundaries.
*
*/

public class QuadTree {

    final int MAX_CAPACITY = 4;
    int level = 0;
    private Boundary root;

    public QuadTree(int xmin, int xmax, int ymin, int ymax) {
        root = new Boundary(xmin, xmax, ymin, ymax, 0);
    }

    public Boundary getRoot() {
        return this.root;
    }

    public void edgeDetect(Pixel[][] image, int gear) {
        edgeDetectRec(root, image, gear);
    }

    /**
     * Checks if the Boundaries are edges
     *
     * @param root  The root of the QuadTree
     * @param image The Pixel array of the image
     * @param gear  The threshold
     * 
     */
    private void edgeDetectRec(Boundary root, Pixel[][] image, int gear) {
        if (root == null) {
            return;
        }
        if (root.isLeaf()) {
            if (root.getBoundaryLevel() == this.level) {
                root.convolution(image, gear);
            } else if (root.getBoundaryLevel() < this.level) {
                root.blackenBoundary(image);
            }
        }
        edgeDetectRec(root.getNE(), image, gear);
        edgeDetectRec(root.getNW(), image, gear);
        edgeDetectRec(root.getSE(), image, gear);
        edgeDetectRec(root.getSW(), image, gear);
    }

    public void compressionSplit(double compLevel, Pixel[][] image) {
        int leaves = (int) (compLevel * root.getXmax() * root.getYmax());
        compressionSplitRec(root, leaves, image);
    }

    /**
     * Splits the image
     *
     * @param root   The root of the QuadTree
     * @param leaves number of target leaves
     * @param image  The Pixel array of the image
     * 
     */
    public void compressionSplitRec(Boundary root, int leaves, Pixel[][] image) {
        if (this.numLeaves() >= leaves || (root.getXmax() - root.getXmin() + 1) < 4
                || (root.getYmax() - root.getYmin() + 1) < 4) { //
            // it does not match the target leaves number exactly, and we keep it smaller
            // than the target, so that the compression level will not exceed 1
            return;
        }
        if (root.needSplit(image) > 300) {
            level++;
            root.split(level);
            compressionSplitRec(root.getNW(), leaves / 4, image);
            compressionSplitRec(root.getNE(), leaves / 4, image);
            compressionSplitRec(root.getSW(), leaves / 4, image);
            compressionSplitRec(root.getSE(), leaves / 4, image);
        } else {
            root.blackenBoundary(image);
            return;
        }
    }

    public int numInternal() {
        return numInternalRec(root, 0);
    }

    private int numInternalRec(Boundary root, int count) {
        if (root.getNW() != null && root.getNE() != null && root.getSW() != null && root.getSE() != null) {
            // If all child nodes exist, this is an internal node
            count = 1;
            // Recursively count internal nodes in child nodes
            count += numInternalRec(root.getNW(), count) + numInternalRec(root.getNE(), count)
                    + numInternalRec(root.getSW(), count) + numInternalRec(root.getSE(), count);
        }
        return count;
    }

    public int numLeaves() {
        return numLeavesRec(this.root, 0);
    }

    private int numLeavesRec(Boundary current, int count) {
        if (current == null) {
            return 0;
        }
        if (current.isLeaf()) {
            count = 1;
        }
        count += numLeavesRec(current.getNW(), count) + numLeavesRec(current.getNE(), count)
                + numLeavesRec(current.getSW(), count) + numLeavesRec(current.getSE(), count);
        return count;
    }

    public static Color newPPM(Pixel[][] image, Boundary current) {
        int size = (current.getXmax() - current.getXmin() + 1) * (current.getYmax() - current.getYmin() + 1);
        int reds = 0;
        int greens = 0;
        int blues = 0;
        for (int i = current.getXmin(); i <= current.getXmax(); i++) {
            for (int j = current.getYmin(); j <= current.getYmax(); j++) {
                reds += image[i][j].getColor().getRed();
                greens += image[i][j].getColor().getGreen();
                blues += image[i][j].getColor().getBlue();
            }
        }
        Color color = new Color(reds / size, greens / size, blues / size);
        return color;
    }

    public Pixel[][] compressMerge(Pixel[][] image) {
        Pixel[][] empty = new Pixel[image.length][image[0].length];
        return compressMergeRec(root, image, empty);
    }

    private Pixel[][] compressMergeRec(Boundary current, Pixel[][] image, Pixel[][] empty) {
        if (current == null) {
            return empty;
        }
        if (current.isLeaf()) {
            if ((current.getXmax()-current.getXmin()+1) != 0 && (current.getYmax()-current.getYmin()+1) != 0){
            Color newest = newPPM(image, current);
            for (int i = current.getXmin(); i <= current.getXmax(); i++) {
                for (int j = current.getYmin(); j <= current.getYmax(); j++) {
                    empty[i][j] = new Pixel(i, j, newest);
                }
            }
        }
        }
        empty = compressMergeRec(current.getNW(), image, empty);
        empty = compressMergeRec(current.getNE(), image, empty);
        empty = compressMergeRec(current.getSW(), image, empty);
        empty = compressMergeRec(current.getSE(), image, empty);
        return empty;
    }

    public String toStringPreOrder() {
        return toStringPreOrderRec(this.getRoot(), "");
    }

    private String toStringPreOrderRec(Boundary current, String s) {
        if (current != null) {
            s += current.toString() + "\n";
            s = toStringPreOrderRec(current.getNW(), s);
            s = toStringPreOrderRec(current.getNE(), s);
            s = toStringPreOrderRec(current.getSW(), s);
            s = toStringPreOrderRec(current.getSE(), s);
        }
        return s;
    }
}