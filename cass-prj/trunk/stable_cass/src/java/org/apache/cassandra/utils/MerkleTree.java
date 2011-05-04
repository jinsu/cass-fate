/*
* Licensed to the Apache Software Foundation (ASF) under one
* or more contributor license agreements.  See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership.  The ASF licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License.  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.apache.cassandra.utils;

import java.io.Serializable;
import java.util.*;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.PeekingIterator;

import org.apache.cassandra.dht.*;

/**
 * A MerkleTree implemented as a binary tree.
 *
 * A MerkleTree is a full binary tree that represents a perfect binary tree of
 * depth 'hashdepth'. In a perfect binary tree, each leaf contains a
 * sequentially hashed range, and each inner node contains the binary hash of
 * its two children. In the MerkleTree, many ranges will not be split to the
 * full depth of the perfect binary tree: the leaves of this tree are Leaf objects,
 * which contain the computed values of the nodes that would be below them if
 * the tree were perfect.
 * 
 * The hash values of the inner nodes of the MerkleTree are calculated lazily based
 * on their children when the hash of a range is requested with hash(range).
 *
 * Inputs passed to TreeRange.validate should be calculated using a very secure hash,
 * because all hashing internal to the tree is accomplished using XOR. 
 *
 * If two MerkleTrees have the same hashdepth, they represent a perfect tree
 * of the same depth, and can always be compared, regardless of size or splits.
 */
public class MerkleTree implements Serializable
{
    private static final long serialVersionUID = 2L;

    public static final byte RECOMMENDED_DEPTH = Byte.MAX_VALUE - 1;

    public static final int CONSISTENT = 0;
    public static final int FULLY_INCONSISTENT = 1;
    public static final int PARTIALLY_INCONSISTENT = 2;

    public final byte hashdepth;

    private transient IPartitioner partitioner;

    private long maxsize;
    private long size;
    private Hashable root;

    /**
     * @param partitioner The partitioner in use.
     * @param hashdepth The maximum depth of the tree. 100/(2^depth) is the %
     *        of the key space covered by each subrange of a fully populated tree.
     * @param maxsize The maximum number of subranges in the tree.
     */
    public MerkleTree(IPartitioner partitioner, byte hashdepth, long maxsize)
    {
        assert hashdepth < Byte.MAX_VALUE;
        this.partitioner = partitioner;
        this.hashdepth = hashdepth;
        this.maxsize = maxsize;

        size = 1;
        root = new Leaf(null);
    }

    static byte inc(byte in)
    {
        assert in < Byte.MAX_VALUE;
        return (byte)(in + 1);
    }

    /**
     * Initializes this tree by splitting it until hashdepth is reached,
     * or until an additional level of splits would violate maxsize.
     *
     * NB: Replaces all nodes in the tree.
     */
    public void init()
    {
        // determine the depth to which we can safely split the tree
        byte sizedepth = (byte)(Math.log10(maxsize) / Math.log10(2));
        byte depth = (byte)Math.min(sizedepth, hashdepth);

        Token mintoken = partitioner.getMinimumToken();
        root = initHelper(mintoken, mintoken, (byte)0, depth);
        size = (long)Math.pow(2, depth);
    }

    private Hashable initHelper(Token left, Token right, byte depth, byte max)
    {
        if (depth == max)
            // we've reached the leaves
            return new Leaf();
        Token midpoint = partitioner.midpoint(left, right);
        Hashable lchild = initHelper(left, midpoint, inc(depth), max);
        Hashable rchild = initHelper(midpoint, right, inc(depth), max);
        return new Inner(midpoint, lchild, rchild);
    }

    Hashable root()
    {
        return root;
    }

    public IPartitioner partitioner()
    {
        return partitioner;
    }

    /**
     * The number of distinct ranges contained in this tree. This is a reasonable
     * measure of the memory usage of the tree (assuming 'this.order' is significant).
     */
    public long size()
    {
        return size;
    }

    public long maxsize()
    {
        return maxsize;
    }

    public void maxsize(long maxsize)
    {
        this.maxsize = maxsize;
    }

    /**
     * TODO: Find another way to use the local partitioner after serialization.
     */
    public void partitioner(IPartitioner partitioner)
    {
        this.partitioner = partitioner;
    }

