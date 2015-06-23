package com.bc.jexp.impl;

import com.bc.ceres.core.Assert;
import com.bc.jexp.EvalEnv;
import com.bc.jexp.EvalException;
import com.bc.jexp.Function;
import com.bc.jexp.Namespace;
import com.bc.jexp.ParseException;
import com.bc.jexp.Symbol;
import com.bc.jexp.Term;
import com.bc.jexp.TermConverter;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Created by Norman on 21.06.2015.
 */
public class RuleTest {

    @Test
    public void testApply() throws Exception {
        assertEquals("A", applyRule("X --> X",
                                    "A"));
        assertEquals("Add(A,1)", applyRule("X --> X + 1",
                                           "A"));


        assertEquals(null, applyRule("X + Y --> Y",
                                     "A"));
        assertEquals(null, applyRule("X + (Y + Z) --> (X + Y) + Z",
                                     "A + 2 + A"));
        assertEquals("Add(Add(A,2),A)", applyRule("X + (Y + Z) --> (X + Y) + Z",
                                                  "A + (2 + A)"));


        assertEquals("Add(Mul(0.5,0.5),A)", applyRule("(C * X) * C --> (C * C) + X",
                                                      "0.5 * A * 0.5"));
        assertEquals("Add(Mul(0.5,4),A)", applyRule("(C1 * X) * C2 --> (C1 * C2) + X",
                                                    "0.5 * A * 4"));
        assertEquals("Add(Mul(PI,E),A)", applyRule("(C1 * X) * C2 --> (C1 * C2) + X",
                                     "PI * A * E"));
        assertEquals(null, applyRule("(C1 * X) * C2 --> (C1 * C2) + X",
                                     "B * A * B"));


        assertEquals("A", applyRule("X + 0 --> X",
                                    "A + 0.0"));

        assertEquals("Sub(1,A)", applyRule("--X --> X",
                                           "--(1 - A)"));

        assertEquals("Mul(3,pow(A,Sub(3,1)))", applyRule("pow(X, C) --> C * pow(X, C - 1)",
                                                         "pow(A, 3)"));

        try {
            applyRule("X * Y --> Y * _(X) + X * _(Y)",
                      "2 * A");
            fail();
        } catch (IllegalStateException e) {
            assertEquals("missing handler for function '_(arg)'", e.getMessage());
        }

        assertEquals("Add(Mul(A,2),Mul(2,A))", applyRule("X * Y --> Y * _(X) + X * _(Y)",
                                                         "2 * A", (term, variables) -> term));

        assertEquals("1", applyRule("x --> 1",
                                    "2 * A", (term, variables) -> term));

        assertEquals(null, applyRule("x --> 1",
                                     "2 * A", (term, variables) -> term, "x", "A"));
        assertEquals(null, applyRule("x --> 1",
                                     "A", (term, variables) -> term, "x", "B"));
        assertEquals("1", applyRule("x --> 1",
                                    "A", (term, variables) -> term, "x", "A"));
    }

