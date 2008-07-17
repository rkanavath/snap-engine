package binio.expr;

import binio.CompoundData;

import java.io.IOException;

public class IntReferenceExpr extends ReferenceExpr {
    public IntReferenceExpr(String name) {
        super(name);
    }

    public IntReferenceExpr(int index) {
        super(index);
    }

    public Object evaluate(CompoundData context) throws IOException {
        return context.getInt(getIndex(context));
    }
}
