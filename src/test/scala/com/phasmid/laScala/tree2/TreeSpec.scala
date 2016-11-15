package com.phasmid.laScala.tree2

import com.phasmid.laScala.Kleenean
import com.phasmid.laScala.tree2.BinaryTree._

import org.scalatest.{FlatSpec, Matchers}
import scala.io.Source

/**
  * Created by scalaprof on 10/19/16.
  */
class TreeSpec extends FlatSpec with Matchers {
  behavior of "compare"
  it should "work for simple values" in {
    compare(1, 2) shouldBe -1
    compare(2, 1) shouldBe 1
    compare(1, 1) shouldBe 0
  }

  it should "work for value-node" in {
    isOrdered(compare(1, Leaf(2))) should matchPattern { case Kleenean(Some(true)) => }
    isOrdered(compare(2, Leaf(1))) should matchPattern { case Kleenean(Some(false)) => }
    isOrdered(compare(1, Leaf(1))) should matchPattern { case Kleenean(None) => }
  }

  it should "work for value-sequence" in {
    isOrdered(compare(1, Seq(Leaf(3), Leaf(4)))) should matchPattern { case Kleenean(Some(true)) => }
    isOrdered(compare(3, Seq(Leaf(1), Leaf(2)))) should matchPattern { case Kleenean(Some(false)) => }
    isOrdered(compare(1, Seq(Leaf(1)))) should matchPattern { case Kleenean(None) => }
    isOrdered(compare(2, Seq(Leaf(1), Leaf(3)))) should matchPattern { case Kleenean(None) => }
  }

  it should "work for nodes" in {
    isOrdered(compare(Leaf(1), Leaf(2))) should matchPattern { case Kleenean(Some(true)) => }
    isOrdered(compare(Leaf(2), Leaf(1))) should matchPattern { case Kleenean(Some(false)) => }
    isOrdered(compare(Leaf(1), Leaf(1))) should matchPattern { case Kleenean(None) => }
  }

  it should "work for node-sequence" in {
    isOrdered(compare(Leaf(1), Seq(Leaf(3), Leaf(4)))) should matchPattern { case Kleenean(Some(true)) => }
    isOrdered(compare(Leaf(3), Seq(Leaf(1), Leaf(2)))) should matchPattern { case Kleenean(Some(false)) => }
    isOrdered(compare(Leaf(1), Seq(Leaf(1)))) should matchPattern { case Kleenean(None) => }
  }

  it should "work for node sequences" in {
    isOrdered(compare(Seq(Leaf(1), Leaf(2)), Seq(Leaf(3), Leaf(4)))) should matchPattern { case Kleenean(Some(true)) => }
    isOrdered(compare(Seq(Leaf(3), Leaf(4)), Seq(Leaf(1), Leaf(2)))) should matchPattern { case Kleenean(Some(false)) => }
    isOrdered(compare(Seq(Leaf(3)), Seq(Leaf(3)))) should matchPattern { case Kleenean(None) => }
  }

  behavior of "nodeIterator"

  it should "work properly for GenericBranch" in {
    val tree = GeneralBranch(0, Seq(Leaf(1), Leaf(2), Leaf(3)))
    val ns = tree.nodeIterator(true)
    ns.size shouldBe 4
  }
  it should "work properly for BinaryTree" in {
    val tree = BinaryTree(BinaryTree(Leaf(1), Leaf(3)), BinaryTree(Leaf(5), Leaf(6)))
    val ns = tree.nodeIterator(true)
    ns.size shouldBe 7
  }

  behavior of "iterator"

  it should "work properly for GenericBranch" in {
    val tree = GeneralBranch(0, Seq(Leaf(1), Leaf(2), Leaf(3)))
    val ns = tree.iterator(false).toList
    ns.size shouldBe 4
    ns shouldBe List(0, 1, 2, 3)
  }

  it should "work properly for BinaryTree" in {
    val tree = BinaryTree(BinaryTree(Leaf(1), Leaf(3)), BinaryTree(Leaf(5), Leaf(6)))
    val ns = tree.iterator(true).toList
    ns.size shouldBe 4
    ns shouldBe List(1, 3, 5, 6)
  }

  behavior of ":+(node)"

