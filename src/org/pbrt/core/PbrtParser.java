/* A Bison parser, made by GNU Bison 3.0.4.  */

/* Skeleton implementation for Bison LALR(1) parsers in Java

   Copyright (C) 2007-2015 Free Software Foundation, Inc.

   This program is free software: you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation, either version 3 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>.  */

/* As a special exception, you may create a larger work that contains
   part or all of the Bison parser skeleton and distribute that work
   under terms of your choice, so long as that work isn't itself a
   parser generator using the skeleton or a modified version thereof
   as a parser skeleton.  Alternatively, if you modify or redistribute
   the parser skeleton itself, you may (at your option) remove this
   special exception, which will cause the skeleton and the resulting
   Bison output files to be licensed under the GNU General Public
   License without this special exception.

   This special exception was added by the Free Software Foundation in
   version 2.2 of Bison.  */

/* First part of user declarations.  */
/* "PbrtParser.y":1  */ /* lalr1.java:91  */

/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */
package org.pbrt.core;

import java.util.ArrayList;
import org.pbrt.core.Api;
import org.pbrt.core.Parser;
import org.pbrt.core.Parser.PbrtParameter;

/*
If this file is not pbrtscene.y, it was generated from pbrtscene.y and
should not be edited directly.
*/


/* "PbrtParser.java":58  */ /* lalr1.java:91  */

/* "PbrtParser.java":60  */ /* lalr1.java:92  */

/**
 * A Bison parser, automatically generated from <tt>PbrtParser.y</tt>.
 *
 * @author LALR (1) parser skeleton written by Paolo Bonzini.
 */
public class PbrtParser
{
    /** Version number for the Bison executable that generated this parser.  */
  public static final String bisonVersion = "3.0.4";

  /** Name of the skeleton that generated this parser.  */
  public static final String bisonSkeleton = "lalr1.java";





  

  /**
   * Communication interface between the scanner and the Bison-generated
   * parser <tt>PbrtParser</tt>.
   */
  public interface Lexer {
    /** Token returned by the scanner to signal the end of its input.  */
    public static final int EOF = 0;

/* Tokens.  */
    /** Token number,to be returned by the scanner.  */
    static final int STRING = 258;
    /** Token number,to be returned by the scanner.  */
    static final int IDENTIFIER = 259;
    /** Token number,to be returned by the scanner.  */
    static final int NUMBER = 260;
    /** Token number,to be returned by the scanner.  */
    static final int LBRACK = 261;
    /** Token number,to be returned by the scanner.  */
    static final int RBRACK = 262;
    /** Token number,to be returned by the scanner.  */
    static final int ACCELERATOR = 263;
    /** Token number,to be returned by the scanner.  */
    static final int ACTIVETRANSFORM = 264;
    /** Token number,to be returned by the scanner.  */
    static final int ALL = 265;
    /** Token number,to be returned by the scanner.  */
    static final int AREALIGHTSOURCE = 266;
    /** Token number,to be returned by the scanner.  */
    static final int ATTRIBUTEBEGIN = 267;
    /** Token number,to be returned by the scanner.  */
    static final int ATTRIBUTEEND = 268;
    /** Token number,to be returned by the scanner.  */
    static final int CAMERA = 269;
    /** Token number,to be returned by the scanner.  */
    static final int CONCATTRANSFORM = 270;
    /** Token number,to be returned by the scanner.  */
    static final int COORDINATESYSTEM = 271;
    /** Token number,to be returned by the scanner.  */
    static final int COORDSYSTRANSFORM = 272;
    /** Token number,to be returned by the scanner.  */
    static final int ENDTIME = 273;
    /** Token number,to be returned by the scanner.  */
    static final int FILM = 274;
    /** Token number,to be returned by the scanner.  */
    static final int IDENTITY = 275;
    /** Token number,to be returned by the scanner.  */
    static final int INCLUDE = 276;
    /** Token number,to be returned by the scanner.  */
    static final int LIGHTSOURCE = 277;
    /** Token number,to be returned by the scanner.  */
    static final int LOOKAT = 278;
    /** Token number,to be returned by the scanner.  */
    static final int MAKENAMEDMATERIAL = 279;
    /** Token number,to be returned by the scanner.  */
    static final int MAKENAMEDMEDIUM = 280;
    /** Token number,to be returned by the scanner.  */
    static final int MEDIUMINTERFACE = 281;
    /** Token number,to be returned by the scanner.  */
    static final int MATERIAL = 282;
    /** Token number,to be returned by the scanner.  */
    static final int NAMEDMATERIAL = 283;
    /** Token number,to be returned by the scanner.  */
    static final int OBJECTBEGIN = 284;
    /** Token number,to be returned by the scanner.  */
    static final int OBJECTEND = 285;
    /** Token number,to be returned by the scanner.  */
    static final int OBJECTINSTANCE = 286;
    /** Token number,to be returned by the scanner.  */
    static final int PIXELFILTER = 287;
    /** Token number,to be returned by the scanner.  */
    static final int REVERSEORIENTATION = 288;
    /** Token number,to be returned by the scanner.  */
    static final int ROTATE = 289;
    /** Token number,to be returned by the scanner.  */
    static final int SAMPLER = 290;
    /** Token number,to be returned by the scanner.  */
    static final int SCALE = 291;
    /** Token number,to be returned by the scanner.  */
    static final int SHAPE = 292;
    /** Token number,to be returned by the scanner.  */
    static final int STARTTIME = 293;
    /** Token number,to be returned by the scanner.  */
    static final int INTEGRATOR = 294;
    /** Token number,to be returned by the scanner.  */
    static final int TEXTURE = 295;
    /** Token number,to be returned by the scanner.  */
    static final int TRANSFORMBEGIN = 296;
    /** Token number,to be returned by the scanner.  */
    static final int TRANSFORMEND = 297;
    /** Token number,to be returned by the scanner.  */
    static final int TRANSFORMTIMES = 298;
    /** Token number,to be returned by the scanner.  */
    static final int TRANSFORM = 299;
    /** Token number,to be returned by the scanner.  */
    static final int TRANSLATE = 300;
    /** Token number,to be returned by the scanner.  */
    static final int WORLDBEGIN = 301;
    /** Token number,to be returned by the scanner.  */
    static final int WORLDEND = 302;


    

    /**
     * Method to retrieve the semantic value of the last scanned token.
     * @return the semantic value of the last scanned token.
     */
    Object getLVal ();

