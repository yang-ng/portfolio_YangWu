/* Name: Yang Wu
* File: Boundary.java
* Desc:
*
* The Boundary class. A Boundary is a node of the quadtree.
*
*/

public class Boundary {

    private int xmin;
    private int xmax;
    private int ymin;
    private int ymax;

    private Boundary nw = null;
    private Boundary ne = null;
    private Boundary sw = null;
    private Boundary se = null;

    private int boundaryLevel = 0;

    public void boudnaryLevelIncrement() {
        boundaryLevel++;
    }

    public Boundary(int xmin, int xmax, int ymin, int ymax, int boundaryLevel) {
        this.xmin = xmin;
        this.xmax = xmax;
        this.ymin = ymin;
        this.ymax = ymax;
        this.boundaryLevel = boundaryLevel;
    }

    public int getBoundaryLevel() {
        return this.boundaryLevel;
    }

    public int getXmin() {
        return xmin;
    }

    public int getYmin() {
        return ymin;
    }

    public int getXmax() {
        return xmax;
    }

    public int getYmax() {
        return ymax;
    }

    public Boundary getNW() {
        return this.nw;
    }

    public Boundary getNE() {
        return this.ne;
    }

    public Boundary getSW() {
        return this.sw;
    }

    public Boundary getSE() {
        return this.se;
    }

    /**
     * Transforms a specific part of the Boundary to an Pixel array
     *
     * @param image The Pixel array of the image
     * @return The Pixel array of the Boundary
     */
    public Pixel[][] getQuadrant(Pixel[][] image) {
        Pixel[][] quadrant = new Pixel[this.getXmax() - this.getXmin() + 1][this.getYmax() - this.getYmin() + 1];
        for (int i = this.getXmin(); i <= this.getXmax(); i++) {
            for (int j = this.getYmin(); j <= this.getYmax(); j++) {
                quadrant[i - this.getXmin()][j - this.getYmin()] = image[i][j];
            }
        }
        return quadrant;
    }

    public boolean inRange(int x, int y) {
        return (x >= this.getXmin() && x <= this.getXmax()
                && y >= this.getYmin() && y <= this.getYmax());
    }

    public void setLeaves(Boundary nw, Boundary ne, Boundary sw, Boundary se) {
        this.nw = nw;
        this.ne = ne;
        this.sw = sw;
        this.se = se;
    }

    /**
     * Splits a Boundary into four smaller Boundaries
     *
     * @param level The level of the QuadTree
     */
    public void split(int level) {
        Boundary nw = new Boundary(this.getXmin(), this.getXmax() / 2, this.getYmin(), this.getYmax() / 2, level);
        Boundary ne = new Boundary(this.getXmax() / 2 + 1, this.getXmax(), this.getYmin(), this.getYmax() / 2, level);
        Boundary sw = new Boundary(this.getXmin(), this.getXmax() / 2, this.getYmax() / 2 + 1, this.getYmax(), level);
        Boundary se = new Boundary(this.getXmax() / 2 + 1, this.getXmax(), this.getYmax() / 2 + 1, this.getYmax(),
                level);
        this.setLeaves(nw, ne, sw, se);
    }

