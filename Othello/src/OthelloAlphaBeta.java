import java.util.HashMap;

/**
 * 
 */

/**
 * 
 * @author Nicholas Ver Hoeve
 */
public class OthelloAlphaBeta {
	public static class Window {
		int alpha;
		int beta;
		
		Window(int alpha, int beta) {
			this.alpha = alpha;
			this.beta = beta;
		}
	}
	
	/*
	 * This class is needed because the depth of the board when analyzed must
	 * be included and factored into the hashcode.
	 */
	public static class BoardHash {
		int hash;
		
		public BoardHash(OthelloBitBoard board, int depth) {
			int depthHash = depth * 136385313;
			hash = board.hashCode() ^ depthHash;
		}
		
		public int hashCode() {
			return hash;
		}
	};
	
	HashMap<BoardHash, Window> transpositionTable;
	
	public int AlphaBetaSearch(OthelloBitBoard position, int alpha, int beta, 
			int turn, int depth) {
		Window storedWindow = transpositionTable.get(new BoardHash(position, depth));
		
		if (storedWindow != null)
		{
			//if we know that this stored position
			if (storedWindow.alpha >= beta) {
				return storedWindow.alpha;
			}
			if (storedWindow.beta <= alpha) {
				return storedWindow.alpha;
			}
		}
		
		//align windows
		alpha = Math.min(alpha, storedWindow.alpha);
		beta = Math.min(beta, storedWindow.beta);
		
		for (long likelyMoves = position.generateLikelyMoves(turn);
				likelyMoves != 0;
				likelyMoves &= (likelyMoves - 1)) {
			int movePos = BitUtil.ulog2(BitUtil.lowSetBit(likelyMoves));
			int moveX = OthelloBitBoard.xyTox(movePos);
			int moveY = OthelloBitBoard.xyToy(movePos);
			
			if (!position.moveIsLegal(moveX, moveY, turn)) {
				continue;
			}
			
			OthelloBitBoard newPosition = position.copyAndMakeMove(moveX, moveY, turn);
			
			int newScore = AlphaBetaSearch(newPosition, -beta, -alpha, turn ^ 1, depth);
			
		}
		
		return 0;
	}
	

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
}
