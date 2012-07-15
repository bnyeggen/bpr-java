package bpr;

/**A message object used to represent a person, an item they had a positive
 * interaction with, and in item they had no (or a less-positive) interaction
 * with.*/
public class PreferenceTuple {
	public final int personID;
	public final int lessPreferredItem;
	public final int morePreferredItem;
	PreferenceTuple(int personID, int morePreferredItem, int lessPreferredItem){
		this.personID=personID;
		this.lessPreferredItem=lessPreferredItem;
		this.morePreferredItem=morePreferredItem;
	}
}
