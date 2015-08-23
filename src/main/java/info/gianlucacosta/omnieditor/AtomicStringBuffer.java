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

/**
 * Provides an atomic buffer for writing and retrieving strings
 */
public class AtomicStringBuffer {
    private final StringBuilder internalBuffer = new StringBuilder();

    /**
     * Atomically sends a string to the buffer
     *
     * @param string The string to print out
     */
    public synchronized void print(String string) {
        internalBuffer.append(string);
    }

    /**
     * Atomically sends a string to the buffer, followed by the newline character
     *
     * @param string The line to print out
     */
    public synchronized void println(String string) {
        print(string + "\n");
    }


    /**
     * Atomically retrieves the text stored in the buffer, then clearing the buffer
     *
     * @return The text within the buffer
     */
    public synchronized String getTextAndClear() {
        String result = internalBuffer.toString();

        internalBuffer.setLength(0);
        return result;
    }
}