  it should "work correctly for GenericTree" in {
    import GeneralBranch._
    val tree = GeneralBranch(0, Seq(Leaf(1), Leaf(2), Leaf(3))) :+ Leaf(4)
    tree shouldBe GeneralBranch(0, Seq(Leaf(1), Leaf(2), Leaf(3), Leaf(4)))
    tree.iterator(true).toSeq shouldBe Seq(1, 2, 3, 4, 0)
    tree.iterator(false).toSeq shouldBe Seq(0, 1, 2, 3, 4)
  }
  it should "work correctly for BinaryTree type 1/3" in {
    import BinaryTree._
    val tree = BinaryTree(BinaryTree(Leaf(1), Leaf(3)), BinaryTree(Leaf(5), Leaf(6))) :+ Leaf(2)
    tree shouldBe BinaryTree(BinaryTree(BinaryTree(Leaf(1), Leaf(2)), Leaf(3)), BinaryTree(Leaf(5), Leaf(6)))
    tree.iterator(true).toSeq shouldBe Seq(1, 2, 3, 5, 6)
    tree.iterator(false).toSeq shouldBe Seq(1, 2, 3, 5, 6)
  }
  it should "work correctly for BinaryTree type 2/3" in {
    import BinaryTree._
    val tree = BinaryTree(Leaf(1), BinaryTree(Leaf(2), Leaf(4))) :+ Leaf(3)
    tree shouldBe BinaryTree(Leaf(1), BinaryTree(BinaryTree(Leaf(2), Leaf(3)), Leaf(4)))
  }
  it should "work correctly for BinaryTree type 4" in {
    import BinaryTree._
    val tree = BinaryTree(Leaf(1), BinaryTree(Leaf(2), Leaf(3))) :+ Leaf(4)
    tree shouldBe BinaryTree(BinaryTree(Leaf(1), BinaryTree(Leaf(2), Leaf(3))), Leaf(4))
  }
  it should "work correctly for BinaryTree type 5" in {
    import BinaryTree._
    val tree = BinaryTree(Leaf(1), BinaryTree(Leaf(2), Leaf(4))) :+ Leaf(0)
    tree shouldBe BinaryTree(Leaf(0), BinaryTree(Leaf(1), BinaryTree(Leaf(2), Leaf(4))))
  }
  behavior of ":+(value)"

  it should "work correctly for GenericTree" in {
    import GeneralBranch._
    val tree = GeneralBranch(0,Nil) :+ 1 :+ 2 :+ 3 :+ 4
    tree shouldBe GeneralBranch(0, Seq(Leaf(1), Leaf(2), Leaf(3), Leaf(4)))
  }
  it should "work correctly for BinaryTree type 1/3" in {
    import BinaryTree._
    val tree = BinaryTree(BinaryTree(Leaf(1), Leaf(3)), BinaryTree(Leaf(5), Leaf(6))) :+ 2
    tree shouldBe BinaryTree(BinaryTree(BinaryTree(Leaf(1), Leaf(2)), Leaf(3)), BinaryTree(Leaf(5), Leaf(6)))
  }
  it should "work correctly for BinaryTree type 2/3" in {
    import BinaryTree._
    val tree = BinaryTree(Leaf(1), BinaryTree(Leaf(2), Leaf(4))) :+ 3
    tree shouldBe BinaryTree(Leaf(1), BinaryTree(BinaryTree(Leaf(2), Leaf(3)), Leaf(4)))
  }
  it should "work correctly for BinaryTree type 4" in {
    import BinaryTree._
    val tree = BinaryTree(1,Empty) :+ Leaf(2) :+ Leaf(3) :+ Leaf(4)
    tree shouldBe BinaryTree(BinaryTree(BinaryTree(BinaryTree(Leaf(1), Leaf(2)), Leaf(3)), Leaf(4)), Empty)
  }
  it should "work correctly for BinaryTree type 5" in {
    import BinaryTree._
    val tree = BinaryTree(Leaf(1), BinaryTree(Leaf(2), Leaf(4))) :+ 0
    tree shouldBe BinaryTree(Leaf(0), BinaryTree(Leaf(1), BinaryTree(Leaf(2), Leaf(4))))
  }

    behavior of "size"
    it should "work for GeneralBranch" in {
      import GeneralBranch._
      val tree = GeneralBranch(0,Nil) :+ 1 :+ 2 :+ 3 :+ 4
      tree.size shouldBe 5
    }
    it should "work for BinaryTree" in {
      import BinaryTree._
      val tree = BinaryTree(1,Empty) :+ Leaf(2) :+ Leaf(3) :+ Leaf(4)
      tree.size shouldBe 4
    }

    behavior of "get"
    it should "work for GeneralBranch" in {
      val tree = GeneralBranch(1,Seq(Leaf(2),Leaf(3)))
      tree.get should matchPattern {case Some(1) => }
    }
  it should "work for BinaryTree" in {
    val tree = BinaryTree(BinaryTree(Leaf(1), Leaf(3)), BinaryTree(Leaf(5), Leaf(6)))
    tree.get should matchPattern {case None => }
  }

