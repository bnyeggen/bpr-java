package com.nyeggen.bpr.interaction;

import com.nyeggen.bpr.sampling.ItemSamplableInteractions;
import com.nyeggen.bpr.sampling.PersonSamplableInteractions;

public interface InteractionsBuilder {

	/**@return a PersonSamplableInteractions based on the current state of the
	 * InteractionsBuilder.*/
	public abstract PersonSamplableInteractions makePersonSamplableInteractions();

	/**@return an ItemSamplableInteractions based on the current state of the
	 * InteractionsBuilder.*/
	public abstract ItemSamplableInteractions makeItemSamplableInteractions();

	/**Adds the person-item combination to the state of the factory.*/
	public abstract void addInteraction(int personID, int itemID);

	/**Remove all records for the person. O(1) operation.*/
	public abstract void removePerson(int personID);

	/**Remove all records for the item.  O(n) operation, as it traverses the
	 * entire map.*/
	public abstract void removeItem(int itemID);

}