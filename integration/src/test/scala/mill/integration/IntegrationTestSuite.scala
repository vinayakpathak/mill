package mill.integration

import ammonite.ops._
import utest._

abstract class IntegrationTestSuite(repoKey: String, workspaceSlug: String) extends TestSuite{
  val workspacePath = pwd / 'target / 'workspace / workspaceSlug
  val buildFilePath = pwd / 'integration / 'src / 'test / 'resource / workspaceSlug
  val runner = new mill.main.MainRunner(ammonite.main.Cli.Config(wd = workspacePath), false)
  def eval(s: String*) = runner.runScript(workspacePath / "build.sc", s.toList)
  def initWorkspace() = {
    rm(workspacePath)
    mkdir(workspacePath / up)
    // The unzipped git repo snapshots we get from github come with a
    // wrapper-folder inside the zip file, so copy the wrapper folder to the
    // destination instead of the folder containing the wrapper.
    val path = sys.props(repoKey)
    val Seq(wrapper) = ls(Path(path))
    cp(wrapper, workspacePath)
    cp(buildFilePath / "build.sc", workspacePath / "build.sc")
    assert(!ls.rec(workspacePath).exists(_.ext == "class"))
  }
}