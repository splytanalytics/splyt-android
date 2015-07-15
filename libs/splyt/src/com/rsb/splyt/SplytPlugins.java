package com.rsb.splyt;

import java.util.HashMap;
import java.util.Map;

/**
 * This class contains a set of helper classes (plugins) that are designed to make it easy to report common categories
 * of app activity to SPLYT.
 * <p>
 * Use the {@code Transaction} method on each plugin to create a transaction for the scenario addressed by that
 * plugin.  A couple of examples:
 * </p>
 * <pre>
 *    SplytPlugins.Session.Transaction().begin();
 *    // ...
 *    SplytPlugins.Session.Transaction().end();
 *    
 *    SplytPlugins.Purchase.Transaction splytPurchase = SplytPlugins.Purchase.Transaction();
 *    splytPurchase.setPrice("usd", 1.99);
 *    splytPurchase.setItemName("Value Pack");
 *    splytPurchase.beginAndEnd();
 * </pre>
 *
 * @author Copyright 2013 Row Sham Bow, Inc.
 */
public class SplytPlugins
{
	/**
	 * Provides factory methods for creating a {@link SplytPlugins.Session.Transaction}, which is used to
	 * report session activity in your app.
	 * <p>
	 * Use the {@link Session#Transaction()} method to create a new session transaction.  An example:
	 * </p>
	 * <pre>
	 *    SplytPlugins.Session.Transaction().begin();
	 *    // ...
	 *    SplytPlugins.Session.Transaction().end();
	 * </pre>
	 */
    public static class Session
    {
        private static final String CATEGORY_NAME = "session";
        private static final Double DEFAULT_TIMEOUT = Double.valueOf(10 * 86400);   // 10 days
        private static final String DEFAULT_TIMEOUT_MODE = SplytConstants.TIMEOUT_MODE_ANY;
        
        /**
         * A light wrapper around {@link Splyt.Instrumentation.Transaction} that makes it easy to report session 
         * activity in your app.
         */
        public static class Transaction extends TransactionBase<Transaction>
        {
            private Transaction() {
                super(CATEGORY_NAME, null);
                
                mTimeoutMode = DEFAULT_TIMEOUT_MODE;
                mTimeout = DEFAULT_TIMEOUT;
            }
            
            /**
             * Report the beginning of a session.
             * <p>
             * When beginning a session, any properties which have been set (see {@link Splyt.Instrumentation.Transaction#setProperty} and 
             * {@link Splyt.Instrumentation.Transaction#setProperties}) are also included with the data sent to SPLYT.
             * 
             * @param  timeout      If SPLYT does not receive any updates to this session for a period longer than
             *                      the timeout interval specified, the session is considered to have timed out.
             */
            public void begin(Double timeout)
            {
                super.begin(DEFAULT_TIMEOUT_MODE, timeout);
            }
        }

        /**
         * Creates a new {@link SplytPlugins.Session.Transaction}.
         */
        public static Transaction Transaction()
        {
            return new Transaction();
        }
    }

	/**
	 * Provides factory methods for creating a {@link SplytPlugins.Purchase.Transaction}, which is used to report
	 * purchases from inside your app.
	 * <p>
	 * Use the {@link Purchase#Transaction()} method to create a new session transaction.  An example:
	 * </p>
	 * <pre>
     *    SplytPlugins.Purchase.Transaction splytPurchase = SplytPlugins.Purchase.Transaction();
     *    splytPurchase.setPrice("usd", 1.99);
     *    splytPurchase.setItemName("Value Pack");
     *    splytPurchase.beginAndEnd();
	 * </pre>
	 */
    public static class Purchase
    {
        private static final String CATEGORY_NAME = "purchase";
        private static final Double DEFAULT_TIMEOUT = Double.valueOf(60);   // 60 seconds
        
        /**
         * A light wrapper around {@link Splyt.Instrumentation.Transaction} that makes it easy to report purchase
         * activity in your app.
         */
        public static class Transaction extends TransactionBase<Transaction>
        {
            private Transaction(String transactionId)
            {
                super(CATEGORY_NAME, transactionId);

                // Set the timeout to 60 seconds...
                mTimeout = DEFAULT_TIMEOUT;

                // Create the property bag for this purchase
                mProperties = new HashMap<String, Object>();
            }

