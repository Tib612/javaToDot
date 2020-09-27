package com.github.mweyssow.thesis_nlp.ast_parser;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.ast.visitor.GenericVisitorAdapter;
import com.github.javaparser.utils.CodeGenerationUtils;
import com.github.javaparser.utils.SourceRoot;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.lang.StringBuffer;
import java.util.*;

class myCallback implements SourceRoot.Callback {

    StringBuffer sb;

    public myCallback(StringBuffer sb) {
        this.sb = sb;
    }

    public Result process(Path localPath, Path absolutePath, ParseResult<CompilationUnit> result) {
        try {

            CompilationUnit cu = result.getResult().get();
            cu.accept(new ClassOrInterfaceVisitor(sb), null);
            
        } catch (NoSuchElementException e) {
            System.out.println(e.toString());
        }
        return Result.TERMINATE;
    }
}

public class Parser {
    private static Integer nbFilesParsed = 0;
    private static long startTotalTime;
    private static Path outputPath;

    public static String[] list = {"byte", "char", "short", "int", "long", "float", "double", "boolean", "Boolean","Byte","Character","ClassLoader","Compiler","Double","Float","Integer","Long","Math","Number","Object","Package","Process","ProcessBuilder","Runtime","RuntimePermission","SecurityManager","Short","StackTraceElement","StrictMath","String","StringBuffer","StringBuilder","System","Thread","ThreadGroup","Throwable","Void","Map"};
    public static final ArrayList<String> typeNames = new ArrayList<String>(Arrays.asList(list));

    public static void main(final String[] args) throws IOException {

        ArrayList<Path> paths = getRootPaths(args[0]);
        startTotalTime = System.currentTimeMillis();
        for (Path path : paths) {

            String projectName = path.getFileName().toString();
            // create and write in file
            try {
                final StringBuffer presentationText = new StringBuffer();
                presentationText.append("digraph D {\n");

                Files.walk(path).filter(Files::isRegularFile)
                        .forEach(filePath ->extractData(filePath, path, presentationText));

                presentationText.append("}");

                Files.write(outputPath.resolve(projectName + ".dot"), presentationText.toString().getBytes(),
                        StandardOpenOption.CREATE);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                long endTotalTime = System.currentTimeMillis();
                System.out.println("1)Passed time : " + (int) ((endTotalTime - startTotalTime) / 1000.) + "s ("
                        + nbFilesParsed + " files) " + (int) (nbFilesParsed / ((endTotalTime - startTotalTime) / 1000.))
                        + " files/sec");
            }
        }
    }

    private static ArrayList<Path> getRootPaths(String path) {
        File[] directories = new File(path).listFiles(File::isDirectory);

        // create new "output" folder
        //try {
            //Files.createDirectory(Paths.get(path + "\\output\\"));
        outputPath = Paths.get(path + "\\output\\");
        /*} catch (IOException e) {
            e.printStackTrace();
        }*/

        return (ArrayList<Path>) (new ArrayList<File>(Arrays.asList(directories))).stream().map(File::toPath)
                .collect(Collectors.toList());
    }


    private static void extractData(Path filePath, Path folderPath, StringBuffer presentationText) {
        final String fileName = filePath.getFileName().toString();
        if (fileName.endsWith(".java")) {
            try {
                final SourceRoot sourceRoot = new SourceRoot(
                        CodeGenerationUtils.mavenModuleRoot(Parser.class).resolve(filePath.getParent()));
                sourceRoot.parse("", fileName,new myCallback(presentationText));
                

                // strToFile.append(symbolsTable.toString());
                nbFilesParsed += 1;

                // strToFile.delete(0, strToFile.length());
                // symbolsTable = new SymbolsTable();

            } catch (final StackOverflowError soe) {
                System.out.println("StackOverflowError while parsing");
            } catch (final Exception e) {
                System.out.println("A parsing error occurred");
            } finally {
                // strToFile.delete(0, strToFile.length());
                if (nbFilesParsed % 10000 == 0) {
                    long endTotalTime = System.currentTimeMillis()+2;
                    System.out.println("2)Passed time : " + (int) ((endTotalTime - startTotalTime) / 1000) + "s ("
                            + nbFilesParsed + " files) "
                            + (int) (nbFilesParsed / ((endTotalTime - startTotalTime) / 1000)) + " files/sec");
                }
            }
        }
    }

    public static void addEdges(StringBuffer strToFile,String className,final VariableDeclarator v){
        // pas besoin de garder ce qu'il y a entre <T>
        String[] types = v.getType().getElementType().toString().split("[<>, \\[\\]]");

        for (String type : types) {
            // Pas besoin de générer un lien vers un type primitif
            if(Parser.typeNames.contains(type)){
                continue;
            }
            // Pas besoin de générer un lien vers un type abstrait (T,U,...)
            if(type.length()<=1){
                continue;
            }
            // Pourquoi java authorise ça ?
            if(type.contains("$")){
                continue;
            }

            String cardinality = (v.getType().getArrayLevel() == 0) ? "1" : "n";
            strToFile.append(className + " -> " + type + " [label=" + cardinality + "]\n");
        }
    }
    
}