    /**
     * Entry point for the scanner.  Returns the token identifier corresponding
     * to the next token and prepares to return the semantic value
     * of the token.
     * @return the token identifier corresponding to the next token.
     */
    int yylex () throws java.io.IOException;

    /**
     * Entry point for error reporting.  Emits an error
     * in a user-defined way.
     *
     * 
     * @param msg The string for the error message.
     */
     void yyerror (String msg);
  }

  /**
   * The object doing lexical analysis for us.
   */
  private Lexer yylexer;
  
  



  /**
   * Instantiates the Bison-generated parser.
   * @param yylexer The scanner that will supply tokens to the parser.
   */
  public PbrtParser (Lexer yylexer) 
  {
    
    this.yylexer = yylexer;
    
  }

  private java.io.PrintStream yyDebugStream = System.err;

  /**
   * Return the <tt>PrintStream</tt> on which the debugging output is
   * printed.
   */
  public final java.io.PrintStream getDebugStream () { return yyDebugStream; }

  /**
   * Set the <tt>PrintStream</tt> on which the debug output is printed.
   * @param s The stream that is used for debugging output.
   */
  public final void setDebugStream(java.io.PrintStream s) { yyDebugStream = s; }

  private int yydebug = 0;

  /**
   * Answer the verbosity of the debugging output; 0 means that all kinds of
   * output from the parser are suppressed.
   */
  public final int getDebugLevel() { return yydebug; }

  /**
   * Set the verbosity of the debugging output; 0 means that all kinds of
   * output from the parser are suppressed.
   * @param level The verbosity level for debugging output.
   */
  public final void setDebugLevel(int level) { yydebug = level; }

  /**
   * Print an error message via the lexer.
   *
   * @param msg The error message.
   */
  public final void yyerror (String msg)
  {
    yylexer.yyerror (msg);
  }


  protected final void yycdebug (String s) {
    if (yydebug > 0)
      yyDebugStream.println (s);
  }

  private final class YYStack {
    private int[] stateStack = new int[16];
    
    private Object[] valueStack = new Object[16];

    public int size = 16;
    public int height = -1;

    public final void push (int state, Object value                            ) {
      height++;
      if (size == height)
        {
          int[] newStateStack = new int[size * 2];
          System.arraycopy (stateStack, 0, newStateStack, 0, height);
          stateStack = newStateStack;
          

          Object[] newValueStack = new Object[size * 2];
          System.arraycopy (valueStack, 0, newValueStack, 0, height);
          valueStack = newValueStack;

          size *= 2;
        }

      stateStack[height] = state;
      
      valueStack[height] = value;
    }

    public final void pop () {
      pop (1);
    }

    public final void pop (int num) {
      // Avoid memory leaks... garbage collection is a white lie!
      if (num > 0) {
        java.util.Arrays.fill (valueStack, height - num + 1, height + 1, null);
        
      }
      height -= num;
    }

    public final int stateAt (int i) {
      return stateStack[height - i];
    }

    public final Object valueAt (int i) {
      return valueStack[height - i];
    }

    // Print the state stack on the debug stream.
    public void print (java.io.PrintStream out)
    {
      out.print ("Stack now");

      for (int i = 0; i <= height; i++)
        {
          out.print (' ');
          out.print (stateStack[i]);
        }
      out.println ();
    }
  }

  /**
   * Returned by a Bison action in order to stop the parsing process and
   * return success (<tt>true</tt>).
   */
  public static final int YYACCEPT = 0;

  /**
   * Returned by a Bison action in order to stop the parsing process and
   * return failure (<tt>false</tt>).
   */
  public static final int YYABORT = 1;



  /**
   * Returned by a Bison action in order to start error recovery without
   * printing an error message.
   */
  public static final int YYERROR = 2;

  /**
   * Internal return codes that are not supported for user semantic
   * actions.
   */
  private static final int YYERRLAB = 3;
  private static final int YYNEWSTATE = 4;
  private static final int YYDEFAULT = 5;
  private static final int YYREDUCE = 6;
  private static final int YYERRLAB1 = 7;
  private static final int YYRETURN = 8;


  private int yyerrstatus_ = 0;


  /**
   * Return whether error recovery is being done.  In this state, the parser
   * reads token until it reaches a known state, and then restarts normal
   * operation.
   */
  public final boolean recovering ()
  {
    return yyerrstatus_ == 0;
  }

  /** Compute post-reduction state.
   * @param yystate   the current state
   * @param yysym     the nonterminal to push on the stack
   */
  private int yy_lr_goto_state_ (int yystate, int yysym)
  {
    int yyr = yypgoto_[yysym - yyntokens_] + yystate;
    if (0 <= yyr && yyr <= yylast_ && yycheck_[yyr] == yystate)
      return yytable_[yyr];
    else
      return yydefgoto_[yysym - yyntokens_];
  }