    /**
     * @param ltree First tree.
     * @param rtree Second tree.
     * @return A list of the largest contiguous ranges where the given trees disagree.
     */
    public static List<TreeRange> difference(MerkleTree ltree, MerkleTree rtree)
    {
        List<TreeRange> diff = new ArrayList<TreeRange>();
        Token mintoken = ltree.partitioner.getMinimumToken();
        TreeRange active = new TreeRange(null, mintoken, mintoken, (byte)0, null);
        
        byte[] lhash = ltree.hash(active);
        byte[] rhash = rtree.hash(active);

        if (lhash != null && rhash != null && !Arrays.equals(lhash, rhash))
        {
            if (FULLY_INCONSISTENT == differenceHelper(ltree, rtree, diff, active))
                diff.add(active);
        }
        else if (lhash == null || rhash == null)
            diff.add(active);
        return diff;
    }

    /**
     * TODO: This function could be optimized into a depth first traversal of
     * the two trees in parallel.
     *
     * Takes two trees and a range for which they have hashes, but are inconsistent.
     * @return FULLY_INCONSISTENT if active is inconsistent, PARTIALLY_INCONSISTENT if only a subrange is inconsistent.
     */
    static int differenceHelper(MerkleTree ltree, MerkleTree rtree, List<TreeRange> diff, TreeRange active)
    {
        Token midpoint = ltree.partitioner().midpoint(active.left, active.right);
        TreeRange left = new TreeRange(null, active.left, midpoint, inc(active.depth), null);
        TreeRange right = new TreeRange(null, midpoint, active.right, inc(active.depth), null);
        byte[] lhash;
        byte[] rhash;
        
        // see if we should recurse left
        lhash = ltree.hash(left);
        rhash = rtree.hash(left);
        int ldiff = CONSISTENT;
        boolean lreso = lhash != null && rhash != null;
        if (lreso && !Arrays.equals(lhash, rhash))
            ldiff = differenceHelper(ltree, rtree, diff, left);
        else if (!lreso)
            ldiff = FULLY_INCONSISTENT;


        // see if we should recurse right
        lhash = ltree.hash(right);
        rhash = rtree.hash(right);
        int rdiff = CONSISTENT;
        boolean rreso = lhash != null && rhash != null;
        if (rreso && !Arrays.equals(lhash, rhash))
            rdiff = differenceHelper(ltree, rtree, diff, right);
        else if (!rreso)
            rdiff = FULLY_INCONSISTENT;

        if (ldiff == FULLY_INCONSISTENT && rdiff == FULLY_INCONSISTENT)
        {
            // both children are fully inconsistent
            return FULLY_INCONSISTENT;
        }
        else if (ldiff == FULLY_INCONSISTENT)
        {
            diff.add(left);
            return PARTIALLY_INCONSISTENT;
        }
        else if (rdiff == FULLY_INCONSISTENT)
        {
            diff.add(right);
            return PARTIALLY_INCONSISTENT;
        }
        return PARTIALLY_INCONSISTENT;
    }

    /**
     * For testing purposes.
     * Gets the smallest range containing the token.
     */
    TreeRange get(Token t)
    {
        Token mintoken = partitioner.getMinimumToken();
        return getHelper(root, mintoken, mintoken, (byte)0, t);
    }

    TreeRange getHelper(Hashable hashable, Token pleft, Token pright, byte depth, Token t)
    {
        if (hashable instanceof Leaf)
        {
            // we've reached a hash: wrap it up and deliver it
            return new TreeRange(this, pleft, pright, depth, hashable);
        }
        // else: node.
        
        Inner node = (Inner)hashable;
        if (Range.contains(pleft, node.token, t))
            // left child contains token
            return getHelper(node.lchild, pleft, node.token, inc(depth), t);
        // else: right child contains token
        return getHelper(node.rchild, node.token, pright, inc(depth), t);
    }

    /**
     * Invalidates the ranges containing the given token.
     */
    public void invalidate(Token t)
    {
        invalidateHelper(root, partitioner.getMinimumToken(), t);
    }

    private void invalidateHelper(Hashable hashable, Token pleft, Token t)
    {
        hashable.hash(null);
        if (hashable instanceof Leaf)
            return;
        // else: node.
        
        Inner node = (Inner)hashable;
        if (Range.contains(pleft, node.token, t))
            // left child contains token
            invalidateHelper(node.lchild, pleft, t);
        else
            // right child contains token
            invalidateHelper(node.rchild, node.token, t);
    }

