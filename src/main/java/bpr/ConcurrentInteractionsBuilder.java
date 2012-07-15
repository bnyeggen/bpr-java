package bpr;

import java.io.Serializable;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

/**A version of InteractionsBuilder that can handle concurrent inserts.*/
public class ConcurrentInteractionsBuilder implements Serializable {
	
	private final ConcurrentHashMap<Integer, HashSet<Integer>> personToItemSet = new ConcurrentHashMap<Integer, HashSet<Integer>>();

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
		personToItemSet.putIfAbsent(personID, new HashSet<Integer>()).add(itemID);
	}
	
	/**Remove all records for the person. O(1) operation.*/
	public void removePerson(int personID){
		personToItemSet.remove(personID);
	}
	
	/**Remove all records for the item.  O(n) operation, as it traverses the
	 * entire map.*/
	public void removeItem(int itemID){
		for(HashSet<Integer> items:personToItemSet.values()){
			items.remove(itemID);
		}
	}
}
