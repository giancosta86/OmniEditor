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

import java.nio.file.Files
import java.time.Duration
import javafx.beans.Observable
import javafx.beans.property.SimpleBooleanProperty
import javafx.fxml.FXML
import javafx.scene.control.{Button, MenuItem}
import javafx.stage.Stage

import info.gianlucacosta.helios.concurrency.AtomicStringBuilder
import info.gianlucacosta.helios.fx.dialogs.Alerts
import info.gianlucacosta.helios.fx.dialogs.FileChooserExtensions._

import scalafx.Includes._
import scalafx.beans.binding.Bindings
import scalafx.scene.control.Tooltip
import scalafx.scene.image.{Image, ImageView}
import scalafx.stage.FileChooser


object MainWindowController {
  private val outputRefreshRate = Duration.ofMillis(300)
}

/**
  * Internally used by the IDE framework
  */
class MainWindowController {
  private var stage: Stage = _
  private var appStrategy: AppStrategy = _

  private var codeEditor: StyledCodeEditor = _

  private var runningThread: Thread = _

  private var workspace: OmniWorkspace = _


  private val runningProperty = new SimpleBooleanProperty(false)

  def running: Boolean =
    runningProperty.get()

  private def running_=(newValue: Boolean): Unit =
    runningProperty.set(newValue)


  private var sourceFileChooser: FileChooser = _

  private var outputFileChooser: FileChooser = _


  /*
   * GUI SETUP
   */
  def init(stage: Stage, appStrategy: AppStrategy) {
    this.stage = stage
    this.appStrategy = appStrategy

    this.sourceFileChooser = appStrategy.createSourceFileChooser()
    this.codeEditor = appStrategy.createCodeEditor()

    this.outputFileChooser = new FileChooser {
      title = appStrategy.title

      extensionFilters.setAll(
        new FileChooser.ExtensionFilter("Text file", "*.txt"),
        new FileChooser.ExtensionFilter("Any file", "*.*")
      )
    }

    workspace = new OmniWorkspace(stage, sourceFileChooser, codeEditor)

    editorPane.setCenter(codeEditor)

    initBindings()
  }


  private def initBindings() {
    stage.titleProperty() <== Bindings.createStringBinding(
      () => {
        val titleBase = appStrategy.title

        val fileString =
          workspace.documentFile.map(
            file => " - " + file.getName
          ).getOrElse(
            ""
          )

        val modifiedString =
          if (workspace.modified) " *" else ""

        s"${titleBase}${fileString}${modifiedString}"
      },

      workspace.documentFileProperty,
      workspace.modifiedProperty
    )

    setupMenusAndToolbar()

    codeEditor.disableProperty <== runningProperty

    codeEditor.textProperty().addListener((observable: Observable) => {
      workspace.setModified()
    })
  }


  private def setupMenusAndToolbar() {
    newMenuItem.disableProperty <== runningProperty
    bindButton(newButton, newMenuItem)

    openMenuItem.disableProperty <== runningProperty
    bindButton(openButton, openMenuItem)


    saveMenuItem.disableProperty <== runningProperty || !workspace.modifiedProperty
    bindButton(saveButton, saveMenuItem)

    saveAsMenuItem.disableProperty <== runningProperty
    bindButton(saveAsButton, saveAsMenuItem)

    val undoAvailable = Bindings.createBooleanBinding(
      () => {
        codeEditor.isUndoAvailable
      },

      codeEditor.undoAvailableProperty
    )

    undoMenuItem.disableProperty <== runningProperty || !undoAvailable
    bindButton(undoButton, undoMenuItem)


    val redoAvailable = Bindings.createBooleanBinding(
      () => {
        codeEditor.isRedoAvailable
      },

      codeEditor.redoAvailableProperty
    )

    redoMenuItem.disableProperty <== runningProperty || !redoAvailable
    bindButton(redoButton, redoMenuItem)


    val noSelectedCodeText =
      Bindings.createBooleanBinding(
        () => {
          codeEditor.getSelectedText.isEmpty
        },

        codeEditor.selectedTextProperty()
      )


    cutMenuItem.disableProperty <== noSelectedCodeText || runningProperty
    bindButton(cutButton, cutMenuItem)

    copyMenuItem.disableProperty <== noSelectedCodeText || runningProperty
    bindButton(copyButton, copyMenuItem)

    pasteMenuItem.disableProperty <== runningProperty
    bindButton(pasteButton, pasteMenuItem)


    startMenuItem.disableProperty <== runningProperty
    bindButton(startButton, startMenuItem)

    stopMenuItem.disableProperty <== !runningProperty
    bindButton(stopButton, stopMenuItem)

    settingsMenuItem.setVisible(appStrategy.settingsSupported)
    settingsMenuItem.disableProperty <== runningProperty

    onlineReferenceMenuItem.disableProperty <== runningProperty
    bindButton(onlineReferenceButton, onlineReferenceMenuItem)

    aboutMenuItem.disableProperty <== runningProperty
    bindButton(aboutButton, aboutMenuItem)
  }


