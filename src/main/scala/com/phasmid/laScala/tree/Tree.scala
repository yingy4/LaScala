/*
 * LaScala
 * Copyright (c) 2016, 2017. Phasmid Software
 */

package com.phasmid.laScala.tree

import com.phasmid.laScala._
import com.phasmid.laScala.fp.FP
import com.phasmid.laScala.fp.FP._
import com.phasmid.laScala.tree.AbstractBinaryTree.isOverlap
import org.slf4j.{Logger, LoggerFactory}

import scala.annotation.tailrec
import scala.language.{implicitConversions, postfixOps}

/**
  * Trait which models the tree-like aspects of a tree
  *
  * @tparam A the underlying type of the tree/node
  */
sealed trait Tree[+A] extends Node[A] {

  /**
    * @return the immediate descendants (children) of this branch
    */
  def children: List[Node[A]]

  /**
    * Calculate the size of this node's subtree
    *
    * @return the size of the subtree
    */
  def size: Int = Parent.traverse[Node[A], Int, Int](_.get match {
    case Some(_) => 1
    case _ => 0
  }, _ + _, { _ => false })(List(this), 0)

  /**
    * NOTE: that this is NOT tail-recursive
    *
    * NOTE: I don't think we can use Parent.traverse for depth. Could be wrong.
    *
    * @return the depth of the subtree
    */
  // NOTE this is a bit more complex than I'd like because max doesn't operate on an empty collection
  def depth: Int = (children.map(_.depth + 1) :+ 1).max

  /**
    * Method to add the given node to this node.
    *
    * @param node the node to add
    * @tparam B the underlying type of the new node (and the resulting tree)
    * @return the resulting tree
    */
  def +[B >: A : TreeBuilder](node: Node[B]): Tree[B] = TreeBuilder[B].buildTree(get, children :+ node)

  /**
    * Method to add the given node to this node
    *
    * @param b the value to add, as a Leaf
    * @tparam B the underlying type of the new node (and the resulting tree)
    * @return the resulting tree
    */
  def +[B >: A : TreeBuilder](b: B): Tree[B] = this + implicitly[TreeBuilder[B]].buildLeaf(b)

  /**
    * Iterate on the values of this tree
    *
    * @param depthFirst if true then we iterate depth-first, else breadth-first (defaults to true)
    * @return an iterator of values
    */
  def iterator(depthFirst: Boolean = true): Iterator[A] = for (n <- nodeIterator(depthFirst); a <- n.get) yield a

  /**
    * Create a String which represents the value of this Node and the values of its immediate descendants
    *
    * @return an appropriate String
    */
  def summary: String = {
    val r = new StringBuilder(s""""$get: """")
    val l = for (c <- children; x <- c.get.orElse(Some(Empty))) yield x.toString
    r.append(l mkString ", ")
    r.toString
  }

  /**
    * Method to determine if this subtree is "like" the given node (without recursion)
    *
    * @param node the node to be searched for
    * @tparam B the underlying type of n
    * @return true if node n is found in the sub-tree defined by this
    */
  def like[B >: A](node: Node[B]): Maybe = node match {
    case Branch(_, ns) =>
      def cf(t: (Node[B], Node[B])): Maybe = t._1.compareValues(t._2)

      (children zip ns).foldLeft(compareValues(node))(_ :&& cf(_))
    case _ => Kleenean(false)
  }

  /**
    * Method to determine if this Tree includes the given node
    *
    * @param node the node to be searched for
    * @tparam B the underlying type of n
    * @return true if node n is found in the sub-tree defined by this
    */
  def includes[B >: A](node: Node[B]): Boolean = (this compareValues node) () match {
    case Some(true) => true
    case _ => children.foldLeft[Boolean](false)((b, n) => if (b) b else n includes node)
  }

  //    val f: Node[B] => Maybe = { n => n compareValues node }
  //    val g: (Boolean, Maybe) => Boolean = { (b, m) => (b |: m) ().getOrElse(false) }
  //    Parent.traverse[Node[B], Maybe, Boolean](f, g, { x => x })(List(this), false)


  /**
    * Determine if this subtree includes a value equal to b
    *
    * CONSIDER renaming this back to just includes (as it was previously)
    *
    * @param b the value to be searched
    * @tparam B the type of b
    * @return true if value b is found in the sub-tree defined by this
    */
  def includesValue[B >: A](b: B): Boolean = get match {
    case Some(`b`) => true
    case _ => children.foldLeft[Boolean](false)((x, n) => if (x) x else n.includesValue(b))
  }

  //    Parent.traverse[Node[B], Boolean, Boolean]({ n => n.get match {
  //    case Some(`b`) => true
  //    case _ => false
  //  }
  //  }, _ | _, { x => x })(List(this), false)

  /**
    * Determine if this tree has a subtree matching b which includes c
    *
    * @param subtree the value of the subtree to be searched
    * @param element the value of the element to be found in the subtree
    * @tparam B the type of subtree and element
    * @return true if value element is found in the sub-tree defined by subtree
    */
  def includes[B >: A](subtree: B, element: B): Boolean = find(subtree) match {
    case Some(s: Node[B]) => s.includesValue(element)
    case _ => false
  }

  /**
    * @param b value to be found, looking at the value of each node in turn (depth first)
    * @tparam B the type of b
    * @return Some(node) where node is the first node to be found with value b
    */
  def find[B >: A](b: B): Option[Node[B]] = find({ n: Node[B] => FP.contains(n.get, b) })

  /**
    *
    * @param p the predicate to be applied to the value of each node in turn (depth first)
    * @tparam B the type of b
    * @return Some(node) where node is the first node to be found that satisfies the predicate p
    */
  def find[B >: A](p: Node[B] => Boolean): Option[Node[B]] = {
    def f(n: Node[B]): Option[Node[B]] = toOption(p)(n)

    def g(bno: Option[Node[B]], t: Option[Node[B]]): Option[Node[B]] = bno orElse t

    def q(bno: Option[Node[B]]): Boolean = bno.isDefined

    Parent.traverse[Node[B], Option[Node[B]], Option[Node[B]]](f, g, q)(List(this), None)
  }

  def filter[B >: A](p: Node[B] => Boolean): Iterator[Node[B]] = nodeIterator().filter(p)


  //  /**
  //    * This was designed for BinaryTree or GeneralTree. Needs testing in general.
  //    * It doesn't work properly at present.
  //    * Alternatively, can invoke nodeIterator and render each node.
  //    * @param z a function to combine a Node with a sequence of Nodes
  //    * @tparam B the underlying type of the Nodes
  //    * @return a String
  //    */
  //  def renderRecursive[B >: A](z: (Seq[Node[B]], Node[B]) => Seq[Node[B]]): String =
  //    Recursion.recurse[Node[B], String, String]({
  //      case Empty => ""
  //      case Leaf(x) => x.toString
  //      case c@Open => c.render
  //      case c@Close => c.render
  //      case n => ""
  //    }, _ + _, z)(Seq(this), "")
  //}

}

/**
  * Trait which models the node of a tree
  *
  * @tparam A the underlying type of the tree/node
  */
sealed trait Node[+A] extends Renderable {

