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

import java.time.Duration
import java.util
import java.util.concurrent.Semaphore
import java.util.regex.Pattern
import javafx.beans.Observable
import javafx.event.EventHandler
import javafx.scene.input.{KeyCode, KeyEvent}

import org.fxmisc.richtext.{CodeArea, LineNumberFactory, StyleSpans, StyleSpansBuilder}

import scala.collection.JavaConversions._
import scalafx.Includes._
import scalafx.application.Platform


object StyledCodeEditor {
  private val leadingSpacePattern = Pattern.compile(raw"^\s*")
}

/**
  * Code editor enabling easy pattern-based syntax highlighting.
  * <p>
  * This class derives from and extends the JavaKeywords demo of the CodeArea control.
  */
class StyledCodeEditor(stylingSleepDuration: Duration = Duration.ofMillis(334)) extends CodeArea {
  private var syntaxPattern = Pattern.compile("")
  private var styles = Map[String, Style]()
  private var descriptorKeyCounter = 0

  private var tabReplacementString: Option[String] = None


  setParagraphGraphicFactory(LineNumberFactory.get(this))


  private val styleDaemon = new StyleDaemon(this, stylingSleepDuration) {
    start()
  }


  /**
    * Binds a pattern to a CSS class.
    * <p>
    * Consequently, such class will style code portions matching the pattern.
    * Patterns are applied in registration (FIFO) order, stopping when a pattern matches.
    *
    * @param cssClass     the CSS class used to style text matching the pattern
    * @param regexPattern a regular expression pattern used to match text in the editor
    */
  def addPattern(cssClass: String, regexPattern: String) {
    val key = "S" + descriptorKeyCounter
    descriptorKeyCounter += 1

    val style = Style(cssClass, regexPattern)
    styles += (key -> style)

    updateSyntaxPattern()
  }


  /**
    * Simplified version of addPattern(), focusing on <em>tokens</em> (for example, keywords)
    *
    * @param cssClass the CSS class used to style the tokens
    * @param tokens   the tokens to style
    */
  def addTokens(cssClass: String, tokens: String*) {
    val tokensPattern =
      String.join("|",
        tokens
          .map(token => s"\\b${Pattern.quote(token)}\\b")
      )

    addPattern(cssClass, tokensPattern)
  }


  private def updateSyntaxPattern(): Unit = {
    val patternBuilder = new StringBuilder()

    styles.foreach { case (key, style) =>
      patternBuilder.append(s"(?<${key}>${style.pattern})|")
    }

    if (patternBuilder.nonEmpty) {
      patternBuilder.deleteCharAt(patternBuilder.length - 1)
    }

    syntaxPattern = Pattern.compile(patternBuilder.toString)
  }


  private def computeHighlighting(text: String): StyleSpans[util.Collection[String]] = {
    val spansBuilder = new StyleSpansBuilder[util.Collection[String]]

    val matcher = syntaxPattern.matcher(text)
    var lastMatchEndPosition = 0

    while (matcher.find()) {
      val styleKey =
        styles
          .keySet
          .filter(key => matcher.group(key) != null)
          .head

      val styleClass = styles(styleKey).cssClass

      spansBuilder.add(Seq(), matcher.start() - lastMatchEndPosition)
      spansBuilder.add(Seq(styleClass), matcher.end() - matcher.start())
      lastMatchEndPosition = matcher.end()
    }
    spansBuilder.add(Seq(), text.length() - lastMatchEndPosition)
    spansBuilder.create()
  }


  def setText(text: String) {
    clear()
    replaceText(0, 0, text)
  }

  /**
    * When the user presses ENTER, initial space is added to the new line
    * to keep it aligned with the previous one.
    */
  def enableIndentedNewline() {
    addEventHandler(KeyEvent.KEY_PRESSED, new EventHandler[KeyEvent] {
      override def handle(keyEvent: KeyEvent): Unit = {
        keyEvent.getCode match {
          case KeyCode.ENTER =>
            val currentPosition = getCaretPosition

            val previousText = getText.substring(0, currentPosition)
            val previousNewlineCharPosition = previousText.lastIndexOf('\n')

            val currentLine = previousText.substring(previousNewlineCharPosition + 1)

            val leadingSpaceMatcher = StyledCodeEditor.leadingSpacePattern.matcher(currentLine)

            if (leadingSpaceMatcher.find()) {
              val leadingSpace = leadingSpaceMatcher.group

              keyEvent.consume()

              val totalSpaceToInsert = "\n" + leadingSpace
              insertText(currentPosition, totalSpaceToInsert)

              moveTo(
                currentPosition + totalSpaceToInsert.length
              )
            }

          case _ =>
        }
      }
    })
  }

  /**
    * Pressing the "Tab" key will only add the "space" character, for the given number of times
    *
    * @param spaceCharacterCount The number of "space" characters to add in lieu of "\t"
    */
  def enableDynamicTabs(spaceCharacterCount: Int) {
    require(spaceCharacterCount >= 0)

    require(tabReplacementString.isEmpty)

    tabReplacementString = Some(" " * spaceCharacterCount)

    addEventHandler(KeyEvent.KEY_PRESSED, new EventHandler[KeyEvent] {
      override def handle(keyEvent: KeyEvent): Unit = {
        keyEvent.getCode match {
          case KeyCode.TAB =>
            keyEvent.consume()

            val currentPosition = getCaretPosition

            insertText(currentPosition, tabReplacementString.get)

            moveTo(
              currentPosition + spaceCharacterCount
            )

          case _ =>
        }
      }
    })
  }


  private var stylingEnabled: Boolean = true


  /**
    * Stops the background thread employed for styling the editor
    */
  def stopStyling(): Unit = {
    stylingEnabled = false
  }


  private class StyleDaemon(codeEditor: StyledCodeEditor, sleepDuration: Duration) extends Thread {
    setDaemon(true)


    override def run(): Unit = {
      var text = ""
      var latestStyledText = ""

      val guiSemaphore = new Semaphore(0)


      while (codeEditor.stylingEnabled) {
        Platform.runLater {
          text =
            codeEditor.getText()

          guiSemaphore.release()
        }


        guiSemaphore.acquire()


        if (text != latestStyledText) {
          val styleSpans =
            computeHighlighting(text)

          Platform.runLater {
            try {
              codeEditor.setStyleSpans(0, styleSpans)
              latestStyledText = text
            } catch {
              case _: Exception =>
              //Just do nothing
            }

            guiSemaphore.release()
          }

          guiSemaphore.acquire()
        }


        Thread.sleep(sleepDuration.toMillis)
      }
    }


    private def computeHighlighting(text: String): StyleSpans[util.Collection[String]] = {
      val spansBuilder = new StyleSpansBuilder[util.Collection[String]]

      val matcher = syntaxPattern.matcher(text)
      var latestMatchEndPosition = 0

      while (matcher.find()) {
        val styleKey =
          styles
            .keySet
            .filter(key => matcher.group(key) != null)
            .head

        val styleClass = styles(styleKey).cssClass

        spansBuilder.add(Seq(), matcher.start() - latestMatchEndPosition)
        spansBuilder.add(Seq(styleClass), matcher.end() - matcher.start())
        latestMatchEndPosition = matcher.end()
      }
      spansBuilder.add(Seq(), text.length() - latestMatchEndPosition)
      spansBuilder.create()
    }
  }

}