package e.edit;

import e.gui.*;
import e.ptextarea.*;
import e.util.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

/**
 * Opens the file corresponding to the selected import.
 * 
 * For C++, this would be the header file corresponding to the selected #include.
 * For Java, this would be the source file corresponding to the selected import.
 */
public class OpenImportAction extends ETextAction {
    public OpenImportAction() {
        super("Open Import...");
        putValue(ACCELERATOR_KEY, e.util.GuiUtilities.makeKeyStroke("O", true));
    }
    
    public void actionPerformed(ActionEvent e) {
        final Evergreen editor = Evergreen.getInstance();
        final FileType fileType = getFocusedTextWindow().getFileType();
        
        // FIXME: if there's no selection, take the whole of the current line.
        String path = getSelectedText().trim();
        
        // Rewrite boilerplate to get a path fragment.
        if (fileType == FileType.C_PLUS_PLUS) {
            // Get rid of "#include <...>" in C++.
            path = path.replaceAll("^\\s*#\\s*include\\s+[<\"]([^>\"]+)[\">].*", "$1");
        } else if (fileType == FileType.JAVA) {
            // Get rid of "import ...;", rewrite '.' as File.separatorChar, and append ".java".
            path = path.replaceAll("^\\s*import\\s+(.+)\\s*;.*", "$1");
            path = path.replace('.', '/') + ".java";
        }
        
        if (path.startsWith("~") || path.startsWith("/")) {
            // If we have an absolute name, we can go straight there.
            // The user probably meant to use "Open Quickly", but there's no need to punish them.
            editor.openFile(path);
            return;
        }
        
        // FIXME: use per-language import paths. This temporarily hard-coded one obviously only applies to C++.
        // FIXME: allow the path(s) to be overridden.
        // FIXME: we should probably allow "." and interpret it as "the directory containing the current file". maybe just implicitly always check there first?
        List<String> importPath = Arrays.asList("/usr/include/:/usr/include/c++/4.2/:native/Headers/".split(":"));
        for (String importDir : importPath) {
            File file;
            if (importDir.startsWith("/")) {
                file = FileUtilities.fileFromParentAndString(importDir, path);
            } else {
                // We interpret non-absolute paths as being rooted at the current workspace's root.
                String root = editor.getCurrentWorkspace().getCanonicalRootDirectory();
                file = new File(FileUtilities.fileFromParentAndString(root, importDir), path);
            }
            
            if (file.exists()) {
                // Even if there would be multiple matches, we just take the first one we come across.
                editor.openFile(file.toString());
                return;
            }
        }
        editor.showAlert("Couldn't open imported file", "There was no file \"" + path + "\" under any of the directories on the import path:\n" + StringUtilities.join(importPath, ":"));
    }
}