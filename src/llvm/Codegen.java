/**
 * **************************************************
 * Esta classe Codegen é a responsável por emitir LLVM-IR.
 * Ela possui o mesmo método 'visit' sobrecarregado de
 * acordo com o tipo do parâmetro. Se o parâmentro for
 * do tipo 'While', o 'visit' emitirá código LLVM-IR que
 * representa este comportamento.
 * Alguns métodos 'visit' já estão prontos e, por isso,
 * a compilação do código abaixo já é possível.VarD
 * <p/>
 * class a{
 * public static void main(String[] args){
 * System.out.println(1+2);
 * }
 * }
 * <p/>
 * O pacote 'llvmast' possui estruturas simples
 * que auxiliam a geração de código em LLVM-IR. Quase todas
 * as classes estão prontas; apenas as seguintes precisam ser
 * implementadas:
 * <p/>
 * // llvmasm/LlvmBranch.java
 * // llvmasm/LlvmIcmp.java
 * // llvmasm/LlvmMinus.java
 * // llvmasm/LlvmTimes.java
 * <p/>
 * <p/>
 * Todas as assinaturas de métodos e construtores
 * necessárias já estão lá.
 * <p/>
 * <p/>
 * Observem todos os métodos e classes já implementados
 * e o manual do LLVM-IR (http://llvm.org/docs/LangRef.html)
 * como guia no desenvolvimento deste projeto.
 * <p/>
 * **************************************************
 */
package llvm;

