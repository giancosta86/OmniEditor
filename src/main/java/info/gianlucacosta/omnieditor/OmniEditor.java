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

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Pair;

import java.io.IOException;
import java.net.URL;


/**
 * Ready-made minimal IDE for custom languages.
 * <p>
 * In the start() method of your JavaFX application, just instantiate this class and call
 * its start() methods: the IDE will appear.
 */
public class OmniEditor {
    private final AppStrategy appStrategy;


    public OmniEditor(AppStrategy appStrategy) {
        this.appStrategy = appStrategy;
    }


    public void start(Stage primaryStage) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("MainWindow.fxml"));

        Parent root;
        try {
            root = loader.load();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        URL syntaxCssUrl = appStrategy.getSyntaxCss();

        MainWindowController mainWindowController = loader.getController();

        Scene scene = new Scene(root);
        scene.getStylesheets().add(syntaxCssUrl.toExternalForm());

        mainWindowController.init(primaryStage, appStrategy);


        Platform.runLater(() -> {
            primaryStage.setScene(scene);
            primaryStage.show();
        });
    }
}
