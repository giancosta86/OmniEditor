/*§
  ===========================================================================
  OmniEditor
  ===========================================================================
  Copyright (C) 2015-2016 Gianluca Costa
  ===========================================================================
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
  ===========================================================================
*/

package info.gianlucacosta.omnieditor

import java.io.File
import java.net.URL

import info.gianlucacosta.helios.concurrency.AtomicStringBuilder

import scalafx.stage.FileChooser

/**
  * Strategy employed to define customized IDE behaviour
  */
trait AppStrategy {
  /**
    * Application title, shown in windows and dialogs.
    *
    * @return The application title
    */
  def title: String

  /**
    * Creates the file chooser employed when opening and saving source files.
    *
    * @return The file chooser
    */
  def createSourceFileChooser(): FileChooser

  /**
    * Creates the code editor for source files.
    *
    * @return The code editor
    */
  def createCodeEditor(): StyledCodeEditor

  /**
    * Computes the actual file after the user has confirmed a file when saving the source code.
    * For example, the function can add a default file extension if the user did not.
    *
    * @param sourceFileChooser The file chooser used to save the file
    * @param selectedFile      The file originally saved by the user
    * @return The actual target file
    */
  def getSavedSourceFile(sourceFileChooser: FileChooser, selectedFile: File): File

  /**
    * Tells the IDE whether to show the "Settings" menu item
    *
    * @return true if the "Settings" menu item should be shown
    */
  def settingsSupported: Boolean

  /**
    * Shows a settings dialog. Can do nothing if settingsSupported() returns false
    */
  def showSettings(): Unit

  /**
    * Runs the current program, probably by employing a dedicated compiler/interpreter.
    * <p>
    * Please, design such executor in order to support thread interruption
    * (that is, by checking for Thread.interrupted()), as the user might want to
    * stop the program, which consists in calling Thread.interrupt() on the thread
    * executing this run() method.
    *
    * Throws InterruptedException: such exception can be thrown by the custom language's
    * virtual machine upon receiving a Thread.interrupt()
    *
    * @param programCode  The source code in the editor
    * @param outputBuffer The buffer to write output to
    */
  @throws[InterruptedException]
  def run(programCode: String, outputBuffer: AtomicStringBuilder)

  /**
    * Retrieves the URL of the CSS file employed to style the code editor
    *
    * @return The CSS url
    */
  def syntaxCss: URL

  /**
    * Shows online reference (for example, opens a web page)
    */
  def showOnlineReference(): Unit

  /**
    * Shows the "About..." window
    */
  def showAboutWindow(): Unit
}
