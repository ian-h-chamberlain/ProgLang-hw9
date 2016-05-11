package hw09;

/**
 * CachedAccount: a class that represents an Account
 *     to hold an old/new value
 *     and mark an account for read or write
 */
public class CachedAccount {

	// oldValue: int - store the original value of an account
	private int oldValue;
	// newValue: int - store the updated value of an account
	private int newValue;
	// read: bool - mark whether or not this account is to be read
	private boolean read = false;
	// written: bool - mark whether or not this account is to be written
	private boolean written = false;

	// Empty constructor – no need to initialize anything
	public CachedAccount() { }
	
	// updateOld: void - setter for oldValue
	// modifies oldValue
	public void updateOld(int val) { oldValue = val; }
	
	// updateNew: void - setter for newValue
	// modifies newValue
	public void updateNew(int val) { newValue = val; }

	// getOldVal: int - getter for oldValue
	// returns oldValue
	public int getOldVal() { return oldValue; }

	// getNewVal: int - getter for newValue
	// returns newValue
	public int getNewVal() { return newValue; }
	
	// markRead: void - setter for read
	// modifies read
	public void markRead() { read = true; }

	// markWritten: void - setter for written
	// modifies written
	public void markWritten() { written = true; }
	
	// getRead: boolean - getter for read
	// returns read
	public boolean getRead() { return read; }

	// markWritten: boolean - getter for written
	// returns written
	public boolean getWritten() { return written; }
}