  /**
    * @return the value of this node, if any
    */
  def get: Option[A]

  /**
    * Iterate on the nodes which are the descendants of this node (including this node at start or end of result)
    *
    * @param depthFirst if true then we iterate depth-first, else breadth-first (defaults to true)
    * @return an iterator of nodes
    */
  def nodeIterator(depthFirst: Boolean = true): Iterator[Node[A]]

  /**
    * Method to determine if this Node's value is equal to node n's value WITHOUT any recursion.
    *
    * @param n the node to be compared
    * @tparam B the underlying type of n
    * @return true if this is "like" n
    */
  def compareValues[B >: A](n: Node[B]): Maybe = Kleenean(map2(get, n.get)(_ == _))

  /**
    * Method to determine if this Node is a leaf or a branch node
    *
    * @return true if this Node is a leaf
    */
  def isLeaf: Boolean = this match {
    case AbstractLeaf(_) => true
    case Empty => true
    case Branch(_, _) => false
    case _ => Tree.logger.info(s"isLeaf fell through with $this"); false
  }

  /**
    * Calculate the size of this node's subtree
    *
    * @return the size of the subtree
    */
  def size: Int

  /**
    * Method to determine the depth of the tree headed by this Node.
    * Ideally, we would put this method into Tree, but because of the specific (non-tail-recursive)
    * implementation, it is actually more logical to declare it here.
    *
    * @return the maximum depth of the tree headed by this Node
    */
  def depth: Int

  /**
    * Method to determine if this Node includes the given node in its subtree (if any)
    *
    * @param node the node to be searched for
    * @tparam B the underlying type of n
    * @return true if node n is found in the sub-tree defined by this
    */
  def includes[B >: A](node: Node[B]): Boolean

  /**
    * Determine if this subtree includes a value equal to b
    *
    * CONSIDER renaming this as includes
    *
    * @param b the value to be searched
    * @tparam B the type of b
    * @return true if value b is found in the sub-tree defined by this
    */
  def includesValue[B >: A](b: B): Boolean

  /**
    * Method to replace a particular node in this tree with another node, but otherwise return a copy of this tree.
    *
    * CONSIDER moving this into Tree since it isn't really a Node method
    *
    * @param x the node to be replaced with y
    * @param y the node to replace x
    * @param f function to determine if two nodes are the same
    * @tparam B the underlying type of the new node (and the resulting tree)
    * @return the resulting tree
    */
  def replaceNode[B >: A : TreeBuilder](x: Node[B], y: Node[B])(f: (Node[B], Node[B]) => Boolean): Tree[B] = {
    def inner(t: Node[B], x: Node[B], y: Node[B])(f: (Node[B], Node[B]) => Boolean): Node[B] = {
      if (f(t, x)) // t and x are the same so simply return y
        y match {
          case Leaf(z) => throw TreeException(s"cannot replace node $x by Leaf($z)")
          case Empty => throw TreeException(s"cannot replace node $x by Empty")
          case _ => y.asInstanceOf[Tree[B]] // CHECK this is OK
        }
      else
        t match {
          // CHECK that t is OK as buildTree previously used get as first parameter
          case Branch(a, ns) => TreeBuilder[B].buildTree(a, replaceChildrenNodes(x, y)(f)(ns))
          case _ => t
        }
    }

    def replaceChildrenNodes(x: Node[B], y: Node[B])(f: (Node[B], Node[B]) => Boolean)(ns: NodeSeq[B]) =
      for (n <- ns) yield inner(n, x, y)(f)

    inner(this, x, y)(f).asInstanceOf[Tree[B]] // CHECK this is OK
  }

  /**
    * Method to render this node
    *
    * @param indent the number of "tabs" before output should start (when writing on a new line).
    * @param tab    an implicit function to translate the tab number (i.e. indent) to a String of white space.
    *               Typically (and by default) this will be uniform. But you're free to set up a series of tabs
    *               like on an old typewriter where the spacing is non-uniform.
    * @return a String that, if it has embedded newlines, will follow each newline with (possibly empty) white space,
    *         which is then followed by some human-legible rendition of *this*.
    */
  def render(indent: Int)(implicit tab: (Int) => Prefix): String = //get map (_.toString) getOrElse("")
    get match {
      case Some(a) => a match {
        case r: Renderable => r.render(indent)
        case _ => a.toString
      }
      case None => ""
    }
}

/**
  * Defines a Node in a tree where the value of any node determines its place within
  * the tree.
  *
  * CONSIDER do we really need A to be covariant?
  *
  * @tparam K the key type
  * @tparam A the underlying type of the tree/node
  */
sealed trait StructuralNode[K, +A] extends Node[A] {
  /**
    * Method to add the given node to this, which may be a leaf, a branch or the root of an entire tree
    *
    * @param node a node
    * @param vo   the (implicit) ValueOps that is used to determine the proper tree position of this node
    * @tparam B the underlying type of the node to be added (and the tree to be returned)
    * @return a tree which is a modified form of this
    */
  protected[tree] def addNode[B >: A : TreeBuilder](node: Node[B])(implicit vo: ValueOps[K, B]): Tree[B]

