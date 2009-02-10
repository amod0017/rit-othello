package core;
import java.io.*;

/**
 * For generating PRECOMPUTED bitboard tables. NOT part of the primary execution.
 * 
 * @author Nicholas Ver Hoeve
 */
public class TableGenerator {
	static DataOutputStream out;
	
	/**
	 * @param args : ignored
	 */
	public static void main(String[] args) {
		try {
			File file = new File("RowLookup.dat");
			out = new DataOutputStream(new FileOutputStream(file));
			makeFormattedTableA();
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("done");
	}
	
	//generate binary file of table data
	static void makeFormattedTableA() throws IOException {
		int bytenum = 0;
		
		for (int i = 0; i < 8; ++i) {
			for (int e = 0; e < 256; ++e) {
				for (int m = 0; m < 256; ++m) {
					int w = 0;
					
					int p = 1 << i;

					if (((p & (m | e)) != 0)) {
						w = m; // can't go on top of another piece
						//the value we store shows 'no change' to indicite illegality
					} else {
						w = m | p;
						int empty = ~(m | e); // empty spaces
						
						int leftPiece = BitUtil.highSetBit(m & (p - 1));
						int zone = (byte)BitUtil.inbetweenZone((byte)(leftPiece | p));
						
						if ((zone & empty) == 0) {
							w |= zone;
						}
						
						int rightPiece = BitUtil.lowSetBit(m & ~(p - 1));
						zone = (byte)BitUtil.inbetweenZone((byte)(rightPiece | p));
						
						if ((zone & empty) == 0) {
							w |= zone;
						}
						
						//last test... must have flipped something or this isn't a move
						if (w == (m | p))
						{
							w = m;
							//the value we store shows 'no change' to indicite illegality
						}
					}
					
					out.writeByte(w);
					++bytenum;
				}
			}
		}
	}
}
