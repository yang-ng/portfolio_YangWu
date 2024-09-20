/* Name: Yang Wu
* File: Main.java
* Desc:
*
* The driver code of the project. It detects the flags
* in the command line and run the corresponding command
*
*/

import java.io.*;
import java.util.Scanner;

public class Main {

    /**
     * write an image to ppm file
     *
     * @param filename The filename
     * @param image    The Pixel array of the image
     * 
     */
    public static void writeImg(String filename, Pixel[][] img) throws IOException {
        PrintWriter out = new PrintWriter(filename);
        out.print("P3 ");
        out.println(img[0].length + " " + img.length + " 255");
        for (int i = 0; i < img.length; i++) {
            for (int j = 0; j < img[0].length; j++) {
                out.print(img[i][j].getColor().getRed() + " " + img[i][j].getColor().getGreen() + " "
                        + img[i][j].getColor().getBlue() + " ");
            }
            out.println();
        }
        out.close();
    }

    /**
     * Parses the ppm file to a Pixel array
     *
     * @param filename The filename
     * @return The Pixel array of the image
     * 
     */
    public static Pixel[][] parseFile(String filename) throws FileNotFoundException {
        Scanner input = new Scanner(new File(filename));
        int numRows = 0;
        int numColumns = 0;
        input.nextLine();
        String test = input.nextLine();
        if (!test.contains("#")) {
            String[] rowCol = test.split(" ");
            numRows = Integer.parseInt(rowCol[0]);
            numColumns = Integer.parseInt(rowCol[1]);
        } else {
            numRows = input.nextInt();
            numColumns = input.nextInt();
            input.nextLine();
        }
        Pixel[][] image = new Pixel[numRows][numColumns];
        input.nextLine();
        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numColumns; j++) {
                int rIndex = input.nextInt();
                int gIndex = input.nextInt();
                int bIndex = input.nextInt();
                Color current = new Color(rIndex, gIndex, bIndex);
                Pixel newest = new Pixel(i, j, current);
                image[i][j] = newest;
            }
        }
        return image;
    }

    /**
     * Sets the Colors to black or white
     *
     * @param image The Pixel array of the image
     * @return The Pixel array of the image after edge detection
     * 
     */
    public static Pixel[][] edgeDetection(Pixel[][] image) {
        for (int i = 0; i < image.length; i++) {
            for (int j = 0; j < image[0].length; j++) {
                if (image[i][j].isWhite == true) {
                    image[i][j].getColor().setWhite();
                } else {
                    image[i][j].getColor().blackenColor();
                    ;
                }
            }
        }
        return image;
    }

    /**
     * prints a ppm file
     *
     * @param image The Pixel array of the image
     * 
     */
    public static String toStringPPM(Pixel[][] image) {
        String s = "";
        s += "P3";
        int x = image.length;
        int y = image[0].length;
        s += "\n" + x + " " + y;
        s += "\n255";
        s += "\n";
        for (int i = 0; i < image.length; i++) {
            for (int j = 0; j < image[0].length; j++) {
                s += image[i][j].getColor().toString();
            }
            s += "\n";
        }
        return s;
    }

    /**
     * A filter that uses gray to display an image
     *
     * @param image The Pixel array of the image
     * 
     */
    public static String shadowFilter(Pixel[][] image) {
        String s = "";
        s += "P3";
        int x = image.length;
        int y = image[0].length;
        s += "\n" + x + " " + y;
        s += "\n255";
        s += "\n";
        for (int i = 0; i < image.length; i++) {
            for (int j = 0; j < image[0].length; j++) {
                image[i][j].getColor().shadowColor();
                s += image[i][j].getColor().toString();
            }
            s += "\n";
        }
        return s;
    }

    public static void main(String args[]) throws IOException {
        String filename = "";
        for (int i = 0; i < args.length; i++) {
            if (args[i].contains("-i")) {
                filename += args[i + 1];
                break;
            }
        }
        Pixel[][] image = parseFile(filename);
        QuadTree qtree = new QuadTree(0, image[0].length - 1, 0, image.length - 1);
        for (int i = 0; i < args.length; i++) {
            if (args[i].contains("-")) {
                if (args[i].contains("o")) {
                    System.out.println("root name: " + filename);
                }
                if (args[i].contains("c")) {
                    double[] compLevels = { 0.002, 0.004, 0.01, 0.033, 0.077, 0.2, 0.5, 0.75};
                    for (int j = 0; j < 8; j++) {
                        qtree.compressionSplit(compLevels[j], image);
                        QuadTree.newPPM(image, qtree.getRoot());
                        image = qtree.compressMerge(image);
                        String ppmName = "output-" + compLevels[j] + ".ppm";
                        FileOutputStream outputStream = new FileOutputStream(ppmName);
                        byte[] bytes = toStringPPM(image).getBytes();
                        outputStream.write(bytes);
                        outputStream.close();
                    }
                } else if (args[i].contains("e")) {
                    int gear = qtree.getRoot().determineThreshold(image);
                    qtree.edgeDetect(image, gear);
                    edgeDetection(image);
                    FileOutputStream outputStream = new FileOutputStream("output-edgeDetection.ppm");
                    OutputStreamWriter ows = new OutputStreamWriter(outputStream);
                    ows.write(toStringPPM(image));
                    ows.close();
                } else if (args[i].contains("x")) {
                    FileOutputStream outputStream = new FileOutputStream("output-filter.ppm");
                    byte[] bytes = shadowFilter(image).getBytes();
                    outputStream.write(bytes);
                    outputStream.close();
                }
                if (args[i].contains("t")) {
                    System.out.println(qtree.toStringPreOrder());
                }
            }
        }
    }
}