  /**
    * Method to get the key for this StructuralNode
    *
    * @param vo the (implicit) ValueOps that is used to determine the proper tree position of this node
    * @tparam B the underlying type of the node to be added (and the tree to be returned)
    * @return the key based on value rather than tree position
    */
  def key[B >: A](implicit vo: ValueOps[K, B]): Option[K] = get map (vo.getKeyFromValue(_))
}

/**
  * Trait which models an index that is useful for building an indexed tree, especially an MPTT-type index.
  */
sealed trait TreeIndex {
  /**
    * the left index (the number of other nodes to the "left" of this one
    *
    * @return
    */
  def lIndex: Option[Long]

  /**
    * the right index (the number of other nodes to the "right" of this one
    *
    * @return
    */
  def rIndex: Option[Long]
}

/**
  * A branch of a tree
  *
  * @tparam A the underlying type of this Branch
  */
trait Branch[+A] extends Tree[A] {
  /**
    * Iterate on the nodes of this branch
    *
    * NOTE: this method is not currently implemented in a tail-recursive manner.
    *
    * @param depthFirst if true then we iterate depth-first, else breadth-first
    * @return an iterator of nodes
    */
  def nodeIterator(depthFirst: Boolean): Iterator[Node[A]] = {
    val z = for (n1 <- children; n2 <- n1.nodeIterator(depthFirst)) yield n2
    val r = if (depthFirst) z :+ this
    else this +: z
    r.toIterator
  }


  /**
    * Create a String which represents this Node and its subtree
    *
    * XXX not tail-recursive.
    * CONSIDER make this tail-recursive, preferably by using traverse... (see renderRecursively in comments)
    *
    * @param indent the number of tabs before output should start on a new line.
    * @param tab    an implicit function to translate the tab number (i.e. indent) to a String of white space.
    * @return a String.
    */
  override def render(indent: Int = 0)(implicit tab: (Int) => Prefix): String = {
    val result = new StringBuilder(super.render(indent) + "--")
    import Renderable.renderableTraversable
    result.append(children.render(indent))
    result.toString
  }
}

/**
  * Trait which combines the Node and TreeIndex traits
  *
  * @tparam A the underlying type of the tree/node
  */
trait IndexedNode[A] extends Node[A] with TreeIndex

/**
  * Trait which combines the IndexedNode and Tree traits
  *
  * @tparam A the underlying type of this Branch
  */
trait IndexedTree[A] extends IndexedNode[A] with Tree[A]

/**
  * Trait which models the tree-like aspects of a structural tree.
  * This kind of tree is distinguishable from an ordinary Tree by
  * the fact that the methods take an implicit parameter of type ValueOps.
  * This parameter allows for the values themselves to imply a position within the tree,
  * in other words a tree which could be faithfully rebuilt from itself.
  *
  * CONSIDER requiring that the ValueOps parameter be a property of the StructuralTree itself.
  *
  * @tparam K the key type
  * @tparam A the underlying type of the tree/node
  */
trait StructuralTree[K, +A] extends Tree[A] with StructuralNode[K, A] {

  /**
    * Method to add a value to this tree: because the addition of values is not order-dependent this method simply invokes :+
    *
    * NOTE the implementation of this method assumes that the order of the operands is not important.
    *
    * @param b the value to add
    * @tparam B the underlying type of the new node (and the resulting tree)
    * @return the resulting tree
    */
  def +:[B >: A : TreeBuilder](b: B)(implicit vo: ValueOps[K, B]): StructuralTree[K, B] = :+(b)

  /**
    * Method to add a value to this tree by simply creating a leaf and calling :+(Node[B])
    *
    * @param b the value to add
    * @tparam B the underlying type of the new node (and the resulting tree)
    * @return the resulting tree
    */
  def :+[B >: A : TreeBuilder](b: B)(implicit vo: ValueOps[K, B]): StructuralTree[K, B] = :+(TreeBuilder[B].buildLeaf(b))

  /**
    * Method to add a node to this tree: because the addition of nodes is not order-dependent this method simply invokes :+
    *
    * NOTE the implementation of this method assumes that the order of the operands is not important.
    *
    * @param node the node to add
    * @tparam B the underlying type of the new node (and the resulting tree)
    * @return the resulting tree
    */
  def +:[B >: A : TreeBuilder](node: Node[B])(implicit vo: ValueOps[K, B]): StructuralTree[K, B] = this :+ node

  /**
    * Method to add a node to this tree
    *
    * @param node the node to add
    * @tparam B the underlying type of the new node (and the resulting tree)
    * @return the resulting tree
    */
  def :+[B >: A : TreeBuilder](node: Node[B])(implicit vo: ValueOps[K, B]): StructuralTree[K, B] = addNode(node)

  /**
    * Non tail-recursive method to find a Node in a tree by its key
    *
    * @param k  the key
    * @param vo (implicit) ValueOps
    * @tparam B the value type
    * @return optionally the node found
    */
  def findByKey[B >: A](k: K)(implicit vo: ValueOps[K, B]): Option[Node[B]] =
    find { n =>
      n.get match {
        case Some(v) => k == vo.getKeyFromValue(v)
        case None => false
      }
    }

  /**
    * CONSIDER making this tail-recursive, given that the parent key is structure-based
    *
    * Method to find a Node in a tree by its structural key.
    * NOTE: this method is not tail-recursive.
    *
    * FIXME: this must be made much more efficient because building an N-node tree is currently an O(N2) problem
    * ... we can reduce that to N log N
    *
    * @param k  the (structural) key for the desired node
    * @param vo (implicit) ValueOps
    * @tparam B the value type
    * @return optionally the node found
    */
  def findByParentKey[B >: A](k: K)(implicit vo: ValueOps[K, B]): Option[Node[B]] =
    find { n =>
      n.get match {
        case Some(v) => k == vo.getKeyAsParent(v)
        case None => false
      }
    }