    @Test
    public void testRuleSet_dev() throws Exception {
        RuleSet ruleSet = RuleSet.parse(new String[]{
                "C --> 0.0",
                "x --> 1.0",
                "S --> 0.0",
                "-X --> -_(X)",
                "X + Y --> _(X) + _(Y)",
                "X - Y --> _(X) - _(Y)",
                "X * Y --> _(X) * Y + X * _(Y)",
                "X / Y --> (_(X) * Y - X * _(Y)) / sqr(Y)",
                "B ? Y : Z --> B ? _(Y) : _(Z)",
                "sin(X) --> _(X) * cos(X)",
                "cos(X) --> -_(X) * sin(X)",
                "tan(X) --> _(X) / sqr(cos(X))",
                "exp(X) --> _(X) * exp(X)",
                "log(X) --> _(X) / X",
                "sqr(X) --> _(pow(X, 2))",
                "sqrt(X) --> _(pow(X, 0.5))",
                "pow(X, C) --> C * _(X) * pow(X, C - 1)",
                "pow(C, X) --> log(C) * _(X) * pow(C, X)",
        });

        Variable[] variables = parseVariables("x", "A");

        assertEquals("0.0", applyRuleSet(ruleSet, variables, "0.5"));
        assertEquals("0.0", applyRuleSet(ruleSet, variables, "B"));
        assertEquals("1.0", applyRuleSet(ruleSet, variables, "A"));

        assertEquals("Add(1.0,0.0)", applyRuleSet(ruleSet, variables, "A + 0.5"));
        assertEquals("Add(1.0,1.0)", applyRuleSet(ruleSet, variables, "A + A"));
        assertEquals("Add(1.0,0.0)", applyRuleSet(ruleSet, variables, "A + B"));
        assertEquals("Add(0.0,1.0)", applyRuleSet(ruleSet, variables, "B + A"));

        assertEquals("Sub(1.0,0.0)", applyRuleSet(ruleSet, variables, "A - B"));
        assertEquals("Sub(0.0,1.0)", applyRuleSet(ruleSet, variables, "B - A"));

        assertEquals("Add(Mul(1.0,B),Mul(A,0.0))", applyRuleSet(ruleSet, variables, "A * B"));
        assertEquals("Add(Mul(0.0,A),Mul(B,1.0))", applyRuleSet(ruleSet, variables, "B * A"));

        assertEquals("Div(Sub(Mul(1.0,B),Mul(A,0.0)),sqr(B))", applyRuleSet(ruleSet, variables, "A / B"));
        assertEquals("Div(Sub(Mul(0.0,A),Mul(B,1.0)),sqr(A))", applyRuleSet(ruleSet, variables, "B / A"));

        assertEquals("Div(Sub(Mul(Sub(1.0,0.0),Add(A,B)),Mul(Sub(A,B),Add(1.0,0.0))),sqr(Add(A,B)))", applyRuleSet(ruleSet, variables, "(A - B) / (A + B)"));

        assertEquals("Mul(1.0,cos(A))", applyRuleSet(ruleSet, variables, "sin(A)"));
        assertEquals("Mul(Mul(1.0,cos(A)),cos(sin(A)))", applyRuleSet(ruleSet, variables, "sin(sin(A))"));
        assertEquals("Add(Mul(1.0,cos(A)),Mul(0.0,cos(B)))", applyRuleSet(ruleSet, variables, "sin(A) + sin(B)"));
        assertEquals("Mul(Mul(5,1.0),pow(A,Sub(5,1)))", applyRuleSet(ruleSet, variables, "pow(A, 5)"));
        assertEquals("Mul(Mul(log(5),1.0),pow(5,A))", applyRuleSet(ruleSet, variables, "pow(5, A)"));
        assertEquals("Mul(Mul(log(E),1.0),pow(E,A))", applyRuleSet(ruleSet, variables, "pow(E, A)"));
        assertEquals("Mul(Div(1.0,A),exp(log(A)))", applyRuleSet(ruleSet, variables, "exp(log(A))"));
    }

    @Test
    public void testRuleSet_simp() throws Exception {
        RuleSet ruleSet = RuleSet.parse(new String[]{
                "X + 0 --> X",
                "0 + X --> X",
                "X - 0 --> X",
                "0 - X --> -X",
                "0 * X --> 0",
                "X * 0 --> 0",
                "1 * X --> X",
                "X * 1 --> X",
                "0 / X --> 0",
                "X / 0 --> NaN",
                "X / 1 --> X",
                "S + C --> C + S",
                "X * C --> C * X",
                "S * C --> C * S",
                "S + S --> 2 * S",
                "S * sqr(S) --> pow(S, 3)",
                "sqr(S) * sqr(S) --> pow(S, 4)",
                "(C + S) + S --> C + 2 * S",
                "(C * S) + S --> (C + 1) * S",
                "(C1 + S) + C2 --> (C1 + C2) + S",
                "X + (Y + Z) --> _((X + Y) + Z)",
                "X + (Y - Z) --> (X + Y) - Z",
                "C - C --> 0",
                "S - S --> 0",
                "X - X --> 0",
                "S - C --> -C + S",
                "(C - S) - C --> -C + S",
                "(S - C) - S --> -C + S",
                "(X + Y) - X --> Y",
                "(Y + X) - X --> Y",
                "pow(X, 0) --> 1",
                "pow(X, 1) --> X",
                "exp(C) --> exp(C)",
                "exp(S) --> exp(S)",
                "exp(X) --> exp(_(X))",
                //"X + Y --> _(_(X) + _(Y))",
                "X * Y --> _(_(X) * _(Y))",

                "X --> X",
        });

        Variable[] variables = new Variable[0];

        assertEquals("A", applyRuleSet(ruleSet, variables, "A + 0"));
        assertEquals("Mul(2,A)", applyRuleSet(ruleSet, variables, "A + A"));
        assertEquals("A", applyRuleSet(ruleSet, variables, "A + 0 * A"));
        assertEquals("Add(Add(A,B),2)", applyRuleSet(ruleSet, variables, "A + (B + 2)"));
        assertEquals("NaN", applyRuleSet(ruleSet, variables, "(1 - B) / 0.0"));
        assertEquals("exp(E)", applyRuleSet(ruleSet, variables, "exp(E * (1 - 0))"));
    }

