package mill.util

import ammonite.ops.Path
import mill.define.{Discover, Input, Target, Task}
import mill.eval.{Evaluator, Result}
import mill.util.Strict.Agg
class TestEvaluator(module: mill.Module,
                    discover: Discover,
                    workspacePath: Path,
                    basePath: Path){
  val evaluator = new Evaluator(workspacePath, basePath, module, discover, DummyLogger)
  //val evaluator = new Evaluator(workspacePath, basePath, module, new PrintLogger(true, ammonite.util.Colors.Default, System.out, System.out, System.err))
  def apply[T](t: Task[T]): Either[Result.Failing, (T, Int)] = {
    val evaluated = evaluator.evaluate(Agg(t))

    if (evaluated.failing.keyCount == 0) {
      Right(
        Tuple2(
          evaluated.rawValues.head.asInstanceOf[Result.Success[T]].value,
          evaluated.evaluated.collect {
            case t: Target[_] if module.millInternal.targets.contains(t) && !t.isInstanceOf[Input[_]] => t
            case t: mill.define.Command[_]           => t
          }.size
        ))
    } else {
      Left(
        evaluated.failing.lookupKey(evaluated.failing.keys().next).items.next())
    }
  }

}
