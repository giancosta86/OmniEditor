/*§
  ===========================================================================
  OmniEditor
  ===========================================================================
  Copyright (C) 2015 Gianluca Costa
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

package info.gianlucacosta.omnieditor;

import javafx.stage.FileChooser;
import javafx.util.Pair;

import java.io.File;
import java.net.URL;

/**
 * Strategy employed to define customized IDE behaviour
 */
public interface AppStrategy {
    /**
     * Application title, shown in windows and dialogs.
     *
     * @return The application title
     */
    String getAppTitle();


    /**
     * Creates the file chooser employed when opening and saving source files.
     *
     * @return The file chooser
     */
    FileChooser createSourceFileChooser();

    /**
     * Creates the code editor for source files.
     *
     * @return The code editor
     */
    StyledCodeEditor createCodeEditor();


    /**
     * Computes the actual file after the user has confirmed a file when saving the source code.
     * For example, the function can add a default file extension if the user did not.
     *
     * @param sourceFileChooser The file chooser used to save the file
     * @param selectedFile      The file originally saved by the user
     * @return The actual target file
     */
    File getSavedSourceFile(FileChooser sourceFileChooser, File selectedFile);

    /**
     * Tells the IDE whether to show the "Settings" menu item
     *
     * @return true if the "Settings" menu item should be shown
     */
    boolean isShowSettings();

    /**
     * Shows a settings dialog. Can do nothing if isShowSettings() returns false
     */
    void showSettings();

    /**
     * Runs the current program, probably by employing a dedicated compiler/interpreter.
     * <p>
     * Please, design such executor in order to support thread interruption
     * (that is, by checking for Thread.interrupted()), as the user might want to
     * stop the program, which consists in calling Thread.interrupt() on the thread
     * executing this run() method.
     *
     * @param programCode  The source code in the editor
     * @param outputBuffer The buffer to write output to
     * @throws InterruptedException Can be thrown by the custom language's virtual machine upon receiving a Thread.interrupt()
     */
    void run(String programCode, AtomicStringBuffer outputBuffer) throws InterruptedException;

    /**
     * Retrieves the URL of the CSS file employed to style the code editor
     *
     * @return The CSS url
     */
    URL getSyntaxCss();


    /*
     * Shows online reference (for example, opens a web page)
     */
    void showOnlineReference();

    /**
     * Shows the "About..." window
     */
    void showAboutWindow();
}
