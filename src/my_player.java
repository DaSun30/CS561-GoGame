public class my_player {

    public static void main(String[] args) {
        Go go = new Go();
        go.buildBoard();
        go.alphaBetaSearch();
        go.outputBoard();
    }

}