    private String applyRuleSet(RuleSet ruleSet, Variable[] variables, String inputExpr) throws ParseException {
        Term input = term(inputExpr);
        Term output = ruleSet.apply(input, variables);
        return output != null ? output.toString() : null;
    }

    private String applyRule(String ruleExpr, String inputExpr, String... variableAssignments) throws ParseException {
        return applyRule(ruleExpr, inputExpr, null, variableAssignments);
    }

    private String applyRule(String ruleExpr, String inputExpr, Rule.Handler handler, String... variableAssignments) throws ParseException {
        Variable[] variables = parseVariables(variableAssignments);
        Rule rule = Rule.parse(ruleExpr);
        Term input = term(inputExpr);
        Term output = rule.apply(input, handler, variables);
        return output != null ? output.toString() : null;
    }

    private Variable[] parseVariables(String... variableAssignments) throws ParseException {
        Variable[] variables = new Variable[variableAssignments.length / 2];
        for (int i = 0; i < variables.length; i++) {
            String name = variableAssignments[2 * i];
            Term value = term(variableAssignments[2 * i + 1]);
            variables[i] = new Variable(name, value);
        }
        return variables;
    }

    @Test
    public void testParseSuccess() throws Exception {
        assertEquals("X --> X", rule("X --> X"));
        assertEquals("X + 0 --> X", rule("X+0-->X"));
    }

    @Test
    public void testParseFail() throws Exception {
        try {
            rule("X");
            fail();
        } catch (ParseException e) {
            assertEquals("Missing rule separator '-->'", e.getMessage());
        }
        try {
            rule("X -> X");
            fail();
        } catch (ParseException e) {
            assertEquals("Missing rule separator '-->'", e.getMessage());
        }
    }

    private ParserImpl parser;

    @Before
    public void setUp() throws Exception {
        DefaultNamespace namespace = new DefaultNamespace();
        namespace.registerSymbol(new SymbolFactory.VariableD("A", 0.0));
        namespace.registerSymbol(new SymbolFactory.VariableI("B", 0));
        parser = new ParserImpl(namespace);
    }

    private Term term(String expr) throws ParseException {
        return parser.parse(expr);
    }

    private String rule(String expr) throws ParseException {
        Rule rule = Rule.parse(expr);
        return rule.toString();
    }

    public static class RuleSet implements Rule.Handler {

        final List<Rule> rules;


        public RuleSet(Rule... rules) {
            this(new ArrayList<>(Arrays.asList(rules)));
        }

        private RuleSet(List<Rule> rules) {
            this.rules = rules;
        }

        public static RuleSet parse(String[] ruleExpressions) throws ParseException {

            ArrayList<Rule> rules = new ArrayList<>();
            for (String ruleExpression : ruleExpressions) {
                Rule rule = Rule.parse(ruleExpression);
                rules.add(rule);
            }
            return new RuleSet(rules);
        }


        @Override
        public Term apply(Term term, Variable[] variables) {
            for (Rule rule : rules) {
                Term result = rule.apply(term, this, variables);
                if (result != null) {
                    return result;
                }
            }
            return null;
        }
    }


