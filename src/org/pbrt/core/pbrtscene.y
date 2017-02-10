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

/*
If this file is not pbrtscene.y, it was generated from pbrtscene.y and
should not be edited directly.
*/

%}

%language "java"

%token <String> STRING IDENTIFIER
%token <Float> NUMBER
%token LBRACK RBRACK

%token ACCELERATOR ACTIVETRANSFORM ALL AREALIGHTSOURCE ATTRIBUTEBEGIN
%token ATTRIBUTEEND CAMERA CONCATTRANSFORM COORDINATESYSTEM COORDSYSTRANSFORM
%token ENDTIME FILM IDENTITY INCLUDE LIGHTSOURCE LOOKAT MAKENAMEDMATERIAL
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
		slist.add($2);
		$$ = slist;
	}
	| STRING
	{
	    ArrayList<String> slist = new ArrayList<String>(16);
		slist.add($1);
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
		flist.add($2);
		$$ = flist;
	}
	| NUMBER
	{
		ArrayList<Float> flist = new ArrayList<Float>(16);
		flist.add($1);
		$$ = flist;
	}
	;
	
param_list
	: param_list_entry param_list
	{
		ArrayList<PbrtParameter> plist = $2;
		plist.add($1);
		$$ = plist;
	}
	|
	{
		// empty list
		$$ = null;
	}
	;

param_list_entry
	: STRING array
	{
		$$ = new PbrtParameter($1, $2);
	}
	;

pbrt_stmt_list
	: pbrt_stmt_list pbrt_stmt
	| pbrt_stmt
	;

