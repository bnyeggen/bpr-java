package bpr;

/**Interface representing a set of interactions, from which can be sampled
 * tuples consisting of a person, a more-preferred item, and a less-preferred
 * item.
 * 
 * Implementations primarily define different sampling approaches.*/
public interface SamplableInteractionList {
	public PreferenceTuple get();
}