import llvmast.*;
import semant.Env;
import syntaxtree.*;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Codegen extends VisitorAdapter {
    private List<LlvmInstruction> assembler;
    private Codegen codeGenerator;

    private SymTab symTab;
    private ClassNode classEnv;    // Aponta para a classe atualmente em uso em symTab
    private MethodNode methodEnv;    // Aponta para a metodo atualmente em uso em symTab


    public Codegen() {
        assembler = new LinkedList<LlvmInstruction>();
        symTab = new SymTab();
    }

    // Método de entrada do Codegen
    public String translate(Program p, Env env) {
        codeGenerator = new Codegen();

        // Preenchendo a Tabela de Símbolos
        // Quem quiser usar 'env', apenas comente essa linha
        codeGenerator.symTab.FillTabSymbol(p);

        // Formato da String para o System.out.printlnijava "%d\n"
        codeGenerator.assembler.add(new LlvmConstantDeclaration("@.formatting.string", "private constant [4 x i8] c\"%d\\0A\\00\""));

        // NOTA: sempre que X.accept(Y), então Y.visit(X);
        // NOTA: Logo, o comando abaixo irá chamar codeGenerator.visit(Program), linha 75
        p.accept(codeGenerator);

        // Link do printf
        List<LlvmType> pts = new LinkedList<LlvmType>();
        pts.add(new LlvmPointer(LlvmPrimitiveType.I8));
        pts.add(LlvmPrimitiveType.DOTDOTDOT);
        codeGenerator.assembler.add(new LlvmExternalDeclaration("@printf", LlvmPrimitiveType.I32, pts));
        List<LlvmType> mallocpts = new LinkedList<LlvmType>();
        mallocpts.add(LlvmPrimitiveType.I32);
        codeGenerator.assembler.add(new LlvmExternalDeclaration("@malloc", new LlvmPointer(LlvmPrimitiveType.I8), mallocpts));


        String r = new String();
        for (LlvmInstruction instr : codeGenerator.assembler)
            r += instr + "\n";
        return r;
    }

    public LlvmValue visit(Program n) {
        n.mainClass.accept(this);

        for (util.List<ClassDecl> c = n.classList; c != null; c = c.tail)
            c.head.accept(this);

        return null;
    }

    public LlvmValue visit(MainClass n) {

        // definicao do main
        assembler.add(new LlvmDefine("@main", LlvmPrimitiveType.I32, new LinkedList<LlvmValue>()));
        assembler.add(new LlvmLabel(new LlvmLabelValue("entry")));
        LlvmRegister R1 = new LlvmRegister(new LlvmPointer(LlvmPrimitiveType.I32));
        assembler.add(new LlvmAlloca(R1, LlvmPrimitiveType.I32, new LinkedList<LlvmValue>()));
        assembler.add(new LlvmStore(new LlvmIntegerLiteral(0), R1));

        // Statement é uma classe abstrata
        // Portanto, o accept chamado é da classe que implementa Statement, por exemplo,  a classe "Print".
        n.stm.accept(this);

        // Final do Main
        LlvmRegister R2 = new LlvmRegister(LlvmPrimitiveType.I32);
        assembler.add(new LlvmLoad(R2, R1));
        assembler.add(new LlvmRet(R2));
        assembler.add(new LlvmCloseDefinition());
        return null;
    }

    public LlvmValue visit(Print n) {

        LlvmValue v = n.exp.accept(this);

        // getelementptr:
        LlvmRegister lhs = new LlvmRegister(new LlvmPointer(LlvmPrimitiveType.I8));
        LlvmRegister src = new LlvmNamedValue("@.formatting.string", new LlvmPointer(new LlvmArray(4, LlvmPrimitiveType.I8)));
        List<LlvmValue> offsets = new LinkedList<LlvmValue>();
        offsets.add(new LlvmIntegerLiteral(0));
        offsets.add(new LlvmIntegerLiteral(0));
        List<LlvmType> pts = new LinkedList<LlvmType>();
        pts.add(new LlvmPointer(LlvmPrimitiveType.I8));
        List<LlvmValue> args = new LinkedList<LlvmValue>();
        args.add(lhs);
        args.add(v);
        assembler.add(new LlvmGetElementPointer(lhs, src, offsets));

        pts = new LinkedList<LlvmType>();
        pts.add(new LlvmPointer(LlvmPrimitiveType.I8));
        pts.add(LlvmPrimitiveType.DOTDOTDOT);

        // printf:
        assembler.add(new LlvmCall(new LlvmRegister(LlvmPrimitiveType.I32),
                LlvmPrimitiveType.I32,
                pts,
                "@printf",
                args
        ));
        return null;
    }


    // Todos os visit's que devem ser implementados
    public LlvmValue visit(IntegerLiteral n) {
        return new LlvmIntegerLiteral(n.value);
    }

    public LlvmValue visit(Plus n) {
        LlvmValue v1 = n.lhs.accept(this);
        LlvmValue v2 = n.rhs.accept(this);
        LlvmRegister lhs = new LlvmRegister(LlvmPrimitiveType.I32);
        assembler.add(new LlvmPlus(lhs, LlvmPrimitiveType.I32, v1, v2));
        return lhs;
    }

    public LlvmValue visit(Minus n) {
        LlvmValue v1 = n.lhs.accept(this);
        LlvmValue v2 = n.rhs.accept(this);
        LlvmRegister lhs = new LlvmRegister(LlvmPrimitiveType.I32);
        assembler.add(new LlvmMinus(lhs, LlvmPrimitiveType.I32, v1, v2));
        return lhs;
    }

    public LlvmValue visit(Times n) {
        LlvmValue v1 = n.lhs.accept(this);
        LlvmValue v2 = n.rhs.accept(this);
        LlvmRegister lhs = new LlvmRegister(LlvmPrimitiveType.I32);
        assembler.add(new LlvmTimes(lhs, LlvmPrimitiveType.I32, v1, v2));
        return lhs;
    }

    public LlvmValue visit(ClassDeclSimple n) {
        // pega o nome atual da classe na tabela de símbolos
        classEnv = symTab.classes.get(n.name.s);

        // declara a estrutura da classe
        assembler.add(classEnv.getClassDeclaration());

        // Percorre n.methodList visitando cada método
        for (util.List<MethodDecl> methodList = n.methodList; methodList != null; methodList = methodList.tail) {
            methodList.head.accept(this);
        }

        return null;
    }

    // TODO
    public LlvmValue visit(ClassDeclExtends n) {
        return null;
    }

    public LlvmValue visit(VarDecl n) {
        LlvmValue value = n.type.accept(this);
        LlvmNamedValue v;
        if (classEnv!= null && methodEnv != null) {
            String varName = "%" + classEnv.getName() + "." + methodEnv.getName() + ".";
            v = new LlvmNamedValue(varName + n.name.s + ".local", value.type);
            assembler.add(new LlvmAlloca(v, value.type, new LinkedList<LlvmValue>()));
        } else if (classEnv != null) {
            String varName = "%" + classEnv.getName() + ".";
            v= new LlvmNamedValue(varName+n.name.s, value.type);
            assembler.add(new LlvmAlloca(v, value.type, new LinkedList<LlvmValue>()));
        } else {
            v = new LlvmNamedValue("%" + n.name.s, value.type);
            assembler.add(new LlvmAlloca(v, value.type, new LinkedList<LlvmValue>()));
        }
        return v;
    }

    public LlvmValue visit(MethodDecl n) {
        methodEnv = classEnv.methods.get(n.name.s);

        // define o método
        assembler.add(methodEnv.getFunctionDefinition(classEnv));

        // label
        assembler.add(new LlvmLabel(new LlvmLabelValue("entry")));

        // aloca os Formals
        for (LlvmValue v : methodEnv.getFormalList()) {
            if (!v.toString().equals("%this")) {
                String varName = "%" + classEnv.getName() + "." + methodEnv.getName() + ".";
                LlvmRegister R1 = new LlvmRegister(varName+v.toString().substring(1)+".pmtr", new LlvmPointer(v.type));
                assembler.add(new LlvmAlloca(R1, v.type, new LinkedList<LlvmValue>()));
                assembler.add(new LlvmStore(v, R1));
            }
        }

        // aloca as variáveis
        for (util.List<VarDecl> vars = n.locals; vars != null; vars = vars.tail) {
            vars.head.accept(this);
        }

        // accept nas expressões
        for (util.List<Statement> exprList = n.body; exprList != null; exprList = exprList.tail) {
            exprList.head.accept(this);
        }

        // retorno do método
        LlvmValue returnValue = n.returnExp.accept(this);
        assembler.add(new LlvmRet(returnValue));
        assembler.add(new LlvmCloseDefinition());

        // Limpa a tabela de símbolos temporária
        methodEnv = null;
        return null;
    }

    public LlvmValue visit(Formal n) {
        return new LlvmNamedValue("%" + classEnv.getName() + "." + methodEnv.getName() + "." + n.name.s + ".pmtr", n.type.accept(this).type);
    }

    public LlvmValue visit(IntArrayType n) {
        return new LlvmNamedValue("int[]", new LlvmPointer(LlvmPrimitiveType.I32));
    }

    public LlvmValue visit(BooleanType n) {
        return new LlvmNamedValue("boolean", LlvmPrimitiveType.I1);
    }

    public LlvmValue visit(IntegerType n) {
        return new LlvmNamedValue("int", LlvmPrimitiveType.I32);
    }

    public LlvmValue visit(IdentifierType n) {
        return new LlvmNamedValue(n.name, symTab.classes.get(n.name).getClassPointer());
    }

    // Block contém uma lista de statements
    public LlvmValue visit(Block n) {
        for (util.List<Statement> s = n.body; s != null; s = s.tail) {
            s.head.accept(this);
        }
        return null;
    }

    public LlvmValue visit(If n) {
        LlvmValue cmp = n.condition.accept(this);
        LlvmLabelValue ifLabel = new LlvmLabelValue("ifLabel" + n.line);
        LlvmLabelValue elseLabel = new LlvmLabelValue("elseLabel" + n.line);
        LlvmLabelValue endLabel = new LlvmLabelValue("endLabel" + n.line);

        assembler.add(new LlvmBranch(cmp, ifLabel, elseLabel));
        assembler.add(new LlvmLabel(ifLabel));
        n.thenClause.accept(this);
        assembler.add(new LlvmBranch(endLabel));

        assembler.add(new LlvmLabel(elseLabel));
        if (n.elseClause != null) {
            n.elseClause.accept(this);
        }
        assembler.add(new LlvmLabel(endLabel));
        return cmp;
    }

    public LlvmValue visit(While n) {
        LlvmValue cond = n.condition.accept(this);
        LlvmLabelValue condLabel = new LlvmLabelValue("cond" + n.line);
        LlvmLabelValue endWhile = new LlvmLabelValue("endWhile" + n.line);
        LlvmLabelValue startBody = new LlvmLabelValue("startBody" + n.line);
        assembler.add(new LlvmLabel(condLabel));
        assembler.add(new LlvmBranch(cond, startBody, endWhile));
        assembler.add(new LlvmLabel(startBody));
        n.body.accept(this);
        assembler.add(new LlvmBranch(condLabel));
        assembler.add(new LlvmLabel(endWhile));
        return cond;
    }

    public LlvmValue visit(Assign n) {
        LlvmValue var = n.var.accept(this);
        LlvmValue exp = n.exp.accept(this);
        // parametro método (formal)
        String varName = classEnv.getName() + "." + methodEnv.getName() + ".";
        if (methodEnv != null && methodEnv.hasFormal("%" + n.var.s)) {
            LlvmValue val = methodEnv.formals.get("%" + n.var.s);
            LlvmRegister R1 = new LlvmRegister("%" + varName + n.var.s + ".pmtr", new LlvmPointer(val.type));
            assembler.add(new LlvmStore(exp, R1));
        } else if (methodEnv != null && methodEnv.hasLocalVariable("%" + n.var.s)) { // local do método
            LlvmRegister R2 = new LlvmRegister("%" + varName + n.var.s + ".local", new LlvmPointer(exp.type));
            LlvmAlloca aux = new LlvmAlloca(R2, exp.type, new LinkedList<LlvmValue>());
            assembler.add(new LlvmStore(exp, R2));

        } else if (classEnv != null && classEnv.vars.containsKey("%" + n.var.s)) { // atributo da classe
            LlvmRegister R3 = new LlvmRegister(exp.type);
            assembler.add(new LlvmGetElementPointer(R3, classEnv.getClassReference(), classEnv.getOffset("%" + n.var.s)));
            LlvmRegister R4 = new LlvmRegister(exp.type);
            assembler.add(new LlvmBitcast(R4, exp, var.type));
            assembler.add(new LlvmStore(R4, R3));
        }
        // local da superclasse
        return null;
    }

    public LlvmValue visit(ArrayAssign n) {
        return null;
    }

    public LlvmValue visit(And n) {
        LlvmValue v1 = n.lhs.accept(this);
        LlvmValue v2 = n.rhs.accept(this);
        LlvmRegister lhs = new LlvmRegister(LlvmPrimitiveType.I1);
        assembler.add(new LlvmAnd(lhs, LlvmPrimitiveType.I1, v1, v2));
        return lhs;
    }

    public LlvmValue visit(LessThan n) {
        LlvmValue v1 = n.lhs.accept(this);
        LlvmValue v2 = n.rhs.accept(this);
        LlvmRegister lhs = new LlvmRegister(LlvmPrimitiveType.I1);
        assembler.add(new LlvmIcmp(lhs, LlvmIcmp.SLT, LlvmPrimitiveType.I32, v1, v2));
        return lhs;
    }

    public LlvmValue visit(Equal n) {
        LlvmValue v1 = n.lhs.accept(this);
        LlvmValue v2 = n.rhs.accept(this);
        LlvmRegister lhs = new LlvmRegister(LlvmPrimitiveType.I1);
        assembler.add(new LlvmIcmp(lhs, LlvmIcmp.EQ, LlvmPrimitiveType.I32, v1, v2));
        return lhs;
    }

    // Procura elemento no array
    public LlvmValue visit(ArrayLookup n) {
        return null;
    }

    // Retorna o tamanho do array
    public LlvmValue visit(ArrayLength n) {
        return null;
    }

    // Chamada de método
    public LlvmValue visit(Call n) {
        LlvmValue obj = n.object.accept(this);
        LlvmValue retType = n.type.accept(this);
        LlvmValue method = n.method.accept(this);
        List<LlvmValue> actuals = new LinkedList<LlvmValue>();
        String name;

        if (obj != null && obj.type instanceof LlvmPointer) {
            LlvmPointer classPtr = (LlvmPointer) obj.type;
            LlvmClassType classType = (LlvmClassType) classPtr.content;
            ClassNode classNode = symTab.classes.get(classType.name);
            MethodNode methodNode = classNode.methods.get(method.toString());
            actuals.add(obj);
            name = "@__" + method.toString() + "_" + classNode.toString();
        } else {
            name = method.toString();
        }

        for (util.List<Exp> actualList = n.actuals; actualList != null; actualList = actualList.tail) {
            actuals.add(actualList.head.accept(this));
        }

        LlvmRegister reg = new LlvmRegister(retType.type);
        assembler.add(new LlvmCall(reg, retType.type, name, actuals));
        return reg;

    }

    public LlvmValue visit(True n) {
        return new LlvmBool(LlvmBool.TRUE);
    }

    public LlvmValue visit(False n) {
        return new LlvmBool(LlvmBool.FALSE);
    }

    public LlvmValue visit(IdentifierExp n) {
        LlvmRegister r = new LlvmRegister(n.type.accept(this).type);
        String name = "%" + n.name.s;
        if (classEnv != null) {
            // verifica se a variável da expressão é local do método
            String varName = "%" + classEnv.getName() + "." + methodEnv.getName() + ".";
            if (methodEnv.vars.containsKey(name)) {
                LlvmValue local = methodEnv.vars.get(name);
                LlvmNamedValue value = new LlvmNamedValue(new LlvmPointer(local.type) + " " + varName + local.toString().substring(1) +  ".local", r.type);
                assembler.add(new LlvmLoad(r, value));
                return r;
            }

            // verifica se a variável da expressão é parâmetro do método
            else if (methodEnv.formals.containsKey(name)) {
                LlvmValue local = methodEnv.formals.get(name);
                LlvmNamedValue value = new LlvmNamedValue(new LlvmPointer(local.type) + " " + varName + local.toString().substring(1) + ".pmtr", r.type);
                assembler.add(new LlvmLoad(r, value));
                return r;
            }

            // verifica se a variável é da classe
            else if (classEnv.vars.containsKey(name)) {
                LlvmValue local = classEnv.vars.get(name);
                LlvmRegister addr = new LlvmRegister(LlvmPrimitiveType.I32);
                assembler.add(new LlvmGetElementPointer(addr, classEnv.getClassReference(), classEnv.getOffset("%" + name)));
                LlvmNamedValue value = new LlvmNamedValue(addr.name, new LlvmPointer(r.type));
                assembler.add(new LlvmLoad(r, value));
                return r;
            }

            // TODO: verifica se a variável é de alguma superclasse
        }
        return null;

    }

    public LlvmValue visit(This n) {
        return new LlvmNamedValue("%this", new LlvmPointer(new LlvmClassType(classEnv.getName())));
    }

    public LlvmValue visit(NewArray n) {
        LlvmValue size = n.size.accept(this);
        int size_int = Integer.valueOf(size.toString());
        LlvmValue lhs = new LlvmRegister(new LlvmArray(size_int, LlvmPrimitiveType.I32));
        assembler.add(new LlvmMalloc(lhs, LlvmPrimitiveType.I32, size));
        return lhs;
    }

    public LlvmValue visit(NewObject n) {
        ClassNode clazz = symTab.classes.get(n.className.s);
        LlvmRegister lhs = new LlvmRegister(clazz.getClassPointer());
        assembler.add(new LlvmMalloc(lhs, clazz.getStructure(), clazz.getClassType().toString()));
        return lhs;
    }

    public LlvmValue visit(Not n) {
        return null;
    }

    public LlvmValue visit(Identifier n) {
        return new LlvmNamedValue(n.s, LlvmPrimitiveType.I32);
    }
}


