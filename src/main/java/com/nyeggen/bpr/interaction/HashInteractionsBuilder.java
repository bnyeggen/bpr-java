package com.nyeggen.bpr.interaction;

import com.nyeggen.bpr.sampling.ItemSamplableInteractions;
import com.nyeggen.bpr.sampling.PersonSamplableInteractions;

import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.hash.TIntHashSet;

/**A factory for SamplableInteractionsLists.  This version does not support
 * concurrent inserts.*/
public class HashInteractionsBuilder implements InteractionsBuilder {
	
	private final TIntObjectHashMap<TIntHashSet> personToItemSet = 
			new TIntObjectHashMap<TIntHashSet>();
	
	/**@return a PersonSamplableInteractions based on the current state of the
	 * InteractionsBuilder.*/
	public PersonSamplableInteractions makePersonSamplableInteractions(){
		return new PersonSamplableInteractions(personToItemSet);
	}
	
	/**@return an ItemSamplableInteractions based on the current state of the
	 * InteractionsBuilder.*/
	public ItemSamplableInteractions makeItemSamplableInteractions(){
		return new ItemSamplableInteractions(personToItemSet);
	}
	
	/**Adds the person-item combination to the state of the factory.*/
	public void addInteraction(int personID, int itemID){
		if(!personToItemSet.containsKey(personID)){
			personToItemSet.put(personID, new TIntHashSet());
		}
		personToItemSet.get(personID).add(itemID);
	}
	
	/**Remove all records for the person. O(1) operation.*/
	public void removePerson(int personID){
		personToItemSet.remove(personID);
	}
	
	/**Remove all records for the item.  O(n) operation, as it traverses the
	 * entire map.*/
	public void removeItem(int itemID){
		for(TIntHashSet items:personToItemSet.valueCollection()){
			items.remove(itemID);
		}
	}
}
