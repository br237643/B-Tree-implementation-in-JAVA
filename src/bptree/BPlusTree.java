package bptree;

/**
 * The {@code BPlusTree} class implements B+-trees. Each {@code BPlusTree}
 * stores its elements in the main memory (not on disks) for simplicity.
 * 
 * @author Jeong-Hyon Hwang (jhh@cs.albany.edu)
 * 
 * @param <K>
 *            the type of keys
 * @param <V>
 *            the type of values
 */
public class BPlusTree<K extends Comparable<K>, V> {

	/**
	 * The maximum number of pointers that each {@code Node} of this
	 * {@code BPlusTree} can have.
	 */
	protected int degree;

	/**
	 * The root node of this {@code BPlusTree}.
	 */
	protected Node<K> root;

	/**
	 * Constructs a {@code BPlusTree}.
	 * 
	 * @param degree
	 *            the maximum number of pointers that each {@code Node} of this
	 *            {@code BPlusTree} can have.
	 */
	public BPlusTree(int degree) {
		this.degree = degree;
	}

	/**
	 * Copy-constructs a {@code BPlusTree}.
	 * 
	 * @param tree
	 *            another {@code BPlusTree} to copy from.
	 */
	@SuppressWarnings("unchecked")
	public BPlusTree(BPlusTree<K, V> tree) {
		this.degree = tree.degree;
		if (tree.root instanceof LeafNode)
			this.root = new LeafNode<K, V>((LeafNode<K, V>) tree.root);
		else
			this.root = new NonLeafNode<K>((NonLeafNode<K>) tree.root);
	}

	/**
	 * Returns the degree of this {@code BPlusTree}.
	 * 
	 * @return the degree of this {@code BPlusTree}.
	 */
	public int degree() {
		return degree;
	}

	/**
	 * Returns the root {@code Node} of this {@code BPlusTree}.
	 * 
	 * @return the root {@code Node} of this {@code BPlusTree}.
	 */
	public Node<K> root() {
		return root;
	}

	/**
	 * Finds the {@code LeafNode} in this {@code BPlusTree} that must be
	 * responsible for the specified key.
	 * 
	 * @param key
	 *            the search key.
	 * @return the {@code LeafNode} in this {@code BPlusTree} that must be
	 *         responsible for the specified key.
	 */
	@SuppressWarnings("unchecked")
	public LeafNode<K, V> find(K key) {
		Node<K> c = root;
		while (c instanceof NonLeafNode) {
			c = ((NonLeafNode<K>) c).child(key);
		}
		return (LeafNode<K, V>) c;
	}

	/**
	 * Finds the parent {@code Node} of the specified {@code Node}.
	 * 
	 * @param node
	 *            a {@code Node}.
	 * @return the parent {@code Node} of the specified {@code Node};
	 *         {@code null} if the parent cannot be found.
	 */
	public NonLeafNode<K> findParent(Node<K> node) {
		Node<K> p = root;
		while (p != null) {
			K key = node.firstKey();
			Node<K> c = ((NonLeafNode<K>) p).child(key);
			if (c == node) { // if found the parent of the node.
				return (NonLeafNode<K>) p;
			}
			p = c;
		}
		return null;
	}

	/**
	 * Inserts the specified key and the value into this {@code BPlusTree}.
	 * 
	 * @param key
	 *            the key to insert.
	 * @param value
	 *            the value to insert.
	 */
	public void insert(K key, V value) {
		LeafNode<K, V> leaf; // the leaf node where insertion will occur
		if (root == null) { // if the root is null
			leaf = new LeafNode<K, V>(degree);
			root = leaf;
		} else { // if root is not null
			leaf = find(key);
		}
		if (leaf.hasRoom()) { // if the leaf node has room for the new entry
			leaf.insert(key, value);
		} else { // if split is required
			LeafNode<K, V> t = new LeafNode<K, V>(degree + 1); // create a
																// temporary
																// leaf node
			t.copy(leaf, 0, leaf.numberOfKeys());// copy everything to the
													// temporary node
			t.insert(key, value); // insert the key and value to the temporary
									// node
			LeafNode<K, V> nLeaf = new LeafNode<K, V>(degree); // create a new
																// leaf node
			nLeaf.setSuccessor(leaf.successor()); // chaining
			leaf.clear(); // clear the leaf node
			leaf.setSuccessor(nLeaf); // chaining from leaf to nLeaf
			int m = (int) Math.ceil(degree / 2.0); // compute the split point
			leaf.copy(t, 0, m); // put the first half into leaf
			nLeaf.copy(t, m, t.numberOfKeys()); // put the second half to nLeaf
			insertInParent(leaf, nLeaf.firstKey(), nLeaf); // use the first key
															// of nLeaf as the
															// separator.
		}
	}