/**********************************************************************************/
/* === Tabela de Símbolos ==== 
 * 
 * 
 */

/**********************************************************************************/

class SymTab extends VisitorAdapter {
    public Map<String, ClassNode> classes;
    private ClassNode classEnv;    //aponta para a classe em uso

    public LlvmValue FillTabSymbol(Program n) {
        n.accept(this);
        return null;
    }

    public LlvmValue visit(Program n) {
        n.mainClass.accept(this);

        for (util.List<ClassDecl> c = n.classList; c != null; c = c.tail)
            c.head.accept(this);

        return null;
    }

    public LlvmValue visit(MainClass n) {
        classes = new HashMap<String, ClassNode>();
        classes.put(n.className.s, new ClassNode(n.className.s, null, null));
        return null;
    }

    public LlvmValue visit(ClassDeclSimple n) {
        // percorre a lista de variáveis para ver os tipos
        List<LlvmType> typeList = new LinkedList<LlvmType>();
        List<LlvmValue> varList = new LinkedList<LlvmValue>();
        LlvmValue val;
        for (util.List<VarDecl> v = n.varList; v != null; v = v.tail) {
            val = v.head.accept(this);
            typeList.add(val.type);
            varList.add(val);
        }

        classEnv = new ClassNode(n.name.s, new LlvmStructure(typeList), varList);
        classes.put(n.name.s, classEnv);

        // Percorre n.methodList visitando cada método
        for (util.List<MethodDecl> methodList = n.methodList; methodList != null; methodList = methodList.tail) {
            methodList.head.accept(this);
        }

        return null;
    }