  /**
    * Method to add the given node to this, which may be a leaf, a branch or the root of an entire tree.
    * Unusually, I will make comments in this code because it is critical and difficult to follow.
    *
    * @param node a node
    * @tparam B the underlying type of the node to be added (and the tree to be returned)
    * @return a tree which is a modified form of this
    */
  protected[tree] def addNode[B >: A : TreeBuilder](node: Node[B])(implicit bKv: ValueOps[K, B]): StructuralTree[K, B] = node.get match {
    case Some(b) =>
      implicit val spyLogger: Logger = null // Spy.getLogger(getClass)
      // b is the content of the node to be added to this Tree
      val tb = TreeBuilder[B]

      tb.getParent(this, b) match {
        case Some(parent) =>
          // parent is the Node which will be replaced by the joining of itself with the input node
          // NOTE: we should always get a match when this is a binary tree
          replaceNode(parent, addNodes(parent, node))(tb.nodesAlike).asInstanceOf[StructuralTree[K, B]]
        case None =>
          // this is the situation where a parent node for the value b cannot be found in the tree.
          // in such a case, we must infer the properties of the parent so that it can be added to the tree, with node as its child.
          val bo = for (k <- bKv.getParentKey(b); _b <- bKv.createValueFromKey(k, node.get)) yield _b
          bo match {
            case Some(_) =>
              // In this case, we successfully inferred a value for the parent, and now we recursively call addNode with
              // the new node (based on bo) having, as its child, node.
              if (node.get != bo)
              // We are not going to recurse infinitely
                addNode(tb.buildTree(bo, Seq(node)))
              else // NOTE if we get here the fault most probably lies with the logic in bKv or possibly tb or in the actual data
                throw TreeException(s"logic error: recursion in addNode for $b")
            case None =>
              // we were unsuccessful in inferring a value, so we give up and simply add node to the root of this tree
              Tree.logger.info(s"cannot get value for new parent of node so we add said node at the root of this tree: node value = ${node.get}")
              replaceNode(this, addNodes(this, node))((_, _) => true).asInstanceOf[StructuralTree[K, B]]
          }
        case p => throw TreeException(s"Parent $p is not a StructuralNode")
      }
    // we cannot use this method to add a node which has no content
    case None => throw TreeException("cannot add node without value")
  }

  private def addNodes[B >: A : TreeBuilder](x: Node[B], y: Node[B]) = x match {
    case l: Leaf[B] => TreeBuilder[B].buildTree(l.get, Nil) + y
    case n: Tree[B] => n + y
    case z => throw TreeException(s"cannot add to parent $z")
  }

}

/**
  * Trait to define a method for building a tree from a Node.
  * CONSIDER making this trait covariant in A
  *
  * @tparam A the underlying type of the Node(s) and Tree
  */
trait TreeBuilder[A] {
  /**
    * Build a new tree, given a value and child nodes
    *
    * @param maybeValue the (optional) value which the new tree will have at its root
    * @param children   the the children of the node
    * @return a tree the (optional) value at the root and children as the immediate descendants
    */
  def buildTree(maybeValue: Option[A], children: Seq[Node[A]]): Tree[A]

  /**
    * Build a new leaf, given a value
    *
    * @param a the value for the leaf
    * @return a node which is a leaf node
    */
  def buildLeaf(a: A): Node[A]

  /**
    * Get a node from an existing tree to which a new node with value a can be attached
    *
    * @param tree the tree whence we want to find the (potential) parent of a new node with value a
    * @param a    the value of the new node
    * @return the Node to which the new node will be attached (if such a node exists). Note that this might be a leaf
    */
  def getParent(tree: Tree[A], a: A): Option[Node[A]]

  /**
    * This method determines if the two given nodes are structurally the same
    *
    * @param x node x
    * @param y node y
    * @return true if they are the same
    */
  def nodesAlike(x: Node[A], y: Node[A]): Boolean
}

/**
  * This trait is implemented by a type class which pertains to the underlying VALUE type of a tree.
  * It does not pertain to the shape of the tree.
  *
  * @tparam K the key type
  * @tparam V the value type
  */
trait ValueOps[K, V] extends Ordering[V] {
  /**
    * Extract the key from a value v (ignoring its actual position within the tree).
    * This method is used strictly for lookup purposes,
    * that's to say it is not used during the build-tree process for finding
    * an appropriate parent for an additional node.
    * If a tree is to be indexed, then this key value must be unique throughout the tree,
    * otherwise confusion will arise -- just like in HashMap, for instance.
    *
    * @param v the value whose key we require
    * @return the key
    */
  def getKeyFromValue(v: V): K

  /**
    * Extract a key from a value v but from a tree-structural point of view.
    * I.e. the value returned should match the parent key of any children of this node.
    * This key is used in the tree-building process, in particular, it is used solely (and indirectly) by the addNode method
    * to find an appropriate node to which an additional node should be attached.
    * It is used for no other purpose whatsoever.
    *
    * @param v the value whose key we require
    * @return the key
    */
  def getKeyAsParent(v: V): K

  /**
    * If such a thing is defined for this type of value, get the parent key
    *
    * @param v the value whose parent key we desire
    * @return an (optional) parent key
    */
  def getParentKey(v: V): Option[K]

  /**
    * In the event that a parent cannot be found, we sometimes have to create a new node.
    * This requires us to find a value for the new node
    *
    * @param k  the key for the parent we must create
    * @param vo an optional value from which the result may be inferred (as the parent), if possible.
    * @return a value for the parent node, wrapped in Option
    */
  def createValueFromKey(k: K, vo: => Option[V]): Option[V]
}

trait StringValueOps[V] extends ValueOps[String, V] {
  def getKeyAsParent(v: V): String = v.toString

  def getKeyFromValue(v: V): String = v.toString

  def getParentKey(v: V): Option[String]

  def createValueFromKey(k: String, vo: => Option[V]): Option[V]

  def compare(x: V, y: V): Int = getKeyFromValue(x).compare(getKeyFromValue(y))
}

/**
  * Trait implementing TreeBuilder for GeneralTree
  *
  * @tparam A the underlying type of the Node(s) and Tree
  */
trait GeneralTreeBuilder[A] extends TreeBuilder[A] {
  def buildTree(maybeValue: Option[A], children: Seq[Node[A]]): Tree[A] = GeneralTree(maybeValue.get, children.toList)

  def buildLeaf(a: A): Node[A] = Leaf(a)

