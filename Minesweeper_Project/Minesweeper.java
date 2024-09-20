//Author: Yang Wu
//Date: 2022/10/27
//Description: playable

class Minesweeper {
    //print out welcome user
    public static void welcome(){
        System.out.println("- - - - - - - - - - - - - -");
        System.out.println("!         WELCOME         !");
        System.out.println("- - - - - - - - - - - - - -");
    }
    
    //print out user win
    public static void win(){
        System.out.println("- - - - - - - - - - - - - -");
        System.out.println("!         YOU WIN         !");
        System.out.println("- - - - - - - - - - - - - -");
    }
    
    //print out user lose
    public static void lose(){
        System.out.println("- - - - - - - - - - - - - -");
        System.out.println("!      BOMB YOU LOSE      !");
        System.out.println("- - - - - - - - - - - - - -");
    }
    
    //print out game over
    public static void over(){
        System.out.println("- - - - - - - - - - - - - -");
        System.out.println("!        GAME OVER        !");
        System.out.println("- - - - - - - - - - - - - -");
    }
    
    //count the number of bombs
    public static int countBomb(int[][]hidden){
        int bombs = 0;
        for (int i = 0; i < hidden.length; i++){
            for(int j = 0; j < hidden[i].length; j++){
                if (hidden[i][j] == -1){
                    bombs += 1;
                }
            }
        }
        return bombs;
    }
    
    
    //ask for a row
    public static int askForRowInteger(){
        System.out.print("Enter a row: ");
        String answer = System.console().readLine();
        while (!CheckInput.IsInteger(answer)){
            System.out.println("Please enter an integer!");
            System.out.print("Enter a row: ");
            answer = System.console().readLine();
        }
        int answerInt = Integer.parseInt(answer);
        return answerInt;
    }
    
    public static int askForRowRange(int rows){
        int answerInt = askForRowInteger();
        while (answerInt < 1||answerInt > rows){
            System.out.printf("Please enter an integer in range [0, %d)", rows);
            System.out.println("");
            answerInt = askForRowInteger();
        }  
        return answerInt;
    }
    
    //ask for a col
    public static int askForColInteger(){
        System.out.print("Enter a col: ");
        String answer = System.console().readLine();
        while (!CheckInput.IsInteger(answer)){
            System.out.println("Please enter an integer!");
            System.out.print("Enter a col: ");
            answer = System.console().readLine();
        }
        int answerInt = Integer.parseInt(answer);
        return answerInt;
    }
    
    public static int askForColRange(int cols){
        int answerInt = askForColInteger();
        while (answerInt < 1||answerInt > cols){
            System.out.printf("Please enter an integer in range [0, %d)", cols);
            System.out.println("");
            answerInt = askForColInteger();
        }  
        return answerInt;
    }
    
    //is the cell a bomb?
    public static boolean checkBoard(int rowUser, int colUser, int[][]hidden){
        if (hidden[rowUser - 1][colUser - 1] == -1){
            return false;
        }
        else {
            return true;
        }
    } 
    
    //print out the board keep track
    public static String[][] trackBoard(int rows, int cols){
        String[][]track = new String[rows][cols];
        for (int i = 0; i < rows; i++){
            for (int j = 0; j < cols; j++){
                track[i][j] = "· ";
            }
        }
        return track;
    }
    
    public static void main(String[] args){
        //generate the board
        int rows = Integer.parseInt(args[0]);
        int cols = Integer.parseInt(args[1]);
        double prob = Double.parseDouble(args[2]);
        int[][] hidden = (Board.generate(rows, cols, prob));
        int bombs = countBomb(hidden);
        
        //the game starts
        welcome();
        String[][]track = trackBoard(rows, cols);
        for (int i = 0; i < rows; i++){
            for (int j = 0; j < cols; j++){
                System.out.print("· ");
            }
            System.out.println("");
        }

        //start to keep track
        int count = 0;
        int cells = rows * cols - bombs;
        while(count != cells){
            int rowUser = askForRowRange(rows);
            int colUser = askForColRange(cols);
            boolean bombOrNot = checkBoard(rowUser, colUser, hidden);
            if (bombOrNot == true){
                if (track[rowUser-1][colUser-1] == "· "){
                    count += 1;
                }
                track[rowUser - 1][colUser - 1] = String.valueOf(hidden[rowUser - 1][colUser - 1]) + " ";
                for (int i = 0; i < rows; i++){
                    for (int j = 0; j < cols; j++){
                        System.out.print(track[i][j]);
                    }
                    System.out.println("");
                }
                System.out.println("");
                if (count == cells){
                    win();        
                    over();
                    Board.display(hidden);
                }
            }
            else {
                count = cells;
                lose();
                over();
                Board.display(hidden);
            }
        }
    }
}
