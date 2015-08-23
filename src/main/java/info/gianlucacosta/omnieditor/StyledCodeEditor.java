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

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.StyleSpans;
import org.fxmisc.richtext.StyleSpansBuilder;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Code editor enabling easy pattern-based syntax highlighting.
 * <p>
 * This class derives from and extends the JavaKeywords demo of the CodeArea control.
 */
public class StyledCodeEditor extends CodeArea {
    private static final Pattern leadingSpacePattern = Pattern.compile("^\\s*");

    private Pattern syntaxPattern = Pattern.compile("");
    private final Map<String, Style> styles = new LinkedHashMap<>();
    private int descriptorKeyCounter = 0;

    private Optional<String> tabReplacementString = Optional.empty();


    public StyledCodeEditor() {
        setParagraphGraphicFactory(LineNumberFactory.get(this));

        textProperty().addListener((obs, oldText, newText) -> {
            setStyleSpans(0, computeHighlighting(newText));
        });
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
    public void addPattern(String cssClass, String regexPattern) {
        String key = "S" + descriptorKeyCounter;
        descriptorKeyCounter++;

        Style style = new Style(cssClass, regexPattern);
        styles.put(key, style);

        updateSyntaxPattern();
    }


    /**
     * Simplified version of addPattern(), but focusing on <em>tokens</em> (for example, keywords)
     *
     * @param cssClass the CSS class use to style the tokens
     * @param tokens   the tokens to style
     */
    public void addTokens(String cssClass, String... tokens) {
        String tokensPattern =
                Stream.of(tokens)
                        .map(token -> String.format("\\b%s\\b", Pattern.quote(token)))
                        .collect(Collectors.joining("|"));

        addPattern(cssClass, tokensPattern);
    }


    private void updateSyntaxPattern() {
        StringBuilder patternBuilder = new StringBuilder();

        for (Map.Entry<String, Style> item : styles.entrySet()) {
            String key = item.getKey();
            Style style = item.getValue();

            patternBuilder.append(String.format("(?<%s>%s)|", key, style.pattern));
        }


        if (patternBuilder.length() > 0) {
            patternBuilder.deleteCharAt(patternBuilder.length() - 1);
        }

        syntaxPattern = Pattern.compile(patternBuilder.toString());
    }


    private StyleSpans<Collection<String>> computeHighlighting(String text) {
        Matcher matcher = syntaxPattern.matcher(text);
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder
                = new StyleSpansBuilder<>();
        while (matcher.find()) {
            String styleClass = null;
            for (String key : styles.keySet()) {
                if (matcher.group(key) != null) {
                    styleClass = styles.get(key).cssClass;
                    break;
                }
            }

            spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
            spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastKwEnd = matcher.end();
        }
        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        return spansBuilder.create();
    }

    public void setText(String text) {
        clear();
        replaceText(0, 0, text);
    }

    /**
     * When the user presses ENTER, initial space is added to the new line
     * to keep it aligned with the previous one.
     */
    public void enableSmartNewline() {
        addEventHandler(KeyEvent.KEY_PRESSED, keyEvent -> {
            if (keyEvent.getCode() == KeyCode.ENTER) {
                int currentPosition = getCaretPosition();

                String previousText = getText().substring(0, currentPosition);
                int previousNewlineCharPosition = previousText.lastIndexOf('\n');

                String currentLine = previousText.substring(previousNewlineCharPosition + 1);


                Matcher leadingSpaceMatcher = leadingSpacePattern.matcher(currentLine);

                if (leadingSpaceMatcher.find()) {
                    String leadingSpace = leadingSpaceMatcher.group();

                    keyEvent.consume();

                    String totalSpaceToInsert = "\n" + leadingSpace;
                    insertText(currentPosition, totalSpaceToInsert);

                    moveTo(
                            currentPosition + totalSpaceToInsert.length()
                    );
                }
            }
        });
    }

    /**
     * Pressing the "Tab" key will only add the "space" character, for the given number of times
     *
     * @param spaceCharacterCount The number of "space" characters to add in lieu of "\t"
     */
    public void enableDynamicTabs(int spaceCharacterCount) {
        if (tabReplacementString.isPresent()) {
            throw new IllegalStateException();
        }

        if (spaceCharacterCount < 0) {
            throw new IllegalArgumentException();
        }

        StringBuilder spaceStringBuilder = new StringBuilder();

        for (int i = 1; i <= spaceCharacterCount; i++) {
            spaceStringBuilder.append(" ");
        }

        tabReplacementString = Optional.of(spaceStringBuilder.toString());


        addEventHandler(KeyEvent.KEY_PRESSED, keyEvent -> {
            if (keyEvent.getCode() == KeyCode.TAB) {
                keyEvent.consume();

                int currentPosition = getCaretPosition();

                insertText(currentPosition, tabReplacementString.get());

                moveTo(
                        currentPosition + spaceCharacterCount
                );
            }
        });
    }

    private class Style {
        public final String cssClass;
        public final String pattern;

        public Style(String cssClass, String pattern) {
            this.cssClass = cssClass;
            this.pattern = pattern;
        }
    }
}