  def nodesAlike(x: Node[A], y: Node[A]): Boolean = x match {
    case b@Branch(_, _) => (b like y).toBoolean(false)
    case AbstractLeaf(a) => FP.contains(y.get, a)
    case _ => x == y
  }
}

/**
  * A general branch of a tree, where there is a value at the node itself and the number of children is unbounded
  *
  * @param value    the value of the branch
  * @param children the children of this Node
  * @tparam A the underlying type of this Branch
  */
case class GeneralTree[+A](value: A, children: List[Node[A]]) extends Branch[A] {
  /**
    * @return Some(value)
    */
  def get = Some(value)
}

/**
  * Case class for a leaf whose value is a
  *
  * @param a the value of this leaf
  * @tparam A the underlying type of this Leaf
  */
case class Leaf[+A](a: A) extends AbstractLeaf[A](a) {
  /**
    * Method to add the given node to this node specifically
    *
    * CONSIDER this looks incorrect because it defines its own K
    *
    * @param node the node to add
    * @tparam B the underlying type of the new node (and the resulting tree)
    * @return the resulting tree
    */
  def +[K, B >: A : TreeBuilder](node: Node[B]): Tree[B] = TreeBuilder[B].buildTree(get, Seq(node))
}

/**
  * Case class for a binary tree where the nodes themselves do not have values
  *
  * @param left  the left-side node
  * @param right the right-side node (assumed to come after the left side node)
  * @tparam A the underlying type which must be Orderable.
  */
case class UnvaluedBinaryTree[+A: Ordering](left: Node[A], right: Node[A]) extends AbstractBinaryTree[A](left, right) {

  /**
    * @return None
    */
  def get = None
}

/**
  * Case class for a binary tree with a value at each node
  *
  * @param a     the value of the root node of this binary tree
  * @param left  the left-side node
  * @param right the right-side node (assumed to come after the left side node)
  * @tparam A the underlying type which must be Orderable.
  */
case class BinaryTree[+A: Ordering](a: A, left: Node[A], right: Node[A]) extends AbstractBinaryTree[A](left, right) {

  /**
    * @return None
    */
  // TODO this looks wrong!
  def get = None

}

/**
  * Case class for an indexed leaf
  *
  * @param lIndex the (optional) index to the left of the leaf
  * @param rIndex the (optional) index to the right of the leaf
  * @param value  the leaf value
  * @tparam A the underlying type of this Leaf
  */
case class IndexedLeaf[A](lIndex: Option[Long], rIndex: Option[Long], value: A) extends AbstractLeaf[A](value) with TreeIndex with IndexedNode[A] {
  override def depth: Int = 1

  /**
    * Create a String which represents this Node and its subtree
    *
    * @param indent the number of tabs before output should start on a new line.
    * @param tab    an implicit function to translate the tab number (i.e. indent) to a String of white space.
    * @return a String.
    */
  override def render(indent: Int)(implicit tab: (Int) => Prefix): String = value match {
    case renderable: Renderable => renderable.render(indent)
    case _ => s"""${tab(indent)}$value [$lIndex:$rIndex]"""
  }

  override def toString: String = s"""L("$value")""" + map2(lIndex, rIndex)(_ + ":" + _).getOrElse("")

  /**
    * Method to add the given node to this node specifically
    *
    * @param node the node to add
    * @tparam B the underlying type of the new node (and the resulting tree)
    * @return the resulting tree
    */
  //noinspection NotImplementedCode
  def +[K, B >: A : TreeBuilder](node: Node[B]): Tree[B] = throw TreeException("cannot add node to an IndexedLeaf")
}

/**
  * A mutable (temporary) tree used as a preliminary to building an MPTT index
  *
  * @param lIndex   the (optional) index to the left of the node
  * @param rIndex   the (optional) index to the right of the node
  * @param value    the leaf value
  * @param children the children of this node
  * @tparam A the underlying type of this Branch
  */
case class MutableGenericIndexedTree[A](var lIndex: Option[Long], var rIndex: Option[Long], var value: Option[A], var children: List[Node[A]]) extends Branch[A] with IndexedTree[A] {
  def get: Option[A] = value
}

/**
  * The exception class for Trees
  *
  * @param msg   the message to be yielded by the exception
  * @param cause the cause (defaults to null)
  */
case class TreeException(msg: String, cause: Throwable = null) extends Exception(msg, cause)

/**
  * Base class for binary trees
  *
  * @param left  the left node
  * @param right the right node
  * @tparam A the underlying type of this binary tree
  */
abstract class AbstractBinaryTree[+A: Ordering](left: Node[A], right: Node[A]) extends Branch[A] {
  assume(AbstractBinaryTree.isOrdered(left, right), s"$left is not ordered properly with $right")

  /**
    * Return a sequence made up of left and right
    *
    * @return the immediate descendants (children) of this branch
    */
  def children: List[Node[A]] = List(left, right)
}

/**
  * Abstract base class for a leaf whose value is a
  *
  * @param a the value of this leaf
  * @tparam A the underlying type of this Leaf
  */
abstract class AbstractLeaf[+A](a: A) extends Node[A] {
  def nodeIterator(depthFirst: Boolean): Iterator[Node[A]] = Iterator.single(this)

  def get = Some(a)

  def size: Int = 1

  def depth: Int = 1

  def includes[B >: A](node: Node[B]): Boolean = this == node

  def includesValue[B >: A](b: B): Boolean = a == b
}

/**
  * Base class for Empty nodes
  */
abstract class AbstractEmpty extends Tree[Nothing] {
  def nodeIterator(depthFirst: Boolean): Iterator[Node[Nothing]] = Iterator.empty

  def get = None

  /**
    * @return the immediate descendants (children) of this branch
    */
  def children: List[Tree[Nothing]] = List.empty

  /**
    * Create a String which represents this Node and its subtree
    *
    * @return an appropriate String
    */
  override def render(indent: Int)(implicit tab: (Int) => Prefix) = s"ø"
}

/**
  * CONSIDER extending AbstractEmpty
  *
  * @param x the String that this punctuation will render as
  */
abstract class Punctuation(x: String) extends Node[Nothing] {

  //noinspection NotImplementedCode
  def +[K, B >: Nothing : TreeBuilder](node: Node[B]): Tree[B] = throw TreeException("cannot add node to a Punctuation")

  def includes[B >: Nothing](node: Node[B]): Boolean = false

  def includes[B >: Nothing](b: B): Boolean = false

  def includesValue[B >: Nothing](b: B): Boolean = false

  def size: Int = 0

  def nodeIterator(depthFirst: Boolean): Iterator[Node[Nothing]] = Iterator.empty

