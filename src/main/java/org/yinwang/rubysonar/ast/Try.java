package org.yinwang.rubysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.rubysonar.Analyzer;
import org.yinwang.rubysonar.State;
import org.yinwang.rubysonar.types.Type;
import org.yinwang.rubysonar.types.UnionType;

import java.util.List;


public class Try extends Node {

    public List<Handler> handlers;
    public Block body;
    public Block orelse;
    public Block finalbody;

    public Try(List<Handler> handlers, Block body, Block orelse, Block finalbody,
               int start, int end) {
        super(start, end);
        this.handlers = handlers;
        this.body = body;
        this.orelse = orelse;
        this.finalbody = finalbody;
        addChildren(handlers);
        addChildren(body, orelse);
    }


    @NotNull
    @Override
    public Type transform(State s) {
        Type tp1 = Type.UNKNOWN;
        Type tp2 = Type.UNKNOWN;
        Type tph = Type.UNKNOWN;
        Type tpFinal = Type.UNKNOWN;

        if (handlers != null) {
            for (Handler h : handlers) {
                tph = UnionType.union(tph, transformExpr(h, s));
            }
        }

        if (body != null) {
            tp1 = transformExpr(body, s);
        }

        if (orelse != null) {
            tp2 = transformExpr(orelse, s);
        }

        if (finalbody != null) {
            tpFinal = transformExpr(finalbody, s);
        }

        return new UnionType(tp1, tp2, tph, tpFinal);
    }


    @NotNull
    @Override
    public String toString() {
        return "<Try:" + handlers + ":" + body + ":" + orelse + ">";
    }


    @Override
    public void visit(@NotNull NodeVisitor v) {
        if (v.visit(this)) {
            visitNodes(handlers, v);
            visitNode(body, v);
            visitNode(orelse, v);
        }
    }
}
