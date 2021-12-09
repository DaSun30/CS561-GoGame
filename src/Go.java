import java.io.*;
import java.util.*;


public class Go {

    public class Chess {
        private int color;
        private int row;
        private int col;
        private boolean visited;

        public Chess() {
            this.color = 0;
            this.row = 0;
            this.col = 0;
            visited = false;
        }

        public Chess(int color, int row, int col) {
            this.color = color;
            this.row = row;
            this.col = col;
            visited = false;
        }
    }

    private Chess[][] preBoard;
    private Chess[][] curBoard;
    private int myColor;
//    private int steps;
    private String outputString;
    private static final int BOARD_SIZE = 5;
    private static final int MAX_DEPTH = 3;   // alpha-beta max depth
    private static final double KOMI = 2.5;
    private static final int MAX_STEPS = 24;
    private static final int SCORE_MULTIPLE = 16;
    private static final int LIBERTY_MULTIPLE = 12;
    private static final int LIBERTY_MULTIPLE_HALF = 6;
    private static final int AWARD_MULTIPLE = 53;  // award multiple used in minmax
    private static final int PUNISH_MULTIPLE = -100;  // punish multiple used in minmax

    public Go() {
        preBoard = new Chess[BOARD_SIZE][BOARD_SIZE];
        curBoard = new Chess[BOARD_SIZE][BOARD_SIZE];
        for(int i = 0; i < BOARD_SIZE; i++) {
            for(int j = 0; j < BOARD_SIZE; j++) {
                preBoard[i][j] = new Chess(0, i, j);
                curBoard[i][j] = new Chess(0, i, j);
            }
        }
        myColor = 0;
//        steps = 0;
    }

