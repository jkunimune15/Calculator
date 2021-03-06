/**
 * MIT License
 *
 * Copyright (c) 2016 Justin Kunimune
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package maths.auxiliary;

import java.util.ArrayList;
import java.util.List;

import maths.BuiltInFunction;
import maths.Comparison;
import maths.Constant;
import maths.Expression;
import maths.Function;
import maths.Locus;
import maths.Operation;
import maths.Set;
import maths.Statement;
import maths.Variable;
import maths.Vector;

/**
 * A class full of static methods that relate standard mathematical notation to
 * Expression data-structures
 *
 * @author jkunimune
 */
public class Notation {

	public static final Statement parseStatement(String input) throws IllegalArgumentException {	// create an expression from a String
		return parse(tokenize(input));
	}
	
	
	private static final List<String> tokenize(String input) throws IllegalArgumentException {
		String newInput = input; // start by checking that the parentheses match
		String nest = ""; // ")]}"
		for (int i = 0; i < input.length(); i ++) { // and add parentheses as necessary
			char c = input.charAt(i);
			if (isOpenP(c) && (nest.isEmpty() || c != nest.charAt(0))) { // if it is an openP (and not the closeP we were waiting for)
				if (c == '|' && !nest.isEmpty() && nest.charAt(0) == '}')
					continue;
				nest = correspondingP(c)+nest;
			}
			else if (isCloseP(c)) {
				if (nest.isEmpty()) // if there is a close parentheses but any previous parentheses are resolved
					newInput = correspondingP(c)+newInput; // add that openP and move on
				else if (nest.charAt(0) != c) { // if there is a standing openP that mismatches the closeP
					throw new IllegalArgumentException(
							"Mismatched parentheses: "+
							correspondingP(nest.charAt(0))+c);
				}
				else // if the close parentheses matches
					nest = nest.substring(1); // resolve that and move on
			}
		}
		input = newInput+nest;
		
		List<String> tokens = new ArrayList<String>();
		
		for (int i = 0; i < input.length(); i ++) {
			if (input.charAt(i) == ' ')	// spaces are ignored
				continue;
			else if (isSymbol(input.charAt(i)))	// symbols are taken one at a time
				tokens.add(input.substring(i,i+1));
			else if (isDigit(input.charAt(i))) {	// digits string together into numbers
				int j = i+1;
				while (j < input.length() && isDigit(input.charAt(j)))
					j ++;
				tokens.add(input.substring(i,j));
				i = j-1;
			}
			else {
				int j = i+1;	// a letter followed by letters, digits, or open parentheses
				while (j < input.length() && !isSymbol(input.charAt(j)))	// make variables
					j ++;
				if (j < input.length() && isOpenP(input.charAt(j)) && !isCloseP(input.charAt(j)))
					j ++;
				tokens.add(input.substring(i,j));
				i = j-1;
			}
		}
		return tokens;
	}
	
	
	private static final Statement parse(List<String> tokens) throws IllegalArgumentException {
		final List<Expression> exps = new ArrayList<Expression>();
		final List<String> oprs = new ArrayList<String>();
		int level = 0;
		int lastOperator = 0;
		for (int i = 0; i < tokens.size(); i ++) {
			final String s = tokens.get(i);
			if (isOpenP(s.charAt(s.length()-1))) {
				level ++;
			}
			else if (isCloseP(s.charAt(s.length()-1))) {
				level --;
			}
			else if (s.length() == 1 && isComparator(s.charAt(0))) {
				if (level == 0) {
					exps.add(parEx(tokens.subList(lastOperator, i)));
					oprs.add(s);
					lastOperator = i+1;
				}
			}
		}
		exps.add(parEx(tokens.subList(lastOperator, tokens.size())));
		if (oprs.isEmpty())
			return exps.get(0);
		else
			return new Comparison(exps, oprs);
	}
	
	
	private static final Expression parEx(List<String> tokens) throws IllegalArgumentException {	// parse an Expression
		final int n = tokens.size();
		
		if (n == 0)
			return Expression.NULL;
		if (n == 1) {
			if (isSymbol(tokens.get(0).charAt(0)))
				return new Operation(Operator.ERROR);
			else if (isDigit(tokens.get(0).charAt(0)))
				return new Constant(Double.parseDouble(tokens.get(0)));
			else
				return new Variable(tokens.get(0));
		}
		
		for (byte rank = 0; rank < 4; rank ++) {	// in order of operations
			String nest = "";
			boolean inParentheses = true; // is it completely in parentheses?
			for (int i = n-1; i >= 0; i --) {
				final String s = tokens.get(i);
				
				if (isCloseP(s.charAt(s.length()-1)) && // keep track of parentheses
						(nest.isEmpty() || s.charAt(s.length()-1) != nest.charAt(0)))
					nest = correspondingP(s.charAt(s.length()-1))+nest;
				else if (isOpenP(s.charAt(s.length()-1)))
					nest = nest.substring(1);
				if (i > 0 && nest.isEmpty())	inParentheses = false;
				
				if (nest.isEmpty()) {
					if (rank == 0) {	// vectors
						if (s.equals(",")) {
							return Vector.concat(
									parEx(tokens.subList(0, i)),
									parEx(tokens.subList(i+1, n)));
						}
					}
					if (rank == 1) {	// arithmetic
						if (s.equals("+"))
							return new Operation(Operator.ADD,
									parEx(tokens.subList(0, i)),
									parEx(tokens.subList(i+1,n)));
						else if (s.equals("-"))
							if (i > 0 && !isOperator(tokens.get(i-1).charAt(0)))	// beware of negation pretending to be subtraction
								return new Operation(Operator.SUBTRACT,
										parEx(tokens.subList(0, i)),
										parEx(tokens.subList(i+1,n)));
					}
					if (rank == 2) {	// geometric
						if (s.equals("*") || s.equals("\u2217"))
							return new Operation(Operator.MULTIPLY,
									parEx(tokens.subList(0, i)),
									parEx(tokens.subList(i+1,n)));
						else if (s.equals("\u00D7"))
							return new Operation(Operator.CROSS,
									parEx(tokens.subList(0, i)),
									parEx(tokens.subList(i+1,n)));
						else if (s.equals("/"))
							return new Operation(Operator.DIVIDE,
									parEx(tokens.subList(0, i)),
									parEx(tokens.subList(i+1,n)));
						else if (s.equals("\\"))
							return new Operation(Operator.DIVIDE,
									parEx(tokens.subList(i+1,n)),
									parEx(tokens.subList(0, i)));
						else if (s.equals("%"))
							return new Operation(Operator.MODULO,
									parEx(tokens.subList(0, i)),
									parEx(tokens.subList(i+1,n)));
						else if (i > 0 && !isOperator(tokens.get(i-1).charAt(0))
								&& !isOperator(s.charAt(0)))
							return new Operation(Operator.MULTIPLY,
									parEx(tokens.subList(0, i)),
									parEx(tokens.subList(i, n)));
					}
					if (rank == 3) {	// exponential
						if (s.equals("^"))
							return new Operation(Operator.POWER,
									parEx(tokens.subList(0, i)),
									parEx(tokens.subList(i+1,n)));
					}
				}
			}
			
			if (rank == 1 && tokens.get(0).equals("-"))	// the special negation operator
				return new Operation(Operator.NEGATE,
						parEx(tokens.subList(1, tokens.size())));
			
			if (inParentheses) {	// brackets
				if (tokens.get(0).equals("{")) {
					if (tokens.size() == 2)
						return Set.EMPTY;
					else
						return parSet(tokens.subList(1, n-1));
				}
				
				final Expression interior = parEx(tokens.subList(1, n-1));
				
				if (tokens.get(0).equals("|"))
					return new Operation(Operator.ABSOLUTE, interior);
				
				final String funcString = tokens.get(0).substring(0,
						tokens.get(0).length()-1);
				
				if (funcString.isEmpty()) {
					if (interior instanceof Vector &&
							!((Vector)interior).getParenthetic())	// vectors ignore parentheses
						return new Vector(
								true, ((Vector)interior).getComponents());
					else
						return new Operation(Operator.PARENTHESES, interior);
				}
				
				else if (funcString.equals("ln"))
					return new Operation(Operator.LN, interior);
				
				else if (funcString.equals("sqrt"))
					return new Operation(Operator.ROOT,
							interior, Constant.TWO);
				
				else if (BuiltInFunction.recognizes(funcString))
					return new BuiltInFunction(funcString, interior);
				
				else {
					if (interior instanceof Vector)
						return new Function(funcString,
								((Vector) interior).getComponents());
					else
						return new Function(funcString, interior);
				}
			}
		}
		
		throw new IllegalArgumentException("No operators detected in "+tokens);
	}
	
	
	private static final Expression parSet(List<String> tokens) throws IllegalArgumentException {	// parse a set
		int numColon = 0, numBars = 0;
		int colonIdx = -1, barIdx = -1;
		String nest = "";
		for (int i = 0; i < tokens.size(); i ++) {
			String s = tokens.get(i);
			char p = s.charAt(s.length()-1);
			if (isOpenP(p) && (nest.isEmpty() || p != nest.charAt(0)) && p != '|') // if it is an openP (and not the closeP we were waiting for)
				nest = correspondingP(p)+nest;
			else if (isCloseP(p))
				if (p != '|') // if there is a standing openP that mismatches the closeP
					nest = nest.substring(1); // resolve that and move on
			
			if (nest.isEmpty()) {
				if (s.equals(":")) {
					numColon ++;
					colonIdx = i;
				}
				else if (s.equals("|")) {
					numBars ++;
					barIdx = i;
				}
			}
		}
		
		if (numColon == 1) {
			Expression template = parEx(tokens.subList(0, colonIdx));
			return parLcs(template, tokens.subList(colonIdx+1, tokens.size()));
		}
		else if (numColon > 1) {
			throw new IllegalArgumentException("There should only be one colon in set-builder notation.");
		}
		else if (numBars%2 == 1) {
			Expression template = parEx(tokens.subList(0, barIdx));
			return parLcs(template, tokens.subList(barIdx+1, tokens.size()));
		}
		else {
			Expression interior = parEx(tokens);
			if (interior instanceof Vector && !((Vector) interior).getParenthetic())
				return new Set(((Vector) interior).getComponents());
			else
				return new Set(interior);
		}
	}
	
	
	private static final Expression parLcs(Expression exp, List<String> tokens) throws IllegalArgumentException {	// parse a set
		List<Expression> lowBounds = new ArrayList<Expression>();
		List<String> names = new ArrayList<String>();
		List<Expression> uppBounds = new ArrayList<Expression>();
		
		int lastCma = -1;
		for (int i = 0; i <= tokens.size(); i ++) {
			if (i == tokens.size() || tokens.get(i).equals(",")) {
				List<String> ineq = tokens.subList(lastCma+1, i);
				
				int j;
				for (j = 0; j < ineq.size(); j ++)
					if (ineq.get(j).equals("<") || ineq.get(j).equals("\u2264"))
						break;
				if (j >= ineq.size()-3)
					throw new IllegalArgumentException("In set-builder notation, each comma-separated condition declaration must have two '<'s");
				else
					lowBounds.add(parEx(ineq.subList(0, j)));
				
				if (!ineq.get(j+2).equals("<")&&!ineq.get(j+2).equals("\u2264"))
					throw new IllegalArgumentException("In set-builder notation, each comma-separated condition declaration must have two '<'s");
				if (isDigit(ineq.get(j+1).charAt(0)))
					throw new IllegalArgumentException("The middle part of any condition declaration must be a valid variable name, not "+ineq.get(j+1));
				for (int k = 0; k < ineq.get(j+1).length(); k ++)
					if (isSymbol(ineq.get(j+1).charAt(k)))
						throw new IllegalArgumentException("The middle part of any condition declaration must be a valid variable name, not "+ineq.get(j+1));
				names.add(ineq.get(j+1));
				
				uppBounds.add(parEx(ineq.subList(j+3, ineq.size())));
				
				lastCma = i;
			}
		}
		return new Locus(exp, names.toArray(new String[0]),
				lowBounds.toArray(new Expression[0]),
				uppBounds.toArray(new Expression[0]));
	}
	
	
	private static final boolean isDigit(char c) {
		return c >= '.' && c <= '9' && c != '/';
	}
	
	
	private static final boolean isSymbol(char c) {
		return isComparator(c) || isOperator(c) || isOpenP(c) || isCloseP(c) ||
				c == ' ';
	}
	
	
	private static final boolean isComparator(char c) {
		final char[] comp = {'=','\u2260','<','\u2264','>','\u2265'};
		for (char o: comp)
			if (c == o)
				return true;
		return false;
	}
	
	
	private static final boolean isOperator(char c) {
		final char[] ops = {'+','-','*','\u2217','\u00D7','/','\\','%','^',',',':'};
		for (char o: ops)
			if (c == o)
				return true;
		return false;
	}
	
	
	private static final boolean isOpenP(char c) {
		final char[] open = {'(','[','{','|'};
		for (char o: open)
			if (c == o)
				return true;
		return false;
	}
	
	
	private static final boolean isCloseP(char c) {
		final char[] close = {')',']','}','|'};
		for (char o: close)
			if (c == o)
				return true;
		return false;
	}
	
	
	private static final char correspondingP(char c) {
		final char[][] matches = {{'(',')'},{'[',']'},{'{','}'},{'|','|'}};
		for (char[] match: matches) {
			if (c == match[0])	return match[1];
			if (c == match[1])	return match[0];
		}
		return 'X';
	}

}