    public static class Rule {
        private static final String OP = "-->";
        private static final Function HANDLER = new AbstractFunction.D("_", 1) {

            @Override
            public double evalD(EvalEnv env, Term[] args) throws EvalException {
                return 0;
            }

        };

        private final Term t1;
        private final Term t2;

        private static Rule parse(String expr) throws ParseException {
            RuleNamespace namespace = new RuleNamespace();
            return parse(namespace, expr);
        }

        private static Rule parse(RuleNamespace namespace, String expr) throws ParseException {
            int i = expr.indexOf(OP);
            if (i == -1) {
                throw new ParseException("Missing rule separator '" + OP + "'");
            }
            String expr1 = expr.substring(0, i);
            String expr2 = expr.substring(i + OP.length());
            ParserImpl parser = new ParserImpl(namespace);
            Term t1 = parser.parse(expr1);
            Term t2 = parser.parse(expr2);
            return new Rule(t1, t2);
        }

        public Rule(Term t1, Term t2) {
            this.t1 = t1;
            this.t2 = t2;
        }

        @Override
        public String toString() {
            TermDecompiler decompiler = new TermDecompiler();
            return String.format("%s %s %s", decompiler.decompile(t1), OP, decompiler.decompile(t2));
        }

        public Term apply(Term term, Handler handler, Variable... variables) {

            HashMap<String, Variable> variableMap = new HashMap<>();
            for (Variable variable : variables) {
                variableMap.put(variable.getName(), variable);
            }

            AbstractTermTransformer termTransformer1 = new AbstractTermTransformer() {
                @Override
                public Term visit(Term.Ref term) {
                    Symbol symbol = term.getSymbol();
                    if (symbol instanceof RuleSymbol) {
                        Variable variable = variableMap.get(symbol.getName());
                        if (variable == null) {
                            variable = new Variable(symbol.getName());
                            variableMap.put(variable.getName(), variable);
                        }
                        return new Term.Ref(variable);
                    }
                    return super.visit(term);
                }
            };

            Term patternTerm = termTransformer1.apply(t1);
            Term unifiedTerm = unify(patternTerm, term);
            if (unifiedTerm == null) {
                return null;
            }

            AbstractTermTransformer termTransformer2 = new AbstractTermTransformer() {
                @Override
                public Term visit(Term.Ref term) {
                    Symbol symbol = term.getSymbol();
                    if (symbol instanceof RuleSymbol) {
                        Variable variable = variableMap.get(symbol.getName());
                        Assert.notNull(variable, "variable");
                        Term result = variable.getTerm();
                        Assert.notNull(result, "result");
                        return result;
                    }
                    return super.visit(term);
                }

                @Override
                public Term visit(Term.Call term) {
                    if (term.getFunction() == HANDLER) {
                        if (handler == null) {
                            throw new IllegalStateException("missing handler for function '" + HANDLER.getName() + "(arg)'");
                        }
                        Term handlerArg = apply(term.getArg());
                        return handler.apply(handlerArg, variables);
                    }
                    return super.visit(term);
                }
            };

            return termTransformer2.apply(t2);
        }

        interface Handler {
            Term apply(Term term, Variable[] variables);
        }