class ClassOrInterfaceVisitor extends VoidVisitorAdapter<Object> {
    StringBuffer strToFile;
    public ClassOrInterfaceVisitor(StringBuffer str) {
        strToFile = str;
    }
    @Override
    public void visit(final ClassOrInterfaceDeclaration c, final Object arg) {
        super.visit(c, arg);
        try {
            if (c.getNameAsString().contains("$")){
                return;
            }
            strToFile.append(c.getNameAsString() + " [label="+c.getNameAsString()+"]\n");
            c.getExtendedTypes().forEach((n) -> 
                {
                    strToFile.append(n.getNameAsString() + " [label="+n.getNameAsString()+"]\n");
                    strToFile.append(c.getNameAsString() +" -> " + n.getNameAsString() + " [style=bold]\n");
                });
            c.getImplementedTypes().forEach((n) -> 
            {
                strToFile.append(n.getNameAsString() + " [label="+n.getNameAsString()+"]\n");
                strToFile.append(c.getNameAsString() +" -> " + n.getNameAsString() + " [style=dotted]\n");
             });
            if (c.isNestedType()) {
                c.accept(new VariableDeclarationInnerClassVisitor(strToFile,c.getNameAsString()), null);
            } else {
                c.accept(new VariableDeclarationVisitor(strToFile,c.getNameAsString()), null);
            }
        } catch (final Exception ioe) {
            // System.out.println(ioe.getMessage());
        }
    }
}
class IsInnerClassVisitor extends GenericVisitorAdapter<Boolean, Void> {
    @Override
    public Boolean visit(final ClassOrInterfaceDeclaration c, final Void arg) {
        super.visit(c, arg);
        if (c.isNestedType())
            return Boolean.TRUE;
        else
            return Boolean.FALSE;
    }
}
class MethodDeclarationInnerClassVisitor extends VoidVisitorAdapter<Object> {
    StringBuffer strToFile;
    public MethodDeclarationInnerClassVisitor(StringBuffer str) {
        strToFile = str;
    }
    @Override
    public void visit(final MethodDeclaration m, final Object arg) {
        super.visit(m, arg);
        try {
            // symbolsTable.getCurrentScope().putFunc(m.getNameAsString(),
            // m.getTypeAsString());
            strToFile.append(m.getNameAsString() + "[fct|" + m.getTypeAsString() + "] ");
            strToFile.append(m.getNameAsString() + " ");
        } catch (final Exception ioe) {
            // System.out.println(ioe.getMessage());
        }
    }
}
 class MethodDeclarationVisitor extends VoidVisitorAdapter<Object> {
    StringBuffer strToFile;
    public MethodDeclarationVisitor(StringBuffer str) {
        strToFile = str;
    }
    @Override
    public void visit(final MethodDeclaration m, final Object arg) {
        super.visit(m, arg);
        final Node classData = m.getParentNode().get();
        final Boolean isInInnerClass = classData.accept(new IsInnerClassVisitor(), null);
        try {
            if (isInInnerClass.equals(Boolean.FALSE)) {
                // symbolsTable.getCurrentScope().putFunc(m.getNameAsString(),
                // m.getTypeAsString());
                strToFile.append(m.getNameAsString() + "[fct|" + m.getTypeAsString() + "] ");
                strToFile.append(m.getNameAsString() + " ");
            }
        } catch (final NullPointerException e) {
        } catch (final Exception ioe) {
        }
    }
}
class VariableDeclarationInnerClassVisitor extends VoidVisitorAdapter<Object> {
    StringBuffer strToFile;
    String className;
    public VariableDeclarationInnerClassVisitor(StringBuffer str,String className) {
        strToFile = str;
        this.className = className;
    }
    @Override
    public void visit(final VariableDeclarator v, final Object arg) {
        super.visit(v, arg);
        Boolean isInMethod = v.findAncestor(MethodDeclaration.class).isPresent();
        try {
            if (!isInMethod) {
                Parser.addEdges(strToFile, className, v);
            }
        } catch (final NullPointerException e) {
        } catch (final Exception ioe) {
        }
    }
}

class VariableDeclarationVisitor extends VoidVisitorAdapter<Object> {
    StringBuffer strToFile;
    String className;
    public VariableDeclarationVisitor(StringBuffer str,String className) {
        strToFile = str;
        this.className = className;
    }
    @Override
    public void visit(final VariableDeclarator v, final Object arg) {
        super.visit(v, arg);
        final Node classData = v.findAncestor(ClassOrInterfaceDeclaration.class).get();
        Boolean isInMethod = v.findAncestor(MethodDeclaration.class).isPresent();
        final Boolean isInInnerClass = classData.accept(new IsInnerClassVisitor(), null);
        try {
            if (isInInnerClass.equals(Boolean.FALSE) && !isInMethod) {
                Parser.addEdges(strToFile, className, v);
            }
        } catch (final NullPointerException e) {
            System.out.println(e.toString());
        } catch (final Exception ioe) {
            System.out.println(ioe.toString());
        }
    }
}