	/**
	 * Inserts pointers to the specified {@code Node}s into an appropriate
	 * parent {@code Node}.
	 * 
	 * @param n
	 *            a {@code Node}.
	 * @param key
	 *            the key that splits the {@code Node}s
	 * @param nn
	 *            a new {@code Node}.
	 */
	void insertInParent(Node<K> n, K key, Node<K> nn) {
		if (n == root) { // if the root was split
			root = new NonLeafNode<K>(degree); // create a new node
			root.insert(key, n, 0); // make the new root point to the nodes.
			root.pointers[1] = nn;
			return;
		}
		NonLeafNode<K> p = findParent(n);
		if (p.hasRoom()) {
			p.insertAfter(key, nn, n); // insert key and nn right after n
		} else { // if split is required
			NonLeafNode<K> t = new NonLeafNode<K>(degree + 1); // crate a
																// temporary
																// node
			t.copy(p, 0, p.numberOfKeys()); // copy everything of p to the
											// temporary node
			t.insertAfter(key, nn, n); // insert key and nn after n
			p.clear(); // clear p
			int m = (int) Math.ceil(degree / 2.0); // compute the split point
			p.copy(t, 0, m - 1);
			NonLeafNode<K> np = new NonLeafNode<K>(degree); // create a new node
			np.copy(t, m, t.numberOfKeys()); // put the second half to np
			insertInParent(p, t.keys[m - 1], np); // use the middle key as the
													// separator
		}
	}

	/**
	 * Deletes the specified key and the value from this {@code BPlusTree}.
	 * 
	 * @param key
	 *            the key to delete.
	 * @param value
	 *            the value to delete.
	 */
	public void delete(K key, V value) {
		Node<K> L = find(key); // Find node which contains the key
		for (int i = 0; i < L.numberOfKeys; i++)
			if (L.keys[i] == key && key != null) {
				delete_entry(L, key);
			}
	}

