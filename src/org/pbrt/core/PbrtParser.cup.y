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
import org.pbrt.core.Parser.PbrtParameter;

parser code {:
    PbrtFlexLexer lexer;
    public parser(PbrtFlexLexer lexer) { this.lexer = lexer; }
:}

scan with {: return lexer.next_token(); :};

terminal String STRING;
terminal Float NUMBER;
terminal LBRACK, RBRACK;

terminal ACCELERATOR, ACTIVETRANSFORM, ALL, AREALIGHTSOURCE, ATTRIBUTEBEGIN;
terminal ATTRIBUTEEND, CAMERA, CONCATTRANSFORM, COORDINATESYSTEM, COORDSYSTRANSFORM;
terminal ENDTIME, FILM, IDENTITY, INCLUDE, LIGHTSOURCE, LOOKAT, MAKENAMEDMATERIAL, MAKENAMEDMEDIUM;
terminal MEDIUMINTERFACE, MATERIAL, NAMEDMATERIAL, OBJECTBEGIN, OBJECTEND, OBJECTINSTANCE, PIXELFILTER;
terminal REVERSEORIENTATION, ROTATE, SAMPLER, SCALE, SHAPE, STARTTIME;
terminal INTEGRATOR, TEXTURE, TRANSFORMBEGIN, TRANSFORMEND, TRANSFORMTIMES;
terminal TRANSFORM, TRANSLATE, WORLDBEGIN, WORLDEND;

non terminal ArrayList<Float> number_list, number_array;
non terminal ArrayList<String> string_list, string_array;
non terminal String single_element_string_array;
non terminal Float single_element_number_array;

non terminal ArrayList<PbrtParameter> param_list;
non terminal PbrtParameter param_list_entry;
non terminal ArrayList<Object> array;
non terminal scene, pbrt_stmt, pbrt_stmt_list;

scene ::= pbrt_stmt_list:sl
	;

array ::= string_array:sa
	{:
	    ArrayList<Object> olist = new ArrayList<Object>();
	    for (String s : sa) {
	        olist.add(s);
	    }
		RESULT = olist;
	:}
	| number_array:na
	{:
	    ArrayList<Object> olist = new ArrayList<Object>();
	    for (Float n : na) {
	        olist.add(n);
	    }
		RESULT = olist;
	:}
	;

string_array ::= LBRACK string_list:sl RBRACK
	{:
		RESULT = sl;
	:}
	| single_element_string_array:sa
	{:
		ArrayList<String> slist = new ArrayList<String>(4);
		slist.add(sa);
		RESULT = slist;
	:}
	;

single_element_string_array ::= STRING:s
	{:
		RESULT = s;
	:}
	;
	
string_list ::= string_list:sl STRING:s
	{:
		ArrayList<String> slist = sl;
		slist.add(s);
		RESULT = slist;
	:}
	| STRING:s
	{:
	    ArrayList<String> slist = new ArrayList<String>(16);
		slist.add(s);
		RESULT = slist;
	:}
	;

number_array ::= LBRACK number_list:nl RBRACK
	{:
		RESULT = nl;
	:}
	| single_element_number_array:na
	{:
		ArrayList<Float> flist = new ArrayList<Float>(4);
		flist.add(na);
		RESULT = flist;
	:}
	;

single_element_number_array ::= NUMBER:n
	{:
		RESULT = n;
	:}
	;
	
number_list ::= number_list:nl NUMBER:v
	{:
		ArrayList<Float> flist = nl;
		flist.add(v);
		RESULT = flist;
	:}
	| NUMBER:v
	{:
		ArrayList<Float> flist = new ArrayList<Float>(16);
		flist.add(v);
		RESULT = flist;
	:}
	;
	
param_list ::= param_list_entry:pe param_list:pl
	{:
		ArrayList<PbrtParameter> plist = pl;
		plist.add(pe);
		RESULT = plist;
	:}
	|
	{:
		// empty list
		RESULT = null;
	:}
	;

param_list_entry ::= STRING:s array:a
	{:
		RESULT = new PbrtParameter(s, a);
	:}
	;

pbrt_stmt_list ::= pbrt_stmt_list pbrt_stmt
	| pbrt_stmt
	;

