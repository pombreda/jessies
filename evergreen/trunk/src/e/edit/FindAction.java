package e.edit;

import java.awt.*;
import java.awt.event.*;
import java.util.regex.*;
import e.ptextarea.*;
import e.util.*;

public class FindAction extends ETextAction implements MinibufferUser {
    public static final String ACTION_NAME = "Find...";
    
    public static final FindAction INSTANCE = new FindAction();
    
    public ETextWindow currentTextWindow;
    
    public String currentRegularExpression;
    
    private StringHistory regularExpressionHistory;
    
    private FindAction() {
        super(ACTION_NAME);
        putValue(ACCELERATOR_KEY, GuiUtilities.makeKeyStroke("F", false));
        regularExpressionHistory = new StringHistory(Edit.getInstance().getPreferenceFilename("e.edit.FindAction-history"));
    }
    
    public void actionPerformed(ActionEvent e) {
        ETextWindow newCurrentTextWindow = getFocusedTextWindow();
        if (newCurrentTextWindow == null) {
            return;
        }
        
        // Only now can we set currentTextWindow; otherwise "Find" while the
        // find mini-buffer is already up fails, and causes "Find Next" and
        // "Find Previous" to also fail.
        currentTextWindow = newCurrentTextWindow;
        Edit.getInstance().showMinibuffer(this);
    }
    
    //
    // MinibufferUser interface.
    //
    
    /** Stores the initial start of the selection so we can restore it if the user cancels the search. */
    private int initialSelectionStart;
    
    /** Stores the initial end of the selection so we can restore it if the user cancels the search. */
    private int initialSelectionEnd;
    
    public StringHistory getHistory() {
        return regularExpressionHistory;
    }
    
    public String getInitialValue() {
        ETextArea textArea = currentTextWindow.getText();
        initialSelectionStart = textArea.getSelectionStart();
        initialSelectionEnd = textArea.getSelectionEnd();
        String selectedText = textArea.getSelectedText();
        if (selectedText.length() == 0) {
            return currentRegularExpression;
        }
        return "(?-i)" + StringUtilities.regularExpressionFromLiteral(selectedText);
    }
    
    public String getPrompt() {
        return "Find";
    }
    
    /** Checks that we can compile the pattern okay. */
    public boolean isValid(String value) {
        try {
            Pattern.compile(value);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
    
    public void valueChangedTo(String value) {
        findAllMatches(value);
    }
    
    /**
     * Interprets C-D and C-G as requests to jump to the previous and next highlighted
     * matches, respectively.
     */
    public boolean interpretSpecialKeystroke(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_D) {
            currentTextWindow.getText().findPrevious();
            return true;
        }
        if (e.getKeyCode() == KeyEvent.VK_G) {
            currentTextWindow.getText().findNext();
            return true;
        }
        return false;
    }
    
    public boolean wasAccepted(String value) {
        return true;
    }
    
    public void wasCanceled() {
        removeAllMatches();
        currentRegularExpression = null;
        currentTextWindow.getText().select(initialSelectionStart, initialSelectionEnd);
    }
    
    //
    // Programmatic interface to finding.
    //
    
    public void findInText(ETextWindow textWindow, String regularExpression) {
        currentTextWindow = textWindow;
        findAllMatches(regularExpression);
    }
    
    public void repeatLastFind(ETextWindow textWindow) {
        currentTextWindow = textWindow;
        findAllMatches(currentRegularExpression);
    }
    
    //
    // Find stuff.
    //
    
    public void removeAllMatches() {
        currentTextWindow.getText().removeHighlights(PFind.MatchHighlight.HIGHLIGHTER_NAME);
        currentTextWindow.getBirdView().clearMatchingLines();
    }
    
    // FIXME: this could defer to PTextArea.findAllMatches if we:
    // 1. caught the PatternSyntaxException here
    // 2. used the match count returned by PTextArea
    // 3. had some way to get the line numbers of matches so we can tell the bird view (it might even be better than the current system if we can give them all at once as an int[])
    public void findAllMatches(String regularExpression) {
        removeAllMatches();
        
        // Do we have something to search for?
        if (regularExpression == null || regularExpression.length() == 0) {
            return;
        }
        
        // Do we have something to search in?
        ETextArea textArea = currentTextWindow.getText();
        String content = textArea.getText();
        if (content == null) {
            return;
        }
        
        currentRegularExpression = regularExpression;
        
        // Compile the regular expression.
        Pattern pattern;
        try {
            pattern = PatternUtilities.smartCaseCompile(regularExpression);
        } catch (PatternSyntaxException patternSyntaxException) {
            Edit.getInstance().showStatus(patternSyntaxException.getDescription());
            return;
        }
        
        // Find all the matches.
        int matchCount = 0;
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            currentTextWindow.getBirdView().addMatchingLine(textArea.getLineOfOffset(matcher.end()));
            textArea.addHighlight(new PFind.MatchHighlight(textArea, matcher.start(), matcher.end()));
            matchCount++;
        }
        Edit.getInstance().showStatus("Found " + matchCount + " " + (matchCount != 1 ? "matches" : "match") + " for \"" + regularExpression + "\"");
    }
}