    public LlvmValue visit(ClassDeclExtends n) {
        return null;
    }

    public LlvmValue visit(VarDecl n) {
        LlvmValue value = n.type.accept(this);
        LlvmNamedValue v = new LlvmNamedValue("%" + n.name.s, value.type);
        return v;
    }

    public LlvmValue visit(Formal n) {
        return new LlvmNamedValue("%" + n.name.s, n.type.accept(this).type);
    }

    public LlvmValue visit(MethodDecl n) {
        LlvmType returnType = n.returnType.accept(this).type;
        List<LlvmValue> args = new LinkedList<LlvmValue>();
        List<LlvmValue> vars = new LinkedList<LlvmValue>();
        args.add(classEnv.getClassReference());

        for (util.List<Formal> formals = n.formals; formals != null; formals = formals.tail) {
            LlvmValue formal = formals.head.accept(this);
            args.add(formal);
        }

        for (util.List<VarDecl> varList = n.locals; varList != null; varList = varList.tail) {
            LlvmValue var = varList.head.accept(this);
            vars.add(var);
        }

        classEnv.addMethod(new MethodNode(n.name.s, args, vars, returnType));
        return null;
    }

    public LlvmValue visit(IdentifierType n) {
        return new LlvmNamedValue(n.name, new LlvmPointer(new LlvmClassType(
                n.name)));
    }