pbrt_stmt ::= ACCELERATOR STRING:s param_list:pl
	{:
		ParamSet params = Parser.CreateParamSet(pl);
		if (params != null) {
			//InitParamSet(params, SPECTRUM_REFLECTANCE)
			Api.pbrtAccelerator(s, params);
		}
	:}
	| ACTIVETRANSFORM ALL
	{:
		Api.pbrtActiveTransformAll();
	:}
	| ACTIVETRANSFORM ENDTIME
	{:
		Api.pbrtActiveTransformEndTime();
	:}
	| ACTIVETRANSFORM STARTTIME
	{:
		Api.pbrtActiveTransformStartTime();
	:}
	| AREALIGHTSOURCE STRING:s param_list:pl
	{:
		ParamSet params = Parser.CreateParamSet(pl);
		if (params != null) {
			//InitParamSet(params, SPECTRUM_ILLUMINANT)
			Api.pbrtAreaLightSource(s, params);
		}
	:}
	| ATTRIBUTEBEGIN
	{:
		Api.pbrtAttributeBegin();
	:}
	| ATTRIBUTEEND
	{:
		Api.pbrtAttributeEnd();
	:}
	| CAMERA STRING:s param_list:pl
	{:
		ParamSet params = Parser.CreateParamSet(pl);
		if (params != null) {
			//InitParamSet(params, SPECTRUM_REFLECTANCE)
			Api.pbrtCamera(s, params);
		}	
	:}
	| CONCATTRANSFORM number_array:na
	{:
		ArrayList<Float> values = na;
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
	:}
	| COORDINATESYSTEM STRING:s
	{:
		Api.pbrtCoordinateSystem(s);
	:}
	| COORDSYSTRANSFORM STRING:s
	{:
		Api.pbrtCoordSysTransform(s);
	:}
	| FILM STRING:s param_list:pl
	{:
		ParamSet params = Parser.CreateParamSet(pl);
		if (params != null) {
			//InitParamSet(params, SPECTRUM_REFLECTANCE)
			Api.pbrtFilm(s, params);
		}		
	:}
	| IDENTITY
	{:
		Api.pbrtIdentity();
	:}
	| INCLUDE STRING:s
	{:
		//include_push($2, yylex)
		Parser.PushInclude(s);
	:}
	| LIGHTSOURCE STRING:s param_list:pl
	{:
		ParamSet params = Parser.CreateParamSet(pl);
		if (params != null) {
			//InitParamSet(params, SPECTRUM_ILLUMINANT)
			Api.pbrtLightSource(s, params);
		} 
		else {
			Error.Error("Failed to parse parameter list for light source.\n");
		}
	:}
	| LOOKAT NUMBER:n0 NUMBER:n1 NUMBER:n2 NUMBER:n3 NUMBER:n4 NUMBER:n5 NUMBER:n6 NUMBER:n7 NUMBER:n8
	{:
    	Api.pbrtLookAt(n0, n1, n2, n3, n4, n5, n6, n7, n8);
	:}
	| MAKENAMEDMATERIAL STRING:s param_list:pl
	{:
		ParamSet params = Parser.CreateParamSet(pl);
		if (params != null) {
			//InitParamSet(params, SPECTRUM_REFLECTANCE)
    		Api.pbrtMakeNamedMaterial(s, params);
    	}
	:}
	| MAKENAMEDMEDIUM STRING:s param_list:pl
	{:
		ParamSet params = Parser.CreateParamSet(pl);
		if (params != null) {
			//InitParamSet(params, SPECTRUM_REFLECTANCE)
    		Api.pbrtMakeNamedMedium(s, params);
    	}
	:}
	| MATERIAL STRING:s param_list:pl
	{:
		ParamSet params = Parser.CreateParamSet(pl);
		if (params != null) {
			//InitParamSet(params, SPECTRUM_REFLECTANCE)
	    	Api.pbrtMaterial(s, params);
	   	}
	:}
	| MEDIUMINTERFACE STRING:s
    {:
        Api.pbrtMediumInterface(s, s);
    :}
    | MEDIUMINTERFACE STRING:s0 STRING:s1
    {:
        Api.pbrtMediumInterface(s0, s1);
    :}
	| NAMEDMATERIAL STRING:s
	{:
    	Api.pbrtNamedMaterial(s);
	:}
	| OBJECTBEGIN STRING:s
	{:
		Api.pbrtObjectBegin(s);
	:}
	| OBJECTEND
	{:
		Api.pbrtObjectEnd();
	:}
	| OBJECTINSTANCE STRING:s
	{:
		Api.pbrtObjectInstance(s);
	:}
	| PIXELFILTER STRING:s param_list:pl
	{:
		ParamSet params = Parser.CreateParamSet(pl);
		if (params != null) {
    		//InitParamSet(params, SPECTRUM_REFLECTANCE)
    		Api.pbrtPixelFilter(s, params);
    	}
	:}
	| REVERSEORIENTATION
	{:
		Api.pbrtReverseOrientation();
	:}
	| ROTATE NUMBER:n0 NUMBER:n1 NUMBER:n2 NUMBER:n3
	{:
		Api.pbrtRotate(n0, n1, n2, n3);
	:}
	| SAMPLER STRING:s param_list:pl
	{:
		ParamSet params = Parser.CreateParamSet(pl);
		if (params != null) {
    		//InitParamSet(params, SPECTRUM_REFLECTANCE)
    		Api.pbrtSampler(s, params);
    	}
	:}
	| SCALE NUMBER:n0 NUMBER:n1 NUMBER:n2
	{:
		Api.pbrtScale(n0, n1, n2);
	:}
	| SHAPE STRING:s param_list:pl
	{:
		ParamSet params = Parser.CreateParamSet(pl);
		if (params != null) {
    		//InitParamSet(params, SPECTRUM_REFLECTANCE)
    		Api.pbrtShape(s, params);
    	}
	:}
	| INTEGRATOR STRING:s param_list:pl
	{:
 		ParamSet params = Parser.CreateParamSet(pl);
		if (params != null) {
  	  		//InitParamSet(params, SPECTRUM_REFLECTANCE)
    		Api.pbrtIntegrator(s, params);
    	}
	:}
	| TEXTURE STRING:s0 STRING:s1 STRING:s2 param_list:pl
	{:
		ParamSet params = Parser.CreateParamSet(pl);
		if (params != null) {
	   	 	//InitParamSet(params, SPECTRUM_REFLECTANCE)
			Api.pbrtTexture(s0, s1, s2, params);
		}
	:}
	| TRANSFORMBEGIN
	{:
		Api.pbrtTransformBegin();
	:}
	| TRANSFORMEND
	{:
		Api.pbrtTransformEnd();
	:}
	| TRANSFORMTIMES NUMBER:n0 NUMBER:n1
	{:
		Api.pbrtTransformTimes(n0, n1);
	:}
	| TRANSFORM number_array:na
	{:
		ArrayList<Float> values = na;
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
	:}
	| TRANSLATE NUMBER:n0 NUMBER:n1 NUMBER:n2
	{:
		Api.pbrtTranslate(n0, n1, n2);
	:}
	| WORLDBEGIN
	{:
		Api.pbrtWorldBegin();
	:}
	| WORLDEND
	{:
		Api.pbrtWorldEnd();
	:}
	;