  private int yyaction (int yyn, YYStack yystack, int yylen) 
  {
    Object yyval;
    

    /* If YYLEN is nonzero, implement the default value of the action:
       '$$ = $1'.  Otherwise, use the top of the stack.

       Otherwise, the following line sets YYVAL to garbage.
       This behavior is undocumented and Bison
       users should not rely upon it.  */
    if (yylen > 0)
      yyval = yystack.valueAt (yylen - 1);
    else
      yyval = yystack.valueAt (0);

    yy_reduce_print (yyn, yystack);

    switch (yyn)
      {
          case 3:
  if (yyn == 3)
    /* "PbrtParser.y":58  */ /* lalr1.java:489  */
    {
		yyval = ((ArrayList<String>)(yystack.valueAt (1-(1))));
	};
  break;
    

  case 4:
  if (yyn == 4)
    /* "PbrtParser.y":62  */ /* lalr1.java:489  */
    {
		yyval = ((ArrayList<Float>)(yystack.valueAt (1-(1))));
	};
  break;
    

  case 5:
  if (yyn == 5)
    /* "PbrtParser.y":69  */ /* lalr1.java:489  */
    {
		yyval = ((ArrayList<String>)(yystack.valueAt (3-(2))));
	};
  break;
    

  case 6:
  if (yyn == 6)
    /* "PbrtParser.y":73  */ /* lalr1.java:489  */
    {
		yyval = ((String)(yystack.valueAt (1-(1))));
	};
  break;
    

  case 7:
  if (yyn == 7)
    /* "PbrtParser.y":80  */ /* lalr1.java:489  */
    {
		yyval = ((String)(yystack.valueAt (1-(1))));
	};
  break;
    

  case 8:
  if (yyn == 8)
    /* "PbrtParser.y":87  */ /* lalr1.java:489  */
    {
		ArrayList<String> slist = ((ArrayList<String>)(yystack.valueAt (2-(1))));
		slist.add(((String)(yystack.valueAt (2-(2)))));
		yyval = slist;
	};
  break;
    

  case 9:
  if (yyn == 9)
    /* "PbrtParser.y":93  */ /* lalr1.java:489  */
    {
	    ArrayList<String> slist = new ArrayList<String>(16);
		slist.add(((String)(yystack.valueAt (1-(1)))));
		yyval = slist;
	};
  break;
    

  case 10:
  if (yyn == 10)
    /* "PbrtParser.y":102  */ /* lalr1.java:489  */
    {
		yyval = ((ArrayList<Float>)(yystack.valueAt (3-(2))));
	};
  break;
    

  case 11:
  if (yyn == 11)
    /* "PbrtParser.y":106  */ /* lalr1.java:489  */
    {
		yyval = ((Float)(yystack.valueAt (1-(1))));
	};
  break;
    

  case 12:
  if (yyn == 12)
    /* "PbrtParser.y":113  */ /* lalr1.java:489  */
    {
		yyval = ((Float)(yystack.valueAt (1-(1))));
	};
  break;
    

  case 13:
  if (yyn == 13)
    /* "PbrtParser.y":120  */ /* lalr1.java:489  */
    {
		ArrayList<Float> flist = ((ArrayList<Float>)(yystack.valueAt (2-(1))));
		flist.add(((Float)(yystack.valueAt (2-(2)))));
		yyval = flist;
	};
  break;
    

  case 14:
  if (yyn == 14)
    /* "PbrtParser.y":126  */ /* lalr1.java:489  */
    {
		ArrayList<Float> flist = new ArrayList<Float>(16);
		flist.add(((Float)(yystack.valueAt (1-(1)))));
		yyval = flist;
	};
  break;
    

  case 15:
  if (yyn == 15)
    /* "PbrtParser.y":135  */ /* lalr1.java:489  */
    {
		ArrayList<PbrtParameter> plist = ((ArrayList<PbrtParameter>)(yystack.valueAt (2-(2))));
		plist.add(((PbrtParameter)(yystack.valueAt (2-(1)))));
		yyval = plist;
	};
  break;
    

  case 16:
  if (yyn == 16)
    /* "PbrtParser.y":141  */ /* lalr1.java:489  */
    {
		// empty list
		yyval = null;
	};
  break;
    

  case 17:
  if (yyn == 17)
    /* "PbrtParser.y":149  */ /* lalr1.java:489  */
    {
		yyval = new PbrtParameter(((String)(yystack.valueAt (2-(1)))), ((ArrayList<Object>)(yystack.valueAt (2-(2)))));
	};
  break;
    

  case 20:
  if (yyn == 20)
    /* "PbrtParser.y":161  */ /* lalr1.java:489  */
    {
		ParamSet params = Parser.CreateParamSet(((ArrayList<PbrtParameter>)(yystack.valueAt (3-(3)))));
		if (params != null) {
			//InitParamSet(params, SPECTRUM_REFLECTANCE)
			Api.pbrtAccelerator(((String)(yystack.valueAt (3-(2)))), params);
		}
	};
  break;
    

  case 21:
  if (yyn == 21)
    /* "PbrtParser.y":169  */ /* lalr1.java:489  */
    {
		Api.pbrtActiveTransformAll();
	};
  break;
    

  case 22:
  if (yyn == 22)
    /* "PbrtParser.y":173  */ /* lalr1.java:489  */
    {
		Api.pbrtActiveTransformEndTime();
	};
  break;
    

  case 23:
  if (yyn == 23)
    /* "PbrtParser.y":177  */ /* lalr1.java:489  */
    {
		Api.pbrtActiveTransformStartTime();
	};
  break;
    

  case 24:
  if (yyn == 24)
    /* "PbrtParser.y":181  */ /* lalr1.java:489  */
    {
		ParamSet params = Parser.CreateParamSet(((ArrayList<PbrtParameter>)(yystack.valueAt (3-(3)))));
		if (params != null) {
			//InitParamSet(params, SPECTRUM_ILLUMINANT)
			Api.pbrtAreaLightSource(((String)(yystack.valueAt (3-(2)))), params);
		}
	};
  break;
    

  case 25:
  if (yyn == 25)
    /* "PbrtParser.y":189  */ /* lalr1.java:489  */
    {
		Api.pbrtAttributeBegin();
	};
  break;
    

  case 26:
  if (yyn == 26)
    /* "PbrtParser.y":193  */ /* lalr1.java:489  */
    {
		Api.pbrtAttributeEnd();
	};
  break;
    

  case 27:
  if (yyn == 27)
    /* "PbrtParser.y":197  */ /* lalr1.java:489  */
    {
		ParamSet params = Parser.CreateParamSet(((ArrayList<PbrtParameter>)(yystack.valueAt (3-(3)))));
		if (params != null) {
			//InitParamSet(params, SPECTRUM_REFLECTANCE)
			Api.pbrtCamera(((String)(yystack.valueAt (3-(2)))), params);
		}	
	};
  break;
    

  case 28:
  if (yyn == 28)
    /* "PbrtParser.y":205  */ /* lalr1.java:489  */
    {
		ArrayList<Float> values = ((ArrayList<Float>)(yystack.valueAt (2-(2))));
		if (values.size() == 16) {
			float[] matrix = new float[16];
			for (int i = 0; i < 16; i++) {
				matrix[i] = values.get(i);
			}
			Api.pbrtConcatTransform(matrix);
		} 
		else {
			// TODO: error - require 16 values
			Error.Error("Array argument to ConcatTransform requires 16 values.\n");
		}
	};
  break;
    

  case 29:
  if (yyn == 29)
    /* "PbrtParser.y":220  */ /* lalr1.java:489  */
    {
		Api.pbrtCoordinateSystem(((String)(yystack.valueAt (2-(2)))));
	};
  break;
    

  case 30:
  if (yyn == 30)
    /* "PbrtParser.y":224  */ /* lalr1.java:489  */
    {
		Api.pbrtCoordSysTransform(((String)(yystack.valueAt (2-(2)))));
	};
  break;
    

  case 31:
  if (yyn == 31)
    /* "PbrtParser.y":228  */ /* lalr1.java:489  */
    {
		ParamSet params = Parser.CreateParamSet(((ArrayList<PbrtParameter>)(yystack.valueAt (3-(3)))));
		if (params != null) {
			//InitParamSet(params, SPECTRUM_REFLECTANCE)
			Api.pbrtFilm(((String)(yystack.valueAt (3-(2)))), params);
		}		
	};
  break;
    

  case 32:
  if (yyn == 32)
    /* "PbrtParser.y":236  */ /* lalr1.java:489  */
    {
		Api.pbrtIdentity();
	};
  break;
    

  case 33:
  if (yyn == 33)
    /* "PbrtParser.y":240  */ /* lalr1.java:489  */
    {
		//include_push($2, yylex)
		Parser.PushInclude(((String)(yystack.valueAt (2-(2)))));
	};
  break;
    

  case 34:
  if (yyn == 34)
    /* "PbrtParser.y":245  */ /* lalr1.java:489  */
    {
		ParamSet params = Parser.CreateParamSet(((ArrayList<PbrtParameter>)(yystack.valueAt (3-(3)))));
		if (params != null) {
			//InitParamSet(params, SPECTRUM_ILLUMINANT)
			Api.pbrtLightSource(((String)(yystack.valueAt (3-(2)))), params);
		} 
		else {
			Error.Error("Failed to parse parameter list for light source.\n");
		}
	};
  break;
    

  case 35:
  if (yyn == 35)
    /* "PbrtParser.y":256  */ /* lalr1.java:489  */
    {
    	Api.pbrtLookAt(((Float)(yystack.valueAt (10-(2)))), ((Float)(yystack.valueAt (10-(3)))), ((Float)(yystack.valueAt (10-(4)))), ((Float)(yystack.valueAt (10-(5)))), ((Float)(yystack.valueAt (10-(6)))), ((Float)(yystack.valueAt (10-(7)))), ((Float)(yystack.valueAt (10-(8)))), ((Float)(yystack.valueAt (10-(9)))), ((Float)(yystack.valueAt (10-(10)))));
	};
  break;
    

  case 36:
  if (yyn == 36)
    /* "PbrtParser.y":260  */ /* lalr1.java:489  */
    {
		ParamSet params = Parser.CreateParamSet(((ArrayList<PbrtParameter>)(yystack.valueAt (3-(3)))));
		if (params != null) {
			//InitParamSet(params, SPECTRUM_REFLECTANCE)
    		Api.pbrtMakeNamedMaterial(((String)(yystack.valueAt (3-(2)))), params);
    	}
	};
  break;
    

  case 37:
  if (yyn == 37)
    /* "PbrtParser.y":268  */ /* lalr1.java:489  */
    {
		ParamSet params = Parser.CreateParamSet(((ArrayList<PbrtParameter>)(yystack.valueAt (3-(3)))));
		if (params != null) {
			//InitParamSet(params, SPECTRUM_REFLECTANCE)
    		Api.pbrtMakeNamedMedium(((String)(yystack.valueAt (3-(2)))), params);
    	}
	};
  break;
    

  case 38:
  if (yyn == 38)
    /* "PbrtParser.y":276  */ /* lalr1.java:489  */
    {
		ParamSet params = Parser.CreateParamSet(((ArrayList<PbrtParameter>)(yystack.valueAt (3-(3)))));
		if (params != null) {
			//InitParamSet(params, SPECTRUM_REFLECTANCE)
	    	Api.pbrtMaterial(((String)(yystack.valueAt (3-(2)))), params);
	   	}
	};
  break;
    

  case 39:
  if (yyn == 39)
    /* "PbrtParser.y":284  */ /* lalr1.java:489  */
    {
        Api.pbrtMediumInterface(((String)(yystack.valueAt (2-(2)))), ((String)(yystack.valueAt (2-(2)))));
    };
  break;
    

  case 40:
  if (yyn == 40)
    /* "PbrtParser.y":288  */ /* lalr1.java:489  */
    {
        Api.pbrtMediumInterface(((String)(yystack.valueAt (3-(2)))), ((String)(yystack.valueAt (3-(3)))));
    };
  break;
    

  case 41:
  if (yyn == 41)
    /* "PbrtParser.y":292  */ /* lalr1.java:489  */
    {
    	Api.pbrtNamedMaterial(((String)(yystack.valueAt (2-(2)))));
	};
  break;
    

  case 42:
  if (yyn == 42)
    /* "PbrtParser.y":296  */ /* lalr1.java:489  */
    {
		Api.pbrtObjectBegin(((String)(yystack.valueAt (2-(2)))));
	};
  break;
    

  case 43:
  if (yyn == 43)
    /* "PbrtParser.y":300  */ /* lalr1.java:489  */
    {
		Api.pbrtObjectEnd();
	};
  break;
    

  case 44:
  if (yyn == 44)
    /* "PbrtParser.y":304  */ /* lalr1.java:489  */
    {
		Api.pbrtObjectInstance(((String)(yystack.valueAt (2-(2)))));
	};
  break;
    

  case 45:
  if (yyn == 45)
    /* "PbrtParser.y":308  */ /* lalr1.java:489  */
    {
		ParamSet params = Parser.CreateParamSet(((ArrayList<PbrtParameter>)(yystack.valueAt (3-(3)))));
		if (params != null) {
    		//InitParamSet(params, SPECTRUM_REFLECTANCE)
    		Api.pbrtPixelFilter(((String)(yystack.valueAt (3-(2)))), params);
    	}
	};
  break;
    

  case 46:
  if (yyn == 46)
    /* "PbrtParser.y":316  */ /* lalr1.java:489  */
    {
		Api.pbrtReverseOrientation();
	};
  break;
    

  case 47:
  if (yyn == 47)
    /* "PbrtParser.y":320  */ /* lalr1.java:489  */
    {
		Api.pbrtRotate(((Float)(yystack.valueAt (5-(2)))), ((Float)(yystack.valueAt (5-(3)))), ((Float)(yystack.valueAt (5-(4)))), ((Float)(yystack.valueAt (5-(5)))));
	};
  break;
    

  case 48:
  if (yyn == 48)
    /* "PbrtParser.y":324  */ /* lalr1.java:489  */
    {
		ParamSet params = Parser.CreateParamSet(((ArrayList<PbrtParameter>)(yystack.valueAt (3-(3)))));
		if (params != null) {
    		//InitParamSet(params, SPECTRUM_REFLECTANCE)
    		Api.pbrtSampler(((String)(yystack.valueAt (3-(2)))), params);
    	}
	};
  break;
    

  case 49:
  if (yyn == 49)
    /* "PbrtParser.y":332  */ /* lalr1.java:489  */
    {
		Api.pbrtScale(((Float)(yystack.valueAt (4-(2)))), ((Float)(yystack.valueAt (4-(3)))), ((Float)(yystack.valueAt (4-(4)))));
	};
  break;
    

  case 50:
  if (yyn == 50)
    /* "PbrtParser.y":336  */ /* lalr1.java:489  */
    {
		ParamSet params = Parser.CreateParamSet(((ArrayList<PbrtParameter>)(yystack.valueAt (3-(3)))));
		if (params != null) {
    		//InitParamSet(params, SPECTRUM_REFLECTANCE)
    		Api.pbrtShape(((String)(yystack.valueAt (3-(2)))), params);
    	}
	};
  break;
    

  case 51:
  if (yyn == 51)
    /* "PbrtParser.y":344  */ /* lalr1.java:489  */
    {
 		ParamSet params = Parser.CreateParamSet(((ArrayList<PbrtParameter>)(yystack.valueAt (3-(3)))));
		if (params != null) {
  	  		//InitParamSet(params, SPECTRUM_REFLECTANCE)
    		Api.pbrtIntegrator(((String)(yystack.valueAt (3-(2)))), params);
    	}
	};
  break;
    

  case 52:
  if (yyn == 52)
    /* "PbrtParser.y":352  */ /* lalr1.java:489  */
    {
		ParamSet params = Parser.CreateParamSet(((ArrayList<PbrtParameter>)(yystack.valueAt (5-(5)))));
		if (params != null) {
	   	 	//InitParamSet(params, SPECTRUM_REFLECTANCE)
			Api.pbrtTexture(((String)(yystack.valueAt (5-(2)))), ((String)(yystack.valueAt (5-(3)))), ((String)(yystack.valueAt (5-(4)))), params);
		}
	};
  break;
    

  case 53:
  if (yyn == 53)
    /* "PbrtParser.y":360  */ /* lalr1.java:489  */
    {
		Api.pbrtTransformBegin();
	};
  break;
    

  case 54:
  if (yyn == 54)
    /* "PbrtParser.y":364  */ /* lalr1.java:489  */
    {
		Api.pbrtTransformEnd();
	};
  break;
    

  case 55:
  if (yyn == 55)
    /* "PbrtParser.y":368  */ /* lalr1.java:489  */
    {
		Api.pbrtTransformTimes(((Float)(yystack.valueAt (3-(2)))), ((Float)(yystack.valueAt (3-(3)))));
	};
  break;
    

  case 56:
  if (yyn == 56)
    /* "PbrtParser.y":372  */ /* lalr1.java:489  */
    {
		ArrayList<Float> values = ((ArrayList<Float>)(yystack.valueAt (2-(2))));
		if (values.size() == 16) {		
			float[] matrix = new float[16];
			for (int i = 0; i < 16; i++) {
				matrix[i] = values.get(i);
			}
			Api.pbrtTransform(matrix);
		} else {
			// TODO: error - require 16 values
			Error.Error("Array argument to Transform requires 16 values.\n");
		}
	};
  break;
    

  case 57:
  if (yyn == 57)
    /* "PbrtParser.y":386  */ /* lalr1.java:489  */
    {
		Api.pbrtTranslate(((Float)(yystack.valueAt (4-(2)))), ((Float)(yystack.valueAt (4-(3)))), ((Float)(yystack.valueAt (4-(4)))));
	};
  break;
    

  case 58:
  if (yyn == 58)
    /* "PbrtParser.y":390  */ /* lalr1.java:489  */
    {
		Api.pbrtWorldBegin();
	};
  break;
    

  case 59:
  if (yyn == 59)
    /* "PbrtParser.y":394  */ /* lalr1.java:489  */
    {
		Api.pbrtWorldEnd();
	};
  break;
    


/* "PbrtParser.java":999  */ /* lalr1.java:489  */
        default: break;
      }

    yy_symbol_print ("-> $$ =", yyr1_[yyn], yyval);

    yystack.pop (yylen);
    yylen = 0;

    /* Shift the result of the reduction.  */
    int yystate = yy_lr_goto_state_ (yystack.stateAt (0), yyr1_[yyn]);
    yystack.push (yystate, yyval);
    return YYNEWSTATE;
  }