    public LlvmValue visit(IntArrayType n) {
        return new LlvmNamedValue("int[]", new LlvmPointer(
                LlvmPrimitiveType.I32));
    }

    public LlvmValue visit(BooleanType n) {
        return new LlvmNamedValue("boolean", LlvmPrimitiveType.I1);
    }

    public LlvmValue visit(IntegerType n) {
        return new LlvmNamedValue("int", LlvmPrimitiveType.I32);
    }
}

class ClassNode extends LlvmType {
    private String name;
    private LlvmStructure structure;
    public List<LlvmValue> varList;
    public List<MethodNode> methodList;
    public Map<String, MethodNode> methods;
    public Map<String, LlvmValue> vars;

    // constructor
    ClassNode(String nameClass, LlvmStructure classType, List<LlvmValue> varList) {
        this.name = nameClass;
        this.structure = classType;
        this.varList = varList;
        this.methodList = new LinkedList<MethodNode>();
        this.methods = new HashMap<String, MethodNode>();
        this.vars = new HashMap<String, LlvmValue>();

        if (varList != null) {
            for (LlvmValue val : varList) {
                vars.put(val.toString(), val);
            }
        }
    }

    // getters
    public String getName() {
        return name;
    }

    public LlvmClassType getClassType() {
        return new LlvmClassType(this.name);
    }