    public void buildBoard(){
        String path = "input.txt";
        int count = 1, preRow = 0, nextRow = 0;
        try {
            File f = new File(path);
            if(f.isFile() && f.exists()) {
                InputStreamReader isr = new InputStreamReader(new FileInputStream(f), "UTF-8");
                BufferedReader br = new BufferedReader(isr);
                String lineText = "";
                while((lineText = br.readLine()) != null) {
                    if(count == 1) {
                        myColor = Integer.parseInt(lineText);
                    } else if(count >= 2 && count <= 6){
                        int j = 0;
                        char[] cArr = lineText.toCharArray();
                        for(char c : cArr) {
                            preBoard[preRow][j].color = Integer.parseInt(String.valueOf(c));
                            j++;
                        }
                        preRow++;
                    } else {
                        int j = 0;
                        char[] cArr = lineText.toCharArray();
                        for(char c : cArr) {
                            curBoard[nextRow][j].color = Integer.parseInt(String.valueOf(c));
                            j++;
                        }
                        nextRow++;
                    }
                    count++;
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void outputBoard() {
        try {
            FileWriter w = new FileWriter("output.txt");
            if(outputString.equals("")) w.write("PASS");
            else w.write(outputString);
            w.close();
        } catch (IOException e) {
            System.out.println("write result to output.txt error");
            e.printStackTrace();
        }
    }

    /*
    * referenced by hosy.py detect_neighbor
    * */
    public ArrayList<Chess> findNeighbor(Chess[][] board, Chess chess) {
        int i = chess.row;
        int j = chess.col;
        ArrayList<Chess> res = new ArrayList<>();

        // find neighbor
        if (i > 0) res.add(board[i-1][j]);
        if (i < BOARD_SIZE - 1) res.add(board[i+1][j]);
        if (j > 0) res.add(board[i][j-1]);
        if (j < BOARD_SIZE - 1) res.add(board[i][j+1]);

        return res;
    }

    /*
     * referenced by hosy.py detect_neighbor_ally
     * */
    public ArrayList<Chess> findNeighborAlly(Chess[][] board, Chess chess) {
        int i = chess.row;
        int j = chess.col;
        int color = chess.color;
        ArrayList<Chess> res = new ArrayList<>();
        ArrayList<Chess> neighbors = findNeighbor(board, chess);

        // find neighbor ally
        for(Chess c : neighbors) {
            if(c.color == color)  res.add(c);
        }

        return res;
    }

    /*
     * referenced by hosy.py dfs
     * */
    public ArrayList<Chess> dfs(Chess[][] board, Chess chess) {
        Stack<Chess> s = new Stack<>();
        ArrayList<Chess> res = new ArrayList<>();
        s.push(chess);
        chess.visited = true;

        while (!s.isEmpty()) {
            Chess cur = s.pop();
            res.add(cur);
            cur.visited = true;
            ArrayList<Chess> neighborAlly = findNeighborAlly(board, cur);
            for(Chess c : neighborAlly) {
                if(!s.contains(c) && !res.contains(c) && !c.visited) {
                    s.push(c);
                }
            }
        }

        clearChessVisited(board);
        return res;
    }

    private void clearChessVisited(Chess[][] board) {
        for(int i = 0; i < BOARD_SIZE; i++) {
            for(int j = 0; j < BOARD_SIZE; j++) {
                board[i][j].visited = false;
            }
        }
    }

    /*
     * referenced by hosy.py find_liberty
     * */
    public boolean getLiberty(Chess[][] board, int row, int col) {
        Chess chess = board[row][col];
        // get all chess based on row & col in groupAlly
        ArrayList<Chess> groupAlly = dfs(board, chess);
        for(Chess c : groupAlly) {
            // use findNeighbor to validate liberty
            ArrayList<Chess> neighbor = findNeighbor(board, c);
            for(Chess cc: neighbor) {
                if(cc.color == 0)  return true;
            }
        }
        return false;
    }

    /*
     *    get liberty value based on the chess color
     * */
    public int getLibertyVal(Chess[][] board, int color) {
        int counter = 0;
        ArrayList<Chess> colorChess = getChessByColor(board, color);
        // get all chess based on row & col in groupAlly
        for(Chess c: colorChess) {
            // use findNeighbor to validate liberty
            ArrayList<Chess> neighbor = findNeighbor(board, c);
            for(Chess cc: neighbor) {
                if(cc.color == 0)  counter++;
            }
        }
        return counter;
    }


    /*
     * function calling getMinMax algorithm
     * */
    public void alphaBetaSearch() {
        double alpha = -Double.MAX_VALUE, beta = Double.MAX_VALUE;
        ArrayList<Chess> traceBack = new ArrayList<>();

        getMinMax(curBoard, 1, alpha, beta, true, traceBack);
        String res = "";
        if(traceBack.size() != 0) {
            Chess cur = traceBack.get(0);
            res = cur.row + "," + cur.col;
        }

//        System.out.println(res);
        // record res to class variable
        outputString = res;
    }

    /*
    * function realizing alpha-beta algorithm based on minmax algorithm
    * */
    public double getMinMax(Chess[][] board, int depth, double alpha, double beta, boolean maxPlayer, ArrayList<Chess> traceBack){
//        System.out.println("alpha:" + alpha + "beta:" + beta);
        // 1. dfs to max depth and judge end, calculate initial utility value.
        if(depth == MAX_DEPTH || judgeEnd(board)) {
            double utilVal = getUtility(board);
            return utilVal;
        }
        // 2. not max depth, do alpha-beta pruning
        double bestVal = maxPlayer == true ? -Double.MAX_VALUE : Double.MAX_VALUE;
        ArrayList<Chess> emptyChess = getChessByColor(board, 0);
//        System.out.println(depth + "," + emptyChess.size());
//        Collections.shuffle(emptyChess);
        if(maxPlayer) {
            // MAX level
            for(Chess c: emptyChess) {
//                System.out.println(c.color + ' ' + c.row + ' ' + c.col);
                // first check valid. If invalid, just jump current chess place
                boolean checkValidRes = checkValid(board, myColor, c.row, c.col);
                if(!checkValidRes)  continue;

                // liberty old
                double myTotalLib = (double) getLibertyVal(board, myColor);
                double oppoTotalLib = (double) getLibertyVal(board, 3-myColor);

                // put my chess and remove opponent's chess
                Chess[][] nextBoard = deepCopy(board);
                nextBoard[c.row][c.col].color = myColor;
                // count dead chess
                double removeNum = deadChessNum(nextBoard, 3 - myColor);
//                System.out.println(c.row + " | " +  c.col + " MAX removeNum:" + removeNum);

                // liberty new
                double myTotalLibNew = (double) getLibertyVal(nextBoard, myColor);
                double oppoTotalLibNew = (double) getLibertyVal(nextBoard, 3-myColor);
                double libNum = ((myTotalLibNew - myTotalLib) + (oppoTotalLib - oppoTotalLibNew)) * LIBERTY_MULTIPLE_HALF;

                // punish aside chess
                double punishNum = 0;
                if(c.row == BOARD_SIZE - 1 || c.row == 0 || c.col == BOARD_SIZE - 1 || c.col == 0) {
                    if(removeNum == 0) punishNum = PUNISH_MULTIPLE;
                }
                double totalNum = punishNum + AWARD_MULTIPLE*removeNum + libNum;
//                System.out.println(c.row + " | " +  c.col + "MAX totalNum:" + totalNum);

//                visualizeBoard(nextBoard);
                nextBoard = removeDeadChess(nextBoard, 3 - myColor);

                boolean checkKO = checkKO(preBoard, nextBoard);
                if(checkKO)  continue;
//                visualizeBoard(nextBoard);
                bestVal = Math.max(bestVal, totalNum + getMinMax(nextBoard, depth + 1, alpha, beta, false, traceBack));
//                System.out.println(depth+ "," + bestVal);
                if(bestVal >= beta)  return bestVal;
                // update the best result at 1st depth, used to place in output.txt
                if(depth == 1 && bestVal > alpha) {
                    System.out.println(c.row + " " + c.col + " " + " bestVal:" + bestVal + " alpha:" + alpha);
                    traceBack.clear();   // must clear traceBack Arraylist before add new element, keep size 1
                    traceBack.add(c);
                }
                alpha = Math.max(alpha, bestVal);
            }
        } else {
            // MIN level
            for(Chess c: emptyChess) {
                // first check valid. If invalid, just jump current chess place
                boolean checkValidRes = checkValid(board, 3 - myColor, c.row, c.col);
                if(!checkValidRes)  continue;

                // liberty old
                double myTotalLib = (double) getLibertyVal(board, myColor);
                double oppoTotalLib = (double) getLibertyVal(board, 3-myColor);

                // put my chess and remove opponent's chess
                Chess[][] nextBoard = deepCopy(board);
                nextBoard[c.row][c.col].color = 3 - myColor;
                // count dead chess
                double removeNum = deadChessNum(nextBoard, 3 - myColor);
//                System.out.println(c.row + " | " +  c.col +  "MIN removeNum:" + removeNum);

                // liberty new
                double myTotalLibNew = (double) getLibertyVal(nextBoard, myColor);
                double oppoTotalLibNew = (double) getLibertyVal(nextBoard, 3-myColor);
                double libNum = ((myTotalLibNew - myTotalLib) + (oppoTotalLib - oppoTotalLibNew)) * LIBERTY_MULTIPLE_HALF;

                // punish aside chess
                double punishNum = 0;
                if(c.row == BOARD_SIZE - 1 || c.row == 0 || c.col == BOARD_SIZE - 1 || c.col == 0) {
                    if(removeNum == 0) punishNum = PUNISH_MULTIPLE;
                }
                double totalNum = punishNum + AWARD_MULTIPLE*removeNum + libNum;
//                System.out.println(c.row + " | " +  c.col +  "MIN totalNum:" + totalNum);

//                visualizeBoard(nextBoard);
                nextBoard = removeDeadChess(nextBoard, 3 - myColor);
//                visualizeBoard(nextBoard);
                boolean checkKO = checkKO(preBoard, nextBoard);
                if(checkKO)  continue;
                bestVal = Math.min(bestVal, -totalNum + getMinMax(nextBoard, depth + 1, alpha, beta, true, traceBack));
                if(bestVal <= alpha)  return bestVal;
                beta = Math.min(beta, bestVal);
            }
        }
        return bestVal;
    }

    /*
     *  get 1 black/2 white/0 empty chess and return ArrayList of them.
     * */
    public ArrayList<Chess> getChessByColor(Chess[][] board, int color) {
        ArrayList<Chess> res = new ArrayList<>();
        for(int i = 0; i < BOARD_SIZE; i++) {
            for(int j = 0; j < BOARD_SIZE; j++) {
                if(board[i][j].color == color) {
                    res.add(board[i][j]);
                }
            }
        }
        return res;
    }

    /*
    *  Judge winner according to the chess num and KOMI
    * */
    public int judgeWinner(Chess[][] board) {
        // Black=1, White=2, Unoccupied=0
        double whiteScore = 0, blackScore = 0;
        for(int i = 0; i < BOARD_SIZE; i++) {
            for(int j = 0; j < BOARD_SIZE; j++) {
                if(board[i][j].color == 1) {
                    blackScore++;
                } else if(board[i][j].color == 2) {
                    whiteScore++;
                }
            }
        }
        // Add KOMI to white score
        whiteScore += KOMI;

        // 1: black win, -1: white win, 0: draw
        return blackScore > whiteScore ? 1 : blackScore < whiteScore ? -1 : 0;
    }

    /*
     *  get utility according to the chess num and KOMI
     * */
    public double getUtility(Chess[][] board) {
        // 1. Black=1, White=2, Unoccupied=0
        double whiteScore = 0, blackScore = 0;
        for(int i = 0; i < BOARD_SIZE; i++) {
            for(int j = 0; j < BOARD_SIZE; j++) {
                if(board[i][j].color == 1) {
                    blackScore++;
                } else if(board[i][j].color == 2) {
                    whiteScore++;
                }
            }
        }
        // Add KOMI to white score
        whiteScore += KOMI;

        // 2. calculate liberty
        double myTotalLib = (double) getLibertyVal(board, myColor);
        double oppoTotalLib = (double) getLibertyVal(board, 3-myColor);

//        // 3. judge whether this move can remove opponent's chess
//        // removeNum > 0 ,  utility = 20*(removeNum)
//        double removeNum = deadChessNum(board, 3 - myColor);
//
//        // 4. judge punish part
//        int punishNum = 0;
//

        // 4. count overall utility
        double utility = 0.0, score = 0.0;
        double liberty = (double)(myTotalLib - oppoTotalLib) * LIBERTY_MULTIPLE;
        if(myColor == 1) {
            // MY black
            score = (blackScore - whiteScore) * SCORE_MULTIPLE;
            utility = score + liberty;
        } else {
            // MY While
            score = (whiteScore - blackScore) * SCORE_MULTIPLE;
            utility = score + liberty;
        }
        // map step 3
//        utility += AWARD_MULTIPLE * removeNum;
//        System.out.println("Utility:" + utility + " liberty:" + liberty + " score:" + score + " AWARD:" + AWARD_MULTIPLE * removeNum);

        return utility;
    }


    public boolean judgeEnd(Chess[][] board) {
        int steps = 0, totalSteps = MAX_STEPS;
        for(int i = 0; i < BOARD_SIZE; i++) {
            for(int j = 0; j < BOARD_SIZE; j++) {
                if(board[i][j].color != 0) {
                    steps++;
                } else {
                    boolean res = checkValid(board, myColor, i, j);
                    if(!res) totalSteps--;
                }
            }
        }
        return steps >= totalSteps;
    }

    /*
    *  Check whether put chess in place is valid.(range/ already chess/ liberty check/ KO rule)
    *  referenced and adapted from host.py
    * */
    public boolean checkValid(Chess[][] board, int color, int row, int col) {
        // 1. within range or already have chess
        if(row < 0 || row >= BOARD_SIZE || col < 0 || col >= BOARD_SIZE || board[row][col].color != 0) return false;
        // 2. liberty rule
        Chess[][] copyBoard = deepCopy(board);
        copyBoard[row][col].color = color;
        boolean libRes = getLiberty(copyBoard, row, col);
        if(libRes)  return true;
        // 2.1 when no liberty, try to remove other's dead chess
        int removeNum = deadChessNum(copyBoard, 3-color);
        copyBoard = removeDeadChess(copyBoard, 3 - color);
        boolean libRes2 = getLiberty(copyBoard, row, col);
        if(!libRes2) return false;
        // 2.2. if has liberty after removing, check KO rule
        boolean checkKORes = checkKO(copyBoard, preBoard);
        if(checkKORes && removeNum > 0) return false;
        return true;
    }

    /*
    * referenced and adapted from host.py
    * */
    public boolean checkKO(Chess[][] board1, Chess[][] board2) {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if(board1[i][j].color != board2[i][j].color) {
                    return false;
                }
            }
        }
        return true;
    }

    /*
     * referenced by hosy.py find_died_pieces
     * */
    public ArrayList<Chess> findDeadChess(Chess[][] board, int color) {
        ArrayList<Chess> res = new ArrayList<>();
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                // have chess and no liberty
                if(board[i][j].color == color) {
                    if(!getLiberty(board, i, j)) res.add(board[i][j]);
                }
            }
        }
        return res;
    }

    /*
     * referenced by hosy.py remove_died_pieces
     * */
    public Chess[][] removeDeadChess(Chess[][] board, int color) {
        ArrayList<Chess> deadList = findDeadChess(board, color);
        for(Chess c : deadList) {
            board[c.row][c.col].color = 0;
        }
        return board;
    }

    public int deadChessNum(Chess[][] board, int color) {
        ArrayList<Chess> deadList = findDeadChess(board, color);
        return deadList.size();
    }

    public Chess[][] deepCopy(Chess[][] oldBoard) {
        Chess[][] newBoard = new Chess[BOARD_SIZE][BOARD_SIZE];
        for(int i = 0; i < BOARD_SIZE; i++) {
            for(int j = 0; j < BOARD_SIZE; j++) {
                Chess cur = oldBoard[i][j];
                newBoard[i][j] = new Chess(cur.color, cur.row, cur.col);
            }
        }
        return newBoard;
    }

    public void visualizeBoard(Chess[][] board) {
        for(int i = 0; i < BOARD_SIZE; i++) {
            for(int j = 0; j < BOARD_SIZE; j++) {
                System.out.print(board[i][j].color);
            }
            System.out.println();
        }
        System.out.println("------------------------");
    }
}