  def get: Option[Nothing] = None

  override def render(indent: Int)(implicit tab: (Int) => Prefix): String = x

  def depth: Int = 0
}

/**
  * Empty object which is necessary for (non-ideal) BinaryTrees.
  * There should be no need for a GeneralTree to reference an Empty but it might happen.
  */
case object Empty extends AbstractEmpty

object TreeBuilder {
  // CONSIDER: this is a rather strange construction -- might want to refactor
  def apply[A: TreeBuilder]: TreeBuilder[A] = implicitly[TreeBuilder[A]]
}

object AbstractLeaf {
  def unapply[A](t: Node[A]): Option[A] = t match {
    case Leaf(x) => Some(x)
    case IndexedLeaf(_, _, x) => Some(x)
    //    case IndexedLeafWithKey(_,_,x) => Some(x)
    case _ => None
  }
}

object Leaf

object Node {

  /**
    * Implicit definition of a Parent for the (type-parameterized, i.e. generic) type Node[A]
    *
    * @tparam A the underlying node type
    * @return a Parent of type Node[A]
    */
  implicit def nodeParent[A]: Parent[Node[A]] = new Parent[Node[A]] {
    /**
      * Get the children of this Node, if any.
      *
      * @return the children as a Seq of Nodes
      */
    def children(t: Node[A]): Seq[Node[A]] = t match {
      case tl: Tree[A] => tl.children
      case _ => Seq.empty
    }
  }
}

object Tree {
  val logger: Logger = LoggerFactory.getLogger(getClass)

  // CONSIDER we want the key value to implement ordering, not the value itself
  // CONSIDER implement this like for general tree (below)

  /**
    * Method to populate an ordered tree.
    *
    * @param values the values of the nodes to put in the tree
    * @param vo     the (implicit) ValueOps
    * @tparam K the key type
    * @tparam A the underlying value type
    * @return a Tree[A]
    */
  def populateOrderedTree[K, A: TreeBuilder](values: Seq[A])(implicit vo: ValueOps[K, A]): Option[Tree[A]] = {
    values match {
      case h :: t =>
        TreeBuilder[A].buildTree(Some(h), Seq()) match {
          case StructuralTree(x) =>
            // CHECK this cast looks bad, also we shouldn't need it given that we're doing pattern matching!
            var result: StructuralTree[K, A] = x.asInstanceOf[StructuralTree[K, A]]
            for (w <- t) {
              result = result :+ Leaf(w)
            }
            Some(result)
          // FIXME we need to implement this case
          case x: Tree[A] => throw TreeException(s"buildTree produced a Tree which isn't a StructuralTree: $x") // Option(t.foldLeft(x)(_ + _))
        }
    }
  }

  def populateGeneralTree[K, A: TreeBuilder](root: Option[A], values: Seq[A])(implicit vo: ValueOps[K, A]): Tree[A] = {
    @tailrec
    def inner(result: StructuralTree[K, A], work: Seq[A]): Tree[A] = work match {
      case Nil => result
      case h :: t => inner(result :+ Leaf(h), t)
    }

    inner(TreeBuilder[A].buildTree(root, Seq()).asInstanceOf[StructuralTree[K, A]], values)
  }

  def join(xo: Option[String], yo: Option[String]): Option[String] = {
    val mt = Some("")
    for (x <- xo.orElse(mt); y <- yo.orElse(mt)) yield x + y
  }

  /**
    * Method to create an indexed tree (suitable for modified pre-order tree traversal -- MPTT).
    *
    * @param node  the node representing the tree to be indexed
    * @param index the starting value of index
    * @tparam A the underlying type
    * @return an IndexedNode[A]
    */
  def createIndexedTree[K, A](node: Node[A], index: Int = 0)(implicit vo: ValueOps[K, A]): IndexedNode[A] = {
    @tailrec
    def inner(r: Seq[Node[A]], work: (Int, Seq[Node[A]])): Seq[Node[A]] = {
      work._2 match {
        case Nil => r
        // NOTE this looks totally wrong!!
        case h :: t => inner(r :+ createIndexedTree(h, work._1), (work._1 + 2 * h.size, t))
      }
    }

    node match {
      case Leaf(x) => IndexedLeaf[A](Some(index), Some(index + 1), x)
      case Branch(ao, ans) =>
        val rIndex = index + 1 + 2 * (ans map (_.size) sum)
        MutableGenericIndexedTree(Some(index), Some(rIndex), ao, inner(Nil, (index + 1, ans)).toList)
      case Empty => EmptyWithIndex.asInstanceOf[IndexedNode[A]] // CHECK this is OK
      case _ => throw TreeException(s"can't created IndexedTree from $node")
    }
  }
}

object GeneralTree {
  trait GeneralTreeBuilder[A] extends TreeBuilder[A] {
    def buildTree(maybeValue: Option[A], children: Seq[Node[A]]): Tree[A] = GeneralTree(maybeValue.get, children.toList)

    def buildLeaf(a: A): Node[A] = Leaf(a)

    def nodesAlike(x: Node[A], y: Node[A]): Boolean = x match {
      case b@Branch(_, _) => (b like y).toBoolean(false)
      case AbstractLeaf(a) => FP.contains(y.get, a)
      case _ => x == y
    }

    // CONSIDER, for a generic getParent, using getParentKey from HasParent and then look it up via findByKey
  }

  implicit object GeneralTreeBuilderString extends GeneralTreeBuilder[String] {
    // CONSIDER moving the intelligence from here into a ValueOps object
    def getParent(tree: Tree[String], t: String): Option[Node[String]] = tree.find(t.substring(0, t.length - 1))
  }

