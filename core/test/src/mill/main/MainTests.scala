package mill.main

import mill.Module
import mill.define.{Discover, Segment, Task}
import mill.util.TestGraphs._
import mill.util.TestUtil.test
import utest._
object MainTests extends TestSuite{

  def check[T <: mill.Module](module: T,
                              discover: Discover)(
                              selectorString: String,
                              expected0: Either[String, Seq[T => Task[_]]]) = {

    val expected = expected0.map(_.map(_(module)))
    val resolved = for{
      selectors <- mill.main.ParseArgs(Seq(selectorString)).map(_._1.head)
      val crossSelectors = selectors.map{case Segment.Cross(x) => x.toList.map(_.toString) case _ => Nil}
      task <- mill.main.Resolve.resolve(selectors, module, discover, Nil, crossSelectors, Nil)
    } yield task
    assert(resolved == expected)
  }
  val tests = Tests{
    val graphs = new mill.util.TestGraphs()
    import graphs._
    'single - {
      val check = MainTests.check(singleton, Discover[singleton.type]) _
      'pos - check("single", Right(Seq(_.single)))
      'neg1 - check("doesntExist", Left("Cannot resolve task doesntExist"))
      'neg2 - check("single.doesntExist", Left("Cannot resolve module single"))
      'neg3 - check("", Left("Selector cannot be empty"))
    }
    'nested - {
      val check = MainTests.check(nestedModule, Discover[nestedModule.type]) _
      'pos1 - check("single", Right(Seq(_.single)))
      'pos2 - check("nested.single", Right(Seq(_.nested.single)))
      'pos3 - check("classInstance.single", Right(Seq(_.classInstance.single)))
      'neg1 - check("doesntExist", Left("Cannot resolve task doesntExist"))
      'neg2 - check("single.doesntExist", Left("Cannot resolve module single"))
      'neg3 - check("nested.doesntExist", Left("Cannot resolve task nested.doesntExist"))
      'neg4 - check("classInstance.doesntExist", Left("Cannot resolve task classInstance.doesntExist"))
      'wildcard - check(
        "_.single",
        Right(Seq(
          _.classInstance.single,
          _.nested.single
        ))
      )
      'wildcardNeg - check(
        "_._.single",
        Left("Cannot resolve module _")
      )
      'wildcardNeg2 - check(
        "_._.__",
        Left("Cannot resolve module _")
      )
      'wildcard2 - check(
        "__.single",
        Right(Seq(
          _.single,
          _.classInstance.single,
          _.nested.single
        ))
      )

      'wildcard3 - check(
        "_.__.single",
        Right(Seq(
          _.classInstance.single,
          _.nested.single
        ))
      )

    }
    'cross - {
      'single - {
        val check = MainTests.check(singleCross, Discover[singleCross.type]) _
        'pos1 - check("cross[210].suffix", Right(Seq(_.cross("210").suffix)))
        'pos2 - check("cross[211].suffix", Right(Seq(_.cross("211").suffix)))
        'neg1 - check("cross[210].doesntExist", Left("Cannot resolve task cross[210].doesntExist"))
        'neg2 - check("cross[doesntExist].doesntExist", Left("Cannot resolve cross cross[doesntExist]"))
        'neg2 - check("cross[doesntExist].suffix", Left("Cannot resolve cross cross[doesntExist]"))
        'wildcard - check(
          "cross[_].suffix",
          Right(Seq(
            _.cross("210").suffix,
            _.cross("211").suffix,
            _.cross("212").suffix
          ))
        )
        'wildcard2 - check(
          "cross[__].suffix",
          Right(Seq(
            _.cross("210").suffix,
            _.cross("211").suffix,
            _.cross("212").suffix
          ))
        )
      }
      'double - {
        val check = MainTests.check(doubleCross, Discover[doubleCross.type]) _
        'pos1 - check(
          "cross[210,jvm].suffix",
          Right(Seq(_.cross("210", "jvm").suffix))
        )
        'pos2 - check(
          "cross[211,jvm].suffix",
          Right(Seq(_.cross("211", "jvm").suffix))
        )
        'wildcard - {
          'labelNeg - check(
            "_.suffix",
            Left("Cannot resolve module _")
          )
          'labelPos - check(
            "__.suffix",
            Right(Seq(
              _.cross("210", "jvm").suffix,
              _.cross("210", "js").suffix,

              _.cross("211", "jvm").suffix,
              _.cross("211", "js").suffix,

              _.cross("212", "jvm").suffix,
              _.cross("212", "js").suffix,
              _.cross("212", "native").suffix
            ))
          )
          'first - check(
            "cross[_,jvm].suffix",
            Right(Seq(
              _.cross("210", "jvm").suffix,
              _.cross("211", "jvm").suffix,
              _.cross("212", "jvm").suffix
            ))
          )
          'second - check(
            "cross[210,_].suffix",
            Right(Seq(
              _.cross("210", "jvm").suffix,
              _.cross("210", "js").suffix
            ))
          )
          'both - check(
            "cross[_,_].suffix",
            Right(Seq(
              _.cross("210", "jvm").suffix,
              _.cross("210", "js").suffix,

              _.cross("211", "jvm").suffix,
              _.cross("211", "js").suffix,

              _.cross("212", "jvm").suffix,
              _.cross("212", "js").suffix,
              _.cross("212", "native").suffix
            ))
          )
          'both2 - check(
            "cross[__].suffix",
            Right(Seq(
              _.cross("210", "jvm").suffix,
              _.cross("210", "js").suffix,

              _.cross("211", "jvm").suffix,
              _.cross("211", "js").suffix,

              _.cross("212", "jvm").suffix,
              _.cross("212", "js").suffix,
              _.cross("212", "native").suffix
            ))
          )
        }
      }
      'nested - {
        val check = MainTests.check(nestedCrosses, Discover[nestedCrosses.type]) _
        'pos1 - check(
          "cross[210].cross2[js].suffix",
          Right(Seq(_.cross("210").cross2("js").suffix))
        )
        'pos2 - check(
          "cross[211].cross2[jvm].suffix",
          Right(Seq(_.cross("211").cross2("jvm").suffix))
        )
        'wildcard - {
          'first - check(
            "cross[_].cross2[jvm].suffix",
            Right(Seq(
              _.cross("210").cross2("jvm").suffix,
              _.cross("211").cross2("jvm").suffix,
              _.cross("212").cross2("jvm").suffix
            ))
          )
          'second - check(
            "cross[210].cross2[_].suffix",
            Right(Seq(
              _.cross("210").cross2("jvm").suffix,
              _.cross("210").cross2("js").suffix,
              _.cross("210").cross2("native").suffix
            ))
          )
          'both - check(
            "cross[_].cross2[_].suffix",
            Right(Seq(
              _.cross("210").cross2("jvm").suffix,
              _.cross("210").cross2("js").suffix,
              _.cross("210").cross2("native").suffix,

              _.cross("211").cross2("jvm").suffix,
              _.cross("211").cross2("js").suffix,
              _.cross("211").cross2("native").suffix,

              _.cross("212").cross2("jvm").suffix,
              _.cross("212").cross2("js").suffix,
              _.cross("212").cross2("native").suffix
            ))
          )
        }
      }
    }
  }
}