    /**
     * Hash the given range in the tree. The range must have been generated
     * with recursive applications of partitioner.midpoint().
     *
     * NB: Currently does not support wrapping ranges that do not end with
     * partitioner.getMinimumToken().
     *
     * @return Null if any subrange of the range is invalid, or if the exact
     *         range cannot be calculated using this tree.
     */
    public byte[] hash(Range range)
    {
        Token mintoken = partitioner.getMinimumToken();
        try
        {
            return hashHelper(root, new Range(mintoken, mintoken), range);
        }
        catch (StopRecursion e)
        {
            return null;
        }
    }

    /**
     * @throws StopRecursion If no match could be found for the range.
     */
    private byte[] hashHelper(Hashable hashable, Range active, Range range) throws StopRecursion
    {
        if (hashable instanceof Leaf)
        {
            if (!range.contains(active))
                // we are not fully contained in this range!
                throw new StopRecursion.BadRange();
            return hashable.hash();
        }
        // else: node.
        
        Inner node = (Inner)hashable;
        Range leftactive = new Range(active.left, node.token);
        Range rightactive = new Range(node.token, active.right);

        if (range.contains(active))
        {
            // this node is fully contained in the range
            if (node.hash() != null)
                // we had a cached value
                return node.hash();
            // continue recursing to hash our children
            byte[] lhash = hashHelper(node.lchild(), leftactive, range);
            byte[] rhash = hashHelper(node.rchild(), rightactive, range);
            // cache the computed value (even if it is null)
            node.hash(lhash, rhash);
            return node.hash();
        } // else: one of our children contains the range
        
        if (leftactive.contains(range))
            // left child contains/matches the range
            return hashHelper(node.lchild, leftactive, range);
        else if (rightactive.contains(range))
            // right child contains/matches the range
            return hashHelper(node.rchild, rightactive, range);
        else
            throw new StopRecursion.BadRange();
    }

    /**
     * Splits the range containing the given token, if no tree limits would be
     * violated. If the range would be split to a depth below hashdepth, or if
     * the tree already contains maxsize subranges, this operation will fail.
     *
     * @return True if the range was successfully split.
     */
    public boolean split(Token t)
    {
        if (!(size < maxsize))
            return false;

        Token mintoken = partitioner.getMinimumToken();
        try
        {
            root = splitHelper(root, mintoken, mintoken, (byte)0, t);
        }
        catch (StopRecursion.TooDeep e)
        {
            return false;
        }
        return true;
    }

    private Hashable splitHelper(Hashable hashable, Token pleft, Token pright, byte depth, Token t) throws StopRecursion.TooDeep
    {
        if (depth >= hashdepth)
            throw new StopRecursion.TooDeep();

        if (hashable instanceof Leaf)
        {
            // split
            size++;
            Token midpoint = partitioner.midpoint(pleft, pright);
            return new Inner(midpoint, new Leaf(), new Leaf());
        }
        // else: node.

        // recurse on the matching child
        Inner node = (Inner)hashable;
        if (Range.contains(pleft, node.token, t))
            // left child contains token
            node.lchild(splitHelper(node.lchild, pleft, node.token, inc(depth), t));
        else
            // else: right child contains token
            node.rchild(splitHelper(node.rchild, node.token, pright, inc(depth), t));
        return node;
    }

    /**
     * Compacts the smallest subranges evenly split by the given token into a
     * single range.
     *
     * Asserts that the given Token falls between two compactable subranges.
     */
    public void compact(Token t)
    {
        root = compactHelper(root, t);
    }

    private Hashable compactHelper(Hashable hashable, Token t)
    {
        // we reached a Leaf without finding an Inner to compact
        assert !(hashable instanceof Leaf);

        Inner node = (Inner)hashable;
        int comp = t.compareTo(node.token);
        if (comp == 0)
        {
            // this is the node to compact
            assert node.lchild() instanceof Leaf && node.rchild() instanceof Leaf :
                "Can only compact a subrange evenly split by the given token!";

            // hash our children together into a new value to replace ourself
            size--;
            return new Leaf(node.lchild().hash(), node.rchild().hash());
        }
        else if (comp < 0)
            // recurse to the left
            node.lchild(compactHelper(node.lchild(), t));
        else
            // recurse to the right
            node.rchild(compactHelper(node.rchild(), t));
        return node;
    }