  /*--------------------------------.
  | Print this symbol on YYOUTPUT.  |
  `--------------------------------*/

  private void yy_symbol_print (String s, int yytype,
                                 Object yyvaluep                                 )
  {
    if (yydebug > 0)
    yycdebug (s + (yytype < yyntokens_ ? " token " : " nterm ")
              + yytname_[yytype] + " ("
              + (yyvaluep == null ? "(null)" : yyvaluep.toString ()) + ")");
  }


  /**
   * Parse input from the scanner that was specified at object construction
   * time.  Return whether the end of the input was reached successfully.
   *
   * @return <tt>true</tt> if the parsing succeeds.  Note that this does not
   *          imply that there were no syntax errors.
   */
   public boolean parse () throws java.io.IOException

  {
    


    /* Lookahead and lookahead in internal form.  */
    int yychar = yyempty_;
    int yytoken = 0;

    /* State.  */
    int yyn = 0;
    int yylen = 0;
    int yystate = 0;
    YYStack yystack = new YYStack ();
    int label = YYNEWSTATE;

    /* Error handling.  */
    int yynerrs_ = 0;
    

    /* Semantic value of the lookahead.  */
    Object yylval = null;

    yycdebug ("Starting parse\n");
    yyerrstatus_ = 0;

    /* Initialize the stack.  */
    yystack.push (yystate, yylval );



    for (;;)
      switch (label)
      {
        /* New state.  Unlike in the C/C++ skeletons, the state is already
           pushed when we come here.  */
      case YYNEWSTATE:
        yycdebug ("Entering state " + yystate + "\n");
        if (yydebug > 0)
          yystack.print (yyDebugStream);

        /* Accept?  */
        if (yystate == yyfinal_)
          return true;

        /* Take a decision.  First try without lookahead.  */
        yyn = yypact_[yystate];
        if (yy_pact_value_is_default_ (yyn))
          {
            label = YYDEFAULT;
            break;
          }

        /* Read a lookahead token.  */
        if (yychar == yyempty_)
          {


            yycdebug ("Reading a token: ");
            yychar = yylexer.yylex ();
            yylval = yylexer.getLVal ();

          }

        /* Convert token to internal form.  */
        if (yychar <= Lexer.EOF)
          {
            yychar = yytoken = Lexer.EOF;
            yycdebug ("Now at end of input.\n");
          }
        else
          {
            yytoken = yytranslate_ (yychar);
            yy_symbol_print ("Next token is", yytoken,
                             yylval);
          }

        /* If the proper action on seeing token YYTOKEN is to reduce or to
           detect an error, take that action.  */
        yyn += yytoken;
        if (yyn < 0 || yylast_ < yyn || yycheck_[yyn] != yytoken)
          label = YYDEFAULT;

        /* <= 0 means reduce or error.  */
        else if ((yyn = yytable_[yyn]) <= 0)
          {
            if (yy_table_value_is_error_ (yyn))
              label = YYERRLAB;
            else
              {
                yyn = -yyn;
                label = YYREDUCE;
              }
          }

        else
          {
            /* Shift the lookahead token.  */
            yy_symbol_print ("Shifting", yytoken,
                             yylval);

            /* Discard the token being shifted.  */
            yychar = yyempty_;

            /* Count tokens shifted since error; after three, turn off error
               status.  */
            if (yyerrstatus_ > 0)
              --yyerrstatus_;

            yystate = yyn;
            yystack.push (yystate, yylval);
            label = YYNEWSTATE;
          }
        break;

      /*-----------------------------------------------------------.
      | yydefault -- do the default action for the current state.  |
      `-----------------------------------------------------------*/
      case YYDEFAULT:
        yyn = yydefact_[yystate];
        if (yyn == 0)
          label = YYERRLAB;
        else
          label = YYREDUCE;
        break;

      /*-----------------------------.
      | yyreduce -- Do a reduction.  |
      `-----------------------------*/
      case YYREDUCE:
        yylen = yyr2_[yyn];
        label = yyaction (yyn, yystack, yylen);
        yystate = yystack.stateAt (0);
        break;

      /*------------------------------------.
      | yyerrlab -- here on detecting error |
      `------------------------------------*/
      case YYERRLAB:
        /* If not already recovering from an error, report this error.  */
        if (yyerrstatus_ == 0)
          {
            ++yynerrs_;
            if (yychar == yyempty_)
              yytoken = yyempty_;
            yyerror (yysyntax_error (yystate, yytoken));
          }

        
        if (yyerrstatus_ == 3)
          {
        /* If just tried and failed to reuse lookahead token after an
         error, discard it.  */

        if (yychar <= Lexer.EOF)
          {
          /* Return failure if at end of input.  */
          if (yychar == Lexer.EOF)
            return false;
          }
        else
            yychar = yyempty_;
          }

        /* Else will try to reuse lookahead token after shifting the error
           token.  */
        label = YYERRLAB1;
        break;

      /*-------------------------------------------------.
      | errorlab -- error raised explicitly by YYERROR.  |
      `-------------------------------------------------*/
      case YYERROR:

        
        /* Do not reclaim the symbols of the rule which action triggered
           this YYERROR.  */
        yystack.pop (yylen);
        yylen = 0;
        yystate = yystack.stateAt (0);
        label = YYERRLAB1;
        break;

      /*-------------------------------------------------------------.
      | yyerrlab1 -- common code for both syntax error and YYERROR.  |
      `-------------------------------------------------------------*/
      case YYERRLAB1:
        yyerrstatus_ = 3;       /* Each real token shifted decrements this.  */

        for (;;)
          {
            yyn = yypact_[yystate];
            if (!yy_pact_value_is_default_ (yyn))
              {
                yyn += yyterror_;
                if (0 <= yyn && yyn <= yylast_ && yycheck_[yyn] == yyterror_)
                  {
                    yyn = yytable_[yyn];
                    if (0 < yyn)
                      break;
                  }
              }

            /* Pop the current state because it cannot handle the
             * error token.  */
            if (yystack.height == 0)
              return false;

            
            yystack.pop ();
            yystate = yystack.stateAt (0);
            if (yydebug > 0)
              yystack.print (yyDebugStream);
          }

        if (label == YYABORT)
            /* Leave the switch.  */
            break;



        /* Shift the error token.  */
        yy_symbol_print ("Shifting", yystos_[yyn],
                         yylval);

        yystate = yyn;
        yystack.push (yyn, yylval);
        label = YYNEWSTATE;
        break;

        /* Accept.  */
      case YYACCEPT:
        return true;

        /* Abort.  */
      case YYABORT:
        return false;
      }
}