    behavior of "depth"
    it should "work for GeneralBranch" in {
      val tree = GeneralBranch(1,Seq(Leaf(2),Leaf(3)))
      tree.depth shouldBe 2
    }
    it should "work for BinaryTree" in {
      val tree = BinaryTree(BinaryTree(Leaf(1), Leaf(3)), BinaryTree(Leaf(5), Leaf(6)))
      tree.depth shouldBe 3
    }

    behavior of "compareValues"
    it should "work for Leaf" in {
       val tree1 = Leaf(1)
      val tree2 = Leaf(1)
      tree2.compareValues(tree1) should matchPattern {case Kleenean(Some(true)) => }
    }

    behavior of "like"
  it should "work for GeneralBranch" in {
    val tree1 = GeneralBranch(1,Seq(Leaf(2),Leaf(3)))
    val tree2 = GeneralBranch(1,Seq(Leaf(5),Leaf(6)))
    val tree3 = GeneralBranch(1,Seq(GeneralBranch(4,Seq(Leaf(5),Leaf(6)))))
    val tree4 = GeneralBranch(1,Seq(GeneralBranch(4,Seq(Leaf(10),Leaf(11)))))
    tree1.like(tree2) should matchPattern {case Kleenean(Some(false)) => }
    // TODO understand why this doesn't work. At appears to yield Some(true)
//    tree3.like(tree4) should matchPattern {case Kleenean(Some(true)) => }
  }
  it should "work for BinaryTree" in {
    val tree1 = BinaryTree(BinaryTree(Leaf(1), Leaf(3)), BinaryTree(Leaf(5), Leaf(6)))
    val tree2 = BinaryTree(BinaryTree(Leaf(1), Leaf(3)), BinaryTree(Leaf(5), Leaf(6)))
    // the like method doesn't really make sense for Binary Trees
    tree1 like tree2 should matchPattern {case Kleenean(None) => }
  }

    behavior of "includes"
  it should "work for GeneralBranch" in {
    val tree = GeneralBranch(1,Seq(Leaf(2),Leaf(3)))
    tree.includes(1) shouldBe true
    tree.includes(2) shouldBe true
    tree.includes(3) shouldBe true
    tree.includes(4) shouldBe false
    tree.includes(0) shouldBe false
  }
  it should "work for BinaryTree" in {
    val tree = BinaryTree(BinaryTree(Leaf(1), Leaf(3)), BinaryTree(Leaf(5), Leaf(6)))
    tree.includes(1) shouldBe true
    tree.includes(2) shouldBe false
    tree.includes(3) shouldBe true
    tree.includes(4) shouldBe false
    tree.includes(5) shouldBe true
    tree.includes(6) shouldBe true
  }

