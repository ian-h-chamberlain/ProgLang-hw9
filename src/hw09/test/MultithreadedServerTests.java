package hw09.test;

import hw09.*;

import java.io.*;
import java.lang.Thread.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.*;
import java.util.Random;

import junit.framework.TestCase;

import org.junit.Test;

public class MultithreadedServerTests extends TestCase {

    private static final int A = constants.A;
    private static final int Z = constants.Z;
    private static final int numLetters = constants.numLetters;
    private static Account[] accounts;
            
    protected static void dumpAccounts() {
	    // output values:
	    for (int i = A; i <= Z; i++) {
	       System.out.print("    ");
	       if (i < 10) System.out.print("0");
	       System.out.print(i + " ");
	       System.out.print(new Character((char) (i + 'A')) + ": ");
	       accounts[i].print();
	       System.out.print(" (");
	       accounts[i].printMod();
	       System.out.print(")\n");
	    }
	 }    
     
        
     // Initially provided test case for incrementing each account
     @Test
	 public void testIncrement() throws IOException {
	
    	// The array itself is never modified by each thread, even if items in it are
		/* shared immutable state */ accounts = new Account[numLetters];

		// initialize all accounts to have value 1
		for (int i = A; i <= Z; i++) {
			accounts[i] = new Account(Z-i);
		}			 
		
		MultithreadedServer.runServer("hw09/data/increment", accounts);
	
		// assert correct account values
		for (int i = A; i <= Z; i++) {
			Character c = new Character((char) (i+'A'));
			assertEquals("Account "+c+" differs",Z-i+1,accounts[i].getValue());
		}		

	 }
     
     // This test case only checks to avoid deadlock and has non-deterministic results
     @Test
     public void testRotate() throws IOException {
    	 
    	 // The array itself is never modified by each thread, even if items in it are
    	 /* shared immutable state */ accounts = new Account[numLetters];
    	 
    	 // initialize all accounts to have value 1
    	 for (int i = A; i <= Z; i++) {
    		 /* shared mutable state */ accounts[i] = new Account(1);
    	 }
    	 
    	 MultithreadedServer.runServer("hw09/data/rotate", accounts);
    	 
    	 // assert nothing really weird happened with account values
    	 for (int i = A; i <= Z; i++) {
    		 Character c = new Character((char) (i+'A'));
    		 assertTrue("Account "+c+" has incorrect value " + accounts[i].getValue(), accounts[i].getValue() > 0);
    	 }
    	 
    	 // dump account values to visually verify nondeterministic output
    	 dumpAccounts();
    		 
     }
     
     // Test case that includes indirection
     @Test
     public void testIndirection() throws IOException {

    	 // The array itself is never modified by each thread, even if items in it are
    	 /* shared immutable state */ accounts = new Account[numLetters];
    	 for (int i = A; i <= Z; i++) {
    		 /* shared mutable state */ accounts[i] = new Account(1);
    	 }
    	 
    	 MultithreadedServer.runServer("hw09/data/indirection", accounts);
    	 
    	 // The result only has a few possible cases for each value
    	 assertTrue("Account A has incorrect value " + accounts[A].getValue(),
    			 accounts[A].getValue() == 0 ||
    			 accounts[A].getValue() == 4 ||
    			 accounts[A].getValue() == 5);

    	 assertTrue("Account E has incorrect value " + accounts[A + 4].getValue(),
    			 accounts[A+4].getValue() == 12 ||
    			 accounts[A+4].getValue() == 1 ||
    			 accounts[A+4].getValue() == 5);
    	 
    	 assertTrue("Account F has incorrect value " + accounts[A + 5].getValue(),
    			 accounts[A+5].getValue() == 1 ||
    			 accounts[A+5].getValue() == 4);
     }
     
     // Test case for decrementing
     @Test
     public void testDecrement() throws IOException {
	
		// The array itself is never modified by each thread, even if items in it are
		/* shared immutable state */ accounts = new Account[numLetters];
		for (int i = A; i <= Z; i++) {
			/* shared mutable state */ accounts[i] = new Account(Z-i);
		}			 
		
		MultithreadedServer.runServer("hw09/data/decrement", accounts);
	
		// assert correct account values
		for (int i = A; i <= Z; i++) {
			Character c = new Character((char) (i+'A'));
			assertEquals("Account "+c+" differs",Z-i-1,accounts[i].getValue());
		}		

     }
     
     // Test passing around a unit over a set of accounts
     @Test
     public void testRotateInc() throws IOException {

		// The array itself is never modified by each thread, even if items in it are
		/* shared immutable state */ accounts = new Account[numLetters];

		for (int i = A; i <= Z; i++) {
			/* shared mutable state */ accounts[i] = new Account(100);
		}			 
		
		MultithreadedServer.runServer("hw09/data/rotateInc", accounts);

		// Assert that after rotating completely, every account has 100 in it
		for (int i = A; i <= Z; i++) {
			Character c = new Character((char) (i+'A'));
			assertEquals("Account "+c+" differs",100,accounts[i].getValue());
		}
     }
}