  // Generate an error message.
  private String yysyntax_error (int yystate, int tok)
  {
    return "syntax error";
  }

  /**
   * Whether the given <code>yypact_</code> value indicates a defaulted state.
   * @param yyvalue   the value to check
   */
  private static boolean yy_pact_value_is_default_ (int yyvalue)
  {
    return yyvalue == yypact_ninf_;
  }

  /**
   * Whether the given <code>yytable_</code>
   * value indicates a syntax error.
   * @param yyvalue the value to check
   */
  private static boolean yy_table_value_is_error_ (int yyvalue)
  {
    return yyvalue == yytable_ninf_;
  }

  private static final byte yypact_ninf_ = -46;
  private static final byte yytable_ninf_ = -1;

  /* YYPACT[STATE-NUM] -- Index in YYTABLE of the portion describing
   STATE-NUM.  */
  private static final byte yypact_[] = yypact_init();
  private static final byte[] yypact_init()
  {
    return new byte[]
    {
      48,    14,    -7,    19,   -46,   -46,    24,    20,    25,    26,
      27,   -46,    29,    31,    30,    33,    34,    35,    36,    37,
      38,   -46,    39,    40,   -46,    41,    42,    43,    44,    46,
      47,   -46,   -46,    49,    20,    50,   -46,   -46,    51,    48,
     -46,    55,   -46,   -46,   -46,    55,    55,   -46,    81,   -46,
     -46,   -46,   -46,    55,   -46,    55,    91,    55,    55,    94,
      55,   -46,   -46,   -46,    55,    93,    55,    95,    55,    55,
      96,    97,   -46,    98,   -46,   -46,     1,   -46,    55,   -46,
     -46,   -46,     9,   -46,   -46,    99,   -46,   -46,   -46,   -46,
     -46,   100,   -46,   101,   -46,   -46,   104,   -46,   103,   -46,
      15,   -46,   -46,   -46,   -46,   -46,   -46,   -46,   105,   106,
     -46,    55,   -46,   -46,     2,   107,   -46,   -46,   -46,   -46,
     108,   109,   110,   111,   -46
    };
  }

/* YYDEFACT[STATE-NUM] -- Default reduction number in state STATE-NUM.
   Performed when YYTABLE does not specify something else to do.  Zero
   means the default is an error.  */
  private static final byte yydefact_[] = yydefact_init();
  private static final byte[] yydefact_init()
  {
    return new byte[]
    {
       0,     0,     0,     0,    25,    26,     0,     0,     0,     0,
       0,    32,     0,     0,     0,     0,     0,     0,     0,     0,
       0,    43,     0,     0,    46,     0,     0,     0,     0,     0,
       0,    53,    54,     0,     0,     0,    58,    59,     0,     2,
      19,    16,    21,    22,    23,    16,    16,    12,     0,    28,
      11,    29,    30,    16,    33,    16,     0,    16,    16,    39,
      16,    41,    42,    44,    16,     0,    16,     0,    16,    16,
       0,     0,    56,     0,     1,    18,     0,    20,    16,    24,
      27,    14,     0,    31,    34,     0,    36,    37,    40,    38,
      45,     0,    48,     0,    50,    51,     0,    55,     0,     7,
       0,    17,     3,     6,     4,    15,    13,    10,     0,     0,
      49,    16,    57,     9,     0,     0,    47,    52,     8,     5,
       0,     0,     0,     0,    35
    };
  }

/* YYPGOTO[NTERM-NUM].  */
  private static final byte yypgoto_[] = yypgoto_init();
  private static final byte[] yypgoto_init()
  {
    return new byte[]
    {
     -46,   -46,   -46,   -46,   -46,   -46,   -32,   -46,   -46,   -45,
     -46,   -46,    13
    };
  }

/* YYDEFGOTO[NTERM-NUM].  */
  private static final byte yydefgoto_[] = yydefgoto_init();
  private static final byte[] yydefgoto_init()
  {
    return new byte[]
    {
      -1,    38,   101,   102,   103,   114,    49,    50,    82,    77,
      78,    39,    40
    };
  }

/* YYTABLE[YYPACT[STATE-NUM]] -- What to do in state STATE-NUM.  If
   positive, shift that token.  If negative, reduce the rule whose
   number is the opposite.  If YYTABLE_NINF, syntax error.  */
  private static final byte yytable_[] = yytable_init();
  private static final byte[] yytable_init()
  {
    return new byte[]
    {
      79,    80,    72,    42,    99,   118,    47,   100,    83,   119,
      84,    43,    86,    87,   106,    89,   107,    41,   113,    90,
      81,    92,    45,    94,    95,    47,    48,    46,    51,    52,
      53,    44,    54,   105,    55,    56,    57,    58,    59,    60,
      61,    62,    63,    64,   104,    66,    65,    68,    67,    69,
      70,    74,    75,     0,    71,    73,     1,     2,    76,     3,
       4,     5,     6,     7,     8,     9,   117,    10,    11,    12,
      13,    14,    15,    16,    17,    18,    19,    20,    21,    22,
      23,    24,    25,    26,    27,    28,    81,    29,    30,    31,
      32,    33,    34,    35,    36,    37,    85,    88,    91,    96,
      93,     0,    97,    98,   108,   109,   110,   111,   112,     0,
     115,   116,   120,   121,   122,   123,   124
    };
  }

private static final byte yycheck_[] = yycheck_init();
  private static final byte[] yycheck_init()
  {
    return new byte[]
    {
      45,    46,    34,    10,     3,     3,     5,     6,    53,     7,
      55,    18,    57,    58,     5,    60,     7,     3,     3,    64,
       5,    66,     3,    68,    69,     5,     6,     3,     3,     3,
       3,    38,     3,    78,     3,     5,     3,     3,     3,     3,
       3,     3,     3,     3,    76,     3,     5,     3,     5,     3,
       3,     0,    39,    -1,     5,     5,     8,     9,     3,    11,
      12,    13,    14,    15,    16,    17,   111,    19,    20,    21,
      22,    23,    24,    25,    26,    27,    28,    29,    30,    31,
      32,    33,    34,    35,    36,    37,     5,    39,    40,    41,
      42,    43,    44,    45,    46,    47,     5,     3,     5,     3,
       5,    -1,     5,     5,     5,     5,     5,     3,     5,    -1,
       5,     5,     5,     5,     5,     5,     5
    };
  }

/* YYSTOS[STATE-NUM] -- The (internal number of the) accessing
   symbol of state STATE-NUM.  */
  private static final byte yystos_[] = yystos_init();
  private static final byte[] yystos_init()
  {
    return new byte[]
    {
       0,     8,     9,    11,    12,    13,    14,    15,    16,    17,
      19,    20,    21,    22,    23,    24,    25,    26,    27,    28,
      29,    30,    31,    32,    33,    34,    35,    36,    37,    39,
      40,    41,    42,    43,    44,    45,    46,    47,    49,    59,
      60,     3,    10,    18,    38,     3,     3,     5,     6,    54,
      55,     3,     3,     3,     3,     3,     5,     3,     3,     3,
       3,     3,     3,     3,     3,     5,     3,     5,     3,     3,
       3,     5,    54,     5,     0,    60,     3,    57,    58,    57,
      57,     5,    56,    57,    57,     5,    57,    57,     3,    57,
      57,     5,    57,     5,    57,    57,     3,     5,     5,     3,
       6,    50,    51,    52,    54,    57,     5,     7,     5,     5,
       5,     3,     5,     3,    53,     5,     5,    57,     3,     7,
       5,     5,     5,     5,     5
    };
  }

/* YYR1[YYN] -- Symbol number of symbol that rule YYN derives.  */
  private static final byte yyr1_[] = yyr1_init();
  private static final byte[] yyr1_init()
  {
    return new byte[]
    {
       0,    48,    49,    50,    50,    51,    51,    52,    53,    53,
      54,    54,    55,    56,    56,    57,    57,    58,    59,    59,
      60,    60,    60,    60,    60,    60,    60,    60,    60,    60,
      60,    60,    60,    60,    60,    60,    60,    60,    60,    60,
      60,    60,    60,    60,    60,    60,    60,    60,    60,    60,
      60,    60,    60,    60,    60,    60,    60,    60,    60,    60
    };
  }

/* YYR2[YYN] -- Number of symbols on the right hand side of rule YYN.  */
  private static final byte yyr2_[] = yyr2_init();
  private static final byte[] yyr2_init()
  {
    return new byte[]
    {
       0,     2,     1,     1,     1,     3,     1,     1,     2,     1,
       3,     1,     1,     2,     1,     2,     0,     2,     2,     1,
       3,     2,     2,     2,     3,     1,     1,     3,     2,     2,
       2,     3,     1,     2,     3,    10,     3,     3,     3,     2,
       3,     2,     2,     1,     2,     3,     1,     5,     3,     4,
       3,     3,     5,     1,     1,     3,     2,     4,     1,     1
    };
  }