    /**
     * Checks if a Boundary needs splitting
     * based on its average Color value
     *
     * @param image The Pixel array of the image
     * @return The double that represents the average Color value
     */
    public double needSplit(Pixel[][] image) {
        int width = this.getXmax() - this.getXmin() + 1;
        int height = this.getYmax() - this.getYmin() + 1;
        int total = width * height;
        int rtotal = 0;
        int gtotal = 0;
        int btotal = 0;
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                rtotal += image[this.getXmin() + i][this.getYmin() + j].getColor().getRed();
                gtotal += image[this.getXmin() + i][this.getYmin() + j].getColor().getGreen();
                btotal += image[this.getXmin() + i][this.getYmin() + j].getColor().getBlue();
            }
        }
        int rmean = rtotal / total;
        int gmean = gtotal / total;
        int bmean = btotal / total;
        double etotal = 0;
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                double ei = Math.pow(image[this.getXmin() + i][this.getYmin() + j].getColor().getRed() - rmean, 2)
                        + Math.pow(image[this.getXmin() + i][this.getYmin() + j].getColor().getGreen() - gmean, 2)
                        + Math.pow(image[this.getXmin() + i][this.getYmin() + j].getColor().getBlue() - bmean, 2);
                etotal += ei;
            }
        }
        double eMean = etotal / total;
        return eMean;
    }

    public boolean isLeaf() {
        if (nw == null && ne == null && sw == null && se == null) {
            return true;
        }
        return false;
    }

    /**
     * Set the Boundary to black
     *
     * @param image The Pixel array of the image
     * 
     */
    public void blackenBoundary(Pixel[][] image) {
        for (int i = xmin; i <= xmax; i++) {
            for (int j = ymin; j <= ymin; j++) {
                image[i][j].blackenPixel();
            }
        }

    }

    /**
     * Determine the threshold of edge detection
     * based on the average Color value of an image
     *
     * @param image The Pixel array of the image
     * @return The integer that indicates three gears in edge detection
     * 
     */
    public int determineThreshold(Pixel[][] image) {
        double gear = this.needSplit(image);
        if (gear < 1000) {
            return 1;
        } else if (gear >= 1000 && gear < 10000) {
            return 2;
        }
        return 3;
    }

    /**
     * Checks if a Pixel is an edge by comparing it
     * with the Pixels around it
     *
     * @param image The Pixel array of the image
     * @param gear  The threshold
     * 
     */
    public void convolution(Pixel[][] image, int gear) {
        int edgeThreshold = 0;
        if (gear == 1) {
            edgeThreshold = 40;
        } else if (gear == 2) {
            edgeThreshold = 160;
        } else {
            edgeThreshold = 200;
        }
        double[][] weights = { { -1, -1, -1 }, { -1, 8, -1 }, { -1, -1, -1 } };
        Pixel[][] current = this.getQuadrant(image);
        for (int i = 0; i < current.length - 1; i++) {
            for (int j = 0; j < current[0].length - 1; j++) {
                double output = 0;
                if (i == 0 && j != 0 && j != current[0].length - 1) {
                    output += current[i][j].getColorMultiplication(5)
                            + current[i][j - 1].getColorMultiplication(-1)
                            + current[i + 1][j - 1].getColorMultiplication(-1)
                            + current[i + 1][j].getColorMultiplication(-1)
                            + current[i + 1][j + 1].getColorMultiplication(-1)
                            + current[i][j + 1].getColorMultiplication(-1);
                } else if (i == 0 && j == 0) {
                    output += current[i][j].getColorMultiplication(3)
                            + current[i + 1][j].getColorMultiplication(-1)
                            + current[i][j + 1].getColorMultiplication(-1)
                            + current[i + 1][j + 1].getColorMultiplication(-1);
                } else if (i == 0 && j == current[0].length - 1) {
                    output += current[i][j].getColorMultiplication(3)
                            + current[i + 1][j].getColorMultiplication(-1)
                            + current[i][j - 1].getColorMultiplication(-1)
                            + current[i + 1][j - 1].getColorMultiplication(-1);
                } else if (j == 0 && i != 0 && i != current.length - 1) {
                    output += current[i][j].getColorMultiplication(5)
                            + current[i - 1][j].getColorMultiplication(-1)
                            + current[i - 1][j + 1].getColorMultiplication(-1)
                            + current[i][j + 1].getColorMultiplication(-1)
                            + current[i + 1][j + 1].getColorMultiplication(-1)
                            + current[i + 1][j].getColorMultiplication(-1);
                } else if (j == 0 && i == current.length - 1) {
                    output += current[i][j].getColorMultiplication(3)
                            + current[i][j + 1].getColorMultiplication(-1)
                            + current[i - 1][j].getColorMultiplication(-1)
                            + current[i - 1][j + 1].getColorMultiplication(-1);
                } else if (j == current[0].length - 1 && i != 0 && i != current.length - 1) {
                    output += current[i][j].getColorMultiplication(5)
                            + current[i - 1][j].getColorMultiplication(-1)
                            + current[i - 1][j - 1].getColorMultiplication(-1)
                            + current[i][j - 1].getColorMultiplication(-1)
                            + current[i + 1][j - 1].getColorMultiplication(-1)
                            + current[i + 1][j].getColorMultiplication(-1);
                } else if (j == current[0].length - 1 && i == current.length - 1) {
                    output += current[i][j].getColorMultiplication(3)
                            + current[i - 1][j].getColorMultiplication(-1)
                            + current[i][j - 1].getColorMultiplication(-1)
                            + current[i - 1][j - 1].getColorMultiplication(-1);
                } else if (i == current.length && j != 0 && j != current.length - 1) {
                    output += current[i][j].getColorMultiplication(5)
                            + current[i][j - 1].getColorMultiplication(-1)
                            + current[i - 1][j - 1].getColorMultiplication(-1)
                            + current[i - 1][j].getColorMultiplication(-1)
                            + current[i - 1][j + 1].getColorMultiplication(-1)
                            + current[i][j + 1].getColorMultiplication(-1);
                } else {
                    for (int row = 0; row < 3; row++) {
                        for (int column = 0; column < 3; column++) {
                            output += current[i - 1 + row][j - 1 + column].getColorMultiplication(weights[row][column]);
                        }
                    }
                }
                if (output >= edgeThreshold) {
                    image[this.getXmin() + i][this.getYmin() + j].isWhite = true;
                }
            }
        }
    }

    public String toString() {
        return "(" + this.getXmin() + "," + this.getXmax() + "," + this.getYmin() + "," + this.getYmax() + ") level: "
                + this.getBoundaryLevel();
    }
}