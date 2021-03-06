package com.phasmid.laScala.tree

import com.phasmid.laScala._
import com.phasmid.laScala.fp.FP
import com.phasmid.laScala.fp.FP._

import scala.collection.mutable
import scala.language.implicitConversions

/**
  * Trait which models the tree-like aspects of a tree
  *
  * @param vo the (implicit) ValueOps
  * @tparam K the Key type
  * @tparam V the underlying type of the tree/node values
  */
abstract class KVTree[K, +V]()(implicit vo: ValueOps[K, V]) extends Branch[V] with StructuralTree[K, V] {

  /**
    * Method to determine if this Node's value is like node n's value WITHOUT any recursion.
    * NOTE: This implementation uses the key for comparison, not the value.
    *
    * @param n the node to be compared
    * @tparam B the underlying type of n
    * @return true if this is "like" n
    */
  override def compareValues[B >: V](n: Node[B]): Maybe = {
    val xo = get map (v => vo.getKeyFromValue(v))
    val yo = n.get map (v => vo.asInstanceOf[ValueOps[K, B]].getKeyFromValue(v)) // CHECK
    Kleenean(map2(xo, yo)(_ == _))
  }

  def asTree[W >: V](n: Node[W])(implicit treeBuilder: TreeBuilder[W]): KVTree[K, W] = n match {
    case t: KVTree[K, W] => t
    // TODO refactor the following to avoid instanceOf
    case _ => treeBuilder.buildTree(n.get, Seq()).asInstanceOf[KVTree[K, W]]
  }

}

/**
  * A general branch of a KV-tree, where there is a value at the node itself and the number of children is unbounded.
  * Parallel to GeneralTree
  *
  * @param value    the value of the branch
  * @param children the children of this Node
  * @tparam V the underlying value type of this GeneralTree
  */
case class GeneralKVTree[K, V](value: Option[V], children: List[Node[V]])(implicit vo: ValueOps[K, V]) extends KVTree[K, V] {
// XXX: works only with Scala 2.11
  // abstract class AbstractGeneralKVTree[K, V](value: Option[V], children: List[Node[V]])(implicit vo: ValueOps[K, V]) extends KVTree[K, V] {
  /**
    * @return (optional) value
    */
  def get: Option[V] = value
}

// XXX: works only with Scala 2.11
///**
//  * A general branch of a KV-tree, where there is a value at the node itself and the number of children is unbounded.
//  * Parallel to GeneralTree
//  *
//  * @param value    the value of the branch
//  * @param children the children of this Node
//  * @tparam V the underlying value type of this GeneralTree
//  */
// case class GeneralKVTree[K, V](value: Option[V], children: List[Node[V]])(implicit vo: ValueOps[K, V]) extends AbstractGeneralKVTree(value, children)(vo)

/**
  * A general branch of a KV-tree, where there is a value at the node itself and the number of children is unbounded.
  * Parallel to GeneralTree
  *
  * @param value    the value of the branch
  * @param children the children of this Node
  * @tparam V the underlying value type of this GeneralTree
  */
case class GeneralKVTreeWithScaffolding[K, V](value: Option[V], children: List[Node[V]])(implicit vo: ValueOps[K, V], m: mutable.HashMap[K, Node[V]]) extends KVTree[K, V] {
  def get: Option[V] = value
//case class GeneralKVTreeWithScaffolding[K, V](value: Option[V], children: List[Node[V]])(implicit vo: ValueOps[K, V], m: mutable.HashMap[K, Node[V]]) extends AbstractGeneralKVTree(value, children) {

  memoizeNode(this.asInstanceOf[Node[V]])

  /**
    * CHECK these casts which should generally be illegal as we are downcasting from type to sub-type
    *
    * @param k  the (structural) key for the desired node
    * @param vo (implicit) ValueOps
    * @tparam B the value type
    * @return optionally the node found
    */
  override def findByParentKey[B >: V](k: K)(implicit vo: ValueOps[K, B]): Option[Node[B]] = m.get(k.asInstanceOf[K])

  override protected[tree] def addNode[B >: V : TreeBuilder](node: Node[B])(implicit bKv: ValueOps[K, B]): StructuralTree[K, B] = {
    memoizeNode(node.asInstanceOf[Node[V]])
    super.addNode(node)
  }

  private def memoizeNode(n: Node[V]): Unit = {for (v <- n.get; k = vo.getKeyAsParent(v)) m.put(k, n)}
}

object KVTree

/**
  * Base class implementing TreeBuilder for GeneralKVTree
  *
  * @param vo implicit ValueOps
  * @tparam K key type
  * @tparam V value type
  */
abstract class GeneralKVTreeBuilder[K, V](implicit vo: ValueOps[K, V]) extends TreeBuilder[V] {

  implicit object NodeOrdering extends Ordering[Node[V]] {
    def compare(x: Node[V], y: Node[V]): Int = FP.map2(x.get, y.get)(implicitly[Ordering[V]].compare).get
  }

  /**
    * This method determines if the two given nodes are structurally the same
    *
    * @param x node x
    * @param y node y
    * @return true if they are the same
    */
  def nodesAlike(x: Node[V], y: Node[V]): Boolean = x match {
    case b@Branch(_, _) => (b like y).toBoolean(false)
    case AbstractLeaf(a) => FP.contains(y.get, a)
    case _ => x == y
  }

  /**
    * Get a node from an existing tree to which a new node with value a can be attached
    *
    * @param tree the tree whence we want to find the (potential) parent of a new node with value a
    * @param a    the value of the new node
    * @return the Node to which the new node will be attached (if such a node exists). Note that this might be a leaf
    */
  def getParent(tree: Tree[V], a: V): Option[Node[V]] =
    tree match {
      case x: StructuralTree[K, V] =>
        // XXX: the following is somewhat ugly but it is necessary to explicitly pass the vo parameter
        for (k <- vo.getParentKey(a); n <- x.findByParentKey(k)(vo)) yield n
      case _ => None
    }

  /**
    * Build a new tree, given a value and child nodes
    *
    * @param maybeValue the (optional) value which the new tree will have at its root
    * @param children   the the children of the node
    * @return a tree the (optional) value at the root and children as the immediate descendants
    */
  def buildTree(maybeValue: Option[V], children: Seq[Node[V]]): Tree[V] = GeneralKVTree(maybeValue, children.toList.sorted)

  /**
    * Build a new leaf for a GeneralKVTree
    *
    * @param a the value for the leaf
    * @return a node which is a leaf node
    */
  def buildLeaf(a: V): Node[V] = Leaf(a)
}

object GeneralKVTree

abstract class GeneralKVTreeBuilderWithScaffolding[K, V](implicit vo: ValueOps[K, V]) extends GeneralKVTreeBuilder[K, V] {
  implicit val scaffolding: mutable.HashMap[K, Node[V]] = mutable.HashMap[K, Node[V]]()

  override def buildTree(maybeValue: Option[V], children: Seq[Node[V]]) = GeneralKVTreeWithScaffolding(maybeValue, children.toList.sorted)
}