pbrt_stmt
	: ACCELERATOR STRING param_list
	{
		ParamSet params = PbrtParameter.CreateParamSet($3);
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
		ParamSet params = PbrtParameter.CreateParamSet($3);
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
		ParamSet params = PbrtParameter.CreateParamSet($3);
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
		ParamSet params = PbrtParameter.CreateParamSet($3);
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
		ParamSet params = PbrtParameter.CreateParamSet($3);
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
		ParamSet params = PbrtParameter.CreateParamSet($3);
		if (params != null) {
			//InitParamSet(params, SPECTRUM_REFLECTANCE)
    		Api.pbrtMakeNamedMaterial($2, params);
    	}
	}
	| MATERIAL STRING param_list
	{
		ParamSet params = PbrtParameter.CreateParamSet($3);
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
		ParamSet params = PbrtParameter.CreateParamSet($3);
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
		ParamSet params = PbrtParameter.CreateParamSet($3);
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
		ParamSet params = PbrtParameter.CreateParamSet($3);
		if (params != null) {
    		//InitParamSet(params, SPECTRUM_REFLECTANCE)
    		Api.pbrtShape($2, params);
    	}
	}
	| INTEGRATOR STRING param_list
	{
 		ParamSet params = PbrtParameter.CreateParamSet($3);
		if (params != null) {
  	  		//InitParamSet(params, SPECTRUM_REFLECTANCE)
    		Api.pbrtIntegrator($2, params);
    	}
	}
	| TEXTURE STRING STRING STRING param_list
	{
		ParamSet params = PbrtParameter.CreateParamSet($5);
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

class PbrtParameter {

    public String type;
    public String name;
    public Object value;

    public PbrtParameter(String typename, Object value) {
        String[] tokens = typename.split(" ");
        assert tokens.length == 2;
        this.type = tokens[0];
        this.name = tokens[1];
        this.value = value;
    }

    public static ParamSet CreateParamSet(ArrayList<PbrtParameter> paramlist) {
        if (paramlist == null) return null;

        ParamSet pset = new ParamSet();
        for (PbrtParameter param : paramlist) {
            if (param.type == "int") {
                if (param.value instanceof Float[]) {
                    // parser converts all values to float, convert back to int
                    Float[] fvalue = (Float[])param.value;
                    Integer[] ivalue = new Integer[fvalue.length];
                    for (int i = 0; i < fvalue.length; i++) {
                        ivalue[i] = (int)Math.floor(fvalue[i]);
                    }
                    pset.AddInt(param.name, ivalue);
                }
                else {
                    Error.Error("Unexpected value array type for 'integer' parameter.\n");
                }
            }
            else if (param.type == "bool") {
                if (param.value instanceof Boolean[]) {
                    pset.AddBoolean(param.name, (Boolean[])param.value);
                }
                else {
                    Error.Error("Unexpected value array type for 'bool' parameter.\n");
                }
            }
            else if (param.type == "float") {
                if (param.value instanceof Float[]) {
                    pset.AddFloat(param.name, (Float[])param.value);
                }
                else {
                    Error.Error("Unexpected value array type for 'float' parameter.\n");
                }
            }
            else if (param.type == "point2") {
                if (param.value instanceof Float[]) {
                    Float[] pvalues = (Float[])param.value;
                    if (pvalues.length % 2 == 0) {
                        Point2f[] points = new Point2f[pvalues.length/2];
                        for (int i = 0; i < pvalues.length; i += 2) {
                            points[i] = new Point2f(pvalues[i], pvalues[i+1]);
                        }
                        pset.AddPoint2f(param.name, points);
                    }
                    else {
                        Error.Error("Length of 'point2' parameter list must be a factor of 2.");
                    }
                }
                else {
                    Error.Error("Unexpected value array type for 'point2' parameter.\n");
                }
            }
            else if (param.type == "vector2") {
                if (param.value instanceof Float[]) {
                    Float[] vvalues = (Float[])param.value;
                    if (vvalues.length % 2 == 0) {
                        Vector2f[] vectors = new Vector2f[vvalues.length/2];
                        for (int i = 0; i < vvalues.length; i += 2) {
                            vectors[i] = new Vector2f(vvalues[i], vvalues[i+1]);
                        }
                        pset.AddVector2f(param.name, vectors);
                    }
                    else {
                        Error.Error("Length of 'vector2' parameter list must be a factor of 2.");
                    }
                }
                else {
                    Error.Error("Unexpected value array type for 'vector2' parameter.\n");
                }
            }
            else if (param.type == "point3") {
                if (param.value instanceof Float[]) {
                    Float[] pvalues = (Float[])param.value;
                    if (pvalues.length % 3 == 0) {
                        Point3f[] points = new Point3f[pvalues.length/3];
                        for (int i = 0; i < pvalues.length; i += 3) {
                            points[i] = new Point3f(pvalues[i], pvalues[i+1], pvalues[i+2]);
                        }
                        pset.AddPoint3f(param.name, points);
                    }
                    else {
                        Error.Error("Length of 'point3' parameter list must be a factor of 3.");
                    }
                }
                else {
                    Error.Error("Unexpected value array type for 'point3' parameter.\n");
                }
            }
            else if (param.type == "vector3") {
                if (param.value instanceof Float[]) {
                    Float[] vvalues = (Float[])param.value;
                    if (vvalues.length % 3 == 0) {
                        Vector3f[] vectors = new Vector3f[vvalues.length/3];
                        for (int i = 0; i < vvalues.length; i += 3) {
                            vectors[i] = new Vector3f(vvalues[i], vvalues[i+1], vvalues[i+2]);
                        }
                        pset.AddVector3f(param.name, vectors);
                    }
                    else {
                        Error.Error("Length of 'vector3' parameter list must be a factor of 3.");
                    }
                }
                else {
                    Error.Error("Unexpected value array type for 'vector3' parameter.\n");
                }
            }
            else if (param.type == "normal") {
                if (param.value instanceof Float[]) {
                    Float[] nvalues = (Float[])param.value;
                    if (nvalues.length % 3 == 0) {
                        Normal3f[] normals = new Normal3f[nvalues.length/3];
                        for (int i = 0; i < nvalues.length; i += 3) {
                            normals[i] = new Normal3f(nvalues[i], nvalues[i+1], nvalues[i+2]);
                        }
                        pset.AddNormal3f(param.name, normals);
                    }
                    else {
                        Error.Error("Length of 'normal' parameter list must be a factor of 3.");
                    }
                }
                else {
                    Error.Error("Unexpected value array type for 'normal' parameter.\n");
                }
            }
            else if (param.type == "rgb" || param.type == "color") {
                if (param.value instanceof Float[]) {
                    Float[] cvalues = (Float[])param.value;
                    if (cvalues.length % 3 == 0) {
                        pset.AddRGBSpectrum(param.name, cvalues);
                    }
                    else {
                        Error.Error("Length of 'rgb' or 'color' parameter list must be a factor of 3.");
                    }
                }
                else {
                    Error.Error("Unexpected value array type for 'rgb' or 'color' parameter.\n");
                }
            }
            else if (param.type == "xyz") {
                if (param.value instanceof Float[]) {
                    Float[] xvalues = (Float[])param.value;
                    if (xvalues.length % 3 == 0) {
                        pset.AddXYZSpectrum(param.name, xvalues);
                    }
                    else {
                        Error.Error("Length of 'xyz' parameter list must be a factor of 3.");
                    }
                }
                else {
                    Error.Error("Unexpected value array type for 'xyz' parameter'.\n");
                }
            }
            else if (param.type == "blackbody") {
                if (param.value instanceof Float[]) {
                    pset.AddBlackbodySpectrum(param.name, (Float[])param.value);
                }
                else {
                    Error.Error("Unexpected value array type for 'blackbody' parameter.\n");
                }
            }
            else if (param.type == "spectrum") {
                if (param.value instanceof Float[]) {
                    pset.AddSampledSpectrum(param.name, (Float[])param.value);
                }
                else if (param.value instanceof String[]) {
                    pset.AddSampledSpectrumFiles(param.name, (String[])param.value);
                }
                else {
                    Error.Error("Unexpected value array type for 'spectrum' parameter.\n");
                }
            }
            else if (param.type == "string") {
                if (param.value instanceof String[]) {
                    pset.AddString(param.name, (String[])param.value);
                }
                else {
                    Error.Error("Unexpected value array type for 'string' parameter.\n");
                }
            }
            else if (param.type == "texture") {
                if (param.value instanceof String[]) {
                    String[] strings = (String[])param.value;
                    pset.AddTexture(param.name, strings[0]);
                }
                else {
                    Error.Error("Unexpected value array type for 'texture' parameter.\n");
                }
            }
            else {
                Error.Error("Unknown parameter type: %s\n", param.type);
            }
        }
        return pset;
    }
}