    /**
     * Returns a lazy iterator of invalid TreeRanges that need to be filled
     * in order to make the given Range valid.
     *
     * @param range The range to find invalid subranges for.
     */
    public TreeRangeIterator invalids(Range range)
    {
        return new TreeRangeIterator(this, range);
    }

    @Override
    public String toString()
    {
        StringBuilder buff = new StringBuilder();
        buff.append("#<MerkleTree root=");
        root.toString(buff, 8);
        buff.append(">");
        return buff.toString();
    }

    /**
     * The public interface to a range in the tree.
     *
     * NB: A TreeRange should not be returned by a public method unless the
     * parents of the range it represents are already invalidated, since it
     * will allow someone to modify the hash. Alternatively, a TreeRange
     * may be created with a null tree, indicating that it is read only.
     */
    public static class TreeRange extends Range
    {
        public static final long serialVersionUID = 1L;
        private final MerkleTree tree;
        public final byte depth;
        private final Hashable hashable;

        TreeRange(MerkleTree tree, Token left, Token right, byte depth, Hashable hashable)
        {
            super(left, right);
            this.tree = tree;
            this.depth = depth;
            this.hashable = hashable;
        }

        public void hash(byte[] hash)
        {
            assert tree != null : "Not intended for modification!";
            hashable.hash(hash);
        }

        public byte[] hash()
        {
            return hashable.hash();
        }

        /**
         * @param entry Row to mix into the hash for this range.
         */
        public void addHash(RowHash entry)
        {
            assert tree != null : "Not intended for modification!";
            assert hashable instanceof Leaf;

            hashable.addHash(entry.hash);
        }

        public void addAll(Iterator<RowHash> entries)
        {
            while (entries.hasNext())
                addHash(entries.next());
        }

        @Override
        public String toString()
        {
            StringBuilder buff = new StringBuilder("#<TreeRange ");
            buff.append(super.toString()).append(" depth=").append(depth);
            return buff.append(">").toString();
        }
    }

    /**
     * Performs a depth-first, inorder traversal of invalid nodes under the given root
     * and intersecting the given range.
     */
    public static class TreeRangeIterator extends AbstractIterator<TreeRange> implements Iterable<TreeRange>, PeekingIterator<TreeRange>
    {
        // stack of ranges to visit
        private final ArrayDeque<TreeRange> tovisit;
        // interesting range
        private final Range range;
        private final MerkleTree tree;
        
        TreeRangeIterator(MerkleTree tree, Range range)
        {
            Token mintoken = tree.partitioner().getMinimumToken();
            tovisit = new ArrayDeque<TreeRange>();
            tovisit.add(new TreeRange(tree, mintoken, mintoken, (byte)0, tree.root));

            this.tree = tree;
            this.range = range;
        }
        
        /**
         * Find the next TreeRange.
         *
         * @return The next TreeRange.
         */
        @Override
        public TreeRange computeNext()
        {
            while (!tovisit.isEmpty())
            {
                TreeRange active = tovisit.pop();

                if (active.hashable.hash() != null)
                    // skip valid ranges
                    continue;

                if (active.hashable instanceof Leaf)
                    // found a leaf invalid range
                    return active;

                Inner node = (Inner)active.hashable;
                // push intersecting children onto the stack
                TreeRange left = new TreeRange(tree, active.left, node.token, inc(active.depth), node.lchild);
                TreeRange right = new TreeRange(tree, node.token, active.right, inc(active.depth), node.rchild);
                if (right.intersects(range))
                    tovisit.push(right);
                if (left.intersects(range))
                    tovisit.push(left);
                    
            }
            return endOfData();
        }
        
        public Iterator<TreeRange> iterator()
        {
            return this;
        }
    }

    /**
     * An inner node in the MerkleTree. Inners can contain cached hash values, which
     * are the binary hash of their two children.
     */
    static class Inner extends Hashable
    {
        public static final long serialVersionUID = 1L;
        public final Token token;
        private Hashable lchild;
        private Hashable rchild;

        /**
         * Constructs an Inner with the given token and children, and a null hash.
         */
        public Inner(Token token, Hashable lchild, Hashable rchild)
        {
            super(null);
            this.token = token;
            this.lchild = lchild;
            this.rchild = rchild;
        }

        public Hashable lchild()
        {
            return lchild;
        }

        public Hashable rchild()
        {
            return rchild;
        }