  /* YYTOKEN_NUMBER[YYLEX-NUM] -- Internal symbol number corresponding
      to YYLEX-NUM.  */
  private static final short yytoken_number_[] = yytoken_number_init();
  private static final short[] yytoken_number_init()
  {
    return new short[]
    {
       0,   256,   257,   258,   259,   260,   261,   262,   263,   264,
     265,   266,   267,   268,   269,   270,   271,   272,   273,   274,
     275,   276,   277,   278,   279,   280,   281,   282,   283,   284,
     285,   286,   287,   288,   289,   290,   291,   292,   293,   294,
     295,   296,   297,   298,   299,   300,   301,   302
    };
  }

  /* YYTNAME[SYMBOL-NUM] -- String name of the symbol SYMBOL-NUM.
     First, the terminals, then, starting at \a yyntokens_, nonterminals.  */
  private static final String yytname_[] = yytname_init();
  private static final String[] yytname_init()
  {
    return new String[]
    {
  "$end", "error", "$undefined", "STRING", "IDENTIFIER", "NUMBER",
  "LBRACK", "RBRACK", "ACCELERATOR", "ACTIVETRANSFORM", "ALL",
  "AREALIGHTSOURCE", "ATTRIBUTEBEGIN", "ATTRIBUTEEND", "CAMERA",
  "CONCATTRANSFORM", "COORDINATESYSTEM", "COORDSYSTRANSFORM", "ENDTIME",
  "FILM", "IDENTITY", "INCLUDE", "LIGHTSOURCE", "LOOKAT",
  "MAKENAMEDMATERIAL", "MAKENAMEDMEDIUM", "MEDIUMINTERFACE", "MATERIAL",
  "NAMEDMATERIAL", "OBJECTBEGIN", "OBJECTEND", "OBJECTINSTANCE",
  "PIXELFILTER", "REVERSEORIENTATION", "ROTATE", "SAMPLER", "SCALE",
  "SHAPE", "STARTTIME", "INTEGRATOR", "TEXTURE", "TRANSFORMBEGIN",
  "TRANSFORMEND", "TRANSFORMTIMES", "TRANSFORM", "TRANSLATE", "WORLDBEGIN",
  "WORLDEND", "$accept", "start", "array", "string_array",
  "single_element_string_array", "string_list", "number_array",
  "single_element_number_array", "number_list", "param_list",
  "param_list_entry", "pbrt_stmt_list", "pbrt_stmt", null
    };
  }

