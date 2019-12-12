%{
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

%}

%language "java"
%name-prefix "Pbrt"
%define parser_class_name {PbrtParser}
%define public

%token <String> STRING
%token <Float> NUMBER
%token LBRACK RBRACK

%token ACCELERATOR ACTIVETRANSFORM ALL AREALIGHTSOURCE ATTRIBUTEBEGIN
%token ATTRIBUTEEND CAMERA CONCATTRANSFORM COORDINATESYSTEM COORDSYSTRANSFORM
%token ENDTIME FILM IDENTITY INCLUDE LIGHTSOURCE LOOKAT MAKENAMEDMATERIAL MAKENAMEDMEDIUM
%token MEDIUMINTERFACE MATERIAL NAMEDMATERIAL OBJECTBEGIN OBJECTEND OBJECTINSTANCE PIXELFILTER
%token REVERSEORIENTATION ROTATE SAMPLER SCALE SHAPE STARTTIME
%token INTEGRATOR TEXTURE TRANSFORMBEGIN TRANSFORMEND TRANSFORMTIMES
%token TRANSFORM TRANSLATE WORLDBEGIN WORLDEND

%type <ArrayList<Float>> number_list number_array
%type <ArrayList<String>> string_list string_array
%type <String> single_element_string_array
%type <Float> single_element_number_array

%type <ArrayList<PbrtParameter>> param_list
%type <PbrtParameter> param_list_entry
%type <ArrayList<Object>> array

%%

start
	: pbrt_stmt_list
	;

array
	: string_array
	{
		$$ = $1;
	}
	| number_array
	{
		$$ = $1;
	}
	;

string_array
	: LBRACK string_list RBRACK
	{
		$$ = $2;
	}
	| single_element_string_array
	{
		$$ = $1;
	}
	;

single_element_string_array
	: STRING
	{
		$$ = $1;
	} 
	;
	
string_list
	: string_list STRING
	{
		ArrayList<String> slist = $1;
		if ($2 != null) {
		    slist.add($2);
		}
		$$ = slist;
	}
	| STRING
	{
	    ArrayList<String> slist = new ArrayList<String>(16);
	    if ($1 != null) {
		    slist.add($1);
		}
		$$ = slist;
	}
	;

number_array
	: LBRACK number_list RBRACK
	{
		$$ = $2;
	}
	| single_element_number_array
	{
		$$ = $1;
	}
	;

single_element_number_array
	: NUMBER
	{
		$$ = $1;
	}
	;
	
number_list
	: number_list NUMBER
	{
		ArrayList<Float> flist = $1;
		if ($2 != null) {
		    flist.add($2);
		}
		$$ = flist;
	}
	| NUMBER
	{
		ArrayList<Float> flist = new ArrayList<Float>(16);
		if ($1 != null) {
		    flist.add($1);
		}
		$$ = flist;
	}
	;
	
param_list
	: param_list_entry param_list
	{
		ArrayList<PbrtParameter> plist = $2;
		if ($2 == null) {
		    plist = new ArrayList<PbrtParameter>(4);
		}
		if ($1 != null) {
		    plist.add($1);
		}
		$$ = plist;
	}
	|
	{
		// empty list
		ArrayList<PbrtParameter> plist = new ArrayList<PbrtParameter>(4);
		$$ = plist;
	}
	;

param_list_entry
	: STRING array
	{
	    if (($1 != null) && ($2 != null)) {
	        System.out.printf("Param type: %s  Num: %d\n", $1, $2.size());
		    $$ = new PbrtParameter($1, $2);
		}
	}
	;

pbrt_stmt_list
	: pbrt_stmt_list pbrt_stmt
	| pbrt_stmt
	;

