package org.pbrt.openexr.exception;

/**
 * Copyright (c) Bartosz Zaczynski, 2010
 * http://syntaxcandy.blogspot.com
 */
public class OpenExrException extends Exception {
	
	private static final long serialVersionUID = 201010161808L;
	
	public OpenExrException(String message) {
		super(message);
	}
	
	public OpenExrException(Throwable cause) {
		super(cause);
	}
	
	@Override
	public String getMessage() {
		Throwable cause = getCause();
		if (cause != null) {
			return cause.getMessage();
		} else {
			return super.getMessage();
		}
	}
}