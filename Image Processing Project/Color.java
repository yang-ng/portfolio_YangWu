/* Name: Yiling Hou & Yang Wu
* File: Color.java
* Desc:
*
* The Color class. Each Color has three values that indicates
* the quantity of red green and blue in this
*
*/
public class Color {
    private int red;
    private int green;
    private int blue;

    public Color(int red, int green, int blue) {
        this.red = red;
        this.green = green;
        this.blue = blue;
    }

    public void setRed(int red) {
        this.red = red;
    }

    public void setGreen(int green) {
        this.green = green;
    }

    public void setBlue(int blue) {
        this.blue = blue;
    }

    public int getRed() {
        return this.red;
    }

    public int getGreen() {
        return this.green;
    }

    public int getBlue() {
        return this.blue;
    }

    public String toString() {
        return red + " " + green + " " + blue + " ";
    }

    /**
     * Set the Color to white
     *
     */
    public void setWhite() {
        this.setRed(255);
        this.setBlue(255);
        this.setGreen(255);
    }

    /**
     * Set the Color to black
     *
     */
    public void blackenColor() {
        this.setRed(0);
        this.setBlue(0);
        this.setGreen(0);
    }

    public void setColor(int red, int green, int blue) {
        this.red = red;
        this.green = green;
        this.blue = blue;
    }

    /**
     * Set the Color with different scales of gray
     * according to how its original brightness
     *
     */
    public void shadowColor() {
        if (this.getRed() + this.getGreen() + this.getBlue() > 360) {
            this.setRed(75);
            this.setGreen(75);
            this.setBlue(75);
        } else {
            this.setRed(125);
            this.setGreen(125);
            this.setBlue(125);
        }
    }
}