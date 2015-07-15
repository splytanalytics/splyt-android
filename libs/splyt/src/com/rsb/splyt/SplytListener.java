package com.rsb.splyt;

/**
 * A callback that is invoked when an asynchronous operation in SPYLT completes.
 */
public interface SplytListener
{
	/**
	 * Called after an asynchronous operation in SPLYT has completed. 
	 * 
	 * @param  error  The result of the operation performed by SPLYT. 
	 */
    public void onComplete(SplytError error);
}