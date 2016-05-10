package hw09;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

// TO DO: Task is currently an ordinary class.
// You will need to modify it to make it a task,
// so it can be given to an Executor thread pool.
//
class Task implements Runnable {
    private static final int A = constants.A;
    private static final int Z = constants.Z;
    private static final int numLetters = constants.numLetters;

    private Account[] accounts;
    private CachedAccount[] cachedAccounts;
    private String transaction;

    // TO DO: The sequential version of Task peeks at accounts
    // whenever it needs to get a value, and opens, updates, and closes
    // an account whenever it needs to set a value.  This won't work in
    // the parallel version.  Instead, you'll need to cache values
    // you've read and written, and then, after figuring out everything
    // you want to do, (1) open all accounts you need, for reading,
    // writing, or both, (2) verify all previously peeked-at values,
    // (3) perform all updates, and (4) close all opened accounts.

    public Task(Account[] allAccounts, String trans) {
        accounts = allAccounts;
        cachedAccounts = new CachedAccount[accounts.length];
        for (int i = 0; i < accounts.length; i++) {
        	cachedAccounts[i] = new CachedAccount();
        }
        transaction = trans;
    }
    
    // TO DO: parseAccount currently returns a reference to an account.
    // You probably want to change it to return a reference to an
    // account *cache* instead.
    //
    private int parseAccount(String name) {
        int accountNum = (int) (name.charAt(0)) - (int) 'A';
        if (accountNum < A || accountNum > Z)
            throw new InvalidTransactionError();
        CachedAccount a = cachedAccounts[accountNum];
        for (int i = 1; i < name.length(); i++) {
            if (name.charAt(i) != '*')
                throw new InvalidTransactionError();
            accountNum = (accounts[accountNum].peek() % numLetters);
            a = cachedAccounts[accountNum];
        }
		a.updateOld(accounts[accountNum].peek());
        return accountNum;
    }

    private CachedAccount parseAccountOrNum(String name) {
        CachedAccount rtn;
        if (name.charAt(0) >= '0' && name.charAt(0) <= '9') {
            rtn = new CachedAccount();
            rtn.updateOld(new Integer(name).intValue());
        } else {
            rtn = cachedAccounts[parseAccount(name)];
        }
        return rtn;
    }

    public void run() {
        // tokenize transaction
        String[] commands = transaction.split(";");

        while (true) {
			for (int i = 0; i < commands.length; i++) {
				String[] words = commands[i].trim().split("\\s");
				if (words.length < 3)
					throw new InvalidTransactionError();
				int lhs = parseAccount(words[0]);
				cachedAccounts[lhs].markWritten();
				if (!words[1].equals("="))
					throw new InvalidTransactionError();
				// int total to keep track of our update value
				int total = 0;
				// arraylist of accounts that create our total
				CachedAccount rhs = parseAccountOrNum(words[2]);
				rhs.markRead();
				total = rhs.getOldVal();
				for (int j = 3; j < words.length; j+=2) {
					if (words[j].equals("+")){
						rhs = parseAccountOrNum(words[j+1]);
						rhs.markRead();
						total += rhs.getOldVal();
					}
					else if (words[j].equals("-")){
						rhs = parseAccountOrNum(words[j+1]);
						rhs.markRead();
						total -= rhs.getOldVal();
					}
					else{
						throw new InvalidTransactionError();
					}
				}

				cachedAccounts[lhs].updateNew(total);
			}
				
			try{
				// open all accounts
				for (int r = A; r <= Z; r++) {
					if (cachedAccounts[r].getRead()) {
						accounts[r].open(false);
					}
					if (cachedAccounts[r].getWritten()) {
						accounts[r].open(true);
					}
				}
			
				// verify all accounts
				for (int r = A; r <= Z; r++) {
					if (cachedAccounts[r].getRead())
						accounts[r].verify(cachedAccounts[r].getOldVal());
				}
				
				// write all values
				for (int r = A; r <= Z; r++) {
					if (cachedAccounts[r].getWritten()) {
						accounts[r].update(cachedAccounts[r].getNewVal());
						System.err.println("Account " + r + " written with " + cachedAccounts[r].getNewVal());
					}
				}

				// close all accounts
				for (int r = A; r <= Z; r++) {
					if (cachedAccounts[r].getRead() || cachedAccounts[r].getWritten()) {
						try {
							accounts[r].close();
						}
						catch (TransactionUsageError ex) {
							// already closed, no need to close again
						}
					}
				}
				
				break;
				
			} catch (TransactionAbortException e){
				for (int r = A; r <= Z; r++) {
					if (cachedAccounts[r].getRead() || cachedAccounts[r].getWritten()) {
						try {
							accounts[r].close();
						}
						catch (TransactionUsageError ex) {
							// already closed, no need to close again
						}
					}
				}
				continue;
			}
		}

        System.out.println("commit: " + transaction);
    }
}

public class MultithreadedServer {
	
	// requires: accounts != null && accounts[i] != null (i.e., accounts are properly initialized)
	// modifies: accounts
	// effects: accounts change according to transactions in inputFile
    public static void runServer(String inputFile, Account accounts[])
        throws IOException {

        // read transactions from input file
        String line;
        BufferedReader input =
            new BufferedReader(new FileReader(inputFile));
        ExecutorService e = Executors.newCachedThreadPool();

        // TO DO: you will need to create an Executor and then modify the
        // following loop to feed tasks to the executor instead of running them
        // directly.  

        while ((line = input.readLine()) != null) {
            Task t = new Task(accounts, line);
            e.execute(t);
        }
        
        e.shutdown();
        
        try {
			e.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
        	System.err.println("Thread interrupted with timeout!");
        }
        
        input.close();

    }
}