	public void delete_entry(Node<K> N, K key) {
		//I have Initialized a new function in Node class as delete.
		// In that function , it directly deletes the node. 
		N.delete(key);
		// N is the root and N has only one remaining child
		if (N == root && numberOfpointers(root) == 1) {
			// make the child of N the new root of the tree and delete N
			for (int i = 0; i < N.pointers.length; i++) {
				if (N.pointers[i] != null) {
					if (N.pointers[i] instanceof LeafNode)
						root = (LeafNode) N.pointers[i]; //Making the child as root
					else
						root = (NonLeafNode) N.pointers[i];
				}
			}
			N.clear(); // Delete N
		} else if (numberOfpointers(N) < 2) { // N has too few values/pointers
			Node<K> nParent = findParent(N);
			int nPos = 0; // Position of N
			int ndpos = 0; // Position of N'
			// Get the position of N in parent
			for (int j = 0; j < nParent.pointers.length; j++)
				if (nParent.pointers[j] == N)
					nPos = j;
			K k_;
			int k_pos = 0;
			if (nPos == 0) { // When N' is next node of same parent
				k_ = nParent.keys[nPos];
				ndpos = nPos + 1;
				k_pos = nPos;
			} else {// if(nPos==1){ // N' is previous/predecessor of N
				ndpos = nPos - 1;
				k_pos = ndpos;
				k_ = nParent.keys[ndpos];
			}
			
			Node<K> Ndash;
			if (nParent.pointers[ndpos] instanceof LeafNode) {
				Ndash = (LeafNode) nParent.pointers[ndpos];
			} else
				Ndash = (NonLeafNode) nParent.pointers[ndpos];
			//(entries in N and N can ﬁt in a single node
			if ((((Node) nParent.pointers[nPos]).numberOfKeys
					+ ((Node) nParent.pointers[ndpos]).numberOfKeys) < degree) {
				if (nPos < ndpos) {
					//Swapping N and N'
					NonLeafNode<K> temp = new NonLeafNode<K>(degree);
					temp.numberOfKeys = 0; // creating a temporary node 
					for (int i = 0; i < N.numberOfKeys; i++) {
						temp.keys[i] = N.keys[i];// Storing the values of N in temporary
						temp.numberOfKeys++;
					}
					for (int i = 0; i < degree; i++)
						temp.pointers[i] = N.pointers[i];
					N.numberOfKeys = 0;
					for (int i = 0; i < Ndash.numberOfKeys; i++) {
						N.keys[i] = (K) Ndash.keys[i]; //Storing N' in N now
						N.numberOfKeys++;
					}
					for (int i = 0; i < degree; i++)
						N.pointers[i] = Ndash.pointers[i];
					Ndash.numberOfKeys = 0;

					for (int i = 0; i < temp.numberOfKeys; i++) {
						Ndash.keys[i] = temp.keys[i]; // now storing the values of N which was 
					                                   //previously stored in Temp , so
						                              // Now N' will have N values
						Ndash.numberOfKeys++;
					}
					for (int i = 0; i < degree; i++)
						Ndash.pointers[i] = temp.pointers[i];
					temp.clear(); // Deleting Temporary
				}
				// N is not a leaf
				if (N instanceof NonLeafNode) {
					Ndash.keys[Ndash.numberOfKeys++] = k_;
					for (int i = 0; i < Ndash.numberOfKeys; i++) {
						Ndash.pointers[Ndash.numberOfKeys] = N.pointers[i];// Copying N into N'
						Ndash.keys[Ndash.numberOfKeys++] = N.keys[i];
					}
					if (N.pointers[N.numberOfKeys] != null) // Last pointer
						Ndash.pointers[Ndash.numberOfKeys] = N.pointers[N.numberOfKeys];
				} else {
					for (int i = 0; i < N.numberOfKeys; i++) {
						Ndash.pointers[Ndash.numberOfKeys] = N.pointers[i];
						Ndash.keys[Ndash.numberOfKeys++] = N.keys[i];
					}
					Ndash.pointers[degree - 1] = N.pointers[degree - 1];
				}
				if (nParent.numberOfKeys == 1)
					nParent.pointers[nPos] = null;
				delete_entry(nParent, k_);
				N.clear();//Delete Node N
			} else {
				//Redistribution borrow an entry from N'
				//(N is a predecessorof N
				if (ndpos < nPos) {
					//If N is a NonleafNode
					if (N instanceof NonLeafNode) {
						//let m be such that N.Pm is the last pointer 
						//in N remove (N.Km−1, N.Pm) fromN 
						int mPos = 0;
						for (int i = 0; i < Ndash.pointers.length; i++) {
							if (Ndash.pointers[i] != null) {
								mPos++;
							}
						}
						
						Object m = Ndash.pointers[mPos];
						K tempKm = Ndash.keys[mPos - 1];
						Ndash.keys[mPos - 1] = null;
						Ndash.numberOfKeys--;
						Ndash.pointers[mPos] = null;
						N.insert(k_, m, 0);
						nParent.keys[k_pos] = nParent.keys[mPos - 1];
					} else {
						int mPos = 0;
						// K lastkey = n.keys[n.numberOfKeys];
						for (int i = 0; i < Ndash.pointers.length; i++) {
							if (Ndash.pointers[i] != null) {
								mPos++;
							}
						}
						K tempKm = Ndash.keys[mPos];
						Object m = Ndash.pointers[mPos];
						Ndash.pointers[mPos] = null;
						Ndash.keys[mPos] = null;
						Ndash.numberOfKeys--;
						N.insert(tempKm, m, 0);
						nParent.keys[k_pos] = nParent.keys[mPos];

					}
				} else {
					//Again If N is nonLeafNode
					if (N instanceof NonLeafNode) {
						int mPos = 0;
						for (int i = 0; i < Ndash.pointers.length; i++) {
							if (Ndash.pointers[i] != null) {
								mPos++;
							}
						}
						
						Object m = Ndash.pointers[mPos];
						K tempKm = Ndash.keys[mPos - 1];
						Ndash.keys[mPos - 1] = null;
						Ndash.numberOfKeys--;
						Ndash.pointers[mPos] = null;
						N.insert(k_, m, 0);
						nParent.keys[k_pos] = nParent.keys[mPos - 1];

					} else {
						int mPos = 0;
						// K lastkey = n.keys[n.numberOfKeys];
						for (int i = 0; i < Ndash.pointers.length; i++) {
							if (Ndash.pointers[i] != null) {
								mPos++;
							}
						}
						K tempKm = Ndash.keys[mPos];
						Object m = Ndash.pointers[mPos];
						Ndash.pointers[mPos] = null;
						Ndash.keys[mPos] = null;
						Ndash.numberOfKeys--;
						N.insert(tempKm, m, N.numberOfKeys);
						nParent.keys[k_pos] = nParent.keys[mPos];
					}
				}
			}
		}
	}
   // This function is introduced to get the number of not null pointers in that 
	//specific node
	private int numberOfpointers(Node p) {
		int count = 0;
		for (int i = 0; i < p.pointers.length; i++) {
			if (p.pointers[i] != null)
				count++;
		}
		return count;
	}

}
