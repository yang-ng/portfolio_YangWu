//Author: Yang Wu
//Date: 2022/10/27
//Description: generate a board and display

class Board {
    public static int[][] generate(int rows, int cols, double prob){
        int[][] hidden = new int [rows][cols];
        //set random bombs
        for (int i = 0; i < rows; i++){
            for(int j = 0; j < cols; j++){
                double randomP = Math.random();
                if (randomP < prob){
                    hidden[i][j] = -1;
                }
            }
        }
        //fill other cells
        for (int i = 0; i < rows; i++){
            for (int j = 0; j < cols; j++){
                int numberOfBombs = 0;
                if (hidden[i][j] != -1){
                    if (i != 0 && j != 0){
                        if (hidden[i-1][j-1] == -1){
                            numberOfBombs += 1;
                        }
                    }
                    if (i != 0){
                        if (hidden[i-1][j] == -1){
                            numberOfBombs += 1;
                        }
                    }
                    if (i != 0 && j != (hidden[i].length - 1)){
                        if (hidden[i-1][j+1] == -1){
                            numberOfBombs += 1;
                        }
                    }
                    if (j != 0){
                        if (hidden[i][j-1] == -1){
                            numberOfBombs += 1;
                        }
                    }
                    if (j != (hidden[i].length - 1)){
                        if (hidden[i][j+1] == -1){
                            numberOfBombs += 1;
                        }
                    }
                    if (i != (hidden.length - 1) && j != 0){
                        if (hidden[i+1][j-1] == -1){
                            numberOfBombs += 1;
                        }
                    }
                    if (i != (hidden.length - 1)){
                        if (hidden[i+1][j] == -1){
                            numberOfBombs += 1;
                        }
                    }
                    if (i != (hidden.length - 1) && j != (hidden[i].length - 1)){
                        if (hidden[i+1][j+1] == -1){
                            numberOfBombs += 1;
                        }
                    }
                    hidden[i][j] = numberOfBombs;
                }
            }
        }
        return hidden; 
    }
    
    //display a 2D array
    public static void display(int[][] hidden){
        for (int i = 0; i < hidden.length; i++){
            for (int j = 0; j < hidden[i].length; j++){
                if (hidden[i][j] == -1){
                    System.out.print("X ");
                }
                else{
                    System.out.print(hidden[i][j] + " ");
                }
            }
            System.out.println("");
        }
    }
    
    //call and test
    public static void main(String[] args){
        int rows = Integer.parseInt(args[0]);
        int cols = Integer.parseInt(args[1]);
        double prob = Double.parseDouble(args[2]);
        int[][] hidden = generate(rows, cols, prob);        
        display(hidden);
    }
}