  behavior of "find"
  it should "work for GeneralBranch" in {
    val tree = GeneralBranch(1,Seq(Leaf(2),Leaf(3)))
    tree.find(1) should matchPattern { case Some(GeneralBranch(1,Seq(Leaf(2),Leaf(3)))) => }
    tree.find(2) should matchPattern { case Some(Leaf(2)) => }
    tree.find(3) should matchPattern { case Some(Leaf(3)) => }
    tree.find(4) should matchPattern { case None => }
    tree.find(0) should matchPattern { case None => }
  }
  it should "work for BinaryTree" in {
    val tree = BinaryTree(BinaryTree(Leaf(1), Leaf(3)), BinaryTree(Leaf(5), Leaf(6)))
    tree.find(1) should matchPattern { case Some(Leaf(1)) => }
    tree.find(2) should matchPattern { case None => }
    tree.find(3) should matchPattern { case Some(Leaf(3)) => }
    tree.find(4) should matchPattern { case None => }
    tree.find(5) should matchPattern { case Some(Leaf(5)) => }
    tree.find(6) should matchPattern { case Some(Leaf(6)) => }
  }

}
//  it should "work correctly for unsorted Flatland tree" in {
//    val uo = Option(getClass.getResource("flatland.txt"))
//    uo should matchPattern { case Some(_) => }
//    val so = uo map {_.openStream}
//    val wso = for (s <- so) yield (for (l <- Source.fromInputStream(s).getLines; w <- l.split("""\W+""")) yield w).toList
//    import scala.language.postfixOps
//    val z: Seq[String] = wso match {
//      case Some(ws) => ws map {_.toLowerCase} filterNot {_.isEmpty} distinct
//      case _ => Seq[String]()
//    }
//    val tree = TreeLike.populateTree(z)
//    println(tree)
//    def f(t: Node[String]): Option[String] = t.get
//    def g(ns: Seq[String], wo: Option[String]): Seq[String] = wo match { case Some(i) => ns :+ i; case _ => ns}
//    Parent.traverse(f,g)(List[Node[String]](tree),List[String]()).take(12) shouldBe List("i", "flatland", "world", "because", "hexagons", "not", "years", "a", "but", "freely", "higher", "make")
//  }
//  val flatLandTree = "a{{about}above{{actually}ago{{alas}all{{and}another{{anything}appear{{are}as{{at}back{{be}because{{become}becoming{{been}below{{bringing}but{{by}call{{can}ceased{{circle}clearer{{condition}contrary{{correct}could{{country}countrymen{{dare}demonstrate{{described}distinguish{{down}drawing{{edge}edges{{exactly}except{{eye}far{{few}figure{{figures}find{{fixed}flatland{{flatlander}freely{{from}gradually{{happy}hard{{has}have{{hexagons}higher{{i}imagine{{impossible}in{{inhabitants}instead{{into}is{{it}its{{kind}last{{leaning}least{{like}line{{lines}live{{look}lower{{luminous}make{{middle}mind{{more}move{{moving}much{{my}nature{{necessity}nor{{not}nothing{{notion}now{{of}on{{once}one{{only}opened{{or}other{{our}oval{{over}p{{paper}penny{{pentagons}perceive{{place}placed{{places}power{{pretty}privileged{{readers}remaining{{rising}said{{say}see{{shadows}sheet{{should}sight{{sinking}so{{solid}space{{speedily}squares{{straight}such{{suppose}surface{{table}tables{{that}the{{their}them{{then}there{{things}this{{thus}to{{triangles}universe{{upon}us{{vast}very{{view}views{{visible}was{{we}were{{what}when{{which}who{{will}with{{without}world{{years}you{{your}yourself}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}"

