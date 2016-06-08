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
import java.nio.file.Files
import javafx.stage.Stage

import info.gianlucacosta.helios.fx.workspace.Workspace

import scalafx.stage.FileChooser

private class OmniWorkspace(stage: Stage, documentFileChooser: FileChooser, codeEditor: StyledCodeEditor) extends Workspace(stage, documentFileChooser) {
  override def doNew(): Boolean = {
    codeEditor.clear()

    codeEditor.getUndoManager.forgetHistory()
    codeEditor.requestFocus()

    true
  }


  override def doOpen(sourceFile: File): Boolean = {
    val fileContent = new String(
      Files.readAllBytes(sourceFile.toPath),
      "utf-8"
    )

    codeEditor.setText(fileContent)

    codeEditor.getUndoManager.forgetHistory()
    codeEditor.requestFocus()

    true
  }


  override def doSave(targetFile: File): Boolean = {
    Files.write(
      targetFile.toPath,
      codeEditor.getText.getBytes("utf-8")
    )

    true
  }
}
