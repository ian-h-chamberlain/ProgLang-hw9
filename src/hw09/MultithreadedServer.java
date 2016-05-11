package hw09;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
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
    private int id;

    public Task(Account[] allAccounts, String trans, int id) {
        accounts = allAccounts;
        // array of cached accounts the size of accounts
        cachedAccounts = new CachedAccount[accounts.length];
        for (int i = 0; i < accounts.length; i++) {
        	cachedAccounts[i] = new CachedAccount();
        }
        transaction = trans;
        // used to determine the amount of time to sleep in the case of deadlock
        this.id = id;
    }
    
    // returns the account number that was parsed
    // modifies the cached account with a peek() value from the original account
    private int parseAccount(String name) {
        int accountNum = (int) (name.charAt(0)) - (int) 'A';
        if (accountNum < A || accountNum > Z)
            throw new InvalidTransactionError();
        // get a reference to the cached account to update
        CachedAccount a = cachedAccounts[accountNum];
        // handles dereferencing
        for (int i = 1; i < name.length(); i++) {
            if (name.charAt(i) != '*')
                throw new InvalidTransactionError();
            accountNum = (accounts[accountNum].peek() % numLetters);
            a = cachedAccounts[accountNum];
        }
		a.updateOld(accounts[accountNum].peek());
        return accountNum;
    }

    // returns a cached account either:
    //    a reference to an account in the array
    //    a new cached account only holding a constant value
    private CachedAccount parseAccountOrNum(String name) {
        CachedAccount rtn;
        // if constant
        if (name.charAt(0) >= '0' && name.charAt(0) <= '9') {
            rtn = new CachedAccount();
            rtn.updateOld(new Integer(name).intValue());
        } else {
        	// get the right cached account from parse account 
        	// which returns and account number
            rtn = cachedAccounts[parseAccount(name)];
        }
        return rtn;
    }
    
    // this is called from the catch block
    // goes through the cachedAccounts and if we wanted to open the account
    // this function tries to close the account
    private void closeAll() {
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
    }

    // the function that each Task runs when called by the thread server
    // parses and completes a transaction
    public void run() {
        // tokenize transaction
        String[] commands = transaction.split(";");

        // while the transaction is not completed
        while (true) {
        	// for each command in the transaction
			for (int i = 0; i < commands.length; i++) {
				
				// parsing of command
				String[] words = commands[i].trim().split("\\s");
				if (words.length < 3)
					throw new InvalidTransactionError();
				// find the account on the left hand side and mark to write
				int lhs = parseAccount(words[0]);
				cachedAccounts[lhs].markWritten();
				if (!words[1].equals("="))
					throw new InvalidTransactionError();
				
				// int total to keep track of the final value for the command
				int total = 0;
				// find all accounts on the right hand side and mark to read
				CachedAccount rhs = parseAccountOrNum(words[2]);
				rhs.markRead();
				total = rhs.getOldVal();
				// loop through the rest of the right hand side equations
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

				// set the value that we will update the left hand side with
				cachedAccounts[lhs].updateNew(total);
			}
				
			int r = -1;
			// try to open all the accounts
			try{
				// open all accounts
				for (r = A; r <= Z; r++) {
					// if we need both read and write open for both
					if (cachedAccounts[r].getRead() && cachedAccounts[r].getWritten()) {
						accounts[r].open(false);
						accounts[r].open(true);
					}
					// if we only need to write
					else if (cachedAccounts[r].getWritten()) {
						accounts[r].open(true);
					}
					// if we only need to read
					else if (cachedAccounts[r].getRead()) {
						accounts[r].open(false);
					}
					
				}
			} catch (TransactionAbortException e) { // failed to open an account
				// close all of the accounts
				closeAll();
				// if two threads are competing we need to offset them from each other
				try {
					// desynchronize competing threads
					Thread.sleep(id * 7);
				} catch (InterruptedException ex) {
					// no need to do anything here
				}
				// run the loop again since transaction failed
				continue;
			}

			// verify all account values before updating the write value
			try {
				// verify all accounts we are reading from
				for (r = A; r <= Z; r++) {
					if (cachedAccounts[r].getRead())
						accounts[r].verify(cachedAccounts[r].getOldVal());
				}
			} catch (TransactionAbortException e){
				// if verify fails
				closeAll();
				continue;
			}

			// write all values
			for (r = A; r <= Z; r++) {
				// write the new values from the cached accounts to be written
				if (cachedAccounts[r].getWritten()) {
					accounts[r].update(cachedAccounts[r].getNewVal());
				}
			}
			// close all of the accounts and break because we succeeded
			closeAll();
			break;
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

        
        int i = 0;
        while ((line = input.readLine()) != null) {
            Task t = new Task(accounts, line, i);
            i++;
            // execute each task (non-blocking)
            e.execute(t);
        }
        // call shutdown when all tasks are started
        e.shutdown();
        
        // wait for all threads to complete
        try {
        	// wait up to 60 seconds for all threads to finish
			e.awaitTermination(60, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
        	System.err.println("Thread interrupted with timeout!");
        }
        
        input.close();

    }
}
