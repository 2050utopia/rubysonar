package org.yinwang.rubysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.rubysonar.State;
import org.yinwang.rubysonar.types.Type;
import org.yinwang.rubysonar.types.UnionType;


public class If extends Node {

    @NotNull
    public Node test;
    public Node body;
    public Node orelse;


    public If(@NotNull Node test, Node body, Node orelse, int start, int end) {
        super(start, end);
        this.test = test;
        this.body = body;
        this.orelse = orelse;
        addChildren(test, body, orelse);
    }


    @NotNull
    @Override
    public Type transform(@NotNull State s) {
        Type type1, type2;
        State s1 = s.copy();
        State s2 = s.copy();

        Type conditionType = transformExpr(test, s);
        if (conditionType.isUndecidedBool()) {
            s1 = conditionType.asBool().getS1();
            s2 = conditionType.asBool().getS2();
        }

        if (body != null) {
            type1 = transformExpr(body, s1);
        } else {
            type1 = Type.CONT;
        }

        if (orelse != null) {
            type2 = transformExpr(orelse, s2);
        } else {
            type2 = Type.CONT;
        }

        boolean cont1 = UnionType.contains(type1, Type.CONT);
        boolean cont2 = UnionType.contains(type2, Type.CONT);

        // decide which branch affects the downstream state
        if (conditionType.isTrue() && cont1) {
            s.overwrite(s1);
        } else if (conditionType.isFalse() && cont2) {
            s.overwrite(s2);
        } else if (cont1 && cont2) {
            s.overwrite(State.merge(s1, s2));
        } else if (cont1) {
            s.overwrite(s1);
        } else if (cont2) {
            s.overwrite(s2);
        }

        // determine return type
        if (conditionType == Type.TRUE) {
            return type1;
        } else if (conditionType == Type.FALSE) {
            return type2;
        } else {
            return UnionType.union(type1, type2);
        }
    }


    @NotNull
    @Override
    public String toString() {
        return "(if:" + start + ":" + test + ":" + body + ":" + orelse + ")";
    }


}