  private def bindButton(button: Button, menuItem: MenuItem) {
    val buttonId = button.getId

    val actionName = buttonId.substring(0, buttonId.lastIndexOf("Button"))
    val expectedMenuItemId = actionName + "MenuItem"

    if (menuItem.getId != expectedMenuItemId) {
      throw new IllegalArgumentException(
        s"'${menuItem.getId}' should be named '${expectedMenuItemId}' instead"
      )
    }


    val actionImage = new Image(getClass.getResourceAsStream(s"actionIcons/${actionName}.png"))

    menuItem.setGraphic(new ImageView(actionImage))
    button.setGraphic(new ImageView(actionImage))

    button.disableProperty <== menuItem.disableProperty
    button.setOnAction(menuItem.getOnAction)

    val tooltip = new Tooltip {
      text = menuItem.getText
    }
    button.setTooltip(tooltip)
  }


  /*
   * ACTIONS
   */

  def newDocument(): Unit = {
    workspace.newDocument()
  }


  def openDocument(): Unit = {
    workspace.openDocument()
  }


  def saveDocument(): Unit = {
    workspace.saveDocument()
  }


  def saveAsDocument(): Unit = {
    workspace.saveAsDocument()
  }


  def exitProgram(): Unit = {
    workspace.closeStage()
  }


  def undo(): Unit = {
    codeEditor.undo()
  }


  def redo(): Unit = {
    codeEditor.redo()
  }


  def cut(): Unit = {
    codeEditor.cut()
  }


  def copy(): Unit = {
    codeEditor.copy()
  }

  def paste(): Unit = {
    codeEditor.paste()
  }

  def selectAll(): Unit = {
    codeEditor.selectAll()
  }


  def saveOutput(): Unit = {
    val chosenFile = outputFileChooser.smartSave(stage)

    if (chosenFile == null) {
      return
    }

    try {
      Files.write(
        chosenFile.toPath,
        outputArea.getText().getBytes()
      )
    } catch {
      case ex: Exception =>
        Alerts.showException(ex)
    }
  }


  def start(): Unit = {
    outputArea.clear()
    running = true

    val outputBuffer = new AtomicStringBuilder


    val outputThread = new OutputThread(
      MainWindowController.outputRefreshRate,
      outputBuffer,
      outputString => outputArea.appendText(outputString)
    )
    outputThread.setDaemon(true)
    outputThread.start()


    new Thread {
      setDaemon(true)
      start()

      override def run(): Unit = {
        try {
          appStrategy.run(codeEditor.getText(), outputBuffer)
        } catch {
          case ex: InterruptedException =>
          //Just do nothing
        }


        outputThread.interrupt()
        try {
          outputThread.join()
        } catch {
          case ex: InterruptedException =>
          //Just do nothing
        }

        running = false
      }
    }
  }


  def stop() {
    runningThread.interrupt()
  }


  def showSettings() {
    appStrategy.showSettings()
  }

  def showOnlineReference() {
    appStrategy.showOnlineReference()
  }


  def showAboutWindow() {
    appStrategy.showAboutWindow()
  }


  @FXML
  var editorPane: javafx.scene.layout.BorderPane = _

  @FXML
  var newMenuItem: javafx.scene.control.MenuItem = _

  @FXML
  var openMenuItem: javafx.scene.control.MenuItem = _

  @FXML
  var saveMenuItem: javafx.scene.control.MenuItem = _

  @FXML
  var saveAsMenuItem: javafx.scene.control.MenuItem = _

  @FXML
  var undoMenuItem: javafx.scene.control.MenuItem = _

  @FXML
  var redoMenuItem: javafx.scene.control.MenuItem = _


  @FXML
  var cutMenuItem: javafx.scene.control.MenuItem = _

  @FXML
  var copyMenuItem: javafx.scene.control.MenuItem = _

  @FXML
  var pasteMenuItem: javafx.scene.control.MenuItem = _

  @FXML
  var saveOutputMenuItem: javafx.scene.control.MenuItem = _

  @FXML
  var startMenuItem: javafx.scene.control.MenuItem = _

  @FXML
  var stopMenuItem: javafx.scene.control.MenuItem = _

  @FXML
  var settingsMenuItem: javafx.scene.control.MenuItem = _

  @FXML
  var onlineReferenceMenuItem: javafx.scene.control.MenuItem = _

  @FXML
  var aboutMenuItem: javafx.scene.control.MenuItem = _


  @FXML
  var newButton: javafx.scene.control.Button = _

  @FXML
  var openButton: javafx.scene.control.Button = _

  @FXML
  var saveButton: javafx.scene.control.Button = _

  @FXML
  var saveAsButton: javafx.scene.control.Button = _


  @FXML
  var undoButton: javafx.scene.control.Button = _

  @FXML
  var redoButton: javafx.scene.control.Button = _

  @FXML
  var cutButton: javafx.scene.control.Button = _

  @FXML
  var copyButton: javafx.scene.control.Button = _

  @FXML
  var pasteButton: javafx.scene.control.Button = _


  @FXML
  var startButton: javafx.scene.control.Button = _

  @FXML
  var stopButton: javafx.scene.control.Button = _


  @FXML
  var onlineReferenceButton: javafx.scene.control.Button = _

  @FXML
  var aboutButton: javafx.scene.control.Button = _

  @FXML
  var outputArea: javafx.scene.control.TextArea = _
}
