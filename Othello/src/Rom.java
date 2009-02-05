import java.io.*;

/*
 * 
 */
public class Rom {
	public static byte[] ROWLOOKUP = loadTable();
	
	static private byte[] loadTable() {
		byte[] array = new byte[8*256*256];
		try {
			File file = new File("RowLookup.dat");
			DataInputStream in = new DataInputStream(new FileInputStream(file));
			in.read(array);
			in.close();
		} catch (IOException e) {
			System.err.println("WARNING: could not load table");
		}
		return array;
	}
}
