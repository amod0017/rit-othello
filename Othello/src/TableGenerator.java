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
	
	//generate text file of table
	static void makeFormattedTableA() throws IOException {
		int m = 0;
		int bytenum = 0;
		
		for (int i = 0; i < 8; ++i) {
			for (int e = 0; e < 256; ++e) {
				m = 0;
				
				for (int y = 0; y < 8; ++y) {
			
					for (int x = 0; x < 32; ++x) {
						int w = 0;
						
						int p = 1 << i;

						if (((p & m) != 0) | ((p & e) != 0)) {
							w = 0; // can't go on top of another piece
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
								w = 0;
							}
						}
						
						out.writeByte(w);
						++bytenum;
					
						++m;
					}
				}
			}
		}
	}
}