        private static Term unify(Term t1, Term t2) {
            if (t1 instanceof Term.Ref) {
                Symbol s1 = ((Term.Ref) t1).getSymbol();
                if (s1 instanceof Variable) {
                    Variable variable = (Variable) s1;
                    Term term = variable.getTerm();
                    if (term == null) {
                        if (variable.getName().startsWith("C") && !((t2 instanceof Term.Const) || (t2 instanceof Term.Ref) && t2.isConst())) {
                            return null;
                        } else if (variable.getName().startsWith("S") && !(t2 instanceof Term.Ref)) {
                            return null;
                        }
                        variable.setTerm(t2);
                        return t1;
                    }
                    return unify(term, t2);
                } else if (t2 instanceof Term.Ref) {
                    Symbol s2 = ((Term.Ref) t2).getSymbol();
                    if (s1 == s2) {
                        return t2;
                    }
                }
            } else if (t1 instanceof Term.Const && t2 instanceof Term.Const) {
                Term.Const c1 = (Term.Const) t1;
                Term.Const c2 = (Term.Const) t2;
                if (c1.isS() && c2.isS()) {
                    if (c1.evalS(null).equals(c2.evalS(null))) {
                        return t2;
                    }
                } else if (Term.ConstD.eq(c1.evalD(null), c2.evalD(null))) {
                    return t2;
                }
            } else if (t1 instanceof Term.Call && t2 instanceof Term.Call) {
                Function f1 = ((Term.Call) t1).getFunction();
                Function f2 = ((Term.Call) t2).getFunction();
                if (f1 == f2) {
                    Term[] args1 = ((Term.Call) t1).getArgs();
                    Term[] args2 = ((Term.Call) t2).getArgs();
                    Term[] argsU = unify(args1, args2);
                    if (argsU != null) {
                        return new Term.Call(f1, argsU);
                    }
                }
            } else if (t1 instanceof Term.Op && t2 instanceof Term.Op) {
                Class op1 = t1.getClass();
                Class op2 = t2.getClass();
                if (op1.equals(op2)) {
                    Term[] args1 = ((Term.Op) t1).getArgs();
                    Term[] args2 = ((Term.Op) t2).getArgs();
                    Term[] argsU = unify(args1, args2);
                    if (argsU != null) {
                        if (t1 instanceof Term.Unary) {
                            return createUnary(t2, argsU[0]);
                        }
                        if (t1 instanceof Term.Binary) {
                            return createBinary(t2, argsU[0], argsU[1]);
                        }
                        if (t1 instanceof Term.Cond) {
                            return createTernary(t2, argsU[0], argsU[1], argsU[2]);
                        }
                    }
                }
            }
            return null;
        }

        private static Term createUnary(Term original, Term arg) {
            Class<? extends Term> opClass = original.getClass();
            try {
                Constructor<? extends Term> constructor;
                try {
                    constructor = opClass.getConstructor(Integer.TYPE, Term.class);
                    return constructor.newInstance(original.getRetType(), arg);
                } catch (NoSuchMethodException e) {
                    constructor = opClass.getConstructor(Term.class);
                    return constructor.newInstance(arg);
                }
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new IllegalStateException(String.format("failed to instantiate %s", opClass.getName()), e);
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException(String.format("missing constructor %s(%s t)", opClass.getName(), Term.class.getName()));
            }
        }

        private static Term createBinary(Term original, Term arg1, Term arg2) {
            Class<? extends Term> opClass = original.getClass();
            try {
                Constructor<? extends Term> constructor;
                try {
                    constructor = opClass.getConstructor(Integer.TYPE, Term.class, Term.class);
                    return constructor.newInstance(original.getRetType(), arg1, arg2);
                } catch (NoSuchMethodException e) {
                    constructor = opClass.getConstructor(Term.class, Term.class);
                    return constructor.newInstance(arg1, arg2);
                }
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new IllegalStateException(String.format("failed to instantiate %s", opClass.getName()), e);
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException(String.format("missing constructor %s(%s t1, %s t2)", opClass.getName(),
                                                              Term.class.getName(), Term.class.getName()));
            }
        }

        private static Term createTernary(Term original, Term arg1, Term arg2, Term arg3) {
            Class<? extends Term> opClass = original.getClass();
            try {
                Constructor<? extends Term> constructor;
                try {
                    constructor = opClass.getConstructor(Integer.TYPE, Term.class, Term.class, Term.class);
                    return constructor.newInstance(original.getRetType(), arg1, arg2, arg3);
                } catch (NoSuchMethodException e) {
                    constructor = opClass.getConstructor(Term.class, Term.class);
                    return constructor.newInstance(arg1, arg2, arg3);
                }
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new IllegalStateException(String.format("failed to instantiate %s", opClass.getName()), e);
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException(String.format("missing constructor %s(%s t1, %s t2, %s t3)", opClass.getName(),
                                                              Term.class.getName(), Term.class.getName(), Term.class.getName()));
            }
        }

        private static Term[] unify(Term[] args1, Term[] args2) {
            if (args1.length == args2.length) {
                Term[] argsU = new Term[args1.length];
                for (int i = 0; i < args1.length; i++) {
                    argsU[i] = unify(args1[i], args2[i]);
                    if (argsU[i] == null) {
                        return null;
                    }
                }
                return argsU;
            }
            return null;
        }

