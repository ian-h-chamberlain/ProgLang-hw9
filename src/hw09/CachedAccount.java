package hw09;

/**
 * @author ianchamberlain
 *
 */
public class CachedAccount {

	private int oldValue;
	private int newValue;
	private boolean read = false;
	private boolean written = false;

	public CachedAccount() { }
	
	public void updateOld(int val) { oldValue = val; }
	public void updateNew(int val) { newValue = val; }

	public int getOldVal() { return oldValue; }
	public int getNewVal() { return newValue; }
	
	public void markRead() { read = true; }
	public void markWritten() { written = true; }
	
	public boolean getRead() { return read; }
	public boolean getWritten() { return written; }

}