pbrt_stmt
	: ACCELERATOR STRING param_list
	{
		ParamSet params = Parser.CreateParamSet($3);
		if (params != null) {
			//InitParamSet(params, SPECTRUM_REFLECTANCE)
			Api.pbrtAccelerator($2, params);
		}
	}
	| ACTIVETRANSFORM ALL
	{
		Api.pbrtActiveTransformAll();
	}
	| ACTIVETRANSFORM ENDTIME
	{
		Api.pbrtActiveTransformEndTime();
	}
	| ACTIVETRANSFORM STARTTIME
	{
		Api.pbrtActiveTransformStartTime();
	}
	| AREALIGHTSOURCE STRING param_list
	{
		ParamSet params = Parser.CreateParamSet($3);
		if (params != null) {
			//InitParamSet(params, SPECTRUM_ILLUMINANT)
			Api.pbrtAreaLightSource($2, params);
		}
	}	
	| ATTRIBUTEBEGIN
	{
		Api.pbrtAttributeBegin();
	}
	| ATTRIBUTEEND
	{
		Api.pbrtAttributeEnd();
	}
	| CAMERA STRING param_list
	{
		ParamSet params = Parser.CreateParamSet($3);
		if (params != null) {
			//InitParamSet(params, SPECTRUM_REFLECTANCE)
			Api.pbrtCamera($2, params);
		}	
	}
	| CONCATTRANSFORM number_array
	{
		ArrayList<Float> values = $2;
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
	}
	| COORDINATESYSTEM STRING
	{
		Api.pbrtCoordinateSystem($2);
	}
	| COORDSYSTRANSFORM STRING
	{
		Api.pbrtCoordSysTransform($2);
	}
	| FILM STRING param_list
	{
		ParamSet params = Parser.CreateParamSet($3);
		if (params != null) {
			//InitParamSet(params, SPECTRUM_REFLECTANCE)
			Api.pbrtFilm($2, params);
		}		
	}
	| IDENTITY
	{
		Api.pbrtIdentity();
	}
	| INCLUDE STRING
	{
		//include_push($2, yylex)
		Parser.PushInclude($2);
	}
	| LIGHTSOURCE STRING param_list
	{
		ParamSet params = Parser.CreateParamSet($3);
		if (params != null) {
			//InitParamSet(params, SPECTRUM_ILLUMINANT)
			Api.pbrtLightSource($2, params);
		} 
		else {
			Error.Error("Failed to parse parameter list for light source.\n");
		}
	}
	| LOOKAT NUMBER NUMBER NUMBER NUMBER NUMBER NUMBER NUMBER NUMBER NUMBER
	{
    	Api.pbrtLookAt($2, $3, $4, $5, $6, $7, $8, $9, $10);
	}
	| MAKENAMEDMATERIAL STRING param_list
	{
		ParamSet params = Parser.CreateParamSet($3);
		if (params != null) {
			//InitParamSet(params, SPECTRUM_REFLECTANCE)
    		Api.pbrtMakeNamedMaterial($2, params);
    	}
	}
	| MAKENAMEDMEDIUM STRING param_list
	{
		ParamSet params = Parser.CreateParamSet($3);
		if (params != null) {
			//InitParamSet(params, SPECTRUM_REFLECTANCE)
    		Api.pbrtMakeNamedMedium($2, params);
    	}
	}
	| MATERIAL STRING param_list
	{
		ParamSet params = Parser.CreateParamSet($3);
		if (params != null) {
			//InitParamSet(params, SPECTRUM_REFLECTANCE)
	    	Api.pbrtMaterial($2, params);
	   	}
	}
	| MEDIUMINTERFACE STRING
    {
        Api.pbrtMediumInterface($2, $2);
    }
    | MEDIUMINTERFACE STRING STRING
    {
        Api.pbrtMediumInterface($2, $3);
    }
	| NAMEDMATERIAL STRING
	{
    	Api.pbrtNamedMaterial($2);
	}
	| OBJECTBEGIN STRING
	{
		Api.pbrtObjectBegin($2);
	}
	| OBJECTEND
	{
		Api.pbrtObjectEnd();
	}
	| OBJECTINSTANCE STRING
	{
		Api.pbrtObjectInstance($2);
	}
	| PIXELFILTER STRING param_list
	{
		ParamSet params = Parser.CreateParamSet($3);
		if (params != null) {
    		//InitParamSet(params, SPECTRUM_REFLECTANCE)
    		Api.pbrtPixelFilter($2, params);
    	}
	}
	| REVERSEORIENTATION
	{
		Api.pbrtReverseOrientation();
	}
	| ROTATE NUMBER NUMBER NUMBER NUMBER
	{
		Api.pbrtRotate($2, $3, $4, $5);
	}	
	| SAMPLER STRING param_list
	{
		ParamSet params = Parser.CreateParamSet($3);
		if (params != null) {
    		//InitParamSet(params, SPECTRUM_REFLECTANCE)
    		Api.pbrtSampler($2, params);
    	}
	}
	| SCALE NUMBER NUMBER NUMBER
	{
		Api.pbrtScale($2, $3, $4);
	}	
	| SHAPE STRING param_list
	{
		ParamSet params = Parser.CreateParamSet($3);
		if (params != null) {
    		//InitParamSet(params, SPECTRUM_REFLECTANCE)
    		Api.pbrtShape($2, params);
    	}
	}
	| INTEGRATOR STRING param_list
	{
 		ParamSet params = Parser.CreateParamSet($3);
		if (params != null) {
  	  		//InitParamSet(params, SPECTRUM_REFLECTANCE)
    		Api.pbrtIntegrator($2, params);
    	}
	}
	| TEXTURE STRING STRING STRING param_list
	{
		ParamSet params = Parser.CreateParamSet($5);
		if (params != null) {
	   	 	//InitParamSet(params, SPECTRUM_REFLECTANCE)
			Api.pbrtTexture($2, $3, $4, params);
		}
	}
	| TRANSFORMBEGIN
	{
		Api.pbrtTransformBegin();
	}
	| TRANSFORMEND
	{
		Api.pbrtTransformEnd();
	}
	| TRANSFORMTIMES NUMBER NUMBER
	{
		Api.pbrtTransformTimes($2, $3);
	}
	| TRANSFORM number_array
	{
		ArrayList<Float> values = $2;
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
	}
	| TRANSLATE NUMBER NUMBER NUMBER
	{
		Api.pbrtTranslate($2, $3, $4);
	}
	| WORLDBEGIN
	{
		Api.pbrtWorldBegin();
	}
	| WORLDEND
	{
		Api.pbrtWorldEnd();
	}
	;

%%
