package org.yinwang.pysonar.demos;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.Analyzer;
import org.yinwang.pysonar.ast.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;


/**
 * Decorates Python source with style runs from the index.
 */
class Styler extends DefaultNodeVisitor {

    static final Pattern BUILTIN =
            Pattern.compile("None|True|False|NotImplemented|Ellipsis|__debug__");

    /**
     * Matches the start of a triple-quote string.
     */
    private static final Pattern TRISTRING_PREFIX =
            Pattern.compile("^[ruRU]{0,2}['\"]{3}");

    private Analyzer analyzer;
    private String source;

    @NotNull
    private List<StyleRun> styles = new ArrayList<>();

    /**
     * Offsets of doc strings found by node visitor.
     */
    @NotNull
    private Set<Integer> docOffsets = new HashSet<>();


    public Styler(Analyzer idx) {
        this.analyzer = idx;
    }


    /**
     * Entry point for decorating a source file.
     *
     * @param path absolute file path
     * @param src  file contents
     */
    @NotNull
    public List<StyleRun> addStyles(String path, String src) {
        source = src;
        Node node = analyzer.getAstForFile(path);
        if (node != null) {
            node.visit(this);
        }
        return styles;
    }


    @Override
    public boolean visit(@NotNull Name n) {
        Node parent = n.getParent();
        if (parent instanceof Function) {
            Function fn = (Function) parent;
            if (n == fn.name) {
                addStyle(n, StyleRun.Type.FUNCTION);
            } else if (n == fn.kwarg || n == fn.vararg) {
                addStyle(n, StyleRun.Type.PARAMETER);
            }
            return true;
        }

        if (BUILTIN.matcher(n.id).matches()) {
            addStyle(n, StyleRun.Type.BUILTIN);
            return true;
        }

        return true;
    }


    @Override
    public boolean visit(Num n) {
        addStyle(n, StyleRun.Type.NUMBER);
        return true;
    }


    @Override
    public boolean visit(@NotNull Str n) {
        String s = sourceString(n.start, n.end);
        if (TRISTRING_PREFIX.matcher(s).lookingAt()) {
            addStyle(n.start, n.end - n.start, StyleRun.Type.DOC_STRING);
            docOffsets.add(n.start);  // don't re-highlight as a string
//            highlightDocString(n);
        }
        return true;
    }


    private void addStyle(@NotNull Node e, int start, int len, StyleRun.Type type) {
        if (e.getFile() != null) {  // if it's an NUrl, for instance
            addStyle(start, len, type);
        }
    }


    private void addStyle(@NotNull Node e, StyleRun.Type type) {
        addStyle(e, e.start, e.end - e.start, type);
    }


    private void addStyle(int begin, int len, StyleRun.Type type) {
        styles.add(new StyleRun(type, begin, len));
    }


    private String sourceString(@NotNull Node e) {
        return sourceString(e.start, e.end);
    }


    private String sourceString(int begin, int end) {
        int a = Math.max(begin, 0);
        int b = Math.min(end, source.length());
        b = Math.max(b, 0);
        try {
            return source.substring(a, b);
        }
        catch (StringIndexOutOfBoundsException sx) {
            // Silent here, only happens for weird encodings in file
            return "";
        }
    }
}