        public void lchild(Hashable child)
        {
            lchild = child;
        }

        public void rchild(Hashable child)
        {
            rchild = child;
        }

        /**
         * Recursive toString.
         */
        @Override
        public void toString(StringBuilder buff, int maxdepth)
        {
            buff.append("#<").append(getClass().getSimpleName());
            buff.append(" ").append(token);
            buff.append(" hash=").append(Hashable.toString(hash()));
            buff.append(" children=[");
            if (maxdepth < 1)
            {
                buff.append("#");
            }
            else
            {
                if (lchild == null)
                    buff.append("null");
                else
                    lchild.toString(buff, maxdepth-1);
                buff.append(" ");
                if (rchild == null)
                    buff.append("null");
                else
                    rchild.toString(buff, maxdepth-1);
            }
            buff.append("]>");
        }

        @Override
        public String toString()
        {
            StringBuilder buff = new StringBuilder();
            toString(buff, 1);
            return buff.toString();
        }
    }

    /**
     * A leaf node in the MerkleTree. Because the MerkleTree represents a much
     * larger perfect binary tree of depth hashdepth, a Leaf object contains
     * the value that would be contained in the perfect tree at its position.
     *
     * When rows are added to the MerkleTree using TreeRange.validate(), the
     * tree extending below the Leaf is generated in memory, but only the root
     * is stored in the Leaf.
     */
    static class Leaf extends Hashable
    {
        public static final long serialVersionUID = 1L;
        /**
         * Constructs a null hash.
         */
        public Leaf()
        {
            super(null);
        }

        public Leaf(byte[] hash)
        {
            super(hash);
        }

        public Leaf(byte[] lefthash, byte[] righthash)
        {
            super(Hashable.binaryHash(lefthash, righthash));
        }

        @Override
        public void toString(StringBuilder buff, int maxdepth)
        {
            buff.append(toString());
        }
        
        @Override
        public String toString()
        {
            return "#<Leaf " + Hashable.toString(hash()) + ">";
        }
    }

    /**
     * Hash value representing a row, to be used to pass hashes to the MerkleTree.
     * The byte[] hash value should contain a digest of the key and value of the row
     * created using a very strong hash function.
     */
    public static class RowHash
    {
        public final Token token;
        public final byte[] hash;
        public RowHash(Token token, byte[] hash)
        {
            this.token = token;
            this.hash = hash;
        }
        
        @Override
        public String toString()
        {
            return "#<RowHash " + token + " " + Hashable.toString(hash) + ">";
        }
    }

    /**
     * Abstract class containing hashing logic, and containing a single hash field.
     */
    static abstract class Hashable implements Serializable
    {
        private static final long serialVersionUID = 1L;

        protected byte[] hash;

        protected Hashable(byte[] hash)
        {
            this.hash = hash;
        }

        public byte[] hash()
        {
            return hash;
        }

        void hash(byte[] hash)
        {
            this.hash = hash;
        }

        /**
         * Sets the value of this hash to binaryHash of its children.
         * @param lefthash Hash of left child.
         * @param righthash Hash of right child.
         */
        void hash(byte[] lefthash, byte[] righthash)
        {
            hash = binaryHash(lefthash, righthash);
        }

        /**
         * Mixes the given value into our hash. If our hash is null,
         * our hash will become the given value.
         */
        void addHash(byte[] righthash)
        {
            if (hash == null)
                hash = righthash;
            else
                hash = binaryHash(hash, righthash);
        }

        /**
         * The primitive with which all hashing should be accomplished: hashes
         * a left and right value together.
         */
        static byte[] binaryHash(final byte[] left, final byte[] right)
        {
            return FBUtilities.xor(left, right);
        }

        public abstract void toString(StringBuilder buff, int maxdepth);
        
        public static String toString(byte[] hash)
        {
            if (hash == null)
                return "null";
            return "[" + FBUtilities.bytesToHex(hash) + "]";
        }
    }

    /**
     * Exceptions that stop recursion early when we are sure that no answer
     * can be found.
     */
    static abstract class StopRecursion extends Exception
    {
        static class BadRange extends StopRecursion
        {
            public BadRange(){ super(); }
        }

        static class InvalidHash extends StopRecursion
        {
            public InvalidHash(){ super(); }
        }

        static class TooDeep extends StopRecursion
        {
            public TooDeep(){ super(); }
        }
    }
}