    public LlvmPointer getClassPointer() {
        return new LlvmPointer(new LlvmClassType(this.name));
    }

    public LlvmNamedValue getClassReference() {
        return new LlvmNamedValue("%this", new LlvmPointer(getClassType()));
    }

    public LlvmStructure getStructure() {
        return structure;
    }

    public LlvmInstruction getClassDeclaration() {
        return new LlvmInstruction() {
            public String toString() {
                return getClassType() + " = type" + getStructure();
            }
        };
    }

    public void addMethod(MethodNode methodNode) {
        methodList.add(methodNode);
        methods.put(methodNode.getName(), methodNode);
    }

    public List<LlvmValue> getOffset(String var) {
        List<LlvmValue> offsets = new LinkedList<LlvmValue>();
        int count = 0;
        for (LlvmValue v : this.varList) {
            if (v.toString().equals(var)) {
                break;
            }
            count++;
        }
        offsets.add(new LlvmIntegerLiteral(0));
        offsets.add(new LlvmIntegerLiteral(count));
        return offsets;
    }
}

class MethodNode extends LlvmType {
    private String name;
    private List<LlvmValue> formalList;
    private List<LlvmValue> varList;
    Map<String, LlvmValue> formals;
    Map<String, LlvmValue> vars;
    LlvmType returnType;

    public MethodNode(String name, List<LlvmValue> formalList,
                      List<LlvmValue> varList, LlvmType returnType) {
        super();
        this.name = name;
        this.formalList = formalList;
        this.varList = varList;
        this.returnType = returnType;

        this.vars = new HashMap<String, LlvmValue>();
        for (LlvmValue v : varList) {
            this.vars.put(v.toString(), v);
        }

        this.formals = new HashMap<String, LlvmValue>();
        for (LlvmValue f : formalList) {
            this.formals.put(f.toString(), f);
        }
    }

    public String getName() {
        return this.name;
    }

    public List<LlvmValue> getFormalList() {
        return this.formalList;
    }

    public List<LlvmValue> getVarList() {
        return this.varList;
    }

    public boolean hasFormal(String formal) {
        return this.formals.containsKey(formal);
    }

    public LlvmInstruction getFunctionDefinition(ClassNode classEnv) {
        return new LlvmDefine(getFunctionName(classEnv), returnType, formalList);
    }

    public String getFunctionName(ClassNode classEnv) {
        return "@__" + this.name + "_" + classEnv.getName();
    }

    public boolean hasLocalVariable(String var) {
        return this.vars.containsKey(var);
    }

}