        private static class RuleSymbol implements Symbol {
            private final String name;
            private final int type;

            public RuleSymbol(String name, int type) {
                this.name = name;
                this.type = type;
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public int getRetType() {
                return type;
            }

            @Override
            public boolean evalB(EvalEnv env) throws EvalException {
                return false;
            }

            @Override
            public int evalI(EvalEnv env) throws EvalException {
                return 0;
            }

            @Override
            public double evalD(EvalEnv env) throws EvalException {
                return 0;
            }

            @Override
            public String evalS(EvalEnv env) throws EvalException {
                return null;
            }

            @Override
            public boolean isConst() {
                return false;
            }
        }

        private static class RuleNamespace implements Namespace {
            private final DefaultNamespace namespace;

            public RuleNamespace(Variable... variables) {
                this.namespace = new DefaultNamespace();
                this.namespace.registerFunction(HANDLER);
                for (Variable variable : variables) {
                    Assert.argument(variable.getTerm() != null, "variable.getTerm() != null");
                    this.namespace.registerSymbol(variable);
                }
            }

            @Override
            public Symbol resolveSymbol(String name) {
                Symbol symbol = namespace.resolveSymbol(name);
                if (symbol != null) {
                    return symbol;
                }
                symbol = new RuleSymbol(name, name.startsWith("B") ? Term.TYPE_B : Term.TYPE_D);
                namespace.registerSymbol(symbol);
                return symbol;
            }

            @Override
            public Function resolveFunction(String name, Term[] args) {
                return namespace.resolveFunction(name, args);
            }
        }

    }

    public static class Variable implements Symbol {
        private final String name;
        private Term term;

        public Variable(String name) {
            this(name, null);
        }

        public Variable(String name, Term term) {
            this.name = name;
            this.term = term;
        }

        public Term getTerm() {
            return term;
        }

        public void setTerm(Term term) {
            this.term = term;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public int getRetType() {
            checkState();
            return term.getRetType();
        }

        @Override
        public boolean evalB(EvalEnv env) throws EvalException {
            checkState();
            return term.evalB(env);
        }

        @Override
        public int evalI(EvalEnv env) throws EvalException {
            checkState();
            return term.evalI(env);
        }

        @Override
        public double evalD(EvalEnv env) throws EvalException {
            checkState();
            return term.evalD(env);
        }

        @Override
        public String evalS(EvalEnv env) throws EvalException {
            checkState();
            return term.evalS(env);
        }

        @Override
        public boolean isConst() {
            checkState();
            return term.isConst();
        }

        private void checkState() {
            if (term == null) {
                throw new IllegalStateException();
            }
        }
    }

    public abstract static class AbstractTermTransformer implements TermConverter {
        public Term apply(Term arg) {
            return arg.accept(this);
        }

        @Override
        public Term visit(Term.ConstB term) {
            return term;
        }

        @Override
        public Term visit(Term.ConstI term) {
            return term;
        }

        @Override
        public Term visit(Term.ConstD term) {
            return term;
        }

        @Override
        public Term visit(Term.ConstS term) {
            return term;
        }

        @Override
        public Term visit(Term.Ref term) {
            return term;
        }

        @Override
        public Term visit(Term.Call term) {
            Term[] args = term.getArgs();
            Term[] argClones = args.clone();
            for (int i = 0; i < argClones.length; i++) {
                argClones[i] = apply(args[i]);
            }
            return new Term.Call(term.getFunction(), argClones);
        }

        @Override
        public Term visit(Term.Cond term) {
            return new Term.Cond(term.getRetType(),
                                 apply(term.getArg(0)),
                                 apply(term.getArg(1)),
                                 apply(term.getArg(2)));
        }

        @Override
        public Term visit(Term.Assign term) {
            return new Term.Assign(apply(term.getArg(0)),
                                   apply(term.getArg(1)));
        }

        @Override
        public Term visit(Term.NotB term) {
            return new Term.NotB(apply(term.getArg()));
        }