  implicit object GeneralTreeBuilderInt extends GeneralTreeBuilder[Int] {
    def getParent(tree: Tree[Int], t: Int): Option[Node[Int]] = tree.find(t / 10)
  }

}

object UnvaluedBinaryTree {
  // CONSIDER creating a common TreeBuilder for all binary trees
  abstract class UnvaluedBinaryTreeBuilder[A: Ordering] extends TreeBuilder[A] {
    def buildTree(maybeValue: Option[A], children: Seq[Node[A]]): Tree[A] = {
      val ns = children filterNot (_ == Empty)
      ns.size match {
        case 0 => buildTree(Empty, Empty)
        case 1 => buildTree(ns.head, Empty)
        case 2 => buildTree(ns.head, ns.last)
        case 3 =>
          // If there are more than two children, then the last element needs to be ordered appropriately amongst the first two children
          val l = ns.head
          val r = ns(1)
          val x = ns(2)
          assert(AbstractBinaryTree.isOrdered(l, r), s"logic error: $l and $r are not properly ordered")
          if (AbstractBinaryTree.isOrdered(x, l)) buildTree(None, Seq(x, buildTree(l, r)))
          else buildTree(None, Seq(l, buildTree(r, x)))
        case _ => throw TreeException(s"buildTree with value: $maybeValue and children: $children")
      }
    }

    // CONSIDER simplify this: now that we explicitly order n1, n2 we don't have to be so particular about the various non-overlapping cases
    def buildTree(x: Node[A], y: Node[A]): Tree[A] = {
      // XXX if x,y are ordered correctly (or overlapping) we create n1, n2 in same order, otherwise we flip the order
      val (n1, n2) = if (AbstractBinaryTree.isOrdered(x, y)) (x, y) else (y, x)
      assert(AbstractBinaryTree.isOrdered(n1, n2), s"logic error: $n1 and $n2 are not properly ordered")
      n1 match {
        case UnvaluedBinaryTree(l, r) =>
          val pair =
            if (AbstractBinaryTree.isOverlap(n2, l))
              (buildTree(l, n2), r) // type 1
            else if (AbstractBinaryTree.isOverlap(n2, r))
              (l, buildTree(r, n2)) // type 2
            else {
              if (AbstractBinaryTree.isOrdered(l, n2))
                if (AbstractBinaryTree.isOrdered(n2, r))
                  (apply(l, n2), r) // type 3
                else
                  (n1, n2) // type 4
              else
                (n2, n1) // type 5
            }
          apply(pair._1, pair._2)
        case p@Leaf(_) =>
          n2 match {
            case UnvaluedBinaryTree(l, r) =>
              val pair =
                if (AbstractBinaryTree.isOverlap(p, l))
                  (buildTree(l, p), r) // type 1
                else if (AbstractBinaryTree.isOverlap(p, r))
                  (l, buildTree(r, p)) // type 2
                else {
                  if (AbstractBinaryTree.isOrdered(l, p))
                    if (AbstractBinaryTree.isOrdered(p, r))
                      (apply(l, p), r) // type 3
                    else
                      (n2, p) // type 4
                  else
                    (p, n2) // type 5
                }
              apply(pair._1, pair._2)
            case q@Leaf(_) => if (AbstractBinaryTree.isOrdered(p, q)) apply(p, q) else apply(q, p)
            case Empty => UnvaluedBinaryTree(p, Empty)
            case _ => throw TreeException(s"treeBuilder not implemented for Leaf $p and $n2")
          }
        case Empty => apply(x, Empty) // CHECK this is OK
        case _ => throw TreeException(s"treeBuilder not implemented for $n1")
      }
    }

    def buildLeaf(a: A): Node[A] = Leaf(a)

    def getParent(tree: Tree[A], t: A): Option[Node[A]] = {
      @tailrec def inner(tree: Node[A], t: A, parent: Option[Node[A]]): Option[Node[A]] = tree match {
        // CONSIDER rewriting the unapply method so that l and r are of type Tree[T]
        // CHECK that this terminates
        case AbstractBinaryTree(_, l, r) =>
          if (isOverlap(t, l)) inner(l, t, Some(tree))
          else inner(r, t, Some(tree))
        case Leaf(_) => parent
        case Empty => parent
        case _ => throw TreeException(s"inner for binary tree didn't terminate properly: $tree, $t, $parent")
      }

      inner(tree, t, None)
    }

    /**
      * This non-tail-recursive method determines if the two given nodes are structurally the same
      *
      * @param x node x
      * @param y node y
      * @return true if they are the same
      */
    def nodesAlike(x: Node[A], y: Node[A]): Boolean = x match {
      case Branch(oa1, ns1) => y match {
        case Branch(oa2, ns2) => ns1.size == ns2.size && Kleenean(map2(oa1, oa2)(_ == _)).toBoolean(true) && ((ns1 zip ns2) forall (p => nodesAlike(p._1, p._2)))
        case _ => false
      }
      case _ => x == y
    }
  }

  implicit object UnvaluedBinaryTreeBuilderInt extends UnvaluedBinaryTreeBuilder[Int]

  implicit object UnvaluedBinaryTreeBuilderString extends UnvaluedBinaryTreeBuilder[String]

}

object BinaryTree {

  // TODO implement this properly, given that there are values in BinaryTree
  // CONSIDER eliminate danger of infinite recursion in this method, perhaps make it tail-recursive

  abstract class BinaryTreeBuilder[A: Ordering] extends TreeBuilder[A] {
    /**
      * Build a new tree, given a value and child nodes
      *
      * @param maybeValue the (optional) value which the new tree will have at its root
      * @param children   the the children of the node
      * @return a tree the (optional) value at the root and children as the immediate descendants
      */
    def buildTree(maybeValue: Option[A], children: Seq[Node[A]]): Tree[A] = {
      require(maybeValue.isDefined, "value must be defined for BinaryTree")
      require(children.size == 2, "children must define exactly two nodes")
      BinaryTree(maybeValue.get, children.head, children.tail.head)
    }

    /**
      * Build a new leaf, given a value
      *
      * @param a the value for the leaf
      * @return a node which is a leaf node
      */
    def buildLeaf(a: A): Node[A] = BinaryTree(a, Empty, Empty)

    /**
      * Get a node from an existing tree to which a new node with value a can be attached
      *
      * @param tree the tree whence we want to find the (potential) parent of a new node with value a
      * @param a    the value of the new node
      * @return the Node to which the new node will be attached (if such a node exists). Note that this might be a leaf
      */
    //noinspection NotImplementedCode
    def getParent(tree: Tree[A], a: A): Option[Node[A]] = ???

    /**
      * This method determines if the two given nodes are structurally the same
      *
      * @param x node x
      * @param y node y
      * @return true if they are the same
      */
    //noinspection NotImplementedCode
    def nodesAlike(x: Node[A], y: Node[A]): Boolean = ???

  }