            /**
             * Reports the price of the item being purchased.
             *
             * @param  currency  For real currency purchases, the ISO 4217 currency code (e.g., {@code "USD"}) that applies to {@code price}. This is NOT case sensitive
             *                   Or if that is unknown, pass currency symbol (e.g., {@code "$", "€", "£", etc} and we will attempt to determine the correct currency for you.
             *                   For virtual currency purchases, this is the name of the virtual currency used to make the purchase (e.g., {@code "coins", "gems", etc.})
             *                   NOTE:  Only ASCII characters are supported for virtual currencies.  Any non-ASCII characters are stripped.
             * @param  price     The price of the item.
             * @return This {@link SplytPlugins.Purchase.Transaction} instance.
             */
            public Transaction setPrice(String currency, Double price)
            {
                Map<String, Double> priceMap = new HashMap<String, Double>();
                priceMap.put(Util.getValidCurrencyString(currency), price);
                
                mProperties.put("price", priceMap);
                
                return this;
                
                /* ONCE WE ADD LIST SUPPORT TO THE DATA COLLECTOR, SWITCH OUT THIS BLOCK
                if(!mProperties.containsKey("price"))
                {
                    mProperties.put("price", new ArrayList<Map<String, Object> >());
                }
                
                @SuppressWarnings("unchecked")
                List<Map<String, Object> > prices = (List<Map<String, Object> >)mProperties.get("price");
                
                Map<String, Object> pricePair = new HashMap<String, Object>(2);
                pricePair.put("currency", currency);
                pricePair.put("amount", price);
                
                prices.add(pricePair);
                return this;
                */
            }

            /**
             * Reports an offer ID for the item being purchased. Useful for identifying promotions or other 
             * application-defined offers.
             * 
             * @param  offerId  The offer ID.
             * @return This {@link SplytPlugins.Purchase.Transaction} instance.
             */
            public Transaction setOfferId(String offerId)
            {
                mProperties.put("offerId", offerId);

                return this;
            }

            /**
             * Reports the name of the item being purchased.
             *
             * @param  itemName  The item name.
             * @return This {@link SplytPlugins.Purchase.Transaction} instance.
             */
            public Transaction setItemName(String itemName)
            {
                mProperties.put("itemName", itemName);

                return this;
            }

            /**
             * Reports the point of sale. Useful in situations where an application may have multiple points of purchase.
             *
             * @param  pointOfSale  An application-defined point of sale.
             * @return This {@link SplytPlugins.Purchase.Transaction} instance.
             */
            public Transaction setPointOfSale(String pointOfSale)
            {
                mProperties.put("pointOfSale", pointOfSale);

                return this;
            }
        }

        /**
         * Factory method for creating a purchase transaction.
         * Use this when <b>more than one</b> purchase transaction can be active at any given point in time
         *
         * @param  transactionId  A unique identifier for the created transaction. This is only required in situations
         *                        where multiple purchase transactions may exist for the same user at the same time.
         * @return The created {@link SplytPlugins.Purchase.Transaction}.              
         */
        public static Transaction Transaction(String transactionId)
        {
            return new Transaction(transactionId);
        }

        /**
         * Creates a new {@link SplytPlugins.Purchase.Transaction}.
         * 
         * @return The created {@link SplytPlugins.Purchase.Transaction}.              
         */
        public static Transaction Transaction()
        {
            return new Transaction(null);
        }
    }

    /**
     * This utility class is required so that Plugins which extend {@link Splyt.Instrumentation.Transaction} do
     * not misbehave when calling the builder methods within it
     */
    private abstract static class TransactionBase<T extends TransactionBase<T> > extends Splyt.Instrumentation.Transaction
    {
        public TransactionBase(String category, String transactionId) {
            super(category, transactionId);
        }

        /**
         * @see Splyt.Instrumentation.Transaction#setProperty
         */
        @Override
        public T setProperty(String key, Object value)
        {
            super.setProperty(key, value);
            
            @SuppressWarnings("unchecked")
            T thisObj = (T) this;
            return thisObj;
        }

        /**
         * @see Splyt.Instrumentation.Transaction#setProperties
         */
        @Override
        public T setProperties(Map<String, Object> properties)
        {
            super.setProperties(properties);
            
            @SuppressWarnings("unchecked")
            T thisObj = (T) this;
            return thisObj;
        }
    }
}