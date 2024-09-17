/* Name: Yang Wu
* File: Pixel.java
* Desc:
*
* The Pixel class. Each Pixel has its coordinates and a color.
*
*/

public class Pixel {
    private int x;
    private int y;
    private Color color;

    public boolean isWhite = false;

    public Pixel(int x, int y, Color color) {
        this.x = x;
        this.y = y;
        this.color = color;
    }

    /**
     * Multiply the weight of a color and the value of its color
     * 
     * @param weight The weight it occupies in a 3x3 block
     * @return The product of the weight and the value of color
     *
     */
    public double getColorMultiplication(double weight) {
        return weight * (this.getColor().getRed() + this.getColor().getGreen() + this.getColor().getBlue());
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public Color getColor() {
        return this.color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public String toString() {
        String s = "";
      //  s += "x: " + x + "\ny: " + y;
        s += "color: " + color;
        return s;
    }

    /**
     * Set the pixel's color to black
     *
     */
    public void blackenPixel() {
        this.color.blackenColor();
    }
}