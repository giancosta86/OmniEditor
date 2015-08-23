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

import java.util.function.Consumer;


class OutputThread extends Thread {
    private final int refreshRateInMilliseconds;
    private final AtomicStringBuffer outputBuffer;
    private final Consumer<String> outputAction;

    public OutputThread(
            int refreshRateInMilliseconds,
            AtomicStringBuffer outputBuffer,
            Consumer<String> outputAction) {
        this.refreshRateInMilliseconds = refreshRateInMilliseconds;
        this.outputBuffer = outputBuffer;
        this.outputAction = outputAction;
    }


    @Override
    public void run() {
        while (!Thread.interrupted()) {
            tryToOutput();

            try {
                Thread.sleep(refreshRateInMilliseconds);
            } catch (InterruptedException e) {
                break;
            }
        }

        tryToOutput();
    }


    private void tryToOutput() {
        String textToOutput = outputBuffer.getTextAndClear();
        if (!textToOutput.isEmpty()) {
            Platform.runLater(() -> {
                outputAction.accept(textToOutput);
            });
        }
    }
}
