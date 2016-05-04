/******************************************************************************
 * Copyright (c) 2000-2016 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   
 *   Keremi, Andras
 *   Eros, Levente
 *   Kovacs, Gabor
 *
 ******************************************************************************/

package org.eclipse.titan.codegenerator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.titan.designer.AST.TTCN3.definitions.Def_Type;
import org.eclipse.titan.designer.AST.TTCN3.types.CompField;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;

public class Def_Type_Union {
	private Def_Type typeNode;
	private StringBuilder unionString = new StringBuilder("");
	private CompilationTimeStamp compilationCounter = CompilationTimeStamp
			.getNewCompilationCounter();
	private List<String> compFieldTypes = new ArrayList<String>();
	private List<String> compFieldNames = new ArrayList<String>();
	private String nodeName = null;

	private static Map<String, Object> unionHashes = new LinkedHashMap<String, Object>();

	private Def_Type_Union(Def_Type typeNode) {
		super();
		this.typeNode = typeNode;
		nodeName = typeNode.getIdentifier().toString();

	}

	public static Def_Type_Union getInstance(Def_Type typeNode) {
		if (!unionHashes.containsKey(typeNode.getIdentifier().toString())) {
			unionHashes.put(typeNode.getIdentifier().toString(),
					new Def_Type_Union(typeNode));
		}
		return (Def_Type_Union) unionHashes.get(typeNode.getIdentifier()
				.toString());
	}

	public void addCompFields(String type, String name) {
		compFieldTypes.add(type);
		compFieldNames.add(name);
	}

	private void writeCompFields() {
		// TODO Auto-generated method stub

	}

	private void writeMatcher() {
		unionString.append("public static boolean match(" + nodeName
				+ " pattern, " + " Object" + " message){" + "\r\n");

		unionString.append("if(!(message instanceof " + nodeName
				+ "))return false;" + "\r\n");

		if (AstWalkerJava.areCommentsAllowed) {
			unionString.append("System.out.println(\"" + nodeName
					+ " tipusegyezes2\");" + "\r\n");
		}

		unionString.append("if(pattern.omitField&&((" + nodeName
				+ ")message).omitField) return true;" + "\r\n");
		unionString.append("if(pattern.anyOrOmitField) return true;" + "\r\n");
		unionString.append("if(pattern.anyField&&((" + nodeName
				+ ")message).omitField) return true;" + "\r\n");
		unionString.append("if(pattern.omitField&&((" + nodeName
				+ ")message).omitField) return false;" + "\r\n");
		unionString.append("if(pattern.anyField&&((" + nodeName
				+ ")message).anyField) return false;" + "\r\n");

		for (int l = 0; l < compFieldTypes.size(); l++) {
			unionString.append("if(pattern instanceof " + "SC_" + (l + 1) + "_"
					+ nodeName + " && message instanceof " + "SC_" + (l + 1)
					+ "_" + nodeName + ") return " + "SC_" + (l + 1) + "_"
					+ nodeName + ".match((" + "SC_" + (l + 1) + "_" + nodeName
					+ ")pattern, (" + "SC_" + (l + 1) + "_" + nodeName
					+ ")message);" + "\r\n");
		}

		unionString.append("return false;" + "\r\n");
		unionString.append("}" + "\r\n");

	}

	private void writeEquals() {
		unionString.append("public boolean equals(" + nodeName + " v){ "
				+ "\r\n");// 0901

		for (int l = 0; l < compFieldTypes.size(); l++) {

			unionString.append("if(this instanceof " + "SC_" + (l + 1) + "_"
					+ nodeName + " && v instanceof " + "SC_" + (l + 1) + "_"
					+ nodeName + ") return ((" + "SC_" + (l + 1) + "_"
					+ nodeName + ")this).equals((" + "SC_" + (l + 1) + "_"
					+ nodeName + ")v);" + "\r\n");

		}

		unionString.append("	return false;" + "\r\n");
		unionString.append("}" + "\r\n");

	}

	public void writeUnionClasses() {

		StringBuilder unionChildString = new StringBuilder("");
		String fileNameBackup = myASTVisitor.currentFileName;

		for (int i = 0; i < compFieldTypes.size(); i++) {
			// set file name
			myASTVisitor.currentFileName = "SC_" + (i + 1) + "_" + nodeName;
			myASTVisitor.visualizeNodeToJava(myASTVisitor.importListStrings);

			unionChildString.append("\r\nclass SC_" + (i + 1) + "_" + nodeName
					+ " extends " + nodeName + "{\r\n");

			unionChildString.append("	");

			unionChildString.append("public " + compFieldTypes.get(i) + " "
					+ compFieldNames.get(i) + ";\r\n");

			unionChildString.append("public static boolean match(" + "SC_"
					+ (i + 1) + "_" + nodeName + " pattern, " + "Object"
					+ " message){" + "\r\n");

			if (AstWalkerJava.areCommentsAllowed) {
				unionChildString.append("System.out.println(\"SC_" + (i + 1)
						+ "\");" + "\r\n");
			}

			unionChildString.append("if(!(message instanceof " + "SC_"
					+ (i + 1) + "_" + nodeName + ")) return false;" + "\r\n");
			unionChildString.append("if(pattern.omitField&&((" + "SC_"
					+ (i + 1) + "_" + nodeName
					+ ")message).omitField) return true;" + "\r\n");
			unionChildString.append("if(pattern.anyOrOmitField) return true;"
					+ "\r\n");
			unionChildString.append("if(pattern.anyField&&((" + "SC_" + (i + 1)
					+ "_" + nodeName + ")message).omitField) return true;"
					+ "\r\n");
			unionChildString.append("if(pattern.omitField&&((" + "SC_"
					+ (i + 1) + "_" + nodeName
					+ ")message).omitField) return false;" + "\r\n");
			unionChildString.append("if(pattern.anyField&&((" + "SC_" + (i + 1)
					+ "_" + nodeName + ")message).anyField) return false;"
					+ "\r\n");

			unionChildString.append("	return " + compFieldTypes.get(i)
					+ ".match(pattern." + compFieldNames.get(i) + ", (("
					+ "SC_" + (i + 1) + "_" + nodeName + ")message)."
					+ compFieldNames.get(i) + ");" + "\r\n");

			unionChildString.append("}\r\n");

			unionChildString.append("public boolean equals(" + "SC_" + (i + 1)
					+ "_" + nodeName + " v){" + "\r\n"); // 0901

			unionChildString.append("	return this." + compFieldNames.get(i)
					+ ".equals(v." + compFieldNames.get(i) + ");" + "\r\n");

			unionChildString.append("}" + "\r\n");
			unionChildString.append("}" + "\r\n");
			myASTVisitor.visualizeNodeToJava(unionChildString.toString());
			unionChildString.delete(0, unionChildString.length());

		}
		myASTVisitor.currentFileName = fileNameBackup;

	}

	public String getJavaSource() {
		unionString.append("class " + nodeName + " extends UnionDef{" + "\r\n");
		this.writeCompFields();
		this.writeMatcher();
		this.writeEquals();
		this.writeUnionClasses();
		unionString.append("\r\n}");
		String returnString = unionString.toString();
		unionString.setLength(0);
		compFieldTypes.clear();
		compFieldNames.clear();
		return returnString;
	}

}
