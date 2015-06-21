package de.t_battermann.dhbw.todolist;

import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;

/**
 * Print error messages to the console
 */
public class ErrorPrinter {
	public static void printError(String p, String s) {
		SimpleDateFormat format = new SimpleDateFormat();
		format.applyPattern("yyyyMMdd'T'HH:mm:ssZ");
		System.err.println("[" + format.format(GregorianCalendar.getInstance().getTime()) + " " + p + "] " + s);
	}

	public static void printInfo(String p, String s) {
		SimpleDateFormat format = new SimpleDateFormat();
		format.applyPattern("yyyyMMdd'T'HH:mm:ssZ");
		System.out.println("[" + format.format(GregorianCalendar.getInstance().getTime()) + " " + p + "] " + s);
	}

	public static void printInfo(String s) {
		ErrorPrinter.printInfo("info", s);
	}

	public static void printError(String s) {
		ErrorPrinter.printError("error", s);
	}

	public static void printDebug(String s) {
		ErrorPrinter.printInfo("debug", s);
	}

	public static void printWarning(String s) {
		ErrorPrinter.printError("warn", s);
	}
}
