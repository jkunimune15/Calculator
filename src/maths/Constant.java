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
package maths;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import gui.Workspace;
import javafx.scene.image.Image;
import maths.auxiliary.Dimension;
import util.ImgUtils;

/**
 * An actual value, with no operators or variables attached. Represented as a
 * complex number with units.
 *
 * @author jkunimune
 */
public class Constant extends Expression {

	public static final Constant TAU = new Constant(2*Math.PI);
	public static final Constant PI = new Constant(Math.PI);
	public static final Constant E = new Constant(Math.E);
	
	public static final Constant ZERO = new Constant(0);
	public static final Constant ONE = new Constant(1);
	public static final Constant NEG_ONE = new Constant(-1);
	public static final Constant I = new Constant(0,1);
	public static final Constant TWO = new Constant(2);
	public static final Constant TEN = new Constant(10);
	
	
	private static final double JDT = Math.pow(2,-51);	// the Java double tolerance
	
	
	
	private final double real;
	private final double imag;
	
	private final HashMap<Dimension, Integer> dimensions;
	
	private final int radix;
	
	
	
	public Constant(double r) {
		this(r, 0);
	}
	
	
	public Constant(double r, double i) {
		real = r;
		imag = i;
		dimensions = new HashMap<Dimension, Integer>();
		radix = 10;
	}
	
	
	public Constant(double r, double i, double tol) {	// this will be rounded to the nearest tol
		real = Math.round(r/tol)*tol;
		imag = Math.round(i/tol)*tol;
		dimensions = new HashMap<Dimension, Integer>();
		radix = 10;
	}
	
	
	
	public boolean matches(Constant that) {
		return this.dimensions.equals(that.dimensions);
	}
	
	
	public double getReal() {
		return real;
	}
	
	
	public double getImag() {
		return imag;
	}
	
	
	@Override
	public int[] shape() {
		final int[] out = {1,1};
		return out;
	}
	
	
	@Override
	protected Expression getComponent(int i, int j) {
		return this;
	}
	
	
	@Override
	public List<String> getInputs(Workspace heap) {
		return new LinkedList<String>();
	}
	
	
	@Override
	public Expression replaced(String[] oldStrs, String[] newStrs) {
		return this;
	}
	
	
	@Override
	public Expression simplified(Workspace heap) {
		return this;	// Constants are already simplified
	}
	
	
	@Override
	public Image toImage() {
		return ImgUtils.drawString(this.toString());
	}
	
	
	@Override
	public String toString() {
		if (imag == 0)	// real numbers need no i component
			return format(real, radix);
		else if (real == 0)	// imaginary numbers need no real component
				return format(imag, radix)+"i";	// integers need no decimals
		else
			return "("+format(real, radix)+" + "+format(imag, radix)+"i)";
	}
	
	
	private String format(double d, int r) {
		if ((int) d == d)
			return Integer.toString((int) d);
		else
			return Double.toString(d);
	}
	
	
	public Constant plus(Constant that) {
		return new Constant(this.real+that.real, this.imag+that.imag,
				Math.max(this.tolerance(), that.tolerance()));	// check the error, because sig figs
	}
	
	public Constant negative() {
		return new Constant(-real, -imag);
	}
	
	public Constant times(Constant that) {
		return new Constant(this.real*that.real - this.imag*that.imag,
				this.real*that.imag + this.imag*that.real);
	}
	
	public Constant recip() {
		final double r2 = real*real + imag*imag;
		return new Constant(real/r2, -imag/r2);
	}
	
	public Constant mod(Constant that) {
		return this.plus(this.times(that.recip()).floor().times(that).negative());
	}
	
	public Constant floor() {
		final double s = Math.floor(Math.hypot(real, imag))
				/Math.hypot(real, imag);
		return new Constant(s*real, s*imag);
	}
	
	public Constant sqrt() {
		return this.ln().times(Constant.TWO.recip()).exp();
	}
	
	public Constant exp() {
		final double mag = Math.exp(real);
		return new Constant(mag*Math.cos(imag), mag*Math.sin(imag));
	}
	
	public Constant ln() {
		return new Constant(Math.log(Math.hypot(real, imag)),
				atan2(imag, real));
	}
	
	public Constant sin() {	// yeah, I'm defining sin in terms of sinh!
		return this.rot90(1).sinh().rot90(-1);	// what of it?!
	}
	
	public Constant cos() {
		return this.rot90(1).cosh();
	}
	
	public Constant asin() {
		return this.rot90(1).asinh().rot90(-1);
	}
	
	public Constant acos() {
		return this.acosh().rot90(-1);
	}
	
	public Constant atan() {
		return this.rot90(1).atanh().rot90(-1);
	}
	
	public Constant sinh() {
		return this.exp().plus(this.negative().exp().negative())
				.times(new Constant(0.5));
	}
	
	public Constant cosh() {
		return this.exp().plus(this.negative().exp())
				.times(new Constant(0.5));
	}
	
	public Constant asinh() {
		return this.times(this).plus(ONE).sqrt().plus(this).ln();
	}
	
	public Constant acosh() {
		return this.times(this).plus(NEG_ONE).sqrt().plus(this).ln();
	}
	
	public Constant atanh() {
		return ONE.plus(this).times(ONE.plus(this.negative()).recip())
				.sqrt().ln();
	}
	
	public Constant re() {
		return new Constant(real);
	}
	
	public Constant im() {
		return new Constant(imag);
	}
	
	public Constant abs() {
		return new Constant(Math.hypot(real, imag));
	}
	
	public Constant arg() {
		return new Constant(atan2(imag, real));
	}
	
	
	public Constant rot90(int num) {	// multiply by i^num
		switch ((num+4)%4) {
		case 0:
			return this;
		case 1:
			return new Constant(-imag, real);
		case 2:
			return new Constant(-real, -imag);
		case 3:
			return new Constant(imag, -real);
		default:
			return null;
		}
	}
	
	
	private double tolerance() {
		return this.abs().real*JDT;
	}
	
	
	
	private static double atan2(double y, double x) {	// I had to manually implement this because of issues with negative zero
		if (x >= 0 && y >= 0)
			return Math.atan(y/x);
		else if (x < 0)
			return Math.atan(y/x) + Math.PI;
		else
			return Math.atan(y/x) + 2*Math.PI;
	}

}