  implicit object BinaryTreeBuilderInt extends BinaryTreeBuilder[Int]

  implicit object BinaryTreeBuilderString extends BinaryTreeBuilder[String]

}

object StructuralTree {
  /**
    * Extractor for use in pattern matching
    *
    * @param t the tree
    * @tparam A the tree type
    * @return an optional tuple of two children.
    */
  def unapply[K, A](t: StructuralTree[K, A]): Option[StructuralTree[K, A]] =
    t match {
      case x: StructuralTree[K, A]@unchecked => Some(x)
      case _ => None
    }
}

object AbstractBinaryTree {

  /**
    * Extractor for use in pattern matching
    *
    * @param t the tree
    * @tparam A the tree type
    * @return an optional tuple of two children
    */
  def unapply[A](t: AbstractBinaryTree[A]): Option[(Option[A], Node[A], Node[A])] = Some((t.get, t.children.head, t.children.tail.head))

  /**
    * Method to determine if node a compares as less than node b
    *
    * @param a node a
    * @param b node b
    * @tparam A the underlying node type
    * @return true if node a compares as less than node b
    */
  def isOrdered[A: Ordering](a: Node[A], b: Node[A]): Boolean = isOrdered(compare(a, b)).toBoolean(true)

  /**
    * Given a sequence of comparison values, determine if the result is true, false or maybe.
    *
    * @param is the comparison values as a sequence of Ints (either -1, 0, or 1)
    * @return Some(true) if all the values are -1, Some(false) if all the values are +1, otherwise None.
    */
  def isOrdered(is: Seq[Int]): Maybe = {
    val distinct = is.distinct
    if (distinct.size == 1)
      Kleenean(distinct.head).deny
    else
      Kleenean(None)
  }

  /**
    * method to determine if there is an overlap between the values of tree n1 and tree n2
    *
    * @param n1 the first tree (order is not important)
    * @param n2 the second tree
    * @tparam A the underlying tree type
    * @return true if there is an overlap of values, that's to say, the values cannot be completely separated.
    */
  def isOverlap[A: Ordering](n1: Node[A], n2: Node[A]): Boolean = isOrdered(compare(n1, n2)) match {
    case Kleenean(None) => true
    case _ => false
  }

  /**
    * method to determine if there is an overlap between a value a and the values of tree n
    *
    * @param a the value to compare
    * @param n the tree
    * @tparam A the underlying tree type
    * @return true if there is an overlap of values, that's to say, the values cannot be completely separated.
    */
  def isOverlap[A: Ordering](a: A, n: Node[A]): Boolean = isOrdered(compare(a, n)) match {
    case Kleenean(None) => true
    case _ => false
  }

  /**
    * Determine whether all of the nodes on the left are ordered before all the nodes on the right
    *
    * @param anL the left-node
    * @param anR the right-node
    * @tparam A the underlying type of the nodes and comparisons
    * @return true if the proposition is proven
    */
  def compare[A: Ordering](anL: Node[A], anR: Node[A]): Seq[Int] = anL match {
    case AbstractLeaf(a) => compare(a, anR)
    case Branch(_, ans) => anR match {
      case AbstractLeaf(b) => compare(b, ans).map(-_)
      case Branch(_, bns) => compare(ans, bns)
      case Empty => Seq(0)
    }
    case Empty => Seq(0)
  }

  /**
    * Determine whether the value on the left is ordered before all the nodes on the right
    *
    * NOTE: this is recursive
    *
    * @param ans the left-node-sequence
    * @param bns the right-node-sequence
    * @tparam A the underlying type of the nodes and comparisons
    * @return true if the proposition is proven
    */
  def compare[A: Ordering](ans: NodeSeq[A], bns: NodeSeq[A]): Seq[Int] = for (a1 <- ans; r <- compare(a1, bns)) yield r

  /**
    * Determine whether the value on the left is ordered before all the nodes on the right
    *
    * NOTE: this is recursive
    *
    * @param an  the left-node
    * @param bns the right-node-sequence
    * @tparam A the underlying type of the nodes and comparisons
    * @return true if the proposition is proven
    */
  def compare[A: Ordering](an: Node[A], bns: NodeSeq[A]): Seq[Int] = for (b <- bns; r <- compare(an, b)) yield r

  /**
    * Determine whether the value on the left is ordered before all the nodes on the right
    *
    * NOTE: this is recursive
    *
    * @param a  the left-value
    * @param an the right-node
    * @tparam A the underlying type of the nodes and comparisons
    * @return true if the proposition is proven
    */
  def compare[A: Ordering](a: A, an: Node[A]): Seq[Int] = an match {
    case Leaf(b) => Seq(compare(a, b))
    case Branch(_, as) => compare(a, as)
    case Empty => Seq(0)
    case _ => throw TreeException(s"match error for: $a, $an")
  }

  /**
    * Determine whether the value on the left is ordered before all the nodes on the right
    *
    * @param a  the left-value
    * @param as the right-iterator
    * @tparam A the underlying type of the nodes and comparisons
    * @return true if the proposition is proven
    */
  def compare[A: Ordering](a: A, as: NodeSeq[A]): Seq[Int] = for (a1 <- as; r <- compare(a, a1)) yield r

  /**
    * Determine whether the value on the left is ordered before the value on the right
    *
    * @param a the left-value
    * @param b the right-value
    * @tparam A the underlying type of the nodes and comparisons
    * @return -1 if proposition is true; 0 if they are equal; 1 if the proposition if false
    */
  def compare[A: Ordering](a: A, b: A): Int = math.signum(implicitly[Ordering[A]].compare(a, b))
}

object Branch {
  def unapply[A](e: Tree[A]): Option[(Option[A], NodeSeq[A])] = Some(e.get, e.children)
}

object IndexedNode {
  def unapply[A](e: IndexedNode[A]): Option[(Node[A], Long, Long)] =
    for (x <- Option(e); l <- x.lIndex; r <- x.rIndex) yield (x, l, r)
}

case object EmptyWithIndex extends AbstractEmpty with TreeIndex {
  override def toString = "EmptyWIthIndex"

  def lIndex: Option[Long] = None

  def rIndex: Option[Long] = None
}

case object Open extends Punctuation("{")

case object Close extends Punctuation("}")