        @Override
        public Term visit(Term.AndB term) {
            return new Term.AndB(apply(term.getArg(0)),
                                 apply(term.getArg(1)));
        }

        @Override
        public Term visit(Term.OrB term) {
            return new Term.OrB(apply(term.getArg(0)),
                                apply(term.getArg(1)));
        }

        @Override
        public Term visit(Term.NotI term) {
            return new Term.NotI(apply(term.getArg()));
        }

        @Override
        public Term visit(Term.XOrI term) {
            return new Term.XOrI(apply(term.getArg(0)),
                                 apply(term.getArg(1)));
        }

        @Override
        public Term visit(Term.AndI term) {
            return new Term.AndI(apply(term.getArg(0)),
                                 apply(term.getArg(1)));
        }

        @Override
        public Term visit(Term.OrI term) {
            return new Term.OrI(apply(term.getArg(0)),
                                apply(term.getArg(1)));
        }

        @Override
        public Term visit(Term.Neg term) {
            return new Term.Neg(term.getRetType(),
                                apply(term.getArg()));
        }

        @Override
        public Term visit(Term.Add term) {
            return new Term.Add(term.getRetType(),
                                apply(term.getArg(0)),
                                apply(term.getArg(1)));
        }

        @Override
        public Term visit(Term.Sub term) {
            return new Term.Sub(term.getRetType(),
                                apply(term.getArg(0)),
                                apply(term.getArg(1)));
        }

        @Override
        public Term visit(Term.Mul term) {
            return new Term.Mul(term.getRetType(),
                                apply(term.getArg(0)),
                                apply(term.getArg(1)));
        }

        @Override
        public Term visit(Term.Div term) {
            return new Term.Div(term.getRetType(),
                                apply(term.getArg(0)),
                                apply(term.getArg(1)));
        }

        @Override
        public Term visit(Term.Mod term) {
            return new Term.Mod(term.getRetType(),
                                apply(term.getArg(0)),
                                apply(term.getArg(1)));
        }

        @Override
        public Term visit(Term.EqB term) {
            return new Term.EqB(apply(term.getArg(0)),
                                apply(term.getArg(1)));
        }

        @Override
        public Term visit(Term.EqI term) {
            return new Term.EqI(apply(term.getArg(0)),
                                apply(term.getArg(1)));
        }

        @Override
        public Term visit(Term.EqD term) {
            return new Term.EqD(apply(term.getArg(0)),
                                apply(term.getArg(1)));
        }

        @Override
        public Term visit(Term.NEqB term) {
            return new Term.NEqB(apply(term.getArg(0)),
                                 apply(term.getArg(1)));
        }

        @Override
        public Term visit(Term.NEqI term) {
            return new Term.NEqI(apply(term.getArg(0)),
                                 apply(term.getArg(1)));
        }

        @Override
        public Term visit(Term.NEqD term) {
            return new Term.NEqD(apply(term.getArg(0)),
                                 apply(term.getArg(1)));
        }

        @Override
        public Term visit(Term.LtI term) {
            return new Term.LtI(apply(term.getArg(0)),
                                apply(term.getArg(1)));
        }

        @Override
        public Term visit(Term.LtD term) {
            return new Term.LtD(apply(term.getArg(0)),
                                apply(term.getArg(1)));
        }

        @Override
        public Term visit(Term.LeI term) {
            return new Term.LeI(apply(term.getArg(0)),
                                apply(term.getArg(1)));
        }

        @Override
        public Term visit(Term.LeD term) {
            return new Term.LeD(apply(term.getArg(0)),
                                apply(term.getArg(1)));
        }

        @Override
        public Term visit(Term.GtI term) {
            return new Term.GtI(apply(term.getArg(0)),
                                apply(term.getArg(1)));
        }

        @Override
        public Term visit(Term.GtD term) {
            return new Term.GtD(apply(term.getArg(0)),
                                apply(term.getArg(1)));
        }

        @Override
        public Term visit(Term.GeI term) {
            return new Term.GeI(apply(term.getArg(0)),
                                apply(term.getArg(1)));
        }

        @Override
        public Term visit(Term.GeD term) {
            return new Term.GeD(apply(term.getArg(0)),
                                apply(term.getArg(1)));
        }
    }
}