  /* YYRLINE[YYN] -- Source line where rule number YYN was defined.  */
  private static final short yyrline_[] = yyrline_init();
  private static final short[] yyrline_init()
  {
    return new short[]
    {
       0,    53,    53,    57,    61,    68,    72,    79,    86,    92,
     101,   105,   112,   119,   125,   134,   141,   148,   155,   156,
     160,   168,   172,   176,   180,   188,   192,   196,   204,   219,
     223,   227,   235,   239,   244,   255,   259,   267,   275,   283,
     287,   291,   295,   299,   303,   307,   315,   319,   323,   331,
     335,   343,   351,   359,   363,   367,   371,   385,   389,   393
    };
  }


  // Report on the debug stream that the rule yyrule is going to be reduced.
  private void yy_reduce_print (int yyrule, YYStack yystack)
  {
    if (yydebug == 0)
      return;

    int yylno = yyrline_[yyrule];
    int yynrhs = yyr2_[yyrule];
    /* Print the symbols being reduced, and their result.  */
    yycdebug ("Reducing stack by rule " + (yyrule - 1)
              + " (line " + yylno + "), ");

    /* The symbols being reduced.  */
    for (int yyi = 0; yyi < yynrhs; yyi++)
      yy_symbol_print ("   $" + (yyi + 1) + " =",
                       yystos_[yystack.stateAt(yynrhs - (yyi + 1))],
                       ((yystack.valueAt (yynrhs-(yyi + 1)))));
  }

