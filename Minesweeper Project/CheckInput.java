//Author: Yang Wu
//Date: 2022/10/20
//Description: check if the input is an integer

class CheckInput {
    
    public static boolean IsInteger(String input) {
        if (input.length() == 0) {
            return false;
        }
        
        int startIndex = 0;
        if (input.charAt(0) == '-') {
            startIndex = 1;
        }
        
        for (int i = startIndex; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }  
    
    public static void main(String[] args) {
        //test the function
        System.out.println(IsInteger("0"));
    }
}
