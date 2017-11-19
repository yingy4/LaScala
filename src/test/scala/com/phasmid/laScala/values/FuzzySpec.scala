/*
 * LaScala
 * Copyright (c) 2017. Phasmid Software
 */

package com.phasmid.laScala.values

import org.scalatest.{FlatSpec, Inside, Matchers}

import scala.language.implicitConversions


/**
  * @author scalaprof
  */
class FuzzySpec extends FlatSpec with Matchers with Inside {

  behavior of "apply"
  it should "work for exact" in {
    val target = Exact(2.0)
    target() shouldBe 2
  }
  it should "work for simple bounded" in {
    val target = Bounded(2.0, 1.0)
    target() shouldBe 2
  }

  behavior of "isExact"
  it should "work for exact" in {
    val target = Exact(2.0)
    target.isExact shouldBe true
  }
  it should "work for simple bounded" in {
    val target = Bounded(2.0, 1.0)
    target.isExact shouldBe false
  }

  behavior of "p"
  it should "work for exact" in {
    val target = Exact(2.0)
    target.p(2) shouldBe Probability.Certain
    target.p(2.00001) shouldBe Probability.Impossible
    target.p(4) shouldBe Probability.Impossible
  }
  it should "work for simple bounded" in {
    val target = Bounded(2.0, 1.0)
    target.p(2) shouldBe Probability(1, 2)
    target.p(4) shouldBe Probability.Impossible
  }

  behavior of "map"
  it should "work for exact" in {
    val two = Exact(Rational[Int](2))
    val target = two.map(Rational(1, 2) * _, identity)
    target.apply() shouldBe Rational.one[Int]
    target.isExact shouldBe true
  }
  it should "work for simple bounded" in {
    val two = Bounded(2.0, 1.0)
    val target: NumericFuzzy[Double] = two.map(_ * 0.5, _ * 0.5).asInstanceOf[NumericFuzzy[Double]]
    target() shouldBe 1.0
    target.fuzziness shouldBe 0.5
    target.isExact shouldBe false
  }

  behavior of "map2"
  it should "work for exact" in {
    val two = Exact(Rational[Int](2))
    val ten = Exact(Rational[Int](10))
    val target = two.map2(ten)(_ * _, _ * _)
    target.apply() shouldBe Rational[Int](20)
    target.isExact shouldBe true
  }
  it should "work for bounded and exact" in {
    val two = Bounded(2.0, 1.0)
    val ten = Exact(10.0)
    val target = two.map2(ten)(_ * _, 10.0 * _ + _)
    target() shouldBe 20.0
    target.fuzziness shouldBe 10.0
    target.isExact shouldBe false
  }
  it should "work for exact and bounded" in {
    val two = Bounded(2.0, 1.0)
    val ten = Exact(10.0)
    val target = ten.map2(two)(_ * _, _ + 10.0 * _)
    target() shouldBe 20.0
    target.fuzziness shouldBe 10.0
    target.isExact shouldBe false
  }

  behavior of "plus"
  it should "work for exact" in {
    val two = Exact(Rational[Int](2))
    val ten = Exact(Rational[Int](10))
    val target: Fuzzy[Rational[Int]] = two.plus(ten)
    target.apply() shouldBe Rational[Int](12)
    target.isExact shouldBe true
  }
  it should "work for bounded and exact" in {
    val two = Bounded(2.0, 1.0)
    val ten = Exact(10.0)
    val target: NumericFuzzy[Double] = two.plus(ten)
    target() shouldBe 12.0
    target.fuzziness shouldBe 1.0
    target.isExact shouldBe false
  }
  it should "work for exact and bounded" in {
    val two = Bounded(2.0, 1.0)
    val ten = Exact(10.0)
    val target: NumericFuzzy[Double] = ten.plus(two)
    target() shouldBe 12.0
    target.fuzziness shouldBe 1.0
    target.isExact shouldBe false
  }

  behavior of "minus"
  it should "work for exact" in {
    val two = Exact(Rational[Int](2))
    val ten = Exact(Rational[Int](10))
    val target: Fuzzy[Rational[Int]] = two.minus(ten)
    target.apply() shouldBe Rational[Int](-8)
    target.isExact shouldBe true
  }
  it should "work for bounded and exact" in {
    val two = Bounded(2.0, 1.0)
    val ten = Exact(10.0)
    val target: NumericFuzzy[Double] = two.minus(ten)
    target() shouldBe -8.0
    target.fuzziness shouldBe 1.0
    target.isExact shouldBe false
  }
  it should "work for exact and bounded" in {
    val two = Bounded(2.0, 1.0)
    val ten = Exact(10.0)
    val target: NumericFuzzy[Double] = ten.minus(two)
    target() shouldBe 8.0
    target.fuzziness shouldBe 1.0
    target.isExact shouldBe false
  }

  behavior of "times"
  it should "work for exact" in {
    val two = Exact(Rational[Int](2))
    val ten = Exact(Rational[Int](10))
    val target: Fuzzy[Rational[Int]] = two.times(ten)
    target.apply() shouldBe Rational[Int](20)
    target.isExact shouldBe true
  }
  it should "work for bounded and exact" in {
    val two = Bounded(2.0, 1.0)
    val ten = Exact(10.0)
    val target: NumericFuzzy[Double] = two.times(ten)
    target() shouldBe 20.0
    target.fuzziness shouldBe 10.0
    target.isExact shouldBe false
  }
  it should "work for exact and bounded" in {
    val two = Bounded(2.0, 1.0)
    val ten = Exact(10.0)
    val target: NumericFuzzy[Double] = ten.times(two)
    target() shouldBe 20.0
    target.fuzziness shouldBe 10.0
    target.isExact shouldBe false
  }

  behavior of "div"
  it should "work for exact" in {
    val two = Exact(Rational[Int](2))
    val ten = Exact(Rational[Int](10))
    val target: Fuzzy[Rational[Int]] = two.div(ten)
    target.apply() shouldBe Rational[Int](5).invert
    target.isExact shouldBe true
  }
  it should "work for bounded and exact" in {
    val two = Bounded(2.0, 1.0)
    val ten = Exact(10.0)
    val target: NumericFuzzy[Double] = two.div(ten)
    target() shouldBe 0.2
    target.fuzziness shouldBe 0.1
    target.isExact shouldBe false
  }
  it should "work for exact and bounded" in {
    val two = Bounded(2.0, 1.0)
    val ten = Exact(10.0)
    val target: NumericFuzzy[Double] = ten.div(two)
    target() shouldBe 5.0
    target.fuzziness shouldBe 2.5 // CHECK this
    target.isExact shouldBe false
  }

  behavior of "power"
  it should "work for exact" in {
    val two = Exact(Rational[Int](2))
    val target: Fuzzy[Rational[Int]] = two.power(5)
    target.apply() shouldBe Rational[Int](32)
    target.isExact shouldBe true
  }
  it should "work for bounded and exact" in {
    val two = Bounded(2.0, 1.0)
    val target: NumericFuzzy[Double] = two.power(5)
    target() shouldBe 32.0
    target.fuzziness shouldBe 5.0 // CHECK this I believe it is incorrect
    target.isExact shouldBe false
  }
}