  /* YYTRANSLATE(YYLEX) -- Bison symbol number corresponding to YYLEX.  */
  private static final byte yytranslate_table_[] = yytranslate_table_init();
  private static final byte[] yytranslate_table_init()
  {
    return new byte[]
    {
       0,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     1,     2,     3,     4,
       5,     6,     7,     8,     9,    10,    11,    12,    13,    14,
      15,    16,    17,    18,    19,    20,    21,    22,    23,    24,
      25,    26,    27,    28,    29,    30,    31,    32,    33,    34,
      35,    36,    37,    38,    39,    40,    41,    42,    43,    44,
      45,    46,    47
    };
  }

  private static final byte yytranslate_ (int t)
  {
    if (t >= 0 && t <= yyuser_token_number_max_)
      return yytranslate_table_[t];
    else
      return yyundef_token_;
  }

  private static final int yylast_ = 116;
  private static final int yynnts_ = 13;
  private static final int yyempty_ = -2;
  private static final int yyfinal_ = 74;
  private static final int yyterror_ = 1;
  private static final int yyerrcode_ = 256;
  private static final int yyntokens_ = 48;

  private static final int yyuser_token_number_max_ = 302;
  private static final int yyundef_token_ = 2;

/* User implementation code.  */

}

/* "PbrtParser.y":399  */ /* lalr1.java:1070  */