///**
//  * Created by scalaprof on 10/19/16.
//  */
//class TreeSpec extends FlatSpec with Matchers {
//
//  behavior of "GenericTree"
//  it should "apply correctly with varargs" in {
//    val tree1 = GenericTree(1,Seq(GenericTree(2,Seq(GenericTree(3,Seq(Empty))))))
//    val tree2 = GenericTree(1,2,3)
//    tree1 shouldBe tree2
//  }
//  it should "apply correctly with List" in {
//    val tree1 = GenericTree(1,Seq(GenericTree(2,Seq(GenericTree(3,Seq(Empty))))))
//    val tree2 = GenericTree(1,2,3)
//    tree1 shouldBe tree2
//  }
//
//  behavior of "BinaryTree"
//  it should "apply correctly with varargs" in {
//    val tree = BinaryTreeAlt(1,2,3)
//    tree.includes(1) shouldBe true
//    tree.depth shouldBe 3
//    tree.render shouldBe "1{2{3}}"
//  }
//  it should "apply correctly with List" in {
//    val tree1 = GenericTree(1,Seq(GenericTree(2,Seq(GenericTree(3,Seq(Empty))))))
//    val tree2 = GenericTree(1,2,3)
//    tree1 shouldBe tree2
//  }
//  it should "support createIndexedTree correctly" in {
//    val tree = BinaryTreeAlt(1,2,3)
//    val indexedTree = Tree.createIndexedTree(tree.asInstanceOf[BinaryTreeAlt[Int]])
//    println(s"indexedTree: $indexedTree")
//  }
//  it should "support MPTT correctly" in {
//    val tree = BinaryTreeAlt(1,2,3)
//    val indexedTree = Tree.createIndexedTree(tree.asInstanceOf[BinaryTreeAlt[Int]])
//    val mptt = MPTT(indexedTree.asInstanceOf[IndexedNode[Int]])
//    println(mptt)
//  }
//
//  behavior of ":+"
//  it should "work" in {
//    import BinaryTreeAlt._
//    val x: BinaryTreeAlt[String] = BinaryTreeAlt("A","C","D").asInstanceOf[BinaryTreeAlt[String]]
//    println(x)
//    val y: BinaryTreeAlt[String] = (x :+ LeafNode("B")).asInstanceOf[BinaryTreeAlt[String]]
//    println(y)
//    y.size shouldBe 4
//    y.depth shouldBe 3
//    y.render shouldBe "A{{B}C{D}}"
//    val z = (y :+ LeafNode("Alpha")).asInstanceOf[BinaryTreeAlt[String]] :+ LeafNode("Bravo")
//    println(z)
//  }
//
//  behavior of "real-life BinaryTree"
//  it should "build correctly unsorted" in {
//    val uo = Option(getClass.getResource("flatland.txt"))
//    uo should matchPattern { case Some(_) => }
//    val so = uo map {_.openStream}
//    val wso = for (s <- so) yield (for (l <- Source.fromInputStream(s).getLines; w <- l.split("""\W+""")) yield w).toList
//    import scala.language.postfixOps
//    val z: Seq[String] = wso match {
//      case Some(ws) => ws map {_.toLowerCase} filterNot {_.isEmpty} distinct
//      case _ => Seq[String]()
//    }
//    val tree = TreeLike.populateTree(z)
//    tree.includes("flatland") shouldBe true
//    tree.depth shouldBe 15
//    tree.size shouldBe 177
//    val nodes = tree.iterator
//    val words = for (n <- nodes; v <- n.get) yield v.toString
//    println(words.toSeq mkString " ")
//    tree.render shouldBe flatLandTree
//    val summaries = for (x <- tree.iterator) yield x.summary
//    summaries foreach println
//  }
//  it should "build correctly sorted" in {
//    val uo = Option(getClass.getResource("flatland.txt"))
//    uo should matchPattern { case Some(_) => }
//    val so = uo map {_.openStream}
//    val wso = for (s <- so) yield (for (l <- Source.fromInputStream(s).getLines; w <- l.split("""\W+""")) yield w).toList
//    import scala.language.postfixOps
//    val z: Seq[String] = wso match {
//      case Some(ws) => (ws map {_.toLowerCase} filterNot {_.isEmpty} distinct) sorted
//      case _ => Seq[String]()
//    }
//    val tree = TreeLike.populateTree(z)
//    tree.includes("flatland") shouldBe true
//    tree.depth shouldBe 90
//    tree.size shouldBe 177
//    tree.render shouldBe flatLandTree
//  }
//
//  behavior of "createIndexedTree"
//  it should "work for BinaryTree" in {
//    val tree = BinaryTreeAlt("A", "B", "C").asInstanceOf[BinaryTreeAlt[String]] :+ BinaryTreeAlt("Catfish")
//    tree.includes("Catfish") shouldBe true
//    val indexedTree = Tree.createIndexedTree(tree)
//  }
//  it should "work with find" in {
//    val tree = BinaryTreeAlt("A", "B", "C").asInstanceOf[BinaryTreeAlt[String]] :+ BinaryTreeAlt("Catfish")
//    val indexedTree = Tree.createIndexedTree(tree)
//    val nco = indexedTree.find("C")
//    nco should matchPattern { case Some(MutableGenericIndexedTree(_,_,_,_)) => }
//    val nxo = indexedTree.find("Catfish")
//    nxo should matchPattern { case Some(MutableGenericIndexedTree(_,_,"Catfish",_)) => }
//    val ndo = indexedTree.find("D")
//    ndo should matchPattern { case None => }
//    indexedTree.includes(nco.get) shouldBe true
//    indexedTree.includes(nxo.get) shouldBe true
//  }
//
//  val flatLandTree = "a{{about}above{{actually}ago{{alas}all{{and}another{{anything}appear{{are}as{{at}back{{be}because{{become}becoming{{been}below{{bringing}but{{by}call{{can}ceased{{circle}clearer{{condition}contrary{{correct}could{{country}countrymen{{dare}demonstrate{{described}distinguish{{down}drawing{{edge}edges{{exactly}except{{eye}far{{few}figure{{figures}find{{fixed}flatland{{flatlander}freely{{from}gradually{{happy}hard{{has}have{{hexagons}higher{{i}imagine{{impossible}in{{inhabitants}instead{{into}is{{it}its{{kind}last{{leaning}least{{like}line{{lines}live{{look}lower{{luminous}make{{middle}mind{{more}move{{moving}much{{my}nature{{necessity}nor{{not}nothing{{notion}now{{of}on{{once}one{{only}opened{{or}other{{our}oval{{over}p{{paper}penny{{pentagons}perceive{{place}placed{{places}power{{pretty}privileged{{readers}remaining{{rising}said{{say}see{{shadows}sheet{{should}sight{{sinking}so{{solid}space{{speedily}squares{{straight}such{{suppose}surface{{table}tables{{that}the{{their}them{{then}there{{things}this{{thus}to{{triangles}universe{{upon}us{{vast}very{{view}views{{visible}was{{we}were{{what}when{{which}who{{will}with{{without}world{{years}you{{your}yourself}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}"
//}
